package com.minibank.backend.chat.service;

import java.text.Normalizer;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.minibank.backend.chat.dto.ChatbotFaqCategoryDto;
import com.minibank.backend.chat.dto.ChatbotFaqItemDto;
import com.minibank.backend.chat.dto.MobileChatConversationDetailResponse;
import com.minibank.backend.chat.dto.MobileChatConversationSummary;
import com.minibank.backend.chat.dto.MobileChatEscalateRequest;
import com.minibank.backend.chat.dto.MobileChatMessageDto;
import com.minibank.backend.chat.dto.MobileChatTranscriptMessage;
import com.minibank.backend.chat.dto.MobileChatbotBootstrapResponse;
import com.minibank.backend.chat.dto.MobileChatbotSendRequest;
import com.minibank.backend.chat.dto.MobileChatbotSendResponse;
import com.minibank.backend.chat.entity.ChatConversation;
import com.minibank.backend.chat.entity.ChatMessage;
import com.minibank.backend.chat.entity.FaqCategory;
import com.minibank.backend.chat.entity.FaqItem;
import com.minibank.backend.chat.entity.FaqKeyword;
import com.minibank.backend.chat.repository.ChatConversationRepository;
import com.minibank.backend.chat.repository.ChatMessageRepository;
import com.minibank.backend.chat.repository.FaqCategoryRepository;
import com.minibank.backend.chat.repository.FaqItemRepository;
import com.minibank.backend.chat.repository.FaqKeywordRepository;
import com.minibank.backend.user.entity.User;
import com.minibank.backend.user.repository.UserRepository;

@Service
public class GuidedChatbotService {
	private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
	private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9\\s]");
	private static final String CHAT_CHANNEL = "GUIDED_SUPPORT";
	private static final String STATUS_OPEN = "OPEN";
	private static final String STATUS_WAITING_AGENT = "WAITING_AGENT";

	private final UserRepository userRepository;
	private final ChatConversationRepository conversationRepository;
	private final ChatMessageRepository messageRepository;
	private final FaqCategoryRepository faqCategoryRepository;
	private final FaqItemRepository faqItemRepository;
	private final FaqKeywordRepository faqKeywordRepository;
	private final ChatRealtimeService realtimeService;
	private final long retentionDays;

	public GuidedChatbotService(
		UserRepository userRepository,
		ChatConversationRepository conversationRepository,
		ChatMessageRepository messageRepository,
		FaqCategoryRepository faqCategoryRepository,
		FaqItemRepository faqItemRepository,
		FaqKeywordRepository faqKeywordRepository,
		ChatRealtimeService realtimeService,
		@Value("${app.chatbot.retention-days:7}") long retentionDays
	) {
		this.userRepository = userRepository;
		this.conversationRepository = conversationRepository;
		this.messageRepository = messageRepository;
		this.faqCategoryRepository = faqCategoryRepository;
		this.faqItemRepository = faqItemRepository;
		this.faqKeywordRepository = faqKeywordRepository;
		this.realtimeService = realtimeService;
		this.retentionDays = retentionDays;
	}

	@Transactional(readOnly = true)
	public MobileChatbotBootstrapResponse bootstrap() {
		List<FaqCategory> categories = faqCategoryRepository.findByActiveTrueOrderBySortOrderAscNameAsc();
		List<FaqItem> items = faqItemRepository.findByActiveTrueOrderByCreatedAtDesc();

		Map<Long, Long> countByCategory = items.stream()
			.filter(item -> item.getParentFaqItem() == null)
			.collect(Collectors.groupingBy(item -> item.getCategory().getId(), Collectors.counting()));

		List<ChatbotFaqCategoryDto> categoryDtos = categories.stream()
			.map(category -> new ChatbotFaqCategoryDto(
				category.getId(),
				category.getCode(),
				category.getName(),
				category.getDescription(),
				countByCategory.getOrDefault(category.getId(), 0L).intValue()
			))
			.toList();

		List<ChatbotFaqItemDto> suggested = items.stream()
			.filter(item -> item.getParentFaqItem() == null)
			.limit(6)
			.map(this::toFaqItemDto)
			.toList();

		return new MobileChatbotBootstrapResponse(categoryDtos, suggested);
	}

	@Transactional(readOnly = true)
	public List<ChatbotFaqItemDto> listFaqItems(Long categoryId, Long parentFaqId) {
		List<FaqItem> items;
		if (parentFaqId != null) {
			items = faqItemRepository.findByParentFaqItemIdAndActiveTrueOrderByCreatedAtDesc(parentFaqId);
		} else if (categoryId != null) {
			items = faqItemRepository.findByCategoryIdAndParentFaqItemIsNullAndActiveTrueOrderByCreatedAtDesc(categoryId);
		} else {
			items = faqItemRepository.findByActiveTrueOrderByCreatedAtDesc().stream()
				.filter(item -> item.getParentFaqItem() == null)
				.toList();
		}
		return items.stream().map(this::toFaqItemDto).toList();
	}

	@Transactional(readOnly = true)
	public List<MobileChatConversationSummary> listConversations(long userId) {
		Instant cutoff = retentionCutoff();
		return conversationRepository.findByUserIdOrderByStartedAtDesc(userId).stream()
			.filter(conversation -> isWithinRetention(conversation.getStartedAt(), cutoff))
			.filter(conversation -> !STATUS_OPEN.equalsIgnoreCase(conversation.getStatus()))
			.map(conversation -> new MobileChatConversationSummary(
				conversation.getId(),
				conversation.getStatus(),
				conversation.getLastIntent(),
				conversation.getLastConfidence(),
				conversation.getStartedAt(),
				conversation.getEscalatedAt(),
				messageRepository.findTop1ByConversationIdOrderByCreatedAtDesc(conversation.getId())
					.map(ChatMessage::getContent)
					.map(content -> content.length() > 80 ? content.substring(0, 80) + "..." : content)
					.orElse(null)
			))
			.toList();
	}

	@Transactional(readOnly = true)
	public MobileChatConversationDetailResponse getConversation(long userId, long conversationId) {
		ChatConversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));
		if (!isWithinRetention(conversation.getStartedAt(), retentionCutoff())) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found");
		}
		List<MobileChatMessageDto> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId())
			.stream()
			.map(this::toMessageDto)
			.toList();
		return new MobileChatConversationDetailResponse(
			conversation.getId(),
			conversation.getStatus(),
			conversation.getLastIntent(),
			conversation.getLastConfidence(),
			conversation.getStartedAt(),
			conversation.getEscalatedAt(),
			messages
		);
	}

	@Transactional
	public MobileChatbotSendResponse send(long userId, MobileChatbotSendRequest request) {
		if (Boolean.TRUE.equals(request.temporary()) && request.conversationId() == null) {
			return answerTemporary(request.message());
		}

		User user = userRepository.findById(userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
		ChatConversation conversation = resolveConversation(user, request.conversationId());

		ChatMessage userMessage = messageRepository.save(ChatMessage.builder()
			.conversation(conversation)
			.senderType("USER")
			.senderId(userId)
			.messageType("TEXT")
			.content(request.message().trim())
			.build());
		realtimeService.broadcastMessage(userMessage);

		if (!STATUS_OPEN.equalsIgnoreCase(conversation.getStatus())) {
			realtimeService.broadcastConversation(conversation, userMessage.getContent());
			return new MobileChatbotSendResponse(
				conversation.getId(),
				toMessageDto(userMessage),
				new MobileChatMessageDto(0L, "SYSTEM", "SYSTEM", "Tin nhắn đã được gửi đến nhân viên CSKH.", Instant.now()),
				null,
				conversation.getLastIntent(),
				conversation.getLastConfidence(),
				List.of(),
				true
			);
		}

		BotAnswer answer = buildAnswer(request.message());
		conversation.setLastIntent(answer.matchedCategoryCode());
		conversation.setLastConfidence(answer.confidence());

		conversationRepository.save(conversation);

		ChatMessage botMessage = messageRepository.save(ChatMessage.builder()
			.conversation(conversation)
			.senderType("BOT")
			.senderId(null)
			.messageType(answer.messageType())
			.content(answer.reply())
			.build());
		realtimeService.broadcastMessage(botMessage);

		return new MobileChatbotSendResponse(
			conversation.getId(),
			toMessageDto(userMessage),
			toMessageDto(botMessage),
			answer.matchedFaqId(),
			answer.matchedCategoryCode(),
			answer.confidence(),
			answer.followUps(),
			STATUS_WAITING_AGENT.equalsIgnoreCase(conversation.getStatus())
		);
	}

	@Transactional(readOnly = true)
	public MobileChatbotSendResponse answerTemporary(String message) {
		BotAnswer answer = buildAnswer(message);
		Instant now = Instant.now();
		MobileChatMessageDto userMessage = new MobileChatMessageDto(
			0L,
			"USER",
			"TEXT",
			message.trim(),
			now
		);
		MobileChatMessageDto botMessage = new MobileChatMessageDto(
			0L,
			"BOT",
			answer.messageType(),
			answer.reply(),
			now.plusMillis(1)
		);
		return new MobileChatbotSendResponse(
			0L,
			userMessage,
			botMessage,
			answer.matchedFaqId(),
			answer.matchedCategoryCode(),
			answer.confidence(),
			answer.followUps(),
			false
		);
	}

	@Transactional
	public MobileChatConversationDetailResponse escalate(long userId, long conversationId) {
		ChatConversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));
		conversation.setStatus(STATUS_WAITING_AGENT);
		conversation.setEscalatedAt(Instant.now());
		conversationRepository.save(conversation);

		ChatMessage systemMessage = messageRepository.save(ChatMessage.builder()
			.conversation(conversation)
			.senderType("BOT")
			.messageType("SYSTEM")
			.content("Yêu cầu đã được chuyển đến nhân viên CSKH. Bạn vui lòng chờ trong giây lát.")
			.build());

		realtimeService.broadcastMessage(systemMessage);
		realtimeService.broadcastConversation(conversation, systemMessage.getContent());

		return getConversation(userId, conversationId);
	}

	@Transactional
	public MobileChatConversationDetailResponse escalateTemporary(long userId, MobileChatEscalateRequest request) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

		ChatConversation conversation = conversationRepository.save(ChatConversation.builder()
			.user(user)
			.channel(CHAT_CHANNEL)
			.status(STATUS_WAITING_AGENT)
			.escalatedAt(Instant.now())
			.build());

		List<MobileChatTranscriptMessage> transcript = request == null || request.messages() == null
			? List.of()
			: request.messages();
		transcript.stream()
			.limit(30)
			.filter(item -> item != null && item.content() != null && !item.content().isBlank())
			.forEach(item -> {
				ChatMessage saved = messageRepository.save(ChatMessage.builder()
					.conversation(conversation)
					.senderType(normalizeSenderType(item.senderType()))
					.senderId("USER".equalsIgnoreCase(item.senderType()) ? userId : null)
					.messageType(item.messageType() == null || item.messageType().isBlank() ? "TEXT" : item.messageType())
					.content(item.content().trim())
					.build());
				realtimeService.broadcastMessage(saved);
			});

		ChatMessage systemMessage = messageRepository.save(ChatMessage.builder()
			.conversation(conversation)
			.senderType("BOT")
			.messageType("SYSTEM")
			.content("Yêu cầu đã được chuyển đến nhân viên CSKH. Bạn vui lòng chờ trong giây lát.")
			.build());

		realtimeService.broadcastMessage(systemMessage);
		realtimeService.broadcastConversation(conversation, systemMessage.getContent());

		return getConversation(userId, conversation.getId());
	}

	private BotAnswer buildAnswer(String rawMessage) {
		IntentMatch match = findBestMatch(rawMessage);
		String botReply = null;
		Long matchedFaqId = null;
		String matchedCategoryCode = null;
		Integer confidence = null;
		List<ChatbotFaqItemDto> followUps = List.of();
		String messageType = "FALLBACK";

		String normalizedInput = normalize(rawMessage);
		Set<String> inputTokens = tokenSet(normalizedInput);

		if (match != null) {
			FaqItem matched = match.item();
			botReply = matched.getAnswer();
			matchedFaqId = matched.getId();
			matchedCategoryCode = matched.getCategory().getCode();
			confidence = match.confidence();
			followUps = faqItemRepository.findByParentFaqItemIdAndActiveTrueOrderByCreatedAtDesc(matched.getId())
				.stream()
				.map(this::toFaqItemDto)
				.toList();
			messageType = "FAQ_MATCH";
		} else if (inputTokens.stream().anyMatch(t -> t.contains("otp"))) {
			botReply = "Mình tìm thấy một số câu hỏi liên quan đến OTP. Bạn muốn xem câu nào?";
			List<FaqKeyword> matches = faqKeywordRepository.findByNormalizedKeywordContaining("otp");
			Set<FaqItem> matchedItems = matches.stream().map(FaqKeyword::getFaqItem).collect(Collectors.toCollection(LinkedHashSet::new));
			followUps = matchedItems.stream().map(this::toFaqItemDto).limit(6).toList();
			confidence = 0;
			matchedCategoryCode = "otp_suggest";
			messageType = "SUGGESTION";
		} else {
			botReply = "Mình chưa xác định chính xác vấn đề. Bạn có thể chọn câu hỏi gợi ý hoặc bấm gặp nhân viên hỗ trợ.";
			followUps = faqItemRepository.findByActiveTrueOrderByCreatedAtDesc().stream()
				.filter(item -> item.getParentFaqItem() == null)
				.limit(5)
				.map(this::toFaqItemDto)
				.toList();
			confidence = 0;
			matchedCategoryCode = "fallback";
			messageType = "FALLBACK";
		}

		return new BotAnswer(botReply, messageType, matchedFaqId, matchedCategoryCode, confidence, followUps);
	}

	private ChatConversation resolveConversation(User user, Long conversationId) {
		if (conversationId != null) {
			ChatConversation existing = conversationRepository.findByIdAndUserId(conversationId, user.getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));
			if (!isWithinRetention(existing.getStartedAt(), retentionCutoff())) {
				throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found");
			}
			return existing;
		}
		// Prevent fast repeated sends from creating many conversations: reuse very recent conversation if exists
		var recent = conversationRepository.findByUserIdOrderByStartedAtDesc(user.getId()).stream()
			.filter(conversation -> isWithinRetention(conversation.getStartedAt(), retentionCutoff()))
			.findFirst();
		if (recent.isPresent()) {
			ChatConversation latest = recent.get();
			if (latest.getStartedAt() != null && latest.getStartedAt().isAfter(Instant.now().minusSeconds(3))) {
				return latest;
			}
		}

		ChatConversation created = ChatConversation.builder()
			.user(user)
			.channel(CHAT_CHANNEL)
			.status(STATUS_OPEN)
			.build();
		return conversationRepository.save(created);
	}

	private IntentMatch findBestMatch(String rawMessage) {
		String normalizedMessage = normalize(rawMessage);
		if (normalizedMessage.isBlank()) {
			return null;
		}
		Set<String> messageTokens = tokenSet(normalizedMessage);
		List<FaqItem> activeItems = faqItemRepository.findByActiveTrueOrderByCreatedAtDesc();
		if (activeItems.isEmpty()) {
			return null;
		}

		Map<Long, List<FaqKeyword>> keywordByItem = faqKeywordRepository.findAll().stream()
			.collect(Collectors.groupingBy(keyword -> keyword.getFaqItem().getId()));

		FaqItem bestItem = null;
		int bestScore = 0;
		for (FaqItem item : activeItems) {
			int score = 0;
			String questionNorm = normalize(item.getQuestion());
			score += phraseScore(normalizedMessage, questionNorm) * 3;
			score += tokenOverlap(messageTokens, tokenSet(questionNorm));

			List<FaqKeyword> keywords = keywordByItem.getOrDefault(item.getId(), List.of());
			for (FaqKeyword keyword : keywords) {
				String normalizedKeyword = normalize(keyword.getKeyword());
				score += phraseScore(normalizedMessage, normalizedKeyword) * 5;
				score += tokenOverlap(messageTokens, tokenSet(normalizedKeyword)) * 2;
			}

			if (score > bestScore) {
				bestScore = score;
				bestItem = item;
			}
		}

		if (bestItem == null || bestScore < 5) {
			return null;
		}
		int confidence = Math.min(98, 45 + (bestScore * 4));
		return new IntentMatch(bestItem, confidence);
	}

	private ChatbotFaqItemDto toFaqItemDto(FaqItem item) {
		List<String> keywords = faqKeywordRepository.findByFaqItemId(item.getId()).stream()
			.map(FaqKeyword::getKeyword)
			.toList();
		FaqCategory category = item.getCategory();
		return new ChatbotFaqItemDto(
			item.getId(),
			category.getId(),
			category.getName(),
			category.getCode(),
			item.getParentFaqItem() == null ? null : item.getParentFaqItem().getId(),
			item.getQuestion(),
			item.getAnswer(),
			faqItemRepository.countByParentFaqItemIdAndActiveTrue(item.getId()),
			keywords,
			item.isActive()
		);
	}

	private Instant retentionCutoff() {
		return Instant.now().minus(retentionDays, ChronoUnit.DAYS);
	}

	private boolean isWithinRetention(Instant startedAt, Instant cutoff) {
		return startedAt != null && !startedAt.isBefore(cutoff);
	}

	private MobileChatMessageDto toMessageDto(ChatMessage message) {
		return new MobileChatMessageDto(
			message.getId(),
			message.getSenderType(),
			message.getMessageType(),
			message.getContent(),
			message.getCreatedAt()
		);
	}

	private static int phraseScore(String input, String phrase) {
		if (phrase == null || phrase.isBlank()) {
			return 0;
		}
		return input.contains(phrase) ? 1 : 0;
	}

	private static int tokenOverlap(Set<String> left, Set<String> right) {
		int score = 0;
		for (String token : left) {
			if (token.length() < 3) {
				continue;
			}
			if (right.contains(token)) {
				score++;
			}
		}
		return score;
	}

	private static Set<String> tokenSet(String value) {
		if (value == null || value.isBlank()) {
			return Set.of();
		}
		return List.of(value.split("\\s+")).stream()
			.map(String::trim)
			.filter(token -> !token.isBlank())
			.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	public static String normalize(String value) {
		if (value == null) {
			return "";
		}
		String lower = value.toLowerCase(Locale.ROOT).trim();
		String normalized = Normalizer.normalize(lower, Normalizer.Form.NFD);
		normalized = DIACRITICS.matcher(normalized).replaceAll("");
		normalized = NON_ALNUM.matcher(normalized).replaceAll(" ");
		return normalized.replaceAll("\\s+", " ").trim();
	}

	private static String normalizeSenderType(String senderType) {
		if (senderType == null) {
			return "USER";
		}
		String normalized = senderType.trim().toUpperCase(Locale.ROOT);
		if (normalized.equals("BOT") || normalized.equals("ADMIN") || normalized.equals("USER")) {
			return normalized;
		}
		return "USER";
	}

	private record BotAnswer(
		String reply,
		String messageType,
		Long matchedFaqId,
		String matchedCategoryCode,
		Integer confidence,
		List<ChatbotFaqItemDto> followUps
	) {}

	private record IntentMatch(FaqItem item, Integer confidence) {}
}
