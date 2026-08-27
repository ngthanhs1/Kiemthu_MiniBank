package com.minibank.backend.ai.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minibank.backend.account.entity.Account;
import com.minibank.backend.account.repository.AccountRepository;
import com.minibank.backend.ai.dto.AiCategorySummary;
import com.minibank.backend.ai.dto.AiFinanceRecommendationItem;
import com.minibank.backend.ai.dto.AiFinanceRecommendationRequest;
import com.minibank.backend.ai.dto.AiFinanceRecommendationResponse;
import com.minibank.backend.ai.entity.AiDailyRecommendation;
import com.minibank.backend.ai.repository.AiDailyRecommendationRepository;
import com.minibank.backend.transaction.entity.Transaction;
import com.minibank.backend.transaction.entity.TransactionCategory;
import com.minibank.backend.transaction.repository.TransactionCategoryRepository;
import com.minibank.backend.transaction.repository.TransactionRepository;
import com.minibank.backend.user.entity.User;
import com.minibank.backend.user.repository.UserRepository;

@Service
public class AiRecommendationService {
	private final AiDailyRecommendationRepository dailyRepository;
	private final TransactionRepository transactionRepository;
	private final TransactionCategoryRepository transactionCategoryRepository;
	private final AccountRepository accountRepository;
	private final UserRepository userRepository;
	private final AiClient aiClient;
	private final ObjectMapper objectMapper;
	private final AiSettingsService settingsService;

	public AiRecommendationService(
		AiDailyRecommendationRepository dailyRepository,
		TransactionRepository transactionRepository,
		TransactionCategoryRepository transactionCategoryRepository,
		AccountRepository accountRepository,
		UserRepository userRepository,
		AiClient aiClient,
		ObjectMapper objectMapper,
		AiSettingsService settingsService
	) {
		this.dailyRepository = dailyRepository;
		this.transactionRepository = transactionRepository;
		this.transactionCategoryRepository = transactionCategoryRepository;
		this.accountRepository = accountRepository;
		this.userRepository = userRepository;
		this.aiClient = aiClient;
		this.objectMapper = objectMapper;
		this.settingsService = settingsService;
	}

	@Transactional
	public AiFinanceRecommendationResponse getDailyRecommendation(long userId) {
		if (!settingsService.getOrCreate().isRecommendationEnabled()) {
			return fallbackResponse(userId, YearMonth.now(ZoneOffset.UTC).toString());
		}
		LocalDate today = LocalDate.now(ZoneOffset.UTC);
		return dailyRepository.findFirstByUserIdAndDay(userId, today)
			.map(existing -> {
				AiFinanceRecommendationResponse response = toResponse(existing);
				return response.recommendations() == null || response.recommendations().isEmpty()
					? fallbackResponse(userId, YearMonth.now(ZoneOffset.UTC).toString())
					: response;
			})
			.orElseGet(() -> buildAndSave(userId, today));
	}

	@Transactional
	public int generateDailyForAllUsers() {
		LocalDate today = LocalDate.now(ZoneOffset.UTC);
		List<User> users = userRepository.findAll().stream()
			.filter(user -> "active".equalsIgnoreCase(user.getStatus()))
			.toList();
		int processed = 0;
		for (User user : users) {
			AiFinanceRecommendationResponse response = getDailyRecommendation(user.getId());
			if (response.recommendations() != null && !response.recommendations().isEmpty()) {
				processed++;
			}
		}
		return processed;
	}

	private AiFinanceRecommendationResponse buildAndSave(long userId, LocalDate day) {
		YearMonth month = YearMonth.now(ZoneOffset.UTC);
		FinanceSummary summary = buildSummary(userId, month);
		AiFinanceRecommendationRequest request = new AiFinanceRecommendationRequest(
			userId,
			month.toString(),
			summary.availableBalance,
			summary.income,
			summary.expense,
			summary.categorySummary
		);

		AiFinanceRecommendationResponse response = aiClient.recommendations(request)
			.orElseGet(() -> fallbackResponse(userId, month.toString()));

		AiDailyRecommendation saved = dailyRepository.save(
			AiDailyRecommendation.builder()
				.userId(userId)
				.day(day)
				.month(response.month())
				.riskLevel(response.riskLevel())
				.savingScore(response.savingScore())
				.recommendationsJson(serializeRecommendations(response.recommendations()))
				.source(response.source())
				.build()
		);

		return toResponse(saved);
	}

	private FinanceSummary buildSummary(long userId, YearMonth month) {
		Instant start = month.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
		Instant end = month.plusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);

		List<Transaction> transactions = transactionRepository.findAllForUser(userId).stream()
			.filter(tx -> !tx.getCreatedAt().isBefore(start) && tx.getCreatedAt().isBefore(end))
			.toList();

		Map<Long, TransactionCategory> latestCategories = latestCategoryMap(transactions);

		BigDecimal income = BigDecimal.ZERO;
		BigDecimal expense = BigDecimal.ZERO;
		Map<String, BigDecimal> totalsByCategory = new LinkedHashMap<>();

		for (Transaction transaction : transactions) {
			boolean incoming = isIncoming(transaction, userId);
			BigDecimal amount = transaction.getAmount() == null ? BigDecimal.ZERO : transaction.getAmount();
			if (incoming) {
				income = income.add(amount);
			} else {
				expense = expense.add(amount);
				TransactionCategory category = latestCategories.get(transaction.getId());
				String mapped = mapToAiExpenseCategory(category == null ? null : category.getCategoryCode());
				BigDecimal current = totalsByCategory.getOrDefault(mapped, BigDecimal.ZERO);
				totalsByCategory.put(mapped, current.add(amount));
			}
		}

		BigDecimal availableBalance = accountRepository.findByUserIdOrderByIdAsc(userId).stream()
			.map(Account::getAvailableBalance)
			.filter(value -> value != null)
			.reduce(BigDecimal.ZERO, BigDecimal::add);

		BigDecimal totalExpense = expense.compareTo(BigDecimal.ZERO) <= 0 ? BigDecimal.ONE : expense;
		List<AiCategorySummary> categorySummary = totalsByCategory.entrySet().stream()
			.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
			.map(entry -> new AiCategorySummary(
				entry.getKey(),
				entry.getValue(),
				percentage(entry.getValue(), totalExpense)
			))
			.toList();

		return new FinanceSummary(income, expense, availableBalance, categorySummary);
	}

	private Map<Long, TransactionCategory> latestCategoryMap(List<Transaction> transactions) {
		List<Long> ids = transactions.stream().map(Transaction::getId).toList();
		if (ids.isEmpty()) {
			return Map.of();
		}
		return transactionCategoryRepository.findByTransactionIdIn(ids).stream()
			.filter(cat -> cat.getTransaction() != null && cat.getTransaction().getId() != null)
			.sorted(Comparator.comparing(TransactionCategory::getTaggedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
			.collect(java.util.stream.Collectors.toMap(
				cat -> cat.getTransaction().getId(),
				java.util.function.Function.identity(),
				(first, second) -> first
			));
	}

	private AiFinanceRecommendationResponse toResponse(AiDailyRecommendation rec) {
		List<AiFinanceRecommendationItem> items = parseRecommendations(rec.getRecommendationsJson());
		return new AiFinanceRecommendationResponse(
			rec.getUserId(),
			rec.getMonth(),
			rec.getRiskLevel(),
			rec.getSavingScore(),
			items,
			rec.getSource()
		);
	}

	private List<AiFinanceRecommendationItem> parseRecommendations(String json) {
		if (json == null || json.isBlank()) {
			return List.of();
		}
		try {
			return objectMapper.readValue(json, new TypeReference<List<AiFinanceRecommendationItem>>() {});
		} catch (Exception ex) {
			return List.of();
		}
	}

	private String serializeRecommendations(List<AiFinanceRecommendationItem> items) {
		if (items == null) {
			return "[]";
		}
		try {
			return objectMapper.writeValueAsString(items);
		} catch (Exception ex) {
			return "[]";
		}
	}

	private AiFinanceRecommendationResponse fallbackResponse(long userId, String month) {
		FinanceSummary summary = buildSummary(userId, YearMonth.now(ZoneOffset.UTC));
		BigDecimal disposable = summary.income().subtract(summary.expense());
		int savingScore = calculateSavingScore(summary.income(), summary.expense());
		String riskLevel = riskLevel(summary.income(), summary.expense(), summary.availableBalance());
		List<AiFinanceRecommendationItem> items = buildRuleBasedRecommendations(summary, disposable, riskLevel);

		return new AiFinanceRecommendationResponse(
			userId,
			month,
			riskLevel,
			savingScore,
			items,
			"RULE_BASED"
		);
	}

	private List<AiFinanceRecommendationItem> buildRuleBasedRecommendations(
		FinanceSummary summary,
		BigDecimal disposable,
		String riskLevel
	) {
		List<AiFinanceRecommendationItem> items = new java.util.ArrayList<>();
		BigDecimal income = summary.income();
		BigDecimal expense = summary.expense();

		if (expense.compareTo(BigDecimal.ZERO) <= 0) {
			items.add(new AiFinanceRecommendationItem(
				"START_TRACKING",
				"Bắt đầu theo dõi chi tiêu",
				"Bạn chưa có dữ liệu chi tiêu trong tháng này. Hãy phân loại giao dịch để MiniBank đưa ra gợi ý chính xác hơn.",
				"LOW"
			));
			return items;
		}

		if (income.compareTo(BigDecimal.ZERO) > 0 && expense.compareTo(income.multiply(new BigDecimal("0.80"))) >= 0) {
			items.add(new AiFinanceRecommendationItem(
				"HIGH_SPENDING",
				"Giảm tốc độ chi tiêu",
				"Chi tiêu tháng này đã chiếm phần lớn thu nhập. Bạn nên rà soát các khoản không thiết yếu và đặt hạn mức cho phần còn lại của tháng.",
				"HIGH"
			));
		} else if (income.compareTo(BigDecimal.ZERO) > 0 && expense.compareTo(income.multiply(new BigDecimal("0.60"))) >= 0) {
			items.add(new AiFinanceRecommendationItem(
				"WATCH_BUDGET",
				"Theo dõi ngân sách sát hơn",
				"Chi tiêu đang ở mức trung bình-cao so với thu nhập. Hãy ưu tiên các khoản cần thiết và hạn chế mua sắm phát sinh.",
				"MEDIUM"
			));
		}

		summary.categorySummary().stream().findFirst().ifPresent(top -> {
			String priority = top.percentage().compareTo(new BigDecimal("40")) >= 0 ? "MEDIUM" : "LOW";
			items.add(new AiFinanceRecommendationItem(
				"TOP_CATEGORY",
				"Danh mục nổi bật: " + top.category(),
				"Danh mục này chiếm khoảng " + top.percentage().setScale(0, RoundingMode.HALF_UP) + "% tổng chi. Hãy kiểm tra xem có khoản nào có thể cắt giảm không.",
				priority
			));
		});

		if (disposable.compareTo(BigDecimal.ZERO) > 0) {
			items.add(new AiFinanceRecommendationItem(
				"SAVE_SURPLUS",
				"Tăng khoản tiết kiệm",
				"Bạn đang còn dư sau chi tiêu. Cân nhắc chuyển một phần sang tài khoản tiết kiệm để tạo quỹ dự phòng.",
				"LOW"
			));
		} else if (!"LOW".equals(riskLevel)) {
			items.add(new AiFinanceRecommendationItem(
				"NEGATIVE_CASHFLOW",
				"Cân đối lại dòng tiền",
				"Chi tiêu đang vượt hoặc gần vượt thu nhập. Hãy tạm hoãn các khoản chưa cần thiết và ưu tiên thanh toán bắt buộc.",
				"HIGH"
			));
		}

		if (items.isEmpty()) {
			items.add(new AiFinanceRecommendationItem(
				"GOOD_CONTROL",
				"Chi tiêu đang được kiểm soát",
				"Tỷ lệ chi tiêu hiện khá an toàn. Tiếp tục duy trì thói quen phân loại giao dịch và tiết kiệm định kỳ.",
				"LOW"
			));
		}
		return items.stream().limit(3).toList();
	}

	private int calculateSavingScore(BigDecimal income, BigDecimal expense) {
		if (income.compareTo(BigDecimal.ZERO) <= 0) {
			return expense.compareTo(BigDecimal.ZERO) <= 0 ? 50 : 20;
		}
		BigDecimal ratio = expense.divide(income, 4, RoundingMode.HALF_UP);
		int score = BigDecimal.ONE.subtract(ratio).multiply(new BigDecimal("100")).intValue();
		return Math.max(0, Math.min(100, score));
	}

	private String riskLevel(BigDecimal income, BigDecimal expense, BigDecimal availableBalance) {
		if (expense.compareTo(BigDecimal.ZERO) <= 0) {
			return "LOW";
		}
		if (income.compareTo(BigDecimal.ZERO) > 0) {
			BigDecimal ratio = expense.divide(income, 4, RoundingMode.HALF_UP);
			if (ratio.compareTo(new BigDecimal("0.90")) >= 0) {
				return "HIGH";
			}
			if (ratio.compareTo(new BigDecimal("0.65")) >= 0) {
				return "MEDIUM";
			}
		}
		if (availableBalance.compareTo(expense.multiply(new BigDecimal("0.50"))) < 0) {
			return "MEDIUM";
		}
		return "LOW";
	}

	private boolean isIncoming(Transaction transaction, long userId) {
		return transaction.getToAccount() != null
			&& transaction.getToAccount().getUser() != null
			&& userId == transaction.getToAccount().getUser().getId();
	}

	private String mapToAiExpenseCategory(String code) {
		if (code == null) {
			return "OTHER";
		}
		return switch (code.toLowerCase()) {
			case "an_uong" -> "FOOD";
			case "mua_sam" -> "SHOPPING";
			case "di_chuyen" -> "TRANSPORT";
			case "tien_ich" -> "BILL";
			case "giai_tri" -> "OTHER";
			case "giao_duc" -> "OTHER";
			case "y_te" -> "OTHER";
			default -> "OTHER";
		};
	}

	private BigDecimal percentage(BigDecimal part, BigDecimal total) {
		if (total.compareTo(BigDecimal.ZERO) <= 0) {
			return BigDecimal.ZERO;
		}
		return part.multiply(new BigDecimal("100")).divide(total, 2, RoundingMode.HALF_UP);
	}

	private record FinanceSummary(
		BigDecimal income,
		BigDecimal expense,
		BigDecimal availableBalance,
		List<AiCategorySummary> categorySummary
	) {}
}
