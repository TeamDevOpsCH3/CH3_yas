package com.yas.location.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.yas.location.service.StateOrProvinceService;
import com.yas.location.viewmodel.stateorprovince.StateOrProvinceVm;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class StateOrProvinceStoreFrontControllerUnitTest {

    @Mock
    private StateOrProvinceService stateOrProvinceService;

    @InjectMocks
    private StateOrProvinceStoreFrontController controller;

    @Test
    void getStateOrProvince_returnsOk() {
        List<StateOrProvinceVm> vms = List.of(new StateOrProvinceVm(1L, "State", "ST", "Type", 2L));
        when(stateOrProvinceService.getAllByCountryId(2L)).thenReturn(vms);

        ResponseEntity<List<StateOrProvinceVm>> response = controller.getStateOrProvince(2L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(vms);
    }
}
