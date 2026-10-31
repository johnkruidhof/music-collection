package nl.johnkruidhof.musiccollection.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import nl.johnkruidhof.musiccollection.entity.WishlistItem;

/** Altijd gescoped op de eigenaar. */
public interface WishlistItemRepository extends JpaRepository<WishlistItem, UUID> {

    @EntityGraph(attributePaths = "release")
    List<WishlistItem> findByUserIdOrderByAddedAtDesc(UUID userId);

    @EntityGraph(attributePaths = "release")
    Optional<WishlistItem> findByIdAndUserId(UUID id, UUID userId);

    @EntityGraph(attributePaths = "release")
    Optional<WishlistItem> findByUserIdAndReleaseId(UUID userId, UUID releaseId);

    long countByReleaseId(UUID releaseId);

    @Query("""
            select r.discogsId from WishlistItem w join w.release r
            where w.user.id = :userId and r.discogsId in :discogsIds""")
    List<String> findWishedDiscogsIds(UUID userId, Collection<String> discogsIds);

    @Query("""
            select r.musicbrainzId from WishlistItem w join w.release r
            where w.user.id = :userId and r.musicbrainzId in :musicbrainzIds""")
    List<String> findWishedMusicbrainzIds(UUID userId, Collection<String> musicbrainzIds);
}
