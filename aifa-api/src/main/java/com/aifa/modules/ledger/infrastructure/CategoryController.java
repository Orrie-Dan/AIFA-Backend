package com.aifa.modules.ledger.infrastructure;

import com.aifa.modules.ledger.application.CategoryService;
import com.aifa.modules.ledger.application.dto.CategoryResponse;
import com.aifa.modules.ledger.application.dto.CreateCategoryRequest;
import com.aifa.shared.security.CurrentUserProvider;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;
    private final CurrentUserProvider currentUserProvider;

    public CategoryController(CategoryService categoryService, CurrentUserProvider currentUserProvider) {
        this.categoryService = categoryService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public List<CategoryResponse> listCategories() {
        return categoryService.listCategories(currentUserProvider.getCurrentUserId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        return categoryService.createCategory(currentUserProvider.getCurrentUserId(), request);
    }
}
