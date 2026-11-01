package nl.johnkruidhof.musiccollection.dto.collection;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import nl.johnkruidhof.musiccollection.dto.release.ReleaseDto;
import nl.johnkruidhof.musiccollection.entity.MediaCondition;

/** Eén item met de volledige release (incl. tracks); voor de detailpagina. */
public record CollectionItemDto(
        UUID id,
        ReleaseDto release,
        MediaCondition conditionMedia,
        MediaCondition conditionSleeve,
        LocalDate purchaseDate,
        BigDecimal purchasePrice,
        String purchaseLocation,
        String storageLocation,
        String notes,
        UUID genreId,
        String genreName,
        String subgenreName,
        Instant addedAt,
        Instant updatedAt) {
}
