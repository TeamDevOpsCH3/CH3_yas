package com.yas.payment.controller;

import com.yas.payment.service.PaymentProviderService;
import com.yas.payment.viewmodel.paymentprovider.CreatePaymentVm;
import com.yas.payment.viewmodel.paymentprovider.PaymentProviderVm;
import com.yas.payment.viewmodel.paymentprovider.UpdatePaymentVm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentProviderControllerTest {

    @Mock
    private PaymentProviderService paymentProviderService;

    @InjectMocks
    private PaymentProviderController paymentProviderController;

    private CreatePaymentVm createPaymentVm;
    private UpdatePaymentVm updatePaymentVm;
    private PaymentProviderVm paymentProviderVm;

    @BeforeEach
    void setUp() {
        createPaymentVm = new CreatePaymentVm();
        createPaymentVm.setId("provider-1");
        createPaymentVm.setName("Test Provider");
        createPaymentVm.setEnabled(true);

        updatePaymentVm = new UpdatePaymentVm();
        updatePaymentVm.setId("provider-1");
        updatePaymentVm.setName("Updated Provider");
        updatePaymentVm.setEnabled(true);

        paymentProviderVm = new PaymentProviderVm("provider-1", "Test Provider", "http://config.url", 1, null, null);
    }

    @Test
    void create_Success() {
        when(paymentProviderService.create(any(CreatePaymentVm.class))).thenReturn(paymentProviderVm);

        ResponseEntity<PaymentProviderVm> response = paymentProviderController.create(createPaymentVm);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo("provider-1");
        assertThat(response.getBody().getName()).isEqualTo("Test Provider");
    }

    @Test
    void update_Success() {
        when(paymentProviderService.update(any(UpdatePaymentVm.class))).thenReturn(paymentProviderVm);

        ResponseEntity<PaymentProviderVm> response = paymentProviderController.update(updatePaymentVm);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo("provider-1");
    }

    @Test
    void getAll_Success() {
        when(paymentProviderService.getEnabledPaymentProviders(any(Pageable.class))).thenReturn(List.of(paymentProviderVm));

        ResponseEntity<List<PaymentProviderVm>> response = paymentProviderController.getAll(Pageable.unpaged());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getId()).isEqualTo("provider-1");
    }
}
