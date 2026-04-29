package com.yas.storefrontbff.viewmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AuthenticatedUserVmTest {

    @Test
    void recordComponents_areReadable() {
        AuthenticatedUserVm vm = new AuthenticatedUserVm("alice");

        assertEquals("alice", vm.username());
    }

    @Test
    void equality_andHashCode_followRecordContract() {
        AuthenticatedUserVm first = new AuthenticatedUserVm("alice");
        AuthenticatedUserVm second = new AuthenticatedUserVm("alice");
        AuthenticatedUserVm third = new AuthenticatedUserVm("bob");

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, third);
    }

    @Test
    void toString_containsComponentValues() {
        AuthenticatedUserVm vm = new AuthenticatedUserVm("alice");

        assertTrue(vm.toString().contains("alice"));
    }
}
