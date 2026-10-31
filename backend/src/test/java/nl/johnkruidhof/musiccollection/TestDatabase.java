package nl.johnkruidhof.musiccollection;

import org.springframework.jdbc.core.JdbcTemplate;

/** Leegt alle applicatietabellen tussen integratietests (Flyway-historie blijft staan). */
public final class TestDatabase {

    private TestDatabase() {
    }

    public static void clean(JdbcTemplate jdbc) {
        jdbc.execute("TRUNCATE wishlist_item, collection_item, cover_image, track, release, app_user CASCADE");
    }
}
