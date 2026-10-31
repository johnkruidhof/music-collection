package nl.johnkruidhof.musiccollection.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Alle applicatie-specifieke instellingen onder het prefix {@code app}.
 * Waarden komen uit application.yml en kunnen overschreven worden met env-vars.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Cors cors, Discogs discogs, MusicBrainz musicbrainz) {

    public record Cors(List<String> allowedOrigins) {
        public Cors {
            allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
        }
    }

    /** Discogs API — https://www.discogs.com/developers */
    public record Discogs(String baseUrl, String token, String userAgent) {
    }

    /** MusicBrainz API — https://musicbrainz.org/doc/MusicBrainz_API */
    public record MusicBrainz(String baseUrl, String coverArtBaseUrl, String userAgent) {
    }
}
