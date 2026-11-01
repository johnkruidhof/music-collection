package nl.johnkruidhof.musiccollection.service;

import java.text.Normalizer;
import java.util.Locale;

/** Artiestnamen vergelijkbaar maken (F-28), o.a. voor het genre-voorstel op basis van de artiest. */
public final class Artists {

    private Artists() {
    }

    /**
     * "Cinematic Orchestra, The" en "The Cinematic Orchestra" → "cinematic orchestra"; "Hüsker Dü" →
     * "husker du"; "Simon & Garfunkel" → "simon and garfunkel"; leestekens en dubbele spaties weg.
     */
    public static String key(String artist) {
        if (artist == null) {
            return "";
        }
        String s = Normalizer.normalize(artist, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).strip();
        s = s.replaceAll(",\\s*the$", "").replaceAll("^the\\s+", "");
        s = s.replace("&", " and ").replaceAll("[^\\p{L}\\p{N}]+", " ");
        return s.strip().replaceAll("\\s+", " ");
    }
}
