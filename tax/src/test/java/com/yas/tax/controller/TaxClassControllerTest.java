package com.yas.tax.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
import com.yas.tax.service.TaxClassService;
import com.yas.tax.viewmodel.taxclass.TaxClassListGetVm;
import com.yas.tax.viewmodel.taxclass.TaxClassPostVm;
import com.yas.tax.viewmodel.taxclass.TaxClassVm;
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
class TaxClassControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private TaxClassService taxClassService;

    @InjectMocks
    private TaxClassController taxClassController;

    private TaxClass taxClass;
    private TaxClassVm taxClassVm;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(taxClassController)
            .setControllerAdvice(new ApiExceptionHandler())
            .build();
        objectMapper = new ObjectMapper();

        taxClass = new TaxClass();
        taxClass.setId(1L);
        taxClass.setName("Standard");

        taxClassVm = new TaxClassVm(1L, "Standard");
    }

    // ------------------------------------------------------------------ //
    // GET /tax-classes/paging                                             //
    // ------------------------------------------------------------------ //

    @Test
    void getPageableTaxClasses_whenCalled_thenReturn200WithBody() throws Exception {
        TaxClassListGetVm listVm = new TaxClassListGetVm(
            List.of(taxClassVm), 0, 10, 1, 1, true
        );
        when(taxClassService.getPageableTaxClasses(anyInt(), anyInt())).thenReturn(listVm);

        mockMvc.perform(get("/backoffice/tax-classes/paging")
                .param("pageNo", "0")
                .param("pageSize", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pageNo").value(0))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    // ------------------------------------------------------------------ //
    // GET /backoffice/tax-classes                                         //
    // ------------------------------------------------------------------ //

    @Test
    void listTaxClasses_whenCalled_thenReturn200WithList() throws Exception {
        when(taxClassService.findAllTaxClasses()).thenReturn(List.of(taxClassVm));

        mockMvc.perform(get("/backoffice/tax-classes"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1L))
            .andExpect(jsonPath("$[0].name").value("Standard"));
    }

    // ------------------------------------------------------------------ //
    // GET /tax-classes/{id}                                               //
    // ------------------------------------------------------------------ //

    @Test
    void getTaxClass_whenExists_thenReturn200() throws Exception {
        when(taxClassService.findById(1L)).thenReturn(taxClassVm);

        mockMvc.perform(get("/backoffice/tax-classes/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.name").value("Standard"));
    }

    @Test
    void getTaxClass_whenNotFound_thenReturn404() throws Exception {
        lenient().when(taxClassService.findById(99L)).thenThrow(new NotFoundException("TAX_CLASS_NOT_FOUND", 99L));

        mockMvc.perform(get("/backoffice/tax-classes/99"))
            .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ //
    // POST /tax-classes                                                   //
    // ------------------------------------------------------------------ //

    @Test
    void createTaxClass_whenValid_thenReturn201() throws Exception {
        TaxClassPostVm postVm = new TaxClassPostVm("1", "Standard");
        when(taxClassService.create(any(TaxClassPostVm.class))).thenReturn(taxClass);

        mockMvc.perform(post("/backoffice/tax-classes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(postVm)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.name").value("Standard"));
    }

    // ------------------------------------------------------------------ //
    // PUT /tax-classes/{id}                                               //
    // ------------------------------------------------------------------ //

    @Test
    void updateTaxClass_whenValid_thenReturn204() throws Exception {
        TaxClassPostVm postVm = new TaxClassPostVm("1", "Updated");
        doNothing().when(taxClassService).update(any(TaxClassPostVm.class), eq(1L));

        mockMvc.perform(put("/backoffice/tax-classes/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(postVm)))
            .andExpect(status().isNoContent());
    }

    @Test
    void updateTaxClass_whenNotFound_thenReturn404() throws Exception {
        TaxClassPostVm postVm = new TaxClassPostVm("99", "Updated");
        lenient().doThrow(new NotFoundException("TAX_CLASS_NOT_FOUND", 99L))
            .when(taxClassService).update(any(TaxClassPostVm.class), eq(99L));

        mockMvc.perform(put("/backoffice/tax-classes/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(postVm)))
            .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ //
    // DELETE /backoffice/tax-classes/{id}                                 //
    // ------------------------------------------------------------------ //

    @Test
    void deleteTaxClass_whenExists_thenReturn204() throws Exception {
        doNothing().when(taxClassService).delete(1L);

        mockMvc.perform(delete("/backoffice/tax-classes/1"))
            .andExpect(status().isNoContent());
    }

    @Test
    void deleteTaxClass_whenNotFound_thenReturn404() throws Exception {
        lenient().doThrow(new NotFoundException("TAX_CLASS_NOT_FOUND", 99L))
            .when(taxClassService).delete(99L);

        mockMvc.perform(delete("/backoffice/tax-classes/99"))
            .andExpect(status().isNotFound());
    }
}
