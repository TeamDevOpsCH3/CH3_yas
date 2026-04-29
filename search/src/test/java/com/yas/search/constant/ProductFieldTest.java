package com.yas.search.constant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.junit.jupiter.api.Test;

class ProductFieldTest {

    @Test
    void constants_haveExpectedValues() {
        assertEquals("name", ProductField.NAME);
        assertEquals("brand", ProductField.BRAND);
        assertEquals("price", ProductField.PRICE);
        assertEquals("isPublished", ProductField.IS_PUBLISHED);
        assertEquals("categories", ProductField.CATEGORIES);
        assertEquals("attributes", ProductField.ATTRIBUTES);
        assertEquals("createdOn", ProductField.CREATE_ON);
    }

    @Test
    void constructor_throwsUnsupportedOperationException() throws Exception {
        Constructor<ProductField> constructor = ProductField.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        InvocationTargetException ex = assertThrows(InvocationTargetException.class, constructor::newInstance);
        assertEquals(UnsupportedOperationException.class, ex.getTargetException().getClass());
    }
}
