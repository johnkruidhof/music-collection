/**
 * Discogs API-client (F-10). Gebruik een {@code @HttpExchange}-interface op een RestClient met
 * headers {@code Authorization: Discogs token=...} en een verplichte {@code User-Agent}.
 * Endpoints: {@code /database/search?barcode=...}, {@code /database/search?artist=&release_title=&year=&type=release},
 * {@code /releases/{id}}. Rate limit: 60 requests/minuut met token.
 */
package nl.johnkruidhof.musiccollection.client.discogs;
