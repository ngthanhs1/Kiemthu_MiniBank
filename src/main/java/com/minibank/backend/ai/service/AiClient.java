package com.minibank.backend.ai.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minibank.backend.ai.dto.AiFinanceRecommendationRequest;
import com.minibank.backend.ai.dto.AiFinanceRecommendationResponse;
import com.minibank.backend.ai.dto.AiTransactionClassifyRequest;
import com.minibank.backend.ai.dto.AiTransactionClassifyResponse;
import com.minibank.backend.transaction.entity.Transaction;

@Service
public class AiClient {
	private static final Logger log = LoggerFactory.getLogger(AiClient.class);

	private final HttpClient client;
	private final ObjectMapper objectMapper;
	private final String baseUrl;

	public AiClient(
		ObjectMapper objectMapper,
		@Value("${app.ai.base-url:http://localhost:8000}") String baseUrl
	) {
		this.objectMapper = objectMapper;
		this.baseUrl = baseUrl.replaceAll("/+$", "");
		this.client = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(3))
			.build();
	}

	public Optional<AiTransactionClassifyResponse> classifyTransaction(Transaction tx) {
		try {
			String type = tx.getFromAccount() == null ? "IN" : "OUT";
			AiTransactionClassifyRequest payload = new AiTransactionClassifyRequest(
				tx.getId(),
				type,
				tx.getAmount(),
				tx.getDescription() == null ? "" : tx.getDescription(),
				null,
				tx.getCreatedAt()
			);

			String body = objectMapper.writeValueAsString(payload);
			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(baseUrl + "/api/v1/transactions/classify"))
				.timeout(Duration.ofSeconds(4))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(body))
				.build();

			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				log.warn("AI classify failed with status {}", response.statusCode());
				return Optional.empty();
			}

			AiTransactionClassifyResponse parsed = objectMapper.readValue(
				response.body(),
				AiTransactionClassifyResponse.class
			);
			return Optional.of(parsed);
		} catch (Exception ex) {
			log.warn("AI classify error: {}", ex.getMessage());
			return Optional.empty();
		}
	}

	public Optional<AiFinanceRecommendationResponse> recommendations(AiFinanceRecommendationRequest payload) {
		try {
			String body = objectMapper.writeValueAsString(payload);
			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(baseUrl + "/api/v1/finance/recommendations"))
				.timeout(Duration.ofSeconds(8))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(body))
				.build();

			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				log.warn("AI recommendations failed with status {}", response.statusCode());
				return Optional.empty();
			}

			AiFinanceRecommendationResponse parsed = objectMapper.readValue(
				response.body(),
				AiFinanceRecommendationResponse.class
			);
			return Optional.of(parsed);
		} catch (Exception ex) {
			log.warn("AI recommendations error: {}", ex.getMessage());
			return Optional.empty();
		}
	}
}
