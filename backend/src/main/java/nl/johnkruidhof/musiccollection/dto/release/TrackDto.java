package nl.johnkruidhof.musiccollection.dto.release;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record TrackDto(
        int discNumber,
        String position,
        int sortOrder,
        @NotBlank String title,
        @PositiveOrZero Integer durationSeconds,
        String artist) {
}
