package com.minibank.backend.support.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.minibank.backend.common.security.CurrentJwt;
import com.minibank.backend.support.dto.CreateLimitChangeRequest;
import com.minibank.backend.support.dto.CreateProfileChangeRequest;
import com.minibank.backend.support.dto.CreateServiceRequestRequest;
import com.minibank.backend.support.dto.LimitChangeRequestResponse;
import com.minibank.backend.support.dto.ServiceRequestResponse;
import com.minibank.backend.support.service.ServiceRequestService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/mobile/service-requests")
public class MobileServiceRequestController {
	private final ServiceRequestService serviceRequestService;

	public MobileServiceRequestController(ServiceRequestService serviceRequestService) {
		this.serviceRequestService = serviceRequestService;
	}

	@GetMapping
	public List<ServiceRequestResponse> getServiceRequests() {
		long userId = CurrentJwt.requireUserId();
		return serviceRequestService.getServiceRequests(userId);
	}

	@GetMapping("/{id}")
	public ServiceRequestResponse getServiceRequest(@PathVariable Long id) {
		long userId = CurrentJwt.requireUserId();
		return serviceRequestService.getServiceRequest(userId, id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ServiceRequestResponse createServiceRequest(@Valid @RequestBody CreateServiceRequestRequest request) {
		long userId = CurrentJwt.requireUserId();
		return serviceRequestService.createServiceRequest(userId, request);
	}

	@PostMapping("/limit-change")
	@ResponseStatus(HttpStatus.CREATED)
	public LimitChangeRequestResponse createLimitChangeRequest(@Valid @RequestBody CreateLimitChangeRequest request) {
		long userId = CurrentJwt.requireUserId();
		return serviceRequestService.createLimitChangeRequest(userId, request);
	}

	@PostMapping("/profile-change")
	@ResponseStatus(HttpStatus.CREATED)
	public ServiceRequestResponse createProfileChangeRequest(@Valid @RequestBody CreateProfileChangeRequest request) {
		long userId = CurrentJwt.requireUserId();
		return serviceRequestService.createProfileChangeRequest(userId, request);
	}
}
