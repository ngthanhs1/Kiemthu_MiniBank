package com.minibank.backend.saving.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.minibank.backend.account.entity.Account;
import com.minibank.backend.account.entity.AccountBalanceLedger;
import com.minibank.backend.account.repository.AccountBalanceLedgerRepository;
import com.minibank.backend.account.repository.AccountRepository;
import com.minibank.backend.admin.entity.AdminUser;
import com.minibank.backend.admin.repository.AdminUserRepository;
import com.minibank.backend.common.otp.SmsOtpService;
import com.minibank.backend.contract.repository.ContractRepository;
import com.minibank.backend.contract.repository.ContractTemplateRepository;
import com.minibank.backend.saving.dto.CreateSavingRequest;
import com.minibank.backend.saving.dto.SavingOpenConfirmRequest;
import com.minibank.backend.saving.dto.SavingOpenConfirmResponse;
import com.minibank.backend.saving.dto.SavingOpenInitiateRequest;
import com.minibank.backend.saving.dto.SavingOpenInitiateResponse;
import com.minibank.backend.saving.dto.SavingProductResponse;
import com.minibank.backend.saving.dto.SavingResponse;
import com.minibank.backend.saving.entity.Saving;
import com.minibank.backend.saving.entity.SavingProduct;
import com.minibank.backend.saving.repository.SavingProductRepository;
import com.minibank.backend.saving.repository.SavingRepository;
import com.minibank.backend.system.service.NotificationService;
import com.minibank.backend.transaction.entity.Transaction;
import com.minibank.backend.transaction.entity.TransactionAuthentication;
import com.minibank.backend.transaction.repository.TransactionAuthenticationRepository;
import com.minibank.backend.transaction.repository.TransactionRepository;
import com.minibank.backend.user.entity.User;
import com.minibank.backend.user.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SavingService {

    private static final Pattern SAVING_ID_PATTERN = Pattern.compile("\\bsavingId=(\\d+)\\b");

    private final SavingRepository savingRepository;
    private final SavingProductRepository savingProductRepository;
    private final AccountRepository accountRepository;
    private final AccountBalanceLedgerRepository ledgerRepository;
    private final UserRepository userRepository;
    private final AdminUserRepository adminUserRepository;
    private final ContractRepository contractRepository;
    private final ContractTemplateRepository contractTemplateRepository;

    private final SmsOtpService smsOtpService;
    private final TransactionRepository transactionRepository;
    private final TransactionAuthenticationRepository transactionAuthenticationRepository;
    private final NotificationService notificationService;

    public List<SavingResponse> getSavings(long userId) {
        return savingRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(SavingResponse::from)
                .toList();
    }

    public SavingResponse getSaving(long userId, long savingId) {
        Saving saving = savingRepository.findWithDetailsByIdAndUserId(savingId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Khong tim thay so tiet kiem"));
        return SavingResponse.from(saving);
    }

    @Transactional
    public SavingOpenInitiateResponse initiateOpenSaving(long userId, SavingOpenInitiateRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        if (!"active".equalsIgnoreCase(user.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is not active");
        }

        SavingProduct product = savingProductRepository.findById(request.savingProductId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Saving product not found"));
        validateProductActive(product);

        Account sourceAccount = accountRepository.findById(request.sourceAccountId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Source account not found"));
        Account settlementAccount = resolveSettlementAccount(request.settlementAccountId(), sourceAccount);

        validateAccountOwner(sourceAccount, userId);
        validateAccountActive(sourceAccount);
        validateAccountOwner(settlementAccount, userId);
        validateAccountActive(settlementAccount);
        validatePrincipalRange(request.principalAmount(), product);
        validateSufficientBalance(sourceAccount, request.principalAmount());

        Saving saving = buildSaving(user, product, sourceAccount, settlementAccount, request.principalAmount(), request.autoRenew());
        saving.setStatus("pending_otp");
        saving.setOpenDate(null);
        saving.setMaturityDate(null);
        saving.setAgreementAcceptedAt(Instant.now());
        saving.setAgreementVersion((request.agreementVersion() == null || request.agreementVersion().isBlank())
            ? "saving_agreement_v1"
            : request.agreementVersion().trim());
        saving = savingRepository.save(saving);

        SmsOtpService.OtpSendResult otpResult = smsOtpService.sendOtp(user.getPhone());
        String otp = otpResult.otp();
        String otpHash = otp == null ? null : sha256Hex(otp);

        Transaction tx = Transaction.builder()
            .transactionCode(generateTransactionCode())
            .fromAccount(sourceAccount)
            .toAccount(settlementAccount)
            .transactionType("saving_open")
            .amount(request.principalAmount())
            .feeAmount(BigDecimal.ZERO)
            .description("savingId=" + saving.getId())
            .status("pending")
            .initiatedByUser(user)
            .build();
        tx = transactionRepository.save(tx);

        TransactionAuthentication auth = TransactionAuthentication.builder()
            .transaction(tx)
            .pinVerified(true)
            .otpVerified(false)
            .otpCodeHash(otpHash)
            .digitalSignature(null)
            .authStatus("otp_sent")
            .build();
        transactionAuthenticationRepository.save(auth);

        return new SavingOpenInitiateResponse(
            tx.getId(),
            tx.getTransactionCode(),
            tx.getStatus(),
            true,
            Boolean.TRUE.equals(otpResult.devMode()) ? otp : null
        );
    }

    @Transactional
    public SavingOpenConfirmResponse confirmOpenSaving(long userId, SavingOpenConfirmRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        if (!"active".equalsIgnoreCase(user.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is not active");
        }

        Transaction tx = transactionRepository.findByIdAndInitiatedByUserId(request.transactionId(), user.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
        if (!"saving_open".equalsIgnoreCase(tx.getTransactionType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transaction is not saving_open");
        }
        if ("completed".equalsIgnoreCase(tx.getStatus())) {
            Long savingId = extractSavingId(tx.getDescription());
            Saving s = savingRepository.findById(savingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Saving not found"));
            return new SavingOpenConfirmResponse(tx.getId(), tx.getStatus(), s.getId(), s.getCode());
        }
        if (!"pending".equalsIgnoreCase(tx.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transaction is not pending");
        }

        TransactionAuthentication auth = transactionAuthenticationRepository.findByTransactionId(tx.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Missing transaction authentication"));
        if (auth.getOtpCodeHash() == null || !sha256Hex(request.otpCode()).equals(auth.getOtpCodeHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid OTP");
        }

        Long savingId = extractSavingId(tx.getDescription());
        Saving saving = savingRepository.findById(savingId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Saving not found"));
        if (saving.getUser() == null || saving.getUser().getId() == null || !saving.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed");
        }
        if (!"pending_otp".equalsIgnoreCase(saving.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Saving is not pending_otp");
        }

        Account fromAccount = accountRepository.findByIdForUpdate(tx.getFromAccount().getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Sender account missing"));
        BigDecimal amount = tx.getAmount();
        if (fromAccount.getAvailableBalance().compareTo(amount) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient balance");
        }
        BigDecimal before = fromAccount.getAvailableBalance();
        fromAccount.setAvailableBalance(before.subtract(amount));
        fromAccount.setCurrentBalance(fromAccount.getCurrentBalance().subtract(amount));
        accountRepository.save(fromAccount);

        ledgerRepository.save(AccountBalanceLedger.builder()
            .account(fromAccount)
            .transaction(tx)
            .entryType("debit")
            .amount(amount)
            .balanceBefore(before)
            .balanceAfter(fromAccount.getAvailableBalance())
            .build());

        auth.setOtpVerified(true);
        auth.setAuthStatus("verified");
        auth.setVerifiedAt(Instant.now());
        transactionAuthenticationRepository.save(auth);

        tx.setStatus("completed");
        tx.setCompletedAt(Instant.now());
        transactionRepository.save(tx);

        // Funds are already debited, but saving still requires admin approval.
        saving.setStatus("pending_approval");
        saving.setOpenDate(null);
        saving.setMaturityDate(null);
        saving.setReviewedAt(null);
        saving.setReviewedBy(null);
        saving.setRejectionReason(null);
        savingRepository.save(saving);

        return new SavingOpenConfirmResponse(tx.getId(), tx.getStatus(), saving.getId(), saving.getCode());
    }

    @Transactional
    public SavingResponse createSaving(long userId, CreateSavingRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Nguoi dung khong ton tai"));

        SavingProduct product = savingProductRepository.findById(request.savingProductId())
                .orElseThrow(() -> new EntityNotFoundException("San pham tiet kiem khong ton tai"));

        Account sourceAccount = accountRepository.findById(request.sourceAccountId())
                .orElseThrow(() -> new EntityNotFoundException("Tai khoan nguon khong ton tai"));
        Account settlementAccount = resolveSettlementAccount(request.settlementAccountId(), sourceAccount);

        validateProductActive(product);
        validateAccountOwner(sourceAccount, userId);
        validateAccountActive(sourceAccount);
        validateAccountOwner(settlementAccount, userId);
        validateAccountActive(settlementAccount);
        validatePrincipalRange(request.principalAmount(), product);
        validateSufficientBalance(sourceAccount, request.principalAmount());

        Saving saving = buildSaving(user, product, sourceAccount, settlementAccount, request.principalAmount(), request.autoRenew());
        saving.setStatus("pending_contract");
        saving = savingRepository.save(saving);

        try {
            com.minibank.backend.contract.entity.ContractTemplate tpl = contractTemplateRepository.findAll().stream().findFirst().orElse(null);
            com.minibank.backend.contract.entity.Contract c = com.minibank.backend.contract.entity.Contract.builder()
                .ownerType("saving")
                .ownerId(saving.getId())
                .template(tpl)
                .contractNumber("SAV-C" + UUID.randomUUID().toString().substring(0,8).toUpperCase())
                .status("SENT")
                .build();
            contractRepository.save(c);
        } catch (Exception ex) {
            // ignore
        }

        return SavingResponse.from(saving);
    }

    public List<SavingProductResponse> getActiveSavingProducts() {
        return savingProductRepository.findByStatusIgnoreCaseOrderByBaseInterestRateDesc("active")
                .stream()
                .map(SavingProductResponse::from)
                .toList();
    }

    @Transactional
    public SavingResponse approveSaving(long savingId) {
        Saving saving = savingRepository.findById(savingId)
                .orElseThrow(() -> new EntityNotFoundException("Khong tim thay so tiet kiem"));

        String currentStatus = normalizeStatus(saving.getStatus());
        if (!isAwaitingApproval(currentStatus)) {
            throw new IllegalStateException("So khong o trang thai cho duyet");
        }

        // Legacy pending_contract flow: debit happens at manual approval time.
        if ("pending_contract".equals(currentStatus)) {
            debitSourceAccountForApproval(saving);
        }

        Instant now = Instant.now();
        saving.setStatus("active");
        saving.setReviewedAt(now);
        saving.setReviewedBy(getSystemAdminOrNull());
        saving.setRejectionReason(null);
        if (saving.getOpenDate() == null) {
            saving.setOpenDate(now);
        }
        if (saving.getMaturityDate() == null) {
            saving.setMaturityDate(calculateMaturityDate(saving.getOpenDate(), saving.getTermUnit(), saving.getTermValue()));
        }
        Saving saved = savingRepository.save(saving);
        if (saved.getUser() != null && saved.getUser().getId() != null) {
            try {
                notificationService.createForUser(
                    saved.getUser().getId(),
                    "SAVING",
                    "Sổ tiết kiệm đã được duyệt",
                    "Sổ tiết kiệm " + (saved.getCode() != null ? saved.getCode() : "#" + saved.getId())
                        + " của bạn đã được duyệt và đang hoạt động."
                );
            } catch (Exception ignored) {}
        }
        return SavingResponse.from(saved);
    }

    @Transactional
    public void rejectSaving(long savingId, String reason) {
        Saving saving = savingRepository.findById(savingId)
                .orElseThrow(() -> new EntityNotFoundException("Khong tim thay so tiet kiem"));

        String currentStatus = normalizeStatus(saving.getStatus());
        if (!isAwaitingApproval(currentStatus)) {
            throw new IllegalStateException("So khong o trang thai cho duyet");
        }

        if ("pending_approval".equals(currentStatus)) {
            refundSourceAccountOnReject(saving);
        }

        saving.setStatus("rejected");
        saving.setRejectionReason(reason);
        saving.setReviewedAt(Instant.now());
        saving.setReviewedBy(getSystemAdminOrNull());
        savingRepository.save(saving);

        if (saving.getUser() != null && saving.getUser().getId() != null) {
            try {
                String detail = (reason != null && !reason.isBlank()) ? " Lý do: " + reason : "";
                notificationService.createForUser(
                    saving.getUser().getId(),
                    "SAVING",
                    "Sổ tiết kiệm bị từ chối",
                    "Sổ tiết kiệm " + (saving.getCode() != null ? saving.getCode() : "#" + saving.getId())
                        + " đã bị từ chối." + detail
                );
            } catch (Exception ignored) {}
        }
    }

    private void validateProductActive(SavingProduct product) {
        if (!"active".equalsIgnoreCase(product.getStatus())) {
            throw new IllegalStateException("Saving product is not active");
        }
    }

    private void validateAccountOwner(Account account, long userId) {
        if (account.getUser() == null || !account.getUser().getId().equals(userId)) {
            throw new SecurityException("Source account does not belong to user");
        }
    }

    private void validateAccountActive(Account account) {
        if (!"active".equalsIgnoreCase(account.getStatus())) {
            throw new IllegalStateException("Source account is not active");
        }
    }

    private void validatePrincipalRange(BigDecimal principal, SavingProduct product) {
        if (principal.compareTo(product.getMinOpenAmount()) < 0) {
            throw new IllegalArgumentException("Amount must be >= " + product.getMinOpenAmount());
        }
        if (product.getMaxOpenAmount() != null && principal.compareTo(product.getMaxOpenAmount()) > 0) {
            throw new IllegalArgumentException("Amount must be <= " + product.getMaxOpenAmount());
        }
    }

    private void validateSufficientBalance(Account account, BigDecimal amount) {
        if (account.getAvailableBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient available balance");
        }
    }

    private Saving buildSaving(User user, SavingProduct product,
            Account sourceAccount, Account settlementAccount, BigDecimal principal, Boolean autoRenew) {
        return Saving.builder()
                .code(generateCode())
                .user(user)
                .savingProduct(product)
                .sourceAccount(sourceAccount)
                .settlementAccount(settlementAccount)
                .principalAmount(principal)
                .actualInterestRate(product.getBaseInterestRate())
                .interestRateType(product.getInterestRateType())
                .penaltyInterestRate(product.getPenaltyInterestRate())
                .bonusInterestRate(product.getBonusInterestRate())
                .interestAccrualFrequency(product.getInterestAccrualFrequency())
                .interestPostingFrequency(product.getInterestPostingFrequency())
                .capitalized(product.isCapitalized())
                .accruedInterestAmount(BigDecimal.ZERO)
                .postedInterestAmount(BigDecimal.ZERO)
                .depositFeeRate(product.getDepositFeeRate())
                .depositFeeFlat(product.getDepositFeeFlat())
                .withdrawalFeeRate(product.getWithdrawalFeeRate())
                .withdrawalFeeFlat(product.getWithdrawalFeeFlat())
                .closeFeeRate(product.getCloseFeeRate())
                .closeFeeFlat(product.getCloseFeeFlat())
                .managementFeeRate(product.getManagementFeeRate())
                .managementFeeFlat(product.getManagementFeeFlat())
                .managementFeeFrequency(product.getManagementFeeFrequency())
                .termUnit(product.getTermUnit())
                .termValue(product.getTermValue())
                .status("pending_approval")
                .openDate(null)
                .maturityDate(null)
                .autoRenew(Boolean.TRUE.equals(autoRenew))
                .locked(false)
                .createdBy(getSystemAdminOrNull())
                .build();
    }

    private Account resolveSettlementAccount(Long settlementAccountId, Account fallback) {
        if (settlementAccountId == null) {
            return fallback;
        }
        return accountRepository.findById(settlementAccountId)
            .orElseThrow(() -> new EntityNotFoundException("Settlement account not found"));
    }

    private AdminUser getSystemAdminOrNull() {
        return adminUserRepository.findByUsernameIgnoreCase("system")
                .or(() -> adminUserRepository.findByUsernameIgnoreCase("admin@gmail.com"))
                .or(() -> adminUserRepository.findFirstByStatusIgnoreCaseOrderByIdAsc("active"))
                .or(() -> adminUserRepository.findFirstByOrderByIdAsc())
                .orElse(null);
    }

    private String generateCode() {
        return "SAV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static String generateTransactionCode() {
        long now = System.currentTimeMillis();
        int r = (int) (Math.random() * 900_000) + 100_000;
        return "TX" + now + r;
    }

    private static Long extractSavingId(String description) {
        if (description == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Missing savingId");
        }
        Matcher m = SAVING_ID_PATTERN.matcher(description);
        if (!m.find()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Missing savingId");
        }
        return Long.valueOf(m.group(1));
    }

    private String normalizeStatus(String status) {
        return status == null ? "" : status.trim().toLowerCase();
    }

    private boolean isAwaitingApproval(String status) {
        return "pending_approval".equals(status) || "pending_contract".equals(status);
    }

    private void debitSourceAccountForApproval(Saving saving) {
        Account source = accountRepository.findByIdForUpdate(saving.getSourceAccount().getId())
            .orElseThrow(() -> new EntityNotFoundException("Source account not found"));
        BigDecimal amount = saving.getPrincipalAmount();
        validateSufficientBalance(source, amount);
        BigDecimal before = source.getAvailableBalance();
        source.setAvailableBalance(before.subtract(amount));
        source.setCurrentBalance(source.getCurrentBalance().subtract(amount));
        accountRepository.save(source);

        ledgerRepository.save(AccountBalanceLedger.builder()
            .account(source)
            .transaction(null)
            .entryType("debit")
            .amount(amount)
            .balanceBefore(before)
            .balanceAfter(source.getAvailableBalance())
            .build());
    }

    private void refundSourceAccountOnReject(Saving saving) {
        Account source = accountRepository.findByIdForUpdate(saving.getSourceAccount().getId())
            .orElseThrow(() -> new EntityNotFoundException("Source account not found"));
        BigDecimal amount = saving.getPrincipalAmount();
        BigDecimal before = source.getAvailableBalance();

        source.setAvailableBalance(before.add(amount));
        source.setCurrentBalance(source.getCurrentBalance().add(amount));
        accountRepository.save(source);

        Transaction refundTx = transactionRepository.save(Transaction.builder()
            .transactionCode(generateTransactionCode())
            .fromAccount(null)
            .toAccount(source)
            .transactionType("saving_reject_refund")
            .amount(amount)
            .feeAmount(BigDecimal.ZERO)
            .description("savingId=" + saving.getId())
            .status("completed")
            .initiatedByUser(saving.getUser())
            .build());
        refundTx.setCompletedAt(Instant.now());
        transactionRepository.save(refundTx);

        ledgerRepository.save(AccountBalanceLedger.builder()
            .account(source)
            .transaction(refundTx)
            .entryType("credit")
            .amount(amount)
            .balanceBefore(before)
            .balanceAfter(source.getAvailableBalance())
            .build());
    }

    private Instant calculateMaturityDate(Instant openDate, String termUnit, int termValue) {
        if (openDate == null) {
            return null;
        }
        ZonedDateTime base = openDate.atZone(ZoneId.systemDefault());
        ZonedDateTime maturity = "MONTH".equalsIgnoreCase(termUnit)
            ? base.plusMonths(termValue)
            : base.plusYears(termValue);
        return maturity.toInstant();
    }
}
