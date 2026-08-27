package com.minibank.backend.system.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.LinkedHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.minibank.backend.admin.entity.Role;
import com.minibank.backend.admin.repository.AdminUserRoleRepository;
import com.minibank.backend.admin.repository.PermissionDefinitionRepository;
import com.minibank.backend.admin.repository.RoleRepository;
import com.minibank.backend.admin.entity.PermissionDefinition;
import com.minibank.backend.system.entity.SystemLog;
import com.minibank.backend.system.service.SystemLogService;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class SystemAdminController {

    private final SystemLogService systemLogService;
    private final AdminUserRoleRepository adminUserRoleRepository;
    private final RoleRepository roleRepository;
    private final PermissionDefinitionRepository permissionDefinitionRepository;

    public SystemAdminController(
            SystemLogService systemLogService,
            AdminUserRoleRepository adminUserRoleRepository,
            RoleRepository roleRepository,
            PermissionDefinitionRepository permissionDefinitionRepository
    ) {
        this.systemLogService = systemLogService;
        this.adminUserRoleRepository = adminUserRoleRepository;
        this.roleRepository = roleRepository;
        this.permissionDefinitionRepository = permissionDefinitionRepository;
    }

    @GetMapping("/audit-logs")
    public List<SystemLog> getAuditLogs() {
        return systemLogService.getAllLogs();
    }

    @GetMapping("/roles")
    public List<Map<String, Object>> getRoles() {
        seedRoleDefinitions();
        return roleRepository.findAll().stream()
                .filter(role -> role.getCode() != null && !role.getCode().equalsIgnoreCase("ADMIN"))
                .sorted((a, b) -> a.getCode().compareToIgnoreCase(b.getCode()))
                .map(this::toRoleMap)
                .toList();
    }

    @GetMapping("/permissions")
    public List<Map<String, Object>> getPermissions() {
        seedPermissionDefinitions();
        return permissionDefinitionRepository.findAllByOrderByTabGroupAscSortOrderAscLabelAsc().stream()
                .map(this::toPermissionMap)
                .toList();
    }

    @PostMapping("/permissions")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createPermission(@RequestBody PermissionRequest request) {
        String code = normalizeCode(request.code());
        if (permissionDefinitionRepository.findByCode(code).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Permission code already exists");
        }
        PermissionDefinition permission = new PermissionDefinition();
        permission.setCode(code);
        apply(permission, request);
        return toPermissionMap(permissionDefinitionRepository.save(permission));
    }

    @PutMapping("/permissions/{code}")
    public Map<String, Object> updatePermission(@PathVariable String code, @RequestBody PermissionRequest request) {
        PermissionDefinition permission = permissionDefinitionRepository.findByCode(normalizeCode(code))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Permission not found"));
        apply(permission, request);
        return toPermissionMap(permissionDefinitionRepository.save(permission));
    }

    @DeleteMapping("/permissions/{code}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePermission(@PathVariable String code) {
        PermissionDefinition permission = permissionDefinitionRepository.findByCode(normalizeCode(code))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Permission not found"));
        permissionDefinitionRepository.delete(permission);
    }

    @PostMapping("/roles")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createRole(@RequestBody RoleRequest request) {
        String code = normalizeCode(request.code());
        if (roleRepository.findByCode(code).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Role code already exists");
        }
        Role role = new Role();
        role.setCode(code);
        apply(role, request);
        return toRoleMap(roleRepository.save(role));
    }

    @PutMapping("/roles/{code}")
    public Map<String, Object> updateRole(@PathVariable String code, @RequestBody RoleRequest request) {
        Role role = roleRepository.findByCode(normalizeCode(code))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));
        apply(role, request);
        return toRoleMap(roleRepository.save(role));
    }

    @DeleteMapping("/roles/{code}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRole(@PathVariable String code) {
        Role role = roleRepository.findByCode(normalizeCode(code))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));
        long userCount = adminUserRoleRepository.countUsersByRoleCode(role.getCode());
        if (userCount > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete role assigned to employees");
        }
        roleRepository.delete(role);
    }

    private void apply(Role role, RoleRequest request) {
        role.setName(blankToDefault(request.name(), role.getCode()));
        role.setDescription(request.description() == null ? "" : request.description().trim());
        role.setColor(blankToDefault(request.color(), "blue"));
        role.setPermissionsJson(String.join("\n", request.permissions() == null ? List.of() : request.permissions()));
    }

    private void apply(PermissionDefinition permission, PermissionRequest request) {
        permission.setLabel(blankToDefault(request.label(), request.code()));
        permission.setTabGroup(blankToDefault(request.tabGroup(), "Khác"));
        permission.setDescription(request.description() == null ? "" : request.description().trim());
        Integer sortOrder = request.sortOrder();
        permission.setSortOrder(sortOrder == null ? 100 : sortOrder);
        permission.setActive(request.active() == null || request.active());
    }

    private Map<String, Object> toRoleMap(Role role) {
        return Map.of(
                "name", role.getName(),
                "code", role.getCode(),
                "description", role.getDescription() == null ? "" : role.getDescription(),
                "totalUsers", adminUserRoleRepository.countUsersByRoleCode(role.getCode()),
                "color", role.getColor() == null || role.getColor().isBlank() ? "blue" : role.getColor(),
                "permissions", parsePermissions(role.getPermissionsJson()));
    }

    private Map<String, Object> toPermissionMap(PermissionDefinition permission) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("code", permission.getCode());
        data.put("label", permission.getLabel());
        data.put("tabGroup", permission.getTabGroup());
        data.put("description", permission.getDescription() == null ? "" : permission.getDescription());
        data.put("sortOrder", permission.getSortOrder());
        data.put("active", permission.isActive());
        data.put("createdAt", permission.getCreatedAt());
        data.put("updatedAt", permission.getUpdatedAt());
        return data;
    }

    private List<String> parsePermissions(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split("\\r?\\n"))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }

    private void seedRoleDefinitions() {
        upsert("SUPER_ADMIN", "Super Admin", "Quản trị hệ thống, nhân viên, log, AI và sản phẩm tài chính", "red",
                List.of("Quản lý nhân viên", "Gán vai trò", "Khóa/mở khóa nhân viên", "Xem dashboard", "Xem log hệ thống", "Cấu hình AI", "Cấu hình sản phẩm vay và tiết kiệm"));
        upsert("CUSTOMER_SUPPORT", "Nhân viên CSKH", "Nhận chat từ AI và trả lời khách hàng", "blue",
                List.of("Nhận cuộc chat chuyển tiếp", "Trả lời khách hàng", "Xem thông tin khách khi chat", "Ghi chú phiên chat", "Ưu tiên khách VIP"));
        upsert("KYC_OFFICER", "Nhân viên KYC", "Xử lý xác minh khách hàng", "emerald",
                List.of("Xem hồ sơ KYC", "Phê duyệt KYC", "Từ chối KYC", "Xem tài liệu CCCD"));
        upsert("SERVICE_OFFICER", "Nhân viên Thủ tục", "Xử lý yêu cầu dịch vụ, hạn mức và tiết kiệm", "amber",
                List.of("Đổi thông tin cá nhân", "Đổi thông tin tài khoản", "Nâng hạn mức", "Mở/tất toán tiết kiệm", "Xử lý yêu cầu dịch vụ"));
        upsert("LOAN_OFFICER", "Nhân viên Tín dụng", "Thẩm định và xử lý khoản vay", "violet",
                List.of("Xem hồ sơ vay", "Thẩm định hồ sơ", "Duyệt/từ chối vay", "Theo dõi nợ", "Nhắc nợ"));
    }

    private void seedPermissionDefinitions() {
        upsertPermission("CUSTOMER_VIEW", "Xem danh sách khách hàng", "Khách hàng", "Xem danh sách khách hàng và thông tin cơ bản", 10, true);
        upsertPermission("CUSTOMER_KYC_VIEW", "Xem hồ sơ KYC", "Khách hàng", "Xem chi tiết hồ sơ KYC của khách hàng", 20, true);
        upsertPermission("CUSTOMER_KYC_APPROVE", "Phê duyệt KYC", "Khách hàng", "Phê duyệt hồ sơ KYC hợp lệ", 30, true);
        upsertPermission("CUSTOMER_KYC_REJECT", "Từ chối KYC", "Khách hàng", "Từ chối hồ sơ KYC không hợp lệ", 40, true);
        upsertPermission("CUSTOMER_DOCUMENT_VIEW", "Xem tài liệu khách hàng", "Khách hàng", "Xem tài liệu định danh và hồ sơ đính kèm", 50, true);
        upsertPermission("TRANSACTION_CLASSIFY", "Phân loại giao dịch", "Tài khoản & Giao dịch", "Phân loại giao dịch phát sinh", 10, true);
        upsertPermission("TRANSACTION_LARGE_APPROVAL", "Duyệt giao dịch lớn", "Tài khoản & Giao dịch", "Phê duyệt hoặc từ chối giao dịch giá trị lớn", 20, true);
        upsertPermission("SAVING_PRODUCT_MANAGE", "Quản lý sản phẩm tiết kiệm", "Sản phẩm tài chính", "Tạo và chỉnh sửa sản phẩm tiết kiệm", 10, true);
        upsertPermission("SAVING_TIER_MANAGE", "Quản lý bậc tiết kiệm", "Sản phẩm tài chính", "Cấu hình bậc lãi suất tiết kiệm", 20, true);
        upsertPermission("SAVING_ACCOUNT_VIEW", "Xem sổ tiết kiệm", "Sản phẩm tài chính", "Xem danh sách và chi tiết sổ tiết kiệm", 30, true);
        upsertPermission("SAVING_APPROVAL", "Duyệt sổ tiết kiệm", "Yêu cầu thủ tục", "Duyệt các yêu cầu mở/tất toán tiết kiệm", 10, true);
        upsertPermission("LOAN_PRODUCT_MANAGE", "Quản lý sản phẩm vay", "Sản phẩm tài chính", "Tạo và chỉnh sửa sản phẩm vay", 40, true);
        upsertPermission("LOAN_TIER_MANAGE", "Quản lý bậc vay", "Sản phẩm tài chính", "Cấu hình bậc lãi suất vay", 50, true);
        upsertPermission("LOAN_APPLICATION_APPROVAL", "Duyệt vay vốn", "Yêu cầu thủ tục", "Duyệt hồ sơ vay vốn", 20, true);
        upsertPermission("LIMIT_REQUEST_APPROVAL", "Duyệt yêu cầu tăng hạn mức", "Yêu cầu thủ tục", "Duyệt yêu cầu tăng hạn mức của khách hàng", 30, true);
        upsertPermission("PROFILE_REQUEST_APPROVAL", "Duyệt yêu cầu đổi thông tin", "Yêu cầu thủ tục", "Duyệt yêu cầu đổi thông tin cá nhân", 40, true);
        upsertPermission("CHAT_CONVERSATION_MANAGE", "Quản lý chat CSKH", "Hỗ trợ khách hàng", "Nhận và xử lý cuộc chat CSKH", 10, true);
        upsertPermission("FAQ_MANAGE", "Quản lý FAQ", "Hỗ trợ khách hàng", "Thêm, sửa, xóa cây FAQ", 20, true);
        upsertPermission("CONTRACT_TEMPLATE_MANAGE", "Quản lý template hợp đồng", "Hợp đồng & Thỏa thuận", "Thêm, sửa, xóa template hợp đồng", 10, true);
        upsertPermission("CONTRACT_LIST_VIEW", "Xem tài liệu đã sinh", "Hợp đồng & Thỏa thuận", "Xem danh sách tài liệu hợp đồng đã sinh", 20, true);
        upsertPermission("STAFF_MANAGE", "Quản lý nhân viên", "Quản trị hệ thống", "Tạo, sửa, xóa và gán quyền nhân viên", 10, true);
        upsertPermission("ROLE_MANAGE", "Quản lý vai trò", "Quản trị hệ thống", "Tạo, sửa, xóa vai trò", 20, true);
        upsertPermission("PERMISSION_MANAGE", "Quản lý quyền", "Quản trị hệ thống", "Tạo, sửa, xóa quyền và tab quyền", 30, true);
        upsertPermission("APPROVAL_POLICY_MANAGE", "Quản lý mức duyệt nghiệp vụ", "Quản trị hệ thống", "Tạo, sửa, xóa cấu hình duyệt đa bước", 40, true);
        upsertPermission("SYSTEM_AUDIT_VIEW", "Xem nhật ký hệ thống", "Quản trị hệ thống", "Xem nhật ký và sự kiện hệ thống", 50, true);
    }

    private void upsertPermission(String code, String label, String tabGroup, String description, int sortOrder, boolean active) {
        PermissionDefinition permission = permissionDefinitionRepository.findByCode(code).orElse(null);
        if (permission == null) {
            permission = new PermissionDefinition();
            permission.setCode(code);
            permission.setLabel(label);
            permission.setTabGroup(tabGroup);
            permission.setDescription(description);
            permission.setSortOrder(sortOrder);
            permission.setActive(active);
            permissionDefinitionRepository.save(permission);
            return;
        }
        boolean changed = false;
        if (permission.getLabel() == null || permission.getLabel().isBlank()) {
            permission.setLabel(label);
            changed = true;
        }
        if (permission.getTabGroup() == null || permission.getTabGroup().isBlank()) {
            permission.setTabGroup(tabGroup);
            changed = true;
        }
        if (permission.getDescription() == null || permission.getDescription().isBlank()) {
            permission.setDescription(description);
            changed = true;
        }
        if (permission.getSortOrder() == 0) {
            permission.setSortOrder(sortOrder);
            changed = true;
        }
        if (!permission.isActive()) {
            permission.setActive(active);
            changed = true;
        }
        if (changed) {
            permissionDefinitionRepository.save(permission);
        }
    }

	private void upsert(String code, String name, String description, String color, List<String> permissions) {
		Role role = roleRepository.findByCode(code).orElse(null);
		if (role == null) {
			role = new Role();
			role.setCode(code);
			role.setName(name);
			role.setDescription(description);
			role.setColor(color);
			role.setPermissionsJson(String.join("\n", permissions));
			roleRepository.save(role);
			return;
		}
		boolean changed = false;
		if (role.getColor() == null || role.getColor().isBlank()) {
			role.setColor(color);
			changed = true;
		}
		if (role.getPermissionsJson() == null || role.getPermissionsJson().isBlank()) {
			role.setPermissionsJson(String.join("\n", permissions));
			changed = true;
		}
		if (changed) {
			roleRepository.save(role);
		}
	}

    private String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role code is required");
        }
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    public record RoleRequest(
            String code,
            String name,
            String description,
            String color,
            List<String> permissions
    ) {}

    public record PermissionRequest(
        String code,
        String label,
        String tabGroup,
        String description,
        Integer sortOrder,
        Boolean active
    ) {}
}
