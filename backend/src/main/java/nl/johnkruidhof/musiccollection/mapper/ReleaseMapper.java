package nl.johnkruidhof.musiccollection.mapper;

import org.mapstruct.Mapper;

import nl.johnkruidhof.musiccollection.dto.release.ReleaseDto;
import nl.johnkruidhof.musiccollection.dto.release.TrackDto;
import nl.johnkruidhof.musiccollection.entity.Release;
import nl.johnkruidhof.musiccollection.entity.Track;

@Mapper
public interface ReleaseMapper {

    ReleaseDto toDto(Release release);

    TrackDto toDto(Track track);
}
