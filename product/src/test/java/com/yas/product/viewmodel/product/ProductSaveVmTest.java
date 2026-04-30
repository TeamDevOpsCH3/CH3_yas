package com.yas.product.viewmodel.product;

import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.junit.jupiter.api.Test;

class ProductSaveVmTest {

    @Test
    void defaultId_returnsNull() {
        ProductSaveVm<ProductVariationPostVm> vm = new ProductSaveVm<>() {
            @Override
            public List<ProductVariationPostVm> variations() {
                return List.of();
            }

            @Override
            public Boolean isPublished() {
                return false;
            }

            @Override
            public Double length() {
                return null;
            }

            @Override
            public Double width() {
                return null;
            }

            @Override
            public String name() {
                return "name";
            }

            @Override
            public String slug() {
                return "slug";
            }

            @Override
            public String sku() {
                return "sku";
            }

            @Override
            public String gtin() {
                return "gtin";
            }

            @Override
            public Double price() {
                return 1.0;
            }

            @Override
            public Long thumbnailMediaId() {
                return null;
            }

            @Override
            public List<Long> productImageIds() {
                return List.of();
            }
        };

        assertNull(vm.id());
    }
}
