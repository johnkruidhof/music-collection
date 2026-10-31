package nl.johnkruidhof.musiccollection.client.musicbrainz;

import java.util.List;
import java.util.Map;

/** Antwoord van het Cover Art Archive; {@code thumbnails} heeft o.a. de sleutels "250", "500", "small". */
public record CoverArtArchiveResponse(List<Image> images) {

    public record Image(boolean front, String image, Map<String, String> thumbnails) {
    }
}
