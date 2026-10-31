package nl.johnkruidhof.musiccollection.client.musicbrainz;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.client.RestClient;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

import nl.johnkruidhof.musiccollection.TestFixtures;
import nl.johnkruidhof.musiccollection.config.AppProperties;
import nl.johnkruidhof.musiccollection.config.MusicBrainzClientConfig;
import nl.johnkruidhof.musiccollection.dto.lookup.ReleaseCandidateDto;
import nl.johnkruidhof.musiccollection.dto.lookup.ReleaseSearchCriteria;
import nl.johnkruidhof.musiccollection.dto.release.ReleaseDto;
import nl.johnkruidhof.musiccollection.dto.release.TrackDto;
import nl.johnkruidhof.musiccollection.entity.MediaType;
import nl.johnkruidhof.musiccollection.entity.MetadataSource;
import nl.johnkruidhof.musiccollection.exception.ExternalLookupException;
import nl.johnkruidhof.musiccollection.mapper.MusicBrainzMapperImpl;

class MusicBrainzMetadataProviderTest {

    private static final String USER_AGENT = "MusicCollectionTest/1.0 ( test@example.com )";
    private static final String MBID = "6a09041b-0f79-3278-88d9-0552dd28d37f";

    @RegisterExtension
    static WireMockExtension musicbrainz = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @RegisterExtension
    static WireMockExtension coverArtArchive = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private MusicBrainzMetadataProvider provider;

    @BeforeEach
    void setUp() {
        AppProperties.MusicBrainz properties = new AppProperties.MusicBrainz(
                musicbrainz.baseUrl(), coverArtArchive.baseUrl(), USER_AGENT, AppProperties.Throttle.NONE);
        provider = new MusicBrainzMetadataProvider(
                MusicBrainzClientConfig.createMusicBrainzApi(RestClient.builder(), properties),
                MusicBrainzClientConfig.createCoverArtArchiveApi(RestClient.builder(), properties),
                new MusicBrainzMapperImpl(),
                new AppProperties(null, null, properties, null, null));
    }

    @Test
    void findByBarcodeSendsLuceneQueryAndMapsCandidates() {
        musicbrainz.stubFor(get(urlPathEqualTo("/release"))
                .withQueryParam("query", equalTo("barcode:724385522925"))
                .withQueryParam("fmt", equalTo("json"))
                .withQueryParam("limit", equalTo("25"))
                .withHeader("User-Agent", equalTo(USER_AGENT))
                .willReturn(okJson(fixture("search-barcode.json"))));

        List<ReleaseCandidateDto> candidates = provider.findByBarcode("724385522925");

        assertThat(candidates).hasSize(2);
        assertThat(candidates.getFirst()).isEqualTo(new ReleaseCandidateDto(
                MetadataSource.MUSICBRAINZ, MBID, "Radiohead", "OK Computer", 1997, MediaType.CD,
                "CD", "Capitol Records", "CDP 7243 8 55229 2 5", "US", "724385522925",
                coverArtArchive.baseUrl() + "/release/" + MBID + "/front-250", null, null));

        ReleaseCandidateDto vinyl = candidates.get(1);
        assertThat(vinyl.mediaType()).isEqualTo(MediaType.VINYL);
        assertThat(vinyl.formatDetails()).isEqualTo("2×12\" Vinyl");
        assertThat(vinyl.releaseYear()).isNull();
        assertThat(vinyl.barcode()).isNull();
        assertThat(vinyl.catalogNumber()).isNull();
        assertThat(vinyl.country()).isEqualTo("XE");
    }

    @Test
    void findByBarcodeWithoutResultsReturnsEmptyList() {
        musicbrainz.stubFor(get(urlPathEqualTo("/release")).willReturn(okJson(fixture("search-empty.json"))));

        assertThat(provider.findByBarcode("0000000000000")).isEmpty();
    }

    @Test
    void findByBarcodeWithInvalidBarcodeDoesNotCallMusicBrainz() {
        assertThat(provider.findByBarcode("abc OR *")).isEmpty();
        assertThat(musicbrainz.getAllServeEvents()).isEmpty();
    }

    @Test
    void searchBuildsLuceneQueryFromCriteria() {
        musicbrainz.stubFor(get(urlPathEqualTo("/release")).willReturn(okJson(fixture("search-empty.json"))));

        provider.search(new ReleaseSearchCriteria("Guns N' Roses", "Appetite \"for\" Destruction", 1987,
                MediaType.VINYL));

        musicbrainz.verify(getRequestedFor(urlPathEqualTo("/release")).withQueryParam("query", equalTo(
                "artist:\"Guns N' Roses\" AND release:\"Appetite \\\"for\\\" Destruction\" AND date:1987"
                        + " AND format:(\"Vinyl\" OR \"12\\\" Vinyl\" OR \"10\\\" Vinyl\" OR \"7\\\" Vinyl\")")));
    }

    @Test
    void searchWithoutArtistAndTitleDoesNotCallMusicBrainz() {
        assertThat(provider.search(new ReleaseSearchCriteria(" ", null, 1997, MediaType.CD))).isEmpty();
        assertThat(musicbrainz.getAllServeEvents()).isEmpty();
    }

    @Test
    void getDetailsMapsReleaseWithTracksAndCoverArt() {
        musicbrainz.stubFor(get(urlPathEqualTo("/release/" + MBID))
                .withQueryParam("inc", equalTo("artist-credits labels recordings"))
                .withQueryParam("fmt", equalTo("json"))
                .willReturn(okJson(fixture("release.json"))));
        coverArtArchive.stubFor(get("/release/" + MBID).willReturn(okJson(fixture("cover-art.json"))));

        ReleaseDto release = provider.getDetails(MBID).orElseThrow();

        assertThat(release.id()).isNull();
        assertThat(release.source()).isEqualTo(MetadataSource.MUSICBRAINZ);
        assertThat(release.musicbrainzId()).isEqualTo(MBID);
        assertThat(release.discogsId()).isNull();
        assertThat(release.artist()).isEqualTo("Radiohead");
        assertThat(release.title()).isEqualTo("OK Computer");
        assertThat(release.releaseYear()).isEqualTo(1997);
        assertThat(release.mediaType()).isEqualTo(MediaType.CD);
        assertThat(release.formatDetails()).isEqualTo("CD + DVD-Video");
        assertThat(release.label()).isEqualTo("Capitol Records");
        assertThat(release.catalogNumber()).isEqualTo("CDP 7243 8 55229 2 5");
        assertThat(release.barcode()).isEqualTo("724385522925");
        // Voorkant (niet de achterkant), http opgewaardeerd naar https.
        assertThat(release.coverImageUrl())
                .isEqualTo("https://coverartarchive.org/release/" + MBID + "/2.jpg");
        assertThat(release.coverThumbnailUrl())
                .isEqualTo("https://coverartarchive.org/release/" + MBID + "/2-250.jpg");

        // Trackartiest alleen als die afwijkt van de releaseartiest; duur afgerond op seconden.
        assertThat(release.tracks()).containsExactly(
                new TrackDto(1, "1", 1, "Airbag", 284, null),
                new TrackDto(1, "2", 2, "Paranoid Android", 383, null),
                new TrackDto(2, "1", 3, "Karma Police (live)", null, "Radiohead feat. Thom Yorke"));
    }

    @Test
    void getDetailsFallsBackToFixedCoverUrlsWhenCoverArtArchiveFails() {
        musicbrainz.stubFor(get(urlPathEqualTo("/release/" + MBID)).willReturn(okJson(fixture("release.json"))));
        coverArtArchive.stubFor(get("/release/" + MBID).willReturn(aResponse().withStatus(500)));

        ReleaseDto release = provider.getDetails(MBID).orElseThrow();

        String base = coverArtArchive.baseUrl() + "/release/" + MBID;
        assertThat(release.coverImageUrl()).isEqualTo(base + "/front");
        assertThat(release.coverThumbnailUrl()).isEqualTo(base + "/front-250");
    }

    @Test
    void getDetailsWithoutFrontCoverDoesNotCallCoverArtArchive() {
        String withoutFront = fixture("release.json").replace("\"front\": true", "\"front\": false");
        musicbrainz.stubFor(get(urlPathEqualTo("/release/" + MBID)).willReturn(okJson(withoutFront)));

        ReleaseDto release = provider.getDetails(MBID).orElseThrow();

        assertThat(release.coverImageUrl()).isNull();
        assertThat(release.coverThumbnailUrl()).isNull();
        assertThat(coverArtArchive.getAllServeEvents()).isEmpty();
    }

    @Test
    void getDetailsOfUnknownReleaseReturnsEmpty() {
        musicbrainz.stubFor(get(urlPathEqualTo("/release/" + MBID)).willReturn(aResponse().withStatus(404)
                .withHeader("Content-Type", "application/json").withBody("{\"error\": \"Not Found\"}")));

        assertThat(provider.getDetails(MBID)).isEmpty();
    }

    @Test
    void getDetailsWithInvalidMbidDoesNotCallMusicBrainz() {
        assertThat(provider.getDetails("2149389")).isEmpty();
        assertThat(provider.getDetails("../artist/x")).isEmpty();
        assertThat(musicbrainz.getAllServeEvents()).isEmpty();
    }

    @Test
    void busyServerBecomesExternalLookupException() {
        musicbrainz.stubFor(get(urlPathEqualTo("/release")).willReturn(aResponse().withStatus(503)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"error\": \"The MusicBrainz web server is currently busy. Please try again later.\"}")));

        assertThatThrownBy(() -> provider.findByBarcode("724385522925"))
                .isInstanceOf(ExternalLookupException.class)
                .extracting("source").isEqualTo("MusicBrainz");
    }

    private static String fixture(String name) {
        return TestFixtures.read("musicbrainz/" + name);
    }
}
