# 🏦 MiniBank - Hệ Thống Ngân Hàng Số & Hồ Sơ Kiểm Định Chất Lượng Toàn Diện

> **ĐỀ TÀI:** KIỂM ĐỊNH VÀ ĐÁNH GIÁ CHẤT LƯỢNG HỆ THỐNG NGÂN HÀNG SỐ TRỰC TUYẾN MINIBANK  
> **Giảng viên hướng dẫn:** TS. Trịnh Thanh Bình
> **Thành viên:** Nguyễn Văn Thành - Hà Xuân Đại 
> **Học phần:** Kiểm định và Đánh giá chất lượng phần mềm  
> **Nhóm thực hiện:** Nhóm 6: Nguyễn Văn Thành (MSV: 23010191) & Hà Xuân Đại (MSV: 23010494)  
> **Phiên bản:** 2.0.0 (Chuẩn hóa Java 17+, Next.js 16, 57 Ca kiểm thử tự động đạt 100% Pass Rate)

---

## 📑 Mục Lục
1. [Giới Thiệu Đề Tài & Tổng Quan Hệ Thống](#1-giới-thiệu-đề-tài--tổng-quan-hệ-thống)
2. [Kiến Trúc Kỹ Thuật (Tech Stack & Architecture)](#2-kiến-trúc-kỹ-thuật-tech-stack--architecture)
3. [Tổng Hợp Kết Quả Kiểm Định Chất Lượng](#3-tổng-hợp-kết-quả-kiểm-định-chất-lượng)
4. [Tổ Chức Thư Mục Dự Án](#4-tổ-chức-thư-mục-dự-án)
5. [Hướng Dẫn Cài Đặt & Khởi Chạy Hệ Thống](#5-hướng-dẫn-cài-đặt--khởi-chạy-hệ-thống)
6. [Hướng Dẫn Thực Thi Toàn Bộ 57 Ca Kiểm Thử Tự Động](#6-hướng-dẫn-thực-thi-toàn-bộ-57-ca-kiểm-thử-tự-động)
7. [Dữ Liệu Thử Nghiệm & Tài Khoản Mẫu (Test Accounts)](#7-dữ-liệu-thử-nghiệm--tài-khoản-mẫu-test-accounts)
8. [Danh Mục Hồ Sơ & Sản Phẩm Đề Tài Đã Bàn Giao](#8-danh-mục-hồ-sơ--sản-phẩm-đề-tài-đã-bàn-giao)

---

## 1. Giới Thiệu Đề Tài & Tổng Quan Hệ Thống

**MiniBank** là hệ thống giải pháp ngân hàng số đa phân hệ (Digital Banking Solution) tích hợp chặt chẽ giữa dịch vụ tài chính cốt lõi (Core Banking API) và Cổng quản trị phân quyền (Admin Portal). 

Đề tài tập trung nghiên cứu, thiết lập và triển khai **quy trình kiểm định chất lượng phần mềm chuyên sâu** theo tiêu chuẩn quốc tế ISO/IEC 25010 và giáo trình chuẩn mực của Khoa CNTT - Đại học Phenikaa:
* **Bảo vệ an toàn số dư tài khoản:** Kiểm thử tranh chấp tài nguyên đồng thời (Concurrency Testing), ngăn chặn triệt để lỗ hổng gian lận rút tiền trùng lặp (Double-Spending) bằng kỹ thuật khóa bi quan cấp CSDL (`SELECT ... FOR UPDATE`).
* **Đo lường độ bao phủ mã nguồn (Code Coverage):** Đạt mức **90.3%** Line Coverage với công cụ JaCoCo.
* **Tự động hóa toàn diện:** 57 ca kiểm thử tự động hóa 100% bao quát từ tầng dịch vụ Service, MockMvc Controller, Postman/Newman API cho đến 16 trang giao diện Selenium WebDriver.

---

## 2. Kiến Trúc Kỹ Thuật (Tech Stack & Architecture)

Hệ thống được thiết kế theo kiến trúc phân tán Client-Server hiện đại:

| Thành Phần Hệ Thống | Công Nghệ / Nền Tảng | Mô Tả & Vai Trò Kỹ Thuật |
| :--- | :--- | :--- |
| **Backend Core** | **Java 17 (LTS)** & **Spring Boot 3.5.x** | Xây dựng RESTful API nghiệp vụ, Spring Security 6, JWT, BCrypt, RSA Signing. |
| **Frontend Portal**| **Next.js 16 (App Router)** & **React 19** | Cổng quản trị Admin Portal, Tailwind CSS 4, quản lý phiên làm việc JWT trong `localStorage`. |
| **Cơ Sở Dữ Liệu** | **PostgreSQL 17** & **H2 In-Memory** | PostgreSQL 17 cho môi trường vận hành thật; H2 Database trong RAM cho môi trường Test siêu tốc. |
| **DB Migration** | **Flyway Migration (V1 - V25)** | Tự động sinh bảng, cấu trúc khóa ngoại và nạp dữ liệu ban đầu (Seed data). |
| **Kiểm Thử Hộp Trắng**| **JUnit 5**, **Mockito**, **JaCoCo** | Phân tích đồ thị luồng điều khiển CFG, độ phức tạp Cyclomatic $V(G)=4$, Branch Testing. |
| **Kiểm Thử Hiệu Năng** | **Apache JMeter 5.6** | Mô phỏng 50 - 100 người dùng ảo đồng thời, bắn tải 20.000 mẫu giao dịch. |
| **Tự Động Hóa API** | **Postman** & **Newman CLI** | Bộ sưu tập 10 API, 17 assertions, chạy tự động tích hợp CI/CD không cần cài đặt. |
| **Tự Động Hóa UI** | **Selenium WebDriver (Python)** & **IDE** | Tự động hóa kiểm thử trọn vẹn 16 trang phân hệ Admin Portal, Record & Playback kịch bản `.side`. |

---

## 3. Tổng Hợp Kết Quả Kiểm Định Chất Lượng

Toàn bộ các chỉ tiêu chất lượng đề ra đều đã hoàn thành và vượt mức kỳ vọng:

* **Tỷ lệ kiểm thử tự động hóa thành công:** **57/57 ca Passed tuyệt đối (100%)**:
  * *Kiểm thử đơn vị & Tích hợp Backend (JUnit 5 / MockMvc):* **31/31 ca Passed**.
  * *Kiểm thử tự động hóa API (Postman Collection / Newman CLI):* **10/10 Requests Passed (17/17 Assertions Passed)**.
  * *Kiểm thử tự động hóa giao diện Frontend (Selenium WebDriver):* **16/16 ca Passed**.
* **Độ bao phủ mã nguồn (Code Coverage - JaCoCo):**
  * **Line Coverage (Độ bao phủ dòng lệnh):** **90.3%** (Chỉ tiêu tối thiểu: 85%).
  * **Branch Coverage (Độ bao phủ nhánh rẽ):** **85.6%**.
* **Năng lực chịu tải & Hiệu năng (Apache JMeter):**
  * Thời gian phản hồi trung bình toàn hệ thống: **54.7 ms** dưới tải 100 người dùng đồng thời.
  * Tỷ lệ lỗi giao dịch (Error Rate): **0.00%**.
  * Thông lượng hệ thống (Throughput): **290.25 requests/giây**.
* **Khắc phục lỗi phần mềm (Defect Resolution):** Phát hiện và đóng dứt điểm **6/6 lỗi logic và bảo mật** (BUG-01 đến BUG-06, trạng thái: CLOSED 100%).

---

## 4. Tổ Chức Thư Mục Dự Án

```text
d:\Code_Hoc\Kiemthu_MiniBank\
├── BAO_CAO_KIEM_DINH_CHAT_LUONG_PHAN_MEM_MINIBANK.docx  # Báo cáo Word 2.02MB hoàn thiện (21 ảnh HD)
├── BAO_CAO_KIEM_DINH_CHAT_LUONG_PHAN_MEM_MINIBANK.md    # Báo cáo Markdown chuẩn học thuật Phenikaa
├── SLIDE_THUYET_TRINH_MINIBANK.pptx                    # Slide thuyết trình PowerPoint 16:9 bảo vệ đề tài
├── README.md                                           # Tài liệu tổng quan đề tài hiện tại
├── pom.xml                                             # Cấu hình Maven (Java 17 baseline, JaCoCo 0.8.12)
│
├── docs/                                               # Hồ sơ tài liệu kỹ thuật
│   ├── figures/                                        # Toàn bộ 21 ảnh minh chứng thực tế sắc nét
│   ├── final_report/                                   # Bản lưu trữ báo cáo chuẩn bị in ấn nộp khoa
│   ├── presentation/                                   # Giáo trình slide, kịch bản thuyết trình & file PPTX
│   │   ├── GIAO_TRINH_SLIDE_VA_KICH_BAN_THUYET_TRINH.md # Lời thoại từng slide, hướng dẫn live demo
│   │   └── SLIDE_THUYET_TRINH_MINIBANK.pptx            # Tệp trình chiếu PowerPoint
│   └── step_docs/                                      # Tài liệu hướng dẫn từng bước chi tiết
│       └── HUONG_DAN_CHAY_VA_KIEM_THU.md               # Sổ tay hướng dẫn chạy và kiểm thử từ A-Z
│
├── frontend/                                           # Mã nguồn Cổng quản trị Admin Portal (Next.js 16)
│   ├── src/app/                                        # 16 trang phân hệ quản trị (login, dashboard, loan,...)
│   └── package.json                                    # Next.js 16, React 19, Tailwind CSS 4
│
├── src/                                                # Mã nguồn Ngân hàng số Backend (Spring Boot 3)
│   ├── main/java/com/minibank/backend/                 # Các tầng Controller, Service, Repository, Entity
│   └── test/java/com/minibank/backend/                 # 31 ca kiểm thử tự động Backend (H2 Database)
│
├── test_automation/                                    # Bộ kịch bản kiểm thử tự động hóa ngoài
│   ├── postman/                                        # Kịch bản kiểm thử API Postman Collection
│   │   └── MiniBank_API_Automation.postman_collection.json # 10 API, 17 assertions tự động
│   ├── selenium/                                       # Kịch bản kiểm thử giao diện tự động
│   │   ├── test_admin_portal_all_pages.py              # Suite 16 trang phân hệ Admin Portal toàn diện
│   │   ├── test_admin_portal_ui.py                     # Suite 10 ca kiểm thử giao diện cơ bản
│   │   └── MiniBank_Admin_UI_Test.side                 # Dự án Selenium IDE Record & Playback
│   └── jmeter/                                         # Kịch bản kiểm thử tải & hiệu năng JMeter
│       └── MiniBank_Performance_TestPlan.jmx           # Thread Group bắn tải 20.000 mẫu giao dịch
│
└── scripts/                                            # Các kịch bản tiện ích tự động
    ├── convert_to_docx.py                              # Trình biên dịch báo cáo sang Word (.docx) chuẩn đẹp
    ├── generate_powerpoint_presentation.py             # Trình sinh slide thuyết trình PowerPoint (.pptx)
    └── capture_all_pages_screenshots.py                # Chụp ảnh tự động toàn bộ 16 trang giao diện
```

---

## 5. Hướng Dẫn Cài Đặt & Khởi Chạy Hệ Thống

### Bước 1: Yêu cầu môi trường
* **Java Development Kit (JDK):** Java 17 trở lên (Đã cấu hình `<java.version>17</java.version>`).
* **Node.js:** Phiên bản 18+ hoặc 20+ (Bao gồm `npm` và `npx`).
* **Python:** Phiên bản 3.10+ (Đã cài thư viện `selenium`, `python-docx`, `python-pptx`, `Pillow`).
* **Cơ sở dữ liệu:** PostgreSQL 17 (Tạo database tên `minibank_db`).

### Bước 2: Cấu hình cơ sở dữ liệu
Chỉnh sửa tệp `.env` tại thư mục gốc dự án:
```env
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/minibank_db
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=mật_khẩu_postgresql_của_bạn
```

### Bước 3: Khởi chạy Backend (Spring Boot Core API)
```powershell
cd d:\Code_Hoc\Kiemthu_MiniBank
.\mvnw.cmd spring-boot:run
```
* Backend sẽ tự động chạy Flyway migration từ `V1` đến `V25` và nạp dữ liệu ban đầu (Seed data).
* Địa chỉ máy chủ: **`http://localhost:8080`**.

### Bước 4: Khởi chạy Frontend (Admin Portal)
Mở một cửa sổ Terminal mới:
```powershell
cd d:\Code_Hoc\Kiemthu_MiniBank\frontend
npm run dev
# Hoặc nếu bị chặn bởi PowerShell Execution Policy:
cmd /c "npm.cmd run dev"
```
* Địa chỉ cổng quản trị: **`http://localhost:3000`**.

---

## 6. Hướng Dẫn Thực Thi Toàn Bộ 57 Ca Kiểm Thử Tự Động

### 6.1. Chạy 31 ca kiểm thử Backend & Đo độ bao phủ JaCoCo (JUnit 5 / MockMvc)
Bộ test này chạy trên **H2 In-Memory Database** trong RAM nên **không cần khởi động PostgreSQL**:
```powershell
cd d:\Code_Hoc\Kiemthu_MiniBank
.\mvnw.cmd test
```
* **Kết quả:** `Tests run: 31, Failures: 0, Errors: 0, Skipped: 0` ➔ `BUILD SUCCESS`.
* **Xem báo cáo JaCoCo HTML (Line Coverage 90.3%):** Mở tệp:
  `d:\Code_Hoc\Kiemthu_MiniBank\target\site\jacoco\index.html`

### 6.2. Chạy 10 ca kiểm thử tự động API bằng Newman CLI (Postman Collection)
Chỉ cần chạy đúng 1 lệnh (sử dụng `npx` của Node.js, không cần cài đặt gói toàn cục):
```powershell
cmd /c "npx --yes newman run test_automation/postman/MiniBank_API_Automation.postman_collection.json --env-var base_url=http://localhost:8080"
```
* **Kết quả:** 10/10 requests thành công, 17/17 assertions Passed, độ trễ trung bình 38 ms.
* **Hoặc mở trực quan:** Khởi động Postman Desktop ➔ Import tệp `test_automation/postman/MiniBank_API_Automation.postman_collection.json` ➔ Bấm **Run collection**.

### 6.3. Chạy 16 ca kiểm thử tự động giao diện toàn diện với Selenium WebDriver
```powershell
python test_automation/selenium/test_admin_portal_all_pages.py -v
```
* **Kết quả:** `Ran 16 tests in 72.952s - OK` (Tỷ lệ đạt tuyệt đối 100%).
* **Hoặc mở trực quan bằng Selenium IDE:** Mở Chrome cài tiện ích Selenium IDE ➔ Mở tệp `test_automation/selenium/MiniBank_Admin_UI_Test.side` ➔ Bấm nút **Play (▶️)** để quan sát trình duyệt tự động điền form và click chuyển trang.

### 6.4. Chạy kiểm thử tải và hiệu năng với Apache JMeter
```powershell
# Chạy Non-GUI và xuất báo cáo Dashboard đồ thị HTML:
jmeter -n -t test_automation\jmeter\MiniBank_Performance_TestPlan.jmx -l test_automation\jmeter\results.jtl -e -o test_automation\jmeter\dashboard_report
```
* **Kết quả:** 20.000 mẫu giao dịch, thời gian phản hồi trung bình **54.7 ms**, tỷ lệ lỗi **0.00%**.

---

## 7. Dữ Liệu Thử Nghiệm & Tài Khoản Mẫu (Test Accounts)

### 7.1. Tài khoản Quản trị viên (Admin Portal)
* **URL đăng nhập:** `http://localhost:3000/login`
* **Tên đăng nhập / Identifier:** `admin` (hoặc `admin@gmail.com`)
* **Mật khẩu (Password):** `admin123` (hoặc `123456`)
* **Vai trò:** `ROLE_ADMIN` (Toàn quyền quản trị Dashboard, duyệt vay, duyệt tiết kiệm, xem audit log).

### 7.2. Tài khoản Khách hàng di động (Mobile User)
* **Số điện thoại định danh:** `0981712585`
* **Mật khẩu:** `123456`
* **Mã xác thực OTP:** `123456` (Chế độ `SMS_DEV_MODE=true`)
* **Số tài khoản thanh toán:** `1000001`
* **Số dư khả dụng ban đầu:** `10.000.000 VNĐ`
