package nl.johnkruidhof.musiccollection.service;

import java.util.Comparator;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import nl.johnkruidhof.musiccollection.dto.stats.CollectionStatsDto;
import nl.johnkruidhof.musiccollection.repository.CollectionStatsRepository;

/** Statistieken van de eigen collectie (F-20). */
@Service
@RequiredArgsConstructor
public class CollectionStatsService {

    /** Mint → Poor (volgorde van de enum), onbekend achteraan. */
    private static final Comparator<CollectionStatsDto.ConditionCount> BY_GOLDMINE_SCALE = Comparator.comparing(
            CollectionStatsDto.ConditionCount::condition, Comparator.nullsLast(Comparator.naturalOrder()));

    private final CollectionStatsRepository statsRepository;
    private final UserService userService;

    @Transactional
    public CollectionStatsDto getStats(Jwt jwt) {
        UUID userId = userService.getOrCreate(jwt).getId();
        return new CollectionStatsDto(
                statsRepository.countItems(userId),
                statsRepository.totalValue(userId),
                statsRepository.countItemsWithPrice(userId),
                statsRepository.countByMediaType(userId),
                statsRepository.countByDecade(userId),
                statsRepository.topArtists(userId, PageRequest.of(0, CollectionStatsDto.TOP_ARTISTS)),
                statsRepository.countByCondition(userId).stream()
                        .sorted(BY_GOLDMINE_SCALE)
                        .toList(),
                statsRepository.countByGenre(userId));
    }
}
