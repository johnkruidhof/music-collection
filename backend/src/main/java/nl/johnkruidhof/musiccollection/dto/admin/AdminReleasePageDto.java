package nl.johnkruidhof.musiccollection.dto.admin;

import java.util.List;

/** Eén pagina catalogus; {@code unusedCount} = releases zonder collectie-items en wensen (hele catalogus). */
public record AdminReleasePageDto(
        List<AdminReleaseDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        long unusedCount) {
}
