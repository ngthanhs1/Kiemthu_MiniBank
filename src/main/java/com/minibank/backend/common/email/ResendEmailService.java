package com.minibank.backend.common.email;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
public class ResendEmailService {
	private final String apiKey;
	private final String senderEmail;
	private final String senderName;
	private final HttpClient client = HttpClient.newHttpClient();
	private final ObjectMapper mapper = new ObjectMapper();

	public ResendEmailService(
		@Value("${RESEND_API_KEY:}") String apiKey,
		@Value("${RESEND_SENDER_EMAIL:}") String senderEmail,
		@Value("${RESEND_SENDER_NAME:MiniBank}") String senderName
	) {
		this.apiKey = apiKey == null ? "" : apiKey.trim();
		this.senderEmail = senderEmail == null ? "" : senderEmail.trim();
		this.senderName = senderName == null ? "MiniBank" : senderName.trim();
	}

	public void sendKycSubmittedEmail(String toEmail, String fullName) {
		if (apiKey.isEmpty() || senderEmail.isEmpty() || toEmail == null || toEmail.isBlank()) {
			return;
		}

		try {
			ObjectNode body = mapper.createObjectNode();
			body.put("from", senderName + " <" + senderEmail + ">");
			body.putArray("to").add(toEmail);
			body.put("subject", "Da tiep nhan yeu cau KYC");
			body.put("text", "Xin chao " + (fullName == null ? "" : fullName) + ",\n\nHo so KYC cua ban da duoc tiep nhan va dang cho duyet.");

			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create("https://api.resend.com/emails"))
				.header("Authorization", "Bearer " + apiKey)
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
				.build();

			client.send(request, HttpResponse.BodyHandlers.discarding());
		} catch (Exception e) {
			// Ignore email errors to avoid blocking KYC flow.
		}
	}
}
