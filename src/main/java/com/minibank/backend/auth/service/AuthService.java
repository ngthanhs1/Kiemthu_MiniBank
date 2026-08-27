package com.minibank.backend.auth.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.minibank.backend.admin.entity.AdminUser;
import com.minibank.backend.admin.entity.Role;
import com.minibank.backend.admin.repository.AdminUserRepository;
import com.minibank.backend.admin.repository.AdminUserRoleRepository;
import com.minibank.backend.admin.repository.RoleRepository;
import com.minibank.backend.auth.dto.AdminRegisterRequest;
import com.minibank.backend.auth.dto.AuthOtpSendResponse;
import com.minibank.backend.auth.dto.AuthResponse;
import com.minibank.backend.auth.dto.LoginOtpSendRequest;
import com.minibank.backend.auth.dto.LoginRequest;
import com.minibank.backend.auth.dto.LoginVerifyRequest;
import com.minibank.backend.auth.dto.PasswordResetOtpSendRequest;
import com.minibank.backend.auth.dto.PasswordResetVerifyRequest;
import com.minibank.backend.auth.dto.PinResetVerifyRequest;
import com.minibank.backend.auth.dto.UserRegisterRequest;
import com.minibank.backend.common.otp.SmsOtpService;
import com.minibank.backend.user.entity.User;
import com.minibank.backend.user.repository.UserRepository;

@Service
public class AuthService {
	private static final Logger log = LoggerFactory.getLogger(AuthService.class);

	private final ObjectProvider<AdminUserRepository> adminUserRepository;
	private final ObjectProvider<AdminUserRoleRepository> adminUserRoleRepository;
	private final ObjectProvider<RoleRepository> roleRepository;
	private final ObjectProvider<UserRepository> userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenService jwtTokenService;
	private final SmsOtpService smsOtpService;

	public AuthService(
		ObjectProvider<AdminUserRepository> adminUserRepository,
		ObjectProvider<AdminUserRoleRepository> adminUserRoleRepository,
		ObjectProvider<RoleRepository> roleRepository,
		ObjectProvider<UserRepository> userRepository,
		PasswordEncoder passwordEncoder,
		JwtTokenService jwtTokenService,
		SmsOtpService smsOtpService
	) {
		this.adminUserRepository = adminUserRepository;
		this.adminUserRoleRepository = adminUserRoleRepository;
		this.roleRepository = roleRepository;
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtTokenService = jwtTokenService;
		this.smsOtpService = smsOtpService;
	}

	private AdminUserRepository adminUsers() {
		AdminUserRepository repo = adminUserRepository.getIfAvailable();
		if (repo == null) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Auth is not available (database disabled)");
		}
		return repo;
	}

	private AdminUserRoleRepository adminUserRoles() {
		AdminUserRoleRepository repo = adminUserRoleRepository.getIfAvailable();
		if (repo == null) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Auth is not available (database disabled)");
		}
		return repo;
	}

	private RoleRepository roles() {
		RoleRepository repo = roleRepository.getIfAvailable();
		if (repo == null) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Auth is not available (database disabled)");
		}
		return repo;
	}

	private UserRepository users() {
		UserRepository repo = userRepository.getIfAvailable();
		if (repo == null) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Auth is not available (database disabled)");
		}
		return repo;
	}

	@Transactional
	public AuthResponse registerAdmin(AdminRegisterRequest request) {
		String username = request.username().trim();
		String email = request.email().trim().toLowerCase();

		if (adminUsers().existsByUsernameIgnoreCase(username)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
		}
		if (adminUsers().existsByEmailIgnoreCase(email)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
		}

		AdminUser adminUser = AdminUser.builder()
			.username(username)
			.email(email)
			.passwordHash(passwordEncoder.encode(request.password()))
			.fullName(request.fullName().trim())
			.status("active")
			.build();

		AdminUser saved = adminUsers().save(adminUser);

		List<String> roles = List.of("ADMIN");
		List<String> permissions = resolvePermissions(roles);
		JwtTokenService.IssuedToken token = jwtTokenService.issueAccessToken(saved.getId(), "ADMIN", saved.getUsername(), roles, permissions);

		return new AuthResponse(
			"Bearer",
			token.token(),
			token.expiresInSeconds(),
			new AuthResponse.UserInfo(saved.getId(), "ADMIN", saved.getUsername(), null, roles, permissions)
		);
	}

	@Transactional(readOnly = true)
	public AuthResponse loginAdmin(LoginRequest request) {
		String username = request.identifier().trim();
		AdminUser adminUser = adminUsers().findByUsernameIgnoreCase(username)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

		if (!passwordEncoder.matches(request.password(), adminUser.getPasswordHash())) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
		}
		if (adminUser.getStatus() != null && !adminUser.getStatus().equalsIgnoreCase("active")) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is not active");
		}

		List<String> roles = adminUserRoles().findRoleCodesByAdminUserId(adminUser.getId());
		if (roles == null || roles.isEmpty()) {
			roles = List.of("ADMIN");
		}

		List<String> permissions = resolvePermissions(roles);
		JwtTokenService.IssuedToken token = jwtTokenService.issueAccessToken(adminUser.getId(), "ADMIN", adminUser.getUsername(), roles, permissions);
		return new AuthResponse(
			"Bearer",
			token.token(),
			token.expiresInSeconds(),
			new AuthResponse.UserInfo(adminUser.getId(), "ADMIN", adminUser.getUsername(), null, roles, permissions)
		);
	}

	@Transactional
	public AuthResponse registerUser(UserRegisterRequest request) {
		String phone = request.phone().trim();
		String email = request.email().trim().toLowerCase();
		log.info("registerUser: phone={}, email={}"
			, phone, email);

		if (users().existsByPhone(phone)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone already exists");
		}
		if (users().existsByEmailIgnoreCase(email)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
		}

		User user = User.builder()
			.phone(phone)
			.email(email)
			.passwordHash(passwordEncoder.encode(request.password()))
			.fullName(request.fullName() == null ? null : request.fullName().trim())
			.status("pending")
			.customerRank("dong")
			.deviceId(request.deviceId() == null ? null : request.deviceId().trim())
			.publicKey(request.publicKey())
			.build();

		User saved = users().save(user);

		List<String> roles = List.of("USER");
		List<String> permissions = List.of();
		JwtTokenService.IssuedToken token = jwtTokenService.issueAccessToken(
			saved.getId(),
			"USER",
			saved.getPhone(),
			roles,
			permissions,
			saved.getDeviceId()
		);
		return new AuthResponse(
			"Bearer",
			token.token(),
			token.expiresInSeconds(),
			new AuthResponse.UserInfo(saved.getId(), "USER", null, saved.getPhone(), roles, permissions)
		);
	}

	@Transactional(readOnly = true)
	public AuthOtpSendResponse sendLoginOtp(LoginOtpSendRequest request) {
		String phone = request.identifier().trim();
		String deviceId = request.deviceId() == null ? null : request.deviceId().trim();
		log.info("sendLoginOtp: phone={}, deviceIdPresent={}", phone, deviceId != null && !deviceId.isBlank());
		if (deviceId == null || deviceId.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "deviceId is required");
		}

		User user = users().findByPhone(phone)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
		}
		if (user.getStatus() != null && user.getStatus().equalsIgnoreCase("blocked")) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is blocked");
		}

		SmsOtpService.OtpSendResult result = smsOtpService.sendOtp(phone);
		return new AuthOtpSendResponse(result.devMode(), result.otp());
	}

	@Transactional
	public AuthResponse verifyLogin(LoginVerifyRequest request) {
		String phone = request.identifier().trim();
		String deviceId = request.deviceId() == null ? null : request.deviceId().trim();
		if (deviceId == null || deviceId.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "deviceId is required");
		}

		User user = users().findByPhone(phone)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

		if (user.getStatus() != null && user.getStatus().equalsIgnoreCase("blocked")) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is blocked");
		}

		boolean otpOk = smsOtpService.verifyOtp(user.getPhone(), request.otpCode());
		if (!otpOk) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid OTP");
		}

		// Override device to enforce single-device policy.
		user.setDeviceId(deviceId);
		if (request.publicKey() != null && !request.publicKey().isBlank()) {
			user.setPublicKey(request.publicKey());
		}
		users().save(user);

		List<String> roles = List.of("USER");
		List<String> permissions = List.of();
		JwtTokenService.IssuedToken token = jwtTokenService.issueAccessToken(
			user.getId(),
			"USER",
			user.getPhone(),
			roles,
			permissions,
			user.getDeviceId()
		);
		return new AuthResponse(
			"Bearer",
			token.token(),
			token.expiresInSeconds(),
			new AuthResponse.UserInfo(user.getId(), "USER", null, user.getPhone(), roles, permissions)
		);
	}

	@Transactional(readOnly = true)
	public void verifyPin(long userId, String pin) {
		User user = users().findById(userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
		if (user.getTransactionPinHash() == null || user.getTransactionPinHash().isBlank()) {
			throw new ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED, "Transaction PIN is not set");
		}
		if (!passwordEncoder.matches(pin, user.getTransactionPinHash())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid PIN");
		}
	}

	@Transactional(readOnly = true)
	public AuthOtpSendResponse sendPasswordResetOtp(PasswordResetOtpSendRequest request) {
		String phone = request.identifier().trim();
		log.info("sendPasswordResetOtp: phone={}", phone);
		
		// Check if user exists
		users().findByPhone(phone)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

		// Send OTP to user's email
		SmsOtpService.OtpSendResult result = smsOtpService.sendOtp(phone);
		return new AuthOtpSendResponse(result.devMode(), result.otp());
	}

	@Transactional
	public void resetPassword(PasswordResetVerifyRequest request) {
		String phone = request.identifier().trim();
		log.info("resetPassword: phone={}", phone);
		
		User user = users().findByPhone(phone)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

		// Verify OTP
		boolean otpOk = smsOtpService.verifyOtp(user.getPhone(), request.otpCode());
		if (!otpOk) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid OTP");
		}

		// Update password
		user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
		users().save(user);
	}

	@Transactional(readOnly = true)
	public AuthOtpSendResponse sendPinResetOtp(long userId) {
		log.info("sendPinResetOtp: userId={}", userId);
		
		User user = users().findById(userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

		// Send OTP to user's email
		SmsOtpService.OtpSendResult result = smsOtpService.sendOtp(user.getPhone());
		return new AuthOtpSendResponse(result.devMode(), result.otp());
	}

	@Transactional
	public void resetPin(long userId, PinResetVerifyRequest request) {
		log.info("resetPin: userId={}", userId);
		
		User user = users().findById(userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

		// Verify OTP
		boolean otpOk = smsOtpService.verifyOtp(user.getPhone(), request.otpCode());
		if (!otpOk) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid OTP");
		}

		// Update PIN
		user.setTransactionPinHash(passwordEncoder.encode(request.newPin()));
		users().save(user);
	}

	private List<String> resolvePermissions(List<String> roleCodes) {
		if (roleCodes == null || roleCodes.isEmpty()) {
			return List.of();
		}

		java.util.LinkedHashSet<String> permissions = new java.util.LinkedHashSet<>();
		for (String code : roleCodes) {
			if (code == null || code.isBlank()) {
				continue;
			}
			Role role = roles().findByCode(code.trim()).orElse(null);
			if (role == null) {
				continue;
			}
			permissions.addAll(parsePermissions(role.getPermissionsJson()));
		}
		return List.copyOf(permissions);
	}

	private List<String> parsePermissions(String value) {
		if (value == null || value.isBlank()) {
			return List.of();
		}
		return java.util.Arrays.stream(value.split("\\r?\\n"))
			.map(String::trim)
			.filter(item -> !item.isBlank())
			.toList();
	}

	@Transactional
	public AuthResponse loginUser(LoginRequest request) {
		// Legacy login endpoint (deprecated). Use OTP + PIN flow instead.
		throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use OTP login flow");
	}
}
