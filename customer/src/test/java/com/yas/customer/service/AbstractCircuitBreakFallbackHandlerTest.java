package com.yas.customer.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class AbstractCircuitBreakFallbackHandlerTest {

    private static class TestHandler extends AbstractCircuitBreakFallbackHandler {
        void callBodiless(Throwable throwable) throws Throwable {
            handleBodilessFallback(throwable);
        }

        void callError(Throwable throwable) throws Throwable {
            handleError(throwable);
        }

        <T> T callTyped(Throwable throwable) throws Throwable {
            return handleTypedFallback(throwable);
        }
    }

    @Test
    void handleError_rethrowsSameException() {
        TestHandler handler = new TestHandler();
        RuntimeException ex = new RuntimeException("boom");

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> handler.callError(ex));

        assertSame(ex, thrown);
    }

    @Test
    void handleBodilessFallback_rethrowsSameException() {
        TestHandler handler = new TestHandler();
        IllegalStateException ex = new IllegalStateException("fallback");

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> handler.callBodiless(ex));

        assertSame(ex, thrown);
    }

    @Test
    void handleTypedFallback_rethrowsSameException() {
        TestHandler handler = new TestHandler();
        RuntimeException ex = new RuntimeException("typed");

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> handler.callTyped(ex));

        assertSame(ex, thrown);
    }
}
