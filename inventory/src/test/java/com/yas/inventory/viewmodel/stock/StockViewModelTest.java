package com.yas.inventory.viewmodel.stock;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yas.inventory.model.Stock;
import com.yas.inventory.model.Warehouse;
import com.yas.inventory.viewmodel.product.ProductInfoVm;
import java.util.List;
import org.junit.jupiter.api.Test;

class StockViewModelTest {

    @Test
    void stockVm_fromModel_mapsFields() {
        Warehouse warehouse = Warehouse.builder().id(1L).name("W").build();
        Stock stock = Stock.builder()
            .id(10L)
            .productId(20L)
            .quantity(5L)
            .reservedQuantity(1L)
            .warehouse(warehouse)
            .build();
        ProductInfoVm product = new ProductInfoVm(20L, "Name", "SKU", true);

        StockVm result = StockVm.fromModel(stock, product);

        assertEquals(10L, result.id());
        assertEquals(20L, result.productId());
        assertEquals("Name", result.productName());
        assertEquals("SKU", result.productSku());
        assertEquals(5L, result.quantity());
        assertEquals(1L, result.reservedQuantity());
        assertEquals(1L, result.warehouseId());
    }

    @Test
    void stockQuantityViewModels_storeComponents() {
        StockQuantityVm quantityVm = new StockQuantityVm(1L, 2L, "note");
        StockQuantityUpdateVm updateVm = new StockQuantityUpdateVm(List.of(quantityVm));
        StockPostVm postVm = new StockPostVm(10L, 11L);

        assertEquals(1L, quantityVm.stockId());
        assertEquals(1, updateVm.stockQuantityList().size());
        assertEquals(10L, postVm.productId());
        assertEquals(11L, postVm.warehouseId());
    }
}
