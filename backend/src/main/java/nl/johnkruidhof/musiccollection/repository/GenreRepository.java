package nl.johnkruidhof.musiccollection.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import nl.johnkruidhof.musiccollection.entity.Genre;

/** Altijd gescoped op de eigenaar. */
public interface GenreRepository extends JpaRepository<Genre, UUID> {

    List<Genre> findByUserIdOrderByPath(UUID userId);

    Optional<Genre> findByIdAndUserId(UUID id, UUID userId);

    Optional<Genre> findByUserIdAndPath(UUID userId, String path);

    List<Genre> findByParentId(UUID parentId);

    /** Aantal collectie-items per genre-id (alleen directe toekenning). */
    interface GenreCount {
        UUID getGenreId();

        long getCount();
    }

    @Query("""
            select ci.genre.id as genreId, count(ci) as count from CollectionItem ci
            where ci.user.id = :userId and ci.genre is not null group by ci.genre.id""")
    List<GenreCount> countItemsPerGenre(UUID userId);
}
