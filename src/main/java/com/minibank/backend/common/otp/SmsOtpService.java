package com.minibank.backend.common.otp;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class SmsOtpService {
	private static final String DEV_OTP = "123456";

	private final boolean devMode;
	private final String accountSid;
	private final String authToken;
	private final String verifyServiceSid;
	private final HttpClient client = HttpClient.newHttpClient();
	private final ObjectMapper mapper = new ObjectMapper();

	public SmsOtpService(
		@Value("${sms.dev-mode:true}") boolean devMode,
		@Value("${sms.twilio-account-sid:}") String accountSid,
		@Value("${sms.twilio-auth-token:}") String authToken,
		@Value("${sms.twilio-verify-service-sid:}") String verifyServiceSid
	) {
		this.devMode = devMode;
		this.accountSid = accountSid == null ? "" : accountSid.trim();
		this.authToken = authToken == null ? "" : authToken.trim();
		this.verifyServiceSid = verifyServiceSid == null ? "" : verifyServiceSid.trim();
	}

	public OtpSendResult sendOtp(String phoneNumber) {
		if (devMode) {
			return new OtpSendResult(true, DEV_OTP);
		}

		ensureConfig();
		try {
			String body = formEncode(
				"To", phoneNumber,
				"Channel", "sms"
			);

			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create("https://verify.twilio.com/v2/Services/" + verifyServiceSid + "/Verifications"))
				.header("Authorization", basicAuth())
				.header("Content-Type", "application/x-www-form-urlencoded")
				.POST(HttpRequest.BodyPublishers.ofString(body))
				.build();

			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to send OTP");
			}
			return new OtpSendResult(false, null);
		} catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to send OTP", e);
		}
	}

	public boolean verifyOtp(String phoneNumber, String code) {
		if (devMode) {
			return DEV_OTP.equals(code);
		}

		ensureConfig();
		try {
			String body = formEncode(
				"To", phoneNumber,
				"Code", code
			);

			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create("https://verify.twilio.com/v2/Services/" + verifyServiceSid + "/VerificationCheck"))
				.header("Authorization", basicAuth())
				.header("Content-Type", "application/x-www-form-urlencoded")
				.POST(HttpRequest.BodyPublishers.ofString(body))
				.build();

			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				return false;
			}
			JsonNode node = mapper.readTree(response.body());
			String status = node.path("status").asText("");
			return "approved".equalsIgnoreCase(status);
		} catch (Exception e) {
			return false;
		}
	}

	private void ensureConfig() {
		if (accountSid.isEmpty() || authToken.isEmpty() || verifyServiceSid.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "SMS service is not configured");
		}
	}

	private String basicAuth() {
		String raw = accountSid + ":" + authToken;
		return "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
	}

	private static String formEncode(String... kv) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < kv.length; i += 2) {
			if (i > 0) sb.append('&');
			sb.append(URLEncoder.encode(kv[i], StandardCharsets.UTF_8));
			sb.append('=');
			sb.append(URLEncoder.encode(kv[i + 1], StandardCharsets.UTF_8));
		}
		return sb.toString();
	}

	public record OtpSendResult(boolean devMode, String otp) {}
}
