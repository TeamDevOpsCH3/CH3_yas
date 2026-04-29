package com.yas.product.viewmodel.category;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yas.product.model.Category;
import org.junit.jupiter.api.Test;

class CategoryViewModelTest {

    @Test
    void categoryGetVm_withParent_usesParentId() {
        Category parent = new Category();
        parent.setId(10L);

        Category child = new Category();
        child.setId(11L);
        child.setName("child");
        child.setSlug("child");
        child.setParent(parent);

        CategoryGetVm result = CategoryGetVm.fromModel(child);

        assertEquals(10L, result.parentId());
    }

    @Test
    void categoryGetDetailVm_withParent_usesParentId() {
        Category parent = new Category();
        parent.setId(21L);

        Category child = new Category();
        child.setId(22L);
        child.setName("child");
        child.setSlug("child");
        child.setDisplayOrder((short) 1);
        child.setIsPublished(true);
        child.setParent(parent);

        CategoryGetDetailVm result = CategoryGetDetailVm.fromModel(child);

        assertEquals(21L, result.parentId());
    }
}
