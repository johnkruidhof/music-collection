package nl.johnkruidhof.musiccollection.repository;

import java.util.Locale;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import nl.johnkruidhof.musiccollection.entity.CollectionItem;
import nl.johnkruidhof.musiccollection.entity.MediaType;

/** Filters voor het collectie-overzicht. */
public final class CollectionItemSpecifications {

    private static final char ESCAPE = '\\';

    private CollectionItemSpecifications() {
    }

    public static Specification<CollectionItem> ownedBy(UUID userId) {
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }

    /** {@code null} = alle types. */
    public static Specification<CollectionItem> hasMediaType(MediaType mediaType) {
        return (root, query, cb) -> mediaType == null ? null : cb.equal(root.get("release").get("mediaType"), mediaType);
    }

    /** Hoofdletterongevoelig "bevat" op artiest of titel; leeg = geen filter. */
    public static Specification<CollectionItem> matchesText(String text) {
        return (root, query, cb) -> {
            if (text == null || text.isBlank()) {
                return null;
            }
            String pattern = "%" + escapeLike(text.strip().toLowerCase(Locale.ROOT)) + "%";
            var release = root.get("release");
            return cb.or(
                    cb.like(cb.lower(release.get("artist")), pattern, ESCAPE),
                    cb.like(cb.lower(release.get("title")), pattern, ESCAPE));
        };
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
