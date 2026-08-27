package com.minibank.backend.loan.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.minibank.backend.common.security.CurrentJwt;
import com.minibank.backend.loan.dto.CreateLoanRequest;
import com.minibank.backend.loan.dto.LoanApplicationResponse;
import com.minibank.backend.loan.dto.LoanResponse;
import com.minibank.backend.loan.service.LoanService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/mobile/loans")
public class MobileLoanController {
    private final LoanService loanService;

    public MobileLoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    // Danh sách khoản vay đang active
    @GetMapping
    public List<LoanResponse> getLoans() {
        return loanService.getLoans(CurrentJwt.requireUserId());
    }

    @GetMapping("/{id}")
    public LoanResponse getLoan(@PathVariable Long id) {
        return loanService.getLoan(CurrentJwt.requireUserId(), id);
    }

    // Danh sách đơn xin vay (để user theo dõi trạng thái)
    @GetMapping("/applications")
    public List<LoanApplicationResponse> getApplications() {
        return loanService.getApplications(CurrentJwt.requireUserId());
    }

    // Tạo đơn xin vay — KHÔNG tạo Loan ngay
    @PostMapping("/applications")
    @ResponseStatus(HttpStatus.CREATED)
    public LoanApplicationResponse applyForLoan(@Valid @RequestBody CreateLoanRequest request) {
        return loanService.applyForLoan(CurrentJwt.requireUserId(), request);
    }
}
