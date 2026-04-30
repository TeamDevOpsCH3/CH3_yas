package com.yas.order.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.yas.order.model.csv.OrderItemCsv;
import com.yas.order.model.enumeration.DeliveryStatus;
import com.yas.order.model.enumeration.OrderStatus;
import com.yas.order.model.enumeration.PaymentStatus;
import com.yas.order.viewmodel.order.OrderBriefVm;
import com.yas.order.viewmodel.orderaddress.OrderAddressVm;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

class OrderMapperTest {

    private final OrderMapper orderMapper = new OrderMapperImpl();

    @Test
    void toCsv_whenNull_returnsNull() {
        assertThat(orderMapper.toCsv(null)).isNull();
    }

    @Test
    void toCsv_whenBillingAddressNull_mapsFieldsAndLeavesPhoneNull() {
        OrderBriefVm order = OrderBriefVm.builder()
            .id(1L)
            .email("user@example.com")
            .billingAddressVm(null)
            .totalPrice(new BigDecimal("10.00"))
            .orderStatus(OrderStatus.PENDING)
            .deliveryStatus(DeliveryStatus.PREPARING)
            .paymentStatus(PaymentStatus.PENDING)
            .createdOn(ZonedDateTime.now())
            .build();

        OrderItemCsv csv = orderMapper.toCsv(order);

        assertThat(csv.getId()).isEqualTo(1L);
        assertThat(csv.getEmail()).isEqualTo("user@example.com");
        assertThat(csv.getPhone()).isNull();
        assertThat(csv.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(csv.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(csv.getDeliveryStatus()).isEqualTo(DeliveryStatus.PREPARING);
        assertThat(csv.getTotalPrice()).isEqualTo(new BigDecimal("10.00"));
    }

    @Test
    void toCsv_whenBillingAddressPresent_mapsPhone() {
        OrderAddressVm billingAddress = OrderAddressVm.builder()
            .id(10L)
            .phone("123")
            .build();

        OrderBriefVm order = OrderBriefVm.builder()
            .id(2L)
            .email("user2@example.com")
            .billingAddressVm(billingAddress)
            .totalPrice(new BigDecimal("5.00"))
            .orderStatus(OrderStatus.COMPLETED)
            .deliveryStatus(DeliveryStatus.DELIVERED)
            .paymentStatus(PaymentStatus.COMPLETED)
            .createdOn(ZonedDateTime.now())
            .build();

        OrderItemCsv csv = orderMapper.toCsv(order);

        assertThat(csv.getPhone()).isEqualTo("123");
        assertThat(csv.getEmail()).isEqualTo("user2@example.com");
        assertThat(csv.getOrderStatus()).isEqualTo(OrderStatus.COMPLETED);
    }
}
