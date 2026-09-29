# 🎯 BƯỚC 1: THIẾT LẬP HẠ TẦNG KIỂM THỬ VÀ ĐẶC TẢ CA KIỂM THỬ (TEST CASE SPECIFICATION)

---
- **Môn học:** Kiểm định và Đánh giá Phần mềm (Software Testing & QA)
- **Giai đoạn:** Bước 1 / 5 trong lộ trình thực hiện bài tập lớn
- **Hệ thống mục tiêu:** MiniBank Digital Banking Backend API
- **Trạng thái thực thi:** ✅ ĐÃ HOÀN THÀNH (Verified & Build Success)
---

## 1. MỤC TIÊU BƯỚC 1
1. **Chuẩn hóa hạ tầng kiểm thử:** Cấu hình môi trường chạy kiểm thử độc lập, không phụ thuộc vào cơ sở dữ liệu production/cloud (sử dụng H2 In-Memory Database mô phỏng PostgreSQL).
2. **Tích hợp công cụ đo độ bao phủ (Code Coverage):** Tích hợp thành công plugin `jacoco-maven-plugin` để đo lường tỷ lệ bao phủ mã nguồn theo dòng lệnh (Line Coverage) và rẽ nhánh (Branch Coverage).
3. **Xây dựng Bảng Đặc tả Ca Kiểm thử Chi tiết (Test Case Specification):** Thiết kế toàn bộ kịch bản kiểm thử áp dụng các kỹ thuật hộp đen chuẩn (Phân vùng tương đương EP, Phân tích giá trị biên BVA, Bảng quyết định Decision Table, Chuyển trạng thái State Transition).

---

## 2. CÁC THAY ĐỔI CẤU HÌNH HẠ TẦNG ĐÃ THỰC HIỆN

### 2.1. Bổ sung Dependency & Plugin trong `pom.xml`
- **H2 Database (Test Scope):** Cho phép khởi tạo cơ sở dữ liệu quan hệ ảo trong bộ nhớ RAM khi chạy kiểm thử tự động.
  ```xml
  <dependency>
      <groupId>com.h2database</groupId>
      <artifactId>h2</artifactId>
      <scope>test</scope>
  </dependency>
  ```
- **JaCoCo Maven Plugin:** Tự động theo dõi việc thực thi mã nguồn và xuất báo cáo trực quan dưới dạng HTML.
  ```xml
  <plugin>
      <groupId>org.jacoco</groupId>
      <artifactId>jacoco-maven-plugin</artifactId>
      <version>0.8.12</version>
      <executions>
          <execution>
              <goals>
                  <goal>prepare-agent</goal>
              </goals>
          </execution>
          <execution>
              <id>report</id>
              <phase>test</phase>
              <goals>
                  <goal>report</goal>
              </goals>
          </execution>
      </executions>
  </plugin>
  ```

### 2.2. Tạo file cấu hình môi trường test `src/test/resources/application-test.properties`
Tách biệt hoàn toàn môi trường kiểm thử với môi trường chạy thật:
```properties
spring.application.name=banking-backend-test
spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.flyway.enabled=false
app.jwt.secret=test-secret-key-at-least-32-chars-long-123456789
app.jwt.issuer=minibank-test
app.jwt.access-token-ttl-seconds=3600
sms.dev-mode=true
app.otp.debug-return=true
```

### 2.3. Cập nhật kiểm thử khởi động `BankingBackendApplicationTests.java`
Sử dụng `@ActiveProfiles("test")` để tự động kích hoạt file cấu hình test trên H2 Database:
```java
@SpringBootTest
@ActiveProfiles("test")
class BankingBackendApplicationTests {
    @Test
    void contextLoads() {
    }
}
```

### 2.4. Kết quả nghiệm thu hạ tầng
- Lệnh thực thi: `$env:JAVA_HOME="C:\Program Files\Java\jdk-24"; .\mvnw.cmd test`
- Trạng thái biên dịch: **`BUILD SUCCESS`**
- Số ca test đã chạy: **1/1 Passed (0 Failures, 0 Errors)**
- Báo cáo JaCoCo đã được sinh tự động tại: `target/site/jacoco/index.html`

---

## 3. BẢNG ĐẶC TẢ CA KIỂM THỬ CHI TIẾT (TEST CASE SPECIFICATION)

### Phân hệ 1: Xác thực & Quản trị Phiên (Authentication & Security)

| ID | Tên ca kiểm thử | Kỹ thuật áp dụng | Tiền điều kiện | Dữ liệu đầu vào | Các bước thực hiện | Kết quả mong đợi | Mức độ |
| :--- | :--- | :---: | :--- | :--- | :--- | :--- | :---: |
| **TC_AUTH_01** | Gửi OTP SĐT hợp lệ | EP (Hợp lệ) | SĐT chưa khóa | SĐT: `0981712585` | 1. Gọi `POST /api/mobile/auth/send-otp` | Trả về `200 OK`, OTP được tạo trong hệ thống | High |
| **TC_AUTH_02** | Gửi OTP SĐT sai định dạng | EP (Không hợp lệ) | Không | SĐT: `12345` hoặc `abc` | 1. Gọi `POST /api/mobile/auth/send-otp` | Trả về `400 Bad Request`, thông báo định dạng sai | Medium |
| **TC_AUTH_03** | Xác thực OTP thành công | Happy Path | Đã gửi OTP | SĐT: `0981712585`, OTP: `123456` | 1. Gọi `POST /api/mobile/auth/verify-otp` | Trả về `200 OK`, cấp JWT Token hợp lệ | Critical |
| **TC_AUTH_04** | Xác thực sai mã OTP | Error Path | Đã gửi OTP | OTP nhập: `999999` | 1. Gọi `POST /api/mobile/auth/verify-otp` | Trả về `400 Bad Request`, số lần thử giảm 1 | High |
| **TC_AUTH_05** | Khóa OTP sau 5 lần nhập sai | BVA / State | Đã nhập sai 4 lần | OTP nhập: `000000` (lần 5) | 1. Nhập sai lần thứ 5 | Trả về `400`, vô hiệu hóa phiên OTP | High |
| **TC_AUTH_06** | Đăng nhập Admin thành công | Happy Path | Admin tồn tại | Email: `admin@gmail.com`, Pass: `123456` | 1. Gọi `POST /api/admin/auth/login` | Trả về `200 OK`, Token chứa Role `ADMIN` | High |
| **TC_AUTH_07** | Đăng nhập Admin sai Pass | Error Path | Admin tồn tại | Email: `admin@gmail.com`, Pass: `sai_pass` | 1. Gọi `POST /api/admin/auth/login` | Trả về `401 Unauthorized` | High |
| **TC_AUTH_08** | Khóa thiết bị lạ (Device Lock) | Security / State | User đã gán Device ID: `dev-01` | Header `X-Device-Id: dev-02` | 1. Gọi API xem thông tin cá nhân | Bị từ chối hoặc yêu cầu xác thực OTP thiết bị mới | High |

---

### Phân hệ 2: Chuyển tiền & Quản lý Số dư (Transfer & Ledger)

Giả sử Tài khoản gửi **A** có số dư khả dụng = **5.000.000 VNĐ**, Tài khoản nhận **B** có số dư = **1.000.000 VNĐ**.

| ID | Tên ca kiểm thử | Kỹ thuật áp dụng | Tiền điều kiện | Dữ liệu đầu vào | Các bước thực hiện | Kết quả mong đợi | Mức độ |
| :--- | :--- | :---: | :--- | :--- | :--- | :--- | :---: |
| **TC_TRF_01** | Chuyển tiền nội bộ thành công | EP (Hợp lệ) | TK A đủ số dư | Người nhận: B, Số tiền: 500.000 đ | 1. Khởi tạo giao dịch<br>2. Xác nhận OTP | TK A = 4.500.000 đ, TK B = 1.500.000 đ, sinh 2 bản ghi Ledger | Critical |
| **TC_TRF_02** | Chuyển đúng số dư khả dụng (Rút hết tiền) | BVA (Biên trên số dư) | TK A có 5.000.000 đ | Số tiền: 5.000.000 đ | 1. Khởi tạo giao dịch<br>2. Xác nhận OTP | TK A = 0 đ, TK B = 6.000.000 đ, trạng thái `SUCCESS` | Critical |
| **TC_TRF_03** | Chuyển vượt số dư 1 đồng | BVA (Biên ngoài số dư) | TK A có 5.000.000 đ | Số tiền: 5.000.001 đ | 1. Khởi tạo giao dịch | Báo lỗi `400 Insufficient Balance`, không trừ tiền | Critical |
| **TC_TRF_04** | Chuyển số tiền biên tối thiểu | BVA (Biên tối thiểu) | TK A đủ số dư | Số tiền: 1.000 đ | 1. Khởi tạo giao dịch<br>2. Xác nhận OTP | Giao dịch thành công, TK A trừ đúng 1.000 đ | High |
| **TC_TRF_05** | Chuyển số tiền dưới mức tối thiểu | BVA (Dưới biên) | TK A đủ số dư | Số tiền: 999 đ | 1. Khởi tạo giao dịch | Báo lỗi `400 Min Transfer Amount is 1,000` | High |
| **TC_TRF_06** | Chuyển số tiền âm hoặc bằng 0 | EP (Không hợp lệ) | TK A hợp lệ | Số tiền: `0` hoặc `-50000` | 1. Khởi tạo giao dịch | Báo lỗi `400 Validation Error` | High |
| **TC_TRF_07** | Chuyển đến tài khoản chính mình | Business Rule | TK A | Người nhận: chính TK A | 1. Khởi tạo giao dịch | Báo lỗi `400 Cannot transfer to same account` | Medium |
| **TC_TRF_08** | Chuyển đến tài khoản không tồn tại | Error Path | TK A | Người nhận: `999999999` | 1. Khởi tạo giao dịch | Báo lỗi `404 Account Not Found` | High |
| **TC_TRF_09** | Giao dịch lớn cần kiểm soát duyệt | Boundary / Rule | TK A có 150tr | Số tiền: 120.000.000 đ (> 100tr) | 1. Khởi tạo giao dịch | Trạng thái chuyển sang `PENDING_APPROVAL`, không chuyển ngay | High |
| **TC_TRF_10** | Quét mã VietQR chuyển tiền | Integration | Mã QR hợp lệ | QR code chứa thông tin TK B | 1. Giải mã QR<br>2. Tạo giao dịch | Thông tin người nhận và số tiền tự động khớp chuẩn xác | Medium |

---

### Phân hệ 3: Tiền gửi tiết kiệm (Savings)

| ID | Tên ca kiểm thử | Kỹ thuật áp dụng | Tiền điều kiện | Dữ liệu đầu vào | Các bước thực hiện | Kết quả mong đợi | Mức độ |
| :--- | :--- | :---: | :--- | :--- | :--- | :--- | :---: |
| **TC_SAV_01** | Mở sổ tiết kiệm thành công | Happy Path | TK thanh toán có 10tr | Gói 6 tháng, Gửi: 5.000.000 đ, Lãi: 6.5%/năm | 1. Gửi `POST /api/mobile/savings` | Trừ 5tr ở TK thanh toán, tạo bản ghi Saving `ACTIVE` | High |
| **TC_SAV_02** | Mở sổ khi không đủ số dư | Error Path | TK thanh toán có 2tr | Gửi: 5.000.000 đ | 1. Gửi `POST /api/mobile/savings` | Báo lỗi `400 Insufficient Balance` | High |
| **TC_SAV_03** | Tất toán sổ tiết kiệm đúng hạn | State / Calc | Sổ đã đến ngày đáo hạn | Gửi 10.000.000 đ, 6 tháng, 6% | 1. Gọi API tất toán | Tiền gốc (10tr) + Lãi (300.000 đ) được chuyển về TK thanh toán | Critical |
| **TC_SAV_04** | Tất toán sổ tiết kiệm trước hạn | State / Calc | Sổ chưa đến ngày đáo hạn | Rút sau 2 tháng | 1. Gọi API tất toán trước hạn | Lãi được tính theo lãi suất không kỳ hạn (0.1%/năm), sổ đóng | High |

---

### Phân hệ 4: Khoản vay & Lịch trả nợ (Loans & Repayment Schedule)

| ID | Tên ca kiểm thử | Kỹ thuật áp dụng | Tiền điều kiện | Dữ liệu đầu vào | Các bước thực hiện | Kết quả mong đợi | Mức độ |
| :--- | :--- | :---: | :--- | :--- | :--- | :--- | :---: |
| **TC_LOAN_01** | Đăng ký khoản vay thành công | State Transition | Đã KYC | Gói vay tiêu dùng: 24.000.000 đ, Kỳ hạn: 12 tháng | 1. Nộp hồ sơ vay | Hồ sơ ở trạng thái `SUBMITTED`, chờ duyệt | High |
| **TC_LOAN_02** | Tính toán lịch trả nợ từng tháng | Math Accuracy | Khoản vay 12.000.000 đ | 12 tháng, Lãi 12%/năm | 1. Gọi API xem Schedule | Bảng lịch gồm 12 kỳ, tổng tiền gốc đúng 12.000.000 đ | Critical |
| **TC_LOAN_03** | Cán bộ tín dụng duyệt khoản vay | Role / State | Hồ sơ `SUBMITTED` | Token Cán bộ `LOAN_OFFICER` | 1. Gọi API duyệt vay | Hồ sơ chuyển sang trạng thái `APPROVED` | High |
| **TC_LOAN_04** | Giải ngân khoản vay vào tài khoản | State Transition | Hồ sơ `APPROVED` | ID khoản vay | 1. Gọi API giải ngân | TK thanh toán của khách được cộng đúng số tiền vay | Critical |
| **TC_LOAN_05** | Chặn thao tác giải ngân khi chưa duyệt | Security / State | Hồ sơ đang `SUBMITTED` | Gọi API giải ngân | 1. Cố tình gọi giải ngân | Báo lỗi `400 Invalid Loan State` | High |

---

## 4. BƯỚC TIẾP THEO (GIAI ĐOẠN 2)
Sau khi hạ tầng kiểm thử và bộ ca kiểm thử đã được chuẩn hóa, bước tiếp theo sẽ là:
- **Bước 2:** Hiện thực hóa các ca kiểm thử đơn vị (**Unit Tests**) bằng **JUnit 5 + Mockito** cho phân hệ cốt lõi nhất: `TransferServiceTest` và `LoanServiceTest`.
- Đo lường và xuất báo cáo tỷ lệ bao phủ mã nguồn (**JaCoCo Coverage**).
