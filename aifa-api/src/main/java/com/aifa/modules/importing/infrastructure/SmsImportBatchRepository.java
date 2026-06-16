package com.aifa.modules.importing.infrastructure;

import com.aifa.modules.importing.domain.SmsImportBatch;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SmsImportBatchRepository extends JpaRepository<SmsImportBatch, UUID> {}
