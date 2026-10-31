package nl.johnkruidhof.musiccollection.client.discogs;

import java.util.List;

/** Antwoord van {@code GET /database/search}; alleen de velden die we gebruiken. */
public record DiscogsSearchResponse(List<DiscogsSearchResult> results) {
}
