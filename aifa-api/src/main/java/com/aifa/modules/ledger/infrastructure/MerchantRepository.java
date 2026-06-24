package com.aifa.modules.ledger.infrastructure;

import com.aifa.modules.ledger.domain.Merchant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MerchantRepository extends JpaRepository<Merchant, UUID> {

    Optional<Merchant> findByNormalizedName(String normalizedName);

    @Query("""
            SELECT m FROM Merchant m
            WHERE LOCATE(m.normalizedName, :normalizedName) > 0
            ORDER BY LENGTH(m.normalizedName) DESC
            """)
    List<Merchant> findMatches(@Param("normalizedName") String normalizedName);
}
