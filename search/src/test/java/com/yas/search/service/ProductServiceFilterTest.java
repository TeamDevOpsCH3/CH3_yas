package com.yas.search.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import com.yas.search.constant.ProductField;
import java.lang.reflect.Method;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

class ProductServiceFilterTest {

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        productService = new ProductService(elasticsearchOperations);
    }

    @Test
    void extractedTermsFilter_whenValuesProvided_addsMustClause() throws Exception {
        BoolQuery.Builder builder = new BoolQuery.Builder();

        Method method = ProductService.class.getDeclaredMethod(
            "extractedTermsFilter",
            String.class,
            String.class,
            BoolQuery.Builder.class
        );
        method.setAccessible(true);
        method.invoke(productService, "brand-a,brand-b", ProductField.BRAND, builder);

        BoolQuery boolQuery = builder.build();
        assertEquals(1, boolQuery.must().size());

        String queryText = boolQuery.toString();
        assertTrue(queryText.contains(ProductField.BRAND));
        assertTrue(queryText.contains("brand-a"));
        assertTrue(queryText.contains("brand-b"));
    }

    @Test
    void extractedRange_whenMinMaxProvided_addsRangeClause() throws Exception {
        BoolQuery.Builder builder = new BoolQuery.Builder();

        Method method = ProductService.class.getDeclaredMethod(
            "extractedRange",
            Number.class,
            Number.class,
            BoolQuery.Builder.class
        );
        method.setAccessible(true);
        method.invoke(productService, 10.0, 20.0, builder);

        BoolQuery boolQuery = builder.build();
        assertEquals(1, boolQuery.must().size());

        String queryText = boolQuery.toString();
        assertTrue(queryText.contains(ProductField.PRICE));
        assertTrue(queryText.contains("10.0"));
        assertTrue(queryText.contains("20.0"));
    }

    @Test
    void extractedRange_whenMinMaxNull_addsNoClause() throws Exception {
        BoolQuery.Builder builder = new BoolQuery.Builder();

        Method method = ProductService.class.getDeclaredMethod(
            "extractedRange",
            Number.class,
            Number.class,
            BoolQuery.Builder.class
        );
        method.setAccessible(true);
        method.invoke(productService, null, null, builder);

        BoolQuery boolQuery = builder.build();
        assertEquals(0, boolQuery.must().size());
    }
}
