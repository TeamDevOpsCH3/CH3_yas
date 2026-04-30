package com.yas.customer.viewmodel.address;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AddressVmTest {

    @Test
    void recordsExposeValues() {
        AddressPostVm postVm = new AddressPostVm(
            "Jane Smith",
            "+1987654321",
            "456 Oak Avenue",
            "Metropolis",
            "54321",
            102L,
            202L,
            302L
        );

        ActiveAddressVm activeVm = new ActiveAddressVm(
            1L,
            "John Doe",
            "+1234567890",
            "123 Elm Street",
            "Springfield",
            "62701",
            101L,
            "Downtown",
            201L,
            "Illinois",
            301L,
            "United States",
            true
        );

        assertThat(postVm.city()).isEqualTo("Metropolis");
        assertThat(activeVm.isActive()).isTrue();
        assertThat(activeVm.countryName()).isEqualTo("United States");
    }

    @Test
    void buildersCreateExpectedInstances() {
        AddressDetailVm detailVm = AddressDetailVm.builder()
            .id(10L)
            .contactName("Alice")
            .phone("+111")
            .addressLine1("789 Pine Road")
            .city("Gotham")
            .zipCode("10001")
            .districtId(103L)
            .districtName("North")
            .stateOrProvinceId(203L)
            .stateOrProvinceName("State")
            .countryId(303L)
            .countryName("Country")
            .build();

        AddressVm addressVm = AddressVm.builder()
            .id(11L)
            .contactName("Bob")
            .phone("+222")
            .addressLine1("456 Maple")
            .city("Star City")
            .zipCode("20002")
            .districtId(104L)
            .stateOrProvinceId(204L)
            .countryId(304L)
            .build();

        assertThat(detailVm.id()).isEqualTo(10L);
        assertThat(detailVm.city()).isEqualTo("Gotham");
        assertThat(addressVm.id()).isEqualTo(11L);
        assertThat(addressVm.city()).isEqualTo("Star City");
    }
}
