package com.yas.backofficebff.viewmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AuthenticatedUserTest {

    @Test
    void recordComponents_areReadable() {
        AuthenticatedUser user = new AuthenticatedUser("admin");

        assertEquals("admin", user.username());
    }

    @Test
    void equality_andHashCode_followRecordContract() {
        AuthenticatedUser first = new AuthenticatedUser("admin");
        AuthenticatedUser second = new AuthenticatedUser("admin");
        AuthenticatedUser third = new AuthenticatedUser("user");

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, third);
    }

    @Test
    void toString_containsUsername() {
        AuthenticatedUser user = new AuthenticatedUser("admin");

        assertTrue(user.toString().contains("admin"));
    }
}
