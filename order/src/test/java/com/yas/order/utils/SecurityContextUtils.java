package com.yas.order.utils;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public class SecurityContextUtils {

    private SecurityContextUtils() {
    }

    public static void setUpSecurityContext(String userName) {
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn(userName);
        Jwt jwt = mock(Jwt.class);
        lenient().when(auth.getPrincipal()).thenReturn(jwt);
        lenient().when(jwt.getTokenValue()).thenReturn("token");
        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);
    }

    public static void setSubjectUpSecurityContext(String subject) {
        JwtAuthenticationToken auth = mock(JwtAuthenticationToken.class);
        Jwt jwt = mock(Jwt.class);
        lenient().when(auth.getToken()).thenReturn(jwt);
        lenient().when(jwt.getSubject()).thenReturn(subject);
        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);
    }

}
