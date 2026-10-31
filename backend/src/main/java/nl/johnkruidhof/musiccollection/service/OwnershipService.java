package nl.johnkruidhof.musiccollection.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import nl.johnkruidhof.musiccollection.dto.lookup.ReleaseCandidateDto;
import nl.johnkruidhof.musiccollection.entity.MetadataSource;
import nl.johnkruidhof.musiccollection.repository.CollectionItemRepository;
import nl.johnkruidhof.musiccollection.repository.WishlistItemRepository;

/**
 * Persoonlijke gegevens bij kandidaten: hoeveel exemplaren de gebruiker al heeft (F-23) en of de release
 * op de verlanglijst staat (F-24). Vier query's (per bron), ongeacht het aantal kandidaten.
 */
@Service
@RequiredArgsConstructor
public class OwnershipService {

    private final CollectionItemRepository collectionItemRepository;
    private final WishlistItemRepository wishlistItemRepository;

    @Transactional(readOnly = true)
    public List<ReleaseCandidateDto> withOwnedCounts(UUID userId, List<ReleaseCandidateDto> candidates) {
        if (candidates.isEmpty()) {
            return candidates;
        }
        List<String> discogsIds = externalIds(candidates, MetadataSource.DISCOGS);
        List<String> musicbrainzIds = externalIds(candidates, MetadataSource.MUSICBRAINZ);
        Map<String, Long> discogs = counts(collectionItemRepository.countOwnedByDiscogsIds(userId, discogsIds));
        Map<String, Long> musicbrainz = counts(collectionItemRepository.countOwnedByMusicbrainzIds(userId,
                musicbrainzIds));
        Set<String> wishedDiscogs = Set.copyOf(wishlistItemRepository.findWishedDiscogsIds(userId, discogsIds));
        Set<String> wishedMusicbrainz = Set.copyOf(wishlistItemRepository.findWishedMusicbrainzIds(userId,
                musicbrainzIds));
        return candidates.stream().map(candidate -> {
            boolean isDiscogs = candidate.source() == MetadataSource.DISCOGS;
            Map<String, Long> owned = isDiscogs ? discogs : musicbrainz;
            Set<String> wished = isDiscogs ? wishedDiscogs : wishedMusicbrainz;
            return personalize(candidate, owned.getOrDefault(candidate.externalId(), 0L).intValue(),
                    wished.contains(candidate.externalId()));
        }).toList();
    }

    private static ReleaseCandidateDto personalize(ReleaseCandidateDto c, int ownedCount, boolean onWishlist) {
        return new ReleaseCandidateDto(c.source(), c.externalId(), c.artist(), c.title(), c.releaseYear(),
                c.mediaType(), c.formatDetails(), c.label(), c.catalogNumber(), c.country(), c.barcode(),
                c.coverThumbnailUrl(), ownedCount, onWishlist);
    }

    private static List<String> externalIds(List<ReleaseCandidateDto> candidates, MetadataSource source) {
        List<String> ids = candidates.stream()
                .filter(c -> c.source() == source && c.externalId() != null)
                .map(ReleaseCandidateDto::externalId)
                .distinct()
                .toList();
        // Een lege IN-lijst is geen geldige SQL; een id dat nooit voorkomt wel.
        return ids.isEmpty() ? List.of("") : ids;
    }

    private static Map<String, Long> counts(List<CollectionItemRepository.ExternalIdCount> rows) {
        Map<String, Long> map = new HashMap<>();
        rows.forEach(row -> map.put(row.getExternalId(), row.getCount()));
        return map;
    }
}
