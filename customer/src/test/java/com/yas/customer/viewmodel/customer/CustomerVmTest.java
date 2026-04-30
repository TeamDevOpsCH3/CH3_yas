package com.yas.customer.viewmodel.customer;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.UserRepresentation;

class CustomerVmTest {

    @Test
    void fromUserRepresentation_mapsFields() {
        UserRepresentation user = new UserRepresentation();
        user.setId("id-1");
        user.setUsername("user1");
        user.setEmail("user1@example.com");
        user.setFirstName("John");
        user.setLastName("Doe");

        CustomerVm vm = CustomerVm.fromUserRepresentation(user);

        assertThat(vm.id()).isEqualTo("id-1");
        assertThat(vm.username()).isEqualTo("user1");
        assertThat(vm.email()).isEqualTo("user1@example.com");
        assertThat(vm.firstName()).isEqualTo("John");
        assertThat(vm.lastName()).isEqualTo("Doe");
    }

    @Test
    void adminVmFromUserRepresentation_mapsCreatedTimestamp() {
        UserRepresentation user = new UserRepresentation();
        user.setId("id-2");
        user.setUsername("admin1");
        user.setEmail("admin@example.com");
        user.setFirstName("Jane");
        user.setLastName("Roe");
        user.setCreatedTimestamp(946684800000L);

        CustomerAdminVm vm = CustomerAdminVm.fromUserRepresentation(user);

        LocalDateTime expected = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(946684800000L),
            TimeZone.getDefault().toZoneId()
        );

        assertThat(vm.createdTimestamp()).isEqualTo(expected);
        assertThat(vm.username()).isEqualTo("admin1");
    }

    @Test
    void recordsExposeFields() {
        CustomerPostVm postVm = new CustomerPostVm("user1", "u1@example.com", "John",
            "Doe", "pass", "ADMIN");
        CustomerProfileRequestVm profileVm = new CustomerProfileRequestVm("A", "B", "a@b.com");
        CustomerListVm listVm = new CustomerListVm(1, List.of(), 1);
        GuestUserVm guestVm = new GuestUserVm("g1", "guest@yas.com", "GUEST");

        assertThat(postVm.username()).isEqualTo("user1");
        assertThat(profileVm.email()).isEqualTo("a@b.com");
        assertThat(listVm.totalPage()).isEqualTo(1);
        assertThat(guestVm.password()).isEqualTo("GUEST");
    }
}
