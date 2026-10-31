package nl.johnkruidhof.musiccollection.dto.user;

import java.util.List;
import java.util.UUID;

public record UserDto(
        UUID id,
        String username,
        String email,
        String displayName,
        List<String> roles) {
}
