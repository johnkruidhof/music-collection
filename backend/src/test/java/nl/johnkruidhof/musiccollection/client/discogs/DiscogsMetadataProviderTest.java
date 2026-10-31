package nl.johnkruidhof.musiccollection.client.discogs;

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
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.client.RestClient;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

import nl.johnkruidhof.musiccollection.TestFixtures;
import nl.johnkruidhof.musiccollection.config.AppProperties;
import nl.johnkruidhof.musiccollection.config.DiscogsClientConfig;
import nl.johnkruidhof.musiccollection.dto.lookup.ReleaseCandidateDto;
import nl.johnkruidhof.musiccollection.dto.lookup.ReleaseSearchCriteria;
import nl.johnkruidhof.musiccollection.dto.release.ReleaseDto;
import nl.johnkruidhof.musiccollection.dto.release.TrackDto;
import nl.johnkruidhof.musiccollection.entity.MediaType;
import nl.johnkruidhof.musiccollection.entity.MetadataSource;
import nl.johnkruidhof.musiccollection.exception.ExternalLookupException;
import nl.johnkruidhof.musiccollection.mapper.DiscogsMapperImpl;

class DiscogsMetadataProviderTest {

    private static final String TOKEN = "test-token";
    private static final String USER_AGENT = "MusicCollectionTest/1.0";

    @RegisterExtension
    static WireMockExtension discogs = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private DiscogsMetadataProvider provider;

    @BeforeEach
    void setUp() {
        DiscogsApi api = DiscogsClientConfig.createDiscogsApi(RestClient.builder(),
                new AppProperties.Discogs(discogs.baseUrl(), TOKEN, USER_AGENT));
        provider = new DiscogsMetadataProvider(api, new DiscogsMapperImpl());
    }

    @Test
    void findByBarcodeSendsTokenAndUserAgentAndMapsCandidates() {
        discogs.stubFor(get(urlPathEqualTo("/database/search"))
                .withQueryParam("barcode", equalTo("724385522912"))
                .withQueryParam("type", equalTo("release"))
                .withHeader("Authorization", equalTo("Discogs token=" + TOKEN))
                .withHeader("User-Agent", equalTo(USER_AGENT))
                .willReturn(okJson(fixture("search-barcode.json"))));

        List<ReleaseCandidateDto> candidates = provider.findByBarcode("724385522912");

        assertThat(candidates).hasSize(2);
        assertThat(candidates.getFirst()).isEqualTo(new ReleaseCandidateDto(
                MetadataSource.DISCOGS, "2149389", "Radiohead", "OK Computer", 1997, MediaType.VINYL,
                "Vinyl, LP, Album", "Parlophone", "NODATA 02", "UK", "724385522912",
                "https://i.discogs.com/thumb-2149389.jpg"));

        // Lege velden van Discogs ("" en spacer.gif) worden null.
        ReleaseCandidateDto cd = candidates.get(1);
        assertThat(cd.mediaType()).isEqualTo(MediaType.CD);
        assertThat(cd.releaseYear()).isNull();
        assertThat(cd.catalogNumber()).isNull();
        assertThat(cd.barcode()).isNull();
        assertThat(cd.coverThumbnailUrl()).isNull();
    }

    @Test
    void findByBarcodeWithoutResultsReturnsEmptyList() {
        discogs.stubFor(get(urlPathEqualTo("/database/search"))
                .willReturn(okJson(fixture("search-empty.json"))));

        assertThat(provider.findByBarcode("0000000000000")).isEmpty();
    }

    @Test
    void searchPassesCriteriaAsDiscogsQueryParameters() {
        discogs.stubFor(get(urlPathEqualTo("/database/search"))
                .willReturn(okJson(fixture("search-barcode.json"))));

        List<ReleaseCandidateDto> candidates = provider.search(
                new ReleaseSearchCriteria("Radiohead", "OK Computer", 1997, MediaType.BLU_RAY));

        assertThat(candidates).hasSize(2);
        discogs.verify(getRequestedFor(urlPathEqualTo("/database/search"))
                .withQueryParam("artist", equalTo("Radiohead"))
                .withQueryParam("release_title", equalTo("OK Computer"))
                .withQueryParam("year", equalTo("1997"))
                .withQueryParam("format", equalTo("Blu-ray"))
                .withQueryParam("type", equalTo("release")));
    }

    @Test
    void searchOmitsEmptyCriteria() {
        discogs.stubFor(get(urlPathEqualTo("/database/search"))
                .willReturn(okJson(fixture("search-empty.json"))));

        provider.search(new ReleaseSearchCriteria(" ", "OK Computer", null, MediaType.OTHER));

        List<String> queries = discogs.getAllServeEvents().stream()
                .map(event -> event.getRequest().getUrl()).toList();
        assertThat(queries).singleElement().satisfies(url -> assertThat(url)
                .contains("release_title=OK%20Computer")
                .doesNotContain("artist=", "year=", "format="));
    }

    @Test
    void searchWithoutArtistAndTitleDoesNotCallDiscogs() {
        assertThat(provider.search(new ReleaseSearchCriteria(null, "  ", 1997, null))).isEmpty();
        assertThat(discogs.getAllServeEvents()).isEmpty();
    }

    @Test
    void getDetailsMapsReleaseWithTracks() {
        discogs.stubFor(get("/releases/2149389").willReturn(okJson(fixture("release.json"))));

        ReleaseDto release = provider.getDetails("2149389").orElseThrow();

        assertThat(release.id()).isNull();
        assertThat(release.source()).isEqualTo(MetadataSource.DISCOGS);
        assertThat(release.discogsId()).isEqualTo("2149389");
        assertThat(release.musicbrainzId()).isNull();
        assertThat(release.artist()).isEqualTo("Radiohead");
        assertThat(release.title()).isEqualTo("OK Computer");
        assertThat(release.releaseYear()).isEqualTo(1997);
        assertThat(release.mediaType()).isEqualTo(MediaType.VINYL);
        assertThat(release.formatDetails()).isEqualTo("2×Vinyl, LP, Album, 180 gram");
        assertThat(release.label()).isEqualTo("Parlophone");
        assertThat(release.catalogNumber()).isEqualTo("NODATA 02");
        assertThat(release.country()).isEqualTo("UK");
        assertThat(release.barcode()).isEqualTo("724385522912");
        assertThat(release.coverImageUrl()).isEqualTo("https://i.discogs.com/front.jpg");
        assertThat(release.coverThumbnailUrl()).isEqualTo("https://i.discogs.com/front-150.jpg");

        // Kopje "Side One" valt weg, de index-track "Suite" wordt vervangen door zijn deeltracks.
        assertThat(release.tracks()).containsExactly(
                new TrackDto(1, "A1", 1, "Airbag", 284, null),
                new TrackDto(1, "A2", 2, "Paranoid Android", 383, null),
                new TrackDto(1, "B1a", 3, "Subterranean Homesick Alien", 267, null),
                new TrackDto(1, "B1b", 4, "Exit Music (For A Film)", 264, null),
                new TrackDto(2, "C1", 5, "Karma Police", 261, "Radiohead feat. Thom Yorke"),
                new TrackDto(2, "D1", 6, "The Tourist", null, null));
    }

    @Test
    void getDetailsOfUnknownReleaseReturnsEmpty() {
        discogs.stubFor(get("/releases/999").willReturn(aResponse().withStatus(404)
                .withHeader("Content-Type", "application/json").withBody("{\"message\": \"Release not found.\"}")));

        assertThat(provider.getDetails("999")).isEmpty();
    }

    @Test
    void getDetailsWithNonNumericIdDoesNotCallDiscogs() {
        assertThat(provider.getDetails("../users/me")).isEqualTo(Optional.empty());
        assertThat(discogs.getAllServeEvents()).isEmpty();
    }

    @Test
    void serverErrorBecomesExternalLookupException() {
        discogs.stubFor(get("/releases/1").willReturn(aResponse().withStatus(503)));

        assertThatThrownBy(() -> provider.getDetails("1"))
                .isInstanceOf(ExternalLookupException.class)
                .extracting("source").isEqualTo("Discogs");
    }

    @Test
    void rateLimitBecomesExternalLookupException() {
        discogs.stubFor(get(urlPathEqualTo("/database/search")).willReturn(aResponse().withStatus(429)
                .withHeader("Content-Type", "application/json").withBody("{\"message\": \"You are making requests too quickly.\"}")));

        assertThatThrownBy(() -> provider.findByBarcode("724385522912"))
                .isInstanceOf(ExternalLookupException.class);
    }

    private static String fixture(String name) {
        return TestFixtures.read("discogs/" + name);
    }
}
