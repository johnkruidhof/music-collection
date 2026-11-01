package nl.johnkruidhof.musiccollection.dto.genre;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Nieuw genre, of subgenre van {@code parentId}; bij hernoemen telt alleen {@code name}. */
public record GenreRequest(
        @NotBlank @Size(max = 100) String name,
        UUID parentId) {
}
