package nl.johnkruidhof.musiccollection.dto.genre;

import java.util.UUID;

/** Genre van een item of wens zetten; {@code null} = geen genre. */
public record GenreAssignment(UUID genreId) {
}
