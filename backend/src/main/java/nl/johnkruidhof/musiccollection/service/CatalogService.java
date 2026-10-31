package nl.johnkruidhof.musiccollection.service;

import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.johnkruidhof.musiccollection.dto.release.ManualReleaseRequest;
import nl.johnkruidhof.musiccollection.dto.release.ReleaseDto;
import nl.johnkruidhof.musiccollection.entity.MetadataSource;
import nl.johnkruidhof.musiccollection.entity.Release;
import nl.johnkruidhof.musiccollection.exception.InvalidRequestException;
import nl.johnkruidhof.musiccollection.exception.ResourceNotFoundException;
import nl.johnkruidhof.musiccollection.mapper.ReleaseMapper;
import nl.johnkruidhof.musiccollection.repository.ReleaseRepository;

/** Beheer van de gedeelde catalogus: releases van externe bronnen eenmalig importeren, handmatige aanmaken. */
@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogService {

    private final ReleaseRepository releaseRepository;
    private final LookupService lookupService;
    private final ExternalLookupService externalLookupService;
    private final ReleaseMapper releaseMapper;

    /**
     * Id van de release in de catalogus; de eerste keer opgehaald bij de bron en opgeslagen.
     * <p>
     * Bewust <b>zonder</b> omringende transactie aanroepen: de externe aanroep kan door rate limiting
     * even duren en mag geen databaseverbinding vasthouden. Het opslaan is een eigen korte transactie
     * (repository); importeren twee gebruikers tegelijk dezelfde release, dan wint er één (unieke index)
     * en leest de ander die daarna terug.
     */
    public UUID getOrImport(MetadataSource source, String externalId) {
        if (source == MetadataSource.MANUAL) {
            throw new InvalidRequestException("Een handmatige release heeft geen externe bron.");
        }
        return lookupService.findLocal(source, externalId)
                .map(Release::getId)
                .orElseGet(() -> importRelease(source, externalId));
    }

    @Transactional
    public Release createManual(ManualReleaseRequest request) {
        Release release = releaseMapper.toEntity(request);
        if (release.getBarcode() != null) {
            String digits = release.getBarcode().replaceAll("[\\s-]", "");
            release.setBarcode(digits.isEmpty() ? null : digits);
        }
        return releaseRepository.save(release);
    }

    private UUID importRelease(MetadataSource source, String externalId) {
        ReleaseDto details = externalLookupService.getDetails(source, externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Release", source.name().toLowerCase() + "/" + externalId));
        try {
            Release saved = releaseRepository.save(releaseMapper.toEntity(details));
            log.info("Release {} geïmporteerd uit {}: {} - {}", externalId, source, saved.getArtist(), saved.getTitle());
            return saved.getId();
        } catch (DataIntegrityViolationException ex) {
            log.info("Release {} uit {} werd tegelijk geïmporteerd; bestaande gebruikt", externalId, source);
            return lookupService.findLocal(source, externalId).map(Release::getId).orElseThrow(() -> ex);
        }
    }
}
