package com.yas.order.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.BadRequestException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.order.service.CheckoutService;
import com.yas.order.viewmodel.checkout.CheckoutPaymentMethodPutVm;
import com.yas.order.viewmodel.checkout.CheckoutPostVm;
import com.yas.order.viewmodel.checkout.CheckoutStatusPutVm;
import com.yas.order.viewmodel.checkout.CheckoutVm;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class CheckoutControllerUnitTest {

    @Mock
    private CheckoutService checkoutService;

    @InjectMocks
    private CheckoutController checkoutController;

    @Test
    void createCheckout_returnsResponseEntity() {
        CheckoutVm vm = CheckoutVm.builder().id("c1").build();
        when(checkoutService.createCheckout(any(CheckoutPostVm.class))).thenReturn(vm);

        var response = checkoutController.createCheckout(new CheckoutPostVm(
            "email",
            "note",
            "promo",
            "ship",
            "pay",
            "1",
            List.of()
        ));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("c1", response.getBody().id());
    }

    @Test
    void updateCheckoutStatus_returnsResponseEntity() {
        CheckoutStatusPutVm vm = new CheckoutStatusPutVm("id", "STATUS");
        when(checkoutService.updateCheckoutStatus(vm)).thenReturn(1L);

        var response = checkoutController.updateCheckoutStatus(vm);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1L, response.getBody());
    }

    @Test
    void getOrderWithItemsById_returnsResponseEntity() {
        when(checkoutService.getCheckoutPendingStateWithItemsById("id"))
            .thenReturn(CheckoutVm.builder().id("id").build());

        var response = checkoutController.getOrderWithItemsById("id");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("id", response.getBody().id());
    }

    @Test
    void updatePaymentMethod_returnsOk() {
        checkoutController.updatePaymentMethod("id", new CheckoutPaymentMethodPutVm("pay"));

        verify(checkoutService).updateCheckoutPaymentMethod("id", new CheckoutPaymentMethodPutVm("pay"));
    }

    @Test
    void updatePaymentMethod_whenNotFound_propagates() {
        doThrow(new NotFoundException("CHECKOUT_NOT_FOUND"))
            .when(checkoutService)
            .updateCheckoutPaymentMethod("id", new CheckoutPaymentMethodPutVm("pay"));

        assertThrows(NotFoundException.class,
            () -> checkoutController.updatePaymentMethod("id", new CheckoutPaymentMethodPutVm("pay")));
    }

    @Test
    void createCheckout_whenBadRequest_propagates() {
        when(checkoutService.createCheckout(any(CheckoutPostVm.class)))
            .thenThrow(new BadRequestException("bad"));

        assertThrows(BadRequestException.class, () -> checkoutController.createCheckout(new CheckoutPostVm(
            "email",
            "note",
            "promo",
            "ship",
            "pay",
            "1",
            List.of()
        )));
    }

    @Test
    void getOrderWithItemsById_whenRuntimeException_propagates() {
        when(checkoutService.getCheckoutPendingStateWithItemsById("id"))
            .thenThrow(new RuntimeException("boom"));

        assertThrows(RuntimeException.class, () -> checkoutController.getOrderWithItemsById("id"));
    }
}
