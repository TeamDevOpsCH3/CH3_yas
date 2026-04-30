package com.yas.inventory.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class AbstractCircuitBreakFallbackHandlerTest {

    @Test
    void handleTypedFallback_throwsOriginalException() {
        TestHandler handler = new TestHandler();
        RuntimeException exception = new RuntimeException("boom");

        assertThrows(RuntimeException.class, () -> handler.callTyped(exception));
    }

    @Test
    void handleBodilessFallback_throwsOriginalException() {
        TestHandler handler = new TestHandler();
        IllegalStateException exception = new IllegalStateException("fail");

        assertThrows(IllegalStateException.class, () -> handler.callBodiless(exception));
    }

    private static final class TestHandler extends AbstractCircuitBreakFallbackHandler {
        void callBodiless(Throwable throwable) throws Throwable {
            handleBodilessFallback(throwable);
        }

        <T> T callTyped(Throwable throwable) throws Throwable {
            return handleTypedFallback(throwable);
        }
    }
}
