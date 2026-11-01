package nl.johnkruidhof.musiccollection.dto.collection;

import java.time.Instant;
import java.util.UUID;

import nl.johnkruidhof.musiccollection.entity.MediaCondition;
import nl.johnkruidhof.musiccollection.entity.MediaType;

/**
 * Compacte weergave voor het collectie-overzicht (geen tracks). {@code genreName} is het hoofdgenre,
 * {@code subgenreName} gevuld als het item een subgenre heeft (F-28).
 */
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
        UUID genreId,
        String genreName,
        String subgenreName,
        Instant addedAt) {
}
