package com.digimate.service;

import com.digimate.dto.CategoryResponse;
import com.digimate.entity.Category;
import com.digimate.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void findAllShouldReturnCategoriesSortedByName() {

        Category first = new Category();
        first.setId(1L);
        first.setName("Computer Help");

        Category second = new Category();
        second.setId(2L);
        second.setName("Phone Setup");

        when(categoryRepository.findAll(Sort.by("name")))
                .thenReturn(List.of(first, second));

        List<CategoryResponse> result = categoryService.findAll();

        assertEquals(2, result.size());

        assertEquals(
                new CategoryResponse(1L, "Computer Help"),
                result.get(0)
        );

        assertEquals(
                new CategoryResponse(2L, "Phone Setup"),
                result.get(1)
        );

        verify(categoryRepository).findAll(Sort.by("name"));
    }
}
