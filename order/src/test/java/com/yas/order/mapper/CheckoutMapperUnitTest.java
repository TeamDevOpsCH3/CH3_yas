package com.yas.order.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.yas.order.model.Checkout;
import com.yas.order.model.CheckoutItem;
import com.yas.order.viewmodel.checkout.CheckoutItemPostVm;
import com.yas.order.viewmodel.checkout.CheckoutItemVm;
import com.yas.order.viewmodel.checkout.CheckoutPostVm;
import com.yas.order.viewmodel.checkout.CheckoutVm;
import java.math.BigDecimal;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CheckoutMapperUnitTest {

    private final CheckoutMapper mapper = new CheckoutMapperImpl();

    @Test
    void toModel_whenCheckoutItemNull_returnsNull() {
        assertThat(mapper.toModel((CheckoutItemPostVm) null)).isNull();
    }

    @Test
    void toModel_whenCheckoutNull_returnsNull() {
        assertThat(mapper.toModel((CheckoutPostVm) null)).isNull();
    }

    @Test
    void toVm_whenCheckoutItemNull_returnsNull() {
        assertThat(mapper.toVm((CheckoutItem) null)).isNull();
    }

    @Test
    void toVm_whenCheckoutNull_returnsNull() {
        assertThat(mapper.toVm((Checkout) null)).isNull();
    }

    @Test
    void toVm_whenCheckoutItemHasNullCheckout_setsCheckoutIdNull() {
        CheckoutItem item = CheckoutItem.builder()
            .id(1L)
            .productId(10L)
            .checkout(null)
            .build();

        CheckoutItemVm vm = mapper.toVm(item);

        assertThat(vm.checkoutId()).isNull();
        assertThat(vm.productId()).isEqualTo(10L);
    }

    @Test
    void toVm_whenCheckoutHasItems_keepsItemVmsNull() {
        Checkout checkout = Checkout.builder()
            .id("c1")
            .email("user@example.com")
            .checkoutItems(java.util.List.of(CheckoutItem.builder().id(2L).build()))
            .build();

        CheckoutVm vm = mapper.toVm(checkout);

        assertThat(vm.id()).isEqualTo("c1");
        assertThat(vm.checkoutItemVms()).isNull();
    }

    @ParameterizedTest
    @MethodSource("amounts")
    void map_whenValueNullOrPresent_returnsExpected(BigDecimal input, BigDecimal expected) {
        assertThat(mapper.map(input)).isEqualByComparingTo(expected);
    }

    private static Stream<Arguments> amounts() {
        return Stream.of(
            Arguments.of(null, BigDecimal.ZERO),
            Arguments.of(new BigDecimal("12.50"), new BigDecimal("12.50"))
        );
    }
}
