package nl.johnkruidhof.musiccollection.dto.admin;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/** De release in de URL gaat op in {@code targetId}: items en wensen verhuizen, daarna wordt hij verwijderd. */
public record MergeReleasesRequest(@NotNull UUID targetId) {
}
