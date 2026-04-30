package com.yas.order.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.BadRequestException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.order.model.enumeration.OrderStatus;
import com.yas.order.model.request.OrderRequest;
import com.yas.order.service.OrderService;
import com.yas.order.viewmodel.order.OrderBriefVm;
import com.yas.order.viewmodel.order.OrderExistsByProductAndUserGetVm;
import com.yas.order.viewmodel.order.OrderGetVm;
import com.yas.order.viewmodel.order.OrderListVm;
import com.yas.order.viewmodel.order.OrderPostVm;
import com.yas.order.viewmodel.order.OrderVm;
import com.yas.order.viewmodel.order.PaymentOrderStatusVm;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class OrderControllerUnitTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;

    @Test
    void createOrder_returnsResponseEntity() {
        OrderVm orderVm = OrderVm.builder().id(1L).build();
        when(orderService.createOrder(any(OrderPostVm.class))).thenReturn(orderVm);

        var response = orderController.createOrder(OrderPostVm.builder().build());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1L, response.getBody().id());
    }

    @Test
    void updateOrderPaymentStatus_returnsResponseEntity() {
        PaymentOrderStatusVm vm = PaymentOrderStatusVm.builder().orderId(1L).build();
        when(orderService.updateOrderPaymentStatus(vm)).thenReturn(vm);

        var response = orderController.updateOrderPaymentStatus(vm);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1L, response.getBody().orderId());
    }

    @Test
    void checkOrderExistsByProductId_returnsResponseEntity() {
        when(orderService.isOrderCompletedWithUserIdAndProductId(1L))
            .thenReturn(new OrderExistsByProductAndUserGetVm(true));

        var response = orderController.checkOrderExistsByProductIdAndUserIdWithStatus(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(true, response.getBody().isPresent());
    }

    @Test
    void getMyOrders_returnsResponseEntity() {
        when(orderService.getMyOrders("p1", OrderStatus.PENDING))
            .thenReturn(List.of(new OrderGetVm(1L, OrderStatus.PENDING, null, null, null, null, null)));

        var response = orderController.getMyOrders("p1", OrderStatus.PENDING);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1L, response.getBody().getFirst().id());
    }

    @Test
    void getOrderWithItemsById_returnsResponseEntity() {
        when(orderService.getOrderWithItemsById(2L)).thenReturn(OrderVm.builder().id(2L).build());

        var response = orderController.getOrderWithItemsById(2L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2L, response.getBody().id());
    }

    @Test
    void getOrderWithCheckoutId_returnsResponseEntity() {
        when(orderService.findOrderVmByCheckoutId("c1"))
            .thenReturn(new OrderGetVm(3L, OrderStatus.PENDING, null, null, null, null, null));

        var response = orderController.getOrderWithCheckoutId("c1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(3L, response.getBody().id());
    }

    @Test
    void getOrders_returnsResponseEntity() {
        OrderListVm listVm = new OrderListVm(List.of(OrderBriefVm.builder().id(1L).build()), 1, 1);
        when(orderService.getAllOrder(any(), any(), any(), any(), any(), any())).thenReturn(listVm);

        var response = orderController.getOrders(
            null,
            null,
            "",
            List.of(),
            "",
            "",
            "",
            0,
            10
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().totalElements());
    }

    @Test
    void getLatestOrders_returnsResponseEntity() {
        when(orderService.getLatestOrders(5)).thenReturn(List.of(OrderBriefVm.builder().id(1L).build()));

        var response = orderController.getLatestOrders(5);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1L, response.getBody().getFirst().id());
    }

    @Test
    void exportCsv_returnsFileResponse() throws IOException {
        when(orderService.exportCsv(any(OrderRequest.class))).thenReturn(new byte[] {1, 2, 3});

        var response = orderController.exportCsv(OrderRequest.builder().build());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(3, response.getBody().length);
    }

    @Test
    void createOrder_whenServiceThrowsBadRequest_propagates() {
        when(orderService.createOrder(any(OrderPostVm.class))).thenThrow(new BadRequestException("bad"));

        assertThrows(BadRequestException.class, () -> orderController.createOrder(OrderPostVm.builder().build()));
    }

    @Test
    void getOrderWithItemsById_whenNotFound_propagates() {
        when(orderService.getOrderWithItemsById(9L)).thenThrow(new NotFoundException("ORDER_NOT_FOUND"));

        assertThrows(NotFoundException.class, () -> orderController.getOrderWithItemsById(9L));
    }

    @Test
    void exportCsv_whenServiceThrowsRuntime_propagates() throws IOException {
        when(orderService.exportCsv(any(OrderRequest.class))).thenThrow(new RuntimeException("boom"));

        assertThrows(RuntimeException.class, () -> orderController.exportCsv(OrderRequest.builder().build()));
    }
}
