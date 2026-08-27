package com.minibank.backend.loan.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.minibank.backend.account.entity.Account;
import com.minibank.backend.account.repository.AccountRepository;
import com.minibank.backend.common.security.CurrentJwt;
import com.minibank.backend.contract.entity.Contract;
import com.minibank.backend.contract.repository.ContractRepository;
import com.minibank.backend.loan.dto.MobileContractResponse;
import com.minibank.backend.loan.entity.Loan;
import com.minibank.backend.loan.entity.LoanRepaymentSchedule;
import com.minibank.backend.loan.repository.LoanApplicationRepository;
import com.minibank.backend.loan.repository.LoanRepaymentScheduleRepository;
import com.minibank.backend.loan.repository.LoanRepository;
import com.minibank.backend.saving.entity.Saving;
import com.minibank.backend.saving.repository.SavingRepository;
import com.minibank.backend.transaction.entity.Transaction;
import com.minibank.backend.transaction.repository.TransactionRepository;

import jakarta.transaction.Transactional;

@RestController
@RequestMapping("/api/mobile/contracts")
public class MobileContractController {
    private final ContractRepository contractRepository;
    private final LoanApplicationRepository loanApplicationRepository;
    private final SavingRepository savingRepository;
    private final LoanRepository loanRepository;
    private final LoanRepaymentScheduleRepository repaymentScheduleRepository;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    public MobileContractController(
        ContractRepository contractRepository,
        LoanApplicationRepository loanApplicationRepository,
        SavingRepository savingRepository,
        LoanRepository loanRepository,
        LoanRepaymentScheduleRepository repaymentScheduleRepository,
        TransactionRepository transactionRepository,
        AccountRepository accountRepository
    ) {
        this.contractRepository = contractRepository;
        this.loanApplicationRepository = loanApplicationRepository;
        this.savingRepository = savingRepository;
        this.loanRepository = loanRepository;
        this.repaymentScheduleRepository = repaymentScheduleRepository;
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
    }

    @GetMapping
    public List<MobileContractResponse> list() {
        long userId = CurrentJwt.requireUserId();
        List<MobileContractResponse> result = new ArrayList<>();

        loanApplicationRepository.findByUserId(userId).forEach(app ->
            contractRepository.findByOwnerTypeAndOwnerIdOrderByCreatedAtDesc("loan_application", app.getId())
                .forEach(c -> result.add(toDto(c))));

        savingRepository.findByUserId(userId).forEach(s ->
            contractRepository.findByOwnerTypeAndOwnerIdOrderByCreatedAtDesc("saving", s.getId())
                .forEach(c -> result.add(toDto(c))));

        return result;
    }

    @GetMapping("/{id}")
    public MobileContractResponse detail(@PathVariable Long id) {
        long userId = CurrentJwt.requireUserId();
        Contract c = contractRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hợp đồng"));

        assertOwnerCanAccess(c, userId);
        return toDto(c);
    }

    @Transactional
    @PostMapping("/{id}/sign")
    public MobileContractResponse sign(@PathVariable Long id) {
        long userId = CurrentJwt.requireUserId();
        Contract c = contractRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hợp đồng"));

        assertOwnerCanAccess(c, userId);

        if ("SIGNED".equalsIgnoreCase(c.getStatus())) {
            return toDto(c);
        }

        String status = c.getStatus() == null ? "" : c.getStatus().toLowerCase();
        if (!(status.equals("pending_signature") || status.equals("sent") || status.equals("pending") || status.equals("draft"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hợp đồng không ở trạng thái chờ ký");
        }

        if ("saving".equalsIgnoreCase(c.getOwnerType())) {
            finalizeSavingContract(c, userId);
        } else if ("loan_application".equalsIgnoreCase(c.getOwnerType())) {
            finalizeLoanContract(c, userId);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Loại chủ sở hữu hợp đồng không hợp lệ");
        }

        c.setStatus("SIGNED");
        c.setSignedAt(Instant.now());
        contractRepository.save(c);

        return toDto(c);
    }

    private void finalizeSavingContract(Contract c, long userId) {
        Saving s = savingRepository.findById(c.getOwnerId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sổ tiết kiệm"));

        if (!s.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không có quyền ký hợp đồng này");
        }

        if (!"pending_contract".equalsIgnoreCase(s.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sổ tiết kiệm không ở trạng thái chờ ký");
        }

        Account from = accountRepository.findById(s.getSourceAccount().getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản nguồn"));

        BigDecimal amount = s.getPrincipalAmount();
        from.setAvailableBalance(from.getAvailableBalance().subtract(amount));
        from.setCurrentBalance(from.getCurrentBalance().subtract(amount));
        accountRepository.save(from);

        Transaction tx = Transaction.builder()
            .transactionCode("TXN-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase())
            .transactionType("saving_open")
            .amount(amount)
            .feeAmount(BigDecimal.ZERO)
            .status("completed")
            .fromAccount(from)
            .toAccount(s.getSettlementAccount() != null ? s.getSettlementAccount() : from)
            .initiatedByUser(s.getUser())
            .build();
        transactionRepository.save(tx);

        Instant now = Instant.now();
        s.setStatus("active");
        s.setOpenDate(now);
        if (s.getMaturityDate() == null) {
            ZonedDateTime base = now.atZone(ZoneId.systemDefault());
            ZonedDateTime maturity = "MONTH".equalsIgnoreCase(s.getTermUnit())
                ? base.plusMonths(s.getTermValue())
                : base.plusYears(s.getTermValue());
            s.setMaturityDate(maturity.toInstant());
        }
        savingRepository.save(s);
    }

    private void finalizeLoanContract(Contract c, long userId) {
        var app = loanApplicationRepository.findById(c.getOwnerId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hồ sơ vay"));

        if (!app.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không có quyền ký hợp đồng này");
        }

        if (!"approved".equalsIgnoreCase(app.getStatus())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Hồ sơ vay chưa được duyệt, chưa thể ký hợp đồng"
            );
        }

        if (loanRepository.existsByLoanApplicationId(app.getId())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Khoản vay đã được tạo từ hồ sơ này"
            );
        }

        LocalDate firstDueDate = LocalDate.now().plusMonths(1);
        Instant now = Instant.now();

        Loan loan = Loan.builder()
            .code("LN-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase())
            .loanApplication(app)
            .user(app.getUser())
            .approvedAmount(app.getRequestedAmount())
            .disbursedAmount(app.getRequestedAmount())
            .loanProduct(app.getLoanProduct())
            .interestRateType(app.getLoanProduct() != null ? app.getLoanProduct().getInterestRateType() : "FIXED")
            .actualInterestRate(app.getLoanProduct() != null ? app.getLoanProduct().getBaseInterestRate() : new BigDecimal("0.11"))
            .interestCalculationMethod(app.getLoanProduct() != null ? app.getLoanProduct().getInterestCalculationMethod() : "REDUCING_BALANCE")
            .repaymentFrequency(app.getLoanProduct() != null ? app.getLoanProduct().getRepaymentFrequency() : "MONTHLY")
            .termMonths(app.getRequestedTermMonths())
            .outstandingPrincipal(app.getRequestedAmount())
            .outstandingInterest(BigDecimal.ZERO)
            .overduePrincipal(BigDecimal.ZERO)
            .overdueInterest(BigDecimal.ZERO)
            .status("active")
            .disbursementAccount(app.getDisbursementAccount())
            .repaymentAccount(app.getRepaymentAccount())
            .disbursedAt(now)
            .nextDueDate(firstDueDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
            .build();

        loan = loanRepository.save(loan);
        createRepaymentSchedule(loan, firstDueDate);
        disburseLoan(loan);
    }

    private void createRepaymentSchedule(Loan loan, LocalDate firstDueDate) {
        BigDecimal principal = loan.getApprovedAmount();
        int months = loan.getTermMonths();
        if (months <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kỳ hạn vay không hợp lệ");
        }

        BigDecimal monthlyPrincipal = principal.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
        BigDecimal annualRate = normalizeAnnualRateFraction(loan.getActualInterestRate());
        BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(12), 8, RoundingMode.HALF_UP);
        BigDecimal remaining = principal;
        LocalDate due = firstDueDate;

        for (int i = 1; i <= months; i++) {
            BigDecimal interest = remaining.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal principalDue = i == months ? remaining : monthlyPrincipal;
            BigDecimal totalDue = principalDue.add(interest);

            LoanRepaymentSchedule schedule = LoanRepaymentSchedule.builder()
                .loan(loan)
                .installmentNo(i)
                .dueDate(due)
                .openingPrincipalBalance(remaining)
                .principalDue(principalDue)
                .interestRate(loan.getActualInterestRate())
                .interestDue(interest)
                .penaltyInterestDue(BigDecimal.ZERO)
                .feeDue(BigDecimal.ZERO)
                .totalDue(totalDue)
                .principalPaid(BigDecimal.ZERO)
                .interestPaid(BigDecimal.ZERO)
                .penaltyInterestPaid(BigDecimal.ZERO)
                .feePaid(BigDecimal.ZERO)
                .status("unpaid")
                .build();

            repaymentScheduleRepository.save(schedule);

            remaining = remaining.subtract(principalDue);
            due = due.plusMonths(1);
        }
    }

    private void disburseLoan(Loan loan) {
        Account to = null;
        if (loan.getDisbursementAccount() != null) {
            to = loan.getDisbursementAccount();
            to.setAvailableBalance(to.getAvailableBalance().add(loan.getDisbursedAmount()));
            to.setCurrentBalance(to.getCurrentBalance().add(loan.getDisbursedAmount()));
            accountRepository.save(to);
        }

        Transaction tx = Transaction.builder()
            .transactionCode("TXN-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase())
            .transactionType("loan_disbursement")
            .amount(loan.getDisbursedAmount())
            .feeAmount(BigDecimal.ZERO)
            .status("completed")
            .fromAccount(null)
            .toAccount(to)
            .initiatedByUser(loan.getUser())
            .build();
        transactionRepository.save(tx);
    }

    private void assertOwnerCanAccess(Contract c, long userId) {
        if ("loan_application".equalsIgnoreCase(c.getOwnerType())) {
            var app = loanApplicationRepository.findById(c.getOwnerId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hồ sơ vay"));
            if (!app.getUser().getId().equals(userId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không có quyền truy cập hợp đồng này");
            }
            return;
        }

        if ("saving".equalsIgnoreCase(c.getOwnerType())) {
            Saving s = savingRepository.findById(c.getOwnerId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sổ tiết kiệm"));
            if (!s.getUser().getId().equals(userId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không có quyền truy cập hợp đồng này");
            }
            return;
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Loại chủ sở hữu hợp đồng không hợp lệ");
    }

    private BigDecimal normalizeAnnualRateFraction(BigDecimal rate) {
        if (rate == null) {
            return new BigDecimal("0.11");
        }
        if (rate.compareTo(BigDecimal.ONE) > 0) {
            return rate.divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);
        }
        return rate;
    }

    private MobileContractResponse toDto(Contract c) {
        return new MobileContractResponse(
            c.getId(),
            c.getOwnerType(),
            c.getOwnerId(),
            c.getContractNumber(),
            c.getStatus(),
            c.getSignedAt(),
            c.getCreatedAt(),
            c.getFileUrl(),
            c.getRenderedBody()
        );
    }
}
