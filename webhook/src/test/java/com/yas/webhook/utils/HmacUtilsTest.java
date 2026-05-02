package com.yas.webhook.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import org.junit.jupiter.api.Test;

class HmacUtilsTest {

    @Test
    void hash_whenValidDataAndKey_thenReturnNonNullResult() throws NoSuchAlgorithmException, InvalidKeyException {
        String result = HmacUtils.hash("test-payload", "secret-key");
        assertNotNull(result);
        assertThat(result).isNotEmpty();
    }

    @Test
    void hash_whenSameInputs_thenReturnSameOutput() throws NoSuchAlgorithmException, InvalidKeyException {
        String result1 = HmacUtils.hash("payload", "key");
        String result2 = HmacUtils.hash("payload", "key");
        assertThat(result1).isEqualTo(result2);
    }

    @Test
    void hash_whenDifferentPayloads_thenReturnDifferentHashes() throws NoSuchAlgorithmException, InvalidKeyException {
        String result1 = HmacUtils.hash("payload-a", "secret");
        String result2 = HmacUtils.hash("payload-b", "secret");
        assertThat(result1).isNotEqualTo(result2);
    }

    @Test
    void hash_whenEmptyData_thenReturnResult() {
        assertDoesNotThrow(() -> HmacUtils.hash("", "some-key"));
    }
}
