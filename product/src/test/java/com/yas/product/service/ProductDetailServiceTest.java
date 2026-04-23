package com.yas.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.product.model.Brand;
import com.yas.product.model.Category;
import com.yas.product.model.Product;
import com.yas.product.model.ProductCategory;
import com.yas.product.model.ProductImage;
import com.yas.product.model.ProductOptionCombination;
import com.yas.product.model.ProductOption;
import com.yas.product.model.attribute.ProductAttribute;
import com.yas.product.model.attribute.ProductAttributeValue;
import com.yas.product.repository.ProductOptionCombinationRepository;
import com.yas.product.repository.ProductRepository;
import com.yas.product.viewmodel.NoFileMediaVm;
import com.yas.product.viewmodel.product.ProductDetailInfoVm;
import com.yas.product.viewmodel.product.ProductVariationGetVm;
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

@ExtendWith(MockitoExtension.class)
class ProductDetailServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private MediaService mediaService;

    @Mock
    private ProductOptionCombinationRepository productOptionCombinationRepository;

    @InjectMocks
    private ProductDetailService productDetailService;

    private Product product;
    private NoFileMediaVm defaultMedia;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setId(1L);
        product.setName("Test Product");
        product.setShortDescription("Short desc");
        product.setDescription("Full desc");
        product.setSpecification("Spec");
        product.setSku("SKU-001");
        product.setGtin("GTIN-001");
        product.setSlug("test-product");
        product.setPublished(true);
        product.setAllowedToOrder(true);
        product.setFeatured(true);
        product.setVisibleIndividually(true);
        product.setStockTrackingEnabled(true);
        product.setPrice(99.99);
        product.setProductCategories(new ArrayList<>());
        product.setAttributeValues(new ArrayList<>());
        product.setProductImages(new ArrayList<>());
        product.setProducts(new ArrayList<>());

        defaultMedia = new NoFileMediaVm(null, "", "", "", "");
    }

    // ─── getProductDetailById ──────────────────────────────────────────────────

    @Test
    void getProductDetailById_whenProductNotFound_thenThrowNotFoundException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> productDetailService.getProductDetailById(99L));
    }

    @Test
    void getProductDetailById_whenProductNotPublished_thenThrowNotFoundException() {
        product.setPublished(false);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThrows(NotFoundException.class,
                () -> productDetailService.getProductDetailById(1L));
    }

    @Test
    void getProductDetailById_whenMinimalProduct_thenReturnCorrectVm() {
        // product without brand, thumbnail, images, attribute values, or options
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductDetailInfoVm result = productDetailService.getProductDetailById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Product", result.getName());
        assertNull(result.getBrandId());
        assertNull(result.getBrandName());
        assertNull(result.getThumbnail());
        assertThat(result.getProductImages()).isEmpty();
        assertThat(result.getAttributeValues()).isEmpty();
        assertThat(result.getVariations()).isEmpty();
        assertThat(result.getCategories()).isEmpty();
    }

    @Test
    void getProductDetailById_withBrand_thenReturnBrandInfo() {
        Brand brand = new Brand();
        brand.setId(10L);
        brand.setName("Test Brand");
        product.setBrand(brand);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductDetailInfoVm result = productDetailService.getProductDetailById(1L);

        assertEquals(10L, result.getBrandId());
        assertEquals("Test Brand", result.getBrandName());
    }

    @Test
    void getProductDetailById_withThumbnail_thenReturnThumbnailVm() {
        product.setThumbnailMediaId(100L);
        NoFileMediaVm mediaVm = new NoFileMediaVm(100L, "caption", "file.jpg", "image/jpeg",
                "http://example.com/img.jpg");
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(mediaService.getMedia(100L)).thenReturn(mediaVm);

        ProductDetailInfoVm result = productDetailService.getProductDetailById(1L);

        assertNotNull(result.getThumbnail());
        assertEquals(100L, result.getThumbnail().id());
        assertEquals("http://example.com/img.jpg", result.getThumbnail().url());
    }

    @Test
    void getProductDetailById_withProductImages_thenReturnImageList() {
        ProductImage img = new ProductImage();
        img.setImageId(200L);
        img.setProduct(product);
        product.setProductImages(List.of(img));

        NoFileMediaVm mediaVm = new NoFileMediaVm(200L, "", "", "", "http://example.com/img2.jpg");
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(mediaService.getMedia(200L)).thenReturn(mediaVm);

        ProductDetailInfoVm result = productDetailService.getProductDetailById(1L);

        assertThat(result.getProductImages()).hasSize(1);
        assertEquals(200L, result.getProductImages().getFirst().id());
    }

    @Test
    void getProductDetailById_withCategories_thenReturnCategoryList() {
        Category category = new Category();
        category.setId(5L);
        category.setName("Electronics");

        ProductCategory productCategory = new ProductCategory();
        productCategory.setProduct(product);
        productCategory.setCategory(category);

        product.setProductCategories(List.of(productCategory));

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductDetailInfoVm result = productDetailService.getProductDetailById(1L);

        assertThat(result.getCategories()).hasSize(1);
        assertEquals("Electronics", result.getCategories().getFirst().getName());
    }

    @Test
    void getProductDetailById_withAttributeValues_thenReturnAttributeList() {
        ProductAttribute attribute = new ProductAttribute();
        attribute.setId(7L);
        attribute.setName("Color");

        ProductAttributeValue attrValue = new ProductAttributeValue();
        attrValue.setId(20L);
        attrValue.setProductAttribute(attribute);
        attrValue.setValue("Red");
        attrValue.setProduct(product);

        product.setAttributeValues(List.of(attrValue));

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductDetailInfoVm result = productDetailService.getProductDetailById(1L);

        assertThat(result.getAttributeValues()).hasSize(1);
        assertEquals("Color", result.getAttributeValues().getFirst().nameProductAttribute());
        assertEquals("Red", result.getAttributeValues().getFirst().value());
    }

    @Test
    void getProductDetailById_withOptions_thenReturnVariations() {
        product.setHasOptions(true);

        // Create variation product (child)
        Product variation = new Product();
        variation.setId(2L);
        variation.setName("Red M");
        variation.setSlug("test-product-red-m");
        variation.setSku("SKU-002");
        variation.setGtin("GTIN-002");
        variation.setPrice(89.99);
        variation.setPublished(true);
        variation.setProductImages(new ArrayList<>());
        variation.setProducts(new ArrayList<>());

        product.setProducts(List.of(variation));

        // Option combination for the variation
        ProductOption productOption = new ProductOption();
        productOption.setId(3L);
        productOption.setName("Color");

        ProductOptionCombination combination = new ProductOptionCombination();
        combination.setProduct(variation);
        combination.setProductOption(productOption);
        combination.setValue("Red");

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productOptionCombinationRepository.findAllByProduct(variation))
                .thenReturn(List.of(combination));

        ProductDetailInfoVm result = productDetailService.getProductDetailById(1L);

        assertThat(result.getVariations()).hasSize(1);
        ProductVariationGetVm variationVm = result.getVariations().getFirst();
        assertEquals(2L, variationVm.id());
        assertEquals("Red M", variationVm.name());
        assertThat(variationVm.options()).containsEntry(3L, "Red");
    }

    @Test
    void getProductDetailById_withOptions_unpublishedVariationsExcluded() {
        product.setHasOptions(true);

        Product publishedVariation = new Product();
        publishedVariation.setId(2L);
        publishedVariation.setName("Red M");
        publishedVariation.setSlug("red-m");
        publishedVariation.setPublished(true);
        publishedVariation.setProductImages(new ArrayList<>());
        publishedVariation.setProducts(new ArrayList<>());

        Product unpublishedVariation = new Product();
        unpublishedVariation.setId(3L);
        unpublishedVariation.setName("Blue L");
        unpublishedVariation.setPublished(false);

        product.setProducts(List.of(publishedVariation, unpublishedVariation));

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productOptionCombinationRepository.findAllByProduct(publishedVariation))
                .thenReturn(Collections.emptyList());

        ProductDetailInfoVm result = productDetailService.getProductDetailById(1L);

        assertThat(result.getVariations()).hasSize(1);
        assertEquals(2L, result.getVariations().getFirst().id());
    }

    @Test
    void getProductDetailById_withVariationThumbnail_thenReturnThumbnailInVariation() {
        product.setHasOptions(true);

        Product variation = new Product();
        variation.setId(4L);
        variation.setName("Green S");
        variation.setSlug("green-s");
        variation.setPublished(true);
        variation.setThumbnailMediaId(300L);
        variation.setProductImages(new ArrayList<>());
        variation.setProducts(new ArrayList<>());

        product.setProducts(List.of(variation));

        NoFileMediaVm thumbMedia = new NoFileMediaVm(300L, "", "", "", "http://example.com/green.jpg");
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productOptionCombinationRepository.findAllByProduct(variation))
                .thenReturn(Collections.emptyList());
        when(mediaService.getMedia(300L)).thenReturn(thumbMedia);

        ProductDetailInfoVm result = productDetailService.getProductDetailById(1L);

        assertThat(result.getVariations()).hasSize(1);
        assertNotNull(result.getVariations().getFirst().thumbnail());
        assertEquals("http://example.com/green.jpg", result.getVariations().getFirst().thumbnail().url());
    }

    @Test
    void getProductDetailById_withNullProductCategories_thenReturnEmptyCategories() {
        product.setProductCategories(null);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductDetailInfoVm result = productDetailService.getProductDetailById(1L);

        assertThat(result.getCategories()).isEmpty();
    }

    @Test
    void getProductDetailById_withNullProductImages_thenReturnEmptyImages() {
        product.setProductImages(null);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductDetailInfoVm result = productDetailService.getProductDetailById(1L);

        assertThat(result.getProductImages()).isEmpty();
    }

    @Test
    void getProductDetailById_withoutThumbnail_thenReturnNullThumbnail() {
        product.setThumbnailMediaId(null);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductDetailInfoVm result = productDetailService.getProductDetailById(1L);

        assertNull(result.getThumbnail());
    }

    @Test
    void getProductDetailById_returnsCorrectPriceAndFlags() {
        product.setPrice(149.99);
        product.setAllowedToOrder(false);
        product.setFeatured(false);
        product.setVisibleIndividually(false);
        product.setStockTrackingEnabled(false);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductDetailInfoVm result = productDetailService.getProductDetailById(1L);

        assertEquals(149.99, result.getPrice());
        assertEquals(false, result.getIsAllowedToOrder());
        assertEquals(false, result.getIsFeatured());
        assertEquals(false, result.getIsVisible());
        assertEquals(false, result.getStockTrackingEnabled());
    }
}
