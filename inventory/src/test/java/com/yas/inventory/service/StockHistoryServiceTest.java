package com.yas.inventory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.inventory.model.Stock;
import com.yas.inventory.model.StockHistory;
import com.yas.inventory.model.Warehouse;
import com.yas.inventory.repository.StockHistoryRepository;
import com.yas.inventory.viewmodel.product.ProductInfoVm;
import com.yas.inventory.viewmodel.stock.StockQuantityVm;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StockHistoryServiceTest {

    @Mock
    private StockHistoryRepository stockHistoryRepository;

    @Mock
    private ProductService productService;

    @InjectMocks
    private StockHistoryService stockHistoryService;

    @Test
    void createStockHistories_whenNoMatchingStockId_savesEmptyList() {
        Stock stock = Stock.builder().id(1L).productId(10L).warehouse(Warehouse.builder().id(1L).build()).build();
        List<StockQuantityVm> quantities = List.of(new StockQuantityVm(2L, 1L, "note"));

        stockHistoryService.createStockHistories(List.of(stock), quantities);

        ArgumentCaptor<List<StockHistory>> captor = ArgumentCaptor.forClass(List.class);
        verify(stockHistoryRepository).saveAll(captor.capture());
        assertEquals(0, captor.getValue().size());
    }

    @Test
    void createStockHistories_whenMatch_savesHistory() {
        Warehouse warehouse = Warehouse.builder().id(1L).build();
        Stock stock = Stock.builder().id(1L).productId(10L).warehouse(warehouse).build();
        List<StockQuantityVm> quantities = List.of(new StockQuantityVm(1L, 5L, "adjust"));

        stockHistoryService.createStockHistories(List.of(stock), quantities);

        ArgumentCaptor<List<StockHistory>> captor = ArgumentCaptor.forClass(List.class);
        verify(stockHistoryRepository).saveAll(captor.capture());
        assertEquals(1, captor.getValue().size());
        assertEquals(10L, captor.getValue().getFirst().getProductId());
        assertEquals("adjust", captor.getValue().getFirst().getNote());
    }

    @Test
    void getStockHistories_returnsMappedList() {
        Warehouse warehouse = Warehouse.builder().id(1L).build();
        StockHistory history = StockHistory.builder().id(1L).productId(10L).warehouse(warehouse).build();
        when(stockHistoryRepository.findByProductIdAndWarehouseIdOrderByCreatedOnDesc(10L, 1L))
            .thenReturn(List.of(history));
        when(productService.getProduct(10L)).thenReturn(new ProductInfoVm(10L, "Name", "SKU", true));

        var result = stockHistoryService.getStockHistories(10L, 1L);

        assertEquals(1, result.data().size());
    }
}
