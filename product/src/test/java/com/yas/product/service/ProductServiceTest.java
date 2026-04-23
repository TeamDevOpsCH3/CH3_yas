package com.yas.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.product.model.Brand;
import com.yas.product.model.Category;
import com.yas.product.model.Product;
import com.yas.product.model.ProductCategory;
import com.yas.product.model.ProductImage;
import com.yas.product.model.ProductOption;
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
import com.yas.product.viewmodel.product.ProductDetailVm;
import com.yas.product.viewmodel.product.ProductEsDetailVm;
import com.yas.product.viewmodel.product.ProductFeatureGetVm;
import com.yas.product.viewmodel.product.ProductListGetVm;
import com.yas.product.viewmodel.product.ProductListVm;
import com.yas.product.viewmodel.product.ProductQuantityPutVm;
import com.yas.product.viewmodel.product.ProductSlugGetVm;
import com.yas.product.viewmodel.product.ProductThumbnailVm;
import com.yas.product.viewmodel.product.ProductsGetVm;
import com.yas.product.viewmodel.product.ProductPostVm;
import com.yas.product.viewmodel.product.ProductVariationPostVm;
import com.yas.product.viewmodel.product.ProductOptionValueDisplay;
import com.yas.product.viewmodel.product.ProductPutVm;
import com.yas.product.viewmodel.product.ProductVariationPutVm;
import com.yas.product.viewmodel.productoption.ProductOptionValuePostVm;
import com.yas.product.viewmodel.productoption.ProductOptionValuePutVm;
import com.yas.product.model.enumeration.DimensionUnit;
import com.yas.commonlibrary.exception.BadRequestException;
import com.yas.commonlibrary.exception.DuplicatedException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private MediaService mediaService;

    @Mock
    private BrandRepository brandRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductCategoryRepository productCategoryRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private ProductOptionRepository productOptionRepository;

    @Mock
    private ProductOptionValueRepository productOptionValueRepository;

    @Mock
    private ProductOptionCombinationRepository productOptionCombinationRepository;

    @Mock
    private ProductRelatedRepository productRelatedRepository;

    @InjectMocks
    private ProductService productService;

    private Product product;
    private NoFileMediaVm defaultMedia;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setId(1L);
        product.setName("Test Product");
        product.setSlug("test-product");
        product.setSku("SKU-001");
        product.setGtin("GTIN-001");
        product.setPrice(99.99);
        product.setPublished(true);
        product.setAllowedToOrder(true);
        product.setFeatured(false);
        product.setVisibleIndividually(true);
        product.setStockTrackingEnabled(false);
        product.setProductCategories(new ArrayList<>());
        product.setProductImages(new ArrayList<>());
        product.setRelatedProducts(new ArrayList<>());
        product.setProducts(new ArrayList<>());
        product.setAttributeValues(new ArrayList<>());
        product.setHasOptions(false);

        defaultMedia = new NoFileMediaVm(null, "", "", "", "");
    }

    // ─── getProductById ────────────────────────────────────────────────────────

    @Test
    void getProductById_whenProductExists_thenReturnDetailVm() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductDetailVm result = productService.getProductById(1L);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals("Test Product", result.name());
    }

    @Test
    void getProductById_whenProductNotFound_thenThrowNotFoundException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> productService.getProductById(99L));
    }

    @Test
    void getProductById_withBrand_thenReturnBrandId() {
        Brand brand = new Brand();
        brand.setId(5L);
        product.setBrand(brand);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductDetailVm result = productService.getProductById(1L);

        assertEquals(5L, result.brandId());
    }

    @Test
    void getProductById_withThumbnail_thenCallMediaService() {
        product.setThumbnailMediaId(100L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(mediaService.getMedia(100L)).thenReturn(new NoFileMediaVm(100L, "", "", "", "http://img.example.com"));

        ProductDetailVm result = productService.getProductById(1L);

        assertNotNull(result.thumbnailMedia());
        assertEquals(100L, result.thumbnailMedia().id());
    }

    @Test
    void getProductById_withProductImages_thenReturnImageList() {
        ProductImage img = new ProductImage();
        img.setImageId(200L);
        img.setProduct(product);
        product.setProductImages(List.of(img));

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(mediaService.getMedia(200L)).thenReturn(new NoFileMediaVm(200L, "", "", "", "http://img2.example.com"));

        ProductDetailVm result = productService.getProductById(1L);

        assertThat(result.productImageMedias()).hasSize(1);
    }

    @Test
    void getProductById_withCategories_thenReturnCategoryList() {
        Category category = new Category();
        category.setId(3L);
        category.setName("Electronics");
        ProductCategory productCategory = new ProductCategory();
        productCategory.setCategory(category);
        productCategory.setProduct(product);
        product.setProductCategories(List.of(productCategory));

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductDetailVm result = productService.getProductById(1L);

        assertThat(result.categories()).hasSize(1);
        assertEquals("Electronics", result.categories().getFirst().getName());
    }

    // ─── getLatestProducts ─────────────────────────────────────────────────────

    @Test
    void getLatestProducts_whenCountIsZero_thenReturnEmptyList() {
        List<ProductListVm> result = productService.getLatestProducts(0);
        assertThat(result).isEmpty();
    }

    @Test
    void getLatestProducts_whenCountIsNegative_thenReturnEmptyList() {
        List<ProductListVm> result = productService.getLatestProducts(-5);
        assertThat(result).isEmpty();
    }

    @Test
    void getLatestProducts_whenProductsExist_thenReturnList() {
        when(productRepository.getLatestProducts(any(Pageable.class))).thenReturn(List.of(product));

        List<ProductListVm> result = productService.getLatestProducts(5);

        assertThat(result).hasSize(1);
    }

    @Test
    void getLatestProducts_whenNoProducts_thenReturnEmptyList() {
        when(productRepository.getLatestProducts(any(Pageable.class))).thenReturn(Collections.emptyList());

        List<ProductListVm> result = productService.getLatestProducts(5);

        assertThat(result).isEmpty();
    }

    // ─── getProductsByBrand ────────────────────────────────────────────────────

    @Test
    void getProductsByBrand_whenBrandNotFound_thenThrowNotFoundException() {
        when(brandRepository.findBySlug("unknown-brand")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> productService.getProductsByBrand("unknown-brand"));
    }

    @Test
    void getProductsByBrand_whenBrandFound_thenReturnThumbnailList() {
        Brand brand = new Brand();
        brand.setId(1L);
        brand.setSlug("test-brand");

        product.setThumbnailMediaId(10L);

        when(brandRepository.findBySlug("test-brand")).thenReturn(Optional.of(brand));
        when(productRepository.findAllByBrandAndIsPublishedTrueOrderByIdAsc(brand)).thenReturn(List.of(product));
        when(mediaService.getMedia(10L)).thenReturn(new NoFileMediaVm(10L, "", "", "", "http://thumb.example.com"));

        List<ProductThumbnailVm> result = productService.getProductsByBrand("test-brand");

        assertThat(result).hasSize(1);
        assertEquals("Test Product", result.getFirst().name());
    }

    // ─── getProductsWithFilter ─────────────────────────────────────────────────

    @Test
    void getProductsWithFilter_thenReturnPaginatedVm() {
        Page<Product> page = new PageImpl<>(List.of(product), PageRequest.of(0, 10), 1);
        when(productRepository.getProductsWithFilter(any(), any(), any(Pageable.class))).thenReturn(page);

        ProductListGetVm result = productService.getProductsWithFilter(0, 10, "test", "");

        assertEquals(0, result.pageNo());
        assertEquals(1, result.totalElements());
        assertThat(result.productContent()).hasSize(1);
    }

    // ─── deleteProduct ─────────────────────────────────────────────────────────

    @Test
    void deleteProduct_whenProductNotFound_thenThrowNotFoundException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> productService.deleteProduct(99L));
    }

    @Test
    void deleteProduct_whenProductHasNoParent_thenSetPublishedFalse() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        productService.deleteProduct(1L);

        verify(productRepository).save(product);
        assertEquals(false, product.isPublished());
    }

    @Test
    void deleteProduct_whenProductIsVariation_thenDeleteCombinations() {
        Product parent = new Product();
        parent.setId(10L);
        product.setParent(parent);

        ProductOptionCombination combo = new ProductOptionCombination();
        combo.setProduct(product);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productOptionCombinationRepository.findAllByProduct(product)).thenReturn(List.of(combo));

        productService.deleteProduct(1L);

        verify(productOptionCombinationRepository).deleteAll(List.of(combo));
        verify(productRepository).save(product);
    }

    // ─── getProductsByMultiQuery ───────────────────────────────────────────────

    @Test
    void getProductsByMultiQuery_thenReturnPaginatedVm() {
        product.setThumbnailMediaId(50L);
        Page<Product> page = new PageImpl<>(List.of(product), PageRequest.of(0, 10), 1);
        when(productRepository.findByProductNameAndCategorySlugAndPriceBetween(
                any(), any(), any(), any(), any(Pageable.class))).thenReturn(page);
        when(mediaService.getMedia(50L)).thenReturn(new NoFileMediaVm(50L, "", "", "", "http://t.example.com"));

        ProductsGetVm result = productService.getProductsByMultiQuery(0, 10, "test", "", 0.0, 200.0);

        assertEquals(0, result.pageNo());
        assertEquals(1, result.totalElements());
    }

    // ─── getProductVariationsByParentId ───────────────────────────────────────

    @Test
    void getProductVariationsByParentId_whenNoOptions_thenReturnEmptyList() {
        product.setHasOptions(false);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        var result = productService.getProductVariationsByParentId(1L);

        assertThat(result).isEmpty();
    }

    @Test
    void getProductVariationsByParentId_whenHasOptions_thenReturnVariations() {
        product.setHasOptions(true);

        Product variation = new Product();
        variation.setId(2L);
        variation.setName("Red M");
        variation.setSlug("red-m");
        variation.setPublished(true);
        variation.setProductImages(new ArrayList<>());

        product.setProducts(List.of(variation));

        ProductOption option = new ProductOption();
        option.setId(3L);
        ProductOptionCombination combination = new ProductOptionCombination();
        combination.setProductOption(option);
        combination.setValue("Red");

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productOptionCombinationRepository.findAllByProduct(variation)).thenReturn(List.of(combination));

        var result = productService.getProductVariationsByParentId(1L);

        assertThat(result).hasSize(1);
        assertEquals(2L, result.getFirst().id());
    }

    // ─── getProductSlug ────────────────────────────────────────────────────────

    @Test
    void getProductSlug_whenProductHasNoParent_thenReturnProductSlug() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductSlugGetVm result = productService.getProductSlug(1L);

        assertEquals("test-product", result.slug());
        assertNull(result.productVariantId());
    }

    @Test
    void getProductSlug_whenProductHasParent_thenReturnParentSlug() {
        Product parent = new Product();
        parent.setId(10L);
        parent.setSlug("parent-product");
        product.setParent(parent);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductSlugGetVm result = productService.getProductSlug(1L);

        assertEquals("parent-product", result.slug());
        assertEquals(1L, result.productVariantId());
    }

    // ─── getProductEsDetailById ────────────────────────────────────────────────

    @Test
    void getProductEsDetailById_whenProductExists_thenReturnEsDetailVm() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductEsDetailVm result = productService.getProductEsDetailById(1L);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals("test-product", result.slug());
        assertNull(result.brand());
        assertNull(result.thumbnailMediaId());
    }

    @Test
    void getProductEsDetailById_withBrandAndThumbnail_thenReturnDetails() {
        Brand brand = new Brand();
        brand.setId(5L);
        brand.setName("My Brand");
        product.setBrand(brand);
        product.setThumbnailMediaId(200L);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductEsDetailVm result = productService.getProductEsDetailById(1L);

        assertEquals("My Brand", result.brand());
        assertEquals(200L, result.thumbnailMediaId());
    }

    // ─── getRelatedProductsBackoffice ──────────────────────────────────────────

    @Test
    void getRelatedProductsBackoffice_whenProductNotFound_thenThrowNotFoundException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> productService.getRelatedProductsBackoffice(99L));
    }

    @Test
    void getRelatedProductsBackoffice_whenHasRelatedProducts_thenReturnList() {
        Product relatedProduct = new Product();
        relatedProduct.setId(20L);
        relatedProduct.setName("Related");
        relatedProduct.setSlug("related");
        relatedProduct.setPrice(50.0);
        relatedProduct.setProducts(new ArrayList<>());

        ProductRelated productRelated = new ProductRelated();
        productRelated.setProduct(product);
        productRelated.setRelatedProduct(relatedProduct);
        product.setRelatedProducts(List.of(productRelated));

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        List<ProductListVm> result = productService.getRelatedProductsBackoffice(1L);

        assertThat(result).hasSize(1);
        assertEquals(20L, result.getFirst().id());
    }

    // ─── getProductsForWarehouse ───────────────────────────────────────────────

    @Test
    void getProductsForWarehouse_whenCalled_thenReturnFilteredList() {
        product.setName("Warehouse Product");
        product.setStockTrackingEnabled(true);
        product.setStockQuantity(100L);

        when(productRepository.findProductForWarehouse(any(), any(), any(), any()))
                .thenReturn(List.of(product));

        var result = productService.getProductsForWarehouse(
                "Warehouse", "SKU-001", List.of(1L),
                com.yas.product.model.enumeration.FilterExistInWhSelection.YES);

        assertThat(result).hasSize(1);
    }

    // ─── getProductByIds ───────────────────────────────────────────────────────

    @Test
    void getProductByIds_whenCalled_thenReturnMatchingProducts() {
        when(productRepository.findAllByIdIn(List.of(1L))).thenReturn(List.of(product));

        List<ProductListVm> result = productService.getProductByIds(List.of(1L));

        assertThat(result).hasSize(1);
        assertEquals("Test Product", result.getFirst().name());
    }

    // ─── getProductByCategoryIds ───────────────────────────────────────────────

    @Test
    void getProductByCategoryIds_whenCalled_thenReturnProducts() {
        when(productRepository.findByCategoryIdsIn(List.of(1L))).thenReturn(List.of(product));

        List<ProductListVm> result = productService.getProductByCategoryIds(List.of(1L));

        assertThat(result).hasSize(1);
    }

    // ─── getProductByBrandIds ──────────────────────────────────────────────────

    @Test
    void getProductByBrandIds_whenCalled_thenReturnProducts() {
        when(productRepository.findByBrandIdsIn(List.of(1L))).thenReturn(List.of(product));

        List<ProductListVm> result = productService.getProductByBrandIds(List.of(1L));

        assertThat(result).hasSize(1);
    }

    // ─── subtractStockQuantity ─────────────────────────────────────────────────

    @Test
    void subtractStockQuantity_whenCalled_thenReduceStockQuantity() {
        product.setStockTrackingEnabled(true);
        product.setStockQuantity(100L);
        when(productRepository.findAllByIdIn(List.of(1L))).thenReturn(List.of(product));
        when(productRepository.saveAll(any())).thenReturn(List.of(product));

        ProductQuantityPutVm putVm = new ProductQuantityPutVm(1L, 10L);
        productService.subtractStockQuantity(List.of(putVm));

        assertEquals(90L, product.getStockQuantity());
    }

    @Test
    void subtractStockQuantity_whenResultNegative_thenSetToZero() {
        product.setStockTrackingEnabled(true);
        product.setStockQuantity(5L);
        when(productRepository.findAllByIdIn(List.of(1L))).thenReturn(List.of(product));
        when(productRepository.saveAll(any())).thenReturn(List.of(product));

        ProductQuantityPutVm putVm = new ProductQuantityPutVm(1L, 100L);
        productService.subtractStockQuantity(List.of(putVm));

        assertEquals(0L, product.getStockQuantity());
    }

    // ─── restoreStockQuantity ──────────────────────────────────────────────────

    @Test
    void restoreStockQuantity_whenCalled_thenIncreaseStockQuantity() {
        product.setStockTrackingEnabled(true);
        product.setStockQuantity(50L);
        when(productRepository.findAllByIdIn(List.of(1L))).thenReturn(List.of(product));
        when(productRepository.saveAll(any())).thenReturn(List.of(product));

        ProductQuantityPutVm putVm = new ProductQuantityPutVm(1L, 25L);
        productService.restoreStockQuantity(List.of(putVm));

        assertEquals(75L, product.getStockQuantity());
    }

    @Test
    void restoreStockQuantity_whenStockTrackingDisabled_thenDoNotUpdateQuantity() {
        product.setStockTrackingEnabled(false);
        product.setStockQuantity(50L);
        when(productRepository.findAllByIdIn(List.of(1L))).thenReturn(List.of(product));
        when(productRepository.saveAll(any())).thenReturn(List.of(product));

        ProductQuantityPutVm putVm = new ProductQuantityPutVm(1L, 25L);
        productService.restoreStockQuantity(List.of(putVm));

        // stock quantity should remain unchanged since tracking is disabled
        assertEquals(50L, product.getStockQuantity());
    }

    // ─── getListFeaturedProducts ───────────────────────────────────────────────

    @Test
    void getListFeaturedProducts_thenReturnFeatureVm() {
        product.setThumbnailMediaId(30L);
        Page<Product> page = new PageImpl<>(List.of(product), PageRequest.of(0, 10), 1);
        when(productRepository.getFeaturedProduct(any(Pageable.class))).thenReturn(page);
        when(mediaService.getMedia(30L)).thenReturn(new NoFileMediaVm(30L, "", "", "", "http://feat.example.com"));

        ProductFeatureGetVm result = productService.getListFeaturedProducts(0, 10);

        assertNotNull(result);
        assertThat(result.productList()).hasSize(1);
    }
}
