package com.yas.order.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
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

    @Test
    void equals_sameInstance_returnsTrue() {
        CheckoutItem item = CheckoutItem.builder().id(3L).build();

        assertThat(item).isEqualTo(item);
    }

    @Test
    void equals_differentType_returnsFalse() {
        CheckoutItem item = CheckoutItem.builder().id(3L).build();

        assertThat(item).isNotEqualTo("other");
    }

    @Test
    void equals_differentId_returnsFalse() {
        CheckoutItem first = CheckoutItem.builder().id(1L).build();
        CheckoutItem second = CheckoutItem.builder().id(2L).build();

        assertThat(first).isNotEqualTo(second);
    }
}
