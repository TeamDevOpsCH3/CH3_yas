package com.yas.backofficebff.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockOAuth2Login;

import com.yas.backofficebff.controller.AuthenticationController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest(controllers = AuthenticationController.class)
@AutoConfigureWebTestClient
@Import(SecurityConfig.class)
class SecurityWebFluxTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ReactiveClientRegistrationRepository clientRegistrationRepository;

    @Test
    void authenticationUser_withAdminRole_returnsOkAndUsername() {
        webTestClient.mutateWith(mockOAuth2Login()
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                .attributes(attrs -> attrs.put("preferred_username", "admin")))
            .get()
            .uri("/authentication/user")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.username").isEqualTo("admin");
    }

    @Test
    void authenticationUser_withNonAdminRole_returnsForbidden() {
        webTestClient.mutateWith(mockOAuth2Login()
                .authorities(new SimpleGrantedAuthority("ROLE_STAFF"))
                .attributes(attrs -> attrs.put("preferred_username", "staff")))
            .get()
            .uri("/authentication/user")
            .exchange()
            .expectStatus().isForbidden();
    }

    @Test
    void authenticationUser_withoutAuth_redirectsOrUnauthorized() {
        webTestClient.get()
            .uri("/authentication/user")
            .exchange()
            .expectStatus()
            .value(status -> assertThat(status).isIn(401, 302, 303, 307, 308));
    }

    @Test
    void healthEndpoint_withoutAuth_isNotBlockedBySecurity() {
        webTestClient.get()
            .uri("/actuator/health")
            .exchange()
            .expectStatus()
            .value(status -> assertThat(status).isNotIn(401, 403));
    }

    @Test
    void authenticationUser_withAdminRole_missingUsername_returnsNullBodyField() {
        webTestClient.mutateWith(mockOAuth2Login()
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
            .get()
            .uri("/authentication/user")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.username").value(value -> assertThat(value).isNull());
    }
}
