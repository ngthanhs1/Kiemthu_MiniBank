package com.minibank.backend.loan.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.minibank.backend.loan.dto.LoanProductResponse;
import com.minibank.backend.loan.repository.LoanProductRepository;

@RestController
@RequestMapping("/api/mobile/loan-products")
public class MobileLoanProductController {
    private final LoanProductRepository loanProductRepository;

    public MobileLoanProductController(LoanProductRepository loanProductRepository) {
        this.loanProductRepository = loanProductRepository;
    }

    @GetMapping
    public List<LoanProductResponse> list(@RequestParam(defaultValue = "active") String status) {
        return loanProductRepository.findByStatusIgnoreCaseOrderByBaseInterestRateDesc(status)
            .stream()
            .map(p -> new LoanProductResponse(
                p.getId(),
                p.getCode(),
                p.getName(),
                p.getLoanType(),
                p.getCurrency(),
                p.getMinAmount(),
                p.getMaxAmount(),
                p.getMinTermMonths(),
                p.getMaxTermMonths(),
                p.getInterestRateType(),
                p.getBaseInterestRate(),
                p.getPenaltyInterestRate(),
                p.getInterestCalculationMethod(),
                p.getRepaymentFrequency(),
                p.getStatus()
            ))
            .toList();
    }
}
