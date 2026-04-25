package com.yas.product.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessagesUtilsTest {

    @Test
    void getMessage_shouldFormatExistingKeyWithArgs() {
        String message = MessagesUtils.getMessage(Constants.ErrorCode.PRODUCT_NOT_FOUND, "P001");
        assertEquals("Product P001 is not found", message);
    }

    @Test
    void getMessage_shouldFallbackToErrorCodeWhenKeyMissing() {
        String message = MessagesUtils.getMessage("UNKNOWN_ERROR_CODE", "ignored");
        assertEquals("UNKNOWN_ERROR_CODE", message);
    }

    @Test
    void getMessage_shouldKeepTemplateWhenNoArgsProvided() {
        String message = MessagesUtils.getMessage(Constants.ErrorCode.PRODUCT_NOT_FOUND);
        assertEquals("Product {} is not found", message);
    }

    @Test
    void getMessage_shouldRenderNullArgAsNullLiteral() {
        String message = MessagesUtils.getMessage(Constants.ErrorCode.PRODUCT_NOT_FOUND, (Object) null);
        assertEquals("Product null is not found", message);
    }
}