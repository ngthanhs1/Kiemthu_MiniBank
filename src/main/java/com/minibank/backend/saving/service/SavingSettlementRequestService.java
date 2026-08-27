package com.minibank.backend.saving.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

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
import com.minibank.backend.admin.entity.AdminUser;
import com.minibank.backend.admin.repository.AdminUserRepository;
import com.minibank.backend.saving.entity.Saving;
import com.minibank.backend.saving.repository.SavingRepository;
import com.minibank.backend.support.entity.ServiceRequest;
import com.minibank.backend.support.repository.ServiceRequestRepository;
import com.minibank.backend.transaction.entity.Transaction;
import com.minibank.backend.transaction.repository.TransactionRepository;
import com.minibank.backend.user.entity.User;
import com.minibank.backend.user.repository.UserRepository;

@Service
public class SavingSettlementRequestService {
	private static final String REQUEST_TYPE = "saving_settlement";
	private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
	private static final BigDecimal DAYS_IN_YEAR = new BigDecimal("365");

	private final ServiceRequestRepository serviceRequestRepository;
	private final SavingRepository savingRepository;
	private final AccountRepository accountRepository;
	private final AccountBalanceLedgerRepository ledgerRepository;
	private final TransactionRepository transactionRepository;
	private final UserRepository userRepository;
	private final AdminUserRepository adminUserRepository;
	private final ObjectMapper objectMapper;

	public SavingSettlementRequestService(
		ServiceRequestRepository serviceRequestRepository,
		SavingRepository savingRepository,
		AccountRepository accountRepository,
		AccountBalanceLedgerRepository ledgerRepository,
		TransactionRepository transactionRepository,
		UserRepository userRepository,
		AdminUserRepository adminUserRepository,
		ObjectMapper objectMapper
	) {
		this.serviceRequestRepository = serviceRequestRepository;
		this.savingRepository = savingRepository;
		this.accountRepository = accountRepository;
		this.ledgerRepository = ledgerRepository;
		this.transactionRepository = transactionRepository;
		this.userRepository = userRepository;
		this.adminUserRepository = adminUserRepository;
		this.objectMapper = objectMapper;
	}

	@Transactional
	public SettlementRequestItem create(long userId, CreateSettlementRequest request) {
		if (request == null || request.savingId() == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "savingId is required");
		}
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
		Saving saving = savingRepository.findWithDetailsByIdAndUserId(request.savingId(), userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Saving not found"));
		if (!"active".equalsIgnoreCase(saving.getStatus())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Saving is not active");
		}

		Account settlementAccount = resolveSettlementAccount(saving, request.settlementAccountId(), userId);
		ensureNoPendingSettlementRequest(saving.getId(), userId);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("savingId", saving.getId());
		payload.put("savingCode", saving.getCode());
		payload.put("settlementAccountId", settlementAccount.getId());
		payload.put("settlementAccountNumber", settlementAccount.getAccountNumber());
		payload.put("reason", request.reason());

		SettlementEstimate estimate = estimate(saving, Instant.now());
		payload.put("principalAmount", saving.getPrincipalAmount());
		payload.put("estimatedInterest", estimate.estimatedInterest());
		payload.put("settlementAmount", estimate.settlementAmount());
		payload.put("earlySettlement", estimate.earlySettlement());

		ServiceRequest serviceRequest = ServiceRequest.builder()
			.user(user)
			.requestType(REQUEST_TYPE)
			.title("Yeu cau tat toan so tiet kiem " + saving.getCode())
			.description(request.reason())
			.priorityTag(estimate.earlySettlement() ? "EARLY_SETTLEMENT" : "MATURITY_SETTLEMENT")
			.payloadJson(writePayload(payload))
			.status("SUBMITTED")
			.build();

		return toItem(serviceRequestRepository.save(serviceRequest));
	}

	@Transactional(readOnly = true)
	public List<SettlementRequestItem> listForUser(long userId) {
		return serviceRequestRepository.findByRequestTypeAndUserIdOrderBySubmittedAtDesc(REQUEST_TYPE, userId)
			.stream()
			.map(this::toItem)
			.toList();
	}

	@Transactional(readOnly = true)
	public List<SettlementRequestItem> listForAdmin(String q, String settlementType, String status) {
		String query = normalize(q);
		String typeFilter = normalize(settlementType);
		String statusFilter = normalize(status);
		return serviceRequestRepository.findByRequestTypeOrderBySubmittedAtDesc(REQUEST_TYPE).stream()
			.map(this::toItem)
			.filter(item -> statusFilter == null || statusFilter.equals(normalize(item.status())))
			.filter(item -> typeFilter == null || typeFilter.equals(normalize(item.settlementType())))
			.filter(item -> matchesQuery(item, query))
			.toList();
	}

	@Transactional(readOnly = true)
	public SettlementRequestDashboard dashboard(String q, String settlementType, String status) {
		List<SettlementRequestItem> items = listForAdmin(q, settlementType, status);
		long pendingCount = items.stream().filter(item -> isPending(item.status())).count();
		long earlyCount = items.stream().filter(item -> "early".equalsIgnoreCase(item.settlementType())).count();
		long managerReviewCount = items.stream().filter(item -> "MANAGER_REVIEW".equalsIgnoreCase(item.status())).count();
		BigDecimal totalValue = items.stream()
			.map(SettlementRequestItem::settlementAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		return new SettlementRequestDashboard(
			items.size(),
			pendingCount,
			earlyCount,
			managerReviewCount,
			totalValue,
			items
		);
	}

	@Transactional(readOnly = true)
	public SettlementRequestItem getForAdmin(long requestId) {
		ServiceRequest request = loadSettlementRequest(requestId);
		return toItem(request);
	}

	@Transactional
	public SettlementRequestItem approve(long requestId, long adminUserId, DecisionRequest decision) {
		ServiceRequest request = loadProcessableRequest(requestId);
		AdminUser admin = loadAdmin(adminUserId);
		Map<String, Object> payload = readPayload(request);
		Long savingId = requireLong(payload, "savingId");
		Long settlementAccountId = optionalLong(payload, "settlementAccountId");

		Saving saving = savingRepository.findWithDetailsById(savingId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Saving not found"));
		if (!"active".equalsIgnoreCase(saving.getStatus())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Saving is not active");
		}

		Account targetAccount = accountRepository.findByIdForUpdate(
				settlementAccountId != null ? settlementAccountId : fallbackSettlementAccountId(saving)
			)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Settlement account not found"));
		if (targetAccount.getUser() == null || !targetAccount.getUser().getId().equals(saving.getUser().getId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Settlement account does not belong to customer");
		}

		SettlementEstimate estimate = estimate(saving, Instant.now());
		creditSettlementAccount(saving, targetAccount, estimate.settlementAmount());

		saving.setStatus("closed");
		saving.setCloseDate(Instant.now());
		saving.setClosedBy(admin);
		saving.setLocked(false);
		savingRepository.save(saving);

		payload.put("approvedSettlementAmount", estimate.settlementAmount());
		payload.put("approvedInterest", estimate.estimatedInterest());
		payload.put("approvedAt", Instant.now().toString());

		request.setPayloadJson(writePayload(payload));
		request.setStatus("APPROVED");
		request.setAssignedTo(admin);
		request.setProcessedAt(Instant.now());
		request.setProcessNote(decision != null ? decision.note() : null);
		return toItem(serviceRequestRepository.save(request));
	}

	@Transactional
	public SettlementRequestItem reject(long requestId, long adminUserId, DecisionRequest decision) {
		ServiceRequest request = loadProcessableRequest(requestId);
		AdminUser admin = loadAdmin(adminUserId);
		request.setStatus("REJECTED");
		request.setAssignedTo(admin);
		request.setProcessedAt(Instant.now());
		request.setProcessNote(decision != null ? decision.note() : null);
		return toItem(serviceRequestRepository.save(request));
	}

	private void ensureNoPendingSettlementRequest(long savingId, long userId) {
		boolean exists = serviceRequestRepository.findByRequestTypeAndUserIdOrderBySubmittedAtDesc(REQUEST_TYPE, userId)
			.stream()
			.filter(req -> isPending(req.getStatus()))
			.anyMatch(req -> Objects.equals(savingId, optionalLong(readPayload(req), "savingId")));
		if (exists) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Saving already has a pending settlement request");
		}
	}

	private Account resolveSettlementAccount(Saving saving, Long settlementAccountId, long userId) {
		Account account;
		if (settlementAccountId != null) {
			account = accountRepository.findById(settlementAccountId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Settlement account not found"));
		} else if (saving.getSettlementAccount() != null) {
			account = saving.getSettlementAccount();
		} else {
			account = saving.getSourceAccount();
		}
		if (account == null || account.getUser() == null || !account.getUser().getId().equals(userId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Settlement account does not belong to user");
		}
		if (!"active".equalsIgnoreCase(account.getStatus())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Settlement account is not active");
		}
		return account;
	}

	private void creditSettlementAccount(Saving saving, Account account, BigDecimal amount) {
		BigDecimal before = account.getAvailableBalance();
		account.setAvailableBalance(before.add(amount));
		account.setCurrentBalance(account.getCurrentBalance().add(amount));
		accountRepository.save(account);

		Transaction tx = Transaction.builder()
			.transactionCode(generateTransactionCode())
			.fromAccount(null)
			.toAccount(account)
			.transactionType("saving_settlement")
			.amount(amount)
			.feeAmount(BigDecimal.ZERO)
			.description("savingId=" + saving.getId())
			.status("completed")
			.initiatedByUser(saving.getUser())
			.build();
		tx = transactionRepository.save(tx);
		tx.setCompletedAt(Instant.now());
		transactionRepository.save(tx);

		ledgerRepository.save(AccountBalanceLedger.builder()
			.account(account)
			.transaction(tx)
			.entryType("credit")
			.amount(amount)
			.balanceBefore(before)
			.balanceAfter(account.getAvailableBalance())
			.build());
	}

	private SettlementRequestItem toItem(ServiceRequest request) {
		Map<String, Object> payload = readPayload(request);
		Long savingId = optionalLong(payload, "savingId");
		Saving saving = savingId == null
			? null
			: savingRepository.findWithDetailsById(savingId).orElse(null);
		SettlementEstimate estimate = saving == null
			? new SettlementEstimate(
				decimal(payload.get("estimatedInterest")),
				decimal(payload.get("settlementAmount")),
				Boolean.TRUE.equals(payload.get("earlySettlement"))
			)
			: estimate(saving, request.getSubmittedAt() != null ? request.getSubmittedAt() : Instant.now());
		User user = request.getUser();
		String customerName = user == null ? null : user.getFullName();
		String savingCode = stringValue(payload.get("savingCode"));
		String settlementAccountNumber = stringValue(payload.get("settlementAccountNumber"));
		return new SettlementRequestItem(
			request.getId(),
			requestCode(request.getId(), request.getSubmittedAt()),
			savingId,
			savingCode,
			user == null ? null : user.getId(),
			customerName,
			customerName == null || customerName.isBlank() ? null : customerName.substring(0, 1).toUpperCase(Locale.ROOT),
			saving != null ? saving.getPrincipalAmount() : decimal(payload.get("principalAmount")),
			estimate.estimatedInterest(),
			estimate.settlementAmount(),
			estimate.earlySettlement() ? "early" : "maturity",
			request.getStatus(),
			statusLabel(request.getStatus()),
			settlementAccountNumber,
			request.getDescription(),
			request.getSubmittedAt(),
			request.getProcessedAt(),
			request.getProcessNote()
		);
	}

	private SettlementEstimate estimate(Saving saving, Instant asOf) {
		BigDecimal principal = saving.getPrincipalAmount() == null ? BigDecimal.ZERO : saving.getPrincipalAmount();
		Instant openDate = saving.getOpenDate() != null ? saving.getOpenDate() : saving.getCreatedAt();
		long days = openDate == null ? 0 : Math.max(0, ChronoUnit.DAYS.between(openDate, asOf));
		BigDecimal rate = saving.getActualInterestRate() == null ? BigDecimal.ZERO : saving.getActualInterestRate();
		BigDecimal interest = principal
			.multiply(rate)
			.multiply(BigDecimal.valueOf(days))
			.divide(ONE_HUNDRED.multiply(DAYS_IN_YEAR), 2, RoundingMode.HALF_UP);
		BigDecimal fee = closeFee(saving, principal);
		BigDecimal settlementAmount = principal.add(interest).subtract(fee).max(BigDecimal.ZERO);
		boolean early = saving.getMaturityDate() != null && asOf.isBefore(saving.getMaturityDate());
		return new SettlementEstimate(interest, settlementAmount, early);
	}

	private BigDecimal closeFee(Saving saving, BigDecimal principal) {
		BigDecimal fee = saving.getCloseFeeFlat() == null ? BigDecimal.ZERO : saving.getCloseFeeFlat();
		if (saving.getCloseFeeRate() != null) {
			fee = fee.add(principal.multiply(saving.getCloseFeeRate()).divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP));
		}
		return fee;
	}

	private ServiceRequest loadSettlementRequest(long requestId) {
		ServiceRequest request = serviceRequestRepository.findById(requestId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Settlement request not found"));
		if (!REQUEST_TYPE.equalsIgnoreCase(request.getRequestType())) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Settlement request not found");
		}
		return request;
	}

	private ServiceRequest loadProcessableRequest(long requestId) {
		ServiceRequest request = loadSettlementRequest(requestId);
		if (!isPending(request.getStatus())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Settlement request is already processed");
		}
		return request;
	}

	private AdminUser loadAdmin(long adminUserId) {
		return adminUserRepository.findById(adminUserId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Admin not found"));
	}

	private Long fallbackSettlementAccountId(Saving saving) {
		if (saving.getSettlementAccount() != null) {
			return saving.getSettlementAccount().getId();
		}
		if (saving.getSourceAccount() != null) {
			return saving.getSourceAccount().getId();
		}
		throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Saving has no settlement account");
	}

	private Map<String, Object> readPayload(ServiceRequest request) {
		if (request.getPayloadJson() == null || request.getPayloadJson().isBlank()) {
			return new LinkedHashMap<>();
		}
		try {
			return objectMapper.readValue(request.getPayloadJson(), new TypeReference<LinkedHashMap<String, Object>>() {});
		} catch (JsonProcessingException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid settlement payload");
		}
	}

	private String writePayload(Map<String, Object> payload) {
		try {
			return objectMapper.writeValueAsString(payload);
		} catch (JsonProcessingException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to serialize settlement payload");
		}
	}

	private static boolean matchesQuery(SettlementRequestItem item, String query) {
		if (query == null) return true;
		return contains(item.requestCode(), query)
			|| contains(item.customerName(), query)
			|| contains(item.savingCode(), query);
	}

	private static boolean contains(String value, String query) {
		return value != null && value.toLowerCase(Locale.ROOT).contains(query);
	}

	private static String normalize(String value) {
		if (value == null) return null;
		String trimmed = value.trim();
		return trimmed.isEmpty() || "all".equalsIgnoreCase(trimmed) ? null : trimmed.toLowerCase(Locale.ROOT);
	}

	private static boolean isPending(String status) {
		return "SUBMITTED".equalsIgnoreCase(status)
			|| "IN_REVIEW".equalsIgnoreCase(status)
			|| "MANAGER_REVIEW".equalsIgnoreCase(status);
	}

	private static String statusLabel(String status) {
		if ("APPROVED".equalsIgnoreCase(status)) return "Đã tất toán";
		if ("REJECTED".equalsIgnoreCase(status)) return "Đã từ chối";
		if ("MANAGER_REVIEW".equalsIgnoreCase(status)) return "Chờ quản lý duyệt";
		if ("IN_REVIEW".equalsIgnoreCase(status)) return "Đang kiểm tra";
		return "Chờ xử lý";
	}

	private static String requestCode(Long id, Instant submittedAt) {
		String date = submittedAt == null
			? java.time.LocalDate.now().toString().replace("-", "")
			: submittedAt.atZone(java.time.ZoneOffset.UTC).toLocalDate().toString().replace("-", "");
		return "REQ" + date + String.format("%03d", id == null ? 0 : id);
	}

	private static Long requireLong(Map<String, Object> payload, String field) {
		Long value = optionalLong(payload, field);
		if (value == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " is required");
		}
		return value;
	}

	private static Long optionalLong(Map<String, Object> payload, String field) {
		Object value = payload.get(field);
		if (value instanceof Number number) return number.longValue();
		if (value instanceof String text && !text.isBlank()) return Long.parseLong(text);
		return null;
	}

	private static BigDecimal decimal(Object value) {
		if (value instanceof BigDecimal decimal) return decimal;
		if (value instanceof Number number) return BigDecimal.valueOf(number.doubleValue()).setScale(2, RoundingMode.HALF_UP);
		if (value instanceof String text && !text.isBlank()) return new BigDecimal(text);
		return BigDecimal.ZERO;
	}

	private static String stringValue(Object value) {
		return value == null ? null : String.valueOf(value);
	}

	private static String generateTransactionCode() {
		long now = System.currentTimeMillis();
		int random = (int) (Math.random() * 900_000) + 100_000;
		return "TX" + now + random;
	}

	public record CreateSettlementRequest(Long savingId, Long settlementAccountId, String reason) {}
	public record DecisionRequest(String note) {}
	public record SettlementRequestDashboard(
		long totalRequests,
		long pendingRequests,
		long earlySettlementRequests,
		long managerReviewRequests,
		BigDecimal totalSettlementValue,
		List<SettlementRequestItem> items
	) {}
	public record SettlementRequestItem(
		Long id,
		String requestCode,
		Long savingId,
		String savingCode,
		Long customerId,
		String customerName,
		String customerInitial,
		BigDecimal principalAmount,
		BigDecimal estimatedInterest,
		BigDecimal settlementAmount,
		String settlementType,
		String status,
		String statusLabel,
		String settlementAccountNumber,
		String reason,
		Instant submittedAt,
		Instant processedAt,
		String processNote
	) {}
	private record SettlementEstimate(BigDecimal estimatedInterest, BigDecimal settlementAmount, boolean earlySettlement) {}
}
