package nl.johnkruidhof.musiccollection.dto.genre;

import java.util.List;
import java.util.UUID;

/** Genre met zijn subgenres (F-28). {@code itemCount} = collectie-items met precies dit genre. */
public record GenreDto(
        UUID id,
        String name,
        UUID parentId,
        long itemCount,
        List<GenreDto> subgenres) {
}
