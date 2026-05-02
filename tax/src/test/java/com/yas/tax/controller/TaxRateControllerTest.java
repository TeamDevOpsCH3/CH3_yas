package com.yas.tax.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yas.commonlibrary.exception.ApiExceptionHandler;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.tax.model.TaxClass;
import com.yas.tax.model.TaxRate;
import com.yas.tax.service.TaxRateService;
import com.yas.tax.viewmodel.taxrate.TaxRateListGetVm;
import com.yas.tax.viewmodel.taxrate.TaxRatePostVm;
import com.yas.tax.viewmodel.taxrate.TaxRateVm;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class TaxRateControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private TaxRateService taxRateService;

    @InjectMocks
    private TaxRateController taxRateController;

    private TaxClass taxClass;
    private TaxRate taxRate;
    private TaxRateVm taxRateVm;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(taxRateController)
            .setControllerAdvice(new ApiExceptionHandler())
            .build();
        objectMapper = new ObjectMapper();

        taxClass = new TaxClass();
        taxClass.setId(1L);
        taxClass.setName("Standard");

        taxRate = TaxRate.builder()
            .rate(10.0)
            .zipCode("10000")
            .taxClass(taxClass)
            .stateOrProvinceId(2L)
            .countryId(3L)
            .build();
        taxRate.setId(10L);

        taxRateVm = TaxRateVm.fromModel(taxRate);
    }

    // ------------------------------------------------------------------ //
    // GET /backoffice/tax-rates/paging                                    //
    // ------------------------------------------------------------------ //

    @Test
    void getPageableTaxRates_whenCalled_thenReturn200WithBody() throws Exception {
        TaxRateListGetVm listVm = new TaxRateListGetVm(List.of(), 0, 10, 0, 0, true);
        when(taxRateService.getPageableTaxRates(anyInt(), anyInt())).thenReturn(listVm);

        mockMvc.perform(get("/backoffice/tax-rates/paging")
                .param("pageNo", "0")
                .param("pageSize", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pageNo").value(0))
            .andExpect(jsonPath("$.totalElements").value(0));
    }

    // ------------------------------------------------------------------ //
    // GET /backoffice/tax-rates/{id}                                      //
    // ------------------------------------------------------------------ //

    @Test
    void getTaxRate_whenExists_thenReturn200() throws Exception {
        when(taxRateService.findById(10L)).thenReturn(taxRateVm);

        mockMvc.perform(get("/backoffice/tax-rates/10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(10L))
            .andExpect(jsonPath("$.rate").value(10.0))
            .andExpect(jsonPath("$.zipCode").value("10000"));
    }

    @Test
    void getTaxRate_whenNotFound_thenReturn404() throws Exception {
        lenient().when(taxRateService.findById(99L))
            .thenThrow(new NotFoundException("TAX_RATE_NOT_FOUND", 99L));

        mockMvc.perform(get("/backoffice/tax-rates/99"))
            .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ //
    // POST /backoffice/tax-rates                                          //
    // ------------------------------------------------------------------ //

    @Test
    void createTaxRate_whenValid_thenReturn201() throws Exception {
        TaxRatePostVm postVm = new TaxRatePostVm(10.0, "10000", 1L, 2L, 3L);
        when(taxRateService.createTaxRate(any(TaxRatePostVm.class))).thenReturn(taxRate);

        mockMvc.perform(post("/backoffice/tax-rates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(postVm)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.rate").value(10.0))
            .andExpect(jsonPath("$.zipCode").value("10000"));
    }

    @Test
    void createTaxRate_whenTaxClassNotFound_thenReturn404() throws Exception {
        TaxRatePostVm postVm = new TaxRatePostVm(10.0, "10000", 99L, 2L, 3L);
        lenient().when(taxRateService.createTaxRate(any(TaxRatePostVm.class)))
            .thenThrow(new NotFoundException("TAX_CLASS_NOT_FOUND", 99L));

        mockMvc.perform(post("/backoffice/tax-rates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(postVm)))
            .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ //
    // PUT /backoffice/tax-rates/{id}                                      //
    // ------------------------------------------------------------------ //

    @Test
    void updateTaxRate_whenValid_thenReturn204() throws Exception {
        TaxRatePostVm postVm = new TaxRatePostVm(15.0, "20000", 1L, 2L, 3L);
        doNothing().when(taxRateService).updateTaxRate(any(TaxRatePostVm.class), eq(10L));

        mockMvc.perform(put("/backoffice/tax-rates/10")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(postVm)))
            .andExpect(status().isNoContent());
    }

    @Test
    void updateTaxRate_whenNotFound_thenReturn404() throws Exception {
        TaxRatePostVm postVm = new TaxRatePostVm(15.0, "20000", 1L, 2L, 3L);
        lenient().doThrow(new NotFoundException("TAX_RATE_NOT_FOUND", 99L))
            .when(taxRateService).updateTaxRate(any(TaxRatePostVm.class), eq(99L));

        mockMvc.perform(put("/backoffice/tax-rates/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(postVm)))
            .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ //
    // DELETE /backoffice/tax-rates/{id}                                   //
    // ------------------------------------------------------------------ //

    @Test
    void deleteTaxRate_whenExists_thenReturn204() throws Exception {
        doNothing().when(taxRateService).delete(10L);

        mockMvc.perform(delete("/backoffice/tax-rates/10"))
            .andExpect(status().isNoContent());
    }

    @Test
    void deleteTaxRate_whenNotFound_thenReturn404() throws Exception {
        lenient().doThrow(new NotFoundException("TAX_RATE_NOT_FOUND", 99L))
            .when(taxRateService).delete(99L);

        mockMvc.perform(delete("/backoffice/tax-rates/99"))
            .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ //
    // GET /backoffice/tax-rates/tax-percent                               //
    // ------------------------------------------------------------------ //

    @Test
    void getTaxPercentByAddress_whenCalled_thenReturn200WithValue() throws Exception {
        when(taxRateService.getTaxPercent(1L, 3L, 2L, "10000")).thenReturn(10.0);

        mockMvc.perform(get("/backoffice/tax-rates/tax-percent")
                .param("taxClassId", "1")
                .param("countryId", "3")
                .param("stateOrProvinceId", "2")
                .param("zipCode", "10000"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value(10.0));
    }

    @Test
    void getTaxPercentByAddress_whenNoMatch_thenReturn200WithZero() throws Exception {
        when(taxRateService.getTaxPercent(anyLong(), anyLong(), anyLong(), anyString()))
            .thenReturn(0.0);

        mockMvc.perform(get("/backoffice/tax-rates/tax-percent")
                .param("taxClassId", "1")
                .param("countryId", "3")
                .param("stateOrProvinceId", "2")
                .param("zipCode", "99999"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value(0.0));
    }

    // ------------------------------------------------------------------ //
    // GET /backoffice/tax-rates/location-based-batch                      //
    // ------------------------------------------------------------------ //

    @Test
    void getBatchTaxPercentsByAddress_whenCalled_thenReturn200WithList() throws Exception {
        when(taxRateService.getBulkTaxRate(List.of(1L), 3L, 2L, "10000"))
            .thenReturn(List.of(taxRateVm));

        mockMvc.perform(get("/backoffice/tax-rates/location-based-batch")
                .param("taxClassIds", "1")
                .param("countryId", "3")
                .param("stateOrProvinceId", "2")
                .param("zipCode", "10000"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].rate").value(10.0));
    }

    @Test
    void getBatchTaxPercentsByAddress_whenNoMatch_thenReturn200WithEmptyList() throws Exception {
        when(taxRateService.getBulkTaxRate(any(), anyLong(), anyLong(), anyString()))
            .thenReturn(List.of());

        mockMvc.perform(get("/backoffice/tax-rates/location-based-batch")
                .param("taxClassIds", "99")
                .param("countryId", "3")
                .param("stateOrProvinceId", "2")
                .param("zipCode", "00000"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isEmpty());
    }
}
