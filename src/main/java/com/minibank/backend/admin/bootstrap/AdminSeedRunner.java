package com.minibank.backend.admin.bootstrap;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.minibank.backend.admin.entity.AdminUser;
import com.minibank.backend.admin.entity.AdminUserRole;
import com.minibank.backend.admin.entity.Role;
import com.minibank.backend.admin.repository.AdminUserRepository;
import com.minibank.backend.admin.repository.AdminUserRoleRepository;
import com.minibank.backend.admin.repository.RoleRepository;

@Component
public class AdminSeedRunner implements CommandLineRunner {
	private static final String DEFAULT_ADMIN_USERNAME = "admin@gmail.com";
	private static final String DEFAULT_ADMIN_EMAIL = "admin@gmail.com";
	private static final String DEFAULT_ADMIN_PASSWORD = "123456";
	private static final String DEFAULT_ADMIN_FULL_NAME = "System Admin";
	private static final String ADMIN_ROLE_CODE = "ADMIN";
	private static final String SUPER_ADMIN_ROLE_CODE = "SUPER_ADMIN";
	private static final String CUSTOMER_SUPPORT_ROLE_CODE = "CUSTOMER_SUPPORT";
	private static final String KYC_OFFICER_ROLE_CODE = "KYC_OFFICER";
	private static final String SERVICE_OFFICER_ROLE_CODE = "SERVICE_OFFICER";
	private static final String LOAN_OFFICER_ROLE_CODE = "LOAN_OFFICER";

	private final AdminUserRepository adminUserRepository;
	private final RoleRepository roleRepository;
	private final AdminUserRoleRepository adminUserRoleRepository;
	private final PasswordEncoder passwordEncoder;

	public AdminSeedRunner(
		AdminUserRepository adminUserRepository,
		RoleRepository roleRepository,
		AdminUserRoleRepository adminUserRoleRepository,
		PasswordEncoder passwordEncoder
	) {
		this.adminUserRepository = adminUserRepository;
		this.roleRepository = roleRepository;
		this.adminUserRoleRepository = adminUserRoleRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	@Transactional
	public void run(String... args) {
		Role adminRole = roleRepository.findByCode(ADMIN_ROLE_CODE)
			.orElseGet(() -> roleRepository.save(Role.builder()
				.code(ADMIN_ROLE_CODE)
				.name("Administrator")
				.description("Legacy full system access")
				.build()));
		Role superAdminRole = upsertRole(SUPER_ADMIN_ROLE_CODE, "Super Admin", "Quyền cao nhất trên hệ thống MiniBank");
		upsertRole(CUSTOMER_SUPPORT_ROLE_CODE, "Nhân viên CSKH", "Nhận và xử lý cuộc chat chuyển tiếp từ AI");
		upsertRole(KYC_OFFICER_ROLE_CODE, "Nhân viên KYC", "Xem, phê duyệt và từ chối hồ sơ KYC");
		upsertRole(SERVICE_OFFICER_ROLE_CODE, "Nhân viên Thủ tục", "Xử lý yêu cầu dịch vụ, hạn mức và tiết kiệm");
		upsertRole(LOAN_OFFICER_ROLE_CODE, "Nhân viên Tín dụng", "Thẩm định, duyệt vay và theo dõi khoản vay");

		AdminUser adminUser = adminUserRepository.findByUsernameIgnoreCase(DEFAULT_ADMIN_USERNAME)
			.orElseGet(() -> adminUserRepository.save(AdminUser.builder()
				.username(DEFAULT_ADMIN_USERNAME)
				.email(DEFAULT_ADMIN_EMAIL)
				.passwordHash(passwordEncoder.encode(DEFAULT_ADMIN_PASSWORD))
				.fullName(DEFAULT_ADMIN_FULL_NAME)
				.status("active")
				.build()));

		if (!adminUserRoleRepository.existsByAdminUserIdAndRoleId(adminUser.getId(), adminRole.getId())) {
			adminUserRoleRepository.save(AdminUserRole.builder()
				.adminUser(adminUser)
				.role(adminRole)
				.build());
		}
		if (!adminUserRoleRepository.existsByAdminUserIdAndRoleId(adminUser.getId(), superAdminRole.getId())) {
			adminUserRoleRepository.save(AdminUserRole.builder()
				.adminUser(adminUser)
				.role(superAdminRole)
				.build());
		}
	}

	private Role upsertRole(String code, String name, String description) {
		return roleRepository.findByCode(code)
			.orElseGet(() -> roleRepository.save(Role.builder()
				.code(code)
				.name(name)
				.description(description)
				.build()));
	}
}
