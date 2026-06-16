package com.aifa.shared.audit;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AuditLogger {

    private static final Logger log = LoggerFactory.getLogger(AuditLogger.class);

    public void logAction(UUID userId, String action, String resource, String detail) {
        log.info("AUDIT userId={} action={} resource={} detail={}", userId, action, resource, detail);
    }
}
