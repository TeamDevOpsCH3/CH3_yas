package com.yas.search.constant;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ActionTest {

    @Test
    void constants_haveExpectedValues() {
        assertEquals("u", Action.UPDATE);
        assertEquals("c", Action.CREATE);
        assertEquals("d", Action.DELETE);
        assertEquals("r", Action.READ);
    }
}
