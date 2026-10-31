package nl.johnkruidhof.musiccollection.dto.collection;

import java.util.List;

/** Eén pagina van de collectie; {@code page} begint bij 0. */
public record CollectionPageDto(
        List<CollectionItemSummaryDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
