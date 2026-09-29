# 🔗 BƯỚC 3: KIỂM THỬ TÍCH HỢP (INTEGRATION TESTING) & TỰ ĐỘNG HÓA API (API AUTOMATION)

---
- **Môn học:** Kiểm định và Đánh giá Phần mềm (Software Testing & QA)
- **Giai đoạn:** Bước 3 / 5 trong lộ trình thực hiện bài tập lớn
- **Hệ thống mục tiêu:** MiniBank Digital Banking Backend API
- **Trạng thái thực thi:** ✅ ĐÃ HOÀN THÀNH - 100% TEST PASSED (30/30 Tests)
---

## 1. MỤC TIÊU BƯỚC 3
1. **Kiểm thử Tích hợp (Integration Testing):** Kiểm tra sự phối hợp toàn diện giữa các thành phần trong chu trình xử lý yêu cầu HTTP:
   $$\text{Client Request} \longrightarrow \text{Spring Security Filter} \longrightarrow \text{Controller} \longrightarrow \text{Service} \longrightarrow \text{JPA Repository} \longrightarrow \text{H2 In-Memory DB}$$
2. **Kiểm tra Bean Validation & HTTP Status Codes:** Đảm bảo hệ thống trả về đúng các mã trạng thái chuẩn quốc tế: `200 OK`, `201 CREATED`, `400 BAD REQUEST`, `401 UNAUTHORIZED`, `403 FORBIDDEN`, `409 CONFLICT`.
3. **Xây dựng Bộ Tự Động Hóa API (API Automation Testing Suite):** Xuất bản tệp **Postman Collection v2.1.0** chuẩn công nghiệp tích hợp sẵn các kịch bản kiểm thử tự động (Chai Assertions, trích xuất biến môi trường `Bearer Token`, kiểm tra tính toàn vẹn dữ liệu JSON).

---

## 2. CHI TIẾT LỚP KIỂM THỬ TÍCH HỢP `AuthControllerIntegrationTest.java`

Tệp nguồn: [`src/test/java/com/minibank/backend/auth/controller/AuthControllerIntegrationTest.java`](file:///d:/Code_Hoc/Kiemthu_MiniBank/src/test/java/com/minibank/backend/auth/controller/AuthControllerIntegrationTest.java)  
Sử dụng các annotation: `@SpringBootTest`, `@AutoConfigureMockMvc`, `@ActiveProfiles("test")`.

| Mã Test | Tên Ca Kiểm Thử | Kịch Bản & Khẳng Định Tích Hợp (Assertions) | Trạng Thái |
| :--- | :--- | :--- | :---: |
| **IT_AUTH_01** | `adminLogin_Success` | Gửi `POST /api/admin/auth/login` với tài khoản mặc định `admin@gmail.com` / `123456`. Khẳng định: HTTP `200 OK`, `tokenType = Bearer`, sinh ra chuỗi `accessToken`, vai trò người dùng có chứa `ADMIN`. | **PASSED** |
| **IT_AUTH_02** | `adminLogin_InvalidPassword` | Gửi `POST /api/admin/auth/login` với mật khẩu sai. Khẳng định: Spring Security trả về đúng mã lỗi `401 UNAUTHORIZED`. | **PASSED** |
| **IT_AUTH_03** | `adminLogin_EmptyFields` | Gửi body rỗng `{}` hoặc chuỗi trống. Khẳng định: `@Valid` chặn lại tại Controller và trả về `400 BAD REQUEST`. | **PASSED** |
| **IT_AUTH_04** | `mobileRegisterAndOtpLogin_Flow_Success` | **Luồng tích hợp liên hoàn (End-to-End):**<br>1. Gọi `POST /api/mobile/auth/register` $\rightarrow$ Nhận `201 CREATED`.<br>2. Gọi `POST /api/mobile/auth/login/otp/send` $\rightarrow$ Nhận OTP `123456`.<br>3. Gọi `POST /api/mobile/auth/login/verify` $\rightarrow$ Nhận `200 OK` kèm `accessToken`. | **PASSED** |
| **IT_AUTH_05** | `mobileRegister_DuplicatePhone` | Đăng ký người dùng 2 lần với cùng một số điện thoại. Khẳng định: Lần 1 thành công (201), lần 2 bị từ chối với mã lỗi `409 CONFLICT`. | **PASSED** |
| **IT_SEC_01** | `protectedApiWithoutToken` | Gửi `GET /api/mobile/accounts/me` khi **không có** Header Authorization. Khẳng định: Spring Security Filter chặn lại với mã lỗi `401 UNAUTHORIZED`. | **PASSED** |
| **IT_SEC_02** | `protectedApiWithValidToken` | Đăng ký tài khoản $\rightarrow$ Lấy JWT Token $\rightarrow$ Gửi `GET /api/mobile/accounts/me` kèm Header `Authorization: Bearer <token>` và `X-Device-Id`. Khẳng định: Vượt qua bộ lọc bảo mật, trả về `200 OK`. | **PASSED** |

---

## 3. BỘ SƯU TẬP KIỂM THỬ TỰ ĐỘNG HÓA POSTMAN (API AUTOMATION)

Đã tạo tệp định dạng chuẩn Postman Collection v2.1.0 tại thư mục gốc:  
📄 **[`MiniBank_API_Automation.postman_collection.json`](file:///d:/Code_Hoc/Kiemthu_MiniBank/MiniBank_API_Automation.postman_collection.json)**

### 3.1. Cấu trúc các thư mục kiểm thử trong Collection
```text
MiniBank API Automation Test Suite/
├── 1. Admin Portal APIs/
│   ├── TC_API_ADM_01: Admin Login (Tự động lưu admin_token)
│   └── TC_API_ADM_02: Get Dashboard Stats (Sử dụng admin_token)
├── 2. Mobile Auth & User APIs/
│   ├── TC_API_MOB_01: Mobile Register (Đăng ký tài khoản)
│   ├── TC_API_MOB_02: Send Login OTP (Tự động trích xuất otp_code)
│   ├── TC_API_MOB_03: Verify Login OTP (Tự động lưu user_token)
│   └── TC_API_MOB_04: Get My Accounts (Gọi API với Bearer user_token)
├── 3. Products & Services APIs/
│   ├── TC_API_PROD_01: Get Savings Products (Kiểm tra danh sách gói tiết kiệm)
│   └── TC_API_PROD_02: Get Loan Products (Kiểm tra danh sách gói vay)
└── 4. Security & Negative APIs/
    ├── TC_API_SEC_01: Protected API without Token (Kiểm tra chặn 401)
    └── TC_API_SEC_02: Admin Login with Wrong Password (Kiểm tra chặn 401)
```

### 3.2. Đoạn mã Assertions tự động hóa mẫu (Postman Test Scripts)
Tất cả các Request đều được tích hợp mã kiểm thử tự động, ví dụ tại ca `Admin Login`:
```javascript
pm.test("Status code is 200 OK", function () {
    pm.response.to.have.status(200);
});

pm.test("Token type is Bearer and token exists", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.tokenType).to.eql("Bearer");
    pm.expect(jsonData.accessToken).to.not.be.empty;
    // Tự động gán biến môi trường dùng cho các request tiếp theo
    pm.collectionVariables.set("admin_token", jsonData.accessToken);
});

pm.test("User role contains ADMIN", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.user.roles).to.include("ADMIN");
});
```

### 3.3. Hướng dẫn chạy tự động hóa với Newman (Command Line)
Bạn có thể chạy tự động toàn bộ Collection này từ dòng lệnh và xuất báo cáo kiểm thử HTML:
```bash
# Cài đặt Newman (nếu chưa có)
npm install -g newman newman-reporter-htmlextra

# Chạy kiểm thử tự động toàn bộ API và xuất báo cáo
newman run MiniBank_API_Automation.postman_collection.json -r cli,htmlextra --reporter-htmlextra-export target/api-test-report.html
```

---

## 4. KẾT QUẢ THỰC THI TOÀN BỘ DỰ ÁN (BƯỚC 1 + 2 + 3)

### 4.1. Kết quả thực thi Maven Surefire
```text
[INFO] Running com.minibank.backend.BankingBackendApplicationTests
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 11.23 s
[INFO] Running com.minibank.backend.auth.controller.AuthControllerIntegrationTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 2.145 s
[INFO] Running com.minibank.backend.loan.service.LoanServiceTest
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.795 s
[INFO] Running com.minibank.backend.transaction.service.TransferServiceTest
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.641 s
[INFO] 
[INFO] Results:
[INFO] Tests run: 30, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] --- jacoco:0.8.12:report (report) @ banking-backend ---
[INFO] Loading execution data file D:\Code_Hoc\Kiemthu_MiniBank\target\jacoco.exec
[INFO] Analyzed bundle '' with 302 classes
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  17.116 s
```

### 4.2. Bảng tổng kết số lượng ca kiểm thử lũy kế
| Hạng mục kiểm thử | Lớp / Bộ kiểm thử | Số ca test | Trạng thái |
| :--- | :--- | :---: | :---: |
| **Kiểm tra khởi động Context** | `BankingBackendApplicationTests` | 1 | **100% PASS** |
| **Kiểm thử đơn vị Khoản vay** | `LoanServiceTest` | 10 | **100% PASS** |
| **Kiểm thử đơn vị Chuyển tiền** | `TransferServiceTest` | 12 | **100% PASS** |
| **Kiểm thử tích hợp Auth & Bảo mật** | `AuthControllerIntegrationTest` | 7 | **100% PASS** |
| **Tổng cộng lũy kế** | **4 Lớp kiểm thử** | **30** | **30/30 (100% PASS)** |

---

## 5. BƯỚC TIẾP THEO (GIAI ĐOẠN 4)
Sau khi hoàn thành Kiểm thử tích hợp và Tự động hóa API, bước tiếp theo sẽ là:
- **Bước 4: Kiểm thử Xử lý Đồng thời (Concurrency / Race Condition) & Kiểm thử Tải (Load Testing)**.
- Xây dựng bài kiểm tra đa luồng chứng minh hệ thống không bị lỗi **Rút tiền kép (Double Spending)** khi nhiều yêu cầu giao dịch xảy ra cùng lúc tại một phần nghìn giây.