package com.yas.storefrontbff.viewmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GuestUserVmTest {

    @Test
    void recordComponents_areReadable() {
        GuestUserVm vm = new GuestUserVm("user-1", "user@example.com", "pass");

        assertEquals("user-1", vm.userId());
        assertEquals("user@example.com", vm.email());
        assertEquals("pass", vm.password());
    }

    @Test
    void equality_andHashCode_followRecordContract() {
        GuestUserVm first = new GuestUserVm("u", "e", "p");
        GuestUserVm second = new GuestUserVm("u", "e", "p");
        GuestUserVm third = new GuestUserVm("u", "e", "x");

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, third);
    }

    @Test
    void toString_containsComponentValues() {
        GuestUserVm vm = new GuestUserVm("u", "e", "p");

        assertTrue(vm.toString().contains("u"));
        assertTrue(vm.toString().contains("e"));
        assertTrue(vm.toString().contains("p"));
    }
}
