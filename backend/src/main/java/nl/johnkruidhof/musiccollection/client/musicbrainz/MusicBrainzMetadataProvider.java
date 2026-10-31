package nl.johnkruidhof.musiccollection.client.musicbrainz;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import lombok.extern.slf4j.Slf4j;
import nl.johnkruidhof.musiccollection.client.MetadataProvider;
import nl.johnkruidhof.musiccollection.config.AppProperties;
import nl.johnkruidhof.musiccollection.dto.lookup.ReleaseCandidateDto;
import nl.johnkruidhof.musiccollection.dto.lookup.ReleaseSearchCriteria;
import nl.johnkruidhof.musiccollection.dto.release.ReleaseDto;
import nl.johnkruidhof.musiccollection.entity.MetadataSource;
import nl.johnkruidhof.musiccollection.exception.ExternalLookupException;
import nl.johnkruidhof.musiccollection.mapper.MusicBrainzMapper;

/**
 * MusicBrainz als {@link MetadataProvider} (fallback na Discogs), met covers uit het Cover Art Archive.
 * Fouten van MusicBrainz worden een {@link ExternalLookupException}; een onbekende release geeft
 * {@link Optional#empty()}. Een haperend Cover Art Archive laat de lookup niet mislukken.
 * Caching, rate limiting (1 req/s) en retry zitten in de lookup-service (F-12).
 */
@Slf4j
@Component
public class MusicBrainzMetadataProvider implements MetadataProvider {

    static final String SOURCE_NAME = "MusicBrainz";
    private static final int MAX_RESULTS = 25;
    private static final Pattern MBID = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

    private final MusicBrainzApi musicBrainzApi;
    private final CoverArtArchiveApi coverArtArchiveApi;
    private final MusicBrainzMapper musicBrainzMapper;
    private final String coverArtBaseUrl;

    public MusicBrainzMetadataProvider(MusicBrainzApi musicBrainzApi, CoverArtArchiveApi coverArtArchiveApi,
            MusicBrainzMapper musicBrainzMapper, AppProperties appProperties) {
        this.musicBrainzApi = musicBrainzApi;
        this.coverArtArchiveApi = coverArtArchiveApi;
        this.musicBrainzMapper = musicBrainzMapper;
        this.coverArtBaseUrl = appProperties.musicbrainz().coverArtBaseUrl();
    }

    @Override
    public MetadataSource source() {
        return MetadataSource.MUSICBRAINZ;
    }

    @Override
    public List<ReleaseCandidateDto> findByBarcode(String barcode) {
        if (barcode == null || !barcode.matches("\\d{6,14}")) {
            return List.of();
        }
        return searchReleases("zoeken op barcode", MusicBrainzQuery.barcode(barcode));
    }

    @Override
    public List<ReleaseCandidateDto> search(ReleaseSearchCriteria criteria) {
        String query = MusicBrainzQuery.release(blankToNull(criteria.artist()), blankToNull(criteria.title()),
                criteria.year(), criteria.mediaType());
        return query == null ? List.of() : searchReleases("zoeken", query);
    }

    @Override
    public Optional<ReleaseDto> getDetails(String externalId) {
        if (externalId == null || !MBID.matcher(externalId).matches()) {
            return Optional.empty();
        }
        MusicBrainzRelease release;
        try {
            release = call("release " + externalId + " ophalen", () -> musicBrainzApi.getRelease(
                    externalId, MusicBrainzApi.RELEASE_INCLUDES, MusicBrainzApi.FORMAT_JSON));
        } catch (ExternalLookupException ex) {
            if (ex.getCause() instanceof HttpClientErrorException.NotFound) {
                return Optional.empty();
            }
            throw ex;
        }
        if (release == null) {
            return Optional.empty();
        }
        return Optional.of(musicBrainzMapper.toReleaseDto(release, coverArt(release)));
    }

    private List<ReleaseCandidateDto> searchReleases(String action, String query) {
        MusicBrainzSearchResponse response = call(action,
                () -> musicBrainzApi.searchReleases(query, MAX_RESULTS, MusicBrainzApi.FORMAT_JSON));
        if (response == null || response.releases() == null) {
            return List.of();
        }
        return musicBrainzMapper.toCandidates(response.releases(), coverArtBaseUrl);
    }

    /**
     * Alleen een request naar het Cover Art Archive als MusicBrainz zegt dat er een voorkant is.
     * Mislukt dat, dan de vaste front-URL's (die redirecten naar de afbeelding).
     */
    private CoverArt coverArt(MusicBrainzRelease release) {
        if (release.coverArtArchive() == null || !release.coverArtArchive().front()) {
            return CoverArt.NONE;
        }
        String base = coverArtBaseUrl + "/release/" + release.id();
        CoverArt fallback = new CoverArt(base + "/front", base + "/front-250");
        try {
            CoverArtArchiveResponse response = coverArtArchiveApi.getRelease(release.id());
            if (response == null || response.images() == null) {
                return fallback;
            }
            return response.images().stream()
                    .filter(CoverArtArchiveResponse.Image::front)
                    .findFirst()
                    .map(image -> new CoverArt(https(image.image()), https(thumbnail(image))))
                    .filter(cover -> cover.imageUrl() != null)
                    .orElse(fallback);
        } catch (RestClientException ex) {
            log.debug("Cover Art Archive voor {} niet bereikbaar, vaste URL's gebruikt", release.id(), ex);
            return fallback;
        }
    }

    private static String thumbnail(CoverArtArchiveResponse.Image image) {
        if (image.thumbnails() == null) {
            return image.image();
        }
        return Objects.requireNonNullElse(image.thumbnails().getOrDefault("250", image.thumbnails().get("small")),
                image.image());
    }

    /** Het Cover Art Archive geeft soms http-URL's; die blokkeert de browser op onze https-pagina. */
    private static String https(String url) {
        return url != null && url.startsWith("http://") ? "https://" + url.substring("http://".length()) : url;
    }

    private <T> T call(String action, Supplier<T> request) {
        try {
            return request.get();
        } catch (RestClientException ex) {
            log.debug("MusicBrainz: {} mislukt", action, ex);
            throw new ExternalLookupException(SOURCE_NAME, "MusicBrainz: " + action + " mislukt: " + ex.getMessage(), ex);
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
