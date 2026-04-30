package com.yas.payment.paypal.service;

import com.paypal.core.PayPalHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PayPalHttpClientInitializerTest {

    private PayPalHttpClientInitializer initializer;

    @BeforeEach
    void setUp() {
        initializer = new PayPalHttpClientInitializer();
    }

    @Test
    void createPaypalClient_withSandboxMode_returnsClient() {
        String settings = "{\"clientId\":\"test-client-id\",\"clientSecret\":\"test-client-secret\",\"mode\":\"sandbox\"}";
        PayPalHttpClient client = initializer.createPaypalClient(settings);
        
        assertNotNull(client);
    }

    @Test
    void createPaypalClient_withLiveMode_returnsClient() {
        String settings = "{\"clientId\":\"test-client-id\",\"clientSecret\":\"test-client-secret\",\"mode\":\"live\"}";
        PayPalHttpClient client = initializer.createPaypalClient(settings);
        
        assertNotNull(client);
    }

    @Test
    void createPaypalClient_withNullSettings_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> initializer.createPaypalClient(null));
    }
}
