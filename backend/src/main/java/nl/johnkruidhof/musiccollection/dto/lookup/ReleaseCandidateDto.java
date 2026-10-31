package nl.johnkruidhof.musiccollection.dto.lookup;

import nl.johnkruidhof.musiccollection.entity.MediaType;
import nl.johnkruidhof.musiccollection.entity.MetadataSource;

/**
 * Een zoekresultaat van een externe bron (of de lokale catalogus) waaruit de gebruiker kiest.
 * Bevat nog geen tracks; die worden pas opgehaald bij {@code GET /api/lookup/{source}/{externalId}}.
 * {@code ownedCount} (F-23) en {@code onWishlist} (F-24) zijn persoonlijk; {@code null} in de (gedeelde)
 * cache, ingevuld door de lookup-service.
 */
public record ReleaseCandidateDto(
        MetadataSource source,
        String externalId,
        String artist,
        String title,
        Integer releaseYear,
        MediaType mediaType,
        String formatDetails,
        String label,
        String catalogNumber,
        String country,
        String barcode,
        String coverThumbnailUrl,
        Integer ownedCount,
        Boolean onWishlist) {
}
