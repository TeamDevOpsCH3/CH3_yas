package com.yas.customer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.AccessDeniedException;
import com.yas.commonlibrary.exception.DuplicatedException;
import com.yas.commonlibrary.exception.ForbiddenException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.customer.config.KeycloakPropsConfig;
import com.yas.customer.utils.Constants;
import com.yas.customer.viewmodel.customer.CustomerPostVm;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomerServiceAdditionalTest {

    private static final String REALM_NAME = "test-realm";
    private static final String RESOURCE_NAME = "test-resource";

    @Mock
    private Keycloak keycloak;

    @Mock
    private KeycloakPropsConfig keycloakPropsConfig;

    @Mock
    private RealmResource realmResource;

    @Mock
    private UsersResource usersResource;

    @Mock
    private UserResource userResource;

    @InjectMocks
    private CustomerService customerService;

    @BeforeEach
    void setUp() {
        when(keycloakPropsConfig.getRealm()).thenReturn(REALM_NAME);
        when(keycloakPropsConfig.getResource()).thenReturn(RESOURCE_NAME);
        when(keycloak.realm(REALM_NAME)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
    }

    @Test
    void getCustomers_forbiddenWrapsAccessDenied() {
        when(usersResource.search(any(), anyInt(), anyInt()))
            .thenThrow(new ForbiddenException("denied"));

        AccessDeniedException thrown = assertThrows(AccessDeniedException.class,
            () -> customerService.getCustomers(0));

        assertThat(thrown.getMessage()).contains("denied");
        assertThat(thrown.getMessage()).contains(RESOURCE_NAME);
    }

    @Test
    void deleteCustomer_userNotFound_throwsNotFound() {
        when(usersResource.get("id-1")).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(null);

        NotFoundException thrown = assertThrows(NotFoundException.class,
            () -> customerService.deleteCustomer("id-1"));

        assertThat(thrown.getMessage()).contains(Constants.ErrorCode.USER_NOT_FOUND);
    }

    @Test
    void create_whenEmailExists_throwsDuplicatedException() {
        CustomerPostVm postVm = new CustomerPostVm("user1", "test@gmail.com", "John",
            "Doe", "123", "ADMIN");

        when(realmResource.users().search(anyString(), anyBoolean()))
            .thenReturn(Collections.emptyList());
        when(realmResource.users().search(any(), any(), any(), eq("test@gmail.com"), any(), any()))
            .thenReturn(Collections.singletonList(new UserRepresentation()));

        assertThrows(DuplicatedException.class, () -> customerService.create(postVm));
    }

    @Test
    void createPasswordCredentials_setsExpectedFields() {
        String password = "strong-pass";

        var credentials = CustomerService.createPasswordCredentials(password);

        assertThat(credentials.getValue()).isEqualTo(password);
        assertThat(credentials.getType()).isEqualTo("password");
        assertThat(credentials.isTemporary()).isFalse();
    }
}
