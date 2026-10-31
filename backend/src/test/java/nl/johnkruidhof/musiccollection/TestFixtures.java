package nl.johnkruidhof.musiccollection;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/** Leest JSON-fixtures (antwoorden van externe API's) uit {@code src/test/resources}. */
public final class TestFixtures {

    private TestFixtures() {
    }

    /** @param path pad vanaf de classpath-root, bv. {@code "discogs/release.json"} */
    public static String read(String path) {
        try (InputStream in = TestFixtures.class.getResourceAsStream("/" + path)) {
            if (in == null) {
                throw new IllegalArgumentException("Fixture niet gevonden: " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
