package com.yas.storefrontbff.viewmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CartItemVmTest {

    @Test
    void fromCartDetailVm_copiesValues() {
        CartDetailVm detail = new CartDetailVm(5L, 20L, 3);

        CartItemVm item = CartItemVm.fromCartDetailVm(detail);

        assertEquals(20L, item.productId());
        assertEquals(3, item.quantity());
    }

    @Test
    void fromCartDetailVm_whenNull_throwsException() {
        assertThrows(NullPointerException.class, () -> CartItemVm.fromCartDetailVm(null));
    }

    @Test
    void equality_andHashCode_followRecordContract() {
        CartItemVm first = new CartItemVm(10L, 2);
        CartItemVm second = new CartItemVm(10L, 2);
        CartItemVm third = new CartItemVm(10L, 3);

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, third);
    }

    @Test
    void toString_containsComponentValues() {
        CartItemVm vm = new CartItemVm(10L, 2);

        assertTrue(vm.toString().contains("10"));
        assertTrue(vm.toString().contains("2"));
    }
}
