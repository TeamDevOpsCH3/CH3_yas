package com.yas.product.viewmodel.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class ErrorVmTest {

    @Test
    void constructor_withoutFieldErrors_createsEmptyList() {
        ErrorVm errorVm = new ErrorVm("400", "Bad Request", "detail");

        assertNotNull(errorVm.fieldErrors());
        assertEquals(0, errorVm.fieldErrors().size());
    }
}
