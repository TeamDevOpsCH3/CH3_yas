package com.yas.order.viewmodel.checkout;

import static org.assertj.core.api.Assertions.assertThat;

import com.yas.order.model.enumeration.CheckoutState;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CheckoutViewModelTest {

    @Test
    void checkoutPostVm_exposesFields() {
        CheckoutItemPostVm item = new CheckoutItemPostVm(1L, "desc", 2);
        CheckoutPostVm postVm = new CheckoutPostVm(
            "user@example.com",
            "note",
            "PROMO",
            "SHIP",
            "PAY",
            "addr",
            List.of(item)
        );

        assertThat(postVm.email()).isEqualTo("user@example.com");
        assertThat(postVm.checkoutItemPostVms()).hasSize(1);
    }

    @Test
    void checkoutVm_builder_setsValues() {
        CheckoutItemVm itemVm = CheckoutItemVm.builder()
            .id(1L)
            .productId(10L)
            .productName("Product")
            .quantity(1)
            .productPrice(new BigDecimal("9.99"))
            .checkoutId("c1")
            .build();

        CheckoutVm vm = CheckoutVm.builder()
            .id("c1")
            .email("user@example.com")
            .checkoutState(CheckoutState.PENDING)
            .checkoutItemVms(Set.of(itemVm))
            .build();

        assertThat(vm.checkoutItemVms()).hasSize(1);
        assertThat(vm.checkoutState()).isEqualTo(CheckoutState.PENDING);
    }

    @Test
    void checkoutStatusAndPaymentVm_exposeFields() {
        CheckoutStatusPutVm statusVm = new CheckoutStatusPutVm("c1", "PENDING");
        CheckoutPaymentMethodPutVm paymentVm = new CheckoutPaymentMethodPutVm("pm-1");

        assertThat(statusVm.checkoutId()).isEqualTo("c1");
        assertThat(paymentVm.paymentMethodId()).isEqualTo("pm-1");
    }
}
