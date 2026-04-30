package com.yas.order.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;

class ConstantsTest {

    @Test
    void errorCodeConstants_areAccessible() {
        assertEquals("ORDER_NOT_FOUND", Constants.ErrorCode.ORDER_NOT_FOUND);
        assertEquals("CHECKOUT_NOT_FOUND", Constants.ErrorCode.CHECKOUT_NOT_FOUND);
    }

    @Test
    void messageCodeConstants_areAccessible() {
        assertTrue(Constants.MessageCode.CREATE_CHECKOUT.contains("Create checkout"));
        assertTrue(Constants.MessageCode.UPDATE_CHECKOUT_STATUS.contains("Update checkout"));
    }

    @Test
    void columnConstants_areAccessible() {
        assertEquals("id", Constants.Column.ID_COLUMN);
        assertEquals("createdOn", Constants.Column.CREATE_ON_COLUMN);
        assertEquals("orderStatus", Constants.Column.ORDER_ORDER_STATUS_COLUMN);
    }

    @Test
    void innerClasses_havePrivateConstructors() throws Exception {
        assertPrivateConstructor(Constants.ErrorCode.class);
        assertPrivateConstructor(Constants.MessageCode.class);
        assertPrivateConstructor(Constants.Column.class);
    }

    private void assertPrivateConstructor(Class<?> type) throws Exception {
        Constructor<?> constructor = type.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        constructor.newInstance();
    }
}
