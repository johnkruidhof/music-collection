package nl.johnkruidhof.musiccollection.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import nl.johnkruidhof.musiccollection.dto.user.UserDto;
import nl.johnkruidhof.musiccollection.entity.AppUser;

@Mapper
public interface UserMapper {

    @Mapping(target = "roles", source = "roles")
    UserDto toDto(AppUser user, List<String> roles);
}
