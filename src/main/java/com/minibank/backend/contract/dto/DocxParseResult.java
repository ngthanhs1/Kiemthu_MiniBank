package com.minibank.backend.contract.dto;

import java.util.List;

public record DocxParseResult(
    String extractedText,
    List<String> placeholders
) {}
