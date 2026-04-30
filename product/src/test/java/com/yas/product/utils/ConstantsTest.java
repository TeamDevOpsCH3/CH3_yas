package com.yas.product.utils;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class ConstantsTest {

    @Test
    void constructors_areAccessibleForCoverage() {
        Constants constants = new Constants();
        Constants.ErrorCode errorCode = constants.new ErrorCode();

        assertNotNull(constants);
        assertNotNull(errorCode);
    }
}
