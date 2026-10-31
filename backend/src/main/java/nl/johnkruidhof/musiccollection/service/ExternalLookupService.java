package nl.johnkruidhof.musiccollection.service;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import nl.johnkruidhof.musiccollection.client.MetadataProvider;
import nl.johnkruidhof.musiccollection.config.CacheConfig;
import nl.johnkruidhof.musiccollection.dto.lookup.ReleaseCandidateDto;
import nl.johnkruidhof.musiccollection.dto.lookup.ReleaseSearchCriteria;
import nl.johnkruidhof.musiccollection.dto.release.ReleaseDto;
import nl.johnkruidhof.musiccollection.entity.MetadataSource;
import nl.johnkruidhof.musiccollection.exception.ExternalLookupException;

/**
 * Vraagt de externe bronnen in volgorde van prioriteit (Discogs, dan MusicBrainz) en cachet de
 * resultaten. Faalt een bron, dan wordt de volgende geprobeerd; levert geen enkele bron iets op en
 * is er minstens één gefaald, dan volgt een {@link ExternalLookupException}. Zo wordt een leeg
 * resultaat dat door een storing komt nooit gecachet.
 */
@Slf4j
@Service
public class ExternalLookupService {

    private final List<MetadataProvider> providers;

    /** Spring levert de providers gesorteerd op {@code @Order}. */
    public ExternalLookupService(List<MetadataProvider> providers) {
        this.providers = List.copyOf(providers);
    }

    /** @param barcode genormaliseerd ({@link Barcodes#normalize}); ook de UPC-A/EAN-13-variant wordt geprobeerd */
    @Cacheable(CacheConfig.BARCODE_LOOKUP_CACHE)
    public List<ReleaseCandidateDto> findByBarcode(String barcode) {
        List<String> variants = Barcodes.variants(barcode);
        return firstNonEmpty("barcode " + barcode, provider -> {
            for (String variant : variants) {
                List<ReleaseCandidateDto> candidates = provider.findByBarcode(variant);
                if (!candidates.isEmpty()) {
                    return candidates;
                }
            }
            return List.of();
        });
    }

    @Cacheable(CacheConfig.SEARCH_CACHE)
    public List<ReleaseCandidateDto> search(ReleaseSearchCriteria criteria) {
        return firstNonEmpty("zoekopdracht " + criteria, provider -> provider.search(criteria));
    }

    @Cacheable(cacheNames = CacheConfig.RELEASE_DETAILS_CACHE, key = "#source.name() + ':' + #externalId")
    public Optional<ReleaseDto> getDetails(MetadataSource source, String externalId) {
        return providers.stream()
                .filter(provider -> provider.source() == source)
                .findFirst()
                .flatMap(provider -> provider.getDetails(externalId));
    }

    private List<ReleaseCandidateDto> firstNonEmpty(String description,
            Function<MetadataProvider, List<ReleaseCandidateDto>> lookup) {
        ExternalLookupException failure = null;
        for (MetadataProvider provider : providers) {
            try {
                List<ReleaseCandidateDto> candidates = lookup.apply(provider);
                if (!candidates.isEmpty()) {
                    return candidates;
                }
            } catch (ExternalLookupException ex) {
                log.warn("{} faalde voor {}, volgende bron proberen: {}", provider.source(), description, ex.getMessage());
                failure = ex;
            }
        }
        if (failure != null) {
            throw failure;
        }
        return List.of();
    }
}
