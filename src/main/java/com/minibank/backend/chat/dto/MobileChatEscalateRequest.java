package com.minibank.backend.chat.dto;

import java.util.List;

public record MobileChatEscalateRequest(
	List<MobileChatTranscriptMessage> messages
) {}
