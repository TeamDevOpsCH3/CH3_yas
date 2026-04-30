package com.yas.customer.viewmodel;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class ErrorVmTest {

    @Test
    void constructorWithoutFieldErrors_defaultsToEmptyList() {
        ErrorVm vm = new ErrorVm("400", "Bad Request", "Invalid input");

        assertThat(vm.fieldErrors()).isNotNull();
        assertThat(vm.fieldErrors()).isEmpty();
    }

    @Test
    void constructorWithFieldErrors_keepsProvidedList() {
        List<String> errors = List.of("email", "password");
        ErrorVm vm = new ErrorVm("400", "Bad Request", "Invalid input", errors);

        assertThat(vm.fieldErrors()).containsExactly("email", "password");
    }
}
