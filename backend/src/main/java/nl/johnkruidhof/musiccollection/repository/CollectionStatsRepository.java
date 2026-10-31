package nl.johnkruidhof.musiccollection.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import nl.johnkruidhof.musiccollection.dto.stats.CollectionStatsDto;
import nl.johnkruidhof.musiccollection.entity.CollectionItem;

/** Aggregaties over de collectie van één gebruiker (altijd gescoped op {@code userId}). */
public interface CollectionStatsRepository extends Repository<CollectionItem, UUID> {

    @Query("select count(ci) from CollectionItem ci where ci.user.id = :userId")
    long countItems(UUID userId);

    @Query("select coalesce(sum(ci.purchasePrice), 0) from CollectionItem ci where ci.user.id = :userId")
    BigDecimal totalValue(UUID userId);

    @Query("select count(ci) from CollectionItem ci where ci.user.id = :userId and ci.purchasePrice is not null")
    long countItemsWithPrice(UUID userId);

    @Query("""
            select new nl.johnkruidhof.musiccollection.dto.stats.CollectionStatsDto$MediaTypeCount(r.mediaType, count(ci))
            from CollectionItem ci join ci.release r
            where ci.user.id = :userId
            group by r.mediaType
            order by count(ci) desc, r.mediaType""")
    List<CollectionStatsDto.MediaTypeCount> countByMediaType(UUID userId);

    @Query("""
            select new nl.johnkruidhof.musiccollection.dto.stats.CollectionStatsDto$DecadeCount(
                cast((r.releaseYear / 10) * 10 as Integer), count(ci))
            from CollectionItem ci join ci.release r
            where ci.user.id = :userId
            group by cast((r.releaseYear / 10) * 10 as Integer)
            order by cast((r.releaseYear / 10) * 10 as Integer) nulls last""")
    List<CollectionStatsDto.DecadeCount> countByDecade(UUID userId);

    @Query("""
            select new nl.johnkruidhof.musiccollection.dto.stats.CollectionStatsDto$ArtistCount(r.artist, count(ci))
            from CollectionItem ci join ci.release r
            where ci.user.id = :userId
            group by r.artist
            order by count(ci) desc, r.artist""")
    List<CollectionStatsDto.ArtistCount> topArtists(UUID userId, Pageable pageable);

    /** Ongesorteerd: de enum staat als tekst in de database, de Goldmine-volgorde regelt de service. */
    @Query("""
            select new nl.johnkruidhof.musiccollection.dto.stats.CollectionStatsDto$ConditionCount(ci.conditionMedia, count(ci))
            from CollectionItem ci
            where ci.user.id = :userId
            group by ci.conditionMedia""")
    List<CollectionStatsDto.ConditionCount> countByCondition(UUID userId);
}
