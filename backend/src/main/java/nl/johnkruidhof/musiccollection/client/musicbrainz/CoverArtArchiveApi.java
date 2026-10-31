package nl.johnkruidhof.musiccollection.client.musicbrainz;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/** Cover Art Archive — https://musicbrainz.org/doc/Cover_Art_Archive/API. 404 = geen afbeeldingen. */
@HttpExchange(accept = "application/json")
public interface CoverArtArchiveApi {

    @GetExchange("/release/{mbid}")
    CoverArtArchiveResponse getRelease(@PathVariable String mbid);
}
