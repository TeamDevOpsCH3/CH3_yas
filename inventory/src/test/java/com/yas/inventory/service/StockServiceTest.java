package com.yas.inventory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.BadRequestException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.commonlibrary.exception.StockExistingException;
import com.yas.inventory.model.Stock;
import com.yas.inventory.model.Warehouse;
import com.yas.inventory.model.enumeration.FilterExistInWhSelection;
import com.yas.inventory.repository.StockRepository;
import com.yas.inventory.repository.WarehouseRepository;
import com.yas.inventory.viewmodel.product.ProductInfoVm;
import com.yas.inventory.viewmodel.stock.StockPostVm;
import com.yas.inventory.viewmodel.stock.StockQuantityUpdateVm;
import com.yas.inventory.viewmodel.stock.StockQuantityVm;
import com.yas.inventory.viewmodel.stock.StockVm;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private ProductService productService;

    @Mock
    private WarehouseService warehouseService;

    @Mock
    private StockHistoryService stockHistoryService;

    @InjectMocks
    private StockService stockService;

    @Test
    void addProductIntoWarehouse_whenExistingInStock_throwsStockExisting() {
        StockPostVm postVm = new StockPostVm(1L, 10L);
        when(stockRepository.existsByWarehouseIdAndProductId(10L, 1L)).thenReturn(true);

        assertThrows(StockExistingException.class, () -> stockService.addProductIntoWarehouse(List.of(postVm)));

        verifyNoMoreInteractions(productService, warehouseRepository);
    }

    @Test
    void addProductIntoWarehouse_whenProductMissing_throwsNotFound() {
        StockPostVm postVm = new StockPostVm(2L, 20L);
        when(stockRepository.existsByWarehouseIdAndProductId(20L, 2L)).thenReturn(false);
        when(productService.getProduct(2L)).thenReturn(null);

        assertThrows(NotFoundException.class, () -> stockService.addProductIntoWarehouse(List.of(postVm)));
    }

    @Test
    void addProductIntoWarehouse_whenWarehouseMissing_throwsNotFound() {
        StockPostVm postVm = new StockPostVm(3L, 30L);
        when(stockRepository.existsByWarehouseIdAndProductId(30L, 3L)).thenReturn(false);
        when(productService.getProduct(3L)).thenReturn(new ProductInfoVm(3L, "P3", "SKU3", true));
        when(warehouseRepository.findById(30L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> stockService.addProductIntoWarehouse(List.of(postVm)));
    }

    @Test
    void addProductIntoWarehouse_whenValid_savesStockWithZeroQuantity() {
        StockPostVm postVm = new StockPostVm(4L, 40L);
        Warehouse warehouse = Warehouse.builder().id(40L).name("W1").build();
        when(stockRepository.existsByWarehouseIdAndProductId(40L, 4L)).thenReturn(false);
        when(productService.getProduct(4L)).thenReturn(new ProductInfoVm(4L, "P4", "SKU4", true));
        when(warehouseRepository.findById(40L)).thenReturn(Optional.of(warehouse));

        stockService.addProductIntoWarehouse(List.of(postVm));

        ArgumentCaptor<List<Stock>> captor = ArgumentCaptor.forClass(List.class);
        verify(stockRepository).saveAll(captor.capture());
        Stock saved = captor.getValue().getFirst();
        assertEquals(4L, saved.getProductId());
        assertEquals(0L, saved.getQuantity());
        assertEquals(0L, saved.getReservedQuantity());
        assertEquals(warehouse, saved.getWarehouse());
    }

    @Test
    void getStocksByWarehouseIdAndProductNameAndSku_mapsStockToVm() {
        Warehouse warehouse = Warehouse.builder().id(5L).name("W5").build();
        Stock stock = Stock.builder().id(1L).productId(10L).warehouse(warehouse).quantity(5L).reservedQuantity(1L)
            .build();
        ProductInfoVm product = new ProductInfoVm(10L, "Name", "SKU", true);

        when(warehouseService.getProductWarehouse(5L, "Name", "SKU", FilterExistInWhSelection.YES))
            .thenReturn(List.of(product));
        when(stockRepository.findByWarehouseIdAndProductIdIn(5L, List.of(10L))).thenReturn(List.of(stock));

        List<StockVm> result = stockService.getStocksByWarehouseIdAndProductNameAndSku(5L, "Name", "SKU");

        assertEquals(1, result.size());
        assertEquals("Name", result.getFirst().productName());
        assertEquals("SKU", result.getFirst().productSku());
        assertEquals(5L, result.getFirst().quantity());
    }

    @Test
    void updateProductQuantityInStock_whenNoStocks_skipsProductUpdate() {
        StockQuantityUpdateVm request = new StockQuantityUpdateVm(List.of(new StockQuantityVm(1L, 1L, "note")));
        when(stockRepository.findAllById(List.of(1L))).thenReturn(List.of());

        stockService.updateProductQuantityInStock(request);

        verify(stockRepository).saveAll(List.of());
        verify(stockHistoryService).createStockHistories(List.of(), request.stockQuantityList());
        verifyNoInteractions(productService);
    }

    @Test
    void updateProductQuantityInStock_whenNoMatchingStock_skipsAdjustment() {
        Stock stock = Stock.builder().id(1L).productId(10L).quantity(5L).warehouse(Warehouse.builder().id(1L).build())
            .build();
        StockQuantityUpdateVm request = new StockQuantityUpdateVm(List.of(new StockQuantityVm(2L, 2L, "note")));
        when(stockRepository.findAllById(List.of(2L))).thenReturn(List.of(stock));

        stockService.updateProductQuantityInStock(request);

        ArgumentCaptor<List<Stock>> captor = ArgumentCaptor.forClass(List.class);
        verify(stockRepository).saveAll(captor.capture());
        assertEquals(5L, captor.getValue().getFirst().getQuantity());
    }

    @Test
    void updateProductQuantityInStock_whenInvalidNegative_throwsBadRequest() {
        Stock stock = Stock.builder().id(1L).productId(10L).quantity(-5L)
            .warehouse(Warehouse.builder().id(1L).build()).build();
        StockQuantityUpdateVm request = new StockQuantityUpdateVm(List.of(new StockQuantityVm(1L, -1L, "note")));
        when(stockRepository.findAllById(List.of(1L))).thenReturn(List.of(stock));

        assertThrows(BadRequestException.class, () -> stockService.updateProductQuantityInStock(request));
    }

    @Test
    void updateProductQuantityInStock_whenNullQuantity_keepsQuantity() {
        Stock stock = Stock.builder().id(1L).productId(10L).quantity(5L)
            .warehouse(Warehouse.builder().id(1L).build()).build();
        StockQuantityUpdateVm request = new StockQuantityUpdateVm(List.of(new StockQuantityVm(1L, null, "note")));
        when(stockRepository.findAllById(List.of(1L))).thenReturn(List.of(stock));

        stockService.updateProductQuantityInStock(request);

        ArgumentCaptor<List<Stock>> captor = ArgumentCaptor.forClass(List.class);
        verify(stockRepository).saveAll(captor.capture());
        assertEquals(5L, captor.getValue().getFirst().getQuantity());
    }

    @Test
    void updateProductQuantityInStock_whenValid_updatesQuantityAndProduct() {
        Stock stock = Stock.builder().id(1L).productId(10L).quantity(5L)
            .warehouse(Warehouse.builder().id(1L).build()).build();
        StockQuantityUpdateVm request = new StockQuantityUpdateVm(List.of(new StockQuantityVm(1L, 3L, "note")));
        when(stockRepository.findAllById(List.of(1L))).thenReturn(List.of(stock));

        stockService.updateProductQuantityInStock(request);

        ArgumentCaptor<List<Stock>> captor = ArgumentCaptor.forClass(List.class);
        verify(stockRepository).saveAll(captor.capture());
        assertEquals(8L, captor.getValue().getFirst().getQuantity());
        verify(productService).updateProductQuantity(any());
    }
}
