package nl.johnkruidhof.musiccollection.dto.release;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import nl.johnkruidhof.musiccollection.entity.MediaType;

/** Handmatig ingevoerde release (geen externe bron). Volgorde van {@code tracks} = volgorde op de release. */
public record ManualReleaseRequest(
        @Pattern(regexp = "[\\d\\s-]{0,32}", message = "alleen cijfers, spaties en streepjes") String barcode,
        @NotBlank @Size(max = 500) String artist,
        @NotBlank @Size(max = 500) String title,
        @Min(1900) @Max(2100) Integer releaseYear,
        @NotNull MediaType mediaType,
        @Size(max = 255) String formatDetails,
        @Size(max = 255) String label,
        @Size(max = 100) String catalogNumber,
        @Size(max = 100) String country,
        @Size(max = 500) List<@Valid @NotNull TrackDto> tracks) {
}
