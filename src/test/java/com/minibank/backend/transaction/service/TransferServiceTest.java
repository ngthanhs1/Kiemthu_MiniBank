package com.minibank.backend.transaction.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.minibank.backend.account.entity.Account;
import com.minibank.backend.account.entity.AccountBalanceLedger;
import com.minibank.backend.account.repository.AccountBalanceLedgerRepository;
import com.minibank.backend.account.repository.AccountRepository;
import com.minibank.backend.account.repository.TransferQrIntentRepository;
import com.minibank.backend.ai.service.AiTransactionClassificationService;
import com.minibank.backend.common.otp.SmsOtpService;
import com.minibank.backend.system.service.NotificationService;
import com.minibank.backend.transaction.dto.TransferConfirmRequest;
import com.minibank.backend.transaction.dto.TransferConfirmResponse;
import com.minibank.backend.transaction.dto.TransferInitiateRequest;
import com.minibank.backend.transaction.dto.TransferInitiateResponse;
import com.minibank.backend.transaction.entity.Transaction;
import com.minibank.backend.transaction.entity.TransactionAuthentication;
import com.minibank.backend.transaction.repository.TransactionAuthenticationRepository;
import com.minibank.backend.transaction.repository.TransactionRepository;
import com.minibank.backend.user.entity.User;
import com.minibank.backend.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private AccountRepository accountRepository;

	@Mock
	private TransactionRepository transactionRepository;

	@Mock
	private TransactionAuthenticationRepository transactionAuthenticationRepository;

	@Mock
	private AccountBalanceLedgerRepository ledgerRepository;

	@Mock
	private TransferQrIntentRepository transferQrIntentRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private RsaSignatureService rsaSignatureService;

	@Mock
	private SmsOtpService smsOtpService;

	@Mock
	private AiTransactionClassificationService aiTransactionClassificationService;

	@Mock
	private NotificationService notificationService;

	private TransferService transferService;

	private User senderUser;
	private Account fromAccount;
	private Account toAccount;

	private static final BigDecimal LARGE_THRESHOLD = new BigDecimal("100000000"); // 100M
	private static final BigDecimal MANAGER_THRESHOLD = new BigDecimal("200000000"); // 200M

	@BeforeEach
	void setUp() {
		transferService = new TransferService(
			userRepository,
			accountRepository,
			transactionRepository,
			transactionAuthenticationRepository,
			ledgerRepository,
			transferQrIntentRepository,
			passwordEncoder,
			rsaSignatureService,
			smsOtpService,
			aiTransactionClassificationService,
			notificationService,
			false,
			LARGE_THRESHOLD,
			MANAGER_THRESHOLD
		);

		senderUser = User.builder()
			.id(1L)
			.phone("0981712585")
			.fullName("Nguyen Van A")
			.status("active")
			.transactionPinHash("$2a$10$pinHash")
			.publicKey("fake-public-key")
			.build();

		fromAccount = Account.builder()
			.id(10L)
			.accountNumber("1000001")
			.accountName("Nguyen Van A")
			.user(senderUser)
			.status("active")
			.availableBalance(new BigDecimal("5000000"))
			.currentBalance(new BigDecimal("5000000"))
			.dailyTransferLimit(new BigDecimal("50000000"))
			.dailyReceiveLimit(new BigDecimal("50000000"))
			.currency("VND")
			.build();

		User receiverUser = User.builder()
			.id(2L)
			.phone("0988888888")
			.fullName("Tran Thi B")
			.status("active")
			.build();

		toAccount = Account.builder()
			.id(20L)
			.accountNumber("2000002")
			.accountName("Tran Thi B")
			.user(receiverUser)
			.status("active")
			.availableBalance(new BigDecimal("1000000"))
			.currentBalance(new BigDecimal("1000000"))
			.dailyTransferLimit(new BigDecimal("50000000"))
			.dailyReceiveLimit(new BigDecimal("50000000"))
			.currency("VND")
			.build();
	}

	// ──────────────────────────────────────────────────────────────────────────
	// TEST CASES: initiate()
	// ──────────────────────────────────────────────────────────────────────────

	@Test
	@DisplayName("TC_TRF_01: Khởi tạo chuyển tiền thành công với số dư hợp lệ và PIN đúng")
	void initiate_Success() {
		TransferInitiateRequest request = new TransferInitiateRequest(
			"1000001",
			"2000002",
			new BigDecimal("500000"),
			"Chuyen tien an trua",
			null,
			null,
			"valid-signature",
			"123456"
		);

		when(userRepository.findById(1L)).thenReturn(Optional.of(senderUser));
		when(accountRepository.findByAccountNumber("1000001")).thenReturn(Optional.of(fromAccount));
		when(accountRepository.findByAccountNumber("2000002")).thenReturn(Optional.of(toAccount));
		when(passwordEncoder.matches("123456", "$2a$10$pinHash")).thenReturn(true);
		when(smsOtpService.sendOtp("0981712585")).thenReturn(new SmsOtpService.OtpSendResult(true, "123456"));
		when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
			Transaction t = inv.getArgument(0);
			t.setId(100L);
			return t;
		});

		TransferInitiateResponse response = transferService.initiate(1L, request);

		assertThat(response).isNotNull();
		assertThat(response.transactionId()).isEqualTo(100L);
		assertThat(response.status()).isEqualTo("pending");
		assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("500000"));
		verify(smsOtpService).sendOtp("0981712585");
		verify(transactionAuthenticationRepository).save(any(TransactionAuthentication.class));
	}

	@Test
	@DisplayName("TC_TRF_ERR_01: Ném lỗi 401 khi không tìm thấy người dùng")
	void initiate_UserNotFound() {
		TransferInitiateRequest request = new TransferInitiateRequest(
			"1000001", "2000002", new BigDecimal("500000"), null, null, null, "sig", "123456"
		);
		when(userRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> transferService.initiate(99L, request))
			.isInstanceOf(ResponseStatusException.class)
			.hasFieldOrPropertyWithValue("status", HttpStatus.UNAUTHORIZED);
	}

	@Test
	@DisplayName("TC_TRF_ERR_02: Ném lỗi 403 khi tài khoản người dùng không hoạt động (inactive/blocked)")
	void initiate_UserNotActive() {
		senderUser.setStatus("blocked");
		TransferInitiateRequest request = new TransferInitiateRequest(
			"1000001", "2000002", new BigDecimal("500000"), null, null, null, "sig", "123456"
		);
		when(userRepository.findById(1L)).thenReturn(Optional.of(senderUser));

		assertThatThrownBy(() -> transferService.initiate(1L, request))
			.isInstanceOf(ResponseStatusException.class)
			.hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);
	}

	@Test
	@DisplayName("TC_TRF_ERR_03: Ném lỗi 404 khi tài khoản người nhận không tồn tại")
	void initiate_RecipientAccountNotFound() {
		TransferInitiateRequest request = new TransferInitiateRequest(
			"1000001", "9999999", new BigDecimal("500000"), null, null, null, "sig", "123456"
		);
		when(userRepository.findById(1L)).thenReturn(Optional.of(senderUser));
		when(accountRepository.findByAccountNumber("1000001")).thenReturn(Optional.of(fromAccount));
		when(accountRepository.findByAccountNumber("9999999")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> transferService.initiate(1L, request))
			.isInstanceOf(ResponseStatusException.class)
			.hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
	}

	@Test
	@DisplayName("TC_TRF_ERR_04: Ném lỗi 400 khi số tiền chuyển <= 0")
	void initiate_AmountZeroOrNegative() {
		TransferInitiateRequest request = new TransferInitiateRequest(
			"1000001", "2000002", BigDecimal.ZERO, null, null, null, "sig", "123456"
		);
		when(userRepository.findById(1L)).thenReturn(Optional.of(senderUser));
		when(accountRepository.findByAccountNumber("1000001")).thenReturn(Optional.of(fromAccount));
		when(accountRepository.findByAccountNumber("2000002")).thenReturn(Optional.of(toAccount));

		assertThatThrownBy(() -> transferService.initiate(1L, request))
			.isInstanceOf(ResponseStatusException.class)
			.hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
	}

	@Test
	@DisplayName("TC_TRF_ERR_05: Ném lỗi 400 khi số dư không đủ (Insufficient balance)")
	void initiate_InsufficientBalance() {
		TransferInitiateRequest request = new TransferInitiateRequest(
			"1000001", "2000002", new BigDecimal("6000000"), null, null, null, "sig", "123456"
		);
		when(userRepository.findById(1L)).thenReturn(Optional.of(senderUser));
		when(accountRepository.findByAccountNumber("1000001")).thenReturn(Optional.of(fromAccount));
		when(accountRepository.findByAccountNumber("2000002")).thenReturn(Optional.of(toAccount));

		assertThatThrownBy(() -> transferService.initiate(1L, request))
			.isInstanceOf(ResponseStatusException.class)
			.hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
			.hasMessageContaining("Insufficient balance");
	}

	@Test
	@DisplayName("TC_TRF_ERR_06: Ném lỗi 400 khi số tiền vượt hạn mức chuyển ngày")
	void initiate_ExceedsDailyLimit() {
		fromAccount.setDailyTransferLimit(new BigDecimal("1000000")); // Limit 1M
		TransferInitiateRequest request = new TransferInitiateRequest(
			"1000001", "2000002", new BigDecimal("2000000"), null, null, null, "sig", "123456"
		);
		when(userRepository.findById(1L)).thenReturn(Optional.of(senderUser));
		when(accountRepository.findByAccountNumber("1000001")).thenReturn(Optional.of(fromAccount));
		when(accountRepository.findByAccountNumber("2000002")).thenReturn(Optional.of(toAccount));

		assertThatThrownBy(() -> transferService.initiate(1L, request))
			.isInstanceOf(ResponseStatusException.class)
			.hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
			.hasMessageContaining("daily transfer limit");
	}

	@Test
	@DisplayName("TC_TRF_ERR_07: Ném lỗi 400 khi mã PIN giao dịch không chính xác")
	void initiate_InvalidPin() {
		TransferInitiateRequest request = new TransferInitiateRequest(
			"1000001", "2000002", new BigDecimal("500000"), null, null, null, "sig", "999999"
		);
		when(userRepository.findById(1L)).thenReturn(Optional.of(senderUser));
		when(accountRepository.findByAccountNumber("1000001")).thenReturn(Optional.of(fromAccount));
		when(accountRepository.findByAccountNumber("2000002")).thenReturn(Optional.of(toAccount));
		when(passwordEncoder.matches("999999", "$2a$10$pinHash")).thenReturn(false);

		assertThatThrownBy(() -> transferService.initiate(1L, request))
			.isInstanceOf(ResponseStatusException.class)
			.hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
			.hasMessageContaining("Invalid PIN");
	}

	// ──────────────────────────────────────────────────────────────────────────
	// TEST CASES: confirm()
	// ──────────────────────────────────────────────────────────────────────────

	@Test
	@DisplayName("TC_TRF_CONFIRM_01: Xác nhận chuyển tiền thành công, trừ tiền người gửi, cộng tiền người nhận và ghi sổ cái")
	void confirm_Success_NormalAmount() {
		String validOtp = "123456";
		String otpHash = sha256Hex(validOtp);

		Transaction tx = Transaction.builder()
			.id(100L)
			.transactionCode("TX123456")
			.status("pending")
			.amount(new BigDecimal("500000"))
			.fromAccount(fromAccount)
			.toAccount(toAccount)
			.initiatedByUser(senderUser)
			.build();

		TransactionAuthentication auth = TransactionAuthentication.builder()
			.transaction(tx)
			.otpCodeHash(otpHash)
			.otpVerified(false)
			.build();

		when(userRepository.findById(1L)).thenReturn(Optional.of(senderUser));
		when(transactionRepository.findByIdAndInitiatedByUserId(100L, 1L)).thenReturn(Optional.of(tx));
		when(transactionAuthenticationRepository.findByTransactionId(100L)).thenReturn(Optional.of(auth));
		when(accountRepository.findByIdForUpdate(fromAccount.getId())).thenReturn(Optional.of(fromAccount));
		when(accountRepository.findByIdForUpdate(toAccount.getId())).thenReturn(Optional.of(toAccount));

		TransferConfirmRequest confirmReq = new TransferConfirmRequest(100L, validOtp);
		TransferConfirmResponse response = transferService.confirm(1L, confirmReq);

		assertThat(response).isNotNull();
		assertThat(response.status()).isEqualTo("completed");
		// Verify balances updated: fromAccount was 5M -> now 4.5M; toAccount was 1M -> now 1.5M
		assertThat(fromAccount.getAvailableBalance()).isEqualByComparingTo(new BigDecimal("4500000"));
		assertThat(toAccount.getAvailableBalance()).isEqualByComparingTo(new BigDecimal("1500000"));
		// Verify accounts saved
		verify(accountRepository).save(fromAccount);
		verify(accountRepository).save(toAccount);
		// Verify 2 ledger entries saved (debit & credit)
		verify(ledgerRepository, times(2)).save(any(AccountBalanceLedger.class));
		verify(notificationService, times(2)).createForUser(any(), eq("TRANSFER"), anyString(), anyString());
	}

	@Test
	@DisplayName("TC_TRF_CONFIRM_02: Ném lỗi 400 khi mã OTP xác nhận không đúng")
	void confirm_InvalidOtp() {
		String correctOtp = "123456";
		String wrongOtp = "654321";
		String otpHash = sha256Hex(correctOtp);

		Transaction tx = Transaction.builder()
			.id(100L)
			.status("pending")
			.amount(new BigDecimal("500000"))
			.fromAccount(fromAccount)
			.toAccount(toAccount)
			.initiatedByUser(senderUser)
			.build();

		TransactionAuthentication auth = TransactionAuthentication.builder()
			.transaction(tx)
			.otpCodeHash(otpHash)
			.build();

		when(userRepository.findById(1L)).thenReturn(Optional.of(senderUser));
		when(transactionRepository.findByIdAndInitiatedByUserId(100L, 1L)).thenReturn(Optional.of(tx));
		when(transactionAuthenticationRepository.findByTransactionId(100L)).thenReturn(Optional.of(auth));

		TransferConfirmRequest confirmReq = new TransferConfirmRequest(100L, wrongOtp);

		assertThatThrownBy(() -> transferService.confirm(1L, confirmReq))
			.isInstanceOf(ResponseStatusException.class)
			.hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
			.hasMessageContaining("Invalid OTP");

		verify(accountRepository, never()).findByIdForUpdate(any());
	}

	@Test
	@DisplayName("TC_TRF_CONFIRM_03: Giao dịch lớn >= 100M chuyển sang trạng thái pending_review chờ duyệt")
	void confirm_LargeAmount_RequiresReview() {
		String validOtp = "123456";
		String otpHash = sha256Hex(validOtp);

		BigDecimal largeAmount = new BigDecimal("150000000"); // 150M >= 100M
		Transaction tx = Transaction.builder()
			.id(101L)
			.status("pending")
			.amount(largeAmount)
			.fromAccount(fromAccount)
			.toAccount(toAccount)
			.initiatedByUser(senderUser)
			.build();

		TransactionAuthentication auth = TransactionAuthentication.builder()
			.transaction(tx)
			.otpCodeHash(otpHash)
			.build();

		when(userRepository.findById(1L)).thenReturn(Optional.of(senderUser));
		when(transactionRepository.findByIdAndInitiatedByUserId(101L, 1L)).thenReturn(Optional.of(tx));
		when(transactionAuthenticationRepository.findByTransactionId(101L)).thenReturn(Optional.of(auth));

		TransferConfirmRequest confirmReq = new TransferConfirmRequest(101L, validOtp);
		TransferConfirmResponse response = transferService.confirm(1L, confirmReq);

		assertThat(response.status()).isEqualTo("pending_review");
		// Tiền chưa được trừ ngay lập tức khi chưa được duyệt
		verify(accountRepository, never()).findByIdForUpdate(any());
		verify(ledgerRepository, never()).save(any());
	}

	@Test
	@DisplayName("TC_TRF_CONFIRM_04: Giao dịch đặc biệt lớn >= 200M chuyển sang trạng thái pending_manager")
	void confirm_ManagerAmount_RequiresManager() {
		String validOtp = "123456";
		String otpHash = sha256Hex(validOtp);

		BigDecimal managerAmount = new BigDecimal("250000000"); // 250M >= 200M
		Transaction tx = Transaction.builder()
			.id(102L)
			.status("pending")
			.amount(managerAmount)
			.fromAccount(fromAccount)
			.toAccount(toAccount)
			.initiatedByUser(senderUser)
			.build();

		TransactionAuthentication auth = TransactionAuthentication.builder()
			.transaction(tx)
			.otpCodeHash(otpHash)
			.build();

		when(userRepository.findById(1L)).thenReturn(Optional.of(senderUser));
		when(transactionRepository.findByIdAndInitiatedByUserId(102L, 1L)).thenReturn(Optional.of(tx));
		when(transactionAuthenticationRepository.findByTransactionId(102L)).thenReturn(Optional.of(auth));

		TransferConfirmRequest confirmReq = new TransferConfirmRequest(102L, validOtp);
		TransferConfirmResponse response = transferService.confirm(1L, confirmReq);

		assertThat(response.status()).isEqualTo("pending_manager");
		verify(accountRepository, never()).findByIdForUpdate(any());
	}

	private static String sha256Hex(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}