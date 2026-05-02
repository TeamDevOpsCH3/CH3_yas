package com.yas.tax.service;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AbstractCircuitBreakFallbackHandlerTest {

    /**
     * Concrete subclass to expose protected methods for testing.
     */
    static class ConcreteHandler extends AbstractCircuitBreakFallbackHandler {
        public void callHandleBodilessFallback(Throwable throwable) throws Throwable {
            handleBodilessFallback(throwable);
        }

        public <T> T callHandleTypedFallback(Throwable throwable) throws Throwable {
            return handleTypedFallback(throwable);
        }
    }

    private final ConcreteHandler handler = new ConcreteHandler();

    // ------------------------------------------------------------------ //
    // handleBodilessFallback                                              //
    // ------------------------------------------------------------------ //

    @Test
    void handleBodilessFallback_whenRuntimeException_thenRethrows() {
        RuntimeException ex = new RuntimeException("circuit open");

        assertThrows(RuntimeException.class, () -> handler.callHandleBodilessFallback(ex));
    }

    @Test
    void handleBodilessFallback_whenCheckedException_thenRethrows() {
        Exception ex = new Exception("checked error");

        assertThrows(Exception.class, () -> handler.callHandleBodilessFallback(ex));
    }

    // ------------------------------------------------------------------ //
    // handleTypedFallback                                                 //
    // ------------------------------------------------------------------ //

    @Test
    void handleTypedFallback_whenRuntimeException_thenRethrows() {
        RuntimeException ex = new RuntimeException("typed circuit open");

        assertThrows(RuntimeException.class, () -> handler.callHandleTypedFallback(ex));
    }

    @Test
    void handleTypedFallback_whenCheckedException_thenRethrows() {
        Exception ex = new Exception("typed checked error");

        assertThrows(Exception.class, () -> handler.callHandleTypedFallback(ex));
    }
}
