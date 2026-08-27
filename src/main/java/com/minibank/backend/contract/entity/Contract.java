package com.minibank.backend.contract.entity;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.minibank.backend.admin.entity.AdminUser;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "contracts")
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contract_number", length = 64)
    private String contractNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private ContractTemplate template;

    /**
     * Loại đối tượng: USER | loan_application | saving
     */
    @Column(name = "owner_type", nullable = false, length = 64)
    private String ownerType;

    /**
     * ID của đối tượng tương ứng với ownerType
     */
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    /**
     * URL file hợp đồng đã render (lưu trên Cloudinary / local)
     */
    @Column(name = "file_url")
    private String fileUrl;

    /**
     * Nội dung đã điền dữ liệu, lưu để audit / hiển thị lại
     */
    @Column(name = "rendered_body", columnDefinition = "text")
    private String renderedBody;

    /**
     * Trạng thái: DRAFT | SENT | PENDING_SIGNATURE | SIGNED | CANCELLED
     */
    @Column(nullable = false, length = 32)
    @Builder.Default
    private String status = "DRAFT";

    @Column(name = "signed_at")
    private Instant signedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private AdminUser createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}