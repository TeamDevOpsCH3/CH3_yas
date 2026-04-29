package com.yas.backofficebff.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yas.backofficebff.viewmodel.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.user.OAuth2User;

@ExtendWith(MockitoExtension.class)
class AuthenticationControllerTest {

    private final AuthenticationController controller = new AuthenticationController();

    @Test
    void user_whenPrincipalPresent_returnsUsername() {
        OAuth2User principal = mock(OAuth2User.class);
        when(principal.getAttribute("preferred_username")).thenReturn("admin");

        ResponseEntity<AuthenticatedUser> response = controller.user(principal);

        assertNotNull(response);
        assertNotNull(response.getBody());
        assertEquals("admin", response.getBody().username());
    }
}
