package com.yas.search.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MessagesUtilsTest {

    @Test
    void getMessage_whenCodeExists_formatsMessage() {
        String result = MessagesUtils.getMessage("PRODUCT_NOT_FOUND", 42);

        assertTrue(result.contains("42"));
        assertTrue(result.toLowerCase().contains("not found"));
    }

    @Test
    void getMessage_whenCodeMissing_returnsCode() {
        String result = MessagesUtils.getMessage("UNKNOWN_CODE");

        assertEquals("UNKNOWN_CODE", result);
    }
}
