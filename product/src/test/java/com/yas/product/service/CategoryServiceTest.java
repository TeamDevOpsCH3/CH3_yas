package com.yas.product.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.BadRequestException;
import com.yas.commonlibrary.exception.DuplicatedException;
import com.yas.product.ProductApplication;
import com.yas.product.model.Category;
import com.yas.product.repository.CategoryRepository;
import com.yas.product.repository.ProductCategoryRepository;
import com.yas.product.viewmodel.NoFileMediaVm;
import com.yas.product.viewmodel.category.CategoryGetDetailVm;
import com.yas.product.viewmodel.category.CategoryGetVm;
import com.yas.product.viewmodel.category.CategoryPostVm;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(classes = ProductApplication.class)
class CategoryServiceTest {
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ProductCategoryRepository productCategoryRepository;
    @MockitoBean
    private MediaService mediaService;
    @Autowired
    private CategoryService categoryService;

    private Category category;
    private NoFileMediaVm noFileMediaVm;

    @BeforeEach
    void setUp() {

        category = new Category();
        category.setName("name");
        category.setSlug("slug");
        category.setDescription("description");
        category.setMetaKeyword("metaKeyword");
        category.setMetaDescription("metaDescription");
        category.setDisplayOrder((short) 1);
        category.setIsPublished(true);
        category.setImageId(1L);
        categoryRepository.save(category);

        noFileMediaVm = new NoFileMediaVm(1L, "caption", "fileName", "mediaType", "url");
    }

    @AfterEach
    void tearDown() {
        productCategoryRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    void getCategoryById_Success() {
        when(mediaService.getMedia(category.getImageId())).thenReturn(noFileMediaVm);
        CategoryGetDetailVm categoryGetDetailVm = categoryService.getCategoryById(category.getId());
        assertNotNull(categoryGetDetailVm);
        assertEquals("name", categoryGetDetailVm.name());
    }

    @Test
    void getCategories_Success() {
        when(mediaService.getMedia(any())).thenReturn(noFileMediaVm);
        Assertions.assertEquals(1, categoryService.getCategories("name").size());
        CategoryGetVm categoryGetVm = categoryService.getCategories("name").getFirst();
        assertEquals("name", categoryGetVm.name());
    }

    @Test
    void getCategoriesPageable_Success() {
        when(mediaService.getMedia(category.getImageId())).thenReturn(noFileMediaVm);
        Assertions.assertEquals(1, categoryService.getPageableCategories(0, 1).categoryContent().size());
        CategoryGetVm categoryGetVm = categoryService.getCategories("a").getFirst();
        assertEquals("name", categoryGetVm.name());
    }

    @Test
    void createCategory_withParentId_setsParent() {
        Category parent = new Category();
        parent.setName("parent-name");
        parent.setSlug("parent-slug");
        categoryRepository.save(parent);

        CategoryPostVm postVm = new CategoryPostVm(
                "child-name",
                "child-slug",
                "child-desc",
                parent.getId(),
                "meta-key",
                "meta-desc",
                (short) 2,
                true,
                null
        );

        Category created = categoryService.create(postVm);

        assertNotNull(created.getParent());
        assertEquals(parent.getId(), created.getParent().getId());
    }

    @Test
    void createCategory_withMissingParentId_throwsBadRequest() {
        CategoryPostVm postVm = new CategoryPostVm(
                "child-missing-parent",
                "child-missing-parent",
                "desc",
                9999L,
                null,
                null,
                (short) 1,
                true,
                null
        );

        Assertions.assertThrows(BadRequestException.class, () -> categoryService.create(postVm));
    }

    @Test
    void createCategory_withDuplicateName_throwsDuplicated() {
        CategoryPostVm postVm = new CategoryPostVm(
                "name",
                "name-slug",
                "desc",
                null,
                null,
                null,
                (short) 1,
                true,
                null
        );

        Assertions.assertThrows(DuplicatedException.class, () -> categoryService.create(postVm));
    }

    @Test
    void updateCategory_withParentIdNull_clearsParent() {
        Category parent = new Category();
        parent.setName("update-parent");
        parent.setSlug("update-parent");
        categoryRepository.save(parent);

        Category child = new Category();
        child.setName("update-child");
        child.setSlug("update-child");
        child.setParent(parent);
        categoryRepository.save(child);

        CategoryPostVm postVm = new CategoryPostVm(
                "update-child",
                "update-child",
                "desc",
                null,
                null,
                null,
                (short) 1,
                true,
                null
        );

        categoryService.update(postVm, child.getId());

        Category updated = categoryRepository.findById(child.getId()).orElseThrow();
        assertNull(updated.getParent());
    }

    @Test
    void updateCategory_withParentIdSelf_throwsBadRequest() {
        CategoryPostVm postVm = new CategoryPostVm(
                "name",
                "slug",
                "desc",
                category.getId(),
                null,
                null,
                (short) 1,
                true,
                null
        );

        Assertions.assertThrows(BadRequestException.class, () -> categoryService.update(postVm, category.getId()));
    }

    @Test
    void getCategoryById_withoutImageAndParent_returnsDefaults() {
        Category noImage = new Category();
        noImage.setName("no-image");
        noImage.setSlug("no-image");
        noImage.setIsPublished(true);
        noImage.setDisplayOrder((short) 0);
        categoryRepository.save(noImage);

        CategoryGetDetailVm result = categoryService.getCategoryById(noImage.getId());

        assertEquals(0L, result.parentId());
        assertNull(result.categoryImage());
    }

    @Test
    void getCategories_withParentAndNoImage_returnsParentId() {
        Category parent = new Category();
        parent.setName("parent-search");
        parent.setSlug("parent-search");
        categoryRepository.save(parent);

        Category child = new Category();
        child.setName("child-search");
        child.setSlug("child-search");
        child.setParent(parent);
        categoryRepository.save(child);

        CategoryGetVm categoryGetVm = categoryService.getCategories("child-search").getFirst();
        assertEquals(parent.getId(), categoryGetVm.parentId());
        assertNull(categoryGetVm.categoryImage());
    }
}