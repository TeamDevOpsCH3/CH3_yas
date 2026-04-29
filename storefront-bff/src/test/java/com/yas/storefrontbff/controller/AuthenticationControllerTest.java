package com.yas.storefrontbff.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yas.storefrontbff.viewmodel.AuthenticationInfoVm;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.user.OAuth2User;

class AuthenticationControllerTest {

    private final AuthenticationController controller = new AuthenticationController();

    @Test
    void user_whenPrincipalNull_returnsAnonymous() {
        ResponseEntity<AuthenticationInfoVm> response = controller.user(null);

        assertNotNull(response);
        AuthenticationInfoVm body = response.getBody();
        assertNotNull(body);
        assertEquals(false, body.isAuthenticated());
        assertNull(body.authenticatedUser());
    }

    @Test
    void user_whenPrincipalPresent_returnsAuthenticatedUser() {
        OAuth2User principal = mock(OAuth2User.class);
        when(principal.getAttribute("preferred_username")).thenReturn("alice");

        ResponseEntity<AuthenticationInfoVm> response = controller.user(principal);

        assertNotNull(response);
        AuthenticationInfoVm body = response.getBody();
        assertNotNull(body);
        assertEquals(true, body.isAuthenticated());
        assertNotNull(body.authenticatedUser());
        assertEquals("alice", body.authenticatedUser().username());
    }
}
