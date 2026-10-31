package nl.johnkruidhof.musiccollection.dto.collection;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import nl.johnkruidhof.musiccollection.entity.MediaCondition;

/** De persoonlijke velden van een collectie-item; bij wijzigen worden ze allemaal overschreven. */
public record CollectionItemFields(
        MediaCondition conditionMedia,
        MediaCondition conditionSleeve,
        @PastOrPresent LocalDate purchaseDate,
        @PositiveOrZero @Digits(integer = 8, fraction = 2) BigDecimal purchasePrice,
        @Size(max = 255) String purchaseLocation,
        @Size(max = 255) String storageLocation,
        @Size(max = 4000) String notes) {

    public static final CollectionItemFields EMPTY = new CollectionItemFields(null, null, null, null, null, null, null);
}
