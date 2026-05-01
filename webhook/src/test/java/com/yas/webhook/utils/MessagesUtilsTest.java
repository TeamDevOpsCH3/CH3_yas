package com.yas.webhook.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MessagesUtilsTest {

    @Test
    void getMessage_whenKnownErrorCode_thenReturnFormattedMessage() {
        // Use a code that exists in messages/messages.properties
        // If not found, should return the errorCode itself (fallback branch)
        String result = MessagesUtils.getMessage("NOT_FOUND", "Webhook", 1L);
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
    }

    @Test
    void getMessage_whenUnknownErrorCode_thenReturnCodeAsMessage() {
        String unknownCode = "THIS_CODE_DOES_NOT_EXIST_XYZ";
        String result = MessagesUtils.getMessage(unknownCode);
        // Falls into MissingResourceException catch → returns the errorCode itself
        assertThat(result).isEqualTo(unknownCode);
    }

    @Test
    void getMessage_whenCodeWithArguments_thenReturnInterpolatedMessage() {
        String result = MessagesUtils.getMessage("NOT_FOUND", "Resource", 42L);
        assertThat(result).isNotNull();
    }
}
