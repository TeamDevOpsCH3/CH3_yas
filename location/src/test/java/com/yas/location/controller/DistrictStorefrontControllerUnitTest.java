package com.yas.location.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.yas.location.service.DistrictService;
import com.yas.location.viewmodel.district.DistrictGetVm;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class DistrictStorefrontControllerUnitTest {

    @Mock
    private DistrictService districtService;

    @InjectMocks
    private DistrictStorefrontController controller;

    @Test
    void getList_returnsOk() {
        List<DistrictGetVm> vms = List.of(new DistrictGetVm(1L, "District"));
        when(districtService.getList(3L)).thenReturn(vms);

        ResponseEntity<List<DistrictGetVm>> response = controller.getList(3L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(vms);
    }
}
