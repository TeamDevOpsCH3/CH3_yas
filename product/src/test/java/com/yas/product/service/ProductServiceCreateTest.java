package com.yas.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.BadRequestException;
import com.yas.commonlibrary.exception.DuplicatedException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.product.model.Brand;
import com.yas.product.model.Category;
import com.yas.product.model.Product;
import com.yas.product.model.ProductCategory;
import com.yas.product.model.ProductImage;
import com.yas.product.model.ProductOption;
import com.yas.product.model.ProductOptionCombination;
import com.yas.product.model.ProductRelated;
import com.yas.product.model.attribute.ProductAttributeGroup;
import com.yas.product.model.attribute.ProductAttributeValue;
import com.yas.product.model.enumeration.DimensionUnit;
import com.yas.product.repository.BrandRepository;
import com.yas.product.repository.CategoryRepository;
import com.yas.product.repository.ProductCategoryRepository;
import com.yas.product.repository.ProductImageRepository;
import com.yas.product.repository.ProductOptionCombinationRepository;
import com.yas.product.repository.ProductOptionRepository;
import com.yas.product.repository.ProductOptionValueRepository;
import com.yas.product.repository.ProductRelatedRepository;
import com.yas.product.repository.ProductRepository;
import com.yas.product.viewmodel.product.ProductGetDetailVm;
import com.yas.product.viewmodel.product.ProductPostVm;
import com.yas.product.viewmodel.product.ProductPutVm;
import com.yas.product.viewmodel.product.ProductVariationPostVm;
import com.yas.product.viewmodel.product.ProductVariationPutVm;
import com.yas.product.viewmodel.productoption.ProductOptionValuePutVm;
import com.yas.product.viewmodel.productoption.ProductOptionValuePostVm;
import com.yas.product.viewmodel.product.ProductOptionValueDisplay;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductServiceCreateTest {

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
    void createProduct_WhenLengthLessThanWidth_ShouldThrowBadRequestException() {
        ProductPostVm vm = new ProductPostVm(
                "Test Product", "test-slug", 1L, List.of(), "Short", "Desc", "Spec", "SKU", "GTIN",
                10.0, DimensionUnit.CM, 1.0, 5.0, 2.0, 100.0, true, true, true, true, true,
                "MetaTitle", "MetaKeyword", "MetaDesc", 1L, List.of(), List.of(), List.of(), List.of(), List.of(), 1L);

        assertThrows(BadRequestException.class, () -> productService.createProduct(vm));
    }

    @Test
    void createProduct_WhenValid_ShouldSaveTargetProduct() {
        ProductOptionValuePostVm postOption = new ProductOptionValuePostVm(1L, "type", 1, List.of("v1"));
        ProductOptionValueDisplay displayVm = new ProductOptionValueDisplay(1L, "type", 1, "v1");
        
        ProductPostVm vm = new ProductPostVm(
                "Test Product", "test-slug", 1L, List.of(1L), "Short", "Desc", "Spec", "SKU", "GTIN",
                10.0, DimensionUnit.CM, 5.0, 1.0, 2.0, 100.0, true, true, true, true, true,
                "MetaTitle", "MetaKeyword", "MetaDesc", 1L, List.of(), List.of(), List.of(postOption), List.of(displayVm), List.of(), 1L);
        
        Brand brand = new Brand();
        brand.setId(1L);
        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));
        when(productRepository.findBySlugAndIsPublishedTrue("test-slug")).thenReturn(Optional.empty());
        when(productRepository.findByGtinAndIsPublishedTrue("GTIN")).thenReturn(Optional.empty());
        when(productRepository.findBySkuAndIsPublishedTrue("SKU")).thenReturn(Optional.empty());
        
        Product p = new Product();
        p.setId(1L);
        when(productRepository.save(any(Product.class))).thenReturn(p);

        Category category = new Category();
        category.setId(1L);
        when(categoryRepository.findAllById(List.of(1L))).thenReturn(List.of(category));

        ProductOption pOption = new ProductOption();
        pOption.setId(1L);
        when(productOptionRepository.findAllByIdIn(anyList())).thenReturn(List.of(pOption));

        when(productOptionValueRepository.saveAll(anyList())).thenReturn(List.of());

        ProductGetDetailVm result = productService.createProduct(vm);

        assertNotNull(result);
        verify(productRepository).save(any(Product.class));
        verify(productCategoryRepository).saveAll(anyList());
    }

    @Test
    void updateProduct_WhenProductNotFound_ShouldThrowNotFoundException() {
        ProductPutVm vm = new ProductPutVm(
                "Test Product", "test-slug", 10.0, true, true, true, true, true, 1L, List.of(),
                "Short", "Desc", "Spec", "SKU", "GTIN", 10.0, DimensionUnit.CM, 5.0, 1.0, 2.0,
                "MetaTitle", "MetaKeyword", "MetaDesc", 1L, List.of(), List.of(), List.of(), List.of(), List.of(), 1L);

        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> productService.updateProduct(1L, vm));
    }

    @Test
    void updateProduct_WhenValid_ShouldUpdateProduct() {
        ProductOptionValuePutVm putVm = new ProductOptionValuePutVm(1L, "type", 1, List.of("v1"));
        ProductOptionValueDisplay displayVm = new ProductOptionValueDisplay(1L, "type", 1, "v1");

        ProductPutVm vm = new ProductPutVm(
                "Test Product edited", "test-slug", 10.0, true, true, true, true, true, 1L, List.of(),
                "Short", "Desc", "Spec", "SKU", "GTIN", 10.0, DimensionUnit.CM, 5.0, 1.0, 2.0,
                "MetaTitle", "MetaKeyword", "MetaDesc", 1L, List.of(), List.of(), List.of(putVm), List.of(displayVm), List.of(), 1L);

        Product product = new Product();
        product.setId(1L);
        product.setProductCategories(List.of());
        product.setRelatedProducts(List.of());
        product.setProducts(List.of());
        
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.findBySlugAndIsPublishedTrue("test-slug")).thenReturn(Optional.empty());
        when(productRepository.findByGtinAndIsPublishedTrue("GTIN")).thenReturn(Optional.empty());
        when(productRepository.findBySkuAndIsPublishedTrue("SKU")).thenReturn(Optional.empty());

        Brand brand = new Brand();
        brand.setId(1L);
        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));

        ProductOption pOption = new ProductOption();
        pOption.setId(1L);
        when(productOptionRepository.findAllByIdIn(anyList())).thenReturn(List.of(pOption));

        when(productOptionValueRepository.saveAll(anyList())).thenReturn(List.of());

        productService.updateProduct(1L, vm);

        verify(productRepository).findById(1L);
        verify(productCategoryRepository).deleteAllInBatch(anyList());
        verify(productCategoryRepository).saveAll(anyList());
    }
}
