package nl.johnkruidhof.musiccollection.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import nl.johnkruidhof.musiccollection.entity.CollectionItem;

/**
 * Alle queries zijn gescoped op de eigenaar, zodat gebruikers nooit elkaars items zien.
 * Specificaties voor het overzicht staan in {@link CollectionItemSpecifications} (altijd met {@code ownedBy}).
 */
public interface CollectionItemRepository extends JpaRepository<CollectionItem, UUID>,
        JpaSpecificationExecutor<CollectionItem> {

    @Override
    @EntityGraph(attributePaths = "release")
    Page<CollectionItem> findAll(Specification<CollectionItem> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"release", "release.tracks"})
    Optional<CollectionItem> findByIdAndUserId(UUID id, UUID userId);

    long countByUserId(UUID userId);

    long countByReleaseId(UUID releaseId);
}
