package com.minibank.backend.admin.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.minibank.backend.loan.entity.Loan;
import com.minibank.backend.loan.entity.LoanApplication;
import com.minibank.backend.loan.entity.LoanRepaymentSchedule;
import com.minibank.backend.loan.repository.LoanApplicationRepository;
import com.minibank.backend.loan.repository.LoanRepaymentScheduleRepository;
import com.minibank.backend.loan.repository.LoanRepository;
import com.minibank.backend.user.entity.User;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.minibank.backend.admin.service.AdminServiceRequestService;
import com.minibank.backend.common.security.CurrentJwt;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'LOAN_APPLICATION_APPROVAL')")
public class AdminLoanManagementController {
	private final LoanApplicationRepository loanApplicationRepository;
	private final LoanRepository loanRepository;
	private final LoanRepaymentScheduleRepository scheduleRepository;
	private final AdminServiceRequestService adminServiceRequestService;

	public AdminLoanManagementController(
		LoanApplicationRepository loanApplicationRepository,
		LoanRepository loanRepository,
		LoanRepaymentScheduleRepository scheduleRepository,
		AdminServiceRequestService adminServiceRequestService
	) {
		this.loanApplicationRepository = loanApplicationRepository;
		this.loanRepository = loanRepository;
		this.scheduleRepository = scheduleRepository;
		this.adminServiceRequestService = adminServiceRequestService;
	}

	@GetMapping("/loan-applications")
	@Transactional(readOnly = true)
	public List<LoanApplicationItem> loanApplications(
		@RequestParam(value = "q", required = false) String q,
		@RequestParam(value = "status", required = false) String status
	) {
		String query = normalize(q);
		String statusFilter = normalize(status);
		return loanApplicationRepository.findAll().stream()
			.sorted(Comparator.comparing(LoanApplication::getSubmittedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
			.filter(app -> statusFilter == null || statusFilter.equals(normalize(app.getStatus())))
			.map(this::toLoanApplicationItem)
			.filter(item -> matchesLoanApplication(item, query))
			.toList();
	}

	@GetMapping("/loan-applications/dashboard")
	@Transactional(readOnly = true)
	public LoanApplicationDashboard loanApplicationDashboard(
		@RequestParam(value = "q", required = false) String q,
		@RequestParam(value = "status", required = false) String status
	) {
		List<LoanApplicationItem> items = loanApplications(q, status);
		long pending = items.stream().filter(item -> "pending".equalsIgnoreCase(item.status())).count();
		long approved = items.stream().filter(item -> "approved".equalsIgnoreCase(item.status())).count();
		long rejected = items.stream().filter(item -> "rejected".equalsIgnoreCase(item.status())).count();
		BigDecimal totalRequested = items.stream()
			.map(LoanApplicationItem::requestedAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		return new LoanApplicationDashboard(items.size(), pending, approved, rejected, totalRequested, items);
	}

	@GetMapping("/loan-applications/{id}")
	@Transactional(readOnly = true)
	public LoanApplicationItem loanApplication(@PathVariable long id) {
		return loanApplicationRepository.findById(id)
			.map(this::toLoanApplicationItem)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan application not found"));
	}

	@GetMapping("/loans")
	@Transactional(readOnly = true)
	public List<LoanItem> loans(
		@RequestParam(value = "q", required = false) String q,
		@RequestParam(value = "status", required = false) String status
	) {
		String query = normalize(q);
		String statusFilter = normalize(status);
		return loanRepository.findAll().stream()
			.sorted(Comparator.comparing(Loan::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
			.map(this::toLoanItem)
			.filter(item -> statusFilter == null || statusFilter.equals(normalize(item.displayStatus())))
			.filter(item -> matchesLoan(item, query))
			.toList();
	}

	@GetMapping("/loans/dashboard")
	@Transactional(readOnly = true)
	public LoanDashboard loanDashboard(
		@RequestParam(value = "q", required = false) String q,
		@RequestParam(value = "status", required = false) String status
	) {
		List<LoanItem> items = loans(q, status);
		BigDecimal totalOutstanding = items.stream()
			.map(LoanItem::outstandingPrincipal)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		long dueSoon = items.stream().filter(item -> "due_soon".equalsIgnoreCase(item.displayStatus())).count();
		long overdue = items.stream().filter(item -> "overdue".equalsIgnoreCase(item.displayStatus())).count();
		return new LoanDashboard(totalOutstanding, dueSoon, overdue, items);
	}

	@GetMapping("/loans/{id}")
	@Transactional(readOnly = true)
	public LoanDetail loan(@PathVariable long id) {
		Loan loan = loanRepository.findById(id)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan not found"));
		List<RepaymentScheduleItem> schedules = scheduleRepository.findByLoanIdOrderByInstallmentNo(loan.getId())
			.stream()
			.map(this::toScheduleItem)
			.toList();
		return new LoanDetail(toLoanItem(loan), schedules);
	}

	public record EarlySettlementRequest(Long repaymentAccountId, String note) {}

	@PostMapping("/loans/{id}/early-settle")
	@Transactional
	public void earlySettle(
		@PathVariable Long id,
		@RequestBody(required = false) EarlySettlementRequest body
	) {
		long adminUserId = CurrentJwt.requireUserId();
		adminServiceRequestService.settleLoanEarlyManually(
			id,
			body != null ? body.repaymentAccountId() : null,
			adminUserId,
			body != null ? body.note() : null
		);
	}

	@GetMapping("/loan-repayment-schedules")
	@Transactional(readOnly = true)
	public List<RepaymentScheduleItem> repaymentSchedules(
		@RequestParam(value = "q", required = false) String q,
		@RequestParam(value = "status", required = false) String status
	) {
		String query = normalize(q);
		String statusFilter = normalize(status);
		return scheduleRepository.findAll().stream()
			.sorted(Comparator.comparing(LoanRepaymentSchedule::getDueDate, Comparator.nullsLast(Comparator.naturalOrder())))
			.map(this::toScheduleItem)
			.filter(item -> statusFilter == null || statusFilter.equals(normalize(item.displayStatus())))
			.filter(item -> matchesSchedule(item, query))
			.toList();
	}

	@GetMapping("/loan-repayment-schedules/dashboard")
	@Transactional(readOnly = true)
	public RepaymentScheduleDashboard repaymentScheduleDashboard(
		@RequestParam(value = "q", required = false) String q,
		@RequestParam(value = "status", required = false) String status
	) {
		List<RepaymentScheduleItem> items = repaymentSchedules(q, status);
		long dueSoon = items.stream().filter(item -> "due_soon".equalsIgnoreCase(item.displayStatus())).count();
		long paid = items.stream().filter(item -> "paid".equalsIgnoreCase(item.displayStatus())).count();
		long overdue = items.stream().filter(item -> "overdue".equalsIgnoreCase(item.displayStatus())).count();
		return new RepaymentScheduleDashboard(items.size(), dueSoon, paid, overdue, items);
	}

	@GetMapping("/loan-repayment-schedules/{id}")
	@Transactional(readOnly = true)
	public RepaymentScheduleItem repaymentSchedule(@PathVariable long id) {
		return scheduleRepository.findById(id)
			.map(this::toScheduleItem)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Repayment schedule not found"));
	}

	private LoanApplicationItem toLoanApplicationItem(LoanApplication app) {
		User user = app.getUser();
		return new LoanApplicationItem(
			app.getId(),
			"LA" + String.format("%03d", app.getId()),
			user == null ? null : user.getId(),
			user == null ? null : user.getFullName(),
			initial(user),
			app.getLoanProduct() == null ? null : app.getLoanProduct().getId(),
			app.getLoanProduct() == null ? null : app.getLoanProduct().getName(),
			app.getRequestedAmount(),
			app.getRequestedTermMonths(),
			app.getMonthlyIncome(),
			app.getPurpose(),
			app.getCollateralDescription(),
			app.getPriorityTag(),
			app.getStatus(),
			loanApplicationStatusLabel(app.getStatus()),
			app.getSubmittedAt(),
			app.getReviewedAt(),
			app.getReviewNote()
		);
	}

	private LoanItem toLoanItem(Loan loan) {
		LoanRepaymentSchedule nextDue = nextUnpaidSchedule(loan.getId());
		String displayStatus = loanDisplayStatus(loan, nextDue);
		User user = loan.getUser();
		return new LoanItem(
			loan.getId(),
			loan.getCode(),
			user == null ? null : user.getId(),
			user == null ? null : user.getFullName(),
			loan.getRepaymentAccount() == null ? null : loan.getRepaymentAccount().getAccountNumber(),
			loan.getLoanProduct() == null ? null : loan.getLoanProduct().getName(),
			loan.getApprovedAmount(),
			loan.getOutstandingPrincipal(),
			loan.getActualInterestRate(),
			loan.getTermMonths(),
			nextDue != null ? nextDue.getDueDate() : toLocalDate(loan.getNextDueDate()),
			nextDue != null ? nextDue.getTotalDue() : BigDecimal.ZERO,
			loan.getStatus(),
			displayStatus,
			loanStatusLabel(displayStatus),
			loan.getCreatedAt()
		);
	}

	private RepaymentScheduleItem toScheduleItem(LoanRepaymentSchedule schedule) {
		Loan loan = schedule.getLoan();
		User user = loan == null ? null : loan.getUser();
		String displayStatus = scheduleDisplayStatus(schedule);
		BigDecimal paidAmount = nvl(schedule.getPrincipalPaid())
			.add(nvl(schedule.getInterestPaid()))
			.add(nvl(schedule.getPenaltyInterestPaid()))
			.add(nvl(schedule.getFeePaid()));
		return new RepaymentScheduleItem(
			schedule.getId(),
			loan == null ? null : loan.getId(),
			loan == null ? null : loan.getCode(),
			schedule.getInstallmentNo(),
			user == null ? null : user.getId(),
			user == null ? null : user.getFullName(),
			initial(user),
			schedule.getDueDate(),
			schedule.getPrincipalDue(),
			schedule.getInterestDue(),
			schedule.getPenaltyInterestDue(),
			schedule.getFeeDue(),
			schedule.getTotalDue(),
			paidAmount,
			schedule.getStatus(),
			displayStatus,
			scheduleStatusLabel(displayStatus),
			schedule.getPaidAt()
		);
	}

	private LoanRepaymentSchedule nextUnpaidSchedule(long loanId) {
		return scheduleRepository.findByLoanIdOrderByInstallmentNo(loanId).stream()
			.filter(schedule -> !"paid".equalsIgnoreCase(schedule.getStatus()))
			.min(Comparator.comparing(LoanRepaymentSchedule::getDueDate, Comparator.nullsLast(Comparator.naturalOrder())))
			.orElse(null);
	}

	private static String loanDisplayStatus(Loan loan, LoanRepaymentSchedule nextDue) {
		if ("closed".equalsIgnoreCase(loan.getStatus())) return "closed";
		if (nvl(loan.getOverduePrincipal()).compareTo(BigDecimal.ZERO) > 0
			|| nvl(loan.getOverdueInterest()).compareTo(BigDecimal.ZERO) > 0
			|| (nextDue != null && "overdue".equals(scheduleDisplayStatus(nextDue)))) {
			return "overdue";
		}
		if (nextDue != null && "due_soon".equals(scheduleDisplayStatus(nextDue))) return "due_soon";
		return normalize(loan.getStatus()) == null ? "active" : normalize(loan.getStatus());
	}

	private static String scheduleDisplayStatus(LoanRepaymentSchedule schedule) {
		if ("paid".equalsIgnoreCase(schedule.getStatus()) || schedule.getPaidAt() != null) return "paid";
		LocalDate today = LocalDate.now();
		if (schedule.getDueDate() != null && schedule.getDueDate().isBefore(today)) return "overdue";
		if (schedule.getDueDate() != null && !schedule.getDueDate().isAfter(today.plusDays(7))) return "due_soon";
		return normalize(schedule.getStatus()) == null ? "unpaid" : normalize(schedule.getStatus());
	}

	private static boolean matchesLoanApplication(LoanApplicationItem item, String query) {
		if (query == null) return true;
		return contains(item.applicationCode(), query)
			|| contains(item.customerName(), query)
			|| contains(item.productName(), query);
	}

	private static boolean matchesLoan(LoanItem item, String query) {
		if (query == null) return true;
		return contains(item.loanCode(), query)
			|| contains(item.customerName(), query)
			|| contains(item.repaymentAccountNumber(), query);
	}

	private static boolean matchesSchedule(RepaymentScheduleItem item, String query) {
		if (query == null) return true;
		return contains(item.loanCode(), query)
			|| contains(item.customerName(), query);
	}

	private static boolean contains(String value, String query) {
		return value != null && value.toLowerCase(Locale.ROOT).contains(query);
	}

	private static String normalize(String value) {
		if (value == null) return null;
		String trimmed = value.trim();
		return trimmed.isEmpty() || "all".equalsIgnoreCase(trimmed) ? null : trimmed.toLowerCase(Locale.ROOT);
	}

	private static String initial(User user) {
		if (user == null || user.getFullName() == null || user.getFullName().isBlank()) return null;
		return user.getFullName().trim().substring(0, 1).toUpperCase(Locale.ROOT);
	}

	private static LocalDate toLocalDate(Instant value) {
		return value == null ? null : value.atZone(ZoneId.systemDefault()).toLocalDate();
	}

	private static BigDecimal nvl(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}

	private static String loanApplicationStatusLabel(String status) {
		if ("approved".equalsIgnoreCase(status)) return "Đã duyệt";
		if ("rejected".equalsIgnoreCase(status)) return "Từ chối";
		return "Chờ duyệt";
	}

	private static String loanStatusLabel(String status) {
		if ("overdue".equalsIgnoreCase(status)) return "Quá hạn";
		if ("due_soon".equalsIgnoreCase(status)) return "Sắp đến hạn";
		if ("closed".equalsIgnoreCase(status)) return "Đã đóng";
		return "Đang vay";
	}

	private static String scheduleStatusLabel(String status) {
		if ("paid".equalsIgnoreCase(status)) return "Đã trả";
		if ("overdue".equalsIgnoreCase(status)) return "Quá hạn";
		if ("due_soon".equalsIgnoreCase(status)) return "Sắp đến hạn";
		return "Chưa trả";
	}

	public record LoanApplicationDashboard(
		long totalApplications,
		long pendingApplications,
		long approvedApplications,
		long rejectedApplications,
		BigDecimal totalRequestedAmount,
		List<LoanApplicationItem> items
	) {}
	public record LoanApplicationItem(
		Long id,
		String applicationCode,
		Long customerId,
		String customerName,
		String customerInitial,
		Long productId,
		String productName,
		BigDecimal requestedAmount,
		int termMonths,
		BigDecimal monthlyIncome,
		String purpose,
		String collateralDescription,
		String priorityTag,
		String status,
		String statusLabel,
		Instant submittedAt,
		Instant reviewedAt,
		String reviewNote
	) {}
	public record LoanDashboard(
		BigDecimal totalOutstandingPrincipal,
		long dueSoonLoanCount,
		long overdueLoanCount,
		List<LoanItem> items
	) {}
	public record LoanItem(
		Long id,
		String loanCode,
		Long customerId,
		String customerName,
		String repaymentAccountNumber,
		String productName,
		BigDecimal approvedAmount,
		BigDecimal outstandingPrincipal,
		BigDecimal annualInterestRate,
		int termMonths,
		LocalDate nextDueDate,
		BigDecimal nextDueAmount,
		String status,
		String displayStatus,
		String statusLabel,
		Instant createdAt
	) {}
	public record LoanDetail(LoanItem loan, List<RepaymentScheduleItem> repaymentSchedules) {}
	public record RepaymentScheduleDashboard(
		long totalInstallments,
		long dueSoonInstallments,
		long paidInstallments,
		long overdueInstallments,
		List<RepaymentScheduleItem> items
	) {}
	public record RepaymentScheduleItem(
		Long id,
		Long loanId,
		String loanCode,
		int installmentNo,
		Long customerId,
		String customerName,
		String customerInitial,
		LocalDate dueDate,
		BigDecimal principalDue,
		BigDecimal interestDue,
		BigDecimal penaltyInterestDue,
		BigDecimal feeDue,
		BigDecimal totalDue,
		BigDecimal paidAmount,
		String status,
		String displayStatus,
		String statusLabel,
		Instant paidAt
	) {}
}
