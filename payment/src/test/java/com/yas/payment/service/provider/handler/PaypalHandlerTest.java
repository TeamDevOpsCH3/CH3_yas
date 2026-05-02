package com.yas.payment.service.provider.handler;

import com.yas.payment.model.CapturedPayment;
import com.yas.payment.model.InitiatedPayment;
import com.yas.payment.model.enumeration.PaymentMethod;
import com.yas.payment.model.enumeration.PaymentStatus;
import com.yas.payment.paypal.service.PaypalService;
import com.yas.payment.paypal.viewmodel.PaypalCapturePaymentRequest;
import com.yas.payment.paypal.viewmodel.PaypalCapturePaymentResponse;
import com.yas.payment.paypal.viewmodel.PaypalCreatePaymentRequest;
import com.yas.payment.paypal.viewmodel.PaypalCreatePaymentResponse;
import com.yas.payment.service.PaymentProviderService;
import com.yas.payment.viewmodel.CapturePaymentRequestVm;
import com.yas.payment.viewmodel.InitPaymentRequestVm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaypalHandlerTest {

    @Mock
    private PaymentProviderService paymentProviderService;

    @Mock
    private PaypalService paypalService;

    @InjectMocks
    private PaypalHandler paypalHandler;

    private InitPaymentRequestVm initPaymentRequestVm;
    private PaypalCreatePaymentResponse paypalCreatePaymentResponse;
    private CapturePaymentRequestVm capturePaymentRequestVm;
    private PaypalCapturePaymentResponse paypalCapturePaymentResponse;

    @BeforeEach
    void setUp() {
        initPaymentRequestVm = InitPaymentRequestVm.builder()
                .paymentMethod("PAYPAL")
                .totalPrice(BigDecimal.TEN)
                .checkoutId("checkout-123")
                .build();

        paypalCreatePaymentResponse = new PaypalCreatePaymentResponse(
                "success", "pay-123", "http://redirect.url"
        );

        capturePaymentRequestVm = CapturePaymentRequestVm.builder()
                .paymentMethod("PAYPAL")
                .token("token-123")
                .build();

        paypalCapturePaymentResponse = new PaypalCapturePaymentResponse(
                "checkout-123",
                BigDecimal.TEN,
                BigDecimal.ZERO,
                "gw-123",
                "PAYPAL",
                "COMPLETED",
                null
        );
    }

    @Test
    void getProviderId_shouldReturnPaypal() {
        assertThat(paypalHandler.getProviderId()).isEqualTo(PaymentMethod.PAYPAL.name());
    }

    @Test
    void initPayment_shouldReturnInitiatedPayment() {
        when(paymentProviderService.getAdditionalSettingsByPaymentProviderId(PaymentMethod.PAYPAL.name()))
                .thenReturn("settings");
        when(paypalService.createPayment(any(PaypalCreatePaymentRequest.class)))
                .thenReturn(paypalCreatePaymentResponse);

        InitiatedPayment result = paypalHandler.initPayment(initPaymentRequestVm);

        assertThat(result.getStatus()).isEqualTo("success");
        assertThat(result.getPaymentId()).isEqualTo("pay-123");
        assertThat(result.getRedirectUrl()).isEqualTo("http://redirect.url");
    }

    @Test
    void capturePayment_shouldReturnCapturedPayment() {
        when(paymentProviderService.getAdditionalSettingsByPaymentProviderId(PaymentMethod.PAYPAL.name()))
                .thenReturn("settings");
        when(paypalService.capturePayment(any(PaypalCapturePaymentRequest.class)))
                .thenReturn(paypalCapturePaymentResponse);

        CapturedPayment result = paypalHandler.capturePayment(capturePaymentRequestVm);

        assertThat(result.getCheckoutId()).isEqualTo("checkout-123");
        assertThat(result.getAmount()).isEqualTo(BigDecimal.TEN);
        assertThat(result.getPaymentFee()).isEqualTo(BigDecimal.ZERO);
        assertThat(result.getGatewayTransactionId()).isEqualTo("gw-123");
        assertThat(result.getPaymentMethod()).isEqualTo(PaymentMethod.PAYPAL);
        assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(result.getFailureMessage()).isNull();
    }
}
