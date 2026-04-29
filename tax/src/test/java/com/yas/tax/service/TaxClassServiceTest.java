package com.yas.tax.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.DuplicatedException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.tax.model.TaxClass;
import com.yas.tax.repository.TaxClassRepository;
import com.yas.tax.viewmodel.taxclass.TaxClassListGetVm;
import com.yas.tax.viewmodel.taxclass.TaxClassPostVm;
import com.yas.tax.viewmodel.taxclass.TaxClassVm;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class TaxClassServiceTest {

    @Mock
    private TaxClassRepository taxClassRepository;

    @InjectMocks
    private TaxClassService taxClassService;

    private TaxClass taxClass;

    @BeforeEach
    void setUp() {
        taxClass = new TaxClass();
        taxClass.setId(1L);
        taxClass.setName("Standard");
    }

    // ------------------------------------------------------------------ //
    // findAllTaxClasses                                                    //
    // ------------------------------------------------------------------ //

    @Test
    void findAllTaxClasses_whenCalled_thenReturnSortedList() {
        when(taxClassRepository.findAll(Sort.by(Sort.Direction.ASC, "name")))
            .thenReturn(List.of(taxClass));

        List<TaxClassVm> result = taxClassService.findAllTaxClasses();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).name()).isEqualTo("Standard");
    }

    // ------------------------------------------------------------------ //
    // findById                                                             //
    // ------------------------------------------------------------------ //

    @Test
    void findById_whenExists_thenReturnTaxClassVm() {
        when(taxClassRepository.findById(1L)).thenReturn(Optional.of(taxClass));

        TaxClassVm result = taxClassService.findById(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Standard");
    }

    @Test
    void findById_whenNotFound_thenThrowNotFoundException() {
        when(taxClassRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> taxClassService.findById(99L));
    }

    // ------------------------------------------------------------------ //
    // create                                                               //
    // ------------------------------------------------------------------ //

    @Test
    void create_whenNameAlreadyExists_thenThrowDuplicatedException() {
        TaxClassPostVm postVm = new TaxClassPostVm("1", "Standard");
        when(taxClassRepository.existsByName("Standard")).thenReturn(true);

        assertThrows(DuplicatedException.class, () -> taxClassService.create(postVm));
        verify(taxClassRepository, never()).save(any());
    }

    @Test
    void create_whenValidName_thenSaveAndReturn() {
        TaxClassPostVm postVm = new TaxClassPostVm("1", "NewClass");
        when(taxClassRepository.existsByName("NewClass")).thenReturn(false);
        when(taxClassRepository.save(any(TaxClass.class))).thenReturn(taxClass);

        TaxClass result = taxClassService.create(postVm);

        assertThat(result).isNotNull();
        verify(taxClassRepository).save(any(TaxClass.class));
    }

    // ------------------------------------------------------------------ //
    // update                                                               //
    // ------------------------------------------------------------------ //

    @Test
    void update_whenTaxClassNotFound_thenThrowNotFoundException() {
        TaxClassPostVm postVm = new TaxClassPostVm("99", "Updated");
        when(taxClassRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> taxClassService.update(postVm, 99L));
    }

    @Test
    void update_whenNameAlreadyUsedByOther_thenThrowDuplicatedException() {
        TaxClassPostVm postVm = new TaxClassPostVm("1", "Duplicate");
        when(taxClassRepository.findById(1L)).thenReturn(Optional.of(taxClass));
        when(taxClassRepository.existsByNameNotUpdatingTaxClass("Duplicate", 1L)).thenReturn(true);

        assertThrows(DuplicatedException.class, () -> taxClassService.update(postVm, 1L));
        verify(taxClassRepository, never()).save(any());
    }

    @Test
    void update_whenValid_thenUpdateAndSave() {
        TaxClassPostVm postVm = new TaxClassPostVm("1", "UpdatedName");
        when(taxClassRepository.findById(1L)).thenReturn(Optional.of(taxClass));
        when(taxClassRepository.existsByNameNotUpdatingTaxClass("UpdatedName", 1L)).thenReturn(false);
        when(taxClassRepository.save(any(TaxClass.class))).thenReturn(taxClass);

        taxClassService.update(postVm, 1L);

        assertThat(taxClass.getName()).isEqualTo("UpdatedName");
        verify(taxClassRepository).save(taxClass);
    }

    // ------------------------------------------------------------------ //
    // delete                                                               //
    // ------------------------------------------------------------------ //

    @Test
    void delete_whenNotFound_thenThrowNotFoundException() {
        when(taxClassRepository.existsById(99L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> taxClassService.delete(99L));
        verify(taxClassRepository, never()).deleteById(anyLong());
    }

    @Test
    void delete_whenExists_thenDeleteById() {
        when(taxClassRepository.existsById(1L)).thenReturn(true);

        taxClassService.delete(1L);

        verify(taxClassRepository).deleteById(1L);
    }

    // ------------------------------------------------------------------ //
    // getPageableTaxClasses                                                //
    // ------------------------------------------------------------------ //

    @Test
    void getPageableTaxClasses_whenCalled_thenReturnPagedResult() {
        Page<TaxClass> page = new PageImpl<>(List.of(taxClass), PageRequest.of(0, 10), 1);
        when(taxClassRepository.findAll(any(PageRequest.class))).thenReturn(page);

        TaxClassListGetVm result = taxClassService.getPageableTaxClasses(0, 10);

        assertThat(result.taxClassContent()).hasSize(1);
        assertThat(result.pageNo()).isZero();
        assertThat(result.pageSize()).isEqualTo(10);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.totalPages()).isEqualTo(1);
        assertThat(result.isLast()).isTrue();
    }
}
