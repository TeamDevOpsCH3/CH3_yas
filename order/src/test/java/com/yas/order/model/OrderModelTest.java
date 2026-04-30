package com.yas.order.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.yas.order.model.enumeration.CheckoutState;
import com.yas.order.model.enumeration.DeliveryMethod;
import com.yas.order.model.enumeration.DeliveryStatus;
import com.yas.order.model.enumeration.OrderStatus;
import com.yas.order.model.enumeration.PaymentStatus;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderModelTest {

    @Test
    void orderBuilder_setsFields() {
        OrderAddress address = OrderAddress.builder()
            .id(1L)
            .contactName("John")
            .phone("123")
            .addressLine1("Main")
            .city("City")
            .zipCode("00000")
            .districtId(1L)
            .districtName("District")
            .stateOrProvinceId(2L)
            .stateOrProvinceName("State")
            .countryId(3L)
            .countryName("Country")
            .build();

        Order order = Order.builder()
            .id(10L)
            .email("user@example.com")
            .shippingAddressId(address)
            .billingAddressId(address)
            .note("Note")
            .tax(1.0f)
            .discount(0.5f)
            .numberItem(2)
            .couponCode("PROMO")
            .totalPrice(new BigDecimal("20.00"))
            .deliveryFee(new BigDecimal("2.00"))
            .orderStatus(OrderStatus.PENDING)
            .deliveryMethod(DeliveryMethod.GRAB_EXPRESS)
            .deliveryStatus(DeliveryStatus.PREPARING)
            .paymentStatus(PaymentStatus.PENDING)
            .checkoutId("checkout-1")
            .build();

        assertThat(order.getId()).isEqualTo(10L);
        assertThat(order.getBillingAddressId().getCountryName()).isEqualTo("Country");
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void checkoutBuilder_defaultsTotalsToZero() {
        Checkout checkout = Checkout.builder()
            .id("c1")
            .email("user@example.com")
            .checkoutState(CheckoutState.PENDING)
            .build();

        assertThat(checkout.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(checkout.getTotalShipmentFee()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(checkout.getTotalShipmentTax()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(checkout.getTotalDiscountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void checkoutBuilder_defaultsItemsToEmptyList() {
        Checkout checkout = Checkout.builder().id("c2").build();

        assertThat(checkout.getCheckoutItems()).isNotNull();
        assertThat(checkout.getCheckoutItems()).isEmpty();
    }
}
