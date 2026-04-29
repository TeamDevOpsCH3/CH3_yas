package com.yas.tax.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.tax.model.TaxClass;
import com.yas.tax.model.TaxRate;
import com.yas.tax.repository.TaxClassRepository;
import com.yas.tax.repository.TaxRateRepository;
import com.yas.tax.viewmodel.location.StateOrProvinceAndCountryGetNameVm;
import com.yas.tax.viewmodel.taxrate.TaxRateListGetVm;
import com.yas.tax.viewmodel.taxrate.TaxRatePostVm;
import com.yas.tax.viewmodel.taxrate.TaxRateVm;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class TaxRateServiceTest {

    @Mock
    private TaxRateRepository taxRateRepository;
    @Mock
    private TaxClassRepository taxClassRepository;
    @Mock
    private LocationService locationService;

    @InjectMocks
    private TaxRateService taxRateService;

    private TaxClass taxClass;
    private TaxRate taxRate;

    @BeforeEach
    void setUp() {
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
        // simulate persisted ID
        taxRate.setId(10L);
    }

    // ------------------------------------------------------------------ //
    // createTaxRate                                                        //
    // ------------------------------------------------------------------ //

    @Test
    void createTaxRate_whenTaxClassNotFound_thenThrowNotFoundException() {
        TaxRatePostVm postVm = new TaxRatePostVm(10.0, "10000", 99L, 2L, 3L);
        when(taxClassRepository.existsById(99L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> taxRateService.createTaxRate(postVm));
        verify(taxRateRepository, never()).save(any());
    }

    @Test
    void createTaxRate_whenValid_thenSaveAndReturn() {
        TaxRatePostVm postVm = new TaxRatePostVm(10.0, "10000", 1L, 2L, 3L);
        when(taxClassRepository.existsById(1L)).thenReturn(true);
        when(taxClassRepository.getReferenceById(1L)).thenReturn(taxClass);
        when(taxRateRepository.save(any(TaxRate.class))).thenReturn(taxRate);

        TaxRate result = taxRateService.createTaxRate(postVm);

        assertThat(result).isNotNull();
        assertThat(result.getRate()).isEqualTo(10.0);
        verify(taxRateRepository).save(any(TaxRate.class));
    }

    // ------------------------------------------------------------------ //
    // updateTaxRate                                                        //
    // ------------------------------------------------------------------ //

    @Test
    void updateTaxRate_whenTaxRateNotFound_thenThrowNotFoundException() {
        TaxRatePostVm postVm = new TaxRatePostVm(5.0, "20000", 1L, 2L, 3L);
        when(taxRateRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> taxRateService.updateTaxRate(postVm, 99L));
    }

    @Test
    void updateTaxRate_whenTaxClassNotFound_thenThrowNotFoundException() {
        TaxRatePostVm postVm = new TaxRatePostVm(5.0, "20000", 99L, 2L, 3L);
        when(taxRateRepository.findById(10L)).thenReturn(Optional.of(taxRate));
        when(taxClassRepository.existsById(99L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> taxRateService.updateTaxRate(postVm, 10L));
        verify(taxRateRepository, never()).save(any());
    }

    @Test
    void updateTaxRate_whenValid_thenUpdateAndSave() {
        TaxRatePostVm postVm = new TaxRatePostVm(15.0, "30000", 1L, 4L, 5L);
        when(taxRateRepository.findById(10L)).thenReturn(Optional.of(taxRate));
        when(taxClassRepository.existsById(1L)).thenReturn(true);
        when(taxClassRepository.getReferenceById(1L)).thenReturn(taxClass);
        when(taxRateRepository.save(any(TaxRate.class))).thenReturn(taxRate);

        taxRateService.updateTaxRate(postVm, 10L);

        assertThat(taxRate.getRate()).isEqualTo(15.0);
        assertThat(taxRate.getZipCode()).isEqualTo("30000");
        verify(taxRateRepository).save(taxRate);
    }

    // ------------------------------------------------------------------ //
    // delete                                                               //
    // ------------------------------------------------------------------ //

    @Test
    void delete_whenNotFound_thenThrowNotFoundException() {
        when(taxRateRepository.existsById(99L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> taxRateService.delete(99L));
        verify(taxRateRepository, never()).deleteById(anyLong());
    }

    @Test
    void delete_whenExists_thenDeleteById() {
        when(taxRateRepository.existsById(10L)).thenReturn(true);

        taxRateService.delete(10L);

        verify(taxRateRepository).deleteById(10L);
    }

    // ------------------------------------------------------------------ //
    // findById                                                             //
    // ------------------------------------------------------------------ //

    @Test
    void findById_whenExists_thenReturnTaxRateVm() {
        when(taxRateRepository.findById(10L)).thenReturn(Optional.of(taxRate));

        TaxRateVm result = taxRateService.findById(10L);

        assertThat(result.rate()).isEqualTo(10.0);
        assertThat(result.zipCode()).isEqualTo("10000");
    }

    @Test
    void findById_whenNotFound_thenThrowNotFoundException() {
        when(taxRateRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> taxRateService.findById(99L));
    }

    // ------------------------------------------------------------------ //
    // findAll                                                              //
    // ------------------------------------------------------------------ //

    @Test
    void findAll_whenCalled_thenReturnList() {
        when(taxRateRepository.findAll()).thenReturn(List.of(taxRate));

        List<TaxRateVm> result = taxRateService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(10L);
    }

    // ------------------------------------------------------------------ //
    // getPageableTaxRates                                                  //
    // ------------------------------------------------------------------ //

    @Test
    void getPageableTaxRates_whenStateOrProvinceIdsEmpty_thenReturnEmptyDetailList() {
        TaxRate rateWithNoProvince = TaxRate.builder()
            .rate(5.0).zipCode("00000").taxClass(taxClass)
            .stateOrProvinceId(null).countryId(1L).build();
        rateWithNoProvince.setId(20L);

        Page<TaxRate> page = new PageImpl<>(List.of(rateWithNoProvince), PageRequest.of(0, 10), 1);
        when(taxRateRepository.findAll(any(PageRequest.class))).thenReturn(page);

        TaxRateListGetVm result = taxRateService.getPageableTaxRates(0, 10);

        // stateOrProvinceId is null → stream of ids will have null → locationService NOT called
        assertThat(result.taxRateGetDetailContent()).isEmpty();
    }

    @Test
    void getPageableTaxRates_whenStateOrProvinceIdsPresent_thenCallLocationService() {
        Page<TaxRate> page = new PageImpl<>(List.of(taxRate), PageRequest.of(0, 10), 1);
        when(taxRateRepository.findAll(any(PageRequest.class))).thenReturn(page);

        StateOrProvinceAndCountryGetNameVm locationVm =
            new StateOrProvinceAndCountryGetNameVm(2L, "Ha Noi", "Vietnam");
        when(locationService.getStateOrProvinceAndCountryNames(List.of(2L)))
            .thenReturn(List.of(locationVm));

        TaxRateListGetVm result = taxRateService.getPageableTaxRates(0, 10);

        assertThat(result.taxRateGetDetailContent()).hasSize(1);
        assertThat(result.taxRateGetDetailContent().get(0).stateOrProvinceName()).isEqualTo("Ha Noi");
        assertThat(result.taxRateGetDetailContent().get(0).countryName()).isEqualTo("Vietnam");
    }

    // ------------------------------------------------------------------ //
    // getTaxPercent                                                        //
    // ------------------------------------------------------------------ //

    @Test
    void getTaxPercent_whenFound_thenReturnValue() {
        when(taxRateRepository.getTaxPercent(3L, 2L, "10000", 1L)).thenReturn(10.0);

        double result = taxRateService.getTaxPercent(1L, 3L, 2L, "10000");

        assertThat(result).isEqualTo(10.0);
    }

    @Test
    void getTaxPercent_whenNotFound_thenReturnZero() {
        when(taxRateRepository.getTaxPercent(3L, 2L, "10000", 1L)).thenReturn(null);

        double result = taxRateService.getTaxPercent(1L, 3L, 2L, "10000");

        assertThat(result).isZero();
    }

    // ------------------------------------------------------------------ //
    // getBulkTaxRate                                                       //
    // ------------------------------------------------------------------ //

    @Test
    void getBulkTaxRate_whenCalled_thenReturnList() {
        when(taxRateRepository.getBatchTaxRates(3L, 2L, "10000", Set.of(1L)))
            .thenReturn(List.of(taxRate));

        List<TaxRateVm> result = taxRateService.getBulkTaxRate(List.of(1L), 3L, 2L, "10000");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).rate()).isEqualTo(10.0);
    }
}
