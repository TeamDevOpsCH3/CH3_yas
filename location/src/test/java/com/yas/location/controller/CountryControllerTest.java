package com.yas.location.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectWriter;
import com.yas.commonlibrary.exception.ApiExceptionHandler;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.location.model.Country;
import com.yas.location.service.CountryService;
import com.yas.location.utils.Constants;
import com.yas.location.viewmodel.country.CountryPostVm;
import com.yas.location.viewmodel.country.CountryListGetVm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;

import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.mockito.Mockito.when;

@WebMvcTest(controllers = CountryController.class,
    excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ApiExceptionHandler.class)
class CountryControllerTest {

    @MockitoBean
    private CountryService countryService;

    @Autowired
    private MockMvc mockMvc;

    private ObjectWriter objectWriter;

    @BeforeEach
    void setUp() {
        objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();
    }

    @Test
    void testCreateCountry_whenRequestIsValid_thenReturnOk() throws Exception {
        CountryPostVm countryPostVm = CountryPostVm.builder()
            .id("id")
            .code2("123")
            .name("name")
            .build();

        String request = objectWriter.writeValueAsString(countryPostVm);
        given(countryService.create(countryPostVm)).willReturn(new Country());

        this.mockMvc.perform(post(Constants.ApiConstant.COUNTRIES_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isCreated());
    }

    @Test
    void testCreateCountry_whenCodeIsOverMaxLength_thenReturnBadRequest() throws Exception {
        CountryPostVm countryPostVm = CountryPostVm.builder()
            .id("id")
            .code2("1234")
            .name("name")
            .build();

        String request = objectWriter.writeValueAsString(countryPostVm);

        this.mockMvc.perform(post(Constants.ApiConstant.COUNTRIES_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateCountry_whenIdIsBlank_thenReturnBadRequest() throws Exception {
        CountryPostVm countryPostVm = CountryPostVm.builder()
            .id("")
            .code2("123")
            .name("name")
            .build();

        String request = objectWriter.writeValueAsString(countryPostVm);

        this.mockMvc.perform(post(Constants.ApiConstant.COUNTRIES_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateCountry_whenRequestIsValid_thenReturnOk() throws Exception {
        CountryPostVm countryPostVm = CountryPostVm.builder()
            .id("id")
            .code2("123")
            .name("name")
            .build();

        String request = objectWriter.writeValueAsString(countryPostVm);

        this.mockMvc.perform(put(Constants.ApiConstant.COUNTRIES_URL + "/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isNoContent());
    }

    @Test
    void testUpdateCountry_whenCodeIsOverMaxLength_thenReturnBadRequest() throws Exception {
        CountryPostVm countryPostVm = CountryPostVm.builder()
            .id("id")
            .code2("1234")
            .name("name")
            .build();

        String request = objectWriter.writeValueAsString(countryPostVm);

        this.mockMvc.perform(put(Constants.ApiConstant.COUNTRIES_URL + "/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateCountry_whenIdIsBlank_thenReturnBadRequest() throws Exception {
        CountryPostVm countryPostVm = CountryPostVm.builder()
            .id("")
            .code2("123")
            .name("name")
            .build();

        String request = objectWriter.writeValueAsString(countryPostVm);

        this.mockMvc.perform(put(Constants.ApiConstant.COUNTRIES_URL + "/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testGetCountry_whenNotFound_thenReturn404() throws Exception {
        when(countryService.findById(1L))
            .thenThrow(new NotFoundException(Constants.ErrorCode.COUNTRY_NOT_FOUND, 1L));

        this.mockMvc.perform(get(Constants.ApiConstant.COUNTRIES_URL + "/1"))
            .andExpect(status().isNotFound());
    }

    @Test
    void testListCountries_whenServiceThrows_thenReturn500() throws Exception {
        when(countryService.findAllCountries())
            .thenThrow(new RuntimeException("boom"));

        this.mockMvc.perform(get(Constants.ApiConstant.COUNTRIES_URL))
            .andExpect(status().isInternalServerError());
    }

    @Test
    void testGetPageableCountries_whenValid_thenReturnOk() throws Exception {
        CountryListGetVm vm = new CountryListGetVm(List.of(), 0, 10, 0, 0, true);
        when(countryService.getPageableCountries(0, 10)).thenReturn(vm);

        this.mockMvc.perform(get(Constants.ApiConstant.COUNTRIES_URL + "/paging")
                .param("pageNo", "0")
                .param("pageSize", "10"))
            .andExpect(status().isOk());
    }

}