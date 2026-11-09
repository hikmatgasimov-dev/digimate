package com.digimate.controller.api;

import com.digimate.dto.CategoryResponse;
import com.digimate.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Category listing")
public class CategoryApiController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(summary = "List all categories alphabetically")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "List of categories"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not logged in"
            )
    })
    public List<CategoryResponse> findAll() {
        return categoryService.findAll();
    }
}
