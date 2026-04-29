package com.yas.product.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.yas.product.model.Category;
import com.yas.product.repository.CategoryRepository;
import com.yas.product.viewmodel.NoFileMediaVm;
import com.yas.product.viewmodel.category.CategoryGetDetailVm;
import com.yas.product.viewmodel.category.CategoryGetVm;
import com.yas.product.viewmodel.category.CategoryPostVm;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategoryServiceUnitTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private MediaService mediaService;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void create_withoutParentId_keepsParentNull() {
        CategoryPostVm postVm = new CategoryPostVm(
                "cat-name",
                "cat-slug",
                "desc",
                null,
                "meta-key",
                "meta-desc",
                (short) 1,
                true,
                null
        );

        when(categoryRepository.findExistedName("cat-name", null)).thenReturn(null);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        Category created = categoryService.create(postVm);

        assertNull(created.getParent());
        assertEquals("cat-name", created.getName());
    }

    @Test
    void update_withValidParent_setsParent() {
        Category existing = new Category();
        existing.setId(1L);

        Category parent = new Category();
        parent.setId(2L);

        when(categoryRepository.findExistedName("cat-name", 1L)).thenReturn(null);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(parent));

        CategoryPostVm postVm = new CategoryPostVm(
                "cat-name",
                "cat-slug",
                "desc",
                2L,
                null,
                null,
                (short) 1,
                true,
                null
        );

        categoryService.update(postVm, 1L);

        assertSame(parent, existing.getParent());
    }

    @Test
    void getCategoryById_withParentAndImage_returnsParentIdAndImage() {
        Category parent = new Category();
        parent.setId(10L);

        Category category = new Category();
        category.setId(11L);
        category.setName("child");
        category.setSlug("child");
        category.setDisplayOrder((short) 1);
        category.setIsPublished(true);
        category.setParent(parent);
        category.setImageId(99L);

        when(categoryRepository.findById(11L)).thenReturn(Optional.of(category));
        when(mediaService.getMedia(99L)).thenReturn(new NoFileMediaVm(99L, "", "", "", "url"));

        CategoryGetDetailVm result = categoryService.getCategoryById(11L);

        assertEquals(10L, result.parentId());
        assertEquals("url", result.categoryImage().url());
    }

    @Test
    void getCategories_withMixedImageAndParent_coversBranches() {
        Category parent = new Category();
        parent.setId(30L);

        Category noImage = new Category();
        noImage.setId(31L);
        noImage.setName("no-image");
        noImage.setSlug("no-image");

        Category withImageAndParent = new Category();
        withImageAndParent.setId(32L);
        withImageAndParent.setName("with-image");
        withImageAndParent.setSlug("with-image");
        withImageAndParent.setParent(parent);
        withImageAndParent.setImageId(100L);

        when(categoryRepository.findByNameContainingIgnoreCase("cat"))
                .thenReturn(List.of(noImage, withImageAndParent));
        when(mediaService.getMedia(100L)).thenReturn(new NoFileMediaVm(100L, "", "", "", "url"));

        List<CategoryGetVm> result = categoryService.getCategories("cat");

        assertEquals(-1L, result.getFirst().parentId());
        assertEquals(30L, result.get(1).parentId());
        assertEquals("url", result.get(1).categoryImage().url());
    }
}
