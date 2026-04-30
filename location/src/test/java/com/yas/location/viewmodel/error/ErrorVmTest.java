package com.yas.location.viewmodel.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class ErrorVmTest {

    @Test
    void constructor_withoutFieldErrors_initializesEmptyList() {
        ErrorVm vm = new ErrorVm("400", "Bad request", "detail");

        assertThat(vm.statusCode()).isEqualTo("400");
        assertThat(vm.title()).isEqualTo("Bad request");
        assertThat(vm.detail()).isEqualTo("detail");
        assertThat(vm.fieldErrors()).isNotNull();
        assertThat(vm.fieldErrors()).isEmpty();
    }

    @Test
    void constructor_withFieldErrors_preservesList() {
        List<String> errors = List.of("field required");
        ErrorVm vm = new ErrorVm("400", "Bad request", "detail", errors);

        assertThat(vm.fieldErrors()).containsExactly("field required");
    }
}
