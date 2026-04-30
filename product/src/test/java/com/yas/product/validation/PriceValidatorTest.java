package com.yas.product.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PriceValidatorTest {
    @Test
    void isValid_returnsTrueForZeroAndPositive() {
        PriceValidator validator = new PriceValidator();

        assertTrue(validator.isValid(0.0, null));
        assertTrue(validator.isValid(10.5, null));
    }

    @Test
    void isValid_returnsFalseForNegative() {
        PriceValidator validator = new PriceValidator();

        assertFalse(validator.isValid(-0.01, null));
    }
}
