package com.yas.product.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AbstractCircuitBreakFallbackHandlerTest {

    private TestCircuitBreakFallbackHandler handler;

    @BeforeEach
    void setUp() {
        handler = new TestCircuitBreakFallbackHandler();
    }

    @Test
    void handleBodilessFallback_ShouldThrowException() {
        Throwable throwable = new RuntimeException("Test exception");

        Throwable thrown = assertThrows(Throwable.class, () -> handler.handleBodilessFallback(throwable));

        assertEquals("Test exception", thrown.getMessage());
    }

    @Test
    void handleTypedFallback_ShouldThrowException() {
        Throwable throwable = new RuntimeException("Test exception");

        Throwable thrown = assertThrows(Throwable.class, () -> handler.handleTypedFallback(throwable));

        assertEquals("Test exception", thrown.getMessage());
    }

    @Test
    void handleError_ShouldThrowException() {
        Throwable throwable = new RuntimeException("Test exception");

        Throwable thrown = assertThrows(Throwable.class, () -> handler.handleError(throwable));

        assertEquals("Test exception", thrown.getMessage());
    }

    private static class TestCircuitBreakFallbackHandler extends AbstractCircuitBreakFallbackHandler {
        // Concrete class to test the abstract class
    }
}
