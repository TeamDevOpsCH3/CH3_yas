package com.yas.storefrontbff.viewmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class CartGetDetailVmTest {

    @Test
    void recordComponents_areReadable() {
        List<CartDetailVm> details = List.of(new CartDetailVm(1L, 2L, 1));
        CartGetDetailVm vm = new CartGetDetailVm(100L, "customer-1", details);

        assertEquals(100L, vm.id());
        assertEquals("customer-1", vm.customerId());
        assertSame(details, vm.cartDetails());
    }

    @Test
    void equality_andHashCode_followRecordContract() {
        List<CartDetailVm> details = List.of(new CartDetailVm(1L, 2L, 1));
        CartGetDetailVm first = new CartGetDetailVm(1L, "c", details);
        CartGetDetailVm second = new CartGetDetailVm(1L, "c", details);
        CartGetDetailVm third = new CartGetDetailVm(1L, "x", details);

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, third);
    }

    @Test
    void toString_containsComponentValues() {
        CartGetDetailVm vm = new CartGetDetailVm(1L, "customer", List.of());

        assertTrue(vm.toString().contains("1"));
        assertTrue(vm.toString().contains("customer"));
    }
}
