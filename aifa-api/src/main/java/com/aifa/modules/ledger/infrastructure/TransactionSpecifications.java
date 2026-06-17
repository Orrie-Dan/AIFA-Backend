package com.aifa.modules.ledger.infrastructure;

import com.aifa.modules.ledger.domain.Transaction;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

final class TransactionSpecifications {

    private TransactionSpecifications() {}

    static Specification<Transaction> forUser(UUID userId) {
        return (root, query, cb) -> cb.equal(root.get("userId"), userId);
    }

    static Specification<Transaction> from(Instant from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("transactionAt"), from);
    }

    static Specification<Transaction> to(Instant to) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("transactionAt"), to);
    }

    static Specification<Transaction> categoryId(UUID categoryId) {
        return (root, query, cb) -> cb.equal(root.get("categoryId"), categoryId);
    }

    static Specification<Transaction> filtered(UUID userId, Instant from, Instant to, UUID categoryId) {
        Specification<Transaction> spec = forUser(userId);
        if (from != null) {
            spec = spec.and(from(from));
        }
        if (to != null) {
            spec = spec.and(to(to));
        }
        if (categoryId != null) {
            spec = spec.and(categoryId(categoryId));
        }
        return spec;
    }
}
