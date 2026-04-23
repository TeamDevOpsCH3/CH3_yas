package com.yas.product.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import com.yas.product.model.enumeration.FilterExistInWhSelection;
import com.yas.product.service.ProductDetailService;
import com.yas.product.service.ProductService;
import com.yas.product.viewmodel.product.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {
    
    @Mock
    private ProductService productService;
    
    @Mock
    private ProductDetailService productDetailService;

    @InjectMocks
    private ProductController productController;

    @Test
    void listProducts_ValidRequest_Success() {
        ResponseEntity<ProductListGetVm> response = productController.listProducts(0, 5, "name", "brand");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(productService).getProductsWithFilter(0, 5, "name", "brand");
    }

    @Test
    void exportProducts_ValidRequest_Success() {
        ResponseEntity<List<ProductExportingDetailVm>> response = productController.exportProducts("name", "brand");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(productService).exportProducts("name", "brand");
    }

    @Test
    void createProduct_ValidRequest_Success() {
        ProductPostVm vm = org.mockito.Mockito.mock(ProductPostVm.class);
        ResponseEntity<ProductGetDetailVm> response = productController.createProduct(vm);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(productService).createProduct(vm);
    }

    @Test
    void updateProduct_ValidRequest_Success() {
        ProductPutVm vm = org.mockito.Mockito.mock(ProductPutVm.class);
        ResponseEntity<Void> response = productController.updateProduct(1L, vm);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(productService).updateProduct(1L, vm);
    }

    @Test
    void getFeaturedProducts_ValidRequest_Success() {
        ResponseEntity<ProductFeatureGetVm> response = productController.getFeaturedProducts(0, 10);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(productService).getListFeaturedProducts(0, 10);
    }

    @Test
    void getProductsByBrand_ValidRequest_Success() {
        ResponseEntity<List<ProductThumbnailVm>> response = productController.getProductsByBrand("brand-slug");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(productService).getProductsByBrand("brand-slug");
    }

    @Test
    void getProductsByCategory_ValidRequest_Success() {
        ResponseEntity<ProductListGetFromCategoryVm> response = productController.getProductsByCategory(0, 2, "category-slug");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(productService).getProductsFromCategory(0, 2, "category-slug");
    }

    @Test
    void getProductById_ValidRequest_Success() {
        ResponseEntity<ProductDetailVm> response = productController.getProductById(1L);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(productService).getProductById(1L);
    }

    @Test
    void getFeaturedProductsById_ValidRequest_Success() {
        List<Long> ids = List.of(1L, 2L);
        ResponseEntity<List<ProductThumbnailGetVm>> response = productController.getFeaturedProductsById(ids);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(productService).getFeaturedProductsById(ids);
    }

    @Test
    void getProductDetail_ValidRequest_Success() {
        ResponseEntity<ProductDetailGetVm> response = productController.getProductDetail("slug");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(productService).getProductDetail("slug");
    }

    @Test
    void deleteProduct_ValidRequest_Success() {
        ResponseEntity<Void> response = productController.deleteProduct(1L);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(productService).deleteProduct(1L);
    }

    @Test
    void getProductsByMultiQuery_ValidRequest_Success() {
        ResponseEntity<ProductsGetVm> response = productController.getProductsByMultiQuery(0, 5, "name", "category", 10.0, 100.0);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(productService).getProductsByMultiQuery(0, 5, "name", "category", 10.0, 100.0);
    }

    @Test
    void getProductVariationsByParentId_ValidRequest_Success() {
        ResponseEntity<List<ProductVariationGetVm>> response = productController.getProductVariationsByParentId(1L);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(productService).getProductVariationsByParentId(1L);
    }

    @Test
    void getProductSlug_ValidRequest_Success() {
        ResponseEntity<ProductSlugGetVm> response = productController.getProductSlug(1L);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(productService).getProductSlug(1L);
    }

    @Test
    void getProductEsDetailById_ValidRequest_Success() {
        ResponseEntity<ProductEsDetailVm> response = productController.getProductEsDetailById(1L);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(productService).getProductEsDetailById(1L);
    }

    @Test
    void getRelatedProductsBackoffice_ValidRequest_Success() {
        ResponseEntity<List<ProductListVm>> response = productController.getRelatedProductsBackoffice(1L);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(productService).getRelatedProductsBackoffice(1L);
    }

    @Test
    void getRelatedProductsStorefront_ValidRequest_Success() {
        ResponseEntity<ProductsGetVm> response = productController.getRelatedProductsStorefront(1L, 0, 5);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(productService).getRelatedProductsStorefront(1L, 0, 5);
    }

    @Test
    void getProductsForWarehouse_ValidRequest_Success() {
        List<Long> ids = List.of(1L);
        ResponseEntity<List<ProductInfoVm>> response = productController.getProductsForWarehouse("name", "sku", ids, FilterExistInWhSelection.ALL);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(productService).getProductsForWarehouse("name", "sku", ids, FilterExistInWhSelection.ALL);
    }

    @Test
    void updateProductQuantity_ValidRequest_Success() {
        List<ProductQuantityPostVm> items = List.of(org.mockito.Mockito.mock(ProductQuantityPostVm.class));
        ResponseEntity<Void> response = productController.updateProductQuantity(items);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(productService).updateProductQuantity(items);
    }

    @Test
    void subtractProductQuantity_ValidRequest_Success() {
        List<ProductQuantityPutVm> items = List.of(org.mockito.Mockito.mock(ProductQuantityPutVm.class));
        ResponseEntity<Void> response = productController.subtractProductQuantity(items);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(productService).subtractStockQuantity(items);
    }

    @Test
    void getProductByIds_ValidRequest_Success() {
        List<Long> ids = List.of(1L, 2L);
        ResponseEntity<List<ProductListVm>> response = productController.getProductByIds(ids);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(productService).getProductByIds(ids);
    }

    @Test
    void getProductByCategories_ValidRequest_Success() {
        List<Long> ids = List.of(1L, 2L);
        ResponseEntity<List<ProductListVm>> response = productController.getProductByCategories(ids);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(productService).getProductByCategoryIds(ids);
    }

    @Test
    void getProductByBrands_ValidRequest_Success() {
        List<Long> ids = List.of(1L, 2L);
        ResponseEntity<List<ProductListVm>> response = productController.getProductByBrands(ids);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(productService).getProductByBrandIds(ids);
    }

    @Test
    void getLatestProducts_ValidRequest_Success() {
        ResponseEntity<List<ProductListVm>> response = productController.getLatestProducts(10);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(productService).getLatestProducts(10);
    }

    @Test
    void getProductDetailById_ValidRequest_Success() {
        ResponseEntity<ProductDetailInfoVm> response = productController.getProductDetailById(1L);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(productDetailService).getProductDetailById(1L);
    }

    @Test
    void getProductCheckoutList_ValidRequest_Success() {
        List<Long> ids = List.of(1L, 2L);
        ResponseEntity<ProductGetCheckoutListVm> response = productController.getProductCheckoutList(0, 20, ids);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(productService).getProductCheckoutList(0, 20, ids);
    }
}
