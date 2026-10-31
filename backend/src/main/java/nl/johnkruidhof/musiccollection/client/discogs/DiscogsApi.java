package nl.johnkruidhof.musiccollection.client.discogs;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * Discogs Database API — https://www.discogs.com/developers.
 * Authenticatie ({@code Authorization: Discogs token=...}) en {@code User-Agent} zet {@code DiscogsClientConfig}.
 * Parameters die {@code null} zijn worden weggelaten.
 */
@HttpExchange(accept = "application/json")
public interface DiscogsApi {

    @GetExchange("/database/search")
    DiscogsSearchResponse searchByBarcode(
            @RequestParam String barcode,
            @RequestParam String type,
            @RequestParam("per_page") int perPage);

    @GetExchange("/database/search")
    DiscogsSearchResponse search(
            @RequestParam(required = false) String artist,
            @RequestParam(name = "release_title", required = false) String releaseTitle,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String format,
            @RequestParam String type,
            @RequestParam("per_page") int perPage);

    @GetExchange("/releases/{id}")
    DiscogsRelease getRelease(@PathVariable String id);
}
