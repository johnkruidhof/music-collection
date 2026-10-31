package nl.johnkruidhof.musiccollection.client.discogs;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Eén zoekresultaat. {@code title} is "Artiest - Titel", {@code year} een string,
 * {@code format} een lijst als ["Vinyl", "LP", "Album"].
 */
public record DiscogsSearchResult(
        long id,
        String type,
        String title,
        String year,
        String country,
        List<String> format,
        List<String> label,
        String catno,
        List<String> barcode,
        String thumb,
        @JsonProperty("cover_image") String coverImage) {
}
