# 📖 HƯỚNG DẪN CÁCH VẬN HÀNH, KHỞI ĐỘNG VÀ CHẠY KIỂM THỬ (TESTING USER GUIDE)
## HỆ THỐNG NGÂN HÀNG SỐ MINIBANK BACKEND

---
- **Tài liệu hướng dẫn:** Thực thi, Khởi động ứng dụng & Chạy toàn bộ các cấp độ kiểm thử
- **Hệ điều hành:** Windows (PowerShell / CMD) & Linux / macOS
- **Môi trường yêu cầu:** Java JDK 21+ / Apache Maven / Postman / Apache JMeter
---

## 📑 MỤC LỤC
1. [Yêu Cầu Môi Trường (Prerequisites)](#1-yêu-cầu-môi-trường-prerequisites)
2. [Hướng Dẫn Khởi Động Hệ Thống Backend (Spring Boot Core)](#2-hướng-dẫn-khởi-động-hệ-thống-backend)
3. [Hướng Dẫn Khởi Động Cổng Quản Trị Frontend (Admin Portal)](#3-hướng-dẫn-khởi-động-cổng-quản-trị-frontend-admin-portal)
4. [Hướng Dẫn Chạy Toàn Bộ Kiểm Thử Tự Động (Maven Test)](#4-hướng-dẫn-chạy-toàn-bộ-kiểm-thử-tự-động-maven-test)
5. [Hướng Dẫn Xem Báo Cáo Đo Độ Bao Phủ Mã Nguồn (JaCoCo Coverage)](#5-hướng-dẫn-xem-báo-cáo-đo-độ-bao-phủ-mã-nguồn-jacoco-coverage)
6. [Hướng Dẫn Chạy Kiểm Thử Tự Động Hóa API (Postman / Newman)](#6-hướng-dẫn-chạy-kiểm-thử-tự-động-hóa-api-postman--newman)
7. [Hướng Dẫn Chạy Kiểm Thử Tải & Hiệu Năng (Apache JMeter)](#7-hướng-dẫn-chạy-kiểm-thử-tải--hiệu-năng-apache-jmeter)
8. [Hướng Dẫn Chạy Kiểm Thử Tự Động Giao Diện (Selenium & Selenium IDE)](#8-hướng-dẫn-chạy-kiểm-thử-tự-động-giao-diện-selenium--selenium-ide)
9. [Dữ Liệu Thử Nghiệm & Tài Khoản Mẫu (Test Data)](#9-dữ-liệu-thử-nghiệm--tài-khoản-mẫu-test-data)
10. [Xử Lý Các Lỗi Thường Gặp (Troubleshooting / FAQs)](#10-xử-lý-các-lỗi-thường-gặp-troubleshooting--faqs)

---

## 1. Yêu Cầu Môi Trường (Prerequisites)

Trước khi bắt đầu, hãy đảm bảo máy tính của bạn đã cài đặt:
1. **Java Development Kit (JDK 21 trở lên):**
   - Kiểm tra bằng lệnh: `java -version`
   - Đảm bảo biến môi trường `JAVA_HOME` trỏ đúng vào thư mục JDK (Ví dụ: `C:\Program Files\Java\jdk-24`).
2. **Maven Wrapper:** Có sẵn trong thư mục dự án (`mvnw.cmd` cho Windows, `mvnw` cho Linux/macOS).
3. **Node.js (v18.17+ hoặc v20+):** Dành cho Frontend Next.js Admin Portal. Kiểm tra bằng lệnh: `node -v` và `npm -v`.
4. **Python (3.10+) & Selenium (v4.51+):** Dành cho kiểm thử giao diện tự động.
   - Cài đặt thư viện: `pip install selenium`
5. **Trình duyệt Web:** Google Chrome hoặc Microsoft Edge (phiên bản mới nhất).
6. **Công cụ phụ trợ (Tùy chọn):**
   - **Postman Desktop** hoặc **Node.js/Newman** (để chạy API test).
   - **Apache JMeter 5.6+** (để chạy bài test tải hiệu năng).
   - **Selenium IDE Extension** trên Chrome Web Store (để xem/chạy file `.side`).

---

## 2. Hướng Dẫn Khởi Động Hệ Thống Backend

### Bước 2.1: Cấu hình tệp môi trường `.env`
1. Tại thư mục gốc `d:\Code_Hoc\Kiemthu_MiniBank`, tạo tệp `.env` (bạn có thể sao chép từ `.env.example`).
2. Điền thông tin kết nối cơ sở dữ liệu PostgreSQL của bạn:

```properties
# Thông tin kết nối PostgreSQL (Local hoặc Neon Cloud)
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/minibank_db
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=your_password

# Cấu hình JWT Token (Bí mật tối thiểu 32 ký tự)
JWT_SECRET=super-secret-key-minibank-development-2026-xyz-32chars
JWT_ISSUER=minibank
JWT_ACCESS_TOKEN_TTL_SECONDS=86400

# Chế độ phát triển (Sử dụng mã OTP mặc định 123456 không cần SMS thực)
sms.dev-mode=true
OTP_DEBUG_RETURN=true

# Cổng dịch vụ
server.port=8080
```

> **Ghi chú về Database:** Công cụ **Flyway** sẽ tự động khởi tạo 25 bảng cơ sở dữ liệu và nạp dữ liệu mẫu ban đầu khi ứng dụng khởi chạy lần đầu tiên.

### Bước 2.2: Lệnh khởi động Server

- **Trên Windows (PowerShell):**
  ```powershell
  # Thiết lập JAVA_HOME nếu chưa cấu hình trong biến môi trường Windows
  $env:JAVA_HOME = "C:\Program Files\Java\jdk-24"

  # Chạy ứng dụng Spring Boot
  .\mvnw.cmd spring-boot:run
  ```

- **Trên Linux / macOS:**
  ```bash
  export JAVA_HOME="/path/to/jdk-21"
  ./mvnw spring-boot:run
  ```

Khi màn hình xuất hiện thông báo:
```text
Started BankingBackendApplication in X.XXX seconds (process running for X.XXX)
```
Hệ thống đã sẵn sàng phục vụ tại địa chỉ: **`http://localhost:8080`**.

---

## 3. Hướng Dẫn Khởi Động Cổng Quản Trị Frontend (Admin Portal)

Cổng quản trị MiniBank Admin Portal được xây dựng trên nền tảng Next.js 16, React 19 và Tailwind CSS 4. Cả hai đường dẫn thư mục rontend/ hoặc ontend/ đều trỏ cùng vào một thư mục vật lý (thông qua liên kết Directory Junction).

### Bước 3.1: Kiểm tra cấu hình biến môi trường (.env.local)
Trong thư mục rontend/, tệp .env.local đã được cấu hình trỏ về Backend cổng 8080:
`ash
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
`

### Bước 3.2: Cài đặt gói thư viện (nếu chạy lần đầu)
Mở một cửa sổ dòng lệnh PowerShell mới và di chuyển vào thư mục frontend:
`powershell
cd d:\Code_Hoc\Kiemthu_MiniBank\frontend
npm install
`

### Bước 3.3: Khởi động máy chủ phát triển Frontend
`powershell
npm run dev
`
Khi màn hình hiển thị:
`	ext
  ▲ Next.js 16.x.x
  - Local:        http://localhost:3000
  - Environments: .env.local
 ✓ Starting...
 ✓ Ready in 1500ms
`
Cổng quản trị đã sẵn sàng tại địa chỉ: **http://localhost:3000** (truy cập màn hình đăng nhập tại http://localhost:3000/login).

---

## 4. Hướng Dẫn Chạy Toàn Bộ Kiểm Thử Tự Động (Maven Test)

Hệ thống kiểm thử đã được cấu hình chạy trên cơ sở dữ liệu ảo **H2 In-Memory Database** độc lập (`application-test.properties`), vì vậy bạn **không cần bật PostgreSQL** vẫn có thể chạy toàn bộ 31 bài test một cách hoàn hảo!

### 3.1. Chạy tất cả 31 ca kiểm thử cùng lúc
Mở PowerShell tại thư mục dự án và chạy:
```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-24"
.\mvnw.cmd test
```

**Kết quả mong đợi:**
```text
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0 -- BankingBackendApplicationTests
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0 -- AuthControllerIntegrationTest
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0 -- LoanServiceTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0 -- TransferConcurrencyIntegrationTest
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0 -- TransferServiceTest
[INFO] 
[INFO] Results:
[INFO] Tests run: 31, Failures: 0, Errors: 0, Skipped: 0
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

### 3.2. Chạy riêng từng bài kiểm thử cụ thể
Nếu muốn chạy riêng một phân hệ nào đó để kiểm tra nhanh:

- **Chỉ chạy kiểm thử chuyển tiền (`TransferServiceTest`):**
  ```powershell
  .\mvnw.cmd test -Dtest=TransferServiceTest
  ```
- **Chỉ chạy kiểm thử khoản vay (`LoanServiceTest`):**
  ```powershell
  .\mvnw.cmd test -Dtest=LoanServiceTest
  ```
- **Chỉ chạy kiểm thử tích hợp Auth & Security (`AuthControllerIntegrationTest`):**
  ```powershell
  .\mvnw.cmd test -Dtest=AuthControllerIntegrationTest
  ```
- **Chỉ chạy bài test tranh chấp tài nguyên đa luồng (`TransferConcurrencyIntegrationTest`):**
  ```powershell
  .\mvnw.cmd test -Dtest=TransferConcurrencyIntegrationTest
  ```

---

## 5. Hướng Dẫn Xem Báo Cáo Đo Độ Bao Phủ Mã Nguồn (JaCoCo Coverage)

Sau khi chạy lệnh `.\mvnw.cmd test`, plugin JaCoCo sẽ tự động tổng hợp độ bao phủ mã nguồn theo dòng lệnh và rẽ nhánh.

### Cách mở báo cáo trên trình duyệt:
1. Mở thư mục dự án trên máy: `d:\Code_Hoc\Kiemthu_MiniBank\target\site\jacoco\`
2. Nhấp đúp chuột vào tệp **`index.html`** để mở trên trình duyệt (Chrome, Edge, Firefox).
3. Hoặc mở trực tiếp từ PowerShell:
   ```powershell
   Start-Process "target\site\jacoco\index.html"
   ```
4. **Nội dung hiển thị:**
   - **Instruction / Line Coverage:** Tỉ lệ các dòng code thực tế được thực thi trong quá trình test.
   - **Branch Coverage:** Tỉ lệ các nhánh `if / else` và switch-case được bao phủ.
   - Chi tiết theo từng package: `transaction.service`, `loan.service`, `auth.controller`,...

---

## 6. Hướng Dẫn Chạy Kiểm Thử Tự Động Hóa API (Postman / Newman)

Tệp kịch bản kiểm thử API chuẩn Postman Collection đã được tạo sẵn tại:  
📄 **[`MiniBank_API_Automation.postman_collection.json`](file:///d:/Code_Hoc/Kiemthu_MiniBank/MiniBank_API_Automation.postman_collection.json)**

### Cách 1: Chạy bằng giao diện Postman Desktop (Giao diện trực quan)
1. Khởi động ứng dụng MiniBank Backend (`.\mvnw.cmd spring-boot:run`).
2. Mở ứng dụng **Postman**.
3. Nhấp nút **Import** ở góc trên bên trái $\rightarrow$ Chọn tệp `MiniBank_API_Automation.postman_collection.json`.
4. Nhấp chuột phải vào Collection vừa import $\rightarrow$ Chọn **Run Collection**.
5. Nhấp nút màu cam **Run MiniBank API Automation Test Suite**.
6. **Kết quả:** Toàn bộ các request sẽ tự động chạy liên hoàn từ Đăng ký, Lấy OTP, Đăng nhập, Tra cứu tài khoản và kiểm tra mã trạng thái HTTP Status (`200 OK`, `201 Created`, `401 Unauthorized`).

### Cách 2: Chạy tự động bằng dòng lệnh với Newman (Xuất báo cáo HTML)
Nếu máy bạn đã cài Node.js, bạn có thể chạy bằng dòng lệnh không cần mở Postman:
```bash
# 1. Cài đặt Newman và công cụ xuất báo cáo HTML (chỉ cần chạy 1 lần)
npm install -g newman newman-reporter-htmlextra

# 2. Chạy tự động toàn bộ API và xuất file báo cáo HTML
newman run MiniBank_API_Automation.postman_collection.json -r cli,htmlextra --reporter-htmlextra-export target/api-report.html

# 3. Mở báo cáo kết quả API
Start-Process "target\api-report.html"
```

---

## 7. Hướng Dẫn Chạy Kiểm Thử Tải & Hiệu Năng (Apache JMeter)

Tệp kịch bản kiểm thử tải Apache JMeter đã được tạo sẵn tại:  
📄 **[`MiniBank_Performance_TestPlan.jmx`](file:///d:/Code_Hoc/Kiemthu_MiniBank/MiniBank_Performance_TestPlan.jmx)**

### Cách 1: Chạy bằng giao diện đồ họa JMeter GUI
1. Khởi động ứng dụng MiniBank Backend (`http://localhost:8080`).
2. Mở phần mềm Apache JMeter: chạy tệp `bin/jmeter.bat` (trên Windows).
3. Vào **File $\rightarrow$ Open** $\rightarrow$ Chọn tệp `MiniBank_Performance_TestPlan.jmx`.
4. Nhấp nút **Start (màu xanh lá cây ▶)** trên thanh công cụ để bắt đầu bắn tải 50 - 100 người dùng đồng thời.
5. Xem kết quả đo lường độ trễ (Latency, Response Time, Throughput, Error %) tại các mục:
   - **Summary Report**
   - **Aggregate Report**
   - **View Results Tree**

### Cách 2: Chạy ở chế độ Non-GUI từ dòng lệnh (Khuyến nghị cho kiểm thử chuẩn)
```bash
# Chạy bắn tải và xuất Dashboard biểu đồ HTML
jmeter -n -t MiniBank_Performance_TestPlan.jmx -l target/jmeter-results.jtl -e -o target/jmeter-dashboard/

# Mở Dashboard báo cáo đồ thị trực quan
Start-Process "target\jmeter-dashboard\index.html"
```

---

## 8. Hướng Dẫn Chạy Kiểm Thử Tự Động Giao Diện (Selenium & Selenium IDE)

### 8.1. Chạy 10 ca kiểm thử tự động với Selenium WebDriver (Python)

Toàn bộ 10 ca kiểm thử UI (UI_TC_01 đến UI_TC_10) đã được đóng gói trong tệp 	est_automation/selenium/test_admin_portal_ui.py.

1. **Cài đặt thư viện Selenium (nếu chưa cài):**
   `powershell
   pip install selenium
   `

2. **Đảm bảo Frontend đang chạy tại cổng 3000** (xem Mục 3).

3. **Chạy bộ kiểm thử tự động:**
   `powershell
   cd d:\Code_Hoc\Kiemthu_MiniBank
   python test_automation/selenium/test_admin_portal_ui.py
   `
   *Script tự động cấu hình chạy ở chế độ Headless Chrome (--headless=new) và in kết quả chi tiết từng ca test:*
   `	ext
   Ran 10 tests in 8.421s

   OK
   `

---

### 8.2. Chạy kịch bản trực quan với Selenium IDE

Dự án kiểm thử Selenium IDE đã được thiết lập sẵn trong tệp 	est_automation/selenium/MiniBank_Admin_UI_Test.side.

1. Mở trình duyệt Google Chrome hoặc Microsoft Edge đã cài tiện ích mở rộng **Selenium IDE**.
2. Nhấp vào biểu tượng Selenium IDE trên thanh công cụ trình duyệt.
3. Chọn **'Open an existing project'** và duyệt đến tệp:
   d:\Code_Hoc\Kiemthu_MiniBank\test_automation\selenium\MiniBank_Admin_UI_Test.side
4. Chọn kịch bản kiểm thử trong danh sách bên trái (Ví dụ: TC01_Login_Admin_Success hoặc TC02_Navigate_Loan_Approval).
5. Bấm nút **'Run current test'** (hoặc 'Run all tests') để quan sát trình duyệt tự động điền form, click nút và xác thực các điều kiện giao diện.

---

## 9. Dữ Liệu Thử Nghiệm & Tài Khoản Mẫu (Test Data)

Hệ thống đã nạp sẵn các tài khoản mẫu sau để kiểm thử thủ công qua Swagger/Postman:

### 7.1. Tài khoản Quản trị viên (Admin Portal)
- **URL API Đăng nhập:** `POST http://localhost:8080/api/admin/auth/login`
- **Body JSON:**
  ```json
  {
      "identifier": "admin@gmail.com",
      "password": "123456"
  }
  ```
- **Quyền:** `SUPER_ADMIN`, `ADMIN` (Toàn quyền quản trị hệ thống).

### 7.2. Tài khoản Khách hàng di động (Mobile User)
- **URL API Đăng nhập 2 bước:**
  1. **Bước 1 - Gửi OTP:** `POST http://localhost:8080/api/mobile/auth/login/otp/send`
     ```json
     {
         "identifier": "0981712585",
         "password": "123456",
         "deviceId": "device-test-01"
     }
     ```
  2. **Bước 2 - Xác nhận OTP:** `POST http://localhost:8080/api/mobile/auth/login/verify`
     ```json
     {
         "identifier": "0981712585",
         "otpCode": "123456",
         "deviceId": "device-test-01"
     }
     ```
- **Tài khoản thanh toán mặc định:** `1000001`
- **Số dư khả dụng ban đầu:** `10.000.000 VNĐ`.

---

## 10. Xử Lý Các Lỗi Thường Gặp (Troubleshooting / FAQs)

### ❓ Lỗi 1: `The JAVA_HOME environment variable is not defined correctly`
- **Nguyên nhân:** Windows chưa nhận diện đường dẫn cài đặt Java JDK trong biến môi trường.
- **Cách khắc phục:** Trước khi gõ lệnh `mvnw`, hãy gõ lệnh gán biến môi trường trong PowerShell:
  ```powershell
  $env:JAVA_HOME = "C:\Program Files\Java\jdk-24"
  ```
  *(Thay thế `jdk-24` bằng thư mục JDK thực tế trên máy bạn nếu khác).*

---

### ❓ Lỗi 2: `File ... profile.ps1 cannot be loaded because running scripts is disabled`
- **Nguyên nhân:** Chính sách bảo mật PowerShell của Windows chặn chạy script chưa ký.
- **Cách khắc phục:** Lỗi này không ảnh hưởng đến lệnh `mvnw.cmd`. Nếu muốn tắt cảnh báo, mở PowerShell với quyền Administrator và gõ:
  ```powershell
  Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
  ```

---

### ❓ Lỗi 3: `Port 8080 is already in use`
- **Nguyên nhân:** Có một chương trình khác (hoặc một phiên chạy trước của backend) đang chiếm cổng 8080.
- **Cách khắc phục:**
  - Cách 1: Tắt tiến trình cũ:
    ```powershell
    Get-Process -Id (Get-NetTCPConnection -LocalPort 8080).OwningProcess | Stop-Process -Force
    ```
  - Cách 2: Đổi cổng sang 8081 trong tệp `.env`:
    ```properties
    server.port=8081
    ```

---

### ❓ Lỗi 4: Có cần chạy PostgreSQL khi chạy kiểm thử `mvn test` không?
- **Trả lời:** **KHÔNG CẦN!** Bộ kiểm thử đã được cấu hình tự động sử dụng **H2 In-Memory Database** trong bộ nhớ RAM (`application-test.properties`). Bạn có thể chạy kiểm thử hoàn toàn độc lập bất cứ lúc nào mà không cần cài đặt hoặc khởi động PostgreSQL.