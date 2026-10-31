package nl.johnkruidhof.musiccollection.mapper;

import java.util.List;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import nl.johnkruidhof.musiccollection.dto.lookup.ReleaseCandidateDto;
import nl.johnkruidhof.musiccollection.dto.release.ManualReleaseRequest;
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

    /** Een release van een externe bron opslaan in de catalogus; tracks via {@link #addTracks}. */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tracks", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Release toEntity(ReleaseDto release);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "source", constant = "MANUAL")
    @Mapping(target = "discogsId", ignore = true)
    @Mapping(target = "musicbrainzId", ignore = true)
    @Mapping(target = "coverImageUrl", ignore = true)
    @Mapping(target = "coverThumbnailUrl", ignore = true)
    @Mapping(target = "tracks", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Release toEntity(ManualReleaseRequest release);

    @AfterMapping
    default void addTracks(ReleaseDto source, @MappingTarget Release release) {
        replaceTracks(release, source.tracks());
    }

    @AfterMapping
    default void addTracks(ManualReleaseRequest source, @MappingTarget Release release) {
        replaceTracks(release, source.tracks());
    }

    /** Vervangt alle tracks; {@code sortOrder} wordt opnieuw genummerd in de aangeleverde volgorde. */
    default void replaceTracks(Release release, List<TrackDto> tracks) {
        release.clearTracks();
        if (tracks == null) {
            return;
        }
        for (TrackDto dto : tracks) {
            Track track = new Track(Math.max(1, dto.discNumber()), release.getTracks().size() + 1,
                    dto.position(), dto.title().strip());
            track.setDurationSeconds(dto.durationSeconds());
            track.setArtist(dto.artist());
            release.addTrack(track);
        }
    }

    default String externalId(Release release) {
        return switch (release.getSource()) {
            case DISCOGS -> release.getDiscogsId();
            case MUSICBRAINZ -> release.getMusicbrainzId();
            case MANUAL -> null;
        };
    }
}
