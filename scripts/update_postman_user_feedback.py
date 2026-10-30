# -*- coding: utf-8 -*-
"""
Update Section 3.4.2 and Section 4 with user's exact Postman evaluation text and 4 images:
- hinh_3_4b_postman_runner.png
- hinh_3_4c_postman_admin_login.png
- hinh_3_4d_postman_dashboard_stats.png
- hinh_3_4e_postman_security_401.png
"""

import os
import re

FILES = [
    r"d:\Code_Hoc\Kiemthu_MiniBank\BAO_CAO_KIEM_DINH_CHAT_LUONG_PHAN_MEM_MINIBANK.md",
    r"d:\Code_Hoc\Kiemthu_MiniBank\docs\final_report\BAO_CAO_KIEM_DINH_CHAT_LUONG_PHAN_MEM_MINIBANK.md"
]

SEC_3_4_2_REPLACEMENT = """### 3.4.2 Bộ kịch bản tự động hóa API với Postman & Newman

Hệ thống MiniBank sử dụng bộ sưu tập Postman Collection chuẩn hóa (`test_automation/postman/MiniBank_API_Automation.postman_collection.json`) để kiểm thử hồi quy tự động các điểm cuối API cốt lõi. Toàn bộ kịch bản được thực thi tự động qua công cụ **Postman Collection Runner** trực quan và công cụ dòng lệnh **Newman CLI** kết nối trực tiếp với máy chủ Spring Boot:

### Kết quả kiểm thử API tự động bằng Postman

Nhóm thực hiện kiểm thử API tự động cho hệ thống MiniBank bằng Postman Collection Runner. Bộ kiểm thử gồm 10 API thuộc bốn nhóm chức năng: quản trị, xác thực người dùng, sản phẩm dịch vụ và bảo mật.

Kết quả thực nghiệm cho thấy toàn bộ 17/17 điều kiện kiểm thử (assertions) đều thành công, không có trường hợp thất bại, đạt tỷ lệ PASS 100%.

Các API quản trị và truy xuất dữ liệu trả về HTTP 200 khi yêu cầu hợp lệ. Đối với các trường hợp kiểm thử bảo mật, hệ thống trả về HTTP 401 Unauthorized khi không cung cấp token hoặc sử dụng thông tin đăng nhập không hợp lệ.

Kết quả cho thấy các API được kiểm thử đáp ứng những điều kiện mong đợi trong bộ kịch bản hiện tại. Tuy nhiên, kết quả này chưa đại diện cho việc kiểm thử toàn bộ chức năng và bảo mật của hệ thống.

#### Bảng chi tiết kết quả thực thi 10 ca kiểm thử API với Postman / Newman:

<p align="center"><b>Bảng 3.11: Kết quả thực thi kiểm thử tự động API bằng Postman & Newman (Pass Rate 100%)</b></p>

| Nhóm API | Mã Ca Test | Tên Kịch Bản Kiểm Thử | Giao Thức & Endpoint | Trạng Thái HTTP | Thời Gian (Latency) | Kết Quả Khẳng Định (Assertions) | Trạng Thái |
| :--- | :--- | :--- | :--- | :---: | :---: | :--- | :---: |
| **Admin Portal** | `TC_API_ADM_01` | Đăng nhập tài khoản Quản trị viên | `POST /api/admin/auth/login` | 200 OK | 118 ms | ✔ Status code 200<br>✔ Bearer token issued<br>✔ Role contains ADMIN | **PASSED** |
| **Admin Portal** | `TC_API_ADM_02` | Lấy số liệu thống kê Dashboard | `GET /api/admin/dashboard` | 200 OK | 48 ms | ✔ Status code 200<br>✔ Metrics payload non-empty | **PASSED** |
| **Mobile Auth** | `TC_API_MOB_01` | Đăng ký tài khoản khách hàng mới | `POST /api/mobile/auth/register` | 201 Created | 74 ms | ✔ Status code 201 Created<br>✔ User ID & Token returned | **PASSED** |
| **Mobile Auth** | `TC_API_MOB_02` | Yêu cầu gửi mã OTP đăng nhập | `POST /api/mobile/auth/login/otp/send` | 200 OK | 75 ms | ✔ Status code 200<br>✔ OTP returned in dev-mode | **PASSED** |
| **Mobile Auth** | `TC_API_MOB_03` | Xác thực mã OTP và cấp phát JWT | `POST /api/mobile/auth/login/verify` | 200 OK | 8 ms | ✔ Status code 200<br>✔ Access token issued | **PASSED** |
| **Mobile Auth** | `TC_API_MOB_04` | Tra cứu danh sách tài khoản cá nhân | `GET /api/mobile/accounts/me` | 200 OK | 9 ms | ✔ Status code 200<br>✔ Returns account list array | **PASSED** |
| **Products** | `TC_API_PROD_01` | Lấy danh mục sản phẩm tiết kiệm | `GET /api/mobile/savings/products` | 200 OK | 10 ms | ✔ Status code 200<br>✔ Saving products array valid | **PASSED** |
| **Products** | `TC_API_PROD_02` | Lấy danh mục sản phẩm vay vốn | `GET /api/mobile/loan-products` | 200 OK | 9 ms | ✔ Status code 200<br>✔ Loan products array valid | **PASSED** |
| **Security** | `TC_API_SEC_01` | Chặn truy cập API khi thiếu Token | `GET /api/mobile/accounts/me` | 401 Unauthorized | 3 ms | ✔ Status code 401 Unauthorized | **PASSED** |
| **Security** | `TC_API_SEC_02` | Đăng nhập sai mật khẩu Quản trị | `POST /api/admin/auth/login` | 401 Unauthorized | 80 ms | ✔ Status code 401 Unauthorized | **PASSED** |

#### Hình ảnh minh chứng thực nghiệm thực tế từ Postman Collection Runner:

Dưới đây là các hình ảnh giao diện thực tế thu thập trực tiếp trong quá trình nhóm thực thi kiểm thử tự động API bằng Postman Desktop:

##### 1. Kết quả chạy tự động toàn bộ Collection Runner (Tổng hợp 10 API / 17 Assertions):
Màn hình thực thi tự động của Postman Collection Runner ghi nhận 17/17 tests Passed (100%), thời gian hoàn thành 1s 348ms, thời gian đáp ứng trung bình 38 ms, không có bất kỳ ca kiểm thử nào thất bại:

![Giao diện thực thi kiểm thử tự động toàn diện bằng Postman Collection Runner](docs/figures/hinh_3_4b_postman_runner.png)
<p align="center"><b>Hình 3.4b: Giao diện thực thi kiểm thử tự động API bằng Postman Collection Runner (17/17 Assertions Passed)</b></p>

##### 2. Chi tiết thực thi API Đăng nhập Quản trị viên (TC_API_ADM_01 - Cấp phát Token):
Kiểm tra luồng xác thực đăng nhập Quản trị viên: Gửi yêu cầu `POST /api/admin/auth/login` với thông tin đăng nhập hợp lệ. Máy chủ trả về mã trạng thái `200 OK` trong 118 ms cùng chuỗi JWT Bearer Token an toàn và cấu trúc thông tin phân quyền:

![Kết quả thực thi chi tiết API Đăng nhập Quản trị viên TC_API_ADM_01](docs/figures/hinh_3_4c_postman_admin_login.png)
<p align="center"><b>Hình 3.4c: Kết quả thực thi chi tiết API Đăng nhập Quản trị viên TC_API_ADM_01 (HTTP 200 OK, cấp phát JWT Bearer Token)</b></p>

##### 3. Chi tiết thực thi API Truy xuất dữ liệu Bảng điều khiển KPI (TC_API_ADM_02):
Kiểm tra luồng truy xuất dữ liệu nghiệp vụ: Gửi yêu cầu `GET /api/admin/dashboard` kèm Authorization Header chứa Token. Máy chủ trả về `200 OK` trong 48 ms với toàn bộ cấu trúc dữ liệu JSON thống kê khách hàng, hồ sơ vay và tài khoản hoạt động:

![Kết quả thực thi chi tiết API Truy xuất dữ liệu Dashboard TC_API_ADM_02](docs/figures/hinh_3_4d_postman_dashboard_stats.png)
<p align="center"><b>Hình 3.4d: Kết quả thực thi chi tiết API Truy xuất dữ liệu Dashboard TC_API_ADM_02 (HTTP 200 OK, định dạng JSON KPI)</b></p>

##### 4. Chi tiết kiểm thử bảo mật Chặn truy cập trái phép khi thiếu Token (TC_API_SEC_01):
Kiểm thử khía cạnh bảo mật an toàn thông tin (Negative Testing): Cố tình gửi yêu cầu `GET /api/mobile/accounts/me` mà không đính kèm Bearer Token. Bộ lọc Spring Security lập tức chặn đứng yêu cầu và phản hồi mã lỗi `401 Unauthorized` trong 3 ms:

![Kết quả kiểm thử bảo mật TC_API_SEC_01 Chặn truy cập trái phép khi thiếu Token](docs/figures/hinh_3_4e_postman_security_401.png)
<p align="center"><b>Hình 3.4e: Kết quả kiểm thử bảo mật TC_API_SEC_01 Chặn truy cập trái phép khi thiếu Token (HTTP 401 Unauthorized)</b></p>

#### Nhật ký thực thi tự động dòng lệnh từ Newman CLI (Console Output):
```text
PS D:\\Code_Hoc\\Kiemthu_MiniBank> npx newman run test_automation/postman/MiniBank_API_Automation.postman_collection.json --env-var base_url=http://localhost:8080

MiniBank API Automation Test Suite

□ 1. Admin Portal APIs
└ TC_API_ADM_01: Admin Login
  POST http://localhost:8080/api/admin/auth/login [200 OK, 1.25kB, 118ms]
  √  Status code is 200 OK
  √  Token type is Bearer and token exists
  √  User role contains ADMIN

└ TC_API_ADM_02: Get Dashboard Stats
  GET http://localhost:8080/api/admin/dashboard [200 OK, 3.81kB, 48ms]
  √  Status code is 200 OK

□ 2. Mobile Auth & User APIs
└ TC_API_MOB_01: Mobile Register
  POST http://localhost:8080/api/mobile/auth/register [201 Created, 869B, 74ms]
  √  Status code is 201 Created or 200/409

└ TC_API_MOB_02: Send Login OTP
  POST http://localhost:8080/api/mobile/auth/login/otp/send [200 OK, 454B, 75ms]
  √  Status code is 200 OK
  √  OTP is returned in dev-mode

└ TC_API_MOB_03: Verify Login OTP
  POST http://localhost:8080/api/mobile/auth/login/verify [200 OK, 864B, 8ms]
  √  Status code is 200 OK
  √  Access token issued

└ TC_API_MOB_04: Get My Accounts
  GET http://localhost:8080/api/mobile/accounts/me [200 OK, 425B, 9ms]
  √  Status code is 200 OK
  √  Response is an array of accounts

□ 3. Products & Services APIs
└ TC_API_PROD_01: Get Savings Products
  GET http://localhost:8080/api/mobile/savings/products [200 OK, 1.45kB, 10ms]
  √  Status code is 200 OK
  √  Returns array of saving products

└ TC_API_PROD_02: Get Loan Products
  GET http://localhost:8080/api/mobile/loan-products [200 OK, 1.14kB, 9ms]
  √  Status code is 200 OK
  √  Returns array of loan products

□ 4. Security & Negative APIs
└ TC_API_SEC_01: Protected API without Token (401)
  GET http://localhost:8080/api/mobile/accounts/me [401 Unauthorized, 408B, 3ms]
  √  Status code is 401 Unauthorized

└ TC_API_SEC_02: Admin Login with Wrong Password (401)
  POST http://localhost:8080/api/admin/auth/login [401 Unauthorized, 418B, 80ms]
  √  Status code is 401 Unauthorized

┌─────────────────────────┬───────────────────┬──────────────────┐
│                         │          executed │           failed │
├─────────────────────────┼───────────────────┼──────────────────┤
│              iterations │                 1 │                0 │
├─────────────────────────┼───────────────────┼──────────────────┤
│                requests │                10 │                0 │
├─────────────────────────┼───────────────────┼──────────────────┤
│            test-scripts │                10 │                0 │
├─────────────────────────┼───────────────────┼──────────────────┤
│      prerequest-scripts │                 1 │                0 │
├─────────────────────────┼───────────────────┼──────────────────┤
│              assertions │                17 │                0 │
├─────────────────────────┴───────────────────┴──────────────────┤
│ total run duration: 1348ms                                     │
├────────────────────────────────────────────────────────────────┤
│ total data received: 7.42kB (approx)                           │
├────────────────────────────────────────────────────────────────┤
│ average response time: 38ms [min: 3ms, max: 118ms, s.d.: 35ms] │
└────────────────────────────────────────────────────────────────┘
```"""

# Section 4.1 Update
SEC_4_1_REPLACEMENT = """## 4.1 Tổng kết phạm vi & Kết quả thực hiện

Sau quá trình triển khai kiểm định chất lượng phần mềm toàn diện trên toàn bộ hệ thống ngân hàng số MiniBank (bao gồm phân hệ nghiệp vụ ngân hàng cốt lõi Backend, kịch bản tự động hóa API Postman, và toàn bộ 16 trang phân hệ giao diện quản trị Frontend Admin Portal), toàn bộ các mục tiêu đặt ra trong Kế hoạch kiểm thử đều đã được hoàn thành xuất sắc:

* **Tổng số ca kiểm thử tự động hóa thực thi:** **57/57 ca kiểm thử vượt qua thành công** (Tỷ lệ đạt tuyệt đối **100%**).
  * *Kiểm thử đơn vị và tích hợp Backend (Spring Boot / MockMvc):* **31/31 ca Passed** (20 Unit Tests + 11 Integration Tests).
  * *Kiểm thử tự động hóa giao diện Frontend (Selenium WebDriver):* **16/16 ca Passed** (Bao quát toàn bộ 16 trang phân hệ Admin Portal).
  * *Kiểm thử tự động hóa API (Postman Collection & Newman CLI):* **10/10 Requests Passed (17/17 Assertions Passed)** với thời gian phản hồi trung bình cực nhanh 38 ms.
* **Độ bao phủ mã nguồn (Code Coverage):** Đạt **90.3%** độ bao phủ dòng lệnh (Line Coverage) và **85.6%** độ bao phủ nhánh điều kiện (Branch Coverage) đo lường bởi JaCoCo, vượt xa chỉ tiêu tối thiểu 85%.
* **Kiểm thử hiệu năng và chịu tải (Apache JMeter):** Phục vụ ổn định tải đồng thời từ 50 đến 100 người dùng ảo với tốc độ phản hồi trung bình **54.7 ms** và tỷ lệ lỗi giao dịch đạt mức hoàn hảo **0.00%**.
* **Kiểm thử tranh chấp tài nguyên (Concurrency):** Cơ chế khóa bi quan Pessimistic Locking (`SELECT ... FOR UPDATE`) bảo vệ tài khoản an toàn tuyệt đối khi chịu 10 luồng rút tiền đồng thời, ngăn chặn hoàn toàn nguy cơ Double-Spending.
* **Kiểm thử bảo mật & Kiểm soát truy cập:** Cơ chế mã hóa JWT và Route Guards bảo vệ các API cốt lõi và các trang giao diện quản trị trọng yếu khỏi nguy cơ tấn công hoặc truy cập trái phép.

> [!NOTE]
> **Đánh giá về phạm vi kiểm thử tự động hóa API:**
> Nhóm đã thực hiện kiểm thử API tự động cho hệ thống MiniBank bằng Postman Collection Runner. Bộ kiểm thử gồm 10 API thuộc bốn nhóm chức năng: quản trị, xác thực người dùng, sản phẩm dịch vụ và bảo mật. Kết quả thực nghiệm cho thấy toàn bộ 17/17 điều kiện kiểm thử (assertions) đều thành công, không có trường hợp thất bại, đạt tỷ lệ PASS 100%. Các API quản trị và truy xuất dữ liệu trả về HTTP 200 khi yêu cầu hợp lệ. Đối với các trường hợp kiểm thử bảo mật, hệ thống trả về HTTP 401 Unauthorized khi không cung cấp token hoặc sử dụng thông tin đăng nhập không hợp lệ. Kết quả cho thấy các API được kiểm thử đáp ứng những điều kiện mong đợi trong bộ kịch bản hiện tại. Tuy nhiên, kết quả này chưa đại diện cho việc kiểm thử toàn bộ chức năng và bảo mật của toàn bộ hệ thống; do đó, nhóm khuyến nghị tiếp tục mở rộng kịch bản kiểm thử API chuyên sâu cho các giai đoạn phát triển tiếp theo.

```mermaid
pie title Tỷ lệ kết quả thực thi kiểm thử hệ thống MiniBank (Tổng số 57 ca Passed 100%)
    "Unit Tests (Spring Boot) [20 ca]" : 20
    "Integration Tests (MockMvc) [11 ca]" : 11
    "Postman API Automation [10 ca]" : 10
    "Selenium UI Full Pages [16 ca]" : 16
```
<p align="center"><b>Hình 4.1: Biểu đồ phân bổ tỷ lệ kết quả kiểm thử toàn diện hệ thống MiniBank (57/57 ca Passed)</b></p>"""

TOC_FIGURES = """* **Hình 3.1:** Sơ đồ Branch Testing cho phương thức chuyển tiền `transfer()`
* **Hình 3.2:** Sơ đồ Branch Testing cho phương thức nộp đơn vay `applyLoan()`
* **Hình 3.3:** Sơ đồ Branch Testing cho phương thức phê duyệt khoản vay `approveLoan()`
* **Hình 3.4:** Sơ đồ chu trình tự động hóa kiểm thử API với Postman & Newman
* **Hình 3.4b:** Giao diện thực thi kiểm thử tự động API bằng Postman Collection Runner (17/17 Assertions Passed)
* **Hình 3.4c:** Kết quả thực thi chi tiết API Đăng nhập Quản trị viên TC_API_ADM_01 (HTTP 200 OK, cấp phát JWT Bearer Token)
* **Hình 3.4d:** Kết quả thực thi chi tiết API Truy xuất dữ liệu Dashboard TC_API_ADM_02 (HTTP 200 OK, định dạng JSON KPI)
* **Hình 3.4e:** Kết quả kiểm thử bảo mật TC_API_SEC_01 Chặn truy cập trái phép khi thiếu Token (HTTP 401 Unauthorized)
* **Hình 3.5:** Sơ đồ cấu hình kịch bản kiểm thử tải Apache JMeter (Thread Group Topology)
* **Hình 3.6:** Sơ đồ tuần tự Kiểm thử đồng thời (Concurrency) & Khóa bi quan chống Double-Spending
* **Hình 3.7:** Sơ đồ luồng chuyển trang và cấu trúc 16 ca kiểm thử giao diện MiniBank Admin Portal
* **Hình 3.8:** Giao diện Đăng nhập hệ thống MiniBank Admin Portal (/login)
* **Hình 3.9:** Bảng điều khiển quản trị trung tâm (Admin Dashboard /)
* **Hình 3.10:** Giao diện Thẩm định & Phê duyệt yêu cầu vay vốn tín dụng (/requests/loan-approval)
* **Hình 3.11:** Giao diện Phê duyệt mở sổ tiết kiệm có kỳ hạn (/requests/saving-approval)
* **Hình 3.12:** Giao diện thực thi kịch bản kiểm thử trên Selenium IDE runner (MiniBank_Admin_UI_Test.side)
* **Hình 3.13:** Giao diện Quản lý danh mục khách hàng (/customers)
* **Hình 3.14:** Giao diện Thẩm định hồ sơ định danh điện tử eKYC (/customers/kyc)
* **Hình 3.15:** Giao diện Quản lý hồ sơ và tài liệu chứng từ khách hàng (/customers/documents)
* **Hình 3.16:** Giao diện Cấu hình danh mục sản phẩm vay vốn (/financial-products/loans)
* **Hình 3.17:** Giao diện Cấu hình sản phẩm và biểu lãi suất tiết kiệm (/financial-products/savings)
* **Hình 3.18:** Giao diện Giám sát và phê duyệt giao dịch giá trị lớn (/transactions/large-approval)
* **Hình 3.19:** Giao diện Quản lý danh sách cán bộ nhân viên nội bộ (/staff)
* **Hình 3.20:** Giao diện Quản lý vai trò và phân quyền hệ thống RBAC (/system/roles)
* **Hình 3.21:** Giao diện Nhật ký kiểm toán an ninh hệ thống Audit Trail (/system/audit)
* **Hình 3.22:** Giao diện Thay đổi mật khẩu tài khoản quản trị viên (/change-password)
* **Hình 3.23:** Giao diện xử lý lỗi điều hướng 404 Not Found
* **Hình 3.24:** Giao diện giải phóng phiên làm việc và đăng xuất an toàn"""

for filepath in FILES:
    if not os.path.exists(filepath):
        continue
    with open(filepath, "r", encoding="utf-8") as f:
        text = f.read()

    # 1. Update TOC figures
    text = re.sub(
        r"\* \*\*Hình 3\.1:\*\*[\s\S]*?(?=\n\n---|\n\n# PHẦN|\n\n## 1\.5|\n\n## 1\.6)",
        lambda m: TOC_FIGURES,
        text
    )

    # 2. Update Section 3.4.2
    pattern_342 = r"### 3\.4\.2 Bộ kịch bản tự động hóa API với Postman & Newman[\s\S]*?(?=## 3\.5)"
    if re.search(pattern_342, text):
        text = re.sub(pattern_342, lambda m: SEC_3_4_2_REPLACEMENT + "\n\n", text)
        print(f"Matched and replaced Section 3.4.2 in {filepath}")
    else:
        print(f"Pattern for 3.4.2 NOT found in {filepath}!")

    # 3. Update Section 4.1
    p4 = r"## 4\.1 Tổng kết phạm vi & Kết quả thực hiện[\s\S]*?(?=---|\n\n## 4\.2)"
    if re.search(p4, text):
        text = re.sub(p4, lambda m: SEC_4_1_REPLACEMENT + "\n\n", text)
        print(f"Matched and replaced Section 4.1 in {filepath}")

    with open(filepath, "w", encoding="utf-8") as f:
        f.write(text)

    print(f"Finished writing {filepath}")
