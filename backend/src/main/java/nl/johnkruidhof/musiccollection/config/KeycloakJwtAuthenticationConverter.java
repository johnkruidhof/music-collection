package nl.johnkruidhof.musiccollection.config;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Zet de Keycloak realm-rollen ({@code realm_access.roles}) om naar Spring authorities
 * ({@code ROLE_user}, {@code ROLE_admin}). De principal-naam is {@code preferred_username}.
 */
public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    public static final String ROLE_PREFIX = "ROLE_";

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String name = jwt.getClaimAsString("preferred_username");
        return new JwtAuthenticationToken(jwt, extractAuthorities(jwt), name != null ? name : jwt.getSubject());
    }

    public static List<String> extractRealmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null || !(realmAccess.get("roles") instanceof Collection<?> roles)) {
            return List.of();
        }
        return roles.stream().map(Object::toString).toList();
    }

    private static Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        return extractRealmRoles(jwt).stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority(ROLE_PREFIX + role))
                .toList();
    }
}
