package com.minibank.backend.admin.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.minibank.backend.admin.dto.AdminStaffRequest;
import com.minibank.backend.admin.dto.AdminStaffResponse;
import com.minibank.backend.admin.entity.AdminUser;
import com.minibank.backend.admin.entity.AdminUserRole;
import com.minibank.backend.admin.entity.Role;
import com.minibank.backend.admin.repository.AdminUserRepository;
import com.minibank.backend.admin.repository.AdminUserRoleRepository;
import com.minibank.backend.admin.repository.RoleRepository;

@Service
public class AdminStaffService {
	private static final String DEFAULT_ROLE = "CUSTOMER_SUPPORT";
	private static final Map<String, String> ROLE_NAMES = Map.ofEntries(
		Map.entry("SUPER_ADMIN", "Super Admin"),
		Map.entry("ADMIN", "Legacy Administrator"),
		Map.entry("CUSTOMER_SUPPORT", "Nhân viên CSKH"),
		Map.entry("KYC_OFFICER", "Nhân viên KYC"),
		Map.entry("SERVICE_OFFICER", "Nhân viên Thủ tục"),
		Map.entry("LOAN_OFFICER", "Nhân viên Tín dụng")
	);

	private final AdminUserRepository adminUserRepository;
	private final AdminUserRoleRepository adminUserRoleRepository;
	private final RoleRepository roleRepository;
	private final PasswordEncoder passwordEncoder;

	public AdminStaffService(
		AdminUserRepository adminUserRepository,
		AdminUserRoleRepository adminUserRoleRepository,
		RoleRepository roleRepository,
		PasswordEncoder passwordEncoder
	) {
		this.adminUserRepository = adminUserRepository;
		this.adminUserRoleRepository = adminUserRoleRepository;
		this.roleRepository = roleRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional(readOnly = true)
	public List<AdminStaffResponse> list(String q) {
		String query = q == null ? null : q.trim().toLowerCase();
		return adminUserRepository.findAll().stream()
			.filter(u -> {
				if (query == null || query.isBlank()) return true;
				String username = u.getUsername() == null ? "" : u.getUsername().toLowerCase();
				String email = u.getEmail() == null ? "" : u.getEmail().toLowerCase();
				String name = u.getFullName() == null ? "" : u.getFullName().toLowerCase();
				return username.contains(query) || email.contains(query) || name.contains(query);
			})
			.map(this::toResponse)
			.toList();
	}

	@Transactional
	public AdminStaffResponse create(AdminStaffRequest request) {
		String username = request.username().trim();
		String email = request.email().trim().toLowerCase();

		if (adminUserRepository.existsByUsernameIgnoreCase(username)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
		}
		if (adminUserRepository.existsByEmailIgnoreCase(email)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
		}

		AdminUser adminUser = AdminUser.builder()
			.username(username)
			.email(email)
			.passwordHash(passwordEncoder.encode(request.password()))
			.fullName(request.fullName().trim())
			.status("active")
			.build();
		AdminUser saved = adminUserRepository.save(adminUser);

		Set<String> requestedRoles = normalizeRoles(request.roles());
		if (requestedRoles.isEmpty()) {
			requestedRoles = Set.of(DEFAULT_ROLE);
		}

		for (String code : requestedRoles) {
			Role role = roleRepository.findByCode(code)
				.orElseGet(() -> roleRepository.save(Role.builder()
					.code(code)
					.name(ROLE_NAMES.getOrDefault(code, code))
					.description("Auto-created role")
					.build()));

			if (!adminUserRoleRepository.existsByAdminUserIdAndRoleId(saved.getId(), role.getId())) {
				adminUserRoleRepository.save(AdminUserRole.builder()
					.adminUser(saved)
					.role(role)
					.build());
			}
		}

		return toResponse(saved);
	}

	@Transactional
	public AdminStaffResponse updateRoles(Long adminUserId, List<String> roles) {
		AdminUser adminUser = adminUserRepository.findById(adminUserId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin user not found"));

		Set<String> requestedRoles = normalizeRoles(roles);
		if (requestedRoles.isEmpty()) {
			requestedRoles = Set.of(DEFAULT_ROLE);
		}

		adminUserRoleRepository.deleteByAdminUserId(adminUserId);
		for (String code : requestedRoles) {
			Role role = roleRepository.findByCode(code)
				.orElseGet(() -> roleRepository.save(Role.builder()
					.code(code)
					.name(ROLE_NAMES.getOrDefault(code, code))
					.description("Auto-created role")
					.build()));

			adminUserRoleRepository.save(AdminUserRole.builder()
				.adminUser(adminUser)
				.role(role)
				.build());
		}

		return toResponse(adminUser);
	}

	@Transactional
	public AdminStaffResponse updateStatus(Long adminUserId, String status) {
		if (status == null || status.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status is required");
		}

		AdminUser adminUser = adminUserRepository.findById(adminUserId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin user not found"));

		String normalized = status.trim().toLowerCase();
		if (!normalized.equals("active") && !normalized.equals("blocked")) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status");
		}
		adminUser.setStatus(normalized);
		adminUserRepository.save(adminUser);
		return toResponse(adminUser);
	}

	private Set<String> normalizeRoles(List<String> roles) {
		Set<String> normalized = new LinkedHashSet<>();
		if (roles == null) return normalized;
		for (String role : roles) {
			if (role == null) continue;
			String trimmed = role.trim();
			if (!trimmed.isBlank()) {
				normalized.add(trimmed.toUpperCase());
			}
		}
		return normalized;
	}

	private AdminStaffResponse toResponse(AdminUser adminUser) {
		List<String> roles = new ArrayList<>(adminUserRoleRepository.findRoleCodesByAdminUserId(adminUser.getId()));
		return new AdminStaffResponse(
			adminUser.getId(),
			adminUser.getUsername(),
			adminUser.getEmail(),
			adminUser.getFullName(),
			adminUser.getStatus(),
			roles,
			adminUser.getCreatedAt()
		);
	}
}
