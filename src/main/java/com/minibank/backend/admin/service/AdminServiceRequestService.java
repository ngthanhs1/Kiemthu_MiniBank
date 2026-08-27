package com.minibank.backend.admin.service;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minibank.backend.account.entity.Account;
import com.minibank.backend.account.entity.AccountBalanceLedger;
import com.minibank.backend.account.repository.AccountBalanceLedgerRepository;
import com.minibank.backend.account.repository.AccountRepository;
import com.minibank.backend.admin.dto.AdminServiceRequestDetail;
import com.minibank.backend.admin.dto.AdminServiceRequestSummary;
import com.minibank.backend.admin.entity.AdminUser;
import com.minibank.backend.admin.repository.AdminUserRepository;
import com.minibank.backend.loan.entity.Loan;
import com.minibank.backend.loan.entity.LoanRepaymentSchedule;
import com.minibank.backend.loan.repository.LoanRepaymentScheduleRepository;
import com.minibank.backend.loan.repository.LoanRepository;
import com.minibank.backend.saving.entity.Saving;
import com.minibank.backend.saving.repository.SavingRepository;
import com.minibank.backend.support.dto.LimitChangeRequestResponse;
import com.minibank.backend.support.entity.LimitChangeRequest;
import com.minibank.backend.support.entity.ServiceRequest;
import com.minibank.backend.support.repository.LimitChangeRequestRepository;
import com.minibank.backend.support.repository.ServiceRequestRepository;
import com.minibank.backend.system.service.NotificationService;
import com.minibank.backend.transaction.entity.Transaction;
import com.minibank.backend.transaction.repository.TransactionRepository;
import com.minibank.backend.user.entity.User;
import com.minibank.backend.user.repository.UserRepository;

@Service
public class AdminServiceRequestService {
	private final ServiceRequestRepository serviceRequestRepository;
	private final LimitChangeRequestRepository limitChangeRequestRepository;
	private final UserRepository userRepository;
	private final AccountRepository accountRepository;
	private final AccountBalanceLedgerRepository ledgerRepository;
	private final AdminUserRepository adminUserRepository;
	private final SavingRepository savingRepository;
	private final LoanRepository loanRepository;
	private final LoanRepaymentScheduleRepository loanRepaymentScheduleRepository;
	private final TransactionRepository transactionRepository;
	private final ObjectMapper objectMapper;
	private final NotificationService notificationService;

	public AdminServiceRequestService(
		ServiceRequestRepository serviceRequestRepository,
		LimitChangeRequestRepository limitChangeRequestRepository,
		UserRepository userRepository,
		AccountRepository accountRepository,
		AccountBalanceLedgerRepository ledgerRepository,
		AdminUserRepository adminUserRepository,
		SavingRepository savingRepository,
		LoanRepository loanRepository,
		LoanRepaymentScheduleRepository loanRepaymentScheduleRepository,
		TransactionRepository transactionRepository,
		ObjectMapper objectMapper,
		NotificationService notificationService
	) {
		this.serviceRequestRepository = serviceRequestRepository;
		this.limitChangeRequestRepository = limitChangeRequestRepository;
		this.userRepository = userRepository;
		this.accountRepository = accountRepository;
		this.ledgerRepository = ledgerRepository;
		this.adminUserRepository = adminUserRepository;
		this.savingRepository = savingRepository;
		this.loanRepository = loanRepository;
		this.loanRepaymentScheduleRepository = loanRepaymentScheduleRepository;
		this.transactionRepository = transactionRepository;
		this.objectMapper = objectMapper;
		this.notificationService = notificationService;
	}

	@Transactional(readOnly = true)
	public List<AdminServiceRequestSummary> list(String status, String type) {
		String statusFilter = normalize(status);
		String typeFilter = normalize(type);
		return serviceRequestRepository.findAll().stream()
			.filter(req -> statusFilter == null || statusFilter.equalsIgnoreCase(req.getStatus()))
			.filter(req -> typeFilter == null || typeFilter.equalsIgnoreCase(req.getRequestType()))
			.map(this::toSummary)
			.toList();
	}

	@Transactional(readOnly = true)
	public AdminServiceRequestDetail getDetail(long requestId) {
		ServiceRequest request = serviceRequestRepository.findById(requestId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service request not found"));
		LimitChangeRequestResponse limitChange = limitChangeRequestRepository.findByServiceRequestId(request.getId())
			.map(this::toLimitChangeResponse)
			.orElse(null);
		User user = request.getUser();
		return new AdminServiceRequestDetail(
			request.getId(),
			request.getRequestType(),
			request.getTitle(),
			request.getDescription(),
			request.getPayloadJson(),
			request.getStatus(),
			request.getPriorityTag(),
			request.getSubmittedAt(),
			request.getProcessedAt(),
			request.getProcessNote(),
			user == null ? null : user.getId(),
			user == null ? null : user.getFullName(),
			user == null ? null : user.getPhone(),
			limitChange
		);
	}

	@Transactional
	public void approve(long requestId, long adminUserId, String note) {
		ServiceRequest request = loadPending(requestId);
		AdminUser adminUser = adminUserRepository.findById(adminUserId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin user not found"));

		String type = normalize(request.getRequestType());
		if ("limit_change".equals(type)) {
			LimitChangeRequest limitChange = limitChangeRequestRepository.findByServiceRequestId(request.getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Limit change request not found"));
			Account account = accountRepository.findById(limitChange.getAccount().getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
			account.setDailyTransferLimit(limitChange.getRequestedDailyTransferLimit());
			accountRepository.save(account);
		} else if ("profile_change".equals(type)) {
			applyProfilePayload(request);
		}

		markProcessed(request, adminUser, "APPROVED", note);
	}

	@Transactional
	public void reject(long requestId, long adminUserId, String note) {
		ServiceRequest request = loadPending(requestId);
		AdminUser adminUser = adminUserRepository.findById(adminUserId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin user not found"));
		markProcessed(request, adminUser, "REJECTED", note);
	}

	@Transactional
	public void closeSavingManually(long savingId, Long settlementAccountId, long adminUserId, String note) {
		AdminUser adminUser = adminUserRepository.findById(adminUserId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin user not found"));
		Saving saving = savingRepository.findWithDetailsById(savingId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Saving not found"));
		if (!"active".equalsIgnoreCase(saving.getStatus())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Saving is not active");
		}

		Account settlement = resolveSettlementAccount(saving, settlementAccountId);
		Account lockedSettlement = accountRepository.findByIdForUpdate(settlement.getId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Settlement account not found"));

		BigDecimal principal = nullToZero(saving.getPrincipalAmount());
		BigDecimal interest = nullToZero(saving.getAccruedInterestAmount()).add(nullToZero(saving.getPostedInterestAmount()));
		BigDecimal closeFee = calculateFee(principal, saving.getCloseFeeRate(), saving.getCloseFeeFlat());
		BigDecimal payout = principal.add(interest).subtract(closeFee).max(BigDecimal.ZERO);

		Transaction tx = transactionRepository.save(Transaction.builder()
			.transactionCode(generateTransactionCode("SAVCLOSE"))
			.fromAccount(null)
			.toAccount(lockedSettlement)
			.transactionType("saving_close")
			.amount(payout)
			.feeAmount(closeFee)
			.description(note == null || note.isBlank() ? "Manual saving settlement savingId=" + saving.getId() : note.trim())
			.status("completed")
			.initiatedByUser(saving.getUser())
			.completedAt(Instant.now())
			.build());

		BigDecimal before = lockedSettlement.getAvailableBalance();
		lockedSettlement.setAvailableBalance(before.add(payout));
		lockedSettlement.setCurrentBalance(lockedSettlement.getCurrentBalance().add(payout));
		accountRepository.save(lockedSettlement);

		ledgerRepository.save(AccountBalanceLedger.builder()
			.account(lockedSettlement)
			.transaction(tx)
			.entryType("credit")
			.amount(payout)
			.balanceBefore(before)
			.balanceAfter(lockedSettlement.getAvailableBalance())
			.build());

		saving.setStatus("closed");
		saving.setCloseDate(Instant.now());
		saving.setClosedBy(adminUser);
		saving.setLocked(false);
		savingRepository.save(saving);
	}

	@Transactional
	public void settleLoanEarlyManually(long loanId, Long repaymentAccountId, long adminUserId, String note) {
		adminUserRepository.findById(adminUserId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin user not found"));
		Loan loan = loanRepository.findById(loanId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan not found"));
		if (!"active".equalsIgnoreCase(loan.getStatus())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Loan is not active");
		}

		Account repayment = resolveRepaymentAccount(loan, repaymentAccountId);
		Account lockedRepayment = accountRepository.findByIdForUpdate(repayment.getId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Repayment account not found"));
		validateSameUser(lockedRepayment, loan.getUser().getId());

		BigDecimal principal = nullToZero(loan.getOutstandingPrincipal()).add(nullToZero(loan.getOverduePrincipal()));
		BigDecimal interest = nullToZero(loan.getOutstandingInterest()).add(nullToZero(loan.getOverdueInterest()));
		BigDecimal fee = calculateFee(principal, loan.getEarlyRepaymentFeeRate(), loan.getEarlyRepaymentFeeFlat());
		BigDecimal total = principal.add(interest).add(fee);
		if (lockedRepayment.getAvailableBalance().compareTo(total) < 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient balance");
		}

		Transaction tx = transactionRepository.save(Transaction.builder()
			.transactionCode(generateTransactionCode("LOANSETTLE"))
			.fromAccount(lockedRepayment)
			.toAccount(null)
			.transactionType("loan_early_settlement")
			.amount(total)
			.feeAmount(fee)
			.description(note == null || note.isBlank() ? "Manual early loan settlement loanId=" + loan.getId() : note.trim())
			.status("completed")
			.initiatedByUser(loan.getUser())
			.completedAt(Instant.now())
			.build());

		BigDecimal before = lockedRepayment.getAvailableBalance();
		lockedRepayment.setAvailableBalance(before.subtract(total));
		lockedRepayment.setCurrentBalance(lockedRepayment.getCurrentBalance().subtract(total));
		accountRepository.save(lockedRepayment);

		ledgerRepository.save(AccountBalanceLedger.builder()
			.account(lockedRepayment)
			.transaction(tx)
			.entryType("debit")
			.amount(total)
			.balanceBefore(before)
			.balanceAfter(lockedRepayment.getAvailableBalance())
			.build());

		Instant now = Instant.now();
		for (LoanRepaymentSchedule schedule : loanRepaymentScheduleRepository.findByLoanIdOrderByInstallmentNo(loan.getId())) {
			if (!"paid".equalsIgnoreCase(schedule.getStatus())) {
				schedule.setPrincipalPaid(nullToZero(schedule.getPrincipalDue()));
				schedule.setInterestPaid(nullToZero(schedule.getInterestDue()));
				schedule.setPenaltyInterestPaid(nullToZero(schedule.getPenaltyInterestDue()));
				schedule.setFeePaid(nullToZero(schedule.getFeeDue()));
				schedule.setStatus("paid");
				schedule.setPaidAt(now);
			}
		}

		loan.setOutstandingPrincipal(BigDecimal.ZERO);
		loan.setOutstandingInterest(BigDecimal.ZERO);
		loan.setOverduePrincipal(BigDecimal.ZERO);
		loan.setOverdueInterest(BigDecimal.ZERO);
		loan.setStatus("closed");
		loan.setClosedAt(now);
		loan.setNextDueDate(null);
		loanRepository.save(loan);
	}

	private ServiceRequest loadPending(long requestId) {
		ServiceRequest request = serviceRequestRepository.findById(requestId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service request not found"));
		if (!"submitted".equalsIgnoreCase(request.getStatus())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Service request is not pending");
		}
		return request;
	}

	private void markProcessed(ServiceRequest request, AdminUser adminUser, String status, String note) {
		request.setStatus(status);
		request.setAssignedTo(adminUser);
		request.setProcessedAt(Instant.now());
		request.setProcessNote(note);
		serviceRequestRepository.save(request);
		notifyUserRequestProcessed(request, status, note);
	}

	private void notifyUserRequestProcessed(ServiceRequest request, String status, String note) {
		User user = request.getUser();
		if (user == null || user.getId() == null) {
			return;
		}
		boolean approved = "APPROVED".equalsIgnoreCase(status);
		String title = approved ? "Yeu cau dich vu da duoc phe duyet" : "Yeu cau dich vu da bi tu choi";
		String content = request.getTitle() + (note == null || note.isBlank() ? "" : ": " + note.trim());
		notificationService.createForUser(user.getId(), "SERVICE_REQUEST", title, content);
	}

	private void applyProfilePayload(ServiceRequest request) {
		String payloadJson = request.getPayloadJson();
		if (payloadJson == null || payloadJson.isBlank()) {
			return;
		}
		User user = userRepository.findById(request.getUser().getId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
		try {
			Map<String, Object> payload = objectMapper.readValue(payloadJson, new TypeReference<>() {});
			Object fullName = payload.get("fullName");
			Object dob = payload.get("dob");
			Object address = payload.get("address");
			if (fullName instanceof String name && !name.isBlank()) {
				user.setFullName(name.trim());
			}
			if (dob instanceof String dobValue && !dobValue.isBlank()) {
				user.setDob(java.time.LocalDate.parse(dobValue));
			}
			if (address instanceof String addressValue && !addressValue.isBlank()) {
				user.setAddress(addressValue.trim());
			}
			userRepository.save(user);
		} catch (JsonProcessingException | DateTimeParseException ex) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid profile payload");
		}
	}

	private AdminServiceRequestSummary toSummary(ServiceRequest request) {
		User user = request.getUser();
		return new AdminServiceRequestSummary(
			request.getId(),
			request.getRequestType(),
			request.getTitle(),
			request.getStatus(),
			request.getPriorityTag(),
			request.getSubmittedAt(),
			user == null ? null : user.getId(),
			user == null ? null : user.getFullName(),
			user == null ? null : user.getPhone()
		);
	}

	private LimitChangeRequestResponse toLimitChangeResponse(LimitChangeRequest request) {
		Account account = request.getAccount();
		ServiceRequest serviceRequest = request.getServiceRequest();
		return new LimitChangeRequestResponse(
			request.getId(),
			serviceRequest.getId(),
			account.getId(),
			account.getAccountNumber(),
			account.getAccountName(),
			request.getCurrentDailyTransferLimit(),
			request.getRequestedDailyTransferLimit(),
			request.getReason(),
			serviceRequest.getStatus(),
			serviceRequest.getSubmittedAt(),
			serviceRequest.getProcessedAt(),
			serviceRequest.getProcessNote()
		);
	}

	private static String normalize(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed.toLowerCase();
	}

	private Account resolveSettlementAccount(Saving saving, Long settlementAccountId) {
		Account account = settlementAccountId == null ? saving.getSettlementAccount() : accountRepository.findById(settlementAccountId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Settlement account not found"));
		if (account == null) {
			account = saving.getSourceAccount();
		}
		validateSameUser(account, saving.getUser().getId());
		return account;
	}

	private Account resolveRepaymentAccount(Loan loan, Long repaymentAccountId) {
		if (repaymentAccountId != null) {
			return accountRepository.findById(repaymentAccountId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Repayment account not found"));
		}
		if (loan.getRepaymentAccount() != null) {
			return loan.getRepaymentAccount();
		}
		throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Repayment account is required");
	}

	private static void validateSameUser(Account account, Long userId) {
		if (account.getUser() == null || account.getUser().getId() == null || !account.getUser().getId().equals(userId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account does not belong to customer");
		}
	}

	private static BigDecimal calculateFee(BigDecimal baseAmount, BigDecimal rate, BigDecimal flat) {
		BigDecimal fee = nullToZero(flat);
		if (rate != null && rate.compareTo(BigDecimal.ZERO) > 0) {
			fee = fee.add(baseAmount.multiply(rate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
		}
		return fee.max(BigDecimal.ZERO);
	}

	private static BigDecimal nullToZero(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}

	private static String generateTransactionCode(String prefix) {
		return prefix + "_" + System.currentTimeMillis() + "_" + ((int) (Math.random() * 900_000) + 100_000);
	}
}
