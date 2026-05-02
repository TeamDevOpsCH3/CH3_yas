package com.yas.location.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.location.service.AddressService;
import com.yas.location.viewmodel.address.AddressDetailVm;
import com.yas.location.viewmodel.address.AddressGetVm;
import com.yas.location.viewmodel.address.AddressPostVm;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class AddressControllerUnitTest {

    @Mock
    private AddressService addressService;

    @InjectMocks
    private AddressController controller;

    @Test
    void createAddress_returnsOk() {
        AddressPostVm postVm = AddressPostVm.builder()
            .contactName("Name")
            .phone("123")
            .addressLine1("Line1")
            .districtId(1L)
            .stateOrProvinceId(2L)
            .countryId(3L)
            .build();
        AddressGetVm vm = AddressGetVm.builder().id(10L).build();
        when(addressService.createAddress(postVm)).thenReturn(vm);

        ResponseEntity<AddressGetVm> response = controller.createAddress(postVm);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(vm);
    }

    @Test
    void updateAddress_returnsNoContent() {
        AddressPostVm postVm = AddressPostVm.builder()
            .contactName("Name")
            .districtId(1L)
            .stateOrProvinceId(2L)
            .countryId(3L)
            .build();

        ResponseEntity<Void> response = controller.updateAddress(4L, postVm);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(addressService).updateAddress(4L, postVm);
    }

    @Test
    void getAddressById_returnsOk() {
        AddressDetailVm vm = AddressDetailVm.builder().id(5L).build();
        when(addressService.getAddress(5L)).thenReturn(vm);

        ResponseEntity<AddressDetailVm> response = controller.getAddressById(5L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(vm);
    }

    @Test
    void getAddressList_returnsOk() {
        List<AddressDetailVm> vms = List.of(AddressDetailVm.builder().id(1L).build());
        when(addressService.getAddressList(List.of(1L, 2L))).thenReturn(vms);

        ResponseEntity<List<AddressDetailVm>> response = controller.getAddressList(List.of(1L, 2L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(vms);
    }

    @Test
    void deleteAddress_returnsOk() {
        ResponseEntity<Void> response = controller.deleteAddress(9L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(addressService).deleteAddress(9L);
    }
}
