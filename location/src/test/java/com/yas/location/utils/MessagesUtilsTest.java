package com.yas.location.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MessagesUtilsTest {

    @Test
    void getMessage_knownCode_formatsParameters() {
        String message = MessagesUtils.getMessage(Constants.ErrorCode.COUNTRY_NOT_FOUND, 1L);

        assertThat(message).isEqualTo("The country 1 is not found");
    }

    @Test
    void getMessage_unknownCode_returnsCode() {
        String message = MessagesUtils.getMessage("UNKNOWN_CODE");

        assertThat(message).isEqualTo("UNKNOWN_CODE");
    }
}
