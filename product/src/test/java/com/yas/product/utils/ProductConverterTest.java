package com.yas.product.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductConverterTest {

    @Test
    void toSlug_shouldTrimLowercaseAndNormalizeSeparators() {
        String result = ProductConverter.toSlug("  Hello   WORLD!!  ");
        assertEquals("hello-world-", result);
    }

    @Test
    void toSlug_shouldCollapseMultipleHyphens() {
        String result = ProductConverter.toSlug("A---B----C");
        assertEquals("a-b-c", result);
    }

    @Test
    void toSlug_shouldRemoveLeadingHyphenAfterNormalization() {
        String result = ProductConverter.toSlug("!!!ABC");
        assertEquals("abc", result);
    }

    @Test
    void toSlug_shouldReturnEmptyForWhitespaceOnlyInput() {
        String result = ProductConverter.toSlug("   ");
        assertEquals("", result);
    }

    @Test
    void toSlug_shouldReturnEmptyForOnlySpecialChars() {
        String result = ProductConverter.toSlug("!!!@@@###");
        assertEquals("", result);
    }

    @Test
    void toSlug_shouldThrowWhenInputIsNull() {
        assertThrows(NullPointerException.class, () -> ProductConverter.toSlug(null));
    }
}