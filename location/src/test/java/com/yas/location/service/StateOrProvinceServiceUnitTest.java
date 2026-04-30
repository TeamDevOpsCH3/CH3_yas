package com.yas.location.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.DuplicatedException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.location.mapper.StateOrProvinceMapper;
import com.yas.location.model.Country;
import com.yas.location.model.StateOrProvince;
import com.yas.location.repository.CountryRepository;
import com.yas.location.repository.StateOrProvinceRepository;
import com.yas.location.utils.Constants;
import com.yas.location.viewmodel.stateorprovince.StateOrProvincePostVm;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class StateOrProvinceServiceUnitTest {

    @Mock
    private StateOrProvinceRepository stateOrProvinceRepository;

    @Mock
    private CountryRepository countryRepository;

    @Mock
    private StateOrProvinceMapper stateOrProvinceMapper;

    @InjectMocks
    private StateOrProvinceService stateOrProvinceService;

    @Test
    void create_whenCountryMissing_throwsNotFound() {
        StateOrProvincePostVm postVm = StateOrProvincePostVm.builder()
            .countryId(10L)
            .name("State")
            .build();

        when(countryRepository.existsById(10L)).thenReturn(false);

        NotFoundException exception = assertThrows(NotFoundException.class,
            () -> stateOrProvinceService.createStateOrProvince(postVm));

        assertTrue(exception.getMessage().contains(Constants.ErrorCode.COUNTRY_NOT_FOUND));
    }

    @Test
    void create_whenNameExists_throwsDuplicated() {
        StateOrProvincePostVm postVm = StateOrProvincePostVm.builder()
            .countryId(1L)
            .name("State")
            .build();

        when(countryRepository.existsById(1L)).thenReturn(true);
        when(stateOrProvinceRepository.existsByNameIgnoreCaseAndCountryId("State", 1L)).thenReturn(true);

        DuplicatedException exception = assertThrows(DuplicatedException.class,
            () -> stateOrProvinceService.createStateOrProvince(postVm));

        assertTrue(exception.getMessage().contains(Constants.ErrorCode.NAME_ALREADY_EXITED));
    }

    @Test
    void update_whenNameExists_throwsDuplicated() {
        StateOrProvincePostVm postVm = StateOrProvincePostVm.builder()
            .name("Updated")
            .build();

        Country country = Country.builder().id(1L).build();
        StateOrProvince state = StateOrProvince.builder().id(2L).country(country).build();

        when(stateOrProvinceRepository.findById(2L)).thenReturn(Optional.of(state));
        when(stateOrProvinceRepository.existsByNameIgnoreCaseAndCountryIdAndIdNot("Updated", 1L, 2L))
            .thenReturn(true);

        DuplicatedException exception = assertThrows(DuplicatedException.class,
            () -> stateOrProvinceService.updateStateOrProvince(postVm, 2L));

        assertTrue(exception.getMessage().contains(Constants.ErrorCode.NAME_ALREADY_EXITED));
    }

    @Test
    void delete_whenNotExists_throwsNotFound() {
        when(stateOrProvinceRepository.existsById(5L)).thenReturn(false);

        NotFoundException exception = assertThrows(NotFoundException.class,
            () -> stateOrProvinceService.delete(5L));

        assertTrue(exception.getMessage().contains(Constants.ErrorCode.STATE_OR_PROVINCE_NOT_FOUND));
    }

    @Test
    void getPageableStateOrProvinces_mapsPageValues() {
        StateOrProvince state = StateOrProvince.builder().id(1L).name("State").build();
        when(stateOrProvinceRepository.getPageableStateOrProvincesByCountry(
            1L,
            PageRequest.of(0, 2, Sort.by(Sort.Direction.ASC, "name"))
        )).thenReturn(new PageImpl<>(List.of(state)));

        var result = stateOrProvinceService.getPageableStateOrProvinces(0, 2, 1L);

        assertEquals(1, result.totalElements());
        assertEquals(1, result.stateOrProvinceContent().size());
    }
}
