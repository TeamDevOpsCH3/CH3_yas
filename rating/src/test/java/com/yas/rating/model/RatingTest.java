package com.yas.rating.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RatingTest {

    @Test
    void equals_WhenSameInstance_ReturnsTrue() {
        Rating rating = Rating.builder().id(1L).build();
        assertTrue(rating.equals(rating));
    }

    @Test
    void equals_WhenOtherType_ReturnsFalse() {
        Rating rating = Rating.builder().id(1L).build();
        assertFalse(rating.equals("not-a-rating"));
    }

    @Test
    void equals_WhenBothIdsNull_ReturnsFalse() {
        Rating first = Rating.builder().build();
        Rating second = Rating.builder().build();
        assertFalse(first.equals(second));
    }

    @Test
    void equals_WhenIdsMatch_ReturnsTrue() {
        Rating first = Rating.builder().id(1L).build();
        Rating second = Rating.builder().id(1L).build();
        assertTrue(first.equals(second));
    }

    @Test
    void equals_WhenIdsDiffer_ReturnsFalse() {
        Rating first = Rating.builder().id(1L).build();
        Rating second = Rating.builder().id(2L).build();
        assertFalse(first.equals(second));
    }

    @Test
    void hashCode_UsesClassHashCode() {
        Rating rating = Rating.builder().id(1L).build();
        assertEquals(Rating.class.hashCode(), rating.hashCode());
    }
}
