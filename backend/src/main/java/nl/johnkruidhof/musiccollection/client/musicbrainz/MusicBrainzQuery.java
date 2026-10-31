package nl.johnkruidhof.musiccollection.client.musicbrainz;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import nl.johnkruidhof.musiccollection.entity.MediaType;

/**
 * Bouwt Lucene-zoekopdrachten voor de MusicBrainz release-zoek-API. Het {@code format}-veld zoekt
 * op de exacte formaatnaam ("Vinyl" vindt geen "12\" Vinyl"), daarom per type een lijst namen.
 */
final class MusicBrainzQuery {

    private MusicBrainzQuery() {
    }

    static String barcode(String barcode) {
        return "barcode:" + barcode;
    }

    /** {@code null} als er niets is om op te zoeken (geen artiest en geen titel). */
    static String release(String artist, String title, Integer year, MediaType mediaType) {
        if (artist == null && title == null) {
            return null;
        }
        List<String> terms = new ArrayList<>();
        if (artist != null) {
            terms.add("artist:" + phrase(artist));
        }
        if (title != null) {
            terms.add("release:" + phrase(title));
        }
        if (year != null) {
            terms.add("date:" + year);
        }
        List<String> formats = formatNames(mediaType);
        if (!formats.isEmpty()) {
            terms.add("format:(" + formats.stream().map(MusicBrainzQuery::phrase)
                    .collect(Collectors.joining(" OR ")) + ")");
        }
        return String.join(" AND ", terms);
    }

    static List<String> formatNames(MediaType mediaType) {
        if (mediaType == null) {
            return List.of();
        }
        return switch (mediaType) {
            case VINYL -> List.of("Vinyl", "12\" Vinyl", "10\" Vinyl", "7\" Vinyl");
            case CD -> List.of("CD", "Enhanced CD", "Copy Control CD", "HDCD", "SACD", "Hybrid SACD",
                    "SHM-CD", "Blu-spec CD", "CD-R");
            case DVD -> List.of("DVD", "DVD-Video", "DVD-Audio");
            case BLU_RAY -> List.of("Blu-ray");
            case CASSETTE -> List.of("Cassette");
            case OTHER -> List.of();
        };
    }

    /** Tussen aanhalingstekens, zodat Lucene-tekens in namen ("AC/DC", "Guns N' Roses") geen syntax zijn. */
    private static String phrase(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
