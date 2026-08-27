package com.minibank.backend.saving.controller;

import com.minibank.backend.common.security.CurrentJwt;
import com.minibank.backend.saving.dto.CreateSavingRequest;
import com.minibank.backend.saving.dto.SavingOpenConfirmRequest;
import com.minibank.backend.saving.dto.SavingOpenConfirmResponse;
import com.minibank.backend.saving.dto.SavingOpenInitiateRequest;
import com.minibank.backend.saving.dto.SavingOpenInitiateResponse;
import com.minibank.backend.saving.dto.SavingProductResponse;
import com.minibank.backend.saving.dto.SavingResponse;
import com.minibank.backend.saving.service.SavingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mobile/savings")
public class MobileSavingController {

    private final SavingService savingService;

    public MobileSavingController(SavingService savingService) {
        this.savingService = savingService;
    }

    /** List the current user's savings. */
    @GetMapping
    public List<SavingResponse> getSavings() {
        return savingService.getSavings(CurrentJwt.requireUserId());
    }

    /** Get a single saving (must belong to current user). */
    @GetMapping("/{id}")
    public SavingResponse getSaving(@PathVariable Long id) {
        return savingService.getSaving(CurrentJwt.requireUserId(), id);
    }

    /**
     * Create a new saving.
     * Status will be PENDING_APPROVAL until admin activates.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SavingResponse createSaving(@Valid @RequestBody CreateSavingRequest request) {
        return savingService.createSaving(CurrentJwt.requireUserId(), request);
    }

    /**
     * Initiate saving opening: requires agreement accepted, sends OTP.
     */
    @PostMapping("/open/initiate")
    public SavingOpenInitiateResponse initiateOpen(@Valid @RequestBody SavingOpenInitiateRequest request) {
        return savingService.initiateOpenSaving(CurrentJwt.requireUserId(), request);
    }

    /**
     * Confirm saving opening with OTP: debits funds and moves to pending_approval.
     */
    @PostMapping("/open/confirm")
    public SavingOpenConfirmResponse confirmOpen(@Valid @RequestBody SavingOpenConfirmRequest request) {
        return savingService.confirmOpenSaving(CurrentJwt.requireUserId(), request);
    }

    /**
     * List active saving products available for the customer to pick from.
     * No auth restriction — products are public within the app.
     */
    @GetMapping("/products")
    public List<SavingProductResponse> getProducts() {
        return savingService.getActiveSavingProducts();
    }
}
