package com.yas.location.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.location.model.Country;
import com.yas.location.service.CountryService;
import com.yas.location.viewmodel.country.CountryListGetVm;
import com.yas.location.viewmodel.country.CountryPostVm;
import com.yas.location.viewmodel.country.CountryVm;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

@ExtendWith(MockitoExtension.class)
class CountryControllerUnitTest {

    @Mock
    private CountryService countryService;

    @InjectMocks
    private CountryController controller;

    @Test
    void getPageableCountries_returnsOk() {
        CountryListGetVm vm = new CountryListGetVm(List.of(), 0, 10, 0, 0, true);
        when(countryService.getPageableCountries(0, 10)).thenReturn(vm);

        ResponseEntity<CountryListGetVm> response = controller.getPageableCountries(0, 10);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(vm);
    }

    @Test
    void listCountries_returnsOk() {
        List<CountryVm> countries = List.of(new CountryVm(1L, "US", "United States", "USA", true,
            true, true, true, true));
        when(countryService.findAllCountries()).thenReturn(countries);

        ResponseEntity<List<CountryVm>> response = controller.listCountries();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(countries);
    }

    @Test
    void getCountry_returnsOk() {
        CountryVm vm = new CountryVm(5L, "US", "United States", "USA", true, true, true, true, true);
        when(countryService.findById(5L)).thenReturn(vm);

        ResponseEntity<CountryVm> response = controller.getCountry(5L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(vm);
    }

    @Test
    void createCountry_returnsCreatedWithLocation() {
        CountryPostVm postVm = CountryPostVm.builder()
            .id("id")
            .code2("US")
            .name("United States")
            .build();
        Country country = Country.builder()
            .id(9L)
            .code2("US")
            .name("United States")
            .build();
        when(countryService.create(postVm)).thenReturn(country);

        ResponseEntity<CountryVm> response = controller.createCountry(postVm, UriComponentsBuilder.fromPath("/"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getLocation()).isEqualTo(URI.create("/countries/9"));
        assertThat(response.getBody()).isEqualTo(CountryVm.fromModel(country));
    }

    @Test
    void updateCountry_returnsNoContent() {
        CountryPostVm postVm = CountryPostVm.builder().id("id").code2("US").name("United States").build();

        ResponseEntity<Void> response = controller.updateCountry(3L, postVm);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(countryService).update(postVm, 3L);
    }

    @Test
    void deleteCountry_returnsNoContent() {
        ResponseEntity<Void> response = controller.deleteCountry(7L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(countryService).delete(7L);
    }
}
