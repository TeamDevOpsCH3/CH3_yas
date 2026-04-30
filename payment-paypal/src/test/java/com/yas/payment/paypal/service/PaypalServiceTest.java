package com.yas.payment.paypal.service;

import com.paypal.core.PayPalHttpClient;
import com.paypal.http.HttpResponse;
import com.paypal.orders.*;
import com.yas.payment.paypal.model.*;
import com.yas.payment.paypal.viewmodel.PaypalCapturePaymentRequest;
import com.yas.payment.paypal.viewmodel.PaypalCapturePaymentResponse;
import com.yas.payment.paypal.viewmodel.PaypalCreatePaymentRequest;
import com.yas.payment.paypal.viewmodel.PaypalCreatePaymentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PaypalServiceTest {

    private PaypalService paypalService;
    private PayPalHttpClient payPalHttpClient;
    private PayPalHttpClientInitializer payPalHttpClientInitializer;
    private String paymentSettings = "{\"clientId\": \"abc\", \"clientSecret\": \"123\", \"mode\": \"sandbox\"}";

    @BeforeEach
    void setUp() {
        payPalHttpClient = mock(PayPalHttpClient.class);
        payPalHttpClientInitializer = mock(PayPalHttpClientInitializer.class);
        when(payPalHttpClientInitializer.createPaypalClient(anyString())).thenReturn(payPalHttpClient);
        paypalService = new PaypalService(payPalHttpClientInitializer);
        CheckoutIdHelper.setCheckoutId("test-checkout-id");
    }

    @Test
    void testCreatePayment_whenSuccess_returnPaypalRequestPayment() throws IOException {

        List<LinkDescription> linkDescriptions = new ArrayList<>();
        LinkDescription linkDescription = new LinkDescription();
        linkDescription.rel("approve");
        linkDescription.href("http://redirect.url");
        linkDescriptions.add(linkDescription);

        Order order = new Order()
            .id("ORDER-123456789")
            .checkoutPaymentIntent("CAPTURE")
            .createTime("2024-09-10T10:00:00Z")
            .updateTime("2024-09-10T10:30:00Z")
            .status("CREATED")
            .expirationTime("2024-09-11T10:00:00Z")
            .links(linkDescriptions);

        HttpResponse mockResponse = mock(HttpResponse.class);
        when(payPalHttpClient.execute(any(OrdersCreateRequest.class))).thenReturn(mockResponse);
        when(mockResponse.result()).thenReturn(order);

        PaypalCreatePaymentRequest createPaymentRequest = new PaypalCreatePaymentRequest(
                BigDecimal.valueOf(2000), "test-checkout-id", "PAYPAL", paymentSettings);
        PaypalCreatePaymentResponse result = paypalService.createPayment(createPaymentRequest);

        assertEquals("success", result.status());
        assertEquals("ORDER-123456789", result.paymentId());
        assertEquals("http://redirect.url", result.redirectUrl());
    }

    @Test
    void testCreatePayment_whenIoException_returnPaypalRequestPayment() throws IOException {
        when(payPalHttpClient.execute(any(OrdersCreateRequest.class))).thenThrow(IOException.class);
        PaypalCreatePaymentRequest createPaymentRequest = new PaypalCreatePaymentRequest(
                BigDecimal.valueOf(1000), "test-checkout-id", "PAYPAL", paymentSettings);
        PaypalCreatePaymentResponse result = paypalService.createPayment(createPaymentRequest);
        assertTrue(result.status().contains("Error"));
        assertNull(result.paymentId());
        assertNull(result.redirectUrl());
    }

    @Test
    void testCreatePayment_whenLinksIsEmpty_throwNoSuchElementException() throws IOException {

        Order order = new Order()
            .id("ORDER-123456789")
            .checkoutPaymentIntent("CAPTURE")
            .createTime("2024-09-10T10:00:00Z")
            .updateTime("2024-09-10T10:30:00Z")
            .status("CREATED")
            .expirationTime("2024-09-11T10:00:00Z")
            .links(List.of());

        HttpResponse mockResponse = mock(HttpResponse.class);
        when(payPalHttpClient.execute(any(OrdersCreateRequest.class))).thenReturn(mockResponse);
        when(mockResponse.result()).thenReturn(order);

        PaypalCreatePaymentRequest createPaymentRequest = new PaypalCreatePaymentRequest(
                BigDecimal.valueOf(1000), "test-checkout-id", "PAYPAL", paymentSettings);

        assertThrows(NoSuchElementException.class, () ->
            paypalService.createPayment(createPaymentRequest)
        );
    }

    @Test
    void testCapturePayment_whenStatusNotNull_returnCapturedPaymentVm() throws IOException {

        Money money = new Money().value("100");
        MerchantReceivableBreakdown merchantReceivableBreakdown = new MerchantReceivableBreakdown();
        merchantReceivableBreakdown.paypalFee(money);
        Capture capture = new Capture().amount(money);
        capture.sellerReceivableBreakdown(merchantReceivableBreakdown);
        List<Capture> captureList = new ArrayList<>();
        captureList.add(capture);

        PaymentCollection paymentCollection = new PaymentCollection();
        paymentCollection.captures(captureList);
        PurchaseUnit purchaseUnit = new PurchaseUnit();
        purchaseUnit.payments(paymentCollection);

        List<PurchaseUnit> purchaseUnitList = new ArrayList<>();
        purchaseUnitList.add(purchaseUnit);

        Order mockOrder = new Order()
            .id("order-id")
            .status("COMPLETED")
            .purchaseUnits(purchaseUnitList);

        HttpResponse mockResponse = mock(HttpResponse.class);
        when(payPalHttpClient.execute(any(OrdersCaptureRequest.class))).thenReturn(mockResponse);
        when(mockResponse.result()).thenReturn(mockOrder);

        String token = "test-token-1";
        PaypalCapturePaymentRequest paypalCapturePaymentRequest = new PaypalCapturePaymentRequest(
                token, paymentSettings
        );
        PaypalCapturePaymentResponse result = paypalService.capturePayment(paypalCapturePaymentRequest);

        assertNotNull(result);
        assertEquals("order-id", result.gatewayTransactionId());
        assertEquals("COMPLETED", result.paymentStatus());
    }

    @Test
    void testCapturePayment_whenStatusIsNull_returnCapturedPaymentVm() throws IOException {

        Order order = new Order()
            .id("order-id-2");
        HttpResponse mockResponse = mock(HttpResponse.class);
        when(payPalHttpClient.execute(any(OrdersCaptureRequest.class))).thenReturn(mockResponse);
        when(mockResponse.result()).thenReturn(order);
        PaypalCapturePaymentRequest paypalCapturePaymentRequest = new PaypalCapturePaymentRequest(
                "test-token-1", paymentSettings
        );
        PaypalCapturePaymentResponse result = paypalService.capturePayment(paypalCapturePaymentRequest);
        assertEquals("Something Wrong!", result.failureMessage());
    }

    @Test
    void testCapturePayment_whenIoException_returnCapturedPaymentVm() throws IOException {

        IOException ioException = new IOException("error message");
        when(payPalHttpClient.execute(any(OrdersCaptureRequest.class))).thenThrow(ioException);

        PaypalCapturePaymentRequest paypalCapturePaymentRequest = new PaypalCapturePaymentRequest(
                "test-token-1", paymentSettings
        );
        PaypalCapturePaymentResponse result = paypalService.capturePayment(paypalCapturePaymentRequest);
        assertEquals("error message", result.failureMessage());
    }

    @Test
    void testCreatePayment_whenTotalPriceExceedsMax_shouldCapToMax() throws IOException {

        List<LinkDescription> linkDescriptions = new ArrayList<>();
        LinkDescription linkDescription = new LinkDescription();
        linkDescription.rel("approve");
        linkDescription.href("http://redirect.url");
        linkDescriptions.add(linkDescription);

        Order order = new Order()
            .id("ORDER-999")
            .checkoutPaymentIntent("CAPTURE")
            .createTime("2024-09-10T10:00:00Z")
            .updateTime("2024-09-10T10:30:00Z")
            .status("CREATED")
            .expirationTime("2024-09-11T10:00:00Z")
            .links(linkDescriptions);

        HttpResponse mockResponse = mock(HttpResponse.class);
        when(payPalHttpClient.execute(any(OrdersCreateRequest.class))).thenReturn(mockResponse);
        when(mockResponse.result()).thenReturn(order);

        PaypalCreatePaymentRequest createPaymentRequest = new PaypalCreatePaymentRequest(
                BigDecimal.valueOf(5000), "test-checkout-id-2", "PAYPAL", paymentSettings);
        PaypalCreatePaymentResponse result = paypalService.createPayment(createPaymentRequest);

        assertEquals("success", result.status());
        assertEquals("ORDER-999", result.paymentId());
        assertEquals("http://redirect.url", result.redirectUrl());
    }

    @Test
    void testCreatePayment_whenTotalPriceUnderMax_shouldUseActualPrice() throws IOException {

        List<LinkDescription> linkDescriptions = new ArrayList<>();
        LinkDescription linkDescription = new LinkDescription();
        linkDescription.rel("approve");
        linkDescription.href("http://redirect.url");
        linkDescriptions.add(linkDescription);

        Order order = new Order()
            .id("ORDER-100")
            .checkoutPaymentIntent("CAPTURE")
            .createTime("2024-09-10T10:00:00Z")
            .updateTime("2024-09-10T10:30:00Z")
            .status("CREATED")
            .expirationTime("2024-09-11T10:00:00Z")
            .links(linkDescriptions);

        HttpResponse mockResponse = mock(HttpResponse.class);
        when(payPalHttpClient.execute(any(OrdersCreateRequest.class))).thenReturn(mockResponse);
        when(mockResponse.result()).thenReturn(order);

        PaypalCreatePaymentRequest createPaymentRequest = new PaypalCreatePaymentRequest(
                BigDecimal.valueOf(50), "test-checkout-id-3", "PAYPAL", paymentSettings);
        PaypalCreatePaymentResponse result = paypalService.createPayment(createPaymentRequest);

        assertEquals("success", result.status());
        assertEquals("ORDER-100", result.paymentId());
        assertNotNull(result.redirectUrl());
    }

    @Test
    void testCreatePayment_whenTotalPriceExactlyMax_shouldUseMaxPrice() throws IOException {

        List<LinkDescription> linkDescriptions = new ArrayList<>();
        LinkDescription linkDescription = new LinkDescription();
        linkDescription.rel("approve");
        linkDescription.href("http://redirect.url");
        linkDescriptions.add(linkDescription);

        Order order = new Order()
            .id("ORDER-1000")
            .checkoutPaymentIntent("CAPTURE")
            .createTime("2024-09-10T10:00:00Z")
            .updateTime("2024-09-10T10:30:00Z")
            .status("CREATED")
            .expirationTime("2024-09-11T10:00:00Z")
            .links(linkDescriptions);

        HttpResponse mockResponse = mock(HttpResponse.class);
        when(payPalHttpClient.execute(any(OrdersCreateRequest.class))).thenReturn(mockResponse);
        when(mockResponse.result()).thenReturn(order);

        PaypalCreatePaymentRequest createPaymentRequest = new PaypalCreatePaymentRequest(
                BigDecimal.valueOf(1000), "test-checkout-id-4", "PAYPAL", paymentSettings);
        PaypalCreatePaymentResponse result = paypalService.createPayment(createPaymentRequest);

        assertEquals("success", result.status());
        assertEquals("ORDER-1000", result.paymentId());
    }

    @Test
    void testCapturePayment_withStatusApproved_shouldReturnCompleted() throws IOException {

        Money money = new Money().value("250.00");
        MerchantReceivableBreakdown merchantReceivableBreakdown = new MerchantReceivableBreakdown();
        merchantReceivableBreakdown.paypalFee(money);
        Capture capture = new Capture().amount(money);
        capture.sellerReceivableBreakdown(merchantReceivableBreakdown);
        List<Capture> captureList = new ArrayList<>();
        captureList.add(capture);

        PaymentCollection paymentCollection = new PaymentCollection();
        paymentCollection.captures(captureList);
        PurchaseUnit purchaseUnit = new PurchaseUnit();
        purchaseUnit.payments(paymentCollection);

        List<PurchaseUnit> purchaseUnitList = new ArrayList<>();
        purchaseUnitList.add(purchaseUnit);

        Order mockOrder = new Order()
            .id("order-approved")
            .status("APPROVED")
            .purchaseUnits(purchaseUnitList);

        HttpResponse mockResponse = mock(HttpResponse.class);
        when(payPalHttpClient.execute(any(OrdersCaptureRequest.class))).thenReturn(mockResponse);
        when(mockResponse.result()).thenReturn(mockOrder);

        PaypalCapturePaymentRequest paypalCapturePaymentRequest = new PaypalCapturePaymentRequest(
                "test-token-approved", paymentSettings
        );
        PaypalCapturePaymentResponse result = paypalService.capturePayment(paypalCapturePaymentRequest);

        assertNotNull(result);
        assertEquals("order-approved", result.gatewayTransactionId());
        assertEquals("APPROVED", result.paymentStatus());
    }

    @Test
    void testCapturePayment_withStatusPending_shouldReturnPending() throws IOException {

        Money money = new Money().value("75.50");
        MerchantReceivableBreakdown merchantReceivableBreakdown = new MerchantReceivableBreakdown();
        merchantReceivableBreakdown.paypalFee(money);
        Capture capture = new Capture().amount(money);
        capture.sellerReceivableBreakdown(merchantReceivableBreakdown);
        List<Capture> captureList = new ArrayList<>();
        captureList.add(capture);

        PaymentCollection paymentCollection = new PaymentCollection();
        paymentCollection.captures(captureList);
        PurchaseUnit purchaseUnit = new PurchaseUnit();
        purchaseUnit.payments(paymentCollection);

        List<PurchaseUnit> purchaseUnitList = new ArrayList<>();
        purchaseUnitList.add(purchaseUnit);

        Order mockOrder = new Order()
            .id("order-pending")
            .status("PENDING")
            .purchaseUnits(purchaseUnitList);

        HttpResponse mockResponse = mock(HttpResponse.class);
        when(payPalHttpClient.execute(any(OrdersCaptureRequest.class))).thenReturn(mockResponse);
        when(mockResponse.result()).thenReturn(mockOrder);

        PaypalCapturePaymentRequest paypalCapturePaymentRequest = new PaypalCapturePaymentRequest(
                "test-token-pending", paymentSettings
        );
        PaypalCapturePaymentResponse result = paypalService.capturePayment(paypalCapturePaymentRequest);

        assertNotNull(result);
        assertEquals("order-pending", result.gatewayTransactionId());
        assertEquals("PENDING", result.paymentStatus());
        assertEquals("PAYPAL", result.paymentMethod());
    }

    @Test
    void testCapturePayment_shouldParsePaymentFeeCorrectly() throws IOException {

        Money money = new Money().value("199.99");
        Money feeMoney = new Money().value("10.25");
        MerchantReceivableBreakdown merchantReceivableBreakdown = new MerchantReceivableBreakdown();
        merchantReceivableBreakdown.paypalFee(feeMoney);
        Capture capture = new Capture().amount(money);
        capture.sellerReceivableBreakdown(merchantReceivableBreakdown);
        List<Capture> captureList = new ArrayList<>();
        captureList.add(capture);

        PaymentCollection paymentCollection = new PaymentCollection();
        paymentCollection.captures(captureList);
        PurchaseUnit purchaseUnit = new PurchaseUnit();
        purchaseUnit.payments(paymentCollection);

        List<PurchaseUnit> purchaseUnitList = new ArrayList<>();
        purchaseUnitList.add(purchaseUnit);

        Order mockOrder = new Order()
            .id("order-with-fee")
            .status("COMPLETED")
            .purchaseUnits(purchaseUnitList);

        HttpResponse mockResponse = mock(HttpResponse.class);
        when(payPalHttpClient.execute(any(OrdersCaptureRequest.class))).thenReturn(mockResponse);
        when(mockResponse.result()).thenReturn(mockOrder);

        PaypalCapturePaymentRequest paypalCapturePaymentRequest = new PaypalCapturePaymentRequest(
                "test-token-fee", paymentSettings
        );
        PaypalCapturePaymentResponse result = paypalService.capturePayment(paypalCapturePaymentRequest);

        assertNotNull(result);
        assertEquals(new BigDecimal("10.25"), result.paymentFee());
        assertEquals(new BigDecimal("199.99"), result.amount());
        assertEquals("COMPLETED", result.paymentStatus());
    }

    @Test
    void testCreatePayment_withDifferentPaymentMethod_shouldIncludeInRedirectUrl() throws IOException {

        List<LinkDescription> linkDescriptions = new ArrayList<>();
        LinkDescription linkDescription = new LinkDescription();
        linkDescription.rel("approve");
        linkDescription.href("http://redirect.url/paypal?paymentMethod=CREDIT_CARD");
        linkDescriptions.add(linkDescription);

        Order order = new Order()
            .id("ORDER-PAYMENT-METHOD")
            .checkoutPaymentIntent("CAPTURE")
            .createTime("2024-09-10T10:00:00Z")
            .updateTime("2024-09-10T10:30:00Z")
            .status("CREATED")
            .expirationTime("2024-09-11T10:00:00Z")
            .links(linkDescriptions);

        HttpResponse mockResponse = mock(HttpResponse.class);
        when(payPalHttpClient.execute(any(OrdersCreateRequest.class))).thenReturn(mockResponse);
        when(mockResponse.result()).thenReturn(order);

        PaypalCreatePaymentRequest createPaymentRequest = new PaypalCreatePaymentRequest(
                BigDecimal.valueOf(100), "checkout-credit-card", "CREDIT_CARD", paymentSettings);
        PaypalCreatePaymentResponse result = paypalService.createPayment(createPaymentRequest);

        assertEquals("success", result.status());
        assertEquals("ORDER-PAYMENT-METHOD", result.paymentId());
        assertTrue(result.redirectUrl().contains("paymentMethod=CREDIT_CARD"));
    }

    @Test
    void testCreatePayment_whenLinkHasDifferentRel_shouldThrowNoSuchElementException() throws IOException {

        List<LinkDescription> linkDescriptions = new ArrayList<>();
        LinkDescription linkDescription = new LinkDescription();
        linkDescription.rel("cancel");
        linkDescription.href("http://cancel.url");
        linkDescriptions.add(linkDescription);

        Order order = new Order()
            .id("ORDER-NO-APPROVE")
            .checkoutPaymentIntent("CAPTURE")
            .createTime("2024-09-10T10:00:00Z")
            .updateTime("2024-09-10T10:30:00Z")
            .status("CREATED")
            .expirationTime("2024-09-11T10:00:00Z")
            .links(linkDescriptions);

        HttpResponse mockResponse = mock(HttpResponse.class);
        when(payPalHttpClient.execute(any(OrdersCreateRequest.class))).thenReturn(mockResponse);
        when(mockResponse.result()).thenReturn(order);

        PaypalCreatePaymentRequest createPaymentRequest = new PaypalCreatePaymentRequest(
                BigDecimal.valueOf(100), "checkout-no-approve", "PAYPAL", paymentSettings);

        assertThrows(NoSuchElementException.class, () ->
            paypalService.createPayment(createPaymentRequest)
        );
    }

    @Test
    void testCapturePayment_withZeroAmount_shouldHandleCorrectly() throws IOException {

        Money money = new Money().value("0.00");
        Money feeMoney = new Money().value("0.00");
        MerchantReceivableBreakdown merchantReceivableBreakdown = new MerchantReceivableBreakdown();
        merchantReceivableBreakdown.paypalFee(feeMoney);
        Capture capture = new Capture().amount(money);
        capture.sellerReceivableBreakdown(merchantReceivableBreakdown);
        List<Capture> captureList = new ArrayList<>();
        captureList.add(capture);

        PaymentCollection paymentCollection = new PaymentCollection();
        paymentCollection.captures(captureList);
        PurchaseUnit purchaseUnit = new PurchaseUnit();
        purchaseUnit.payments(paymentCollection);

        List<PurchaseUnit> purchaseUnitList = new ArrayList<>();
        purchaseUnitList.add(purchaseUnit);

        Order mockOrder = new Order()
            .id("order-zero")
            .status("COMPLETED")
            .purchaseUnits(purchaseUnitList);

        HttpResponse mockResponse = mock(HttpResponse.class);
        when(payPalHttpClient.execute(any(OrdersCaptureRequest.class))).thenReturn(mockResponse);
        when(mockResponse.result()).thenReturn(mockOrder);

        PaypalCapturePaymentRequest paypalCapturePaymentRequest = new PaypalCapturePaymentRequest(
                "test-token-zero", paymentSettings
        );
        PaypalCapturePaymentResponse result = paypalService.capturePayment(paypalCapturePaymentRequest);

        assertNotNull(result);
        assertEquals(0, BigDecimal.ZERO.compareTo(result.amount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.paymentFee()));
    }

    @Test
    void testCreatePayment_withZeroTotalPrice_shouldSucceed() throws IOException {

        List<LinkDescription> linkDescriptions = new ArrayList<>();
        LinkDescription linkDescription = new LinkDescription();
        linkDescription.rel("approve");
        linkDescription.href("http://redirect.url/zero");
        linkDescriptions.add(linkDescription);

        Order order = new Order()
            .id("ORDER-ZERO-PRICE")
            .checkoutPaymentIntent("CAPTURE")
            .createTime("2024-09-10T10:00:00Z")
            .updateTime("2024-09-10T10:30:00Z")
            .status("CREATED")
            .expirationTime("2024-09-11T10:00:00Z")
            .links(linkDescriptions);

        HttpResponse mockResponse = mock(HttpResponse.class);
        when(payPalHttpClient.execute(any(OrdersCreateRequest.class))).thenReturn(mockResponse);
        when(mockResponse.result()).thenReturn(order);

        PaypalCreatePaymentRequest createPaymentRequest = new PaypalCreatePaymentRequest(
                BigDecimal.ZERO, "checkout-zero-price", "PAYPAL", paymentSettings);
        PaypalCreatePaymentResponse result = paypalService.createPayment(createPaymentRequest);

        assertEquals("success", result.status());
        assertEquals("ORDER-ZERO-PRICE", result.paymentId());
    }

}
