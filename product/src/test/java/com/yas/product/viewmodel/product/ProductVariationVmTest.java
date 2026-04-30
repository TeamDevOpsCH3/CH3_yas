package com.yas.product.viewmodel.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProductVariationVmTest {

    @Test
    void variationPostVm_idIsNull() {
        ProductVariationPostVm postVm = new ProductVariationPostVm(
                "name",
                "slug",
                "sku",
                "gtin",
                10.0,
                1L,
                List.of(2L, 3L),
                Map.of(5L, "opt")
        );

        assertNull(postVm.id());
    }

    @Test
    void variationPutVm_returnsProvidedId() {
        ProductVariationPutVm putVm = new ProductVariationPutVm(
                10L,
                "name",
                "slug",
                "sku",
                "gtin",
                11.0,
                2L,
                List.of(),
                Map.of()
        );

        assertEquals(10L, putVm.id());
    }
}
