package com.yas.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.product.model.Brand;
import com.yas.product.model.Category;
import com.yas.product.model.Product;
import com.yas.product.model.ProductCategory;
import com.yas.product.model.ProductImage;
import com.yas.product.model.ProductOptionCombination;
import com.yas.product.model.ProductRelated;
import com.yas.product.repository.BrandRepository;
import com.yas.product.repository.CategoryRepository;
import com.yas.product.repository.ProductCategoryRepository;
import com.yas.product.repository.ProductImageRepository;
import com.yas.product.repository.ProductOptionCombinationRepository;
import com.yas.product.repository.ProductOptionRepository;
import com.yas.product.repository.ProductOptionValueRepository;
import com.yas.product.repository.ProductRelatedRepository;
import com.yas.product.repository.ProductRepository;
import com.yas.product.viewmodel.NoFileMediaVm;
import com.yas.product.viewmodel.product.ProductCheckoutListVm;
import com.yas.product.viewmodel.product.ProductDetailGetVm;
import com.yas.product.viewmodel.product.ProductExportingDetailVm;
import com.yas.product.viewmodel.product.ProductGetCheckoutListVm;
import com.yas.product.viewmodel.product.ProductQuantityPostVm;
import com.yas.product.viewmodel.product.ProductThumbnailGetVm;
import com.yas.product.viewmodel.product.ProductsGetVm;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
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
class ProductServiceOtherTest {

    @Mock private ProductRepository productRepository;
    @Mock private MediaService mediaService;
    @Mock private BrandRepository brandRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private ProductCategoryRepository productCategoryRepository;
    @Mock private ProductImageRepository productImageRepository;
    @Mock private ProductOptionRepository productOptionRepository;
    @Mock private ProductOptionValueRepository productOptionValueRepository;
    @Mock private ProductOptionCombinationRepository productOptionCombinationRepository;
    @Mock private ProductRelatedRepository productRelatedRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void getProductDetail_WhenFound_ShouldReturnProductDetailGetVm() {
        Product p = new Product();
        p.setId(1L);
        p.setName("Product 1");
        p.setSlug("product-1");
        p.setThumbnailMediaId(100L);
        p.setProductImages(List.of(ProductImage.builder().imageId(101L).build()));
        
        Brand b = new Brand();
        b.setName("Brand 1");
        p.setBrand(b);
        p.setProductCategories(List.of());
        p.setAttributeValues(List.of());
        
        when(productRepository.findBySlugAndIsPublishedTrue("product-1")).thenReturn(Optional.of(p));
        when(mediaService.getMedia(100L)).thenReturn(new NoFileMediaVm(100L, "", "", "", "http://thumb.jpg"));
        when(mediaService.getMedia(101L)).thenReturn(new NoFileMediaVm(101L, "", "", "", "http://image.jpg"));

        ProductDetailGetVm result = productService.getProductDetail("product-1");
        assertNotNull(result);
        assertEquals("Product 1", result.name());
        assertEquals("http://thumb.jpg", result.thumbnailMediaUrl());
        assertEquals(1, result.productImageMediaUrls().size());
    }

    @Test
    void getFeaturedProductsById_ShouldReturnThumbnailList() {
        Product p = new Product();
        p.setId(1L);
        p.setThumbnailMediaId(10L);
        when(productRepository.findAllByIdIn(List.of(1L))).thenReturn(List.of(p));
        when(mediaService.getMedia(10L)).thenReturn(new NoFileMediaVm(10L, "", "", "", "http://feature.jpg"));

        List<ProductThumbnailGetVm> result = productService.getFeaturedProductsById(List.of(1L));
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("http://feature.jpg", result.get(0).thumbnailUrl());
    }

    @Test
    void exportProducts_ShouldReturnExportList() {
        Product p = new Product();
        p.setId(1L);
        Brand b = new Brand();
        b.setId(2L);
        b.setName("Brand 2");
        p.setBrand(b);
        when(productRepository.getExportingProducts("name", "brand")).thenReturn(List.of(p));

        List<ProductExportingDetailVm> result = productService.exportProducts("name", "brand");
        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).brandId());
    }

    @Test
    void getRelatedProductsStorefront_ShouldReturnPaginatedList() {
        Product p = new Product();
        p.setId(1L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(p));

        Product related = new Product();
        related.setId(2L);
        related.setPublished(true);
        related.setThumbnailMediaId(20L);

        ProductRelated pr = new ProductRelated();
        pr.setRelatedProduct(related);

        Page<ProductRelated> page = new PageImpl<>(List.of(pr), PageRequest.of(0, 10), 1);
        when(productRelatedRepository.findAllByProduct(eq(p), any(Pageable.class))).thenReturn(page);
        when(mediaService.getMedia(20L)).thenReturn(new NoFileMediaVm(20L, "", "", "", "http://related.jpg"));

        ProductsGetVm result = productService.getRelatedProductsStorefront(1L, 0, 10);
        assertNotNull(result);
        assertEquals(1, result.productContent().size());
        assertEquals("http://related.jpg", result.productContent().get(0).thumbnailUrl());
    }

    @Test
    void updateProductQuantity_ShouldUpdateStock() {
        Product p = new Product();
        p.setId(1L);
        p.setStockQuantity(10L);
        when(productRepository.findAllByIdIn(List.of(1L))).thenReturn(List.of(p));
        
        ProductQuantityPostVm postVm = new ProductQuantityPostVm(1L, 50L);
        productService.updateProductQuantity(List.of(postVm));

        assertEquals(50L, p.getStockQuantity());
        verify(productRepository).saveAll(anyList());
    }

    @Test
    void getProductCheckoutList_ShouldReturnPaginatedList() {
        Product p = new Product();
        p.setId(1L);
        Brand b = new Brand();
        b.setId(2L);
        p.setBrand(b);
        p.setThumbnailMediaId(30L);
        Page<Product> page = new PageImpl<>(List.of(p), PageRequest.of(0, 10), 1);
        when(productRepository.findAllPublishedProductsByIds(eq(List.of(1L)), any(Pageable.class))).thenReturn(page);
        when(mediaService.getMedia(30L)).thenReturn(new NoFileMediaVm(30L, "", "", "", "http://checkout.jpg"));

        ProductGetCheckoutListVm result = productService.getProductCheckoutList(0, 10, List.of(1L));
        assertNotNull(result);
        assertEquals(1, result.productCheckoutListVms().size());
        assertEquals("http://checkout.jpg", result.productCheckoutListVms().get(0).thumbnailUrl());
    }
}
