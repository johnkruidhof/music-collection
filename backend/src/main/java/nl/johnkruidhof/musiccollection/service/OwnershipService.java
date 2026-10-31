package nl.johnkruidhof.musiccollection.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import nl.johnkruidhof.musiccollection.dto.lookup.ReleaseCandidateDto;
import nl.johnkruidhof.musiccollection.entity.MetadataSource;
import nl.johnkruidhof.musiccollection.repository.CollectionItemRepository;

/**
 * Dubbelen-detectie (F-23): vult per kandidaat in hoeveel exemplaren de gebruiker al heeft. Twee
 * groepsquery's (Discogs- en MusicBrainz-id's), ongeacht het aantal kandidaten.
 */
@Service
@RequiredArgsConstructor
public class OwnershipService {

    private final CollectionItemRepository collectionItemRepository;

    @Transactional(readOnly = true)
    public List<ReleaseCandidateDto> withOwnedCounts(UUID userId, List<ReleaseCandidateDto> candidates) {
        if (candidates.isEmpty()) {
            return candidates;
        }
        Map<String, Long> discogs = counts(collectionItemRepository.countOwnedByDiscogsIds(userId,
                externalIds(candidates, MetadataSource.DISCOGS)));
        Map<String, Long> musicbrainz = counts(collectionItemRepository.countOwnedByMusicbrainzIds(userId,
                externalIds(candidates, MetadataSource.MUSICBRAINZ)));
        return candidates.stream().map(candidate -> {
            Map<String, Long> owned = candidate.source() == MetadataSource.DISCOGS ? discogs : musicbrainz;
            return withOwnedCount(candidate, owned.getOrDefault(candidate.externalId(), 0L).intValue());
        }).toList();
    }

    private static ReleaseCandidateDto withOwnedCount(ReleaseCandidateDto c, int count) {
        return new ReleaseCandidateDto(c.source(), c.externalId(), c.artist(), c.title(), c.releaseYear(),
                c.mediaType(), c.formatDetails(), c.label(), c.catalogNumber(), c.country(), c.barcode(),
                c.coverThumbnailUrl(), count);
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
