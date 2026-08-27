package com.minibank.backend.contract.controller;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.minibank.backend.common.otp.SmsOtpService;
import com.minibank.backend.common.security.CurrentJwt;
import com.minibank.backend.contract.dto.ContractAcceptRequest;
import com.minibank.backend.contract.dto.ContractAcceptResult;
import com.minibank.backend.contract.dto.ContractOtpSendResponse;
import com.minibank.backend.contract.dto.TemplateSummary;
import com.minibank.backend.contract.service.ContractTemplateService;
import com.minibank.backend.contract.service.MobileContractService;
import com.minibank.backend.user.entity.User;
import com.minibank.backend.user.repository.UserRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Mobile API — người dùng xem template và ký/từ chối hợp đồng.
 *
 * Endpoints:
 *   GET  /api/mobile/contract-templates/active?code=SAVING_AGREEMENT
 *        — Lấy template đang active cho loại hợp đồng. Mobile dùng để render nội dung.
 *
 *   POST /api/mobile/contracts/accept
 *        — Người dùng chấp nhận & ký điện tử.
 *        Body: { referenceType, referenceId, templateCode, signatureData? }
 *
 *   POST /api/mobile/contracts/decline
 *        — Người dùng từ chối hợp đồng.
 */
@RestController("contractMobileContractController")
@RequestMapping("/api/mobile")
@RequiredArgsConstructor
public class MobileContractController {

    private final ContractTemplateService templateService;
    private final MobileContractService mobileContractService;
    private final UserRepository userRepository;
    private final SmsOtpService smsOtpService;

    // ── Template (đã có sẵn, giữ nguyên) ────────────────────────────────────

    @GetMapping("/contract-templates/active")
    public TemplateSummary getActiveTemplate(@RequestParam String code) {
        CurrentJwt.requireUserId();
        return templateService.getActiveByCode(code);
    }

    @PostMapping("/contracts/otp/send")
    @Transactional(readOnly = true)
    public ContractOtpSendResponse sendContractOtp() {
        User user = currentUser();
        if (user.getPhone() == null || user.getPhone().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phone number is required");
        }
        SmsOtpService.OtpSendResult result = smsOtpService.sendOtp(user.getPhone());
        return new ContractOtpSendResponse(result.devMode(), result.otp());
    }

    // ── Accept / Decline ─────────────────────────────────────────────────────

    /**
     * Ký điện tử hợp đồng.
     *
     * Với SAVING_AGREEMENT: đánh dấu saving.agreementAcceptedAt + agreementVersion.
     * Với LOAN_CREDIT / LOAN_MORTGAGE: tạo Contract entity với status SIGNED và signedAt = now().
     *
     * @return ContractAcceptResult chứa contractNumber và fileUrl (nếu sinh được PDF/HTML)
     */
    @PostMapping("/contracts/accept")
    public ContractAcceptResult accept(@Valid @RequestBody ContractAcceptRequest req) {
        User user = currentUser();
        if (req.otpCode() == null || req.otpCode().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP is required");
        }
        if (!smsOtpService.verifyOtp(user.getPhone(), req.otpCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid OTP");
        }
        return mobileContractService.accept(user.getId(), req);
    }

    /**
     * Từ chối hợp đồng — ghi nhận lý do, không tạo contract entity.
     */
    @PostMapping("/contracts/decline")
    public void decline(@Valid @RequestBody ContractAcceptRequest req) {
        Long userId = CurrentJwt.requireUserId();
        mobileContractService.decline(userId, req);
    }

    private User currentUser() {
        Long userId = CurrentJwt.requireUserId();
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
