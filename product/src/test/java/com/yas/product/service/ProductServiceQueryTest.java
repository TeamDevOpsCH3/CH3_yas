package com.yas.product.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.yas.product.model.Brand;
import com.yas.product.model.Product;
import com.yas.product.repository.ProductRepository;
import com.yas.product.viewmodel.NoFileMediaVm;
import com.yas.product.viewmodel.product.ProductListGetVm;
import com.yas.product.viewmodel.product.ProductsGetVm;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductServiceQueryTest {

    @Mock private ProductRepository productRepository;
    @Mock private MediaService mediaService;

    @InjectMocks
    private ProductService productService;

    @Test
    void getProductsByMultiQuery_ShouldReturnList() {
        Product p = new Product();
        p.setId(1L);
        p.setThumbnailMediaId(40L);
        Brand b = new Brand();
        b.setName("Query Brand");
        p.setBrand(b);

        Page<Product> page = new PageImpl<>(List.of(p), PageRequest.of(0, 10), 1);
        when(productRepository.findByProductNameAndCategorySlugAndPriceBetween(anyString(), anyString(), anyDouble(), anyDouble(), any(Pageable.class))).thenReturn(page);
        when(mediaService.getMedia(40L)).thenReturn(new NoFileMediaVm(40L, "", "", "", "http://query.jpg"));

        ProductsGetVm result = productService.getProductsByMultiQuery(0, 10, "name", "brand", 10.0, 100.0);
        assertNotNull(result);
        assertEquals(1, result.productContent().size());
    }

    @Test
    void getProductsWithFilter_ShouldReturnPaginatedList() {
        Product p = new Product();
        p.setId(1L);
        p.setThumbnailMediaId(50L);
        p.setSlug("slug");

        Page<Product> page = new PageImpl<>(List.of(p), PageRequest.of(0, 10), 1);
        when(productRepository.getProductsWithFilter(anyString(), anyString(), any(Pageable.class))).thenReturn(page);
        when(mediaService.getMedia(50L)).thenReturn(new NoFileMediaVm(50L, "", "", "", "http://filter.jpg"));

        ProductListGetVm result = productService.getProductsWithFilter(0, 10, "brand1", "category1");
        assertNotNull(result);
        assertEquals(1, result.productContent().size());
    }
}
