package com.yas.inventory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.DuplicatedException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.inventory.model.Warehouse;
import com.yas.inventory.model.enumeration.FilterExistInWhSelection;
import com.yas.inventory.repository.StockRepository;
import com.yas.inventory.repository.WarehouseRepository;
import com.yas.inventory.viewmodel.address.AddressDetailVm;
import com.yas.inventory.viewmodel.address.AddressPostVm;
import com.yas.inventory.viewmodel.address.AddressVm;
import com.yas.inventory.viewmodel.product.ProductInfoVm;
import com.yas.inventory.viewmodel.warehouse.WarehousePostVm;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WarehouseServiceTest {

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private ProductService productService;

    @Mock
    private LocationService locationService;

    @InjectMocks
    private WarehouseService warehouseService;

    @Test
    void getProductWarehouse_whenProductIdsEmpty_returnsFilteredList() {
        ProductInfoVm product = new ProductInfoVm(1L, "Name", "SKU", false);
        when(stockRepository.getProductIdsInWarehouse(1L)).thenReturn(List.of());
        when(productService.filterProducts("Name", "SKU", List.of(), FilterExistInWhSelection.YES))
            .thenReturn(List.of(product));

        List<ProductInfoVm> result = warehouseService.getProductWarehouse(1L, "Name", "SKU",
            FilterExistInWhSelection.YES);

        assertEquals(1, result.size());
        assertEquals(false, result.getFirst().existInWh());
    }

    @Test
    void getProductWarehouse_whenProductIdsPresent_setsExistFlag() {
        ProductInfoVm product = new ProductInfoVm(2L, "Name", "SKU", false);
        when(stockRepository.getProductIdsInWarehouse(1L)).thenReturn(List.of(2L));
        when(productService.filterProducts("Name", "SKU", List.of(2L), FilterExistInWhSelection.NO))
            .thenReturn(List.of(product));

        List<ProductInfoVm> result = warehouseService.getProductWarehouse(1L, "Name", "SKU",
            FilterExistInWhSelection.NO);

        assertEquals(1, result.size());
        assertEquals(true, result.getFirst().existInWh());
    }

    @Test
    void findById_whenNotFound_throwsNotFound() {
        when(warehouseRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> warehouseService.findById(9L));
    }

    @Test
    void create_whenDuplicateName_throwsDuplicated() {
        WarehousePostVm postVm = buildPostVm();
        when(warehouseRepository.existsByName(postVm.name())).thenReturn(true);

        assertThrows(DuplicatedException.class, () -> warehouseService.create(postVm));
        verifyNoInteractions(locationService);
    }

    @Test
    void update_whenDuplicateName_throwsDuplicated() {
        WarehousePostVm postVm = buildPostVm();
        when(warehouseRepository.findById(5L)).thenReturn(Optional.of(Warehouse.builder().id(5L).build()));
        when(warehouseRepository.existsByNameWithDifferentId(postVm.name(), 5L)).thenReturn(true);

        assertThrows(DuplicatedException.class, () -> warehouseService.update(postVm, 5L));
    }

    @Test
    void delete_whenNotFound_throwsNotFound() {
        when(warehouseRepository.findById(7L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> warehouseService.delete(7L));
    }

    @Test
    void delete_whenFound_deletesWarehouseAndAddress() {
        Warehouse warehouse = Warehouse.builder().id(8L).addressId(100L).build();
        when(warehouseRepository.findById(8L)).thenReturn(Optional.of(warehouse));

        warehouseService.delete(8L);

        verify(warehouseRepository).deleteById(8L);
        verify(locationService).deleteAddress(100L);
    }

    @Test
    void update_whenFound_updatesWarehouseAndAddress() {
        WarehousePostVm postVm = buildPostVm();
        Warehouse warehouse = Warehouse.builder().id(6L).addressId(101L).build();
        when(warehouseRepository.findById(6L)).thenReturn(Optional.of(warehouse));
        when(warehouseRepository.existsByNameWithDifferentId(postVm.name(), 6L)).thenReturn(false);

        warehouseService.update(postVm, 6L);

        verify(locationService).updateAddress(101L, new AddressPostVm(
            postVm.contactName(),
            postVm.phone(),
            postVm.addressLine1(),
            postVm.addressLine2(),
            postVm.city(),
            postVm.zipCode(),
            postVm.districtId(),
            postVm.stateOrProvinceId(),
            postVm.countryId()
        ));
        verify(warehouseRepository).save(warehouse);
    }

    @Test
    void findById_whenFound_mapsAddressFields() {
        Warehouse warehouse = Warehouse.builder().id(11L).name("W11").addressId(201L).build();
        AddressDetailVm address = AddressDetailVm.builder()
            .id(201L)
            .contactName("Name")
            .phone("123")
            .addressLine1("Line1")
            .addressLine2("Line2")
            .city("City")
            .zipCode("Zip")
            .districtId(1L)
            .stateOrProvinceId(2L)
            .countryId(3L)
            .build();

        when(warehouseRepository.findById(11L)).thenReturn(Optional.of(warehouse));
        when(locationService.getAddressById(201L)).thenReturn(address);

        var result = warehouseService.findById(11L);

        assertEquals("Name", result.contactName());
        assertEquals("Line1", result.addressLine1());
        assertEquals(3L, result.countryId());
    }

    @Test
    void create_whenValid_savesWarehouseWithAddress() {
        WarehousePostVm postVm = buildPostVm();
        when(warehouseRepository.existsByName(postVm.name())).thenReturn(false);
        when(locationService.createAddress(new AddressPostVm(
            postVm.contactName(),
            postVm.phone(),
            postVm.addressLine1(),
            postVm.addressLine2(),
            postVm.city(),
            postVm.zipCode(),
            postVm.districtId(),
            postVm.stateOrProvinceId(),
            postVm.countryId()
        ))).thenReturn(AddressVm.builder().id(500L).build());

        warehouseService.create(postVm);

        verify(warehouseRepository).save(org.mockito.ArgumentMatchers.argThat(
            warehouse -> "WH".equals(warehouse.getName()) && 500L == warehouse.getAddressId()
        ));
    }

    private WarehousePostVm buildPostVm() {
        return WarehousePostVm.builder()
            .name("WH")
            .contactName("Contact")
            .phone("123")
            .addressLine1("Line1")
            .addressLine2("Line2")
            .city("City")
            .zipCode("Zip")
            .districtId(1L)
            .stateOrProvinceId(2L)
            .countryId(3L)
            .build();
    }
}
