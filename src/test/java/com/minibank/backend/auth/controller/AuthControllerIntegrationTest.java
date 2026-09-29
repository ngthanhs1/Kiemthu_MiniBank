package com.minibank.backend.auth.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	@DisplayName("IT_AUTH_01: Đăng nhập Admin thành công với tài khoản mặc định và nhận JWT Token")
	void adminLogin_Success() throws Exception {
		String loginJson = """
			{
				"identifier": "admin@gmail.com",
				"password": "123456"
			}
			""";

		mockMvc.perform(post("/api/admin/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginJson))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.accessToken", notNullValue()))
			.andExpect(jsonPath("$.user.username").value("admin@gmail.com"))
			.andExpect(jsonPath("$.user.roles[0]").value("ADMIN"));
	}

	@Test
	@DisplayName("IT_AUTH_02: Đăng nhập Admin thất bại khi nhập sai mật khẩu (401 Unauthorized)")
	void adminLogin_InvalidPassword_ReturnsUnauthorized() throws Exception {
		String loginJson = """
			{
				"identifier": "admin@gmail.com",
				"password": "wrong_password"
			}
			""";

		mockMvc.perform(post("/api/admin/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginJson))
			.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("IT_AUTH_03: Bắt lỗi Validation (400 Bad Request) khi bỏ trống identifier hoặc password")
	void adminLogin_EmptyFields_ReturnsBadRequest() throws Exception {
		String invalidJson = """
			{
				"identifier": "",
				"password": ""
			}
			""";

		mockMvc.perform(post("/api/admin/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(invalidJson))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("IT_AUTH_04: Luồng tích hợp Đăng ký người dùng mới và Đăng nhập bằng OTP trên Mobile")
	void mobileRegisterAndOtpLogin_Flow_Success() throws Exception {
		String phone = "0911223344";
		String email = "testuser_it@minibank.com";
		String password = "Password123";
		String deviceId = "device-it-01";

		// 1. Đăng ký tài khoản
		String registerJson = String.format("""
			{
				"phone": "%s",
				"email": "%s",
				"password": "%s",
				"fullName": "Test User IT",
				"deviceId": "%s"
			}
			""", phone, email, password, deviceId);

		mockMvc.perform(post("/api/mobile/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(registerJson))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.accessToken", notNullValue()))
			.andExpect(jsonPath("$.tokenType").value("Bearer"));

		// 2. Gửi mã OTP đăng nhập
		String sendOtpJson = String.format("""
			{
				"identifier": "%s",
				"password": "%s",
				"deviceId": "%s"
			}
			""", phone, password, deviceId);

		mockMvc.perform(post("/api/mobile/auth/login/otp/send")
				.contentType(MediaType.APPLICATION_JSON)
				.content(sendOtpJson))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.devMode").value(true))
			.andExpect(jsonPath("$.otp").value("123456"));

		// 3. Xác thực OTP để hoàn tất đăng nhập
		String verifyOtpJson = String.format("""
			{
				"identifier": "%s",
				"otpCode": "123456",
				"deviceId": "%s"
			}
			""", phone, deviceId);

		mockMvc.perform(post("/api/mobile/auth/login/verify")
				.contentType(MediaType.APPLICATION_JSON)
				.content(verifyOtpJson))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accessToken", notNullValue()))
			.andExpect(jsonPath("$.user.phone").value(phone));
	}

	@Test
	@DisplayName("IT_AUTH_05: Đăng ký thất bại khi số điện thoại đã tồn tại (409 Conflict)")
	void mobileRegister_DuplicatePhone_ReturnsConflict() throws Exception {
		String duplicatePhone = "0988776655";
		String registerJson = String.format("""
			{
				"phone": "%s",
				"email": "first@minibank.com",
				"password": "Password123",
				"fullName": "First User"
			}
			""", duplicatePhone);

		// Lần 1: Thành công
		mockMvc.perform(post("/api/mobile/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(registerJson))
			.andExpect(status().isCreated());

		// Lần 2: Trùng số điện thoại -> Báo lỗi 409
		String secondRegisterJson = String.format("""
			{
				"phone": "%s",
				"email": "second@minibank.com",
				"password": "Password123",
				"fullName": "Second User"
			}
			""", duplicatePhone);

		mockMvc.perform(post("/api/mobile/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(secondRegisterJson))
			.andExpect(status().isConflict());
	}

	@Test
	@DisplayName("IT_SEC_01: Truy cập API được bảo vệ khi không có Token (401 Unauthorized)")
	void protectedApiWithoutToken_ReturnsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/mobile/accounts/me"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("IT_SEC_02: Truy cập API được bảo vệ với JWT Token hợp lệ (200 OK)")
	void protectedApiWithValidToken_ReturnsSuccess() throws Exception {
		// 1. Đăng ký & lấy Token
		String phone = "0933445566";
		String registerJson = String.format("""
			{
				"phone": "%s",
				"email": "auth_test@minibank.com",
				"password": "Password123",
				"fullName": "Token User",
				"deviceId": "device-token-01"
			}
			""", phone);

		MvcResult registerResult = mockMvc.perform(post("/api/mobile/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(registerJson))
			.andExpect(status().isCreated())
			.andReturn();

		JsonNode root = objectMapper.readTree(registerResult.getResponse().getContentAsString());
		String token = root.path("accessToken").asText();
		assertThat(token).isNotEmpty();

		// 2. Gọi API bảo vệ kèm Header Authorization: Bearer <token>
		mockMvc.perform(get("/api/mobile/accounts/me")
				.header("Authorization", "Bearer " + token)
				.header("X-Device-Id", "device-token-01"))
			.andExpect(status().isOk());
	}
}