package com.minibank.backend.loan.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.minibank.backend.loan.entity.LoanRepaymentSchedule;

@Repository
public interface LoanRepaymentScheduleRepository extends JpaRepository<LoanRepaymentSchedule, Long> {
    List<LoanRepaymentSchedule> findByLoanIdOrderByInstallmentNo(Long loanId);
}
