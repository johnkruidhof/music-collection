package nl.johnkruidhof.musiccollection.dto.wishlist;

import java.time.Instant;
import java.util.UUID;

import nl.johnkruidhof.musiccollection.entity.MediaType;
import nl.johnkruidhof.musiccollection.entity.MetadataSource;

/** Een wens met de belangrijkste releasegegevens (F-24). */
public record WishlistItemDto(
        UUID id,
        UUID releaseId,
        String artist,
        String title,
        Integer releaseYear,
        MediaType mediaType,
        String formatDetails,
        String label,
        String country,
        String coverThumbnailUrl,
        MetadataSource source,
        String discogsId,
        String musicbrainzId,
        String notes,
        Instant addedAt) {
}
