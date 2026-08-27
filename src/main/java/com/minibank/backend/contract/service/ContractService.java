package com.minibank.backend.contract.service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.minibank.backend.admin.entity.AdminUser;
import com.minibank.backend.admin.repository.AdminUserRepository;
import com.minibank.backend.common.service.StorageService;
import com.minibank.backend.contract.dto.ContractDetail;
import com.minibank.backend.contract.dto.ContractAcceptanceSummary;
import com.minibank.backend.contract.dto.ContractGenerateRequest;
import com.minibank.backend.contract.dto.ContractSummary;
import com.minibank.backend.contract.entity.Contract;
import com.minibank.backend.contract.entity.ContractTemplate;
import com.minibank.backend.contract.repository.ContractRepository;
import com.minibank.backend.contract.repository.ContractTemplateRepository;
import com.minibank.backend.loan.entity.LoanApplication;
import com.minibank.backend.loan.repository.LoanApplicationRepository;
import com.minibank.backend.saving.entity.Saving;
import com.minibank.backend.saving.repository.SavingRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContractService {

    private final ContractRepository contractRepo;
    private final ContractTemplateRepository templateRepo;
    private final AdminUserRepository adminUserRepo;
    private final ContractDataResolver dataResolver;
    private final DocxParserService docxParser;
    private final StorageService storageService;
    private final SavingRepository savingRepository;
    private final LoanApplicationRepository loanApplicationRepository;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_INSTANT;

    // ── List / Get ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ContractSummary> listAll() {
        return contractRepo.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toSummary).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ContractSummary> listByOwner(String ownerType, Long ownerId) {
        return contractRepo.findByOwnerTypeAndOwnerIdOrderByCreatedAtDesc(ownerType, ownerId)
                .stream().map(this::toSummary).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ContractDetail getDetail(Long id) {
        return toDetail(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<ContractAcceptanceSummary> listAcceptances(String type) {
        String normalizedType = type == null ? "all" : type.trim().toLowerCase();
        boolean includeSavings = "all".equals(normalizedType) || "saving".equals(normalizedType);
        boolean includeLoans = "all".equals(normalizedType) || "loan".equals(normalizedType);

        List<ContractAcceptanceSummary> out = new ArrayList<>();

        if (includeSavings) {
            List<Saving> savings = savingRepository.findAllByOrderByCreatedAtDesc();
            for (Saving saving : savings) {
                if (saving.getAgreementAcceptedAt() == null || saving.getUser() == null) continue;
                out.add(new ContractAcceptanceSummary(
                    "saving_agreement",
                    "saving",
                    saving.getId(),
                    saving.getUser().getId(),
                    saving.getUser().getFullName(),
                    saving.getUser().getPhone(),
                    null,
                    null,
                    "Thoa thuan tiet kiem",
                    saving.getAgreementVersion(),
                    saving.getCode(),
                    normalizeAcceptanceStatus(saving.getStatus()),
                    FMT.format(saving.getAgreementAcceptedAt())
                ));
            }
        }

        if (includeLoans) {
            List<Contract> contracts = contractRepo.findAllByOrderByCreatedAtDesc();
            for (Contract contract : contracts) {
                if (!"loan_application".equalsIgnoreCase(contract.getOwnerType())) continue;
                if (contract.getSignedAt() == null) continue;

                LoanApplication app = loanApplicationRepository.findById(contract.getOwnerId()).orElse(null);
                if (app == null || app.getUser() == null) continue;

                out.add(new ContractAcceptanceSummary(
                    "loan_contract",
                    "loan_application",
                    contract.getOwnerId(),
                    app.getUser().getId(),
                    app.getUser().getFullName(),
                    app.getUser().getPhone(),
                    contract.getTemplate() != null ? contract.getTemplate().getId() : null,
                    contract.getTemplate() != null ? contract.getTemplate().getCode() : null,
                    contract.getTemplate() != null ? contract.getTemplate().getName() : null,
                    null,
                    contract.getContractNumber(),
                    normalizeAcceptanceStatus(contract.getStatus()),
                    FMT.format(contract.getSignedAt())
                ));
            }
        }

        return out.stream()
            .sorted((a, b) -> {
                String left = a.acceptedAt() == null ? "" : a.acceptedAt();
                String right = b.acceptedAt() == null ? "" : b.acceptedAt();
                return right.compareTo(left);
            })
            .toList();
    }

    // ── Generate ──────────────────────────────────────────────────────────────

    /**
     * Sinh hợp đồng:
     *  1. Lấy template
     *  2. Resolve dữ liệu từ DB theo ownerType
     *  3. Điền placeholder vào templateBody
     *  4. Lưu renderedBody + upload file
     *  5. Trả về ContractDetail
     */
    @Transactional
    public ContractDetail generate(ContractGenerateRequest req, Long adminId) {
        AdminUser admin = requireAdmin(adminId);
        ContractTemplate tpl = templateRepo.findById(req.templateId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy mẫu #" + req.templateId()));

        // 1. Resolve dữ liệu
        Map<String, String> data = new LinkedHashMap<>(resolveData(req.ownerType(), req.ownerId()));
        mergeOverrides(data, req.dataOverrides());

        // 2. Điền placeholder
        String rendered = docxParser.fillTemplate(tpl.getTemplateBody(), data);

        // 3. Tạo contract number nếu chưa có
        String contractNumber = req.contractNumber() != null && !req.contractNumber().isBlank()
                ? req.contractNumber()
                : generateContractNumber(req.ownerType());

        // 4. Tạo entity
        Contract contract = Contract.builder()
                .contractNumber(contractNumber)
                .template(tpl)
                .ownerType(req.ownerType())
                .ownerId(req.ownerId())
                .renderedBody(rendered)
                .status("DRAFT")
                .createdBy(admin)
                .build();
        contractRepo.save(contract);

        // 5. Upload rendered content lên storage (HTML)
        try {
            String htmlContent = wrapHtml(rendered, contractNumber);
            String fileUrl = storageService.uploadText(
                    htmlContent,
                    "contract_" + contract.getId() + ".html",
                    "contracts"
            );
            contract.setFileUrl(fileUrl);
            contractRepo.save(contract);
        } catch (Exception ex) {
            log.warn("Upload contract file failed for contract #{}: {}", contract.getId(), ex.getMessage());
        }

        return toDetail(contract);
    }

    // ── Status update ─────────────────────────────────────────────────────────

    @Transactional
    public ContractDetail updateStatus(Long id, String status, Long adminId) {
        Contract c = findOrThrow(id);
        validateStatusTransition(c.getStatus(), status);
        c.setStatus(status);
        if ("SIGNED".equals(status)) {
            c.setSignedAt(java.time.Instant.now());
        }
        return toDetail(contractRepo.save(c));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Map<String, String> resolveData(String ownerType, Long ownerId) {
        return switch (ownerType.toUpperCase()) {
            case "USER"              -> dataResolver.resolveForUser(ownerId);
            case "LOAN_APPLICATION"  -> dataResolver.resolveForLoanApplication(ownerId);
            case "SAVING"            -> dataResolver.resolveForSaving(ownerId);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "ownerType không hợp lệ: " + ownerType);
        };
    }

    private String generateContractNumber(String ownerType) {
        String prefix = switch (ownerType.toUpperCase()) {
            case "LOAN_APPLICATION" -> "HD-LOAN";
            case "SAVING"           -> "HD-SAVE";
            default                 -> "HD";
        };
        String year = String.valueOf(java.time.LocalDate.now().getYear());
        String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return prefix + "-" + year + "-" + suffix;
    }

    private void mergeOverrides(Map<String, String> target, Map<String, String> overrides) {
        if (overrides == null || overrides.isEmpty()) return;
        overrides.forEach((key, value) -> {
            if (key == null) return;
            String normalizedKey = key.trim();
            if (normalizedKey.isEmpty()) return;
            target.put(normalizedKey, value == null ? "" : value.trim());
        });
    }

    private String normalizeAcceptanceStatus(String status) {
        if (status == null || status.isBlank()) {
            return "unknown";
        }
        String normalized = status.trim().toLowerCase();
        if ("active".equals(normalized) || "signed".equals(normalized)) {
            return "accepted";
        }
        if ("rejected".equals(normalized) || "cancelled".equals(normalized)) {
            return "rejected";
        }
        if (normalized.startsWith("pending")) {
            return "pending";
        }
        return normalized;
    }

    private void validateStatusTransition(String current, String next) {
        // Cho phép transition hợp lệ
        boolean valid = switch (current) {
            case "DRAFT"             -> List.of("SENT", "CANCELLED").contains(next);
            case "SENT"              -> List.of("PENDING_SIGNATURE", "CANCELLED").contains(next);
            case "PENDING_SIGNATURE" -> List.of("SIGNED", "CANCELLED").contains(next);
            case "SIGNED"            -> false;
            case "CANCELLED"         -> false;
            default                  -> false;
        };
        if (!valid) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Không thể chuyển trạng thái từ '" + current + "' sang '" + next + "'");
        }
    }

    private String wrapHtml(String body, String title) {
        return """
            <!DOCTYPE html>
            <html lang="vi">
            <head>
              <meta charset="UTF-8">
              <title>%s</title>
              <style>
                body { font-family: 'Times New Roman', serif; max-width: 800px; margin: 40px auto;
                       padding: 40px; font-size: 14px; line-height: 1.8; color: #1a1a1a; }
                pre { white-space: pre-wrap; font-family: inherit; }
              </style>
            </head>
            <body><pre>%s</pre></body>
            </html>
            """.formatted(title, body);
    }

    private ContractSummary toSummary(Contract c) {
        return new ContractSummary(
                c.getId(), c.getContractNumber(),
                c.getTemplate() != null ? c.getTemplate().getId() : null,
                c.getTemplate() != null ? c.getTemplate().getName() : null,
                c.getOwnerType(), c.getOwnerId(),
                c.getStatus(), c.getFileUrl(),
                c.getCreatedAt() != null ? FMT.format(c.getCreatedAt()) : null
        );
    }

    private ContractDetail toDetail(Contract c) {
        return new ContractDetail(
                c.getId(), c.getContractNumber(),
                c.getTemplate() != null ? c.getTemplate().getId() : null,
                c.getTemplate() != null ? c.getTemplate().getName() : null,
                c.getOwnerType(), c.getOwnerId(),
                c.getStatus(), c.getFileUrl(), c.getRenderedBody(),
                c.getSignedAt() != null ? FMT.format(c.getSignedAt()) : null,
                c.getCreatedAt() != null ? FMT.format(c.getCreatedAt()) : null
        );
    }

    private Contract findOrThrow(Long id) {
        return contractRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy hợp đồng #" + id));
    }

    private AdminUser requireAdmin(Long id) {
        return adminUserRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Admin not found"));
    }
}
