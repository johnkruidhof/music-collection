package nl.johnkruidhof.musiccollection.service;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import nl.johnkruidhof.musiccollection.config.KeycloakJwtAuthenticationConverter;
import nl.johnkruidhof.musiccollection.dto.user.UserDto;
import nl.johnkruidhof.musiccollection.entity.AppUser;
import nl.johnkruidhof.musiccollection.mapper.UserMapper;
import nl.johnkruidhof.musiccollection.repository.AppUserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Koppelt de Keycloak-identiteit (JWT) aan een lokale {@link AppUser}.
 * Bij de eerste aanroep wordt de gebruiker aangemaakt; daarna worden naam/e-mail bijgewerkt.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final AppUserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional
    public UserDto getCurrentUser(Jwt jwt) {
        AppUser user = getOrCreate(jwt);
        return userMapper.toDto(user, KeycloakJwtAuthenticationConverter.extractRealmRoles(jwt));
    }

    /** Voor gebruik in andere services die de eigenaar van data nodig hebben. */
    @Transactional
    public AppUser getOrCreate(Jwt jwt) {
        AppUser user = userRepository.findByKeycloakId(jwt.getSubject())
                .orElseGet(() -> {
                    log.info("Nieuwe gebruiker aangemaakt: {}", jwt.getClaimAsString("preferred_username"));
                    return userRepository.save(new AppUser(jwt.getSubject(), username(jwt)));
                });
        syncProfile(user, jwt);
        return user;
    }

    private static void syncProfile(AppUser user, Jwt jwt) {
        user.setUsername(username(jwt));
        user.setEmail(jwt.getClaimAsString("email"));
        user.setDisplayName(jwt.getClaimAsString("name"));
    }

    private static String username(Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        return username != null ? username : jwt.getSubject();
    }
}
