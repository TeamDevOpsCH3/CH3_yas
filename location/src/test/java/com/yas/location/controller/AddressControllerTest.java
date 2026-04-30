package com.yas.location.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectWriter;
import com.yas.commonlibrary.exception.ApiExceptionHandler;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.location.service.AddressService;
import com.yas.location.viewmodel.address.AddressDetailVm;
import com.yas.location.viewmodel.address.AddressPostVm;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;

import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebMvcTest(controllers = AddressController.class,
    excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ApiExceptionHandler.class)
class AddressControllerTest {

    @MockitoBean
    private AddressService addressService;

    @Autowired
    private MockMvc mockMvc;

    private ObjectWriter objectWriter;

    @BeforeEach
    void setUp() {
        objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();
    }

    @Test
    void testCreateAddress_whenRequestIsValid_thenReturnOk() throws Exception {
        AddressPostVm addressPostVm = AddressPostVm.builder()
            .contactName("contactName")
            .phone("12345678")
            .addressLine1("addressLine1")
            .addressLine2("addressLine2")
            .city("city")
            .zipCode("zipCode")
            .districtId(1L)
            .stateOrProvinceId(1L)
            .countryId(1L)
            .build();

        String request = objectWriter.writeValueAsString(addressPostVm);

        this.mockMvc.perform(post("/storefront/addresses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isOk());
    }

    @Test
    void testCreateAddress_whenPhoneIsOverMaxLength_thenReturnBadRequest() throws Exception {
        AddressPostVm addressPostVm = AddressPostVm.builder()
            .contactName("contactName")
            .phone("12345678912345678912345678")
            .addressLine1("addressLine1")
            .addressLine2("addressLine2")
            .city("city")
            .zipCode("zipCode")
            .districtId(1L)
            .stateOrProvinceId(1L)
            .countryId(1L)
            .build();

        String request = objectWriter.writeValueAsString(addressPostVm);

        this.mockMvc.perform(post("/storefront/addresses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateAddress_whenDistrictIsNull_thenReturnBadRequest() throws Exception {
        AddressPostVm addressPostVm = AddressPostVm.builder()
            .contactName("contactName")
            .phone("12345678")
            .addressLine1("addressLine1")
            .addressLine2("addressLine2")
            .city("city")
            .zipCode("zipCode")
            .districtId(null)
            .stateOrProvinceId(1L)
            .countryId(1L)
            .build();

        String request = objectWriter.writeValueAsString(addressPostVm);

        this.mockMvc.perform(post("/storefront/addresses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateAddress_whenRequestIsValid_thenReturnOk() throws Exception {
        AddressPostVm addressPostVm = AddressPostVm.builder()
            .contactName("contactName")
            .phone("12345678")
            .addressLine1("addressLine1")
            .addressLine2("addressLine2")
            .city("city")
            .zipCode("zipCode")
            .districtId(1L)
            .stateOrProvinceId(1L)
            .countryId(1L)
            .build();

        String request = objectWriter.writeValueAsString(addressPostVm);

        this.mockMvc.perform(put("/storefront/addresses/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isNoContent());
    }

    @Test
    void testUpdateAddress_whenPhoneIsOverMaxLength_thenReturnBadRequest() throws Exception {
        AddressPostVm addressPostVm = AddressPostVm.builder()
            .contactName("contactName")
            .phone("12345678912345678912345678")
            .addressLine1("addressLine1")
            .addressLine2("addressLine2")
            .city("city")
            .zipCode("zipCode")
            .districtId(1L)
            .stateOrProvinceId(1L)
            .countryId(1L)
            .build();

        String request = objectWriter.writeValueAsString(addressPostVm);

        this.mockMvc.perform(put("/storefront/addresses/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateAddress_whenDistrictIsNull_thenReturnBadRequest() throws Exception {
        AddressPostVm addressPostVm = AddressPostVm.builder()
            .contactName("contactName")
            .phone("12345678")
            .addressLine1("addressLine1")
            .addressLine2("addressLine2")
            .city("city")
            .zipCode("zipCode")
            .districtId(null)
            .stateOrProvinceId(1L)
            .countryId(1L)
            .build();

        String request = objectWriter.writeValueAsString(addressPostVm);

        this.mockMvc.perform(put("/storefront/addresses/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testGetAddressById_whenNotFound_thenReturn404() throws Exception {
        when(addressService.getAddress(1L))
            .thenThrow(new NotFoundException("ADDRESS_NOT_FOUND", 1L));

        this.mockMvc.perform(get("/storefront/addresses/1"))
            .andExpect(status().isNotFound());
    }

    @Test
    void testGetAddressList_whenValid_thenReturnOk() throws Exception {
        when(addressService.getAddressList(List.of(1L, 2L)))
            .thenReturn(List.of(AddressDetailVm.builder().id(1L).build()));

        this.mockMvc.perform(get("/storefront/addresses")
                .param("ids", "1", "2"))
            .andExpect(status().isOk());
    }

    @Test
    void testCreateAddress_whenServiceThrows_thenReturn500() throws Exception {
        AddressPostVm addressPostVm = AddressPostVm.builder()
            .contactName("contactName")
            .phone("12345678")
            .addressLine1("addressLine1")
            .addressLine2("addressLine2")
            .city("city")
            .zipCode("zipCode")
            .districtId(1L)
            .stateOrProvinceId(1L)
            .countryId(1L)
            .build();

        String request = objectWriter.writeValueAsString(addressPostVm);
        when(addressService.createAddress(any(AddressPostVm.class)))
            .thenThrow(new RuntimeException("boom"));

        this.mockMvc.perform(post("/storefront/addresses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isInternalServerError());
    }
}