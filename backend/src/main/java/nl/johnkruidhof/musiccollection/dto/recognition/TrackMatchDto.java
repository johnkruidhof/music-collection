package nl.johnkruidhof.musiccollection.dto.recognition;

import java.util.UUID;

/** Welke track van een eigen item het herkende nummer is (F-32), bv. disc 1, positie "B3". */
public record TrackMatchDto(
        UUID itemId,
        int discNumber,
        String position,
        String title) {
}
