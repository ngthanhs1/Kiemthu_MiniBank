package com.minibank.backend.transaction.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.minibank.backend.account.entity.Account;
import com.minibank.backend.transaction.dto.ExpenseCategoryOptionResponse;
import com.minibank.backend.transaction.dto.ExpenseCategorySummaryResponse;
import com.minibank.backend.transaction.dto.ExpenseClassifyRequest;
import com.minibank.backend.transaction.dto.ExpenseClassifyResponse;
import com.minibank.backend.transaction.dto.ExpenseMonthlyTrendResponse;
import com.minibank.backend.transaction.dto.ExpenseOverviewResponse;
import com.minibank.backend.transaction.dto.ExpenseUnclassifiedTransactionResponse;
import com.minibank.backend.transaction.entity.Transaction;
import com.minibank.backend.transaction.entity.TransactionCategory;
import com.minibank.backend.transaction.repository.TransactionCategoryRepository;
import com.minibank.backend.transaction.repository.TransactionRepository;
import com.minibank.backend.user.entity.User;
import com.minibank.backend.user.repository.UserRepository;

@Service
public class ExpenseManagementService {
	private static final Map<String, List<CategoryDefinition>> CATEGORY_CATALOG = Map.of(
		"out", List.of(
			new CategoryDefinition("an_uong", "Ăn uống"),
			new CategoryDefinition("mua_sam", "Mua sắm"),
			new CategoryDefinition("di_chuyen", "Di chuyển"),
			new CategoryDefinition("tien_ich", "Tiện ích"),
			new CategoryDefinition("giai_tri", "Giải trí"),
			new CategoryDefinition("giao_duc", "Giáo dục"),
			new CategoryDefinition("y_te", "Y tế"),
			new CategoryDefinition("khac", "Khác")
		),
		"in", List.of(
			new CategoryDefinition("luong", "Lương"),
			new CategoryDefinition("chuyen_khoan", "Chuyển khoản"),
			new CategoryDefinition("hoan_tien", "Hoàn tiền"),
			new CategoryDefinition("thu_nhap_khac", "Thu nhập khác"),
			new CategoryDefinition("khac", "Khác")
		)
	);

	private final TransactionRepository transactionRepository;
	private final TransactionCategoryRepository transactionCategoryRepository;
	private final UserRepository userRepository;

	public ExpenseManagementService(
		TransactionRepository transactionRepository,
		TransactionCategoryRepository transactionCategoryRepository,
		UserRepository userRepository
	) {
		this.transactionRepository = transactionRepository;
		this.transactionCategoryRepository = transactionCategoryRepository;
		this.userRepository = userRepository;
	}

	@Transactional(readOnly = true)
	public ExpenseOverviewResponse overview(long userId, String flowTypeInput) {
		String flowType = normalizeFlowType(flowTypeInput);
		List<Transaction> allTransactions = transactionRepository.findAllForUser(userId);
		Map<Long, TransactionCategory> latestCategoryByTransactionId = latestCategoriesFor(allTransactions.stream().map(Transaction::getId).toList());

		BigDecimal totalIncome = BigDecimal.ZERO;
		BigDecimal totalExpense = BigDecimal.ZERO;
		BigDecimal selectedFlowTotal = BigDecimal.ZERO;
		long selectedFlowTransactionCount = 0;
		Map<String, CategoryTotals> totalsByCategory = new LinkedHashMap<>();
		Map<YearMonth, TrendTotals> trendByMonth = initialTrendBuckets();
		List<ExpenseUnclassifiedTransactionResponse> unclassified = new ArrayList<>();

		for (Transaction transaction : allTransactions) {
			boolean incoming = isIncoming(transaction, userId);
			BigDecimal amount = safeAmount(transaction.getAmount());
			if (incoming) {
				totalIncome = totalIncome.add(amount);
			} else {
				totalExpense = totalExpense.add(amount);
			}

			String direction = incoming ? "in" : "out";
			if (!flowType.equals(direction)) {
				continue;
			}

			selectedFlowTransactionCount++;
			selectedFlowTotal = selectedFlowTotal.add(amount);
			YearMonth txMonth = YearMonth.from(transaction.getCreatedAt().atZone(ZoneOffset.UTC));
			TrendTotals monthTotal = trendByMonth.getOrDefault(txMonth, new TrendTotals(BigDecimal.ZERO, 0));
			trendByMonth.put(txMonth, new TrendTotals(monthTotal.amount().add(amount), monthTotal.transactionCount() + 1));

			TransactionCategory category = latestCategoryByTransactionId.get(transaction.getId());
			if (category == null || !flowType.equalsIgnoreCase(category.getFlowType())) {
				unclassified.add(toUnclassifiedResponse(transaction, direction));
				continue;
			}

			String categoryCode = category.getCategoryCode();
			CategoryDefinition definition = categoryDefinition(flowType, categoryCode);
			CategoryTotals current = totalsByCategory.get(categoryCode);
			if (current == null) {
				current = new CategoryTotals(categoryCode, definition.name(), BigDecimal.ZERO, 0);
			}
			totalsByCategory.put(categoryCode, new CategoryTotals(
				categoryCode,
				definition.name(),
				current.amount().add(amount),
				current.transactionCount() + 1
			));
		}

		BigDecimal selectedFlowTotalValue = selectedFlowTotal;
		List<ExpenseCategorySummaryResponse> categories = totalsByCategory.values().stream()
			.sorted(Comparator.comparing(CategoryTotals::amount).reversed())
			.map(total -> new ExpenseCategorySummaryResponse(
				total.categoryCode(),
				total.categoryName(),
				total.amount(),
				percentage(total.amount(), selectedFlowTotalValue),
				total.transactionCount()
			))
			.toList();

		return new ExpenseOverviewResponse(
			flowType,
			totalIncome,
			totalExpense,
			selectedFlowTotal,
			selectedFlowTransactionCount,
			unclassified.size(),
			categories,
			unclassified.stream().sorted(Comparator.comparing(ExpenseUnclassifiedTransactionResponse::createdAt).reversed()).toList(),
			trendByMonth.entrySet().stream()
				.sorted(Map.Entry.comparingByKey())
				.map(entry -> new ExpenseMonthlyTrendResponse(
					entry.getKey().toString(),
					entry.getKey().format(DateTimeFormatter.ofPattern("MM/yy")),
					entry.getValue().amount(),
					entry.getValue().transactionCount()
				))
				.toList()
		);
	}

	@Transactional(readOnly = true)
	public List<ExpenseCategoryOptionResponse> catalog(String flowTypeInput) {
		String flowType = normalizeFlowType(flowTypeInput);
		return CATEGORY_CATALOG.get(flowType).stream()
			.map(item -> new ExpenseCategoryOptionResponse(item.code(), item.name()))
			.toList();
	}

	@Transactional(readOnly = true)
	public List<ExpenseUnclassifiedTransactionResponse> unclassified(long userId, String flowTypeInput) {
		String flowType = normalizeFlowType(flowTypeInput);
		List<Transaction> allTransactions = transactionRepository.findAllForUser(userId);
		Map<Long, TransactionCategory> latestCategoryByTransactionId = latestCategoriesFor(allTransactions.stream().map(Transaction::getId).toList());

		return allTransactions.stream()
			.filter(tx -> flowType.equals(isIncoming(tx, userId) ? "in" : "out"))
			.filter(tx -> {
				TransactionCategory category = latestCategoryByTransactionId.get(tx.getId());
				return category == null || !flowType.equalsIgnoreCase(category.getFlowType());
			})
			.map(tx -> toUnclassifiedResponse(tx, isIncoming(tx, userId) ? "in" : "out"))
			.sorted(Comparator.comparing(ExpenseUnclassifiedTransactionResponse::createdAt).reversed())
			.toList();
	}

	@Transactional
	public ExpenseClassifyResponse classify(long userId, long transactionId, ExpenseClassifyRequest request) {
		Transaction transaction = transactionRepository.findAccessibleByIdAndUserId(transactionId, userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));

		String actualFlowType = isIncoming(transaction, userId) ? "in" : "out";
		String requestedFlowType = request.flowType() == null || request.flowType().isBlank()
			? actualFlowType
			: normalizeFlowType(request.flowType());
		if (!actualFlowType.equals(requestedFlowType)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "flowType does not match transaction direction");
		}

		CategoryDefinition definition = categoryDefinition(actualFlowType, request.categoryCode());
		String source = request.source() == null || request.source().isBlank() ? "manual" : request.source().trim();
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

		transactionCategoryRepository.deleteByTransactionId(transactionId);

		TransactionCategory saved = transactionCategoryRepository.save(
			TransactionCategory.builder()
				.transaction(transaction)
				.categoryCode(definition.code())
				.flowType(actualFlowType)
				.confidence(BigDecimal.ONE)
				.source(source)
				.taggedByUser(user)
				.build()
		);

		return new ExpenseClassifyResponse(
			saved.getTransaction().getId(),
			definition.code(),
			definition.name(),
			actualFlowType,
			saved.getSource(),
			saved.getTaggedAt()
		);
	}

	private Map<Long, TransactionCategory> latestCategoriesFor(Collection<Long> transactionIds) {
		if (transactionIds.isEmpty()) {
			return Map.of();
		}

		return transactionCategoryRepository.findByTransactionIdIn(transactionIds).stream()
			.sorted(Comparator.comparing(TransactionCategory::getTaggedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
			.collect(Collectors.toMap(
				category -> category.getTransaction().getId(),
				category -> category,
				(first, second) -> first,
				LinkedHashMap::new
			));
	}

	private ExpenseUnclassifiedTransactionResponse toUnclassifiedResponse(Transaction transaction, String direction) {
		Account from = transaction.getFromAccount();
		Account to = transaction.getToAccount();
		boolean incoming = "in".equals(direction);
		Account counterparty = incoming ? from : to;
		return new ExpenseUnclassifiedTransactionResponse(
			transaction.getId(),
			transaction.getTransactionCode(),
			direction,
			safeAmount(transaction.getAmount()),
			transaction.getDescription(),
			counterparty == null ? null : counterparty.getAccountNumber(),
			counterparty == null ? null : counterparty.getAccountName(),
			transaction.getTransactionType(),
			transaction.getCreatedAt()
		);
	}

	private static BigDecimal safeAmount(BigDecimal amount) {
		return amount == null ? BigDecimal.ZERO : amount;
	}

	private static Map<YearMonth, TrendTotals> initialTrendBuckets() {
		YearMonth current = YearMonth.now(ZoneOffset.UTC).minusMonths(5);
		Map<YearMonth, TrendTotals> buckets = new LinkedHashMap<>();
		for (int i = 0; i < 6; i++) {
			buckets.put(current.plusMonths(i), new TrendTotals(BigDecimal.ZERO, 0));
		}
		return buckets;
	}

	private static BigDecimal percentage(BigDecimal part, BigDecimal total) {
		if (total == null || total.compareTo(BigDecimal.ZERO) <= 0) {
			return BigDecimal.ZERO;
		}
		return part.multiply(new BigDecimal("100")).divide(total, 2, RoundingMode.HALF_UP);
	}

	private static boolean isIncoming(Transaction transaction, long userId) {
		Account to = transaction.getToAccount();
		return to != null && to.getUser() != null && Objects.equals(to.getUser().getId(), userId);
	}

	private static String normalizeFlowType(String flowTypeInput) {
		String value = flowTypeInput == null ? "out" : flowTypeInput.trim().toLowerCase();
		if (value.isBlank()) {
			return "out";
		}
		if ("income".equals(value) || "thu".equals(value) || "inbound".equals(value)) {
			return "in";
		}
		if ("expense".equals(value) || "chi".equals(value) || "outbound".equals(value)) {
			return "out";
		}
		if (!"in".equals(value) && !"out".equals(value)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "flowType must be in or out");
		}
		return value;
	}

	private static CategoryDefinition categoryDefinition(String flowType, String categoryCode) {
		String code = categoryCode == null ? null : categoryCode.trim().toLowerCase();
		if (code == null || code.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "categoryCode is required");
		}

		return CATEGORY_CATALOG.getOrDefault(flowType, List.of()).stream()
			.filter(item -> item.code().equals(code))
			.findFirst()
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown categoryCode for flowType " + flowType));
	}

	private record CategoryDefinition(String code, String name) {}

	private record CategoryTotals(String categoryCode, String categoryName, BigDecimal amount, long transactionCount) {}

	private record TrendTotals(BigDecimal amount, long transactionCount) {}
}