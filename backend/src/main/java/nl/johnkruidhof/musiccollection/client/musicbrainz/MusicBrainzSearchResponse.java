package nl.johnkruidhof.musiccollection.client.musicbrainz;

import java.util.List;

/** Antwoord van {@code GET /release?query=...}; zoekresultaten hebben media zonder tracks. */
public record MusicBrainzSearchResponse(Integer count, List<MusicBrainzRelease> releases) {
}
