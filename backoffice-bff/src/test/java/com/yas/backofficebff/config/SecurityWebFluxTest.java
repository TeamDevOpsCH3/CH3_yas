package com.yas.backofficebff.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yas.backofficebff.controller.AuthenticationController;
import com.yas.backofficebff.viewmodel.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.user.OAuth2User;

@ExtendWith(MockitoExtension.class)
class SecurityWebFluxTest {

    private final AuthenticationController controller = new AuthenticationController();

    @Test
    void authenticationUser_withPrincipal_returnsOkAndUsername() {
        OAuth2User principal = mock(OAuth2User.class);
        when(principal.getAttribute("preferred_username")).thenReturn("admin");

        ResponseEntity<AuthenticatedUser> response = controller.user(principal);

        assertNotNull(response);
        assertNotNull(response.getBody());
        assertEquals("admin", response.getBody().username());
    }

    @Test
    void authenticationUser_withMissingUsername_returnsNullField() {
        OAuth2User principal = mock(OAuth2User.class);
        when(principal.getAttribute("preferred_username")).thenReturn(null);

        ResponseEntity<AuthenticatedUser> response = controller.user(principal);

        assertNotNull(response);
        assertNotNull(response.getBody());
        assertEquals(null, response.getBody().username());
    }
}
