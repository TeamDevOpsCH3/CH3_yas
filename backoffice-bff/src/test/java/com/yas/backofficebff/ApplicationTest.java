package com.yas.backofficebff;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;

//Trigger pipeline
@SpringBootTest
class ApplicationTest {

    @MockitoBean
    ReactiveClientRegistrationRepository clientRegistrationRepository;

    @Test
    void contextLoads() {
        // Verify Spring application context loads successfully
    }
}
