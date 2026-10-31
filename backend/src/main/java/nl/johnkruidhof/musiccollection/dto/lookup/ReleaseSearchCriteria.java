package nl.johnkruidhof.musiccollection.dto.lookup;

import nl.johnkruidhof.musiccollection.entity.MediaType;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** Zoekcriteria als er geen barcode is. Minstens artiest of titel moet gevuld zijn (te valideren in de service). */
public record ReleaseSearchCriteria(
        String artist,
        String title,
        @Min(1900) @Max(2100) Integer year,
        MediaType mediaType) {
}
