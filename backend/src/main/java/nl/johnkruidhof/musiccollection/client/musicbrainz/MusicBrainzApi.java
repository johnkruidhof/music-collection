package nl.johnkruidhof.musiccollection.client.musicbrainz;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * MusicBrainz Web Service v2 — https://musicbrainz.org/doc/MusicBrainz_API.
 * Geen token, wel een verplichte User-Agent; maximaal 1 request per seconde (anders 503).
 */
@HttpExchange(accept = "application/json")
public interface MusicBrainzApi {

    String FORMAT_JSON = "json";
    /** Nodig voor artiesten, labels en tracks in {@link #getRelease}. */
    String RELEASE_INCLUDES = "artist-credits labels recordings";

    /** Lucene-zoekopdracht op releases, zie {@link MusicBrainzQuery}. */
    @GetExchange("/release")
    MusicBrainzSearchResponse searchReleases(
            @RequestParam String query,
            @RequestParam int limit,
            @RequestParam String fmt);

    @GetExchange("/release/{mbid}")
    MusicBrainzRelease getRelease(
            @PathVariable String mbid,
            @RequestParam String inc,
            @RequestParam String fmt);
}
