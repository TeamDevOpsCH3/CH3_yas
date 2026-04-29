package com.yas.storefrontbff.viewmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TokenResponseVmTest {

    @Test
    void recordComponents_areReadable() {
        TokenResponseVm vm = new TokenResponseVm("access", "refresh");

        assertEquals("access", vm.accessToken());
        assertEquals("refresh", vm.refreshToken());
    }

    @Test
    void equality_andHashCode_followRecordContract() {
        TokenResponseVm first = new TokenResponseVm("a", "b");
        TokenResponseVm second = new TokenResponseVm("a", "b");
        TokenResponseVm third = new TokenResponseVm("a", "c");

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, third);
    }

    @Test
    void toString_containsComponentValues() {
        TokenResponseVm vm = new TokenResponseVm("access", "refresh");

        assertTrue(vm.toString().contains("access"));
        assertTrue(vm.toString().contains("refresh"));
    }
}
