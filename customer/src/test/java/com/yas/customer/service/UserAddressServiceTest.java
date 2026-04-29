package com.yas.customer.service;

import static com.yas.customer.util.SecurityContextUtils.setUpSecurityContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.AccessDeniedException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.customer.model.UserAddress;
import com.yas.customer.repository.UserAddressRepository;
import com.yas.customer.viewmodel.address.ActiveAddressVm;
import com.yas.customer.viewmodel.address.AddressDetailVm;
import com.yas.customer.viewmodel.address.AddressPostVm;
import com.yas.customer.viewmodel.address.AddressVm;
import com.yas.customer.viewmodel.useraddress.UserAddressVm;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class UserAddressServiceTest {

    @Mock
    private UserAddressRepository userAddressRepository;

    @Mock
    private LocationService locationService;

    @InjectMocks
    private UserAddressService userAddressService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getUserAddressList_anonymousUser_throwsAccessDenied() {
        setUpSecurityContext("anonymousUser");

        assertThrows(AccessDeniedException.class, () -> userAddressService.getUserAddressList());
    }

    @Test
    void getUserAddressList_mapsAndSortsActiveFirst() {
        setUpSecurityContext("user-1");

        List<UserAddress> userAddresses = List.of(
            UserAddress.builder().id(1L).userId("user-1").addressId(10L).isActive(false).build(),
            UserAddress.builder().id(2L).userId("user-1").addressId(20L).isActive(true).build()
        );

        when(userAddressRepository.findAllByUserId("user-1")).thenReturn(userAddresses);
        when(locationService.getAddressesByIdList(List.of(10L, 20L)))
            .thenReturn(List.of(buildAddressDetail(10L), buildAddressDetail(20L)));

        List<ActiveAddressVm> result = userAddressService.getUserAddressList();

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().isActive()).isTrue();
        assertThat(result.getFirst().id()).isEqualTo(20L);
        assertThat(result.get(1).isActive()).isFalse();
        assertThat(result.get(1).id()).isEqualTo(10L);
    }

    @Test
    void getAddressDefault_anonymousUser_throwsAccessDenied() {
        setUpSecurityContext("anonymousUser");

        assertThrows(AccessDeniedException.class, () -> userAddressService.getAddressDefault());
    }

    @Test
    void getAddressDefault_notFound_throwsNotFound() {
        setUpSecurityContext("user-1");
        when(userAddressRepository.findByUserIdAndIsActiveTrue("user-1")).thenReturn(Optional.empty());

        NotFoundException thrown = assertThrows(NotFoundException.class, () -> userAddressService.getAddressDefault());
        assertThat(thrown.getMessage()).contains("User address not found");
    }

    @Test
    void getAddressDefault_returnsAddressDetail() {
        setUpSecurityContext("user-1");

        UserAddress activeAddress = UserAddress.builder()
            .id(3L)
            .userId("user-1")
            .addressId(99L)
            .isActive(true)
            .build();

        when(userAddressRepository.findByUserIdAndIsActiveTrue("user-1"))
            .thenReturn(Optional.of(activeAddress));

        AddressDetailVm addressDetail = buildAddressDetail(99L);
        when(locationService.getAddressById(99L)).thenReturn(addressDetail);

        AddressDetailVm result = userAddressService.getAddressDefault();

        assertSame(addressDetail, result);
    }

    @Test
    void createAddress_firstAddress_setsActiveTrue() {
        setUpSecurityContext("user-1");

        when(userAddressRepository.findAllByUserId("user-1")).thenReturn(List.of());

        AddressPostVm postVm = buildAddressPostVm();
        AddressVm addressVm = buildAddressVm(5L);
        when(locationService.createAddress(postVm)).thenReturn(addressVm);

        when(userAddressRepository.save(any(UserAddress.class))).thenAnswer(invocation -> {
            UserAddress saved = invocation.getArgument(0, UserAddress.class);
            saved.setId(100L);
            return saved;
        });

        UserAddressVm result = userAddressService.createAddress(postVm);

        assertThat(result.id()).isEqualTo(100L);
        assertThat(result.addressGetVm()).isEqualTo(addressVm);
        assertThat(result.isActive()).isTrue();
    }

    @Test
    void createAddress_nonFirstAddress_setsActiveFalse() {
        setUpSecurityContext("user-1");

        when(userAddressRepository.findAllByUserId("user-1"))
            .thenReturn(List.of(UserAddress.builder().id(1L).userId("user-1").addressId(1L).isActive(true).build()));

        AddressPostVm postVm = buildAddressPostVm();
        AddressVm addressVm = buildAddressVm(7L);
        when(locationService.createAddress(postVm)).thenReturn(addressVm);

        when(userAddressRepository.save(any(UserAddress.class))).thenAnswer(invocation -> {
            UserAddress saved = invocation.getArgument(0, UserAddress.class);
            saved.setId(200L);
            return saved;
        });

        UserAddressVm result = userAddressService.createAddress(postVm);

        assertThat(result.id()).isEqualTo(200L);
        assertThat(result.isActive()).isFalse();
    }

    @Test
    void deleteAddress_notFound_throwsNotFound() {
        setUpSecurityContext("user-1");

        when(userAddressRepository.findOneByUserIdAndAddressId("user-1", 10L)).thenReturn(null);

        NotFoundException thrown = assertThrows(NotFoundException.class,
            () -> userAddressService.deleteAddress(10L));
        assertThat(thrown.getMessage()).contains("User address not found");
    }

    @Test
    void deleteAddress_existing_deletes() {
        setUpSecurityContext("user-1");

        UserAddress userAddress = UserAddress.builder()
            .id(11L)
            .userId("user-1")
            .addressId(10L)
            .isActive(true)
            .build();

        when(userAddressRepository.findOneByUserIdAndAddressId("user-1", 10L)).thenReturn(userAddress);

        userAddressService.deleteAddress(10L);

        verify(userAddressRepository).delete(userAddress);
    }

    @Test
    void chooseDefaultAddress_updatesActiveFlags() {
        setUpSecurityContext("user-1");

        UserAddress first = UserAddress.builder().id(1L).userId("user-1").addressId(10L).isActive(true).build();
        UserAddress second = UserAddress.builder().id(2L).userId("user-1").addressId(20L).isActive(false).build();

        when(userAddressRepository.findAllByUserId("user-1")).thenReturn(List.of(first, second));

        userAddressService.chooseDefaultAddress(20L);

        ArgumentCaptor<List<UserAddress>> captor = ArgumentCaptor.forClass(List.class);
        verify(userAddressRepository).saveAll(captor.capture());

        List<UserAddress> saved = captor.getValue();
        assertThat(saved).hasSize(2);
        assertThat(saved.getFirst().getAddressId()).isEqualTo(10L);
        assertThat(saved.getFirst().getIsActive()).isFalse();
        assertThat(saved.get(1).getAddressId()).isEqualTo(20L);
        assertThat(saved.get(1).getIsActive()).isTrue();
    }

    private AddressDetailVm buildAddressDetail(Long id) {
        return new AddressDetailVm(
            id,
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
            "United States"
        );
    }

    private AddressPostVm buildAddressPostVm() {
        return new AddressPostVm(
            "Jane Smith",
            "+1987654321",
            "456 Oak Avenue",
            "Metropolis",
            "54321",
            102L,
            202L,
            302L
        );
    }

    private AddressVm buildAddressVm(Long id) {
        return new AddressVm(
            id,
            "Alice Johnson",
            "+1239874560",
            "789 Pine Road",
            "Gotham",
            "10001",
            103L,
            203L,
            303L
        );
    }
}
