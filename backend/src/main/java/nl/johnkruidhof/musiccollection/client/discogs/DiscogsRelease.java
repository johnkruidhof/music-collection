package nl.johnkruidhof.musiccollection.client.discogs;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Antwoord van {@code GET /releases/{id}}; alleen de velden die we gebruiken. */
public record DiscogsRelease(
        long id,
        String title,
        List<Artist> artists,
        Integer year,
        String country,
        List<Label> labels,
        List<Format> formats,
        List<Identifier> identifiers,
        List<Image> images,
        List<Track> tracklist) {

    /** {@code join} is het verbindingswoord naar de volgende artiest ("&", ",", "feat." ...). */
    public record Artist(String name, String anv, String join) {
    }

    public record Label(String name, String catno) {
    }

    /** {@code qty} is een string ("2"), {@code descriptions} bv. ["LP", "Album"]. */
    public record Format(String name, String qty, List<String> descriptions, String text) {
    }

    /** {@code type} is o.a. "Barcode", "Matrix / Runout". */
    public record Identifier(String type, String value) {
    }

    /** {@code type} is "primary" of "secondary"; {@code uri150} is de thumbnail. */
    public record Image(String type, String uri, String uri150) {
    }

    /** {@code type} is "track", "heading" of "index" (met {@code subTracks}). */
    public record Track(
            String position,
            @JsonProperty("type_") String type,
            String title,
            String duration,
            List<Artist> artists,
            @JsonProperty("sub_tracks") List<Track> subTracks) {
    }
}
