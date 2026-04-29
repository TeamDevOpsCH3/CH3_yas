package com.yas.search.constant.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SortTypeTest {

    @Test
    void values_haveExpectedOrder() {
        SortType[] values = SortType.values();

        assertEquals(3, values.length);
        assertEquals(SortType.DEFAULT, values[0]);
        assertEquals(SortType.PRICE_ASC, values[1]);
        assertEquals(SortType.PRICE_DESC, values[2]);
    }
}
