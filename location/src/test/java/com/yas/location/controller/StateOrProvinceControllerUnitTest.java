package com.yas.location.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.location.model.Country;
import com.yas.location.model.StateOrProvince;
import com.yas.location.service.StateOrProvinceService;
import com.yas.location.viewmodel.stateorprovince.StateOrProvinceAndCountryGetNameVm;
import com.yas.location.viewmodel.stateorprovince.StateOrProvinceListGetVm;
import com.yas.location.viewmodel.stateorprovince.StateOrProvincePostVm;
import com.yas.location.viewmodel.stateorprovince.StateOrProvinceVm;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

@ExtendWith(MockitoExtension.class)
class StateOrProvinceControllerUnitTest {

    @Mock
    private StateOrProvinceService stateOrProvinceService;

    @InjectMocks
    private StateOrProvinceController controller;

    @Test
    void getPageableStateOrProvinces_returnsOk() {
        StateOrProvinceListGetVm vm = new StateOrProvinceListGetVm(List.of(), 0, 10, 0, 0, true);
        when(stateOrProvinceService.getPageableStateOrProvinces(0, 10, 1L)).thenReturn(vm);

        ResponseEntity<StateOrProvinceListGetVm> response = controller.getPageableStateOrProvinces(0, 10, 1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(vm);
    }

    @Test
    void getAllByCountryId_returnsOk() {
        List<StateOrProvinceVm> vms = List.of(new StateOrProvinceVm(1L, "State", "ST", "Type", 2L));
        when(stateOrProvinceService.getAllByCountryId(2L)).thenReturn(vms);

        ResponseEntity<List<StateOrProvinceVm>> response = controller.getAllByCountryId(2L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(vms);
    }

    @Test
    void getStateOrProvince_returnsOk() {
        StateOrProvinceVm vm = new StateOrProvinceVm(3L, "State", "ST", "Type", 4L);
        when(stateOrProvinceService.findById(3L)).thenReturn(vm);

        ResponseEntity<StateOrProvinceVm> response = controller.getStateOrProvince(3L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(vm);
    }

    @Test
    void getStateOrProvinceAndCountryNames_returnsOk() {
        List<StateOrProvinceAndCountryGetNameVm> vms = List.of(new StateOrProvinceAndCountryGetNameVm(1L,
            "State", "Country"));
        when(stateOrProvinceService.getStateOrProvinceAndCountryNames(List.of(1L, 2L))).thenReturn(vms);

        ResponseEntity<List<StateOrProvinceAndCountryGetNameVm>> response =
            controller.getStateOrProvinceAndCountryNames(List.of(1L, 2L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(vms);
    }

    @Test
    void createStateOrProvince_returnsCreatedWithLocation() {
        StateOrProvincePostVm postVm = StateOrProvincePostVm.builder()
            .name("State")
            .code("ST")
            .type("Type")
            .countryId(1L)
            .build();
        StateOrProvince state = StateOrProvince.builder()
            .id(5L)
            .name("State")
            .code("ST")
            .type("Type")
            .country(Country.builder().id(1L).name("Country").build())
            .build();
        when(stateOrProvinceService.createStateOrProvince(postVm)).thenReturn(state);

        ResponseEntity<StateOrProvinceVm> response = controller.createStateOrProvince(postVm,
            UriComponentsBuilder.fromPath("/"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getLocation()).isEqualTo(URI.create("/state-or-provinces/5"));
        assertThat(response.getBody()).isEqualTo(StateOrProvinceVm.fromModel(state));
    }

    @Test
    void updateStateOrProvince_returnsNoContent() {
        StateOrProvincePostVm postVm = StateOrProvincePostVm.builder().name("State").build();

        ResponseEntity<Void> response = controller.updateStateOrProvince(6L, postVm);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(stateOrProvinceService).updateStateOrProvince(postVm, 6L);
    }

    @Test
    void deleteStateOrProvince_returnsNoContent() {
        ResponseEntity<Void> response = controller.deleteStateOrProvince(7L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(stateOrProvinceService).delete(7L);
    }
}
