package com.yas.payment.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AbstractCircuitBreakFallbackHandlerTest {

    private static class TestHandler extends AbstractCircuitBreakFallbackHandler {
        public void triggerBodilessFallback(Throwable t) throws Throwable {
            handleBodilessFallback(t);
        }

        public <T> T triggerTypedFallback(Throwable t) throws Throwable {
            return handleTypedFallback(t);
        }
    }

    @Test
    void handleBodilessFallback_shouldThrowException() {
        TestHandler handler = new TestHandler();
        Throwable exception = new RuntimeException("Test Exception");

        assertThatThrownBy(() -> handler.triggerBodilessFallback(exception))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Test Exception");
    }

    @Test
    void handleTypedFallback_shouldThrowException() {
        TestHandler handler = new TestHandler();
        Throwable exception = new RuntimeException("Test Exception");

        assertThatThrownBy(() -> handler.triggerTypedFallback(exception))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Test Exception");
    }
}
