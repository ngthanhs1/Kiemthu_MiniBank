# -*- coding: utf-8 -*-
import os

AUTH_SERVICE = r"d:\Code_Hoc\Kiemthu_MiniBank\src\main\java\com\minibank\backend\auth\service\AuthService.java"
ADMIN_SEED = r"d:\Code_Hoc\Kiemthu_MiniBank\src\main\java\com\minibank\backend\admin\bootstrap\AdminSeedRunner.java"

# 1. Update AuthService to search by username or email
with open(AUTH_SERVICE, "r", encoding="utf-8") as f:
    auth_code = f.read()

old_auth = """\t@Transactional(readOnly = true)
\tpublic AuthResponse loginAdmin(LoginRequest request) {
\t\tString username = request.identifier().trim();
\t\tAdminUser adminUser = adminUsers().findByUsernameIgnoreCase(username)
\t\t\t.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));"""

new_auth = """\t@Transactional(readOnly = true)
\tpublic AuthResponse loginAdmin(LoginRequest request) {
\t\tString identifier = request.identifier().trim();
\t\tAdminUser adminUser = adminUsers().findByUsernameIgnoreCase(identifier)
\t\t\t.or(() -> adminUsers().findByEmailIgnoreCase(identifier))
\t\t\t.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));"""

if old_auth in auth_code:
    auth_code = auth_code.replace(old_auth, new_auth)
    with open(AUTH_SERVICE, "w", encoding="utf-8") as f:
        f.write(auth_code)
    print("Updated AuthService.java!")
else:
    print("Warning: old_auth pattern not matched!")

# 2. Update AdminSeedRunner to seed both 'admin' (admin123) and 'admin@gmail.com' (123456)
with open(ADMIN_SEED, "r", encoding="utf-8") as f:
    seed_code = f.read()

old_seed = """\t\tif (!adminUserRoleRepository.existsByAdminUserIdAndRoleId(adminUser.getId(), superAdminRole.getId())) {
\t\t\tadminUserRoleRepository.save(AdminUserRole.builder()
\t\t\t\t.adminUser(adminUser)
\t\t\t\t.role(superAdminRole)
\t\t\t\t.build());
\t\t}
\t}"""

new_seed = """\t\tif (!adminUserRoleRepository.existsByAdminUserIdAndRoleId(adminUser.getId(), superAdminRole.getId())) {
\t\t\tadminUserRoleRepository.save(AdminUserRole.builder()
\t\t\t\t.adminUser(adminUser)
\t\t\t\t.role(superAdminRole)
\t\t\t\t.build());
\t\t}

\t\t// Seed secondary admin account: 'admin' with password 'admin123'
\t\tAdminUser adminSimple = adminUserRepository.findByUsernameIgnoreCase("admin")
\t\t\t.orElseGet(() -> adminUserRepository.save(AdminUser.builder()
\t\t\t\t.username("admin")
\t\t\t\t.email("admin@minibank.com")
\t\t\t\t.passwordHash(passwordEncoder.encode("admin123"))
\t\t\t\t.fullName("System Administrator")
\t\t\t\t.status("active")
\t\t\t\t.build()));

\t\tif (!adminUserRoleRepository.existsByAdminUserIdAndRoleId(adminSimple.getId(), adminRole.getId())) {
\t\t\tadminUserRoleRepository.save(AdminUserRole.builder()
\t\t\t\t.adminUser(adminSimple)
\t\t\t\t.role(adminRole)
\t\t\t\t.build());
\t\t}
\t\tif (!adminUserRoleRepository.existsByAdminUserIdAndRoleId(adminSimple.getId(), superAdminRole.getId())) {
\t\t\tadminUserRoleRepository.save(AdminUserRole.builder()
\t\t\t\t.adminUser(adminSimple)
\t\t\t\t.role(superAdminRole)
\t\t\t\t.build());
\t\t}
\t}"""

if old_seed in seed_code:
    seed_code = seed_code.replace(old_seed, new_seed)
    with open(ADMIN_SEED, "w", encoding="utf-8") as f:
        f.write(seed_code)
    print("Updated AdminSeedRunner.java!")
else:
    print("Warning: old_seed pattern not matched!")
