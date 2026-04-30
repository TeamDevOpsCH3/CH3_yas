package com.yas.location.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.yas.location.service.CountryService;
import com.yas.location.viewmodel.country.CountryVm;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class CountryStorefrontControllerUnitTest {

    @Mock
    private CountryService countryService;

    @InjectMocks
    private CountryStorefrontController controller;

    @Test
    void listCountries_returnsOk() {
        List<CountryVm> countries = List.of(new CountryVm(1L, "US", "United States", "USA", true,
            true, true, true, true));
        when(countryService.findAllCountries()).thenReturn(countries);

        ResponseEntity<List<CountryVm>> response = controller.listCountries();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(countries);
    }
}
