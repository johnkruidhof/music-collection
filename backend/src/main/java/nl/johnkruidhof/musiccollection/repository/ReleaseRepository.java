package nl.johnkruidhof.musiccollection.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import nl.johnkruidhof.musiccollection.entity.MetadataSource;
import nl.johnkruidhof.musiccollection.entity.Release;

public interface ReleaseRepository extends JpaRepository<Release, UUID> {

    List<Release> findByBarcode(String barcode);

    List<Release> findByBarcodeInAndSourceNot(Collection<String> barcodes, MetadataSource source);

    Optional<Release> findByDiscogsId(String discogsId);

    /** Releases van externe bronnen waarvan de cover nog niet lokaal staat (F-21). */
    @Query("""
            select r.id from Release r
            where r.source <> nl.johnkruidhof.musiccollection.entity.MetadataSource.MANUAL
              and r.coverImageUrl is not null
              and r.coverImageUrl not like '/api/covers/%'
              and r.coverCacheAttempts < :maxAttempts
            order by r.createdAt""")
    List<UUID> findIdsWithUncachedCover(int maxAttempts, Pageable pageable);

    Optional<Release> findByMusicbrainzId(String musicbrainzId);
}
