package com.yas.location.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yas.commonlibrary.exception.ApiExceptionHandler;
import com.yas.location.service.CountryService;
import com.yas.location.utils.Constants;
import com.yas.location.viewmodel.country.CountryVm;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = CountryStorefrontController.class,
    excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ApiExceptionHandler.class)
class CountryStorefrontControllerTest {

    @MockitoBean
    private CountryService countryService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testListCountries_whenValid_thenReturnOk() throws Exception {
        when(countryService.findAllCountries()).thenReturn(
            List.of(new CountryVm(1L, "US", "United States", "USA", true, true, true, true, true))
        );

        mockMvc.perform(get(Constants.ApiConstant.COUNTRIES_STOREFRONT_URL))
            .andExpect(status().isOk());
    }

    @Test
    void testListCountries_whenServiceThrows_thenReturn500() throws Exception {
        when(countryService.findAllCountries()).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get(Constants.ApiConstant.COUNTRIES_STOREFRONT_URL))
            .andExpect(status().isInternalServerError());
    }
}
