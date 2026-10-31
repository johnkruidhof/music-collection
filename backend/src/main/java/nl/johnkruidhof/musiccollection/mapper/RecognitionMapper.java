package nl.johnkruidhof.musiccollection.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import nl.johnkruidhof.musiccollection.client.audd.AuddResult;
import nl.johnkruidhof.musiccollection.dto.recognition.RecognizedTrackDto;

@Mapper
public interface RecognitionMapper {

    @Mapping(target = "releaseYear", source = "releaseDate", qualifiedByName = "yearFromDate")
    RecognizedTrackDto toDto(AuddResult result);

    @Named("yearFromDate")
    default Integer yearFromDate(String date) {
        return date != null && date.matches("\\d{4}(-.*)?") ? Integer.valueOf(date.substring(0, 4)) : null;
    }
}
