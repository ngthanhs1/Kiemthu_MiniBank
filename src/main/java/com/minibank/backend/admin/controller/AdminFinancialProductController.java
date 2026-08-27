package com.minibank.backend.admin.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.minibank.backend.loan.entity.LoanProduct;
import com.minibank.backend.loan.entity.LoanProductInterestTier;
import com.minibank.backend.loan.repository.LoanProductInterestTierRepository;
import com.minibank.backend.loan.repository.LoanProductRepository;
import com.minibank.backend.saving.entity.SavingProduct;
import com.minibank.backend.saving.entity.SavingProductInterestTier;
import com.minibank.backend.saving.repository.SavingProductInterestTierRepository;
import com.minibank.backend.saving.repository.SavingProductRepository;

@RestController
@RequestMapping("/api/admin/financial-products")
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public class AdminFinancialProductController {
    private final SavingProductRepository savingProductRepository;
    private final SavingProductInterestTierRepository savingTierRepository;
    private final LoanProductRepository loanProductRepository;
    private final LoanProductInterestTierRepository loanTierRepository;

    public AdminFinancialProductController(
        SavingProductRepository savingProductRepository,
        SavingProductInterestTierRepository savingTierRepository,
        LoanProductRepository loanProductRepository,
        LoanProductInterestTierRepository loanTierRepository
    ) {
        this.savingProductRepository = savingProductRepository;
        this.savingTierRepository = savingTierRepository;
        this.loanProductRepository = loanProductRepository;
        this.loanTierRepository = loanTierRepository;
    }

    @GetMapping("/saving-products")
    public List<SavingProductItem> listSavingProducts() {
        return savingProductRepository.findAllByOrderByUpdatedAtDesc().stream().map(this::toSavingProductItem).toList();
    }

    @PostMapping("/saving-products")
    @ResponseStatus(HttpStatus.CREATED)
    public SavingProductItem createSavingProduct(@RequestBody SavingProductUpsertRequest req) {
        SavingProduct entity = new SavingProduct();
        applySavingProduct(entity, req);
        return toSavingProductItem(savingProductRepository.save(entity));
    }

    @PutMapping("/saving-products/{id}")
    public SavingProductItem updateSavingProduct(@PathVariable Long id, @RequestBody SavingProductUpsertRequest req) {
        SavingProduct entity = savingProductRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Saving product not found"));
        applySavingProduct(entity, req);
        return toSavingProductItem(savingProductRepository.save(entity));
    }

    @PatchMapping("/saving-products/{id}/status")
    public SavingProductItem updateSavingProductStatus(@PathVariable Long id, @RequestBody StatusRequest req) {
        SavingProduct entity = savingProductRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Saving product not found"));
        if (req.status == null || req.status.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status is required");
        }
        entity.setStatus(req.status.trim().toLowerCase());
        return toSavingProductItem(savingProductRepository.save(entity));
    }

    @GetMapping("/saving-interest-tiers")
    @Transactional(readOnly = true)
    public List<SavingTierItem> listSavingTiers(@RequestParam Long savingProductId) {
        return savingTierRepository.findBySavingProductIdOrderByEffectiveFromDescMinAmountAsc(savingProductId)
            .stream()
            .map(this::toSavingTierItem)
            .toList();
    }

    @PostMapping("/saving-interest-tiers")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public SavingTierItem createSavingTier(@RequestBody SavingTierUpsertRequest req) {
        SavingProduct product = savingProductRepository.findById(req.savingProductId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Saving product not found"));
        SavingProductInterestTier tier = new SavingProductInterestTier();
        tier.setSavingProduct(product);
        applySavingTier(tier, req);
        return toSavingTierItem(savingTierRepository.save(tier));
    }

    @PutMapping("/saving-interest-tiers/{id}")
    @Transactional
    public SavingTierItem updateSavingTier(@PathVariable Long id, @RequestBody SavingTierUpsertRequest req) {
        SavingProductInterestTier tier = savingTierRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Saving tier not found"));
        if (!tier.getSavingProduct().getId().equals(req.savingProductId)) {
            SavingProduct product = savingProductRepository.findById(req.savingProductId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Saving product not found"));
            tier.setSavingProduct(product);
        }
        applySavingTier(tier, req);
        return toSavingTierItem(savingTierRepository.save(tier));
    }

    @DeleteMapping("/saving-interest-tiers/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSavingTier(@PathVariable Long id) {
        savingTierRepository.deleteById(id);
    }

    @GetMapping("/loan-products")
    public List<LoanProductItem> listLoanProducts() {
        return loanProductRepository.findAllByOrderByUpdatedAtDesc().stream().map(this::toLoanProductItem).toList();
    }

    @PostMapping("/loan-products")
    @ResponseStatus(HttpStatus.CREATED)
    public LoanProductItem createLoanProduct(@RequestBody LoanProductUpsertRequest req) {
        if (req.code == null || req.code.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "code is required");
        }
        if (loanProductRepository.existsByCodeIgnoreCase(req.code.trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Loan product code already exists");
        }
        LoanProduct entity = new LoanProduct();
        applyLoanProduct(entity, req);
        return toLoanProductItem(loanProductRepository.save(entity));
    }

    @PutMapping("/loan-products/{id}")
    public LoanProductItem updateLoanProduct(@PathVariable Long id, @RequestBody LoanProductUpsertRequest req) {
        LoanProduct entity = loanProductRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan product not found"));
        applyLoanProduct(entity, req);
        return toLoanProductItem(loanProductRepository.save(entity));
    }

    @PatchMapping("/loan-products/{id}/status")
    public LoanProductItem updateLoanProductStatus(@PathVariable Long id, @RequestBody StatusRequest req) {
        LoanProduct entity = loanProductRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan product not found"));
        if (req.status == null || req.status.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status is required");
        }
        entity.setStatus(req.status.trim().toLowerCase());
        return toLoanProductItem(loanProductRepository.save(entity));
    }

    @GetMapping("/loan-interest-tiers")
    @Transactional(readOnly = true)
    public List<LoanTierItem> listLoanTiers(@RequestParam Long loanProductId) {
        return loanTierRepository.findByLoanProductIdOrderByEffectiveFromDescMinAmountAsc(loanProductId)
            .stream()
            .map(this::toLoanTierItem)
            .toList();
    }

    @PostMapping("/loan-interest-tiers")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public LoanTierItem createLoanTier(@RequestBody LoanTierUpsertRequest req) {
        LoanProduct product = loanProductRepository.findById(req.loanProductId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Loan product not found"));
        LoanProductInterestTier tier = new LoanProductInterestTier();
        tier.setLoanProduct(product);
        applyLoanTier(tier, req);
        return toLoanTierItem(loanTierRepository.save(tier));
    }

    @PutMapping("/loan-interest-tiers/{id}")
    @Transactional
    public LoanTierItem updateLoanTier(@PathVariable Long id, @RequestBody LoanTierUpsertRequest req) {
        LoanProductInterestTier tier = loanTierRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan tier not found"));
        if (!tier.getLoanProduct().getId().equals(req.loanProductId)) {
            LoanProduct product = loanProductRepository.findById(req.loanProductId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Loan product not found"));
            tier.setLoanProduct(product);
        }
        applyLoanTier(tier, req);
        return toLoanTierItem(loanTierRepository.save(tier));
    }

    @DeleteMapping("/loan-interest-tiers/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLoanTier(@PathVariable Long id) {
        loanTierRepository.deleteById(id);
    }

    @Transactional
    protected void applySavingProduct(SavingProduct target, SavingProductUpsertRequest req) {
        target.setCode(requireText(req.code, "code").toUpperCase());
        target.setName(requireText(req.name, "name"));
        target.setCurrency(defaultText(req.currency, "VND").toUpperCase());
        target.setTermUnit(defaultText(req.termUnit, "MONTH").toUpperCase());
        target.setTermValue(requirePositiveInt(req.termValue, "termValue"));
        target.setInterestRateType(defaultText(req.interestRateType, "FIXED").toUpperCase());
        target.setBaseInterestRate(requireRate(req.baseInterestRate, "baseInterestRate"));
        target.setPenaltyInterestRate(req.penaltyInterestRate);
        target.setBonusInterestRate(req.bonusInterestRate);
        target.setInterestAccrualFrequency(defaultText(req.interestAccrualFrequency, "DAILY").toUpperCase());
        target.setInterestPostingFrequency(defaultText(req.interestPostingFrequency, "END_OF_TERM").toUpperCase());
        target.setCapitalized(req.capitalized == null || req.capitalized);
        target.setMinOpenAmount(requireMoney(req.minOpenAmount, "minOpenAmount"));
        target.setMaxOpenAmount(req.maxOpenAmount);
        target.setDepositFeeRate(nullToZero(req.depositFeeRate));
        target.setDepositFeeFlat(nullToZero(req.depositFeeFlat));
        target.setWithdrawalFeeRate(nullToZero(req.withdrawalFeeRate));
        target.setWithdrawalFeeFlat(nullToZero(req.withdrawalFeeFlat));
        target.setCloseFeeRate(nullToZero(req.closeFeeRate));
        target.setCloseFeeFlat(nullToZero(req.closeFeeFlat));
        target.setManagementFeeRate(req.managementFeeRate);
        target.setManagementFeeFlat(req.managementFeeFlat);
        target.setManagementFeeFrequency(req.managementFeeFrequency);
        target.setStatus(defaultText(req.status, "active").toLowerCase());
    }

    protected void applyLoanProduct(LoanProduct target, LoanProductUpsertRequest req) {
        target.setCode(requireText(req.code, "code").toUpperCase());
        target.setName(requireText(req.name, "name"));
        target.setLoanType(defaultText(req.loanType, "PERSONAL").toUpperCase());
        target.setCurrency(defaultText(req.currency, "VND").toUpperCase());
        target.setMinAmount(requireMoney(req.minAmount, "minAmount"));
        target.setMaxAmount(requireMoney(req.maxAmount, "maxAmount"));
        target.setMinTermMonths(requirePositiveInt(req.minTermMonths, "minTermMonths"));
        target.setMaxTermMonths(requirePositiveInt(req.maxTermMonths, "maxTermMonths"));
        target.setInterestRateType(defaultText(req.interestRateType, "FIXED").toUpperCase());
        target.setBaseInterestRate(requireRate(req.baseInterestRate, "baseInterestRate"));
        target.setPenaltyInterestRate(req.penaltyInterestRate);
        target.setGraceInterestRate(req.graceInterestRate);
        target.setProcessingFeeRate(nullToZero(req.processingFeeRate));
        target.setProcessingFeeFlat(nullToZero(req.processingFeeFlat));
        target.setEarlyRepaymentFeeRate(nullToZero(req.earlyRepaymentFeeRate));
        target.setEarlyRepaymentFeeFlat(nullToZero(req.earlyRepaymentFeeFlat));
        target.setInterestCalculationMethod(defaultText(req.interestCalculationMethod, "REDUCING_BALANCE").toUpperCase());
        target.setRepaymentFrequency(defaultText(req.repaymentFrequency, "MONTHLY").toUpperCase());
        target.setStatus(defaultText(req.status, "active").toLowerCase());
    }

    protected void applySavingTier(SavingProductInterestTier target, SavingTierUpsertRequest req) {
        target.setMinAmount(requireMoney(req.minAmount, "minAmount"));
        target.setMaxAmount(req.maxAmount);
        target.setInterestRate(requireRate(req.interestRate, "interestRate"));
        target.setEffectiveFrom(req.effectiveFrom != null ? req.effectiveFrom : LocalDate.now());
        target.setEffectiveTo(req.effectiveTo);
    }

    protected void applyLoanTier(LoanProductInterestTier target, LoanTierUpsertRequest req) {
        target.setMinAmount(requireMoney(req.minAmount, "minAmount"));
        target.setMaxAmount(req.maxAmount);
        target.setMinTermMonths(req.minTermMonths);
        target.setMaxTermMonths(req.maxTermMonths);
        target.setInterestRate(requireRate(req.interestRate, "interestRate"));
        target.setEffectiveFrom(req.effectiveFrom != null ? req.effectiveFrom : LocalDate.now());
        target.setEffectiveTo(req.effectiveTo);
    }

    private SavingProductItem toSavingProductItem(SavingProduct p) {
        return new SavingProductItem(
            p.getId(),
            p.getCode(),
            p.getName(),
            p.getCurrency(),
            p.getTermUnit(),
            p.getTermValue(),
            p.getInterestRateType(),
            p.getBaseInterestRate(),
            p.getInterestAccrualFrequency(),
            p.getInterestPostingFrequency(),
            p.getMinOpenAmount(),
            p.getMaxOpenAmount(),
            p.getCloseFeeRate(),
            p.getStatus()
        );
    }

    private LoanProductItem toLoanProductItem(LoanProduct p) {
        return new LoanProductItem(
            p.getId(),
            p.getCode(),
            p.getName(),
            p.getLoanType(),
            p.getCurrency(),
            p.getMinAmount(),
            p.getMaxAmount(),
            p.getMinTermMonths(),
            p.getMaxTermMonths(),
            p.getBaseInterestRate(),
            p.getStatus()
        );
    }

    private SavingTierItem toSavingTierItem(SavingProductInterestTier t) {
        return new SavingTierItem(
            t.getId(),
            t.getSavingProduct() != null ? t.getSavingProduct().getId() : null,
            t.getSavingProduct() != null ? t.getSavingProduct().getName() : null,
            t.getMinAmount(),
            t.getMaxAmount(),
            t.getInterestRate(),
            t.getEffectiveFrom(),
            t.getEffectiveTo()
        );
    }

    private LoanTierItem toLoanTierItem(LoanProductInterestTier t) {
        return new LoanTierItem(
            t.getId(),
            t.getLoanProduct() != null ? t.getLoanProduct().getId() : null,
            t.getLoanProduct() != null ? t.getLoanProduct().getName() : null,
            t.getMinAmount(),
            t.getMaxAmount(),
            t.getMinTermMonths(),
            t.getMaxTermMonths(),
            t.getInterestRate(),
            t.getEffectiveFrom(),
            t.getEffectiveTo()
        );
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " is required");
        }
        return value.trim();
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private int requirePositiveInt(Integer value, String field) {
        if (value == null || value <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " must be > 0");
        }
        return value;
    }

    private BigDecimal requireMoney(BigDecimal value, String field) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " must be > 0");
        }
        return value;
    }

    private BigDecimal requireRate(BigDecimal value, String field) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " is invalid");
        }
        return value;
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    public static class StatusRequest {
        public String status;
    }

    public static class SavingProductUpsertRequest {
        public String code;
        public String name;
        public String currency;
        public String termUnit;
        public Integer termValue;
        public String interestRateType;
        public BigDecimal baseInterestRate;
        public BigDecimal penaltyInterestRate;
        public BigDecimal bonusInterestRate;
        public String interestAccrualFrequency;
        public String interestPostingFrequency;
        public Boolean capitalized;
        public BigDecimal minOpenAmount;
        public BigDecimal maxOpenAmount;
        public BigDecimal depositFeeRate;
        public BigDecimal depositFeeFlat;
        public BigDecimal withdrawalFeeRate;
        public BigDecimal withdrawalFeeFlat;
        public BigDecimal closeFeeRate;
        public BigDecimal closeFeeFlat;
        public BigDecimal managementFeeRate;
        public BigDecimal managementFeeFlat;
        public String managementFeeFrequency;
        public String status;
    }

    public static class LoanProductUpsertRequest {
        public String code;
        public String name;
        public String loanType;
        public String currency;
        public BigDecimal minAmount;
        public BigDecimal maxAmount;
        public Integer minTermMonths;
        public Integer maxTermMonths;
        public String interestRateType;
        public BigDecimal baseInterestRate;
        public BigDecimal penaltyInterestRate;
        public BigDecimal graceInterestRate;
        public BigDecimal processingFeeRate;
        public BigDecimal processingFeeFlat;
        public BigDecimal earlyRepaymentFeeRate;
        public BigDecimal earlyRepaymentFeeFlat;
        public String interestCalculationMethod;
        public String repaymentFrequency;
        public String status;
    }

    public static class SavingTierUpsertRequest {
        public Long savingProductId;
        public BigDecimal minAmount;
        public BigDecimal maxAmount;
        public BigDecimal interestRate;
        public LocalDate effectiveFrom;
        public LocalDate effectiveTo;
    }

    public static class LoanTierUpsertRequest {
        public Long loanProductId;
        public BigDecimal minAmount;
        public BigDecimal maxAmount;
        public Integer minTermMonths;
        public Integer maxTermMonths;
        public BigDecimal interestRate;
        public LocalDate effectiveFrom;
        public LocalDate effectiveTo;
    }

    public record SavingProductItem(
        Long id,
        String code,
        String name,
        String currency,
        String termUnit,
        int termValue,
        String interestRateType,
        BigDecimal baseInterestRate,
        String interestAccrualFrequency,
        String interestPostingFrequency,
        BigDecimal minOpenAmount,
        BigDecimal maxOpenAmount,
        BigDecimal closeFeeRate,
        String status
    ) {}

    public record LoanProductItem(
        Long id,
        String code,
        String name,
        String loanType,
        String currency,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        int minTermMonths,
        int maxTermMonths,
        BigDecimal baseInterestRate,
        String status
    ) {}

    public record SavingTierItem(
        Long id,
        Long savingProductId,
        String savingProductName,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        BigDecimal interestRate,
        LocalDate effectiveFrom,
        LocalDate effectiveTo
    ) {}

    public record LoanTierItem(
        Long id,
        Long loanProductId,
        String loanProductName,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        Integer minTermMonths,
        Integer maxTermMonths,
        BigDecimal interestRate,
        LocalDate effectiveFrom,
        LocalDate effectiveTo
    ) {}
}
