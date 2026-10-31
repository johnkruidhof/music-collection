package nl.johnkruidhof.musiccollection.dto.release;

import java.util.List;
import java.util.UUID;

import nl.johnkruidhof.musiccollection.entity.MediaType;
import nl.johnkruidhof.musiccollection.entity.MetadataSource;

public record ReleaseDto(
        UUID id,
        String barcode,
        String artist,
        String title,
        Integer releaseYear,
        MediaType mediaType,
        String formatDetails,
        String label,
        String catalogNumber,
        String country,
        String coverImageUrl,
        String coverThumbnailUrl,
        MetadataSource source,
        String discogsId,
        String musicbrainzId,
        List<TrackDto> tracks) {
}
