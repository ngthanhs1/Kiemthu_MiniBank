# 📖 HƯỚNG DẪN CÁCH VẬN HÀNH, KHỞI ĐỘNG VÀ CHẠY KIỂM THỬ (TESTING USER GUIDE)
## HỆ THỐNG NGÂN HÀNG SỐ MINIBANK (FULL-STACK: CORE BACKEND & ADMIN PORTAL)

---
- **Tài liệu hướng dẫn:** Khởi động ứng dụng Full-Stack, Thiết lập cơ sở dữ liệu & Chạy toàn bộ các cấp độ kiểm thử
- **Hệ điều hành:** Windows (PowerShell / CMD) & Linux / macOS
- **Môi trường yêu cầu:** Java JDK 17 trở lên / Node.js 18+ / Python 3.10+ / Maven Wrapper / PostgreSQL (hoặc H2 RAM)
---

## 📑 MỤC LỤC
1. [Yêu Cầu Môi Trường (Prerequisites)](#1-yêu-cầu-môi-trường-prerequisites)
2. [Hướng Dẫn Thiết Lập Cơ Sở Dữ Liệu (Database Setup)](#2-hướng-dẫn-thiết-lập-cơ-sở-dữ-liệu-database-setup)
3. [Hướng Dẫn Khởi Động Hệ Thống Backend (Spring Boot Core)](#3-hướng-dẫn-khởi-động-hệ-thống-backend-spring-boot-core)
4. [Hướng Dẫn Từng Bước Khởi Động Giao Diện Frontend (Admin Portal)](#4-hướng-dẫn-từng-bước-khởi-động-giao-diện-frontend-admin-portal)
5. [Hướng Dẫn Chạy Toàn Bộ Kiểm Thử Tự Động (Maven Test 31 Ca Backend)](#5-hướng-dẫn-chạy-toàn-bộ-kiểm-thử-tự-động-maven-test-31-ca-backend)
6. [Hướng Dẫn Xem Báo Cáo Đo Độ Bao Phủ Mã Nguồn (JaCoCo Coverage 90.3%)](#6-hướng-dẫn-xem-báo-cáo-đo-độ-bao-phủ-mã-nguồn-jacoco-coverage-903)
7. [Hướng Dẫn Chạy Kiểm Thử Tự Động Hóa API (Postman / Newman)](#7-hướng-dẫn-chạy-kiểm-thử-tự-động-hóa-api-postman--newman)
8. [Hướng Dẫn Chạy Kiểm Thử Tải & Hiệu Năng (Apache JMeter)](#8-hướng-dẫn-chạy-kiểm-thử-tải--hiệu-năng-apache-jmeter)
9. [Hướng Dẫn Chạy Kiểm Thử Tự Động Giao Diện (Selenium & Selenium IDE)](#9-hướng-dẫn-chạy-kiểm-thử-tự-động-giao-diện-selenium--selenium-ide)
10. [Dữ Liệu Thử Nghiệm & Tài Khoản Mẫu (Test Data)](#10-dữ-liệu-thử-nghiệm--tài-khoản-mẫu-test-data)
11. [Xử Lý Các Lỗi Thường Gặp (Troubleshooting / FAQs)](#11-xử-lý-các-lỗi-thường-gặp-troubleshooting--faqs)

---

## 1. Yêu Cầu Môi Trường (Prerequisites)

Trước khi bắt đầu, hãy đảm bảo máy tính của bạn đã chuẩn bị các môi trường sau:

1. **Java Development Kit (JDK 17 trở lên):**
   - Hỗ trợ tốt nhất trên các phiên bản **Java 17 LTS, Java 21 LTS, Java 24**.
   - Kiểm tra bằng lệnh trong Terminal:
     ```bash
     java -version
     ```
   - Đảm bảo biến môi trường `JAVA_HOME` trỏ đúng vào thư mục cài đặt JDK (Ví dụ: `C:\Program Files\Java\jdk-17` hoặc `C:\Program Files\Java\jdk-21`).
2. **Maven Wrapper:** Có sẵn trong thư mục dự án (`.\mvnw.cmd` trên Windows, `./mvnw` trên Linux/macOS), **không cần cài thêm Maven bên ngoài**.
3. **Node.js (v18.17+ hoặc v20+):** Dành cho Frontend Next.js Admin Portal.
   - Kiểm tra bằng lệnh: `node -v` và `npm -v`.
4. **Python (3.10+) & Selenium:** Dành cho kiểm thử giao diện tự động.
   - Cài đặt thư viện: `pip install selenium`
5. **Trình duyệt Web:** Google Chrome hoặc Microsoft Edge (phiên bản mới nhất).
6. **Công cụ phụ trợ (Tùy chọn):**
   - **Postman Desktop** hoặc **Node.js/Newman** (chạy API test).
   - **Apache JMeter 5.6+** (chạy bài test tải hiệu năng).
   - **Selenium IDE Extension** trên Chrome Web Store (xem file `.side`).

---

## 2. Hướng Dẫn Thiết Lập Cơ Sở Dữ Liệu (Database Setup)

### 📌 Trả lời nhanh: Cơ sở dữ liệu tạo và kết nối ở đâu?
* **File cấu hình kết nối:** Nằm tại thư mục gốc của dự án: `d:\Code_Hoc\Kiemthu_MiniBank\.env` (Được sao chép từ tệp mẫu `.env.example`).
* **Cơ chế tự động tạo bảng (Flyway Migration):** Bạn **KHÔNG CẦN** phải tự tay tạo bảng, khóa ngoại hay cột dữ liệu! Dự án đã tích hợp công cụ **Flyway** (`src/main/resources/db/migration`). Khi Backend khởi chạy, Flyway sẽ tự động chạy 25 file SQL script (`V1__init.sql` -> `V25...`), tự sinh toàn bộ hơn 25 bảng và nạp dữ liệu mẫu ban đầu.
* **Chạy kiểm thử (Testing) có cần CSDL không?:** **KHÔNG CẦN CÀI ĐẶT GÌ CẢ!** Khi chạy lệnh `mvn test`, hệ thống tự động kích hoạt CSDL ảo **H2 In-Memory trong bộ nhớ RAM** (`application-test.properties`).

---

### Bước 2.1: Tạo cơ sở dữ liệu PostgreSQL (Chọn 1 trong 2 cách dưới đây)

#### 👉 CÁCH A: Dùng PostgreSQL cài trên máy tính của bạn (Localhost)
1. Mở công cụ **pgAdmin 4** (hoặc mở Terminal CMD gõ `psql -U postgres`).
2. Chạy đúng 1 câu lệnh SQL duy nhất để tạo database:
   ```sql
   CREATE DATABASE minibank_db;
   ```
3. Mở tệp `.env` tại thư mục gốc `d:\Code_Hoc\Kiemthu_MiniBank\.env` (nếu chưa có thì copy từ `.env.example`), điền thông tin kết nối:
   ```properties
   SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/minibank_db
   SPRING_DATASOURCE_USERNAME=postgres
   SPRING_DATASOURCE_PASSWORD=mat_khau_postgres_cua_ban
   ```

#### 👉 CÁCH B: Dùng Cloud Database miễn phí (Neon PostgreSQL - Khuyến nghị không cần cài gì trên máy)
1. Truy cập trang web miễn phí [https://neon.tech](https://neon.tech) và đăng nhập bằng tài khoản Google/GitHub.
2. Bấm nút **"Create Project"**, đặt tên project là `minibank`.
3. Sao chép chuỗi kết nối PostgreSQL (Connection String dạng `postgresql://...`) do Neon cung cấp.
4. Dán trực tiếp vào tệp `.env`:
   ```properties
   SPRING_DATASOURCE_URL=postgresql://neondb_owner:npg_xxxx@ep-xyz.ap-southeast-1.aws.neon.tech/neondb?sslmode=require
   ```

---

### Bước 2.2: Kiểm tra toàn bộ tệp `.env` hoàn chỉnh

Tệp `d:\Code_Hoc\Kiemthu_MiniBank\.env` chuẩn có dạng như sau:

```properties
# Thông tin kết nối PostgreSQL (Local hoặc Cloud)
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/minibank_db
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres

# Cơ chế tự động tạo bảng Flyway
FLYWAY_BASELINE_ON_MIGRATE=false
FLYWAY_BASELINE_VERSION=1
FLYWAY_VALIDATE_ON_MIGRATE=false

# Cổng dịch vụ Backend
server.port=8080

# Cấu hình bảo mật JWT Token (Tối thiểu 32 ký tự)
JWT_SECRET=super-secret-key-minibank-development-2026-xyz-32chars
JWT_ISSUER=minibank
JWT_ACCESS_TOKEN_TTL_SECONDS=86400

# Chế độ phát triển (Sử dụng mã OTP mặc định 123456 không cần SMS thực)
SMS_DEV_MODE=true
```

---

## 3. Hướng Dẫn Khởi Động Hệ Thống Backend (Spring Boot Core)

Mở một cửa sổ **PowerShell** hoặc **Terminal CMD** thứ nhất tại thư mục dự án `d:\Code_Hoc\Kiemthu_MiniBank`:

```powershell
# 1. Di chuyển vào thư mục dự án
cd d:\Code_Hoc\Kiemthu_MiniBank

# 2. Thiết lập JAVA_HOME (nếu máy chưa cài biến môi trường toàn cục)
# Có thể trỏ tới bất kỳ JDK 17, 21 hoặc 24 nào bạn đã cài
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"

# 3. Khởi động máy chủ Spring Boot
.\mvnw.cmd spring-boot:run
```

Khi Terminal xuất hiện dòng chữ:
```text
Started BankingBackendApplication in X.XXX seconds (process running for X.XXX)
```
👉 **Backend đã hoạt động thành công tại địa chỉ:** **`http://localhost:8080`**.
*(Giữ nguyên cửa sổ Terminal này để Backend tiếp tục chạy)*.

---

## 4. Hướng Dẫn Từng Bước Khởi Động Giao Diện Frontend (Admin Portal)

Cổng quản trị MiniBank Admin Portal được viết bằng **Next.js 16, React 19 và Tailwind CSS 4**. Cả hai đường dẫn thư mục `frontend/` và `fontend/` đều trỏ vào cùng một thư mục vật lý.

Mở một cửa sổ **PowerShell** hoặc **Terminal CMD** thứ hai (chạy song song với Backend):

### Bước 4.1: Di chuyển vào thư mục giao diện
```powershell
cd d:\Code_Hoc\Kiemthu_MiniBankrontend
```

### Bước 4.2: Cài đặt các gói thư viện (Chỉ cần chạy 1 lần duy nhất)
```powershell
npm install
```
*(Lệnh này sẽ tải các module cần thiết của Next.js và Tailwind CSS. Nếu bạn đã chạy rồi thì có thể bỏ qua bước này).*

### Bước 4.3: Kiểm tra tệp cấu hình kết nối Backend (`.env.local`)
Tệp `.env.local` đã được thiết lập sẵn trong thư mục `frontend/` trỏ về Backend cổng 8080:
```bash
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
```

### Bước 4.4: Khởi chạy máy chủ giao diện phát triển (Dev Server)
```powershell
npm run dev
```

Khi màn hình hiển thị thông báo sẵn sàng:
```text
  ▲ Next.js 16.x.x
  - Local:        http://localhost:3000
  - Environments: .env.local
 ✓ Starting...
 ✓ Ready in 1.5s
```

### Bước 4.5: Mở trình duyệt và sử dụng hệ thống
1. Mở trình duyệt (Chrome / Edge / Firefox) và truy cập: **`http://localhost:3000/login`**
2. Nhập thông tin tài khoản Quản trị viên mặc định:
   * **Tên đăng nhập / Identifier:** `admin`
   * **Mật khẩu / Password:** `admin123`
3. Bấm **"Đăng nhập hệ thống"**: Hệ thống sẽ cấp phát JWT Token và chuyển hướng sang **Bảng điều khiển quản trị (Dashboard)** tại `http://localhost:3000/`.
4. Các trang chức năng chính để bấm thử nghiệm:
   * **Phê duyệt khoản vay:** `http://localhost:3000/requests/loan-approval`
   * **Phê duyệt sổ tiết kiệm:** `http://localhost:3000/requests/saving-approval`
   * **Quản lý danh sách khách hàng:** `http://localhost:3000/customers`
   * **Nhật ký kiểm toán & Hệ thống:** `http://localhost:3000/system/audit`

---

## 5. Hướng Dẫn Chạy Toàn Bộ Kiểm Thử Tự Động (Maven Test 31 Ca Backend)

Bộ kiểm thử tự động tầng Backend gồm **31 ca kiểm thử** hoàn chỉnh (Unit Test & Integration Test), chạy độc lập trên **H2 In-Memory Database** trong RAM nên **không cần khởi động PostgreSQL**:

### 5.1. Chạy tất cả 31 ca kiểm thử cùng lúc:
```powershell
cd d:\Code_Hoc\Kiemthu_MiniBank
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
.\mvnw.cmd test
```

*Kết quả mong đợi:*
```text
[INFO] Results:
[INFO] 
[INFO] Tests run: 31, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

### 5.2. Chạy riêng từng bài kiểm thử cụ thể:
- **Kiểm thử White-box Chuyển tiền (Branch Testing):**
  ```powershell
  .\mvnw.cmd test -Dtest=TransferServiceTest
  ```
- **Kiểm thử White-box Khoản vay tín dụng:**
  ```powershell
  .\mvnw.cmd test -Dtest=LoanServiceTest
  ```
- **Kiểm thử Đồng thời chống Double-Spending (Concurrency Race Condition):**
  ```powershell
  .\mvnw.cmd test -Dtest=ConcurrencyRaceConditionTest
  ```
- **Kiểm thử Tích hợp API Controller:**
  ```powershell
  .\mvnw.cmd test -Dtest=AuthControllerIntegrationTest,TransferControllerIntegrationTest
  ```

---

## 6. Hướng Dẫn Xem Báo Cáo Đo Độ Bao Phủ Mã Nguồn (JaCoCo Coverage 90.3%)

Sau khi chạy xong lệnh `.\mvnw.cmd test`, công cụ JaCoCo sẽ tự động tính toán độ bao phủ mã nguồn và xuất ra báo cáo HTML trực quan.

### Cách mở báo cáo trên trình duyệt:
- **Mở bằng lệnh PowerShell:**
  ```powershell
  Start-Process "d:\Code_Hoc\Kiemthu_MiniBank	arget\site\jacoco\index.html"
  ```
- **Hoặc mở thủ công:** Dùng Windows Explorer mở tệp:
  `d:\Code_Hoc\Kiemthu_MiniBank	arget\site\jacoco\index.html`

*Chỉ số đạt được:*
* **Line Coverage (Độ bao phủ dòng lệnh):** **90.3%**
* **Branch Coverage (Độ bao phủ nhánh điều kiện):** **85.6%**

---

## 7. Hướng Dẫn Chạy Kiểm Thử Tự Động Hóa API (Postman / Newman)

Tệp kịch bản Postman Collection đã được tạo sẵn tại:
`test_automation/postman/MiniBank_API_Automation.postman_collection.json`

### Cách 1: Chạy bằng giao diện Postman Desktop
1. Mở Postman Desktop.
2. Bấm nút **Import** -> Chọn tệp `MiniBank_API_Automation.postman_collection.json`.
3. Nhấp chuột phải vào thư mục Collection -> Chọn **"Run collection"**.
4. Bấm **"Run MiniBank API Automation"** để quan sát 11/11 bài test API chạy thành công.

### Cách 2: Chạy tự động bằng dòng lệnh với Newman (Xuất báo cáo HTML)
```powershell
# 1. Cài đặt Newman (chỉ cần chạy 1 lần nếu chưa cài)
npm install -g newman newman-reporter-htmlextra

# 2. Chạy tự động và xuất báo cáo
newman run test_automation/postman/MiniBank_API_Automation.postman_collection.json -r cli,htmlextra --reporter-htmlextra-export test_automation/postman/api_test_report.html

# 3. Mở xem báo cáo HTML
Start-Process "test_automation\postmanpi_test_report.html"
```

---

## 8. Hướng Dẫn Chạy Kiểm Thử Tải & Hiệu Năng (Apache JMeter)

Kịch bản kiểm thử tải đã được cấu hình tại:
`test_automation/jmeter/MiniBank_Performance_TestPlan.jmx`

### Cách 1: Chạy bằng giao diện đồ họa JMeter GUI
1. Mở Apache JMeter (`jmeter.bat`).
2. Vào **File** -> **Open** -> Chọn tệp `MiniBank_Performance_TestPlan.jmx`.
3. Bấm biểu tượng nút **Start (Play xanh lá cây)**.
4. Xem kết quả tại các bảng: **Summary Report**, **Aggregate Report**, và **View Results Tree**.

### Cách 2: Chạy bằng dòng lệnh Non-GUI (Chuẩn công nghiệp)
```powershell
# Chạy bắn tải mô phỏng 50 - 100 người dùng ảo đồng thời và xuất báo cáo Dashboard
jmeter -n -t test_automation\jmeter\MiniBank_Performance_TestPlan.jmx -l test_automation\jmeteresults.jtl -e -o test_automation\jmeter\dashboard_report

# Mở xem báo cáo đồ thị
Start-Process "test_automation\jmeter\dashboard_report\index.html"
```
*Kết quả:* Thời gian phản hồi trung bình **54.7 ms**, tỷ lệ lỗi giao dịch **0.00%**.

---

## 9. Hướng Dẫn Chạy Kiểm Thử Tự Động Giao Diện (Selenium & Selenium IDE)

### 9.1. Chạy 10 ca kiểm thử tự động với Selenium WebDriver (Python)

Toàn bộ 10 ca kiểm thử UI (`UI_TC_01` đến `UI_TC_10`) đã được đóng gói trong tệp:
`test_automation/selenium/test_admin_portal_ui.py`.

1. **Cài đặt thư viện Selenium (nếu chưa cài):**
   ```powershell
   pip install selenium
   ```
2. **Đảm bảo Frontend đang chạy tại cổng 3000** (xem Mục 4).
3. **Chạy bộ kiểm thử tự động:**
   ```powershell
   cd d:\Code_Hoc\Kiemthu_MiniBank
   python test_automation/selenium/test_admin_portal_ui.py
   ```
   *Kết quả thực thi:*
   ```text
   Ran 10 tests in 8.421s

   OK
   ```

---

### 9.2. Chạy kịch bản trực quan với Selenium IDE

Tệp dự án Selenium IDE: `test_automation/selenium/MiniBank_Admin_UI_Test.side`.

1. Mở trình duyệt Google Chrome hoặc Edge có tiện ích **Selenium IDE**.
2. Nhấp biểu tượng Selenium IDE trên thanh tiện ích mở rộng.
3. Chọn **"Open an existing project"** -> Duyệt mở tệp:
   `d:\Code_Hoc\Kiemthu_MiniBank	est_automation\selenium\MiniBank_Admin_UI_Test.side`
4. Chọn kịch bản kiểm thử trong bảng danh sách bên trái (như `TC01_Login_Admin_Success` hoặc `TC02_Navigate_Loan_Approval`).
5. Bấm nút **"Run current test"** để quan sát trình duyệt tự động điền form, click chuyển trang và xác thực dữ liệu.

---

## 10. Dữ Liệu Thử Nghiệm & Tài Khoản Mẫu (Test Data)

### 10.1. Tài khoản Quản trị viên (Admin Portal)
- **Địa chỉ đăng nhập:** `http://localhost:3000/login`
- **Tên đăng nhập (Username / Identifier):** `admin`
- **Mật khẩu (Password):** `admin123`
- **Vai trò:** `ROLE_ADMIN` (Quyền cao nhất: xem Dashboard, duyệt khoản vay, duyệt tiết kiệm, xem nhật ký kiểm toán hệ thống).

### 10.2. Tài khoản Khách hàng di động (Mobile User)
- **Số điện thoại / Identifier:** `0981712585`
- **Mật khẩu:** `123456`
- **Mã xác thực OTP:** `123456` (Trong chế độ dev `SMS_DEV_MODE=true`)
- **Số tài khoản thanh toán:** `1000001`
- **Số dư khả dụng ban đầu:** `10.000.000 VNĐ`

---

## 11. Xử Lý Các Lỗi Thường Gặp (Troubleshooting / FAQs)

### ❓ Lỗi 1: `The JAVA_HOME environment variable is not defined correctly`
- **Nguyên nhân:** Windows chưa nhận diện đường dẫn cài đặt Java JDK.
- **Cách khắc phục:** Trước khi gõ lệnh `mvnw`, hãy gõ lệnh gán biến môi trường trong PowerShell:
  ```powershell
  $env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
  ```
  *(Thay thế `jdk-17` bằng thư mục JDK thực tế trên máy bạn: `jdk-21`, `jdk-24`...).*

---

### ❓ Lỗi 2: `File ... profile.ps1 cannot be loaded because running scripts is disabled`
- **Nguyên nhân:** Chính sách bảo mật PowerShell của Windows chặn chạy script chưa ký.
- **Cách khắc phục:** Lỗi này không ảnh hưởng đến lệnh `mvnw.cmd`. Nếu muốn tắt cảnh báo, mở PowerShell với quyền Administrator và gõ:
  ```powershell
  Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
  ```

---

### ❓ Lỗi 3: `Port 8080 is already in use` hoặc `Port 3000 is already in use`
- **Nguyên nhân:** Có một chương trình khác hoặc phiên chạy cũ đang chiếm cổng.
- **Cách khắc phục:**
  - Tắt tiến trình cổng 8080:
    ```powershell
    Get-Process -Id (Get-NetTCPConnection -LocalPort 8080).OwningProcess | Stop-Process -Force
    ```
  - Tắt tiến trình cổng 3000:
    ```powershell
    Get-Process -Id (Get-NetTCPConnection -LocalPort 3000).OwningProcess | Stop-Process -Force
    ```

---

### ❓ Lỗi 4: Có bắt buộc phải cài PostgreSQL khi chỉ muốn chạy test `mvn test` không?
- **Trả lời:** **KHÔNG CẦN!** Toàn bộ 31 bài test tự động được cấu hình chạy trên CSDL ảo **H2 In-Memory trong bộ nhớ RAM** (`src/test/resources/application-test.properties`). Bạn có thể chạy kiểm thử hoàn toàn độc lập bất cứ lúc nào mà không cần cài đặt hoặc mở PostgreSQL.
