package com.yas.order.service;

import static com.yas.order.utils.SecurityContextUtils.setUpSecurityContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.order.config.ServiceUrlConfig;
import com.yas.order.viewmodel.order.OrderItemVm;
import com.yas.order.viewmodel.order.OrderVm;
import com.yas.order.viewmodel.product.ProductCheckoutListVm;
import com.yas.order.viewmodel.product.ProductGetCheckoutListVm;
import com.yas.order.viewmodel.product.ProductVariationVm;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

class ProductServiceTest {

    private static final String PRODUCT_URL = "http://api.yas.local/product";

    private RestClient restClient;

    private ServiceUrlConfig serviceUrlConfig;

    private ProductService productService;

    private RestClient.ResponseSpec responseSpec;

    @BeforeEach
    void setUp() {
        restClient = mock(RestClient.class);
        serviceUrlConfig = mock(ServiceUrlConfig.class);
        productService = new ProductService(restClient, serviceUrlConfig);
        responseSpec = Mockito.mock(RestClient.ResponseSpec.class);
        setUpSecurityContext("test");
        when(serviceUrlConfig.product()).thenReturn(PRODUCT_URL);
    }

    @Test
    void getProductVariations_returnsList() {
        Long productId = 1L;
        URI url = UriComponentsBuilder
            .fromUriString(PRODUCT_URL)
            .path("/backoffice/product-variations/{productId}")
            .buildAndExpand(productId)
            .toUri();

        RestClient.RequestHeadersUriSpec requestHeadersUriSpec = Mockito.mock(RestClient.RequestHeadersUriSpec.class);
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(url)).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.headers(any())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);

        List<ProductVariationVm> variations = List.of(new ProductVariationVm(2L, "v1", "sku"));
        when(responseSpec.toEntity(new ParameterizedTypeReference<List<ProductVariationVm>>() {}))
            .thenReturn(ResponseEntity.ok(variations));

        List<ProductVariationVm> result = productService.getProductVariations(productId);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id()).isEqualTo(2L);
    }

    @Test
    void getProductInfomation_whenResponseNull_throwsNotFound() {
        RestClient.RequestHeadersUriSpec requestHeadersUriSpec = Mockito.mock(RestClient.RequestHeadersUriSpec.class);
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(URI.class))).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.headers(any())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(new ParameterizedTypeReference<ProductGetCheckoutListVm>() {}))
            .thenReturn(null);

        assertThrows(NotFoundException.class,
            () -> productService.getProductInfomation(Set.of(1L), 0, 1));
    }

    @Test
    void getProductInfomation_whenListNull_throwsNotFound() {
        RestClient.RequestHeadersUriSpec requestHeadersUriSpec = Mockito.mock(RestClient.RequestHeadersUriSpec.class);
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(URI.class))).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.headers(any())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);

        ProductGetCheckoutListVm response = new ProductGetCheckoutListVm(null, 0, 1, 0, 0, true);
        when(responseSpec.toEntity(new ParameterizedTypeReference<ProductGetCheckoutListVm>() {}))
            .thenReturn(ResponseEntity.ok(response));

        assertThrows(NotFoundException.class,
            () -> productService.getProductInfomation(Set.of(1L), 0, 1));
    }

    @Test
    void getProductInfomation_whenListPresent_returnsMap() {
        RestClient.RequestHeadersUriSpec requestHeadersUriSpec = Mockito.mock(RestClient.RequestHeadersUriSpec.class);
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(URI.class))).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.headers(any())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);

        ProductCheckoutListVm product = ProductCheckoutListVm.builder()
            .id(10L)
            .name("Product")
            .price(10.0)
            .taxClassId(1L)
            .build();

        ProductGetCheckoutListVm response = new ProductGetCheckoutListVm(List.of(product), 0, 1, 1, 1, true);
        when(responseSpec.toEntity(new ParameterizedTypeReference<ProductGetCheckoutListVm>() {}))
            .thenReturn(ResponseEntity.ok(response));

        Map<Long, ProductCheckoutListVm> result = productService.getProductInfomation(Set.of(10L), 0, 1);

        assertThat(result).containsKey(10L);
        assertThat(result.get(10L).getName()).isEqualTo("Product");
    }

    @Test
    void subtractProductStockQuantity_sendsProductQuantities() {
        RestClient.RequestBodyUriSpec requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        when(restClient.put()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(any(URI.class))).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.headers(any())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.body(any())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.retrieve()).thenReturn(responseSpec);

        OrderItemVm item = OrderItemVm.builder()
            .productId(101L)
            .quantity(2)
            .build();

        OrderVm orderVm = OrderVm.builder()
            .orderItemVms(Set.of(item))
            .build();

        productService.subtractProductStockQuantity(orderVm);

        ArgumentCaptor<Object> bodyCaptor = ArgumentCaptor.forClass(Object.class);
        verify(requestBodyUriSpec).body(bodyCaptor.capture());
        assertThat(bodyCaptor.getValue()).isInstanceOf(List.class);
        assertThat((List<?>) bodyCaptor.getValue()).hasSize(1);
    }
}
