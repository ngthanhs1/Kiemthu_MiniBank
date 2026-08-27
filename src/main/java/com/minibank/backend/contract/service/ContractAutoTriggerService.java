package com.minibank.backend.contract.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.minibank.backend.admin.entity.AdminUser;
import com.minibank.backend.contract.dto.ContractDetail;
import com.minibank.backend.contract.dto.ContractGenerateRequest;
import com.minibank.backend.contract.entity.ContractTemplate;
import com.minibank.backend.contract.repository.ContractTemplateRepository;
import com.minibank.backend.loan.entity.LoanApplication;
import com.minibank.backend.loan.repository.LoanApplicationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Tự động sinh hợp đồng khi:
 *  - Admin phê duyệt đơn vay  → ownerType = LOAN_APPLICATION
 *  - Admin phê duyệt sổ tiết kiệm → ownerType = SAVING
 *
 * Gọi từ LoanService / SavingService sau khi persist entity.
 * Dùng REQUIRES_NEW để lỗi sinh HĐ không rollback giao dịch chính.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContractAutoTriggerService {

    private final ContractTemplateRepository templateRepo;
    private final ContractTemplateService templateService;
    private final ContractService contractService;
    private final LoanApplicationRepository loanApplicationRepository;

    private static final String CODE_UNSECURED = "LOAN_CREDIT";
    private static final String CODE_SECURED = "LOAN_MORTGAGE";
    private static final String CODE_SAVING = "SAVING_AGREEMENT";

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Gọi sau khi duyệt đơn vay (loan_application).
     * @param loanApplicationId ID của loan_application vừa được duyệt
     * @param approvedBy        Admin đã phê duyệt
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void triggerForLoan(Long loanApplicationId, AdminUser approvedBy) {
        ContractTemplate tpl = resolveLoanTemplate(loanApplicationId);
        generate(tpl, "loan", "LOAN_APPLICATION", loanApplicationId, approvedBy);
    }

    /**
     * Gọi sau khi duyệt sổ tiết kiệm.
     * @param savingId   ID của saving vừa được kích hoạt
     * @param approvedBy Admin đã phê duyệt
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void triggerForSaving(Long savingId, AdminUser approvedBy) {
        ContractTemplate tpl = templateRepo.findActiveByCode(CODE_SAVING).orElse(null);
        generate(tpl, "saving", "SAVING", savingId, approvedBy);
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private void generate(ContractTemplate preferred,
                          String service,
                          String ownerType,
                          Long ownerId,
                          AdminUser admin) {
        try {
            // Tìm mẫu active phù hợp với dịch vụ
            ContractTemplate tpl = preferred;
            if (tpl == null) {
                List<ContractTemplate> templates = templateRepo.findActiveByService(service.toLowerCase());
                if (templates.isEmpty()) {
                    log.warn("[ContractAutoTrigger] Không tìm thấy mẫu active cho service='{}' — bỏ qua", service);
                    return;
                }
                // Lấy mẫu ưu tiên nhất (đầu danh sách, mới nhất)
                tpl = templates.get(0);
            }

            ContractGenerateRequest req = new ContractGenerateRequest(
                    tpl.getId(), ownerType, ownerId, null, null // contractNumber tự sinh
            );
            ContractDetail result = contractService.generate(req, admin.getId());
            log.info("[ContractAutoTrigger] Đã sinh hợp đồng #{} ({}) cho {}#{}",
                    result.id(), result.contractNumber(), ownerType, ownerId);

        } catch (Exception ex) {
            // Không throw để không ảnh hưởng luồng chính
            log.error("[ContractAutoTrigger] Lỗi khi sinh hợp đồng cho {}#{}: {}",
                    ownerType, ownerId, ex.getMessage(), ex);
        }
    }

    private ContractTemplate resolveLoanTemplate(Long loanApplicationId) {
        LoanApplication app = loanApplicationRepository.findById(loanApplicationId).orElse(null);
        String loanType = app != null && app.getLoanProduct() != null
                ? app.getLoanProduct().getLoanType()
                : null;
        String normalized = loanType == null ? "" : loanType.trim().toLowerCase();
        String code = normalized.contains("secured") || normalized.contains("collateral") || normalized.contains("mortgage")
                ? CODE_SECURED
                : CODE_UNSECURED;
        return templateService.findActiveTemplateByCodeOrAlias(code).orElse(null);
    }
}
