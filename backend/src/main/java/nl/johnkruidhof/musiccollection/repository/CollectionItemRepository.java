package nl.johnkruidhof.musiccollection.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

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

    long countByUserIdAndReleaseId(UUID userId, UUID releaseId);

    /**
     * Eigen items bij een herkend nummer (F-30): artiest bevat {@code artist} en een track of de releasetitel
     * bevat {@code title} / {@code album}. Patronen in kleine letters, LIKE-escape '\\'.
     */
    @EntityGraph(attributePaths = "release")
    @Query("""
            select distinct ci from CollectionItem ci join ci.release r left join r.tracks t
            where ci.user.id = :userId
              and lower(r.artist) like :artist escape '\\'
              and (lower(t.title) like :title escape '\\' or lower(r.title) like :album escape '\\'
                   or lower(r.title) like :title escape '\\')""")
    List<CollectionItem> findByRecognizedTrack(UUID userId, String artist, String title, String album);

    /** Aantal exemplaren per extern id (F-23). */
    interface ExternalIdCount {
        String getExternalId();

        long getCount();
    }

    @Query("""
            select r.discogsId as externalId, count(ci) as count
            from CollectionItem ci join ci.release r
            where ci.user.id = :userId and r.discogsId in :discogsIds
            group by r.discogsId""")
    List<ExternalIdCount> countOwnedByDiscogsIds(UUID userId, Collection<String> discogsIds);

    @Query("""
            select r.musicbrainzId as externalId, count(ci) as count
            from CollectionItem ci join ci.release r
            where ci.user.id = :userId and r.musicbrainzId in :musicbrainzIds
            group by r.musicbrainzId""")
    List<ExternalIdCount> countOwnedByMusicbrainzIds(UUID userId, Collection<String> musicbrainzIds);
}
