package com.minibank.backend.contract.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.minibank.backend.admin.entity.AdminUser;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "contract_templates")
public class ContractTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(columnDefinition = "text")
    private String description;

    /**
     * Dịch vụ áp dụng: "loan", "saving", "loan,saving", "general"
     */
    @Column(nullable = false, length = 128)
    @Builder.Default
    private String services = "general";

    /**
     * Trạng thái: draft | active | archived
     */
    @Column(nullable = false, length = 32)
    @Builder.Default
    private String status = "draft";

    /**
     * Nội dung mẫu dạng plain-text, có chứa {{placeholder}}
     */
    @Column(name = "template_body", columnDefinition = "text")
    private String templateBody;

    /**
     * URL file .docx gốc (upload lên Cloudinary hoặc local)
     */
    @Column(name = "template_file_url")
    private String templateFileUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private AdminUser createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_id")
    private AdminUser updatedBy;

    @OneToMany(mappedBy = "contractTemplate", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<ContractTemplatePlaceholder> placeholders = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // ── helper ──────────────────────────────────────────────
    public void syncPlaceholders(List<ContractTemplatePlaceholder> newList) {
        this.placeholders.clear();
        if (newList != null) {
            newList.forEach(p -> p.setContractTemplate(this));
            this.placeholders.addAll(newList);
        }
    }
}