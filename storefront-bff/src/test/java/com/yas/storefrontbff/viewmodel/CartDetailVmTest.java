package com.yas.storefrontbff.viewmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CartDetailVmTest {

    @Test
    void recordComponents_areReadable() {
        CartDetailVm vm = new CartDetailVm(1L, 10L, 2);

        assertEquals(1L, vm.id());
        assertEquals(10L, vm.productId());
        assertEquals(2, vm.quantity());
    }

    @Test
    void equality_andHashCode_followRecordContract() {
        CartDetailVm first = new CartDetailVm(1L, 10L, 2);
        CartDetailVm second = new CartDetailVm(1L, 10L, 2);
        CartDetailVm third = new CartDetailVm(1L, 10L, 3);

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, third);
    }

    @Test
    void toString_containsComponentValues() {
        CartDetailVm vm = new CartDetailVm(1L, 10L, 2);

        assertTrue(vm.toString().contains("1"));
        assertTrue(vm.toString().contains("10"));
        assertTrue(vm.toString().contains("2"));
    }
}
