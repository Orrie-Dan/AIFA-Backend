package com.aifa.modules.ledger.infrastructure;

import com.aifa.modules.ledger.domain.Category;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findByUserIdIsNullOrUserIdOrderByNameAsc(UUID userId);

    Optional<Category> findBySlugAndUserIdIsNull(String slug);

    Optional<Category> findBySlugAndUserId(String slug, UUID userId);

    boolean existsBySlugAndUserId(String slug, UUID userId);
}
