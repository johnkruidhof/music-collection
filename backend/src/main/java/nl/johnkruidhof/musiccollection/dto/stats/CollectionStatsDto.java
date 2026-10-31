package nl.johnkruidhof.musiccollection.dto.stats;

import java.math.BigDecimal;
import java.util.List;

import nl.johnkruidhof.musiccollection.entity.MediaCondition;
import nl.johnkruidhof.musiccollection.entity.MediaType;

/**
 * Statistieken over de eigen collectie (F-20).
 *
 * @param totalValue        som van alle ingevulde aankoopprijzen
 * @param itemsWithPrice    aantal items met een aankoopprijs (de rest telt niet mee in de waarde)
 * @param byDecade          {@code decade} is bv. 1990; {@code null} = jaar onbekend (staat achteraan)
 * @param topArtists        maximaal {@link #TOP_ARTISTS} artiesten, meeste items eerst
 */
public record CollectionStatsDto(
        long totalItems,
        BigDecimal totalValue,
        long itemsWithPrice,
        List<MediaTypeCount> byMediaType,
        List<DecadeCount> byDecade,
        List<ArtistCount> topArtists,
        List<ConditionCount> byCondition) {

    public static final int TOP_ARTISTS = 10;

    public record MediaTypeCount(MediaType mediaType, long count) {
    }

    public record DecadeCount(Integer decade, long count) {
    }

    public record ArtistCount(String artist, long count) {
    }

    /** {@code condition} {@code null} = staat niet ingevuld. */
    public record ConditionCount(MediaCondition condition, long count) {
    }
}
