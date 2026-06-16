package com.aifa.modules.ledger.infrastructure;

import com.aifa.modules.ledger.application.CategoryService;
import com.aifa.modules.ledger.application.WalletService;
import com.aifa.modules.ledger.application.dto.CategoryResponse;
import com.aifa.modules.ledger.application.dto.CreateCategoryRequest;
import com.aifa.modules.ledger.application.dto.CreateWalletRequest;
import com.aifa.modules.ledger.application.dto.UpdateWalletRequest;
import com.aifa.modules.ledger.application.dto.WalletResponse;
import com.aifa.shared.security.CurrentUserProvider;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class WalletController {

    private final WalletService walletService;
    private final CurrentUserProvider currentUserProvider;

    public WalletController(WalletService walletService, CurrentUserProvider currentUserProvider) {
        this.walletService = walletService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/wallets")
    public List<WalletResponse> listWallets() {
        return walletService.listWallets(currentUserProvider.getCurrentUserId());
    }

    @PostMapping("/wallets")
    @ResponseStatus(HttpStatus.CREATED)
    public WalletResponse createWallet(@Valid @RequestBody CreateWalletRequest request) {
        return walletService.createWallet(currentUserProvider.getCurrentUserId(), request);
    }

    @PatchMapping("/wallets/{id}")
    public WalletResponse updateWallet(@PathVariable UUID id, @Valid @RequestBody UpdateWalletRequest request) {
        return walletService.updateWallet(currentUserProvider.getCurrentUserId(), id, request);
    }
}
