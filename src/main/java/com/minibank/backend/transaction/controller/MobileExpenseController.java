package com.minibank.backend.transaction.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.minibank.backend.common.security.CurrentJwt;
import com.minibank.backend.transaction.dto.ExpenseCategoryOptionResponse;
import com.minibank.backend.transaction.dto.ExpenseClassifyRequest;
import com.minibank.backend.transaction.dto.ExpenseClassifyResponse;
import com.minibank.backend.transaction.dto.ExpenseOverviewResponse;
import com.minibank.backend.transaction.dto.ExpenseUnclassifiedTransactionResponse;
import com.minibank.backend.transaction.service.ExpenseManagementService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/mobile/expenses")
public class MobileExpenseController {
	private final ExpenseManagementService expenseManagementService;

	public MobileExpenseController(ExpenseManagementService expenseManagementService) {
		this.expenseManagementService = expenseManagementService;
	}

	@GetMapping("/overview")
	@Transactional(readOnly = true)
	public ExpenseOverviewResponse overview(@RequestParam(value = "flowType", required = false) String flowType) {
		return expenseManagementService.overview(CurrentJwt.requireUserId(), flowType);
	}

	@GetMapping("/categories")
	@Transactional(readOnly = true)
	public List<ExpenseCategoryOptionResponse> categories(@RequestParam(value = "flowType", required = false) String flowType) {
		return expenseManagementService.catalog(flowType);
	}

	@GetMapping("/unclassified")
	@Transactional(readOnly = true)
	public List<ExpenseUnclassifiedTransactionResponse> unclassified(@RequestParam(value = "flowType", required = false) String flowType) {
		return expenseManagementService.unclassified(CurrentJwt.requireUserId(), flowType);
	}

	@PostMapping("/transactions/{transactionId}/classify")
	@ResponseStatus(HttpStatus.OK)
	public ExpenseClassifyResponse classify(
		@PathVariable long transactionId,
		@Valid @RequestBody ExpenseClassifyRequest request
	) {
		return expenseManagementService.classify(CurrentJwt.requireUserId(), transactionId, request);
	}
}