package com.yas.inventory.viewmodel.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class ErrorVmTest {

    @Test
    void constructorWithoutFieldErrors_initializesEmptyList() {
        ErrorVm error = new ErrorVm("400", "Bad Request", "detail");

        assertNotNull(error.fieldErrors());
        assertEquals(0, error.fieldErrors().size());
    }
}
