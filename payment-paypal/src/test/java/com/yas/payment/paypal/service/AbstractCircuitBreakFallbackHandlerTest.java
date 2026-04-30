package com.yas.payment.paypal.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AbstractCircuitBreakFallbackHandlerTest {

    private static class TestCircuitBreakFallbackHandler extends AbstractCircuitBreakFallbackHandler {
        public void executeBodilessFallback(Throwable t) throws Throwable {
            handleBodilessFallback(t);
        }

        public <T> T executeTypedFallback(Throwable t) throws Throwable {
            return handleTypedFallback(t);
        }
    }

    @Test
    void testHandleBodilessFallback() {
        TestCircuitBreakFallbackHandler handler = new TestCircuitBreakFallbackHandler();
        Throwable t = new RuntimeException("test error");
        
        RuntimeException exception = assertThrows(RuntimeException.class, () -> handler.executeBodilessFallback(t));
        assertEquals("test error", exception.getMessage());
    }

    @Test
    void testHandleTypedFallback() {
        TestCircuitBreakFallbackHandler handler = new TestCircuitBreakFallbackHandler();
        Throwable t = new RuntimeException("test error");
        
        RuntimeException exception = assertThrows(RuntimeException.class, () -> handler.executeTypedFallback(t));
        assertEquals("test error", exception.getMessage());
    }
}
