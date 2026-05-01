package com.yas.payment.controller;

import com.yas.payment.model.enumeration.PaymentMethod;
import com.yas.payment.model.enumeration.PaymentStatus;
import com.yas.payment.service.PaymentService;
import com.yas.payment.viewmodel.CapturePaymentRequestVm;
import com.yas.payment.viewmodel.CapturePaymentResponseVm;
import com.yas.payment.viewmodel.InitPaymentRequestVm;
import com.yas.payment.viewmodel.InitPaymentResponseVm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController paymentController;

    private InitPaymentRequestVm initPaymentRequestVm;
    private InitPaymentResponseVm initPaymentResponseVm;
    private CapturePaymentRequestVm capturePaymentRequestVm;
    private CapturePaymentResponseVm capturePaymentResponseVm;

    @BeforeEach
    void setUp() {
        initPaymentRequestVm = InitPaymentRequestVm.builder()
                .paymentMethod("PAYPAL")
                .totalPrice(BigDecimal.TEN)
                .checkoutId("checkout-123")
                .build();

        initPaymentResponseVm = InitPaymentResponseVm.builder()
                .status("success")
                .paymentId("pay-123")
                .redirectUrl("http://example.com/redirect")
                .build();

        capturePaymentRequestVm = CapturePaymentRequestVm.builder()
                .paymentMethod("PAYPAL")
                .token("token-123")
                .build();

        capturePaymentResponseVm = CapturePaymentResponseVm.builder()
                .orderId(1L)
                .checkoutId("checkout-123")
                .amount(BigDecimal.TEN)
                .paymentFee(BigDecimal.ZERO)
                .gatewayTransactionId("gw-123")
                .paymentMethod(PaymentMethod.PAYPAL)
                .paymentStatus(PaymentStatus.COMPLETED)
                .failureMessage(null)
                .build();
    }

    @Test
    void initPayment_Success() {
        when(paymentService.initPayment(any(InitPaymentRequestVm.class))).thenReturn(initPaymentResponseVm);

        InitPaymentResponseVm response = paymentController.initPayment(initPaymentRequestVm);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("success");
        assertThat(response.paymentId()).isEqualTo("pay-123");
        assertThat(response.redirectUrl()).isEqualTo("http://example.com/redirect");
    }

    @Test
    void capturePayment_Success() {
        when(paymentService.capturePayment(any(CapturePaymentRequestVm.class))).thenReturn(capturePaymentResponseVm);

        CapturePaymentResponseVm response = paymentController.capturePayment(capturePaymentRequestVm);

        assertThat(response).isNotNull();
        assertThat(response.orderId()).isEqualTo(1L);
        assertThat(response.checkoutId()).isEqualTo("checkout-123");
        assertThat(response.paymentMethod()).isEqualTo(PaymentMethod.PAYPAL);
        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
    }

    @Test
    void cancelPayment_Success() {
        ResponseEntity<String> response = paymentController.cancelPayment();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("Payment cancelled");
    }

    @Test
    void initPayment_WithNullResponse_ShouldReturnNullFields() {
        when(paymentService.initPayment(any(InitPaymentRequestVm.class))).thenReturn(
                InitPaymentResponseVm.builder()
                        .status(null)
                        .paymentId(null)
                        .redirectUrl(null)
                        .build()
        );

        InitPaymentResponseVm response = paymentController.initPayment(initPaymentRequestVm);

        assertThat(response).isNotNull();
        assertThat(response.status()).isNull();
        assertThat(response.paymentId()).isNull();
        assertThat(response.redirectUrl()).isNull();
    }

    @Test
    void capturePayment_WithFailedStatus_ShouldReturnFailedResponse() {
        CapturePaymentResponseVm failedResponse = CapturePaymentResponseVm.builder()
                .orderId(1L)
                .checkoutId("checkout-123")
                .amount(BigDecimal.TEN)
                .paymentFee(BigDecimal.ZERO)
                .gatewayTransactionId("gw-123")
                .paymentMethod(PaymentMethod.PAYPAL)
                .paymentStatus(PaymentStatus.CANCELLED)
                .failureMessage("Payment cancelled")
                .build();
        when(paymentService.capturePayment(any(CapturePaymentRequestVm.class))).thenReturn(failedResponse);

        CapturePaymentResponseVm response = paymentController.capturePayment(capturePaymentRequestVm);

        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(response.failureMessage()).isEqualTo("Payment declined");
    }
}
