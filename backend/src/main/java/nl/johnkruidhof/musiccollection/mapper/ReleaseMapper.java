package nl.johnkruidhof.musiccollection.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import nl.johnkruidhof.musiccollection.dto.lookup.ReleaseCandidateDto;
import nl.johnkruidhof.musiccollection.dto.release.ReleaseDto;
import nl.johnkruidhof.musiccollection.dto.release.TrackDto;
import nl.johnkruidhof.musiccollection.entity.Release;
import nl.johnkruidhof.musiccollection.entity.Track;

@Mapper
public interface ReleaseMapper {

    ReleaseDto toDto(Release release);

    TrackDto toDto(Track track);

    /** Een release uit de lokale catalogus als kandidaat; {@code externalId} hoort bij de bron. */
    @Mapping(target = "externalId", expression = "java(externalId(release))")
    ReleaseCandidateDto toCandidate(Release release);

    default String externalId(Release release) {
        return switch (release.getSource()) {
            case DISCOGS -> release.getDiscogsId();
            case MUSICBRAINZ -> release.getMusicbrainzId();
            case MANUAL -> null;
        };
    }
}
