package com.yas.order.viewmodel;

import static org.assertj.core.api.Assertions.assertThat;

import com.yas.order.viewmodel.cart.CartItemDeleteVm;
import com.yas.order.viewmodel.customer.CustomerVm;
import com.yas.order.viewmodel.product.ProductCheckoutListVm;
import com.yas.order.viewmodel.product.ProductGetCheckoutListVm;
import com.yas.order.viewmodel.product.ProductQuantityItem;
import com.yas.order.viewmodel.product.ProductVariationVm;
import com.yas.order.viewmodel.promotion.PromotionUsageVm;
import java.util.List;
import org.junit.jupiter.api.Test;

class MiscViewModelTest {

    @Test
    void errorVm_defaultConstructorCreatesEmptyList() {
        ErrorVm vm = new ErrorVm("400", "Bad Request", "Invalid");

        assertThat(vm.fieldErrors()).isNotNull();
        assertThat(vm.fieldErrors()).isEmpty();
    }

    @Test
    void responseStatusVm_exposesFields() {
        ResponeStatusVm vm = new ResponeStatusVm("OK", "Done", "200");

        assertThat(vm.title()).isEqualTo("OK");
        assertThat(vm.statusCode()).isEqualTo("200");
    }

    @Test
    void simpleViewModels_exposeFields() {
        CartItemDeleteVm cartVm = new CartItemDeleteVm(1L, 2);
        CustomerVm customerVm = new CustomerVm("user", "user@example.com", "First", "Last");
        ProductVariationVm variationVm = new ProductVariationVm(1L, "Var", "SKU");

        assertThat(cartVm.quantity()).isEqualTo(2);
        assertThat(customerVm.username()).isEqualTo("user");
        assertThat(variationVm.sku()).isEqualTo("SKU");
    }

    @Test
    void productAndPromotionBuilders_work() {
        ProductCheckoutListVm product = ProductCheckoutListVm.builder()
            .id(10L)
            .name("Product")
            .price(10.0)
            .taxClassId(1L)
            .build();

        ProductGetCheckoutListVm listVm = new ProductGetCheckoutListVm(List.of(product), 0, 1, 1, 1, true);

        ProductQuantityItem quantityItem = ProductQuantityItem.builder()
            .productId(10L)
            .quantity(2L)
            .build();

        PromotionUsageVm promotion = PromotionUsageVm.builder()
            .promotionCode("PROMO")
            .userId("user-1")
            .orderId(100L)
            .productId(10L)
            .build();

        assertThat(listVm.productCheckoutListVms()).hasSize(1);
        assertThat(quantityItem.quantity()).isEqualTo(2L);
        assertThat(promotion.promotionCode()).isEqualTo("PROMO");
        assertThat(product.getName()).isEqualTo("Product");
    }
}
