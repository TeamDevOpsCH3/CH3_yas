package com.yas.backofficebff.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Test
    void generateAuthoritiesFromClaim_mapsRolesWithPrefix() {
        SecurityConfig config = new SecurityConfig(mock(ReactiveClientRegistrationRepository.class));

        Collection<GrantedAuthority> authorities = config.generateAuthoritiesFromClaim(List.of("ADMIN", "STAFF"));

        assertEquals(2, authorities.size());
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_STAFF")));
    }

    @Test
    void userAuthoritiesMapperForKeycloak_whenOidcAuthority_mapsRoles() {
        SecurityConfig config = new SecurityConfig(mock(ReactiveClientRegistrationRepository.class));
        GrantedAuthoritiesMapper mapper = config.userAuthoritiesMapperForKeycloak();

        Map<String, Object> realmAccess = Map.of("roles", List.of("ADMIN"));
        OidcUserInfo userInfo = new OidcUserInfo(Map.of("realm_access", realmAccess));
        Instant now = Instant.now();
        OidcIdToken idToken = new OidcIdToken(
            "token",
            now,
            now.plusSeconds(60),
            Map.of("sub", "user-id", "groups", List.of("role_admin"))
        );
        OidcUserAuthority authority = new OidcUserAuthority(idToken, userInfo);

        Collection<? extends GrantedAuthority> result = mapper.mapAuthorities(List.of(authority));

        assertEquals(1, result.size());
        assertTrue(result.contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Test
    void userAuthoritiesMapperForKeycloak_whenOauth2Authority_mapsRoles() {
        SecurityConfig config = new SecurityConfig(mock(ReactiveClientRegistrationRepository.class));
        GrantedAuthoritiesMapper mapper = config.userAuthoritiesMapperForKeycloak();

        Map<String, Object> realmAccess = Map.of("roles", List.of("STAFF"));
        OAuth2UserAuthority authority = new OAuth2UserAuthority(Map.of("realm_access", realmAccess));

        Collection<? extends GrantedAuthority> result = mapper.mapAuthorities(List.of(authority));

        assertEquals(1, result.size());
        assertTrue(result.contains(new SimpleGrantedAuthority("ROLE_STAFF")));
    }

    @Test
    void userAuthoritiesMapperForKeycloak_whenNoRealmAccess_returnsEmpty() {
        SecurityConfig config = new SecurityConfig(mock(ReactiveClientRegistrationRepository.class));
        GrantedAuthoritiesMapper mapper = config.userAuthoritiesMapperForKeycloak();

        OAuth2UserAuthority authority = new OAuth2UserAuthority(Map.of("other", "value"));
        Collection<? extends GrantedAuthority> result = mapper.mapAuthorities(List.of(authority));

        assertTrue(result.isEmpty());
    }
}
