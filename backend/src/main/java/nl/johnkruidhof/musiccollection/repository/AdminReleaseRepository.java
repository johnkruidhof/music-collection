package nl.johnkruidhof.musiccollection.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import nl.johnkruidhof.musiccollection.dto.admin.AdminReleaseDto;
import nl.johnkruidhof.musiccollection.entity.MetadataSource;
import nl.johnkruidhof.musiccollection.entity.Release;

/** Catalogusbeheer over alle gebruikers heen (F-27); alleen bereikbaar via /api/admin (rol admin). */
public interface AdminReleaseRepository extends Repository<Release, UUID> {

    /** {@code pattern} is een LIKE-patroon in kleine letters (met escape '\'); {@code exact} voor barcode/id's. */
    @Query(value = """
            select new nl.johnkruidhof.musiccollection.dto.admin.AdminReleaseDto(
                r.id, r.artist, r.title, r.releaseYear, r.mediaType, r.formatDetails, r.label, r.catalogNumber,
                r.country, r.barcode, r.source, r.discogsId, r.musicbrainzId, r.coverThumbnailUrl,
                (select count(ci) from CollectionItem ci where ci.release = r),
                (select count(w) from WishlistItem w where w.release = r),
                r.createdAt)
            from Release r
            where r.source in :sources
              and (lower(r.artist) like :pattern escape '\\' or lower(r.title) like :pattern escape '\\'
                   or r.barcode = :exact or r.discogsId = :exact or r.musicbrainzId = :exact)""",
            countQuery = """
            select count(r) from Release r
            where r.source in :sources
              and (lower(r.artist) like :pattern escape '\\' or lower(r.title) like :pattern escape '\\'
                   or r.barcode = :exact or r.discogsId = :exact or r.musicbrainzId = :exact)""")
    Page<AdminReleaseDto> search(Collection<MetadataSource> sources, String pattern, String exact, Pageable pageable);

    @Query("""
            select new nl.johnkruidhof.musiccollection.dto.admin.AdminReleaseDto(
                r.id, r.artist, r.title, r.releaseYear, r.mediaType, r.formatDetails, r.label, r.catalogNumber,
                r.country, r.barcode, r.source, r.discogsId, r.musicbrainzId, r.coverThumbnailUrl,
                (select count(ci) from CollectionItem ci where ci.release = r),
                (select count(w) from WishlistItem w where w.release = r),
                r.createdAt)
            from Release r
            where r.id = :id""")
    Optional<AdminReleaseDto> findDtoById(UUID id);

    @Query("""
            select r.id from Release r
            where not exists (select 1 from CollectionItem ci where ci.release = r)
              and not exists (select 1 from WishlistItem w where w.release = r)""")
    List<UUID> findUnusedIds();

    @Query("select count(ci) from CollectionItem ci where ci.release.id = :releaseId")
    long countCollectionItems(UUID releaseId);

    @Query("select count(w) from WishlistItem w where w.release.id = :releaseId")
    long countWishlistItems(UUID releaseId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(nativeQuery = true, value = "update collection_item set release_id = :targetId where release_id = :sourceId")
    int moveCollectionItems(UUID sourceId, UUID targetId);

    /** Wensen die bij dezelfde gebruiker al op het doel staan, vervallen (uniek per gebruiker + release). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(nativeQuery = true, value = """
            delete from wishlist_item w
            where w.release_id = :sourceId
              and exists (select 1 from wishlist_item t where t.user_id = w.user_id and t.release_id = :targetId)""")
    int deleteDuplicateWishes(UUID sourceId, UUID targetId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(nativeQuery = true, value = "update wishlist_item set release_id = :targetId where release_id = :sourceId")
    int moveWishes(UUID sourceId, UUID targetId);
}
