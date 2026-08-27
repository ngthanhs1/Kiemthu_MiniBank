package com.minibank.backend.admin.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.minibank.backend.admin.dto.AdminChatConversationDetail;
import com.minibank.backend.admin.dto.AdminChatConversationSummary;
import com.minibank.backend.admin.dto.AdminChatNoteRequest;
import com.minibank.backend.admin.dto.AdminChatReplyRequest;
import com.minibank.backend.chat.service.AdminChatbotService;
import com.minibank.backend.common.security.CurrentJwt;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/customer-support")
@PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER_SUPPORT')")
public class AdminCustomerSupportController {
	private final AdminChatbotService chatbotService;

	public AdminCustomerSupportController(AdminChatbotService chatbotService) {
		this.chatbotService = chatbotService;
	}

	@GetMapping("/conversations")
	public List<AdminChatConversationSummary> conversations(
		@RequestParam(value = "status", required = false) String status
	) {
		return chatbotService.listConversations(status);
	}

	@GetMapping("/conversations/{conversationId}")
	public AdminChatConversationDetail conversation(@PathVariable long conversationId) {
		return chatbotService.getConversation(conversationId);
	}

	@PostMapping("/conversations/{conversationId}/assign-self")
	public AdminChatConversationDetail assignSelf(@PathVariable long conversationId) {
		return chatbotService.assignSelf(conversationId, CurrentJwt.requireUserId());
	}

	@PostMapping("/conversations/{conversationId}/reply")
	public AdminChatConversationDetail reply(
		@PathVariable long conversationId,
		@Valid @RequestBody AdminChatReplyRequest request
	) {
		return chatbotService.sendReply(conversationId, CurrentJwt.requireUserId(), request.message());
	}

	@PostMapping("/conversations/{conversationId}/close")
	public AdminChatConversationDetail close(@PathVariable long conversationId) {
		return chatbotService.closeConversation(conversationId, CurrentJwt.requireUserId());
	}

	@PostMapping("/conversations/{conversationId}/notes")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void note(
		@PathVariable long conversationId,
		@Valid @RequestBody AdminChatNoteRequest request
	) {
		chatbotService.addNote(conversationId, CurrentJwt.requireUserId(), request.note());
	}
}
