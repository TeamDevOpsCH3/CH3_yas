package com.yas.location.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.location.model.Address;
import com.yas.location.model.Country;
import com.yas.location.model.District;
import com.yas.location.model.StateOrProvince;
import com.yas.location.repository.AddressRepository;
import com.yas.location.repository.CountryRepository;
import com.yas.location.repository.DistrictRepository;
import com.yas.location.repository.StateOrProvinceRepository;
import com.yas.location.viewmodel.address.AddressDetailVm;
import com.yas.location.viewmodel.address.AddressGetVm;
import com.yas.location.viewmodel.address.AddressPostVm;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AddressServiceUnitTest {

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private StateOrProvinceRepository stateOrProvinceRepository;

    @Mock
    private CountryRepository countryRepository;

    @Mock
    private DistrictRepository districtRepository;

    @InjectMocks
    private AddressService addressService;

    @Test
    void createAddress_whenCountryMissing_throwsNotFound() {
        AddressPostVm postVm = AddressPostVm.builder()
            .contactName("contact")
            .districtId(1L)
            .stateOrProvinceId(2L)
            .countryId(3L)
            .build();

        when(countryRepository.findById(3L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> addressService.createAddress(postVm));
    }

    @Test
    void createAddress_whenOptionalRefsMissing_stillSaves() {
        AddressPostVm postVm = AddressPostVm.builder()
            .contactName("contact")
            .districtId(1L)
            .stateOrProvinceId(2L)
            .countryId(3L)
            .build();

        Country country = Country.builder().id(3L).build();
        StateOrProvince state = StateOrProvince.builder().id(2L).country(country).build();
        District district = District.builder().id(1L).stateProvince(state).build();

        when(stateOrProvinceRepository.findById(2L)).thenReturn(Optional.of(state));
        when(districtRepository.findById(1L)).thenReturn(Optional.of(district));
        when(countryRepository.findById(3L)).thenReturn(Optional.of(country));
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AddressGetVm result = addressService.createAddress(postVm);

        assertNotNull(result);
    }

    @Test
    void updateAddress_whenNotFound_throwsNotFound() {
        AddressPostVm postVm = AddressPostVm.builder()
            .contactName("contact")
            .districtId(1L)
            .stateOrProvinceId(2L)
            .countryId(3L)
            .build();

        when(addressRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> addressService.updateAddress(10L, postVm));
    }

    @Test
    void deleteAddress_whenNotFound_throwsNotFound() {
        when(addressRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> addressService.deleteAddress(10L));
    }

    @Test
    void getAddressList_mapsResults() {
        Address address = buildAddress();
        when(addressRepository.findAllByIdIn(List.of(1L))).thenReturn(List.of(address));

        List<AddressDetailVm> result = addressService.getAddressList(List.of(1L));

        assertEquals(1, result.size());
        assertEquals("contact", result.getFirst().contactName());
    }

    private Address buildAddress() {
        Country country = Country.builder().id(3L).name("Country").build();
        StateOrProvince state = StateOrProvince.builder().id(2L).name("State").country(country).build();
        District district = District.builder().id(1L).name("District").stateProvince(state).build();

        return Address.builder()
            .id(1L)
            .contactName("contact")
            .city("city")
            .zipCode("zip")
            .country(country)
            .stateOrProvince(state)
            .district(district)
            .build();
    }
}
