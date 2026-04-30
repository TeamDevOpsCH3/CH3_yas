package com.yas.product.viewmodel.product;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yas.product.model.Brand;
import com.yas.product.model.Product;
import org.junit.jupiter.api.Test;

class ProductViewModelTest {

    @Test
    void productListVm_withParent_usesParentId() {
        Product parent = new Product();
        parent.setId(1L);

        Product child = new Product();
        child.setId(2L);
        child.setName("child");
        child.setSlug("child");
        child.setAllowedToOrder(true);
        child.setPublished(true);
        child.setFeatured(false);
        child.setVisibleIndividually(true);
        child.setParent(parent);

        ProductListVm result = ProductListVm.fromModel(child);

        assertEquals(1L, result.parentId());
    }

    @Test
    void productCheckoutListVm_withParent_usesParentId() {
        Brand brand = new Brand();
        brand.setId(5L);

        Product parent = new Product();
        parent.setId(3L);

        Product child = new Product();
        child.setId(4L);
        child.setName("child");
        child.setDescription("desc");
        child.setShortDescription("short");
        child.setSku("sku");
        child.setBrand(brand);
        child.setParent(parent);

        ProductCheckoutListVm result = ProductCheckoutListVm.fromModel(child);

        assertEquals(3L, result.parentId());
        assertEquals(5L, result.brandId());
    }
}
