package com.aifa.modules.importing.infrastructure;

import com.aifa.modules.importing.domain.SmsParsedRow;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SmsParsedRowRepository extends JpaRepository<SmsParsedRow, UUID> {

    List<SmsParsedRow> findByBatchIdOrderByRowIndexAsc(UUID batchId);
}
