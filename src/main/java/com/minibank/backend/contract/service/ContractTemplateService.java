package com.minibank.backend.contract.service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.minibank.backend.admin.entity.AdminUser;
import com.minibank.backend.admin.repository.AdminUserRepository;
import com.minibank.backend.common.service.StorageService;
import com.minibank.backend.contract.dto.DocxParseResult;
import com.minibank.backend.contract.dto.TemplateDetail;
import com.minibank.backend.contract.dto.TemplateSummary;
import com.minibank.backend.contract.dto.TemplateUpsertRequest;
import com.minibank.backend.contract.entity.ContractTemplate;
import com.minibank.backend.contract.entity.ContractTemplatePlaceholder;
import com.minibank.backend.contract.repository.ContractRepository;
import com.minibank.backend.contract.repository.ContractTemplateRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ContractTemplateService {

    private final ContractTemplateRepository templateRepo;
    private final ContractRepository contractRepo;
    private final AdminUserRepository adminUserRepo;
    private final DocxParserService docxParser;
    private final StorageService storageService;   // Cloudinary / local uploader

    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_INSTANT;

    // ── List / Get ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<TemplateSummary> listAll() {
        return templateRepo.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toSummary).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TemplateSummary> listByService(String service) {
        return templateRepo.findAllByOrderByCreatedAtDesc()
                .stream()
                .filter(t -> matchesService(t.getServices(), service))
                .map(this::toSummary).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TemplateDetail getDetail(Long id) {
        return toDetail(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public TemplateSummary getActiveByCode(String code) {
        if (code == null || code.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiếu mã template");
        }
        ContractTemplate tpl = findActiveTemplateByCodeOrAlias(code.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy template active cho mã: " + code));
        return toSummary(tpl);
    }

    @Transactional(readOnly = true)
    public java.util.Optional<ContractTemplate> findActiveTemplateByCodeOrAlias(String code) {
        for (String candidate : equivalentCodes(code)) {
            java.util.Optional<ContractTemplate> tpl = templateRepo.findActiveByCode(candidate);
            if (tpl.isPresent()) return tpl;
        }
        return java.util.Optional.empty();
    }

    // ── Create ────────────────────────────────────────────────────────────────

    @Transactional
    public TemplateDetail create(TemplateUpsertRequest req, Long adminId) {
        AdminUser admin = requireAdmin(adminId);
        if (templateRepo.existsByCode(req.code().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Mã mẫu '" + req.code() + "' đã tồn tại");
        }
        ContractTemplate t = ContractTemplate.builder()
                .name(req.name().trim())
                .code(req.code().trim())
                .description(req.description())
                .services(req.services())
                .status(req.status() != null ? req.status() : "draft")
                .templateBody(req.templateBody())
                .templateFileUrl(req.templateFileUrl())
                .createdBy(admin)
                .updatedBy(admin)
                .build();
        buildPlaceholders(t, req, 0);
        return toDetail(templateRepo.save(t));
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Transactional
    public TemplateDetail update(Long id, TemplateUpsertRequest req, Long adminId) {
        AdminUser admin = requireAdmin(adminId);
        ContractTemplate t = findOrThrow(id);

        // code conflict check (bỏ qua nếu code không đổi)
        if (!t.getCode().equals(req.code().trim()) && templateRepo.existsByCode(req.code().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Mã mẫu '" + req.code() + "' đã tồn tại");
        }
        t.setName(req.name().trim());
        t.setCode(req.code().trim());
        t.setDescription(req.description());
        t.setServices(req.services());
        if (req.status() != null) t.setStatus(req.status());
        if (req.templateBody() != null) t.setTemplateBody(req.templateBody());
        if (req.templateFileUrl() != null) t.setTemplateFileUrl(req.templateFileUrl());
        t.setUpdatedBy(admin);

		// Remove existing children before re-adding to avoid unique conflicts on field_code.
		t.syncPlaceholders(List.of());
		templateRepo.flush();
        buildPlaceholders(t, req, 0);
        return toDetail(templateRepo.save(t));
    }

    // ── Upload DOCX ───────────────────────────────────────────────────────────

    // /
    //   Upload file .docx:
    //    1. Parse nội dung + placeholder
    //    2. Lưu file lên storage
    //    3. Tạo / cập nhật template
    //  /
    @Transactional
    public TemplateDetail uploadDocx(MultipartFile file,
                                     String name, String code,
                                     String description, String services,
                                     Long adminId) throws Exception {
        AdminUser admin = requireAdmin(adminId);

        // 1. Parse DOCX
        DocxParseResult parsed = docxParser.parse(file);

        // 2. Upload file gốc lên storage
        String fileUrl = storageService.upload(file, "contract-templates");

        // 3. Tạo template
        boolean exists = templateRepo.existsByCode(code.trim());
        ContractTemplate t;
        if (exists) {
            t = templateRepo.findByCode(code.trim()).orElseThrow();
        } else {
            t = ContractTemplate.builder()
                    .name(name.trim())
                    .code(code.trim())
                    .description(description)
                    .services(services != null ? services : "general")
                    .status("draft")
                    .createdBy(admin)
                    .build();
        }
        t.setTemplateBody(parsed.extractedText());
        t.setTemplateFileUrl(fileUrl);
        t.setUpdatedBy(admin);

        // 4. Đồng bộ placeholder (tự động detect, dataSource = 'auto')
        List<ContractTemplatePlaceholder> autoPlaceholders = buildAutoPlaceholders(t, parsed.placeholders());
        t.syncPlaceholders(autoPlaceholders);

        return toDetail(templateRepo.save(t));
    }

    // ── Delete / Archive ──────────────────────────────────────────────────────

    @Transactional
    public void archive(Long id, Long adminId) {
        ContractTemplate t = findOrThrow(id);
        t.setStatus("archived");
        t.setUpdatedBy(requireAdmin(adminId));
        templateRepo.save(t);
    }

      @Transactional
      public TemplateDetail activate(Long id, Long adminId) {
          AdminUser admin = requireAdmin(adminId);
          ContractTemplate target = findOrThrow(id);
     
          // Không activate nếu đã active
          if ("active".equalsIgnoreCase(target.getStatus())) {
              return toDetail(target);
          }
     
          // Archive tất cả template cùng code đang active
          // (đảm bảo chỉ 1 bản active cho mỗi loại)
          List<String> activeGroupCodes = equivalentCodes(target.getCode());
          templateRepo.findAllByOrderByCreatedAtDesc().stream()
                  .filter(t -> !t.getId().equals(id))
                  .filter(t -> activeGroupCodes.contains(normalizeCode(t.getCode())))
                  .filter(t -> "active".equalsIgnoreCase(t.getStatus()))
                  .forEach(t -> {
                      t.setStatus("archived");
                      t.setUpdatedBy(admin);
                      templateRepo.save(t);
                  });
     
          // Activate target
          target.setStatus("active");
          target.setUpdatedBy(admin);
          return toDetail(templateRepo.save(target));
      }
     
      @Transactional
      public void delete(Long id, Long adminId) {
          ContractTemplate t = findOrThrow(id);
          if ("active".equalsIgnoreCase(t.getStatus())) {
              throw new ResponseStatusException(HttpStatus.CONFLICT,
                      "Không thể xóa template đang active. Vui lòng archive trước.");
          }
          if (contractRepo.existsByTemplate_Id(id)) {
              throw new ResponseStatusException(HttpStatus.CONFLICT,
                      "Template đã phát sinh hợp đồng nên không thể xóa. Vui lòng archive để ngừng sử dụng.");
          }
          templateRepo.delete(t);
      }

    // ── Mapping helpers ───────────────────────────────────────────────────────

    private TemplateSummary toSummary(ContractTemplate t) {
        return new TemplateSummary(
                t.getId(), t.getName(), t.getCode(), t.getDescription(),
                t.getServices(), t.getStatus(), t.getTemplateBody(), t.getTemplateFileUrl(),
                t.getPlaceholders().size(),
                t.getCreatedAt() != null ? FMT.format(t.getCreatedAt()) : null,
                t.getUpdatedAt() != null ? FMT.format(t.getUpdatedAt()) : null
        );
    }

    private TemplateDetail toDetail(ContractTemplate t) {
        List<TemplateDetail.PlaceholderDetail> pds = t.getPlaceholders().stream()
                .map(p -> new TemplateDetail.PlaceholderDetail(
                        p.getId(), p.getFieldCode(), p.getFieldLabel(),
                        p.getDataSource(), p.getSortOrder() != null ? p.getSortOrder() : 0))
                .collect(Collectors.toList());
        return new TemplateDetail(
                t.getId(), t.getName(), t.getCode(), t.getDescription(),
                t.getServices(), t.getStatus(),
                t.getTemplateBody(), t.getTemplateFileUrl(), pds,
                t.getCreatedAt() != null ? FMT.format(t.getCreatedAt()) : null,
                t.getUpdatedAt() != null ? FMT.format(t.getUpdatedAt()) : null
        );
    }

    private void buildPlaceholders(ContractTemplate t, TemplateUpsertRequest req, int baseOrder) {
        if (req.placeholders() == null) {
            // Nếu không truyền placeholder, auto-detect từ templateBody
            List<String> detected = docxParser.detectPlaceholders(req.templateBody() != null ? req.templateBody() : "");
            t.syncPlaceholders(buildAutoPlaceholders(t, detected));
        } else {
            List<ContractTemplatePlaceholder> list = new java.util.ArrayList<>();
            int order = baseOrder;
            for (TemplateUpsertRequest.PlaceholderItem item : req.placeholders()) {
                list.add(ContractTemplatePlaceholder.builder()
                        .contractTemplate(t)
                        .fieldCode(item.fieldCode())
                        .fieldLabel(item.fieldLabel())
                        .dataSource(item.dataSource())
                        .sortOrder(item.sortOrder() != null ? item.sortOrder() : order++)
                        .build());
            }
            t.syncPlaceholders(list);
        }
    }

    private List<ContractTemplatePlaceholder> buildAutoPlaceholders(ContractTemplate t, List<String> codes) {
        List<ContractTemplatePlaceholder> list = new java.util.ArrayList<>();
        int order = 0;
        for (String code : codes) {
            list.add(ContractTemplatePlaceholder.builder()
                    .contractTemplate(t)
                    .fieldCode(code)
                    .fieldLabel(code)  // Admin có thể cập nhật nhãn sau
                    .dataSource("auto")
                    .sortOrder(order++)
                    .build());
        }
        return list;
    }

    private ContractTemplate findOrThrow(Long id) {
        return templateRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy mẫu #" + id));
    }

    private List<String> equivalentCodes(String code) {
        return switch (normalizeCode(code)) {
            case "LOAN_CREDIT", "UNSECURED_LOAN_CONTRACT" ->
                    List.of("LOAN_CREDIT", "UNSECURED_LOAN_CONTRACT");
            case "LOAN_MORTGAGE", "SECURED_LOAN_CONTRACT" ->
                    List.of("LOAN_MORTGAGE", "SECURED_LOAN_CONTRACT");
            default -> List.of(normalizeCode(code));
        };
    }

    private String normalizeCode(String code) {
        return code == null ? "" : code.trim().toUpperCase();
    }

    private AdminUser requireAdmin(Long id) {
        return adminUserRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Admin not found"));
    }

    private boolean matchesService(String services, String expected) {
        if (expected == null || expected.isBlank()) return true;
        if (services == null || services.isBlank()) return false;
        String normalizedExpected = expected.trim().toLowerCase();
        if ("general".equals(normalizedExpected)) return true;

        return java.util.Arrays.stream(services.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .anyMatch(s -> s.equals(normalizedExpected) || s.equals("general"));
    }
}
