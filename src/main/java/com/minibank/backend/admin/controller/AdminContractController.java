package com.minibank.backend.admin.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.minibank.backend.common.security.CurrentJwt;
import com.minibank.backend.contract.dto.*;
import com.minibank.backend.contract.service.ContractService;
import com.minibank.backend.contract.service.ContractTemplateService;
import com.minibank.backend.contract.service.DocxParserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Base URL: /api/admin/contracts
 *
 * Templates:
 *   GET    /templates                    - Danh sách tất cả mẫu
 *   GET    /templates?service=loan       - Lọc theo dịch vụ
 *   GET    /templates/{id}              - Chi tiết mẫu
 *   POST   /templates                    - Tạo mẫu mới (JSON)
 *   PUT    /templates/{id}              - Cập nhật mẫu
 *   POST   /templates/{id}/archive      - Archive mẫu
 *   POST   /templates/upload            - Upload file .docx → tạo mẫu
 *   POST   /templates/parse-docx        - Chỉ parse, không tạo mẫu (preview)
 *
 * Contracts:
 *   GET    /                            - Danh sách tất cả hợp đồng
 *   GET    /?ownerType=USER&ownerId=5   - Lọc theo owner
 *   GET    /{id}                        - Chi tiết hợp đồng
 *   POST   /generate                    - Sinh hợp đồng từ mẫu
 *   PATCH  /{id}/status                 - Cập nhật trạng thái
 */
@RestController
@RequestMapping("/api/admin/contracts")
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
@RequiredArgsConstructor
public class AdminContractController {

    private final ContractTemplateService templateService;
    private final ContractService contractService;
    private final DocxParserService docxParser;

    // ── Template endpoints ────────────────────────────────────────────────────

    @GetMapping("/templates")
    public List<TemplateSummary> listTemplates(
            @RequestParam(required = false) String service) {
        if (service != null && !service.isBlank()) {
            return templateService.listByService(service);
        }
        return templateService.listAll();
    }

    @GetMapping("/templates/{id}")
    public TemplateDetail getTemplate(@PathVariable Long id) {
        return templateService.getDetail(id);
    }

    @PostMapping("/templates")
    @ResponseStatus(HttpStatus.CREATED)
    public TemplateDetail createTemplate(@Valid @RequestBody TemplateUpsertRequest req) {
        return templateService.create(req, CurrentJwt.requireUserId());
    }

    @PutMapping("/templates/{id}")
    public TemplateDetail updateTemplate(@PathVariable Long id,
                                         @Valid @RequestBody TemplateUpsertRequest req) {
        return templateService.update(id, req, CurrentJwt.requireUserId());
    }

    @PostMapping("/templates/{id}/archive")
    public void archiveTemplate(@PathVariable Long id) {
        templateService.archive(id, CurrentJwt.requireUserId());
    }

    /**
     * Upload file .docx → parse → tạo template mới
     */
    @PostMapping(value = "/templates/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public TemplateDetail uploadTemplate(
            @RequestParam("file") MultipartFile file,
            @RequestParam("name") String name,
            @RequestParam("code") String code,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "services", defaultValue = "general") String services) throws Exception {

        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File không được rỗng");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".docx")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ hỗ trợ file .docx");
        }
        return templateService.uploadDocx(file, name, code, description, services,
                CurrentJwt.requireUserId());
    }

    /**
     * Chỉ parse .docx để preview placeholder, không lưu vào DB
     */
    @PostMapping(value = "/templates/parse-docx", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DocxParseResult parseDocx(@RequestParam("file") MultipartFile file) throws Exception {
        if (file.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File rỗng");
        return docxParser.parse(file);
    }

    // ── Contract endpoints ────────────────────────────────────────────────────

    @GetMapping
    public List<ContractSummary> listContracts(
            @RequestParam(required = false) String ownerType,
            @RequestParam(required = false) Long ownerId) {
        if (ownerType != null && ownerId != null) {
            return contractService.listByOwner(ownerType, ownerId);
        }
        return contractService.listAll();
    }

    @GetMapping("/acceptances")
    public List<ContractAcceptanceSummary> listAcceptances(
            @RequestParam(defaultValue = "all") String type) {
        return contractService.listAcceptances(type);
    }

    @GetMapping("/{id}")
    public ContractDetail getContract(@PathVariable Long id) {
        return contractService.getDetail(id);
    }

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public ContractDetail generateContract(@Valid @RequestBody ContractGenerateRequest req) {
        return contractService.generate(req, CurrentJwt.requireUserId());
    }

    @PatchMapping("/{id}/status")
    public ContractDetail updateStatus(@PathVariable Long id,
                                       @Valid @RequestBody ContractStatusRequest req) {
        return contractService.updateStatus(id, req.status(), CurrentJwt.requireUserId());
    }
}
