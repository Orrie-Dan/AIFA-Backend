package com.aifa.modules.importing.infrastructure;

import com.aifa.modules.importing.application.SmsImportService;
import com.aifa.modules.importing.application.dto.SmsImportConfirmRequest;
import com.aifa.modules.importing.application.dto.SmsImportConfirmResponse;
import com.aifa.modules.importing.application.dto.SmsImportPreviewRequest;
import com.aifa.modules.importing.application.dto.SmsImportPreviewResponse;
import com.aifa.shared.security.CurrentUserProvider;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/import/sms")
public class SmsImportController {

    private final SmsImportService smsImportService;
    private final CurrentUserProvider currentUserProvider;

    public SmsImportController(SmsImportService smsImportService, CurrentUserProvider currentUserProvider) {
        this.smsImportService = smsImportService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SmsImportPreviewResponse preview(@Valid @RequestBody SmsImportPreviewRequest request) {
        return smsImportService.preview(currentUserProvider.getCurrentUserId(), request);
    }

    @PostMapping("/confirm")
    public SmsImportConfirmResponse confirm(@Valid @RequestBody SmsImportConfirmRequest request) {
        return smsImportService.confirm(currentUserProvider.getCurrentUserId(), request);
    }
}
