package nl.johnkruidhof.musiccollection.service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import nl.johnkruidhof.musiccollection.dto.lookup.ReleaseCandidateDto;
import nl.johnkruidhof.musiccollection.dto.lookup.ReleaseSearchCriteria;
import nl.johnkruidhof.musiccollection.dto.release.ReleaseDto;
import nl.johnkruidhof.musiccollection.entity.MetadataSource;
import nl.johnkruidhof.musiccollection.entity.Release;
import nl.johnkruidhof.musiccollection.exception.InvalidRequestException;
import nl.johnkruidhof.musiccollection.exception.ResourceNotFoundException;
import nl.johnkruidhof.musiccollection.mapper.ReleaseMapper;
import nl.johnkruidhof.musiccollection.repository.ReleaseRepository;

/**
 * Lookup-strategie (CLAUDE.md §4.4): barcode normaliseren, eerst de lokale catalogus, dan de externe
 * bronnen via {@link ExternalLookupService} (gecachet). Handmatig ingevoerde releases worden nooit als
 * kandidaat aan andere gebruikers getoond.
 */
@Service
@RequiredArgsConstructor
public class LookupService {

    private final ReleaseRepository releaseRepository;
    private final ExternalLookupService externalLookupService;
    private final ReleaseMapper releaseMapper;

    @Transactional(readOnly = true)
    public List<ReleaseCandidateDto> lookupBarcode(String barcode) {
        String normalized = Barcodes.normalize(barcode);
        List<Release> local = releaseRepository.findByBarcodeInAndSourceNot(Barcodes.variants(normalized),
                MetadataSource.MANUAL);
        if (!local.isEmpty()) {
            return local.stream().map(releaseMapper::toCandidate).toList();
        }
        return externalLookupService.findByBarcode(normalized);
    }

    public List<ReleaseCandidateDto> search(ReleaseSearchCriteria criteria) {
        if (isBlank(criteria.artist()) && isBlank(criteria.title())) {
            throw new InvalidRequestException("Vul minstens een artiest of een titel in.");
        }
        return externalLookupService.search(new ReleaseSearchCriteria(
                strip(criteria.artist()), strip(criteria.title()), criteria.year(), criteria.mediaType()));
    }

    /** Uit de lokale catalogus als de release daar al staat (met id), anders van de externe bron. */
    @Transactional(readOnly = true)
    public ReleaseDto getDetails(String source, String externalId) {
        MetadataSource metadataSource = parseSource(source);
        return findLocal(metadataSource, externalId)
                .map(releaseMapper::toDto)
                .or(() -> externalLookupService.getDetails(metadataSource, externalId))
                .orElseThrow(() -> new ResourceNotFoundException("Release", source + "/" + externalId));
    }

    Optional<Release> findLocal(MetadataSource source, String externalId) {
        return switch (source) {
            case DISCOGS -> releaseRepository.findByDiscogsId(externalId);
            case MUSICBRAINZ -> releaseRepository.findByMusicbrainzId(externalId);
            case MANUAL -> Optional.empty();
        };
    }

    /** "discogs" / "musicbrainz" uit de URL; handmatige releases hebben geen externe bron. */
    public static MetadataSource parseSource(String source) {
        try {
            MetadataSource parsed = MetadataSource.valueOf(source.toUpperCase(Locale.ROOT));
            if (parsed != MetadataSource.MANUAL) {
                return parsed;
            }
        } catch (IllegalArgumentException | NullPointerException ex) {
            // valt door naar de foutmelding hieronder
        }
        throw new InvalidRequestException("Onbekende bron '" + source + "': gebruik discogs of musicbrainz.");
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String strip(String value) {
        return isBlank(value) ? null : value.strip();
    }
}
