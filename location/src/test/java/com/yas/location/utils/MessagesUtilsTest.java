package com.yas.location.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
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

    @ParameterizedTest
    @CsvSource({
        "COUNTRY_NOT_FOUND,1,The country 1 is not found",
        "NAME_ALREADY_EXITED,foo,Request name foo is already existed"
    })
    void getMessage_knownCodes_formatSingleArgument(String code, String arg, String expected) {
        String message = MessagesUtils.getMessage(code, arg);

        assertThat(message).isEqualTo(expected);
    }
}
