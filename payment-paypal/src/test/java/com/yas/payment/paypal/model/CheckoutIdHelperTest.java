package com.yas.payment.paypal.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CheckoutIdHelperTest {

    @Test
    void testCheckoutId() {
        CheckoutIdHelper.setCheckoutId("test-checkout-id");
        assertEquals("test-checkout-id", CheckoutIdHelper.getCheckoutId());
        
        CheckoutIdHelper.setCheckoutId(null);
        assertNull(CheckoutIdHelper.getCheckoutId());
    }
}
