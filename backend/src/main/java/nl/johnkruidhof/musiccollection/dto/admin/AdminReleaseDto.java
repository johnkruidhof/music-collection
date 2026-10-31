package nl.johnkruidhof.musiccollection.dto.admin;

import java.time.Instant;
import java.util.UUID;

import nl.johnkruidhof.musiccollection.entity.MediaType;
import nl.johnkruidhof.musiccollection.entity.MetadataSource;

/** Een release in de catalogus met het gebruik ervan (F-27, alleen voor beheerders). */
public record AdminReleaseDto(
        UUID id,
        String artist,
        String title,
        Integer releaseYear,
        MediaType mediaType,
        String formatDetails,
        String label,
        String catalogNumber,
        String country,
        String barcode,
        MetadataSource source,
        String discogsId,
        String musicbrainzId,
        String coverThumbnailUrl,
        long collectionCount,
        long wishlistCount,
        Instant createdAt) {
}
