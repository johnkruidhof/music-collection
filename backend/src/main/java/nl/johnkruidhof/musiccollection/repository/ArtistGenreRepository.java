package nl.johnkruidhof.musiccollection.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import nl.johnkruidhof.musiccollection.entity.ArtistGenre;

public interface ArtistGenreRepository extends JpaRepository<ArtistGenre, UUID> {

    @EntityGraph(attributePaths = {"genre", "genre.parent"})
    Optional<ArtistGenre> findByUserIdAndArtistKey(UUID userId, String artistKey);

    @EntityGraph(attributePaths = "genre")
    List<ArtistGenre> findByUserId(UUID userId);
}
