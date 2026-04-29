package com.yas.storefrontbff.viewmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AuthenticationInfoVmTest {

    @Test
    void recordComponents_areReadable() {
        AuthenticatedUserVm user = new AuthenticatedUserVm("alice");
        AuthenticationInfoVm vm = new AuthenticationInfoVm(true, user);

        assertEquals(true, vm.isAuthenticated());
        assertEquals(user, vm.authenticatedUser());
    }

    @Test
    void recordComponents_allowNullUser() {
        AuthenticationInfoVm vm = new AuthenticationInfoVm(false, null);

        assertEquals(false, vm.isAuthenticated());
        assertNull(vm.authenticatedUser());
    }

    @Test
    void equality_andHashCode_followRecordContract() {
        AuthenticationInfoVm first = new AuthenticationInfoVm(true, new AuthenticatedUserVm("alice"));
        AuthenticationInfoVm second = new AuthenticationInfoVm(true, new AuthenticatedUserVm("alice"));
        AuthenticationInfoVm third = new AuthenticationInfoVm(false, new AuthenticatedUserVm("alice"));

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, third);
    }

    @Test
    void toString_containsComponentValues() {
        AuthenticationInfoVm vm = new AuthenticationInfoVm(true, new AuthenticatedUserVm("alice"));

        assertTrue(vm.toString().contains("alice"));
    }
}
