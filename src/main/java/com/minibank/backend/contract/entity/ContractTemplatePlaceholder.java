package com.minibank.backend.contract.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "contract_template_placeholders",
       uniqueConstraints = @UniqueConstraint(columnNames = {"contract_template_id", "field_code"}))
public class ContractTemplatePlaceholder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_template_id", nullable = false)
    private ContractTemplate contractTemplate;

    /**
     * Tên trường kỹ thuật, ví dụ: full_name, loan_amount
     */
    @Column(name = "field_code", nullable = false, length = 100)
    private String fieldCode;

    /**
     * Nhãn hiển thị tiếng Việt
     */
    @Column(name = "field_label", length = 255)
    private String fieldLabel;

    /**
     * Nguồn dữ liệu: users | accounts | loans | savings | system | custom
     */
    @Column(name = "data_source", length = 50)
    private String dataSource;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;
}