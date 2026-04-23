package com.yas.product.model;

import com.yas.product.model.attribute.ProductAttribute;
import com.yas.product.model.attribute.ProductAttributeGroup;
import com.yas.product.model.attribute.ProductTemplate;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ModelTest {

    private <T> void testEqualsAndHashCode(Class<T> clazz) throws Exception {
        T obj1 = clazz.getDeclaredConstructor().newInstance();
        T obj2 = clazz.getDeclaredConstructor().newInstance();
        T obj3 = clazz.getDeclaredConstructor().newInstance();
        T obj4 = clazz.getDeclaredConstructor().newInstance();

        Method setId = clazz.getMethod("setId", Long.class);
        setId.invoke(obj1, 1L);
        setId.invoke(obj2, 1L);
        setId.invoke(obj3, 2L);

        // Same object
        assertEquals(obj1, obj1);

        // Same ID
        assertEquals(obj1, obj2);

        // Different ID
        assertNotEquals(obj1, obj3);

        // Null ID on one side
        assertNotEquals(obj1, obj4);
        assertNotEquals(obj4, obj1);

        // Null ID on both sides (should be equal if both are null? wait. The equals implementation says: return id != null && id.equals(((Brand) o).id);)
        // If both are null, it returns false! Which is correct for newly transient entities but breaks symmetric equality. We'll test what it does.
        T obj5 = clazz.getDeclaredConstructor().newInstance(); assertNotEquals(obj4, obj5);

        // Different types
        assertNotEquals(obj1, new Object());
        assertNotEquals(obj1, null);

        // HashCode
        assertEquals(clazz.hashCode(), obj1.hashCode());
    }

    @Test
    void testBrand() throws Exception {
        testEqualsAndHashCode(Brand.class);
    }

    @Test
    void testCategory() throws Exception {
        testEqualsAndHashCode(Category.class);
    }

    @Test
    void testProduct() throws Exception {
        testEqualsAndHashCode(Product.class);
    }

    @Test
    void testProductOption() throws Exception {
        testEqualsAndHashCode(ProductOption.class);
    }

    @Test
    void testProductOptionValue() throws Exception {
        testEqualsAndHashCode(ProductOptionValue.class);
    }

    @Test
    void testProductOptionCombination() throws Exception {
        testEqualsAndHashCode(ProductOptionCombination.class);
    }

    @Test
    void testProductRelated() throws Exception {
        testEqualsAndHashCode(ProductRelated.class);
    }

    @Test
    void testProductAttribute() throws Exception {
        testEqualsAndHashCode(ProductAttribute.class);
    }

    @Test
    void testProductAttributeGroup() throws Exception {
        testEqualsAndHashCode(ProductAttributeGroup.class);
    }

    @Test
    void testProductTemplate() throws Exception {
        testEqualsAndHashCode(ProductTemplate.class);
    }
}
