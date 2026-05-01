package com.yas.location.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.DuplicatedException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.location.mapper.CountryMapper;
import com.yas.location.model.Country;
import com.yas.location.repository.CountryRepository;
import com.yas.location.viewmodel.country.CountryPostVm;
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
//Trigger
@ExtendWith(MockitoExtension.class)
class CountryServiceUnitTest {

    @Mock
    private CountryRepository countryRepository;

    @Mock
    private CountryMapper countryMapper;

    @InjectMocks
    private CountryService countryService;

    @Test
    void create_whenCodeExists_throwsDuplicatedException() {
        CountryPostVm postVm = CountryPostVm.builder()
            .code2("US")
            .name("United States")
            .build();

        when(countryRepository.existsByCode2IgnoreCase("US")).thenReturn(true);

        assertThrows(DuplicatedException.class, () -> countryService.create(postVm));
        verifyNoInteractions(countryMapper);
    }

    @Test
    void create_whenNameExists_throwsDuplicatedException() {
        CountryPostVm postVm = CountryPostVm.builder()
            .code2("US")
            .name("United States")
            .build();

        when(countryRepository.existsByCode2IgnoreCase("US")).thenReturn(false);
        when(countryRepository.existsByNameIgnoreCase("United States")).thenReturn(true);

        assertThrows(DuplicatedException.class, () -> countryService.create(postVm));
    }

    @Test
    void update_whenNameExists_throwsDuplicatedException() {
        CountryPostVm postVm = CountryPostVm.builder()
            .code2("US")
            .name("United States")
            .build();

        Country existing = Country.builder().id(1L).build();

        when(countryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(countryRepository.existsByNameIgnoreCaseAndIdNot("United States", 1L)).thenReturn(true);

        assertThrows(DuplicatedException.class, () -> countryService.update(postVm, 1L));
    }

    @Test
    void update_whenCodeExists_throwsDuplicatedException() {
        CountryPostVm postVm = CountryPostVm.builder()
            .code2("US")
            .name("United States")
            .build();

        Country existing = Country.builder().id(1L).build();

        when(countryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(countryRepository.existsByNameIgnoreCaseAndIdNot("United States", 1L)).thenReturn(false);
        when(countryRepository.existsByCode2IgnoreCaseAndIdNot("US", 1L)).thenReturn(true);

        assertThrows(DuplicatedException.class, () -> countryService.update(postVm, 1L));
    }

    @Test
    void delete_whenNotExists_throwsNotFoundException() {
        when(countryRepository.existsById(99L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> countryService.delete(99L));
    }

    @Test
    void getPageableCountries_mapsPageValues() {
        Country country = Country.builder().id(1L).name("Alpha").code2("AL").build();
        when(countryRepository.findAll(PageRequest.of(0, 2, Sort.by(Sort.Direction.ASC, "name"))))
            .thenReturn(new PageImpl<>(List.of(country)));

        var result = countryService.getPageableCountries(0, 2);

        assertEquals(1, result.totalElements());
        assertEquals(1, result.countryContent().size());
    }
}
