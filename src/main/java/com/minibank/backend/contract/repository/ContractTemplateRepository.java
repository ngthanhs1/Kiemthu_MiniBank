package com.minibank.backend.contract.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.minibank.backend.contract.entity.ContractTemplate;

// ── ContractTemplateRepository ──────────────────────────────────────────────
public interface ContractTemplateRepository extends JpaRepository<ContractTemplate, Long> {

    List<ContractTemplate> findAllByOrderByCreatedAtDesc();

    List<ContractTemplate> findByStatusOrderByCreatedAtDesc(String status);

    /**
     * Tìm mẫu áp dụng cho một dịch vụ cụ thể (loan / saving / general).
     * services lưu dạng: "loan", "saving", "loan,saving", "general"
     */
        @Query("select t from ContractTemplate t " +
            "where lower(t.status) = 'active' " +
            "and (lower(t.services) = :svc or lower(t.services) like concat('%', :svc, '%') " +
            "     or lower(t.services) = 'general') " +
            "order by coalesce(t.updatedAt, t.createdAt) desc")
        List<ContractTemplate> findActiveByService(@Param("svc") String service);

        @Query("select t from ContractTemplate t " +
            "where lower(t.status) = 'active' and lower(t.code) = lower(:code)")
        Optional<ContractTemplate> findActiveByCode(@Param("code") String code);

    boolean existsByCode(String code);

    Optional<ContractTemplate> findByCode(String code);
}
