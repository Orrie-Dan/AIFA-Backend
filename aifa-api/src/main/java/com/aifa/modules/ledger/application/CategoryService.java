package com.aifa.modules.ledger.application;

import com.aifa.modules.ledger.application.dto.CategoryResponse;
import com.aifa.modules.ledger.application.dto.CreateCategoryRequest;
import com.aifa.modules.ledger.domain.Category;
import com.aifa.modules.ledger.infrastructure.CategoryRepository;
import com.aifa.shared.audit.AuditLogger;
import com.aifa.shared.exception.ConflictException;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final AuditLogger auditLogger;
    private final Clock clock;

    public CategoryService(CategoryRepository categoryRepository, AuditLogger auditLogger, Clock clock) {
        this.categoryRepository = categoryRepository;
        this.auditLogger = auditLogger;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> listCategories(UUID userId) {
        return categoryRepository.findByUserIdIsNullOrUserIdOrderByNameAsc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CategoryResponse createCategory(UUID userId, CreateCategoryRequest request) {
        if (categoryRepository.existsBySlugAndUserId(request.slug(), userId)) {
            throw new ConflictException("Category slug already exists");
        }

        Category category = new Category();
        category.setName(request.name().trim());
        category.setSlug(request.slug().trim().toLowerCase());
        category.setIcon(request.icon());
        category.setSystem(false);
        category.setUserId(userId);
        category.setCreatedAt(clock.instant());
        categoryRepository.save(category);

        auditLogger.logAction(userId, "CREATE", "category", category.getSlug());
        return toResponse(category);
    }

    @Transactional(readOnly = true)
    public Optional<UUID> findSystemCategoryId(String slug) {
        return categoryRepository.findBySlugAndUserId(slug, null).map(Category::getId);
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(), category.getName(), category.getSlug(), category.getIcon(), category.isSystem());
    }
}
