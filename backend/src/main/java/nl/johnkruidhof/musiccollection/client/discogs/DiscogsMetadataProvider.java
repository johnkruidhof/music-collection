package nl.johnkruidhof.musiccollection.client.discogs;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.johnkruidhof.musiccollection.client.MetadataProvider;
import nl.johnkruidhof.musiccollection.dto.lookup.ReleaseCandidateDto;
import nl.johnkruidhof.musiccollection.dto.lookup.ReleaseSearchCriteria;
import nl.johnkruidhof.musiccollection.dto.release.ReleaseDto;
import nl.johnkruidhof.musiccollection.entity.MetadataSource;
import nl.johnkruidhof.musiccollection.exception.ExternalLookupException;
import nl.johnkruidhof.musiccollection.mapper.DiscogsMapper;

/**
 * Discogs als {@link MetadataProvider}. Fouten van Discogs (5xx, 429, time-outs ...) worden een
 * {@link ExternalLookupException}; een onbekende release geeft {@link Optional#empty()}.
 * Caching, rate limiting en retry zitten in de lookup-service (F-12).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DiscogsMetadataProvider implements MetadataProvider {

    static final String SOURCE_NAME = "Discogs";
    private static final String TYPE_RELEASE = "release";
    private static final int MAX_RESULTS = 25;

    private final DiscogsApi discogsApi;
    private final DiscogsMapper discogsMapper;

    @Override
    public MetadataSource source() {
        return MetadataSource.DISCOGS;
    }

    @Override
    public List<ReleaseCandidateDto> findByBarcode(String barcode) {
        DiscogsSearchResponse response = call("zoeken op barcode",
                () -> discogsApi.searchByBarcode(barcode, TYPE_RELEASE, MAX_RESULTS));
        return toCandidates(response);
    }

    @Override
    public List<ReleaseCandidateDto> search(ReleaseSearchCriteria criteria) {
        String artist = blankToNull(criteria.artist());
        String title = blankToNull(criteria.title());
        if (artist == null && title == null) {
            return List.of();
        }
        String format = discogsMapper.toDiscogsFormat(criteria.mediaType());
        DiscogsSearchResponse response = call("zoeken",
                () -> discogsApi.search(artist, title, criteria.year(), format, TYPE_RELEASE, MAX_RESULTS));
        return toCandidates(response);
    }

    @Override
    public Optional<ReleaseDto> getDetails(String externalId) {
        if (externalId == null || !externalId.matches("\\d{1,12}")) {
            return Optional.empty();
        }
        try {
            DiscogsRelease release = call("release " + externalId + " ophalen",
                    () -> discogsApi.getRelease(externalId));
            return Optional.ofNullable(release).map(discogsMapper::toReleaseDto);
        } catch (ExternalLookupException ex) {
            if (ex.getCause() instanceof HttpClientErrorException.NotFound) {
                return Optional.empty();
            }
            throw ex;
        }
    }

    private List<ReleaseCandidateDto> toCandidates(DiscogsSearchResponse response) {
        if (response == null || response.results() == null) {
            return List.of();
        }
        return discogsMapper.toCandidates(response.results());
    }

    private <T> T call(String action, Supplier<T> request) {
        try {
            return request.get();
        } catch (RestClientException ex) {
            log.debug("Discogs: {} mislukt", action, ex);
            throw new ExternalLookupException(SOURCE_NAME, "Discogs: " + action + " mislukt: " + ex.getMessage(), ex);
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
