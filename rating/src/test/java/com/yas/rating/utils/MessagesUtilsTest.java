package com.yas.rating.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MessagesUtilsTest {

    @Test
    void getMessage_WhenKeyExists_ReturnsFormattedMessage() {
        String message = MessagesUtils.getMessage(Constants.ErrorCode.RATING_NOT_FOUND, 10);
        assertEquals("RATING 10 is not found", message);
    }

    @Test
    void getMessage_WhenKeyMissing_ReturnsKey() {
        String message = MessagesUtils.getMessage("UNKNOWN_KEY");
        assertEquals("UNKNOWN_KEY", message);
    }
}
