package com.yas.order.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CheckoutItemTest {

    @Test
    void equals_sameId_returnsTrue() {
        CheckoutItem first = CheckoutItem.builder().id(1L).build();
        CheckoutItem second = CheckoutItem.builder().id(1L).build();

        assertThat(first).isEqualTo(second);
    }

    @Test
    void equals_nullId_returnsFalse() {
        CheckoutItem first = CheckoutItem.builder().id(null).build();
        CheckoutItem second = CheckoutItem.builder().id(null).build();

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void hashCode_usesClassHash() {
        CheckoutItem item = CheckoutItem.builder().id(2L).build();

        assertThat(item.hashCode()).isEqualTo(CheckoutItem.class.hashCode());
    }
}
