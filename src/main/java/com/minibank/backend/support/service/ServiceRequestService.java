package com.minibank.backend.support.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minibank.backend.account.entity.Account;
import com.minibank.backend.account.repository.AccountRepository;
import com.minibank.backend.support.dto.CreateLimitChangeRequest;
import com.minibank.backend.support.dto.CreateProfileChangeRequest;
import com.minibank.backend.support.dto.CreateServiceRequestRequest;
import com.minibank.backend.support.dto.LimitChangeRequestResponse;
import com.minibank.backend.support.dto.ServiceRequestResponse;
import com.minibank.backend.support.entity.LimitChangeRequest;
import com.minibank.backend.support.entity.ServiceRequest;
import com.minibank.backend.support.repository.LimitChangeRequestRepository;
import com.minibank.backend.support.repository.ServiceRequestRepository;
import com.minibank.backend.user.entity.User;
import com.minibank.backend.user.repository.UserRepository;

@Service
public class ServiceRequestService {
	private final ServiceRequestRepository serviceRequestRepository;
	private final LimitChangeRequestRepository limitChangeRequestRepository;
	private final AccountRepository accountRepository;
	private final UserRepository userRepository;
	private final ObjectMapper objectMapper;
	private final SimpMessagingTemplate messagingTemplate;

	public ServiceRequestService(
		ServiceRequestRepository serviceRequestRepository,
		LimitChangeRequestRepository limitChangeRequestRepository,
		AccountRepository accountRepository,
		UserRepository userRepository,
		ObjectMapper objectMapper,
		SimpMessagingTemplate messagingTemplate
	) {
		this.serviceRequestRepository = serviceRequestRepository;
		this.limitChangeRequestRepository = limitChangeRequestRepository;
		this.accountRepository = accountRepository;
		this.userRepository = userRepository;
		this.objectMapper = objectMapper;
		this.messagingTemplate = messagingTemplate;
	}

	@Transactional(readOnly = true)
	public List<ServiceRequestResponse> getServiceRequests(long userId) {
		return serviceRequestRepository.findByUserIdOrderBySubmittedAtDesc(userId)
			.stream()
			.map(this::toServiceRequestResponse)
			.toList();
	}

	@Transactional(readOnly = true)
	public ServiceRequestResponse getServiceRequest(long userId, long requestId) {
		ServiceRequest request = serviceRequestRepository.findByIdAndUserId(requestId, userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service request not found"));
		return toServiceRequestResponse(request);
	}

	@Transactional
	public ServiceRequestResponse createServiceRequest(long userId, CreateServiceRequestRequest request) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

		ServiceRequest serviceRequest = ServiceRequest.builder()
			.user(user)
			.requestType(request.requestType())
			.title(request.title())
			.description(request.description())
			.priorityTag(request.priorityTag())
			.payloadJson(request.payloadJson())
			.status("SUBMITTED")
			.build();

		serviceRequest = serviceRequestRepository.save(serviceRequest);
		notifyAdminNewRequest(serviceRequest, user);
		return toServiceRequestResponse(serviceRequest);
	}

	@Transactional
	public LimitChangeRequestResponse createLimitChangeRequest(long userId, CreateLimitChangeRequest request) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

		Account account = accountRepository.findById(request.accountId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
		if (account.getUser() == null || !account.getUser().getId().equals(userId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account does not belong to user");
		}

		String title = "Yeu cau tang han muc";
		String description = request.reason();
		ServiceRequest serviceRequest = ServiceRequest.builder()
			.user(user)
			.requestType("limit_change")
			.title(title)
			.description(description)
			.priorityTag("LIMIT")
			.status("SUBMITTED")
			.build();
		serviceRequest = serviceRequestRepository.save(serviceRequest);
		notifyAdminNewRequest(serviceRequest, user);

		LimitChangeRequest limitChangeRequest = LimitChangeRequest.builder()
			.serviceRequest(serviceRequest)
			.account(account)
			.currentDailyTransferLimit(account.getDailyTransferLimit())
			.requestedDailyTransferLimit(request.requestedDailyTransferLimit())
			.reason(request.reason())
			.build();
		limitChangeRequest = limitChangeRequestRepository.save(limitChangeRequest);

		return toLimitChangeResponse(limitChangeRequest, serviceRequest, account);
	}

	@Transactional
	public ServiceRequestResponse createProfileChangeRequest(long userId, CreateProfileChangeRequest request) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

		String payloadJson = toProfilePayloadJson(request);
		ServiceRequest serviceRequest = ServiceRequest.builder()
			.user(user)
			.requestType("profile_change")
			.title("Yeu cau doi thong tin")
			.description(request.reason())
			.payloadJson(payloadJson)
			.priorityTag("PROFILE")
			.status("SUBMITTED")
			.build();
		serviceRequest = serviceRequestRepository.save(serviceRequest);
		notifyAdminNewRequest(serviceRequest, user);
		return toServiceRequestResponse(serviceRequest);
	}

	private void notifyAdminNewRequest(ServiceRequest request, User user) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("type", "SERVICE_REQUEST");
		payload.put("title", "Yeu cau dich vu moi");
		payload.put("content", (user.getFullName() == null ? user.getPhone() : user.getFullName()) + " vua gui " + request.getTitle());
		payload.put("requestId", request.getId());
		payload.put("requestType", request.getRequestType());
		payload.put("createdAt", request.getSubmittedAt());
		messagingTemplate.convertAndSend("/topic/admin/notifications", payload);
	}

	@Transactional
	public ServiceRequestResponse updateServiceRequestStatus(long userId, long requestId, String status) {
		ServiceRequest request = serviceRequestRepository.findByIdAndUserId(requestId, userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service request not found"));

		request.setStatus(status);
		request = serviceRequestRepository.save(request);
		return toServiceRequestResponse(request);
	}

	private String toProfilePayloadJson(CreateProfileChangeRequest request) {
		Map<String, Object> payload = new LinkedHashMap<>();
		if (request.fullName() != null) {
			payload.put("fullName", request.fullName().trim());
		}
		if (request.dob() != null) {
			payload.put("dob", request.dob());
		}
		if (request.address() != null) {
			payload.put("address", request.address().trim());
		}
		try {
			return objectMapper.writeValueAsString(payload);
		} catch (JsonProcessingException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to serialize payload");
		}
	}

	private LimitChangeRequestResponse toLimitChangeResponse(LimitChangeRequest request, ServiceRequest serviceRequest, Account account) {
		return new LimitChangeRequestResponse(
			request.getId(),
			serviceRequest.getId(),
			account.getId(),
			account.getAccountNumber(),
			account.getAccountName(),
			request.getCurrentDailyTransferLimit(),
			request.getRequestedDailyTransferLimit(),
			request.getReason(),
			serviceRequest.getStatus(),
			serviceRequest.getSubmittedAt(),
			serviceRequest.getProcessedAt(),
			serviceRequest.getProcessNote()
		);
	}

	private ServiceRequestResponse toServiceRequestResponse(ServiceRequest request) {
		return new ServiceRequestResponse(
			request.getId(),
			request.getRequestType(),
			request.getTitle(),
			request.getDescription(),
			request.getPriorityTag(),
			request.getStatus(),
			request.getSubmittedAt(),
			request.getProcessedAt(),
			request.getProcessNote()
		);
	}
}
