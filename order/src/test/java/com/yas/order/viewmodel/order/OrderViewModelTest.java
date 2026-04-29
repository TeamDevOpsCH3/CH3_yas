package com.yas.order.viewmodel.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.yas.order.model.Order;
import com.yas.order.model.OrderAddress;
import com.yas.order.model.OrderItem;
import com.yas.order.model.enumeration.DeliveryMethod;
import com.yas.order.model.enumeration.DeliveryStatus;
import com.yas.order.model.enumeration.OrderStatus;
import com.yas.order.model.enumeration.PaymentStatus;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class OrderViewModelTest {

    @Test
    void orderVmFromModel_mapsFieldsAndItems() {
        Order order = buildOrder(1L);
        OrderItem item = OrderItem.builder()
            .id(2L)
            .productId(10L)
            .productName("Product")
            .quantity(1)
            .productPrice(new BigDecimal("9.99"))
            .note("Note")
            .discountAmount(new BigDecimal("1.00"))
            .taxAmount(new BigDecimal("0.50"))
            .taxPercent(new BigDecimal("5.00"))
            .orderId(1L)
            .build();

        OrderVm vm = OrderVm.fromModel(order, Set.of(item));

        assertThat(vm.id()).isEqualTo(1L);
        assertThat(vm.orderItemVms()).hasSize(1);
        assertThat(vm.billingAddressVm().countryName()).isEqualTo("Country");
    }

    @Test
    void orderVmFromModel_whenItemsNull_keepsNullItems() {
        Order order = buildOrder(2L);

        OrderVm vm = OrderVm.fromModel(order, null);

        assertThat(vm.orderItemVms()).isNull();
    }

    @Test
    void orderGetVmFromModel_mapsItems() {
        Order order = buildOrder(3L);
        OrderItem item = OrderItem.builder().id(3L).productId(20L).productName("P").quantity(2).build();

        OrderGetVm vm = OrderGetVm.fromModel(order, Set.of(item));

        assertThat(vm.id()).isEqualTo(3L);
        assertThat(vm.orderItems()).hasSize(1);
    }

    @Test
    void orderItemGetVmFromModels_handlesEmpty() {
        assertThat(OrderItemGetVm.fromModels(null)).isEmpty();
        assertThat(OrderItemGetVm.fromModels(List.of())).isEmpty();
    }

    @Test
    void orderBriefVmFromModel_mapsFields() {
        Order order = buildOrder(4L);

        OrderBriefVm vm = OrderBriefVm.fromModel(order);

        assertThat(vm.id()).isEqualTo(4L);
        assertThat(vm.deliveryStatus()).isEqualTo(DeliveryStatus.PREPARING);
    }

    private Order buildOrder(Long id) {
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

        return Order.builder()
            .id(id)
            .email("user@example.com")
            .billingAddressId(address)
            .shippingAddressId(address)
            .note("Note")
            .tax(1.5f)
            .discount(0.5f)
            .numberItem(1)
            .totalPrice(new BigDecimal("20.00"))
            .couponCode("PROMO")
            .orderStatus(OrderStatus.PENDING)
            .deliveryMethod(DeliveryMethod.GRAB_EXPRESS)
            .deliveryStatus(DeliveryStatus.PREPARING)
            .paymentStatus(PaymentStatus.PENDING)
            .createdOn(ZonedDateTime.now())
            .build();
    }
}
