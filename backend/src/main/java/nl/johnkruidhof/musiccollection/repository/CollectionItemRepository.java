package nl.johnkruidhof.musiccollection.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import nl.johnkruidhof.musiccollection.entity.CollectionItem;

/** Alle queries zijn gescoped op de eigenaar, zodat gebruikers nooit elkaars items zien. */
public interface CollectionItemRepository extends JpaRepository<CollectionItem, UUID> {

    @EntityGraph(attributePaths = "release")
    Page<CollectionItem> findByUserId(UUID userId, Pageable pageable);

    @EntityGraph(attributePaths = {"release", "release.tracks"})
    Optional<CollectionItem> findByIdAndUserId(UUID id, UUID userId);

    long countByUserId(UUID userId);
}
