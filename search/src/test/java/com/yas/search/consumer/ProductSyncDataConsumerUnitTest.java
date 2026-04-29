package com.yas.search.consumer;

import static com.yas.commonlibrary.kafka.cdc.message.Operation.CREATE;
import static com.yas.commonlibrary.kafka.cdc.message.Operation.DELETE;
import static com.yas.commonlibrary.kafka.cdc.message.Operation.READ;
import static com.yas.commonlibrary.kafka.cdc.message.Operation.UPDATE;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.yas.commonlibrary.kafka.cdc.message.Product;
import com.yas.commonlibrary.kafka.cdc.message.ProductCdcMessage;
import com.yas.commonlibrary.kafka.cdc.message.ProductMsgKey;
import com.yas.search.kafka.consumer.ProductSyncDataConsumer;
import com.yas.search.service.ProductSyncDataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductSyncDataConsumerUnitTest {

    @InjectMocks
    private ProductSyncDataConsumer productSyncDataConsumer;

    @Mock
    private ProductSyncDataService productSyncDataService;

    @Test
    void sync_whenCreateOperation_createsProduct() {
        long productId = 1L;

        productSyncDataConsumer.sync(
            ProductMsgKey.builder().id(productId).build(),
            ProductCdcMessage.builder()
                .op(CREATE)
                .after(Product.builder().id(productId).build())
                .build()
        );

        verify(productSyncDataService).createProduct(productId);
        verifyNoMoreInteractions(productSyncDataService);
    }

    @Test
    void sync_whenReadOperation_createsProduct() {
        long productId = 4L;

        productSyncDataConsumer.sync(
            ProductMsgKey.builder().id(productId).build(),
            ProductCdcMessage.builder()
                .op(READ)
                .after(Product.builder().id(productId).build())
                .build()
        );

        verify(productSyncDataService).createProduct(productId);
        verifyNoMoreInteractions(productSyncDataService);
    }

    @Test
    void sync_whenUpdateOperation_updatesProduct() {
        long productId = 2L;

        productSyncDataConsumer.sync(
            ProductMsgKey.builder().id(productId).build(),
            ProductCdcMessage.builder()
                .op(UPDATE)
                .after(Product.builder().id(productId).build())
                .build()
        );

        verify(productSyncDataService).updateProduct(productId);
        verifyNoMoreInteractions(productSyncDataService);
    }

    @Test
    void sync_whenDeleteOperation_deletesProduct() {
        long productId = 3L;

        productSyncDataConsumer.sync(
            ProductMsgKey.builder().id(productId).build(),
            ProductCdcMessage.builder()
                .op(DELETE)
                .after(Product.builder().id(productId).build())
                .build()
        );

        verify(productSyncDataService).deleteProduct(productId);
        verifyNoMoreInteractions(productSyncDataService);
    }

    @Test
    void sync_whenMessageNull_deletesProduct() {
        long productId = 5L;

        productSyncDataConsumer.sync(
            ProductMsgKey.builder().id(productId).build(),
            null
        );

        verify(productSyncDataService).deleteProduct(productId);
        verifyNoMoreInteractions(productSyncDataService);
    }
}
