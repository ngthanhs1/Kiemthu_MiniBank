package com.minibank.backend.transaction.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import com.minibank.backend.account.entity.Account;
import com.minibank.backend.account.repository.AccountRepository;
import com.minibank.backend.transaction.dto.TransferConfirmRequest;
import com.minibank.backend.transaction.entity.Transaction;
import com.minibank.backend.transaction.entity.TransactionAuthentication;
import com.minibank.backend.transaction.repository.TransactionAuthenticationRepository;
import com.minibank.backend.transaction.repository.TransactionRepository;
import com.minibank.backend.user.entity.User;
import com.minibank.backend.user.repository.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
class TransferConcurrencyIntegrationTest {

	@Autowired
	private TransferService transferService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private TransactionRepository transactionRepository;

	@Autowired
	private TransactionAuthenticationRepository transactionAuthenticationRepository;

	private User senderUser;
	private User receiverUser;
	private Account fromAccount;
	private Account toAccount;

	private static final String OTP_CODE = "123456";
	private static final String OTP_HASH = sha256Hex(OTP_CODE);

	@BeforeEach
	void setUp() {
		// Dọn dẹp dữ liệu cũ
		transactionAuthenticationRepository.deleteAll();
		transactionRepository.deleteAll();

		senderUser = userRepository.save(User.builder()
			.phone("0919999001")
			.email("concurrency_sender@minibank.com")
			.passwordHash("$2a$10$pinHash")
			.fullName("Concurrency Sender")
			.status("active")
			.customerRank("dong")
			.build());

		receiverUser = userRepository.save(User.builder()
			.phone("0919999002")
			.email("concurrency_receiver@minibank.com")
			.passwordHash("$2a$10$pinHash")
			.fullName("Concurrency Receiver")
			.status("active")
			.customerRank("dong")
			.build());

		// Số dư ban đầu: Tài khoản gửi chỉ có đúng 1.000.000 VND
		fromAccount = accountRepository.save(Account.builder()
			.accountNumber("CONCUR_001")
			.accountName("Concurrency Sender")
			.accountType("payment")
			.user(senderUser)
			.status("active")
			.availableBalance(new BigDecimal("1000000"))
			.currentBalance(new BigDecimal("1000000"))
			.dailyTransferLimit(new BigDecimal("50000000"))
			.dailyReceiveLimit(new BigDecimal("50000000"))
			.currency("VND")
			.build());

		// Số dư ban đầu: Tài khoản nhận có 0 VND
		toAccount = accountRepository.save(Account.builder()
			.accountNumber("CONCUR_002")
			.accountName("Concurrency Receiver")
			.accountType("payment")
			.user(receiverUser)
			.status("active")
			.availableBalance(BigDecimal.ZERO)
			.currentBalance(BigDecimal.ZERO)
			.dailyTransferLimit(new BigDecimal("50000000"))
			.dailyReceiveLimit(new BigDecimal("50000000"))
			.currency("VND")
			.build());
	}

	@Test
	@DisplayName("CONCURRENCY_01: Chống rút tiền kép (Race Condition/Double Spending) - 2 luồng rút 1.000.000đ cùng lúc")
	void concurrentTransfer_ShouldPreventDoubleSpending() throws InterruptedException {
		// Tạo 2 giao dịch pending riêng biệt, mỗi giao dịch rút toàn bộ 1.000.000 VND
		Transaction tx1 = transactionRepository.save(Transaction.builder()
			.transactionCode("TX_RACE_001")
			.status("pending")
			.transactionType("transfer")
			.amount(new BigDecimal("1000000"))
			.feeAmount(BigDecimal.ZERO)
			.fromAccount(fromAccount)
			.toAccount(toAccount)
			.initiatedByUser(senderUser)
			.build());

		transactionAuthenticationRepository.save(TransactionAuthentication.builder()
			.transaction(tx1)
			.otpCodeHash(OTP_HASH)
			.otpVerified(false)
			.pinVerified(true)
			.authStatus("pin_verified")
			.build());

		Transaction tx2 = transactionRepository.save(Transaction.builder()
			.transactionCode("TX_RACE_002")
			.status("pending")
			.transactionType("transfer")
			.amount(new BigDecimal("1000000"))
			.feeAmount(BigDecimal.ZERO)
			.fromAccount(fromAccount)
			.toAccount(toAccount)
			.initiatedByUser(senderUser)
			.build());

		transactionAuthenticationRepository.save(TransactionAuthentication.builder()
			.transaction(tx2)
			.otpCodeHash(OTP_HASH)
			.otpVerified(false)
			.pinVerified(true)
			.authStatus("pin_verified")
			.build());

		int numberOfThreads = 2;
		ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
		CountDownLatch readyLatch = new CountDownLatch(numberOfThreads);
		CountDownLatch startLatch = new CountDownLatch(1);
		CountDownLatch finishLatch = new CountDownLatch(numberOfThreads);

		AtomicInteger successCount = new AtomicInteger(0);
		AtomicInteger failureCount = new AtomicInteger(0);
		List<String> failureReasons = Collections.synchronizedList(new ArrayList<>());

		Long[] txIds = new Long[]{tx1.getId(), tx2.getId()};

		for (int i = 0; i < numberOfThreads; i++) {
			final long transactionId = txIds[i];
			executor.submit(() -> {
				readyLatch.countDown();
				try {
					startLatch.await(); // Chờ phát súng xuất phát cùng 1 mili-giây
					transferService.confirm(senderUser.getId(), new TransferConfirmRequest(transactionId, OTP_CODE));
					successCount.incrementAndGet();
				} catch (ResponseStatusException rse) {
					failureCount.incrementAndGet();
					failureReasons.add(rse.getReason());
				} catch (Exception e) {
					failureCount.incrementAndGet();
					failureReasons.add(e.getMessage());
				} finally {
					finishLatch.countDown();
				}
			});
		}

		readyLatch.await(5, TimeUnit.SECONDS);
		startLatch.countDown(); // Bắn 2 request chạy song song cùng lúc
		finishLatch.await(10, TimeUnit.SECONDS);
		executor.shutdown();

		// Khẳng định 1: Chỉ duy nhất 1 giao dịch được phép thành công
		assertThat(successCount.get())
			.as("Chỉ duy nhất 1 giao dịch được phép thành công để tránh trừ âm tiền")
			.isEqualTo(1);

		// Khẳng định 2: Giao dịch còn lại phải thất bại do không đủ số dư
		assertThat(failureCount.get())
			.as("Giao dịch thứ 2 phải thất bại")
			.isEqualTo(1);

		assertThat(failureReasons.get(0))
			.as("Lý do thất bại phải là Không đủ số dư (Insufficient balance)")
			.contains("Insufficient balance");

		// Khẳng định 3: Kiểm tra trạng thái tài khoản trong Database
		Account updatedFromAccount = accountRepository.findById(fromAccount.getId()).orElseThrow();
		Account updatedToAccount = accountRepository.findById(toAccount.getId()).orElseThrow();

		assertThat(updatedFromAccount.getAvailableBalance())
			.as("Số dư tài khoản gửi phải về đúng 0 VND, tuyệt đối không được âm (-1.000.000 VND)")
			.isEqualByComparingTo(BigDecimal.ZERO);

		assertThat(updatedToAccount.getAvailableBalance())
			.as("Số dư tài khoản nhận chỉ được nhận đúng 1.000.000 VND")
			.isEqualByComparingTo(new BigDecimal("1000000"));
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