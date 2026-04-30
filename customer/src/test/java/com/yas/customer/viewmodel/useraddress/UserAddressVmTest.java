package com.yas.customer.viewmodel.useraddress;

import static org.assertj.core.api.Assertions.assertThat;

import com.yas.customer.model.UserAddress;
import com.yas.customer.viewmodel.address.AddressVm;
import org.junit.jupiter.api.Test;

class UserAddressVmTest {

    @Test
    void fromModel_mapsFields() {
        UserAddress userAddress = UserAddress.builder()
            .id(10L)
            .userId("user-1")
            .addressId(99L)
            .isActive(true)
            .build();

        AddressVm addressVm = AddressVm.builder()
            .id(99L)
            .contactName("John")
            .phone("+111")
            .addressLine1("123 Street")
            .city("City")
            .zipCode("00000")
            .districtId(1L)
            .stateOrProvinceId(2L)
            .countryId(3L)
            .build();

        UserAddressVm vm = UserAddressVm.fromModel(userAddress, addressVm);

        assertThat(vm.id()).isEqualTo(10L);
        assertThat(vm.userId()).isEqualTo("user-1");
        assertThat(vm.addressGetVm()).isEqualTo(addressVm);
        assertThat(vm.isActive()).isTrue();
    }
}
