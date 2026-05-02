package com.yas.order.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
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

    @ParameterizedTest
    @MethodSource("errorCodes")
    void errorCode_constants_matchValues(String actual, String expected) {
        assertEquals(expected, actual);
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

    private static Stream<Arguments> errorCodes() {
        return Stream.of(
            Arguments.of(Constants.ErrorCode.ORDER_NOT_FOUND, "ORDER_NOT_FOUND"),
            Arguments.of(Constants.ErrorCode.CHECKOUT_NOT_FOUND, "CHECKOUT_NOT_FOUND"),
            Arguments.of(Constants.ErrorCode.CHECKOUT_ITEM_NOT_EMPTY, "CHECKOUT_ITEM_NOT_EMPTY"),
            Arguments.of(Constants.ErrorCode.SIGN_IN_REQUIRED, "SIGN_IN_REQUIRED")
        );
    }
}
