package nl.johnkruidhof.musiccollection;

import org.springframework.jdbc.core.JdbcTemplate;

/** Leegt alle applicatietabellen tussen integratietests (Flyway-historie blijft staan). */
public final class TestDatabase {

    private TestDatabase() {
    }

    public static void clean(JdbcTemplate jdbc) {
        jdbc.execute("TRUNCATE artist_genre, wishlist_item, collection_item, genre, cover_image, track, release, app_user CASCADE");
    }
}
