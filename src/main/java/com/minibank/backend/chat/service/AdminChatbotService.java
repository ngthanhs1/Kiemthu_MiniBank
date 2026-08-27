package com.minibank.backend.chat.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.minibank.backend.admin.dto.AdminChatConversationDetail;
import com.minibank.backend.admin.dto.AdminChatConversationSummary;
import com.minibank.backend.admin.dto.AdminChatMessageResponse;
import com.minibank.backend.admin.dto.AdminFaqCategoryRequest;
import com.minibank.backend.admin.dto.AdminFaqCategoryResponse;
import com.minibank.backend.admin.dto.AdminFaqItemRequest;
import com.minibank.backend.admin.dto.AdminFaqItemResponse;
import com.minibank.backend.admin.entity.AdminUser;
import com.minibank.backend.admin.repository.AdminUserRepository;
import com.minibank.backend.chat.entity.ChatConversation;
import com.minibank.backend.chat.entity.ChatMessage;
import com.minibank.backend.chat.entity.ChatNote;
import com.minibank.backend.chat.entity.FaqCategory;
import com.minibank.backend.chat.entity.FaqItem;
import com.minibank.backend.chat.entity.FaqKeyword;
import com.minibank.backend.chat.repository.ChatConversationRepository;
import com.minibank.backend.chat.repository.ChatMessageRepository;
import com.minibank.backend.chat.repository.ChatNoteRepository;
import com.minibank.backend.chat.repository.FaqCategoryRepository;
import com.minibank.backend.chat.repository.FaqItemRepository;
import com.minibank.backend.chat.repository.FaqKeywordRepository;

@Service
public class AdminChatbotService {
	private final FaqCategoryRepository faqCategoryRepository;
	private final FaqItemRepository faqItemRepository;
	private final FaqKeywordRepository faqKeywordRepository;
	private final ChatConversationRepository conversationRepository;
	private final ChatMessageRepository messageRepository;
	private final ChatNoteRepository chatNoteRepository;
	private final AdminUserRepository adminUserRepository;
	private final ChatRealtimeService realtimeService;
	private final long retentionDays;

	public AdminChatbotService(
		FaqCategoryRepository faqCategoryRepository,
		FaqItemRepository faqItemRepository,
		FaqKeywordRepository faqKeywordRepository,
		ChatConversationRepository conversationRepository,
		ChatMessageRepository messageRepository,
		ChatNoteRepository chatNoteRepository,
		AdminUserRepository adminUserRepository,
		ChatRealtimeService realtimeService,
		@Value("${app.chatbot.retention-days:7}") long retentionDays
	) {
		this.faqCategoryRepository = faqCategoryRepository;
		this.faqItemRepository = faqItemRepository;
		this.faqKeywordRepository = faqKeywordRepository;
		this.conversationRepository = conversationRepository;
		this.messageRepository = messageRepository;
		this.chatNoteRepository = chatNoteRepository;
		this.adminUserRepository = adminUserRepository;
		this.realtimeService = realtimeService;
		this.retentionDays = retentionDays;
	}

	@Transactional(readOnly = true)
	public List<AdminFaqCategoryResponse> listCategories() {
		Map<Long, Long> faqCountByCategory = faqItemRepository.findAll().stream()
			.filter(item -> item.getParentFaqItem() == null)
			.collect(Collectors.groupingBy(item -> item.getCategory().getId(), Collectors.counting()));
		return faqCategoryRepository.findAllByOrderBySortOrderAscNameAsc().stream()
			.map(category -> new AdminFaqCategoryResponse(
				category.getId(),
				category.getCode(),
				category.getName(),
				category.getDescription(),
				category.getSortOrder(),
				category.isActive(),
				faqCountByCategory.getOrDefault(category.getId(), 0L)
			))
			.toList();
	}

	@Transactional
	public AdminFaqCategoryResponse createCategory(AdminFaqCategoryRequest request) {
		FaqCategory category = FaqCategory.builder()
			.code(request.code().trim())
			.name(request.name().trim())
			.description(request.description())
			.sortOrder(request.sortOrder())
			.active(Boolean.TRUE.equals(request.active()))
			.build();
		FaqCategory saved = faqCategoryRepository.save(category);
		return new AdminFaqCategoryResponse(
			saved.getId(),
			saved.getCode(),
			saved.getName(),
			saved.getDescription(),
			saved.getSortOrder(),
			saved.isActive(),
			0
		);
	}

	@Transactional
	public AdminFaqCategoryResponse updateCategory(long id, AdminFaqCategoryRequest request) {
		FaqCategory category = faqCategoryRepository.findById(id)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
		category.setCode(request.code().trim());
		category.setName(request.name().trim());
		category.setDescription(request.description());
		category.setSortOrder(request.sortOrder());
		category.setActive(Boolean.TRUE.equals(request.active()));
		FaqCategory saved = faqCategoryRepository.save(category);
		long count = faqItemRepository.findByCategoryIdAndActiveTrueOrderByCreatedAtDesc(saved.getId()).size();
		return new AdminFaqCategoryResponse(
			saved.getId(),
			saved.getCode(),
			saved.getName(),
			saved.getDescription(),
			saved.getSortOrder(),
			saved.isActive(),
			count
		);
	}

	@Transactional(readOnly = true)
	public List<AdminFaqItemResponse> listFaqItems(Long categoryId, String q) {
		String query = q == null ? null : q.trim();
		List<FaqItem> items;
		if (categoryId != null) {
			items = faqItemRepository.findByCategoryIdOrderByCreatedAtDesc(categoryId);
		} else if (query != null && !query.isEmpty()) {
			items = faqItemRepository.findByQuestionContainingIgnoreCaseOrderByCreatedAtDesc(query);
		} else {
			items = faqItemRepository.findAllByOrderByCreatedAtDesc();
		}
		if (query != null && !query.isEmpty() && categoryId != null) {
			String normalizedQuery = GuidedChatbotService.normalize(query);
			items = items.stream()
				.filter(item -> GuidedChatbotService.normalize(item.getQuestion()).contains(normalizedQuery))
				.toList();
		}
		return items.stream().map(this::toFaqItemResponse).toList();
	}

	@Transactional(readOnly = true)
	public AdminFaqItemResponse getFaqItem(long id) {
		FaqItem item = faqItemRepository.findById(id)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "FAQ not found"));
		return toFaqItemResponse(item);
	}

	@Transactional
	public AdminFaqItemResponse createFaqItem(AdminFaqItemRequest request) {
		FaqCategory category = faqCategoryRepository.findById(request.categoryId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
		FaqItem parent = resolveParent(request.parentFaqId(), category.getId());
		FaqItem item = FaqItem.builder()
			.category(category)
			.parentFaqItem(parent)
			.question(request.question().trim())
			.answer(request.answer().trim())
			.active(Boolean.TRUE.equals(request.active()))
			.build();
		FaqItem saved = faqItemRepository.save(item);
		replaceKeywords(saved, request.keywords());
		return toFaqItemResponse(saved);
	}

	@Transactional
	public AdminFaqItemResponse updateFaqItem(long id, AdminFaqItemRequest request) {
		FaqItem item = faqItemRepository.findById(id)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "FAQ not found"));
		FaqCategory category = faqCategoryRepository.findById(request.categoryId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
		FaqItem parent = resolveParent(request.parentFaqId(), category.getId());
		if (parent != null && parent.getId().equals(item.getId())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parent FAQ cannot be itself");
		}

		item.setCategory(category);
		item.setParentFaqItem(parent);
		item.setQuestion(request.question().trim());
		item.setAnswer(request.answer().trim());
		item.setActive(Boolean.TRUE.equals(request.active()));
		FaqItem saved = faqItemRepository.save(item);
		replaceKeywords(saved, request.keywords());
		return toFaqItemResponse(saved);
	}

	@Transactional
	public void deleteFaqItem(long id) {
		FaqItem item = faqItemRepository.findById(id)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "FAQ not found"));
		for (FaqItem child : faqItemRepository.findByParentFaqItemIdOrderByCreatedAtDesc(item.getId())) {
			deleteFaqItem(child.getId());
		}
		faqKeywordRepository.deleteByFaqItemId(item.getId());
		faqItemRepository.delete(item);
	}

	@Transactional(readOnly = true)
	public List<AdminChatConversationSummary> listConversations(String status) {
		String statusFilter = status == null ? null : status.trim().toLowerCase();
		Instant cutoff = retentionCutoff();
		// Load all conversations and sort by: 1) escalated (recent first), 2) customer rank (VIP->..), 3) startedAt desc
		return conversationRepository.findAll().stream()
			.filter(conversation -> isWithinRetention(conversation.getStartedAt(), cutoff))
			.filter(conversation -> statusFilter == null || statusFilter.isBlank() || conversation.getStatus().equalsIgnoreCase(statusFilter))
			.sorted((a, b) -> {
				// 1) escalatedAt: non-null first, most recent first
				if (a.getEscalatedAt() == null && b.getEscalatedAt() != null) return 1;
				if (a.getEscalatedAt() != null && b.getEscalatedAt() == null) return -1;
				if (a.getEscalatedAt() != null && b.getEscalatedAt() != null) {
					int cmp = b.getEscalatedAt().compareTo(a.getEscalatedAt());
					if (cmp != 0) return cmp;
				}
				// 2) customer rank priority
				int ra = rankPriority(a.getUser().getCustomerRank());
				int rb = rankPriority(b.getUser().getCustomerRank());
				if (ra != rb) return Integer.compare(ra, rb);
				// 3) startedAt desc
				if (a.getStartedAt() == null && b.getStartedAt() == null) return 0;
				if (a.getStartedAt() == null) return 1;
				if (b.getStartedAt() == null) return -1;
				return b.getStartedAt().compareTo(a.getStartedAt());
			})
			.map(this::toConversationSummary)
			.toList();
	}

	private int rankPriority(String rank) {
		if (rank == null) return 99;
		switch (rank.trim().toUpperCase()) {
			case "VIP": return 1;
			case "GOLD": return 2;
			case "SILVER": return 3;
			case "BRONZE": return 4;
			default: return 50;
		}
	}

	@Transactional(readOnly = true)
	public AdminChatConversationDetail getConversation(long id) {
		ChatConversation conversation = conversationRepository.findById(id)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));
		if (!isWithinRetention(conversation.getStartedAt(), retentionCutoff())) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found");
		}
		List<AdminChatMessageResponse> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId())
			.stream()
			.map(this::toChatMessageResponse)
			.toList();
		return new AdminChatConversationDetail(
			conversation.getId(),
			conversation.getUser().getId(),
			conversation.getUser().getFullName(),
			conversation.getUser().getPhone(),
			conversation.getUser().getCustomerRank(),
			conversation.getChannel(),
			conversation.getStatus(),
			conversation.getLastIntent(),
			conversation.getLastConfidence(),
			conversation.getStartedAt(),
			conversation.getEscalatedAt(),
			conversation.getAssignedAdminUser() == null ? null : conversation.getAssignedAdminUser().getId(),
			conversation.getAssignedAdminUser() == null ? null : conversation.getAssignedAdminUser().getUsername(),
			messages
		);
	}

	@Transactional
	public AdminChatConversationDetail assignSelf(long conversationId, long adminUserId) {
		ChatConversation conversation = loadConversation(conversationId);
		AdminUser admin = loadAdmin(adminUserId);
		conversation.setAssignedAdminUser(admin);
		conversation.setStatus("IN_PROGRESS");
		conversationRepository.save(conversation);
		realtimeService.broadcastConversation(conversation, null);
		return getConversation(conversationId);
	}

	@Transactional
	public AdminChatConversationDetail sendReply(long conversationId, long adminUserId, String message) {
		ChatConversation conversation = loadConversation(conversationId);
		AdminUser admin = loadAdmin(adminUserId);
		conversation.setAssignedAdminUser(admin);
		conversation.setStatus("IN_PROGRESS");
		conversationRepository.save(conversation);

		ChatMessage saved = messageRepository.save(ChatMessage.builder()
			.conversation(conversation)
			.senderType("ADMIN")
			.senderId(admin.getId())
			.messageType("TEXT")
			.content(message.trim())
			.build());
		realtimeService.broadcastMessage(saved);
		realtimeService.broadcastConversation(conversation, saved.getContent());
		return getConversation(conversationId);
	}

	@Transactional
	public AdminChatConversationDetail closeConversation(long conversationId, long adminUserId) {
		ChatConversation conversation = loadConversation(conversationId);
		AdminUser admin = loadAdmin(adminUserId);
		conversation.setAssignedAdminUser(admin);
		conversation.setStatus("CLOSED");
		conversation.setEndedAt(Instant.now());
		conversationRepository.save(conversation);
		ChatMessage systemMessage = messageRepository.save(ChatMessage.builder()
			.conversation(conversation)
			.senderType("SYSTEM")
			.messageType("TEXT")
			.content("CSKH đã đóng cuộc trò chuyện. Nếu cần hỗ trợ thêm, vui lòng tạo cuộc trò chuyện mới.")
			.build());
		realtimeService.broadcastMessage(systemMessage);
		realtimeService.broadcastConversation(conversation, systemMessage.getContent());
		return getConversation(conversationId);
	}

	@Transactional
	public void addNote(long conversationId, long adminUserId, String note) {
		ChatConversation conversation = loadConversation(conversationId);
		AdminUser admin = loadAdmin(adminUserId);
		chatNoteRepository.save(ChatNote.builder()
			.conversation(conversation)
			.adminUser(admin)
			.note(note.trim())
			.build());
	}

	private ChatConversation loadConversation(long conversationId) {
		return conversationRepository.findById(conversationId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));
	}

	private AdminUser loadAdmin(long adminUserId) {
		return adminUserRepository.findById(adminUserId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin user not found"));
	}

	private void replaceKeywords(FaqItem item, List<String> keywords) {
		faqKeywordRepository.deleteByFaqItemId(item.getId());
		Set<String> dedup = sanitizeKeywords(keywords);
		for (String keyword : dedup) {
			faqKeywordRepository.save(FaqKeyword.builder()
				.faqItem(item)
				.keyword(keyword)
				.normalizedKeyword(GuidedChatbotService.normalize(keyword))
				.build());
		}
	}

	private FaqItem resolveParent(Long parentFaqId, Long categoryId) {
		if (parentFaqId == null) {
			return null;
		}
		FaqItem parent = faqItemRepository.findById(parentFaqId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parent FAQ not found"));
		if (!parent.getCategory().getId().equals(categoryId)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parent FAQ must be in the same category");
		}
		return parent;
	}

	private Set<String> sanitizeKeywords(List<String> keywords) {
		if (keywords == null || keywords.isEmpty()) {
			return Set.of();
		}
		return keywords.stream()
			.map(value -> value == null ? "" : value.trim())
			.filter(value -> !value.isEmpty())
			.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private AdminFaqItemResponse toFaqItemResponse(FaqItem item) {
		List<String> keywords = faqKeywordRepository.findByFaqItemId(item.getId()).stream()
			.map(FaqKeyword::getKeyword)
			.toList();
		return new AdminFaqItemResponse(
			item.getId(),
			item.getCategory().getId(),
			item.getCategory().getName(),
			item.getCategory().getCode(),
			item.getParentFaqItem() == null ? null : item.getParentFaqItem().getId(),
			item.getQuestion(),
			item.getAnswer(),
			item.isActive(),
			faqItemRepository.countByParentFaqItemIdAndActiveTrue(item.getId()),
			keywords
		);
	}

	private AdminChatConversationSummary toConversationSummary(ChatConversation conversation) {
		String lastMessage = messageRepository.findTop1ByConversationIdOrderByCreatedAtDesc(conversation.getId())
			.map(ChatMessage::getContent)
			.orElse(null);
		if (lastMessage != null && lastMessage.length() > 120) {
			lastMessage = lastMessage.substring(0, 120) + "...";
		}
		return new AdminChatConversationSummary(
			conversation.getId(),
			conversation.getUser().getId(),
			conversation.getUser().getFullName(),
			conversation.getUser().getPhone(),
			conversation.getUser().getCustomerRank(),
			conversation.getStatus(),
			conversation.getLastIntent(),
			conversation.getLastConfidence(),
			conversation.getStartedAt(),
			conversation.getEscalatedAt(),
			lastMessage,
			conversation.getAssignedAdminUser() == null ? null : conversation.getAssignedAdminUser().getId(),
			conversation.getAssignedAdminUser() == null ? null : conversation.getAssignedAdminUser().getUsername()
		);
	}

	private AdminChatMessageResponse toChatMessageResponse(ChatMessage message) {
		return new AdminChatMessageResponse(
			message.getId(),
			message.getSenderType(),
			message.getSenderId(),
			message.getMessageType(),
			message.getContent(),
			message.getCreatedAt()
		);
	}

	private Instant retentionCutoff() {
		return Instant.now().minus(retentionDays, ChronoUnit.DAYS);
	}

	private boolean isWithinRetention(Instant startedAt, Instant cutoff) {
		return startedAt != null && !startedAt.isBefore(cutoff);
	}
}
