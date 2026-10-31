package nl.johnkruidhof.musiccollection.dto.recognition;

import java.util.List;

import nl.johnkruidhof.musiccollection.dto.collection.CollectionItemSummaryDto;
import nl.johnkruidhof.musiccollection.dto.lookup.ReleaseCandidateDto;

/**
 * Resultaat van luisteren (F-30): {@code track} {@code null} = niets herkend. {@code inCollection} = eigen
 * items met dit nummer of album; {@code candidates} = releases om toe te voegen (zoeken op artiest + album).
 */
public record RecognitionResultDto(
        RecognizedTrackDto track,
        List<CollectionItemSummaryDto> inCollection,
        List<ReleaseCandidateDto> candidates) {

    public static final RecognitionResultDto NOTHING = new RecognitionResultDto(null, List.of(), List.of());
}
