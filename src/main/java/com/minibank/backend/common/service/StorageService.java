package com.minibank.backend.common.service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;

/**
 * Abstraction for file storage.
 * - Nếu có CLOUDINARY_CLOUD_NAME + CLOUDINARY_UPLOAD_PRESET → upload Cloudinary
 * - Fallback → lưu local /uploads/{folder}/
 */
@Slf4j
@Service
public class StorageService {

    private final RestTemplate restTemplate = new RestTemplate();

    // ── Upload MultipartFile ──────────────────────────────────────────────────

    public String upload(MultipartFile file, String folder) throws Exception {
        String cloudName = System.getenv("CLOUDINARY_CLOUD_NAME");
        String uploadPreset = System.getenv("CLOUDINARY_UPLOAD_PRESET");

        if (isCloudinaryConfigured(cloudName, uploadPreset)) {
            return uploadToCloudinary(file.getBytes(), file.getOriginalFilename(),
                    cloudName, uploadPreset, folder);
        }
        return saveLocally(file.getBytes(), file.getOriginalFilename(), folder);
    }

    // ── Upload text/HTML content ──────────────────────────────────────────────

    public String uploadText(String content, String filename, String folder) throws Exception {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        String cloudName = System.getenv("CLOUDINARY_CLOUD_NAME");
        String uploadPreset = System.getenv("CLOUDINARY_UPLOAD_PRESET");

        if (isCloudinaryConfigured(cloudName, uploadPreset)) {
            return uploadToCloudinary(bytes, filename, cloudName, uploadPreset, folder);
        }
        return saveLocally(bytes, filename, folder);
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private boolean isCloudinaryConfigured(String cloudName, String preset) {
        return cloudName != null && !cloudName.isBlank()
                && preset != null && !preset.isBlank();
    }

    @SuppressWarnings("unchecked")
    private String uploadToCloudinary(byte[] bytes, String originalFilename,
                                      String cloudName, String preset, String folder) {
        String url = "https://api.cloudinary.com/v1_1/" + cloudName + "/auto/upload";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        String finalName = originalFilename != null ? originalFilename
                : UUID.randomUUID() + ".bin";

        ByteArrayResource resource = new ByteArrayResource(bytes) {
            @Override public String getFilename() { return finalName; }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", resource);
        body.add("upload_preset", preset);
        body.add("folder", folder);

        ResponseEntity<java.util.Map> resp = restTemplate.postForEntity(
                url, new HttpEntity<>(body, headers), java.util.Map.class);

        if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
            Object secureUrl = resp.getBody().get("secure_url");
            if (secureUrl != null) return secureUrl.toString();
        }
        throw new RuntimeException("Cloudinary upload failed");
    }

    private String saveLocally(byte[] bytes, String originalFilename, String folder) throws Exception {
        String fileName = UUID.randomUUID() + "_" + (originalFilename != null ? originalFilename : "file");
        Path dir = Paths.get("uploads", folder);
        Files.createDirectories(dir);
        Path out = dir.resolve(fileName);
        Files.write(out, bytes);
        log.info("Saved file locally: {}", out);
        return "/uploads/" + folder + "/" + fileName;
    }
}
