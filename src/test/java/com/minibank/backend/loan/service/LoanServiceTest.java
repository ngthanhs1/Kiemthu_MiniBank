package com.minibank.backend.loan.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.minibank.backend.account.entity.Account;
import com.minibank.backend.account.repository.AccountRepository;
import com.minibank.backend.loan.dto.CreateLoanRequest;
import com.minibank.backend.loan.dto.LoanApplicationResponse;
import com.minibank.backend.loan.dto.LoanResponse;
import com.minibank.backend.loan.entity.Loan;
import com.minibank.backend.loan.entity.LoanApplication;
import com.minibank.backend.loan.entity.LoanProduct;
import com.minibank.backend.loan.repository.LoanApplicationRepository;
import com.minibank.backend.loan.repository.LoanProductRepository;
import com.minibank.backend.loan.repository.LoanRepaymentScheduleRepository;
import com.minibank.backend.loan.repository.LoanRepository;
import com.minibank.backend.user.entity.User;
import com.minibank.backend.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

	@Mock
	private LoanRepository loanRepository;

	@Mock
	private LoanApplicationRepository loanApplicationRepository;

	@Mock
	private LoanProductRepository loanProductRepository;

	@Mock
	private LoanRepaymentScheduleRepository loanRepaymentScheduleRepository;

	@Mock
	private AccountRepository accountRepository;

	@Mock
	private UserRepository userRepository;

	private LoanService loanService;

	private User applicant;
	private LoanProduct personalProduct;
	private Account userAccount;

	@BeforeEach
	void setUp() {
		loanService = new LoanService(
			loanRepository,
			loanApplicationRepository,
			loanProductRepository,
			loanRepaymentScheduleRepository,
			accountRepository,
			userRepository
		);

		applicant = User.builder()
			.id(1L)
			.phone("0981712585")
			.fullName("Nguyen Van A")
			.status("active")
			.customerRank("standard")
			.build();

		personalProduct = LoanProduct.builder()
			.id(10L)
			.code("PROD_PERSONAL_01")
			.name("Gói vay tiêu dùng cá nhân")
			.loanType("PERSONAL")
			.status("active")
			.minAmount(new BigDecimal("5000000"))   // 5M
			.maxAmount(new BigDecimal("100000000")) // 100M
			.minTermMonths(3)
			.maxTermMonths(36)
			.currency("VND")
			.build();

		userAccount = Account.builder()
			.id(100L)
			.accountNumber("1000001")
			.user(applicant)
			.status("active")
			.build();
	}

	// ──────────────────────────────────────────────────────────────────────────
	// TEST CASES: applyForLoan()
	// ──────────────────────────────────────────────────────────────────────────

	@Test
	@DisplayName("TC_LOAN_01: Nộp hồ sơ xin vay thành công với số tiền và kỳ hạn hợp lệ")
	void applyForLoan_Success() {
		CreateLoanRequest req = new CreateLoanRequest(
			10L, 100L, 100L,
			new BigDecimal("20000000"), // 20M
			12, // 12 tháng
			"Mua sam do gia dung",
			"unsecured",
			new BigDecimal("15000000"),
			null, null, null, null, null, null, null, null, null, null, null, null, null
		);

		when(userRepository.findById(1L)).thenReturn(Optional.of(applicant));
		when(loanProductRepository.findById(10L)).thenReturn(Optional.of(personalProduct));
		when(accountRepository.findById(100L)).thenReturn(Optional.of(userAccount));
		when(loanApplicationRepository.save(any(LoanApplication.class))).thenAnswer(inv -> {
			LoanApplication app = inv.getArgument(0);
			app.setId(1000L);
			return app;
		});

		LoanApplicationResponse response = loanService.applyForLoan(1L, req);

		assertThat(response).isNotNull();
		assertThat(response.id()).isEqualTo(1000L);
		assertThat(response.status()).isEqualTo("pending");
		assertThat(response.requestedAmount()).isEqualByComparingTo(new BigDecimal("20000000"));
		assertThat(response.requestedTermMonths()).isEqualTo(12);
		verify(loanApplicationRepository).save(any(LoanApplication.class));
	}

	@Test
	@DisplayName("TC_LOAN_ERR_01: Ném lỗi 401 khi không tìm thấy người dùng nộp đơn vay")
	void applyForLoan_UserNotFound() {
		CreateLoanRequest req = new CreateLoanRequest(
			10L, 100L, 100L, new BigDecimal("20000000"), 12, "Vay tien",
			null, null, null, null, null, null, null, null, null, null, null, null, null, null, null
		);
		when(userRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> loanService.applyForLoan(99L, req))
			.isInstanceOf(ResponseStatusException.class)
			.hasFieldOrPropertyWithValue("status", HttpStatus.UNAUTHORIZED);
	}

	@Test
	@DisplayName("TC_LOAN_ERR_02: Ném lỗi 400 khi sản phẩm vay không hoạt động (inactive)")
	void applyForLoan_ProductNotActive() {
		personalProduct.setStatus("disabled");
		CreateLoanRequest req = new CreateLoanRequest(
			10L, 100L, 100L, new BigDecimal("20000000"), 12, "Vay tien",
			null, null, null, null, null, null, null, null, null, null, null, null, null, null, null
		);
		when(userRepository.findById(1L)).thenReturn(Optional.of(applicant));
		when(loanProductRepository.findById(10L)).thenReturn(Optional.of(personalProduct));

		assertThatThrownBy(() -> loanService.applyForLoan(1L, req))
			.isInstanceOf(ResponseStatusException.class)
			.hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
			.hasMessageContaining("Loan product is not available");
	}

	@Test
	@DisplayName("TC_LOAN_ERR_03: Ném lỗi 400 khi số tiền vay thấp hơn mức tối thiểu (BVA)")
	void applyForLoan_AmountBelowMinimum() {
		CreateLoanRequest req = new CreateLoanRequest(
			10L, 100L, 100L, new BigDecimal("4999999"), 12, "Vay tien", // < 5M min
			null, null, null, null, null, null, null, null, null, null, null, null, null, null, null
		);
		when(userRepository.findById(1L)).thenReturn(Optional.of(applicant));
		when(loanProductRepository.findById(10L)).thenReturn(Optional.of(personalProduct));

		assertThatThrownBy(() -> loanService.applyForLoan(1L, req))
			.isInstanceOf(ResponseStatusException.class)
			.hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
			.hasMessageContaining("Amount must be between");
	}

	@Test
	@DisplayName("TC_LOAN_ERR_04: Ném lỗi 400 khi số tiền vay vượt quá mức tối đa (BVA)")
	void applyForLoan_AmountAboveMaximum() {
		CreateLoanRequest req = new CreateLoanRequest(
			10L, 100L, 100L, new BigDecimal("100000001"), 12, "Vay tien", // > 100M max
			null, null, null, null, null, null, null, null, null, null, null, null, null, null, null
		);
		when(userRepository.findById(1L)).thenReturn(Optional.of(applicant));
		when(loanProductRepository.findById(10L)).thenReturn(Optional.of(personalProduct));

		assertThatThrownBy(() -> loanService.applyForLoan(1L, req))
			.isInstanceOf(ResponseStatusException.class)
			.hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
			.hasMessageContaining("Amount must be between");
	}

	@Test
	@DisplayName("TC_LOAN_ERR_05: Ném lỗi 400 khi kỳ hạn vay dưới mức tối thiểu (BVA)")
	void applyForLoan_TermBelowMinimum() {
		CreateLoanRequest req = new CreateLoanRequest(
			10L, 100L, 100L, new BigDecimal("20000000"), 2, "Vay tien", // 2 tháng < 3 tháng min
			null, null, null, null, null, null, null, null, null, null, null, null, null, null, null
		);
		when(userRepository.findById(1L)).thenReturn(Optional.of(applicant));
		when(loanProductRepository.findById(10L)).thenReturn(Optional.of(personalProduct));

		assertThatThrownBy(() -> loanService.applyForLoan(1L, req))
			.isInstanceOf(ResponseStatusException.class)
			.hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
			.hasMessageContaining("Term must be between");
	}

	@Test
	@DisplayName("TC_LOAN_ERR_06: Ném lỗi 400 khi loại vay yêu cầu không khớp với sản phẩm")
	void applyForLoan_TypeMismatch() {
		CreateLoanRequest req = new CreateLoanRequest(
			10L, 100L, 100L, new BigDecimal("20000000"), 12, "Vay tien",
			"secured", // Mortage != PERSONAL
			null, null, null, null, null, null, null, null, null, null, null, null, null, null
		);
		when(userRepository.findById(1L)).thenReturn(Optional.of(applicant));
		when(loanProductRepository.findById(10L)).thenReturn(Optional.of(personalProduct));

		assertThatThrownBy(() -> loanService.applyForLoan(1L, req))
			.isInstanceOf(ResponseStatusException.class)
			.hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
			.hasMessageContaining("không khớp");
	}

	@Test
	@DisplayName("TC_LOAN_ERR_07: Ném lỗi 403 khi tài khoản giải ngân không thuộc về người nộp đơn")
	void applyForLoan_AccountNotBelongToUser() {
		User otherUser = User.builder().id(2L).build();
		Account otherAccount = Account.builder().id(200L).user(otherUser).build();

		CreateLoanRequest req = new CreateLoanRequest(
			10L, 200L, 100L, new BigDecimal("20000000"), 12, "Vay tien",
			null, null, null, null, null, null, null, null, null, null, null, null, null, null, null
		);
		when(userRepository.findById(1L)).thenReturn(Optional.of(applicant));
		when(loanProductRepository.findById(10L)).thenReturn(Optional.of(personalProduct));
		when(accountRepository.findById(200L)).thenReturn(Optional.of(otherAccount));
		when(accountRepository.findById(100L)).thenReturn(Optional.of(userAccount));

		assertThatThrownBy(() -> loanService.applyForLoan(1L, req))
			.isInstanceOf(ResponseStatusException.class)
			.hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);
	}

	// ──────────────────────────────────────────────────────────────────────────
	// TEST CASES: getLoans() & getLoan()
	// ──────────────────────────────────────────────────────────────────────────

	@Test
	@DisplayName("TC_LOAN_VIEW_01: Lấy danh sách khoản vay của người dùng thành công")
	void getLoans_Success() {
		Loan loan = Loan.builder()
			.id(50L)
			.code("LOAN-50")
			.user(applicant)
			.status("active")
			.approvedAmount(new BigDecimal("30000000"))
			.outstandingPrincipal(new BigDecimal("25000000"))
			.build();

		when(loanRepository.findByUserId(1L)).thenReturn(List.of(loan));
		when(loanRepaymentScheduleRepository.findByLoanIdOrderByInstallmentNo(50L)).thenReturn(List.of());

		List<LoanResponse> loans = loanService.getLoans(1L);

		assertThat(loans).hasSize(1);
		assertThat(loans.get(0).id()).isEqualTo(50L);
		assertThat(loans.get(0).code()).isEqualTo("LOAN-50");
	}

	@Test
	@DisplayName("TC_LOAN_VIEW_02: Ném lỗi 404 khi không tìm thấy khoản vay của người dùng")
	void getLoan_NotFound() {
		when(loanRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> loanService.getLoan(1L, 999L))
			.isInstanceOf(ResponseStatusException.class)
			.hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
	}
}
