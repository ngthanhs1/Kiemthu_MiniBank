package com.minibank.backend.admin.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.minibank.backend.account.repository.AccountRepository;
import com.minibank.backend.admin.dto.AdminDashboardResponse;
import com.minibank.backend.loan.repository.LoanApplicationRepository;
import com.minibank.backend.loan.repository.LoanRepository;
import com.minibank.backend.saving.repository.SavingRepository;
import com.minibank.backend.support.repository.ServiceRequestRepository;
import com.minibank.backend.transaction.entity.Transaction;
import com.minibank.backend.transaction.repository.TransactionRepository;
import com.minibank.backend.user.repository.KycRequestRepository;
import com.minibank.backend.user.repository.UserRepository;

@RestController
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {
	private final UserRepository userRepository;
	private final KycRequestRepository kycRequestRepository;
	private final AccountRepository accountRepository;
	private final TransactionRepository transactionRepository;
	private final SavingRepository savingRepository;
	private final LoanRepository loanRepository;
	private final LoanApplicationRepository loanApplicationRepository;
	private final ServiceRequestRepository serviceRequestRepository;
	private final BigDecimal largeThreshold;

	public AdminDashboardController(
		UserRepository userRepository,
		KycRequestRepository kycRequestRepository,
		AccountRepository accountRepository,
		TransactionRepository transactionRepository,
		SavingRepository savingRepository,
		LoanRepository loanRepository,
		LoanApplicationRepository loanApplicationRepository,
		ServiceRequestRepository serviceRequestRepository,
		@Value("${app.transaction.large-threshold:100000000}") BigDecimal largeThreshold
	) {
		this.userRepository = userRepository;
		this.kycRequestRepository = kycRequestRepository;
		this.accountRepository = accountRepository;
		this.transactionRepository = transactionRepository;
		this.savingRepository = savingRepository;
		this.loanRepository = loanRepository;
		this.loanApplicationRepository = loanApplicationRepository;
		this.serviceRequestRepository = serviceRequestRepository;
		this.largeThreshold = largeThreshold;
	}

	@GetMapping
	@Transactional(readOnly = true)
	public AdminDashboardResponse dashboard() {
		Instant now = Instant.now();
		ZoneId zone = ZoneId.systemDefault();
		Instant todayStart = LocalDate.now(zone).atStartOfDay(zone).toInstant();
		Instant yesterdayStart = todayStart.minus(1, ChronoUnit.DAYS);
		Instant twoDaysAgo = todayStart.minus(2, ChronoUnit.DAYS);

		List<Transaction> todayTransactions = transactionRepository.findAllWithAccountsSince(todayStart).stream()
			.filter(this::isVisibleTransaction)
			.toList();
		List<Transaction> yesterdayTransactions = transactionRepository.findAllWithAccountsSince(twoDaysAgo).stream()
			.filter(this::isVisibleTransaction)
			.filter(tx -> tx.getCreatedAt() != null && !tx.getCreatedAt().isBefore(yesterdayStart) && tx.getCreatedAt().isBefore(todayStart))
			.toList();

		long pendingKyc = kycRequestRepository.findByStatusOrderBySubmittedAtDesc("pending").size();
		long activeAccounts = accountRepository.countByStatusActive();
		long activeSavings = savingRepository.countByStatusActive();
		long activeLoans = loanRepository.countByStatusActive();
		long pendingLoanApplications = loanApplicationRepository.findByStatusOrderBySubmittedAtDesc("pending").size();
		long submittedServiceRequests = serviceRequestRepository.countByStatusSubmitted();
		long pendingLarge = transactionRepository.countByStatusInAndAmountGreaterThanEqual(
			List.of("pending_review", "pending_manager"),
			largeThreshold
		);
		Instant threshold = Instant.now().plus(7, ChronoUnit.DAYS);
		long dueSoonSavingsCount = savingRepository.countDueSoon(threshold);
		long dueSoonLoansCount = loanRepository.countDueSoon(threshold);

		BigDecimal todayAmount = sumCompletedAmount(todayTransactions);
		BigDecimal yesterdayAmount = sumCompletedAmount(yesterdayTransactions);

		return new AdminDashboardResponse(
			now,
			List.of(
				metric("customers", "Tong so khach hang", userRepository.count(), "number", null, "blue", "/customers"),
				metric("kycPending", "Cho KYC", pendingKyc, "number", "Can xu ly", "amber", "/customers/kyc"),
				metric("activeAccounts", "Tai khoan dang hoat dong", activeAccounts, "number", null, "green", "/transactions/bank-accounts"),
				metric("todayTransactions", "GD trong ngay", todayTransactions.size(), "number", compareCount(todayTransactions.size(), yesterdayTransactions.size()), "indigo", "/transactions/list"),
				metric("todayAmount", "Tong tien GD hom nay", todayAmount, "money", compareAmount(todayAmount, yesterdayAmount), "emerald", "/transactions/list"),
				metric("activeSavings", "So TK dang mo", activeSavings, "number", dueSoonSavingsCount + " sap doi han", "purple", "/financial-products/savings/accounts"),
				metric("activeLoans", "Khoan vay dang hoat dong", activeLoans, "number", dueSoonLoansCount + " sap den han", "orange", "/financial-products/loans/contracts"),
				metric("pendingLoanApplications", "Ho so vay cho duyet", pendingLoanApplications, "number", "Can xu ly", "rose", "/financial-products/loans/applications"),
				metric("serviceRequests", "Yeu cau tu ho tro", submittedServiceRequests, "number", "Can xu ly", "teal", "/requests"),
				metric("largePending", "GD lon cho duyet", pendingLarge, "number", "Can xu ly", "cyan", "/transactions/large-approval")
			),
			buildCountSeries(todayTransactions, zone),
			buildAmountSeries(todayTransactions, zone),
			buildProductPerformance(todayTransactions),
			recentTransactions(todayTransactions),
			List.of(
				new AdminDashboardResponse.RequestMetric("KYC", pendingKyc, "/customers/kyc"),
				new AdminDashboardResponse.RequestMetric("Ho so vay", pendingLoanApplications, "/financial-products/loans/applications"),
				new AdminDashboardResponse.RequestMetric("Yeu cau ho tro", submittedServiceRequests, "/requests"),
				new AdminDashboardResponse.RequestMetric("GD lon", pendingLarge, "/transactions/large-approval")
			)
		);
	}

	private AdminDashboardResponse.Metric metric(String key, String label, long value, String valueType, String delta, String tone, String href) {
		return metric(key, label, BigDecimal.valueOf(value), valueType, delta, tone, href);
	}

	private AdminDashboardResponse.Metric metric(String key, String label, BigDecimal value, String valueType, String delta, String tone, String href) {
		return new AdminDashboardResponse.Metric(key, label, value, valueType, delta, tone, href);
	}

	private List<AdminDashboardResponse.TimePoint> buildCountSeries(List<Transaction> transactions, ZoneId zone) {
		Map<Integer, Long> byHour = transactions.stream()
			.collect(java.util.stream.Collectors.groupingBy(tx -> tx.getCreatedAt().atZone(zone).getHour(), java.util.stream.Collectors.counting()));
		return IntStream.range(0, 24)
			.mapToObj(hour -> new AdminDashboardResponse.TimePoint(String.format("%02d:00", hour), BigDecimal.valueOf(byHour.getOrDefault(hour, 0L))))
			.toList();
	}

	private List<AdminDashboardResponse.TimePoint> buildAmountSeries(List<Transaction> transactions, ZoneId zone) {
		Map<Integer, BigDecimal> byHour = transactions.stream()
			.filter(tx -> "completed".equalsIgnoreCase(tx.getStatus()))
			.collect(java.util.stream.Collectors.groupingBy(
				tx -> tx.getCreatedAt().atZone(zone).getHour(),
				java.util.stream.Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
			));
		return IntStream.range(0, 24)
			.mapToObj(hour -> new AdminDashboardResponse.TimePoint(
				String.format("%02d:00", hour),
				byHour.getOrDefault(hour, BigDecimal.ZERO).divide(BigDecimal.valueOf(1_000_000), 2, RoundingMode.HALF_UP)
			))
			.toList();
	}

	private List<AdminDashboardResponse.ProductMetric> buildProductPerformance(List<Transaction> transactions) {
		BigDecimal savingPrincipal = nullToZero(savingRepository.sumPrincipalByStatusActive())
			.divide(BigDecimal.valueOf(1_000_000_000), 2, RoundingMode.HALF_UP);
		BigDecimal loanOutstanding = nullToZero(loanRepository.sumOutstandingByStatusActive())
			.divide(BigDecimal.valueOf(1_000_000_000), 2, RoundingMode.HALF_UP);
		BigDecimal transferVolume = transactions.stream()
			.filter(tx -> "completed".equalsIgnoreCase(tx.getStatus()))
			.filter(tx -> "transfer".equalsIgnoreCase(tx.getTransactionType()))
			.map(Transaction::getAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add)
			.divide(BigDecimal.valueOf(1_000_000_000), 2, RoundingMode.HALF_UP);
		return List.of(
			new AdminDashboardResponse.ProductMetric("Tiet kiem", savingPrincipal),
			new AdminDashboardResponse.ProductMetric("Vay von", loanOutstanding),
			new AdminDashboardResponse.ProductMetric("Chuyen tien", transferVolume)
		);
	}

	private List<AdminDashboardResponse.RecentTransaction> recentTransactions(List<Transaction> transactions) {
		return transactions.stream()
			.sorted(Comparator.comparing(Transaction::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
			.limit(5)
			.map(tx -> new AdminDashboardResponse.RecentTransaction(
				tx.getId(),
				tx.getTransactionCode(),
				tx.getToAccount() != null ? tx.getToAccount().getAccountName() : tx.getFromAccount() != null ? tx.getFromAccount().getAccountName() : null,
				tx.getDescription(),
				tx.getAmount(),
				tx.getToAccount() != null ? "in" : "out",
				tx.getTransactionType(),
				tx.getStatus(),
				tx.getCreatedAt()
			))
			.toList();
	}

	private BigDecimal sumCompletedAmount(List<Transaction> transactions) {
		return transactions.stream()
			.filter(tx -> "completed".equalsIgnoreCase(tx.getStatus()))
			.map(Transaction::getAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private String compareCount(long current, long previous) {
		if (previous == 0) return current == 0 ? "0 so voi hom qua" : "+" + current + " so voi hom qua";
		BigDecimal pct = BigDecimal.valueOf(current - previous)
			.multiply(BigDecimal.valueOf(100))
			.divide(BigDecimal.valueOf(previous), 1, RoundingMode.HALF_UP);
		return (pct.signum() >= 0 ? "+" : "") + pct + "% so voi hom qua";
	}

	private String compareAmount(BigDecimal current, BigDecimal previous) {
		if (previous.compareTo(BigDecimal.ZERO) == 0) {
			return current.compareTo(BigDecimal.ZERO) == 0 ? "0 so voi hom qua" : "+100% so voi hom qua";
		}
		BigDecimal pct = current.subtract(previous)
			.multiply(BigDecimal.valueOf(100))
			.divide(previous, 1, RoundingMode.HALF_UP);
		return (pct.signum() >= 0 ? "+" : "") + pct + "% so voi hom qua";
	}

	private boolean isVisibleTransaction(Transaction tx) {
		String type = tx.getTransactionType();
		return type == null || !type.trim().toLowerCase().startsWith("saving_");
	}

	private static BigDecimal nullToZero(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}
}
