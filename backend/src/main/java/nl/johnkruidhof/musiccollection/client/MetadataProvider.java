package nl.johnkruidhof.musiccollection.client;

import java.util.List;
import java.util.Optional;

import nl.johnkruidhof.musiccollection.dto.lookup.ReleaseCandidateDto;
import nl.johnkruidhof.musiccollection.dto.lookup.ReleaseSearchCriteria;
import nl.johnkruidhof.musiccollection.dto.release.ReleaseDto;
import nl.johnkruidhof.musiccollection.entity.MetadataSource;

/**
 * Gemeenschappelijke interface voor externe metadatabronnen (Discogs, MusicBrainz, ...).
 * De lookup-service (F-12) vraagt de providers in volgorde van prioriteit.
 */
public interface MetadataProvider {

    MetadataSource source();

    /** Kandidaten voor een (genormaliseerde) barcode; leeg als niets gevonden. */
    List<ReleaseCandidateDto> findByBarcode(String barcode);

    List<ReleaseCandidateDto> search(ReleaseSearchCriteria criteria);

    /** Volledige gegevens inclusief tracks; {@code id} in de DTO is nog leeg (niet opgeslagen). */
    Optional<ReleaseDto> getDetails(String externalId);
}
