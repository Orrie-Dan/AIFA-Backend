package com.aifa.modules.ledger.application;

import com.aifa.modules.ledger.domain.CategorySource;
import com.aifa.modules.ledger.infrastructure.MerchantRepository;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class MerchantLookupService {

    private static final Map<String, String> KEYWORD_SLUGS = Map.of(
            "kigali bus", "transport",
            "simba", "food",
            "nakumatt", "food",
            "sp rwanda", "transport",
            "mtn", "other",
            "airtel", "other");

    private final MerchantRepository merchantRepository;
    private final CategoryService categoryService;

    public MerchantLookupService(MerchantRepository merchantRepository, CategoryService categoryService) {
        this.merchantRepository = merchantRepository;
        this.categoryService = categoryService;
    }

    public Optional<CategoryMatch> resolveCategory(String merchantName) {
        if (merchantName == null || merchantName.isBlank()) {
            return Optional.empty();
        }
        String normalized = normalize(merchantName);

        var merchantMatch = merchantRepository.findByNormalizedName(normalized);
        if (merchantMatch.isEmpty()) {
            var matches = merchantRepository.findMatches(normalized);
            merchantMatch = matches.isEmpty() ? Optional.empty() : Optional.of(matches.get(0));
        }
        if (merchantMatch.isPresent() && merchantMatch.get().getCategoryId() != null) {
            return Optional.of(new CategoryMatch(
                    merchantMatch.get().getCategoryId(), CategorySource.merchant_table));
        }

        for (Map.Entry<String, String> keyword : KEYWORD_SLUGS.entrySet()) {
            if (normalized.contains(keyword.getKey())) {
                return categoryService.findSystemCategoryId(keyword.getValue())
                        .map(id -> new CategoryMatch(id, CategorySource.keyword));
            }
        }
        return Optional.empty();
    }

    public static String normalize(String name) {
        return name.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }

    public record CategoryMatch(UUID categoryId, CategorySource source) {}
}
