package com.yas.search.viewmodel.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import org.junit.jupiter.api.Test;

class ErrorVmTest {

    @Test
    void constructorWithoutFieldErrors_initializesEmptyList() {
        ErrorVm errorVm = new ErrorVm("400", "Bad Request", "Invalid input");

        assertNotNull(errorVm.fieldErrors());
        assertEquals(0, errorVm.fieldErrors().size());
    }

    @Test
    void constructorWithFieldErrors_preservesValues() {
        List<String> fieldErrors = List.of("name is required", "price must be positive");
        ErrorVm errorVm = new ErrorVm("422", "Validation error", "Invalid fields", fieldErrors);

        assertEquals(fieldErrors, errorVm.fieldErrors());
        assertEquals("422", errorVm.statusCode());
        assertEquals("Validation error", errorVm.title());
        assertEquals("Invalid fields", errorVm.detail());
    }
}
