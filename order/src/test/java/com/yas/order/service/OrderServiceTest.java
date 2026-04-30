package com.yas.order.service;

import static com.yas.order.utils.SecurityContextUtils.setSubjectUpSecurityContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.order.mapper.OrderMapper;
import com.yas.order.model.Order;
import com.yas.order.model.OrderAddress;
import com.yas.order.model.csv.OrderItemCsv;
import com.yas.order.model.enumeration.DeliveryMethod;
import com.yas.order.model.enumeration.DeliveryStatus;
import com.yas.order.model.enumeration.OrderStatus;
import com.yas.order.model.enumeration.PaymentMethod;
import com.yas.order.model.enumeration.PaymentStatus;
import com.yas.order.model.request.OrderRequest;
import com.yas.order.repository.OrderItemRepository;
import com.yas.order.repository.OrderRepository;
import com.yas.order.viewmodel.order.OrderExistsByProductAndUserGetVm;
import com.yas.order.viewmodel.order.OrderItemPostVm;
import com.yas.order.viewmodel.order.OrderPostVm;
import com.yas.order.viewmodel.order.OrderVm;
import com.yas.order.viewmodel.order.PaymentOrderStatusVm;
import com.yas.order.viewmodel.orderaddress.OrderAddressPostVm;
import com.yas.order.viewmodel.promotion.PromotionUsageVm;
import com.yas.order.viewmodel.product.ProductVariationVm;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ProductService productService;

    @Mock
    private CartService cartService;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private PromotionService promotionService;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createOrder_happyPath_updatesDependencies() {
        setSubjectUpSecurityContext("user-1");

        OrderPostVm orderPostVm = buildOrderPostVm();
        Order savedOrder = buildOrder(10L);

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(savedOrder));
        when(orderItemRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(productService).subtractProductStockQuantity(any(OrderVm.class));
        doNothing().when(cartService).deleteCartItems(any(OrderVm.class));
        doNothing().when(promotionService).updateUsagePromotion(any());

        OrderVm result = orderService.createOrder(orderPostVm);

        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.email()).isEqualTo(orderPostVm.email());
        verify(productService).subtractProductStockQuantity(any(OrderVm.class));
        verify(cartService).deleteCartItems(any(OrderVm.class));

        ArgumentCaptor<List<PromotionUsageVm>> usageCaptor = ArgumentCaptor.forClass(List.class);
        verify(promotionService).updateUsagePromotion(usageCaptor.capture());
        assertThat(usageCaptor.getValue()).hasSize(orderPostVm.orderItemPostVms().size());
    }

    @Test
    void getAllOrder_whenEmptyPage_returnsEmptyListVm() {
        when(orderRepository.findAll(argThat(Objects::nonNull), any(Pageable.class))).thenReturn(Page.empty());

        var result = orderService.getAllOrder(
            org.springframework.data.util.Pair.of(ZonedDateTime.now().minusDays(1), ZonedDateTime.now()),
            null,
            List.of(),
            org.springframework.data.util.Pair.of(null, null),
            null,
            org.springframework.data.util.Pair.of(0, 10)
        );

        assertThat(result.orderList()).isNull();
        assertThat(result.totalElements()).isZero();
        assertThat(result.totalPages()).isZero();
    }

    @Test
    void getLatestOrders_whenCountZero_returnsEmpty() {
        assertThat(orderService.getLatestOrders(0)).isEmpty();
    }

    @Test
    void getLatestOrders_whenRepositoryEmpty_returnsEmpty() {
        when(orderRepository.getLatestOrders(any(Pageable.class))).thenReturn(List.of());

        assertThat(orderService.getLatestOrders(3)).isEmpty();
    }

    @Test
    void getLatestOrders_whenRepositoryHasOrders_returnsList() {
        Order order = buildOrder(11L);
        when(orderRepository.getLatestOrders(any(Pageable.class))).thenReturn(List.of(order));

        assertThat(orderService.getLatestOrders(1)).hasSize(1);
    }

    @Test
    void isOrderCompletedWithUserIdAndProductId_whenNoVariations_returnsFalse() {
        setSubjectUpSecurityContext("user-1");
        when(productService.getProductVariations(1L)).thenReturn(List.of());
        when(orderRepository.findOne(argThat(Objects::nonNull))).thenReturn(Optional.empty());

        OrderExistsByProductAndUserGetVm result = orderService.isOrderCompletedWithUserIdAndProductId(1L);

        assertThat(result.isPresent()).isFalse();
    }

    @Test
    void isOrderCompletedWithUserIdAndProductId_whenVariationsFound_returnsTrue() {
        setSubjectUpSecurityContext("user-1");
        when(productService.getProductVariations(2L))
            .thenReturn(List.of(new ProductVariationVm(21L, "v1", "sku1")));
        when(orderRepository.findOne(argThat(Objects::nonNull))).thenReturn(Optional.of(buildOrder(20L)));

        OrderExistsByProductAndUserGetVm result = orderService.isOrderCompletedWithUserIdAndProductId(2L);

        assertThat(result.isPresent()).isTrue();
    }

    @Test
    void updateOrderPaymentStatus_completed_setsOrderPaid() {
        Order order = buildOrder(30L);
        order.setOrderStatus(OrderStatus.PENDING);
        when(orderRepository.findById(30L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        PaymentOrderStatusVm request = PaymentOrderStatusVm.builder()
            .orderId(30L)
            .paymentId(999L)
            .paymentStatus(PaymentStatus.COMPLETED.name())
            .build();

        PaymentOrderStatusVm result = orderService.updateOrderPaymentStatus(request);

        assertThat(result.orderStatus()).isEqualTo(OrderStatus.PAID.getName());
    }

    @Test
    void updateOrderPaymentStatus_pending_keepsOrderStatus() {
        Order order = buildOrder(31L);
        order.setOrderStatus(OrderStatus.PENDING);
        when(orderRepository.findById(31L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        PaymentOrderStatusVm request = PaymentOrderStatusVm.builder()
            .orderId(31L)
            .paymentId(111L)
            .paymentStatus(PaymentStatus.PENDING.name())
            .build();

        PaymentOrderStatusVm result = orderService.updateOrderPaymentStatus(request);

        assertThat(result.orderStatus()).isEqualTo(OrderStatus.PENDING.getName());
    }

    @Test
    void rejectOrder_updatesStatusAndReason() {
        Order order = buildOrder(40L);
        when(orderRepository.findById(40L)).thenReturn(Optional.of(order));

        orderService.rejectOrder(40L, "Out of stock");

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.REJECT);
        assertThat(order.getRejectReason()).isEqualTo("Out of stock");
        verify(orderRepository).save(order);
    }

    @Test
    void acceptOrder_updatesStatus() {
        Order order = buildOrder(41L);
        when(orderRepository.findById(41L)).thenReturn(Optional.of(order));

        orderService.acceptOrder(41L);

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.ACCEPTED);
        verify(orderRepository).save(order);
    }

    @Test
    void findOrderByCheckoutId_notFound_throwsNotFound() {
        when(orderRepository.findByCheckoutId("missing")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> orderService.findOrderByCheckoutId("missing"));
    }

    @Test
    void exportCsv_whenOrderListNull_returnsCsvBytes() throws IOException {
        when(orderRepository.findAll(argThat(Objects::nonNull), any(Pageable.class))).thenReturn(Page.empty());

        OrderRequest request = OrderRequest.builder()
            .createdFrom(ZonedDateTime.now().minusDays(1))
            .createdTo(ZonedDateTime.now())
            .pageNo(0)
            .pageSize(10)
            .build();

        byte[] result = orderService.exportCsv(request);

        assertNotNull(result);
    }

    @Test
    void exportCsv_whenOrderListPresent_returnsCsvBytes() throws IOException {
        Order order = buildOrder(50L);
        Page<Order> page = new PageImpl<>(List.of(order));
        when(orderRepository.findAll(argThat(Objects::nonNull), any(Pageable.class))).thenReturn(page);
        when(orderMapper.toCsv(any())).thenReturn(OrderItemCsv.builder().build());

        OrderRequest request = OrderRequest.builder()
            .createdFrom(ZonedDateTime.now().minusDays(1))
            .createdTo(ZonedDateTime.now())
            .pageNo(0)
            .pageSize(10)
            .build();

        byte[] result = orderService.exportCsv(request);

        assertNotNull(result);
    }

    private OrderPostVm buildOrderPostVm() {
        OrderAddressPostVm address = OrderAddressPostVm.builder()
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

        OrderItemPostVm item = OrderItemPostVm.builder()
            .productId(100L)
            .productName("Product")
            .quantity(2)
            .productPrice(new BigDecimal("10.00"))
            .note("Note")
            .discountAmount(new BigDecimal("1.00"))
            .taxAmount(new BigDecimal("0.50"))
            .taxPercent(new BigDecimal("5.00"))
            .build();

        return OrderPostVm.builder()
            .checkoutId("checkout-1")
            .email("user@example.com")
            .shippingAddressPostVm(address)
            .billingAddressPostVm(address)
            .note("Note")
            .tax(1.5f)
            .discount(0.5f)
            .numberItem(1)
            .totalPrice(new BigDecimal("20.00"))
            .deliveryFee(new BigDecimal("2.00"))
            .couponCode("PROMO")
            .deliveryMethod(DeliveryMethod.GRAB_EXPRESS)
            .paymentMethod(PaymentMethod.COD)
            .paymentStatus(PaymentStatus.PENDING)
            .orderItemPostVms(List.of(item))
            .build();
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
            .shippingAddressId(address)
            .billingAddressId(address)
            .note("Note")
            .tax(1.5f)
            .discount(0.5f)
            .numberItem(1)
            .totalPrice(new BigDecimal("20.00"))
            .deliveryFee(new BigDecimal("2.00"))
            .couponCode("PROMO")
            .orderStatus(OrderStatus.PENDING)
            .deliveryMethod(DeliveryMethod.GRAB_EXPRESS)
            .deliveryStatus(DeliveryStatus.PREPARING)
            .paymentStatus(PaymentStatus.PENDING)
            .checkoutId("checkout-1")
            .build();
    }
}
