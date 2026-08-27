package com.minibank.backend.contract.service;

import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.minibank.backend.account.entity.Account;
import com.minibank.backend.account.repository.AccountRepository;
import com.minibank.backend.loan.entity.Loan;
import com.minibank.backend.loan.entity.LoanApplication;
import com.minibank.backend.loan.repository.LoanApplicationRepository;
import com.minibank.backend.loan.repository.LoanRepository;
import com.minibank.backend.saving.entity.Saving;
import com.minibank.backend.saving.repository.SavingRepository;
import com.minibank.backend.user.entity.User;
import com.minibank.backend.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Thu thập dữ liệu thực từ DB và trả về Map<fieldCode, value>
 * dùng để điền vào template {{placeholder}}.
 */
@Component
@RequiredArgsConstructor
public class ContractDataResolver {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final LoanRepository loanRepository;
    private final LoanApplicationRepository loanApplicationRepository;
    private final SavingRepository savingRepository;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));
    private static final DateTimeFormatter DATETIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));
    private static final Locale VN = new Locale("vi", "VN");

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Giải quyết dữ liệu cho ownerType = "USER"
     * (dùng khi admin sinh hợp đồng trực tiếp cho user)
     */
    public Map<String, String> resolveForUser(Long userId) {
        Map<String, String> data = systemData();
        userRepository.findById(userId).ifPresent(u -> data.putAll(fromUser(u)));
        // tài khoản thanh toán đầu tiên
        accountRepository.findFirstByUserId(userId).ifPresent(a -> data.putAll(fromAccount(a)));
        return data;
    }

    /**
     * Giải quyết dữ liệu cho ownerType = "loan_application"
     */
    public Map<String, String> resolveForLoanApplication(Long loanAppId) {
        Map<String, String> data = systemData();
        loanApplicationRepository.findById(loanAppId).ifPresent(app -> {
            data.putAll(fromLoanApplication(app));
            userRepository.findById(app.getUser().getId()).ifPresent(u -> data.putAll(fromUser(u)));
            accountRepository.findFirstByUserId(app.getUser().getId())
                    .ifPresent(a -> data.putAll(fromAccount(a)));
            // nếu đã được duyệt, lấy thêm từ loan
            loanRepository.findByLoanApplicationId(loanAppId)
                    .ifPresent(loan -> data.putAll(fromLoan(loan)));
        });
        return data;
    }

    /**
     * Giải quyết dữ liệu cho ownerType = "saving"
     */
    public Map<String, String> resolveForSaving(Long savingId) {
        Map<String, String> data = systemData();
        savingRepository.findById(savingId).ifPresent(sv -> {
            data.putAll(fromSaving(sv));
            userRepository.findById(sv.getUser().getId()).ifPresent(u -> data.putAll(fromUser(u)));
            accountRepository.findFirstByUserId(sv.getUser().getId())
                    .ifPresent(a -> data.putAll(fromAccount(a)));
        });
        return data;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Map<String, String> systemData() {
        Map<String, String> m = new HashMap<>();
        m.put("bank_name", "MiniBank");
        m.put("branch_name", "Chi nhánh Hà Nội");
        m.put("sign_date", DATE_FMT.format(Instant.now()));
        m.put("today", DATE_FMT.format(Instant.now()));
        m.put("contract_date", DATE_FMT.format(Instant.now()));
        return m;
    }

    private Map<String, String> fromUser(User u) {
        Map<String, String> m = new HashMap<>();
        m.put("full_name", nvl(u.getFullName()));
        m.put("customer_name", nvl(u.getFullName()));
        m.put("phone", nvl(u.getPhone()));
        m.put("customer_phone", nvl(u.getPhone()));
        m.put("email", nvl(u.getEmail()));
        m.put("citizen_id", nvl(u.getCitizenId()));
        m.put("customer_citizen_id", nvl(u.getCitizenId()));
        m.put("address", nvl(u.getAddress()));
        m.put("customer_address", nvl(u.getAddress()));
        m.put("dob", u.getDob() != null ? u.getDob().toString() : "");
        m.put("customer_dob", u.getDob() != null ? u.getDob().toString() : "");
        m.put("customer_rank", nvl(u.getCustomerRank()));
        return m;
    }

    private Map<String, String> fromAccount(Account a) {
        Map<String, String> m = new HashMap<>();
        m.put("account_number", nvl(a.getAccountNumber()));
        m.put("available_balance", formatMoney(a.getAvailableBalance()));
        m.put("daily_transfer_limit", formatMoney(a.getDailyTransferLimit()));
        return m;
    }

    private Map<String, String> fromLoanApplication(LoanApplication app) {
        Map<String, String> m = new HashMap<>();
        m.put("loan_amount", formatMoney(app.getRequestedAmount()));
        m.put("loan_term_months", String.valueOf(app.getRequestedTermMonths()));
        m.put("loan_purpose", nvl(app.getPurpose()));
        m.put("collateral", nvl(app.getCollateralDescription()));
        m.put("collateral_desc", nvl(app.getCollateralDescription()));
        return m;
    }

    private Map<String, String> fromLoan(Loan loan) {
        Map<String, String> m = new HashMap<>();
        m.put("loan_amount", formatMoney(loan.getApprovedAmount()));
        m.put("loan_term_months", String.valueOf(loan.getTermMonths()));
        m.put("interest_rate", loan.getActualInterestRate().toPlainString());
        m.put("loan_interest_rate", loan.getActualInterestRate().toPlainString());
        m.put("monthly_payment", formatMoney(
                loan.getApprovedAmount()
                    .divide(java.math.BigDecimal.valueOf(loan.getTermMonths()), 0,
                            java.math.RoundingMode.HALF_UP)));
        m.put("loan_monthly_payment", m.get("monthly_payment"));
        if (loan.getDisbursedAt() != null) {
            m.put("disburse_date", DATE_FMT.format(loan.getDisbursedAt()));
        }
        if (loan.getNextDueDate() != null) {
            m.put("next_due_date", DATETIME_FMT.format(loan.getNextDueDate()));
        }
        return m;
    }

    private Map<String, String> fromSaving(Saving sv) {
        Map<String, String> m = new HashMap<>();
        m.put("saving_principal", formatMoney(sv.getPrincipalAmount()));
        m.put("saving_amount", formatMoney(sv.getPrincipalAmount()));
        m.put("saving_term", sv.getTermValue() + " " + sv.getTermUnit());
        m.put("saving_term_months", sv.getTermUnit() != null && sv.getTermUnit().equalsIgnoreCase("YEAR")
                ? String.valueOf(sv.getTermValue() * 12)
                : String.valueOf(sv.getTermValue()));
        m.put("saving_interest", sv.getActualInterestRate().toPlainString());
        m.put("saving_interest_rate", sv.getActualInterestRate().toPlainString());
        if (sv.getMaturityDate() != null) {
            m.put("maturity_date", DATE_FMT.format(sv.getMaturityDate()));
            m.put("saving_maturity_date", DATE_FMT.format(sv.getMaturityDate()));
        }
        if (sv.getOpenDate() != null) {
            m.put("open_date", DATE_FMT.format(sv.getOpenDate()));
            m.put("saving_open_date", DATE_FMT.format(sv.getOpenDate()));
        }
        m.put("auto_renew", sv.isAutoRenew() ? "Có" : "Không");
        return m;
    }

    private String formatMoney(java.math.BigDecimal amount) {
        if (amount == null) return "";
        return NumberFormat.getNumberInstance(VN).format(amount);
    }

    private String nvl(String s) {
        return s == null ? "" : s;
    }
}
