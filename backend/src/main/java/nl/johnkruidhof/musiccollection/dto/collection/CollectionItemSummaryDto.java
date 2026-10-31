package nl.johnkruidhof.musiccollection.dto.collection;

import java.time.Instant;
import java.util.UUID;

import nl.johnkruidhof.musiccollection.entity.MediaCondition;
import nl.johnkruidhof.musiccollection.entity.MediaType;

/** Compacte weergave voor het collectie-overzicht (geen tracks). */
public record CollectionItemSummaryDto(
        UUID id,
        UUID releaseId,
        String artist,
        String title,
        Integer releaseYear,
        MediaType mediaType,
        String formatDetails,
        String coverThumbnailUrl,
        MediaCondition conditionMedia,
        String storageLocation,
        Instant addedAt) {
}
