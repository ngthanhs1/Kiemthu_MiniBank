package com.minibank.backend.contract.service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.minibank.backend.contract.dto.DocxParseResult;

/**
 * Parse file .docx dùng Apache POI:
 *  - Trích xuất toàn bộ text (đoạn văn + bảng)
 *  - Phát hiện tất cả {{placeholder}} trong nội dung
 *
 * Dependency (pom.xml):
 *   <dependency>
 *     <groupId>org.apache.poi</groupId>
 *     <artifactId>poi-ooxml</artifactId>
 *     <version>5.2.5</version>
 *   </dependency>
 */
@Service
public class DocxParserService {

    private static final Pattern PLACEHOLDER_PATTERN =
            Pattern.compile("\\{\\{([^}]+)\\}\\}");

    // ── Public API ────────────────────────────────────────────────────────────

    public DocxParseResult parse(MultipartFile file) throws Exception {
        try (InputStream is = file.getInputStream();
             XWPFDocument doc = new XWPFDocument(is)) {
            StringBuilder sb = new StringBuilder();
            extractFromDocument(doc, sb);
            String text = sb.toString();
            List<String> placeholders = detectPlaceholders(text);
            return new DocxParseResult(text, placeholders);
        }
    }

    public DocxParseResult parse(InputStream is) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(is)) {
            StringBuilder sb = new StringBuilder();
            extractFromDocument(doc, sb);
            String text = sb.toString();
            List<String> placeholders = detectPlaceholders(text);
            return new DocxParseResult(text, placeholders);
        }
    }

    // ── Text extraction ───────────────────────────────────────────────────────

    private void extractFromDocument(XWPFDocument doc, StringBuilder sb) {
        // Đoạn văn thông thường
        for (XWPFParagraph para : doc.getParagraphs()) {
            String text = getParagraphText(para);
            if (!text.isBlank()) {
                sb.append(text).append("\n");
            }
        }

        // Bảng
        for (XWPFTable table : doc.getTables()) {
            for (XWPFTableRow row : table.getRows()) {
                List<String> cellTexts = new ArrayList<>();
                for (XWPFTableCell cell : row.getTableCells()) {
                    StringBuilder cellSb = new StringBuilder();
                    for (XWPFParagraph para : cell.getParagraphs()) {
                        String t = getParagraphText(para);
                        if (!t.isBlank()) cellSb.append(t).append(" ");
                    }
                    cellTexts.add(cellSb.toString().trim());
                }
                sb.append(String.join(" | ", cellTexts)).append("\n");
            }
        }
    }

    /**
     * Ghép text từ các run trong paragraph.
     * Quan trọng: một {{placeholder}} có thể bị tách thành nhiều run,
     * nên phải ghép toàn bộ text của paragraph trước khi dùng.
     */
    private String getParagraphText(XWPFParagraph para) {
        return para.getRuns().stream()
                .map(run -> run.getText(0))
                .filter(t -> t != null)
                .reduce("", String::concat);
    }

    // ── Placeholder detection ─────────────────────────────────────────────────

    public List<String> detectPlaceholders(String text) {
        Set<String> found = new LinkedHashSet<>();
        Matcher m = PLACEHOLDER_PATTERN.matcher(text);
        while (m.find()) {
            found.add(m.group(1).trim());
        }
        return new ArrayList<>(found);
    }

    /**
     * Điền dữ liệu vào template: thay thế tất cả {{key}} bằng value tương ứng.
     * Key không có trong map sẽ giữ nguyên dạng {{key}}.
     */
    public String fillTemplate(String templateBody, java.util.Map<String, String> data) {
        if (templateBody == null) return "";
        String result = templateBody;
        for (java.util.Map.Entry<String, String> entry : data.entrySet()) {
            result = result.replace(
                    "{{" + entry.getKey() + "}}",
                    entry.getValue() == null ? "" : entry.getValue()
            );
        }
        return result;
    }
}