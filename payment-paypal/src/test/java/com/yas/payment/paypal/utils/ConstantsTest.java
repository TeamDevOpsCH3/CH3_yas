package com.yas.payment.paypal.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConstantsTest {

    @Test
    void testErrorCodeConstants() {
        assertEquals("SIGN_IN_REQUIRED", Constants.ErrorCode.SIGN_IN_REQUIRED);
        assertEquals("FORBIDDEN", Constants.ErrorCode.FORBIDDEN);
    }

    @Test
    void testMessageConstants() {
        assertEquals("PAYMENT_FAIL_MESSAGE", Constants.Message.PAYMENT_FAIL_MESSAGE);
        assertEquals("PAYMENT_SUCCESS_MESSAGE", Constants.Message.PAYMENT_SUCCESS_MESSAGE);
    }

    @Test
    void testYasConstants() {
        assertEquals("Yas", Constants.Yas.BRAND_NAME);
    }
}
