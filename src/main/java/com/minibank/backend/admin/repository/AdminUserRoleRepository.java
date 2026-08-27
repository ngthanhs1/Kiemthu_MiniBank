package com.minibank.backend.admin.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.minibank.backend.admin.entity.AdminUserRole;

public interface AdminUserRoleRepository extends JpaRepository<AdminUserRole, Long> {
	boolean existsByAdminUserIdAndRoleId(Long adminUserId, Long roleId);
	void deleteByAdminUserId(Long adminUserId);

	@Query("select r.code from AdminUserRole aur join aur.role r where aur.adminUser.id = :adminUserId")
	List<String> findRoleCodesByAdminUserId(@Param("adminUserId") Long adminUserId);

	@Query("select count(aur) from AdminUserRole aur join aur.role r where upper(r.code) = upper(:roleCode)")
	long countUsersByRoleCode(@Param("roleCode") String roleCode);
}
