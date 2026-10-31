package nl.johnkruidhof.musiccollection.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Alle applicatie-specifieke instellingen onder het prefix {@code app}.
 * Waarden komen uit application.yml en kunnen overschreven worden met env-vars.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Cors cors, Discogs discogs, MusicBrainz musicbrainz, Covers covers, Audd audd) {

    public AppProperties {
        covers = covers == null ? Covers.DEFAULT : covers;
        audd = audd == null ? new Audd(null, null) : audd;
    }

    public record Cors(List<String> allowedOrigins) {
        public Cors {
            allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
        }
    }

    /** Discogs API — https://www.discogs.com/developers */
    public record Discogs(String baseUrl, String token, String userAgent, Throttle throttle) {
        public Discogs {
            throttle = throttle == null ? Throttle.DEFAULT : throttle;
        }
    }

    /** MusicBrainz API — https://musicbrainz.org/doc/MusicBrainz_API */
    public record MusicBrainz(String baseUrl, String coverArtBaseUrl, String userAgent, Throttle throttle) {
        public MusicBrainz {
            throttle = throttle == null ? Throttle.DEFAULT : throttle;
        }
    }

    /**
     * Rate limiting en retry per externe bron.
     *
     * @param minInterval  minimale tijd tussen twee requests (Discogs 60/min → 1 s, MusicBrainz 1/s)
     * @param maxRetries   aantal extra pogingen bij 429/502/503/504 of netwerkfouten
     * @param retryBackoff wachttijd vóór de eerste extra poging; verdubbelt per poging
     * @param maxWait      langer in de wachtrij → direct een fout i.p.v. de gebruiker laten wachten
     */
    public record Throttle(Duration minInterval, int maxRetries, Duration retryBackoff, Duration maxWait) {

        public static final Throttle DEFAULT = new Throttle(null, 2, null, null);
        public static final Throttle NONE = new Throttle(Duration.ZERO, 0, Duration.ZERO, Duration.ZERO);

        public Throttle {
            minInterval = minInterval == null ? Duration.ofSeconds(1) : minInterval;
            retryBackoff = retryBackoff == null ? Duration.ofSeconds(1) : retryBackoff;
            maxWait = maxWait == null ? Duration.ofSeconds(20) : maxWait;
            maxRetries = Math.max(0, maxRetries);
        }
    }

    /**
     * AudD-muziekherkenning (F-30) — https://docs.audd.io. Zonder {@code apiToken} staat luisteren uit.
     */
    public record Audd(String baseUrl, String apiToken) {

        public Audd {
            baseUrl = baseUrl == null || baseUrl.isBlank() ? "https://api.audd.io" : baseUrl;
        }

        public boolean enabled() {
            return apiToken != null && !apiToken.isBlank();
        }
    }

    /**
     * Covers van externe bronnen lokaal cachen (F-21).
     *
     * @param cacheEnabled  na import en in de periodieke backfill automatisch downloaden
     * @param maxAttempts   na zoveel mislukte downloads wordt een release overgeslagen
     * @param batchSize     aantal releases per backfill-ronde
     * @param minInterval   minimale tijd tussen twee downloads (vriendelijk voor de image-CDN's)
     */
    public record Covers(boolean cacheEnabled, int maxAttempts, int batchSize, Duration minInterval) {

        public static final Covers DEFAULT = new Covers(true, 3, 25, null);

        public Covers {
            maxAttempts = maxAttempts <= 0 ? 3 : maxAttempts;
            batchSize = batchSize <= 0 ? 25 : batchSize;
            minInterval = minInterval == null ? Duration.ofMillis(500) : minInterval;
        }
    }
}
