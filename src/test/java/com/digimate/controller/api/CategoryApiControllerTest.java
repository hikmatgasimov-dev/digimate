package com.digimate.controller.api;

import com.digimate.config.SecurityConfig;
import com.digimate.dto.CategoryResponse;
import com.digimate.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** MVC tests for category responses and authenticated access. */
@WebMvcTest(CategoryApiController.class)
@Import(SecurityConfig.class)
class CategoryApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void shouldReturn401WhenAnonymousListsCategories() throws Exception {
        mockMvc.perform(get("/api/categories")).andExpect(status().isUnauthorized());

        verifyNoInteractions(categoryService);
    }

    @Test
    @WithMockUser
    void shouldReturnCategoriesForAuthenticatedUser() throws Exception {
        when(categoryService.findAll()).thenReturn(List.of(
                new CategoryResponse(2L, "Banking"),
                new CategoryResponse(1L, "Phone")));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].name").value("Banking"))
                .andExpect(jsonPath("$[1].id").value(1))
                .andExpect(jsonPath("$[1].name").value("Phone"));
    }

    @Test
    @WithMockUser
    void shouldReturnEmptyListWhenNoCategoriesExist() throws Exception {
        when(categoryService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}
