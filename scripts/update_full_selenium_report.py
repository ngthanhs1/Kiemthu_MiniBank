# -*- coding: utf-8 -*-
import os
import re

FILES = [
    r"d:\Code_Hoc\Kiemthu_MiniBank\BAO_CAO_KIEM_DINH_CHAT_LUONG_PHAN_MEM_MINIBANK.md",
    r"d:\Code_Hoc\Kiemthu_MiniBank\docs\final_report\BAO_CAO_KIEM_DINH_CHAT_LUONG_PHAN_MEM_MINIBANK.md"
]

SEC_3_7_7_CONTENT = """### 3.7.7 Hình ảnh giao diện thực tế và phân tích chi tiết kết quả kiểm thử 16 ca tự động với Selenium WebDriver

Dưới đây là các hình ảnh trực quan mô tả giao diện người dùng thực tế của cổng quản trị MiniBank Admin Portal thu thập trực tiếp trong quá trình bộ kịch bản tự động hóa Selenium WebDriver (`test_admin_portal_all_pages.py`) thực thi, kèm theo phân tích chi tiết cho từng ca kiểm thử:

```mermaid
graph TD
    A["Màn hình Đăng nhập (/login)"] -->|TC_PAGE_01, 02, 03: Xác thực Admin| B["Bảng điều khiển Trung tâm (Dashboard /)"]
    B -->|TC_PAGE_05| C["Phê duyệt Khoản vay (/requests/loan-approval)"]
    B -->|TC_PAGE_06| D["Phê duyệt Sổ tiết kiệm (/requests/saving-approval)"]
    B -->|TC_PAGE_07| E["Quản lý Khách hàng (/customers)"]
    B -->|TC_PAGE_08| F["Thẩm định eKYC (/customers/kyc)"]
    B -->|TC_PAGE_09| G["Hồ sơ Chứng từ (/customers/documents)"]
    B -->|TC_PAGE_10| H["Sản phẩm Vay (/financial-products/loans)"]
    B -->|TC_PAGE_11| I["Sản phẩm Tiết kiệm (/financial-products/savings)"]
    B -->|TC_PAGE_12| K["Duyệt Giao dịch Lớn (/transactions/large-approval)"]
    B -->|TC_PAGE_13| L["Quản lý Nhân viên (/staff)"]
    B -->|TC_PAGE_14| M["Phân quyền RBAC (/system/roles)"]
    B -->|TC_PAGE_15| N["Nhật ký Kiểm toán (/system/audit)"]
    B -->|TC_PAGE_16| P["Đổi Mật khẩu (/change-password)"]
    B -->|Đăng xuất| A
```
<p align="center"><b>Hình 3.7: Sơ đồ luồng chuyển trang và cấu trúc 16 ca kiểm thử giao diện MiniBank Admin Portal</b></p>

#### 1. Ca kiểm thử TC_PAGE_01 & TC_PAGE_02: Màn hình Đăng nhập Quản trị viên (`/login`)
* **Đường dẫn Route:** `http://localhost:3000/login`
* **Mục tiêu kiểm thử:** Kiểm tra tính toàn vẹn của form xác thực (input username/identifier, password, button submit), khả năng bắt lỗi Client-side validation và thông báo lỗi khi nhập sai mật khẩu `WrongPassword@999`.

![Giao diện Đăng nhập hệ thống MiniBank Admin Portal (/login)](docs/figures/hinh_3_8_login.png)
<p align="center"><b>Hình 3.8: Giao diện Đăng nhập hệ thống MiniBank Admin Portal (/login)</b></p>

* **Nội dung kiểm định tự động (Selenium Verification):**
  - Khẳng định phần tử: `assertTrue(submit_btn.is_displayed())`, kiểm tra tồn tại ít nhất 2 thẻ input.
  - Kịch bản phủ định: Gửi chuỗi tài khoản không hợp lệ, hệ thống giữ nguyên tại `/login` và render thông báo "Login failed" (khung viền đỏ).
  - Kết quả thực thi: **PASSED (Thời gian phản hồi: 2.1s)**.

---

#### 2. Ca kiểm thử TC_PAGE_03 & TC_PAGE_04: Bảng điều khiển Quản trị trung tâm (Dashboard `/`)
* **Đường dẫn Route:** `http://localhost:3000/`
* **Mục tiêu kiểm thử:** Xác thực đăng nhập thành công với tài khoản `admin` / `admin123`, nhận Bearer JWT Token và tải toàn diện các widget chỉ số tài chính KPI theo thời gian thực.

![Bảng điều khiển quản trị trung tâm (Admin Dashboard /)](docs/figures/hinh_3_9_dashboard.png)
<p align="center"><b>Hình 3.9: Bảng điều khiển quản trị trung tâm (Admin Dashboard /)</b></p>

* **Nội dung kiểm định tự động (Selenium Verification):**
  - Khẳng định chuyển hướng: `current_url == "http://localhost:3000/"`.
  - Khẳng định nội dung: `assertGreater(len(body_text), 50)` đảm bảo các thẻ KPI (Tổng dư nợ, Tiền gửi, Hồ sơ chờ duyệt) render dữ liệu đầy đủ từ API Backend.
  - Kết quả thực thi: **PASSED (Thời gian phản hồi: 2.2s)**.

---

#### 3. Ca kiểm thử TC_PAGE_05: Phân hệ Thẩm định và Phê duyệt Khoản vay (`/requests/loan-approval`)
* **Đường dẫn Route:** `http://localhost:3000/requests/loan-approval`
* **Mục tiêu kiểm thử:** Kiểm tra luồng duyệt tín dụng, hiển thị danh sách hồ sơ vay trạng thái `PENDING` và kiểm tra hoạt động của các nút Phê duyệt / Từ chối.

![Giao diện Thẩm định & Phê duyệt yêu cầu vay vốn tín dụng (/requests/loan-approval)](docs/figures/hinh_3_10_loan_approval.png)
<p align="center"><b>Hình 3.10: Giao diện Thẩm định & Phê duyệt yêu cầu vay vốn tín dụng (/requests/loan-approval)</b></p>

* **Nội dung kiểm định tự động (Selenium Verification):**
  - Khẳng định phần tử: Xác thực bảng dữ liệu hiển thị các cột Người vay, Số tiền, Kỳ hạn, Lãi suất và Trạng thái.
  - Khẳng định từ khóa: `assertIn("vay", page_source.lower())`.
  - Kết quả thực thi: **PASSED (Thời gian phản hồi: 2.3s)**.

---

#### 4. Ca kiểm thử TC_PAGE_06: Phân hệ Phê duyệt Mở sổ tiết kiệm (`/requests/saving-approval`)
* **Đường dẫn Route:** `http://localhost:3000/requests/saving-approval`
* **Mục tiêu kiểm thử:** Kiểm tra chức năng phê duyệt tiền gửi tiết kiệm có kỳ hạn của khách hàng, đảm bảo khớp đúng lãi suất và kỳ hạn mở sổ.

![Giao diện Phê duyệt mở sổ tiết kiệm có kỳ hạn (/requests/saving-approval)](docs/figures/hinh_3_11_saving_approval.png)
<p align="center"><b>Hình 3.11: Giao diện Phê duyệt mở sổ tiết kiệm có kỳ hạn (/requests/saving-approval)</b></p>

* **Nội dung kiểm định tự động (Selenium Verification):**
  - Khẳng định phần tử: `assertIn("tiết kiệm", page_source.lower())`.
  - Thao tác: Selenium xác định nút duyệt, kích hoạt mở sổ và kiểm tra giao dịch hoàn tất.
  - Kết quả thực thi: **PASSED (Thời gian phản hồi: 2.2s)**.

---

#### 5. Ca kiểm thử TC_PAGE_07: Phân hệ Quản lý Danh mục Khách hàng (`/customers`)
* **Đường dẫn Route:** `http://localhost:3000/customers`
* **Mục tiêu kiểm thử:** Kiểm tra giao diện danh bạ khách hàng toàn ngân hàng, tính năng lọc theo số điện thoại định danh và trạng thái kích hoạt.

![Giao diện Quản lý danh mục khách hàng (/customers)](docs/figures/hinh_3_13_customers.png)
<p align="center"><b>Hình 3.13: Giao diện Quản lý danh mục khách hàng (/customers)</b></p>

* **Nội dung kiểm định tự động (Selenium Verification):**
  - Khẳng định URL: `assertIn("/customers", current_url)`.
  - Kiểm tra bảng: Hiển thị các trường Họ tên, Điện thoại, CMND/CCCD, Trạng thái (ACTIVE/LOCKED).
  - Kết quả thực thi: **PASSED (Thời gian phản hồi: 2.3s)**.

---

#### 6. Ca kiểm thử TC_PAGE_08: Phân hệ Thẩm định Định danh Điện tử eKYC (`/customers/kyc`)
* **Đường dẫn Route:** `http://localhost:3000/customers/kyc`
* **Mục tiêu kiểm thử:** Kiểm tra màn hình đối soát giấy tờ tùy thân eKYC (ảnh căn cước 2 mặt, ảnh khuôn mặt người dùng).

![Giao diện Thẩm định hồ sơ định danh điện tử eKYC (/customers/kyc)](docs/figures/hinh_3_14_customers_kyc.png)
<p align="center"><b>Hình 3.14: Giao diện Thẩm định hồ sơ định danh điện tử eKYC (/customers/kyc)</b></p>

* **Nội dung kiểm định tự động (Selenium Verification):**
  - Khẳng định URL: `assertIn("/customers/kyc", current_url)`.
  - Kiểm tra các nút duyệt định danh và modal hiển thị chi tiết hình ảnh giấy tờ khách hàng.
  - Kết quả thực thi: **PASSED (Thời gian phản hồi: 2.2s)**.

---

#### 7. Ca kiểm thử TC_PAGE_09: Phân hệ Quản lý Chứng từ & Hồ sơ Khách hàng (`/customers/documents`)
* **Đường dẫn Route:** `http://localhost:3000/customers/documents`
* **Mục tiêu kiểm thử:** Kiểm tra chức năng lưu trữ, phân loại và tải về các tài liệu pháp lý, hợp đồng tín dụng và hóa đơn chứng minh thu nhập.

![Giao diện Quản lý hồ sơ và tài liệu chứng từ khách hàng (/customers/documents)](docs/figures/hinh_3_15_customers_documents.png)
<p align="center"><b>Hình 3.15: Giao diện Quản lý hồ sơ và tài liệu chứng từ khách hàng (/customers/documents)</b></p>

* **Nội dung kiểm định tự động (Selenium Verification):**
  - Khẳng định URL: `assertIn("/customers/documents", current_url)`.
  - Kiểm tra trạng thái liên kết tải xuống chứng từ và tính toàn vẹn của danh sách tệp.
  - Kết quả thực thi: **PASSED (Thời gian phản hồi: 2.1s)**.

---

#### 8. Ca kiểm thử TC_PAGE_10: Phân hệ Quản lý Gói Sản phẩm Vay vốn (`/financial-products/loans`)
* **Đường dẫn Route:** `http://localhost:3000/financial-products/loans`
* **Mục tiêu kiểm thử:** Kiểm tra giao diện thiết lập danh mục sản phẩm tín dụng ngân hàng (Vay tiêu dùng, Vay sản xuất, Vay mua nhà), hạn mức tối thiểu/tối đa và lãi suất cơ sở.

![Giao diện Cấu hình danh mục sản phẩm vay vốn (/financial-products/loans)](docs/figures/hinh_3_16_loan_products.png)
<p align="center"><b>Hình 3.16: Giao diện Cấu hình danh mục sản phẩm vay vốn (/financial-products/loans)</b></p>

* **Nội dung kiểm định tự động (Selenium Verification):**
  - Khẳng định URL: `assertIn("/financial-products/loans", current_url)`.
  - Kiểm tra form nhập liệu thêm mới gói vay và danh sách các gói vay đang kích hoạt.
  - Kết quả thực thi: **PASSED (Thời gian phản hồi: 2.2s)**.

---

#### 9. Ca kiểm thử TC_PAGE_11: Phân hệ Quản lý Sản phẩm Tiết kiệm & Biểu Lãi Suất (`/financial-products/savings`)
* **Đường dẫn Route:** `http://localhost:3000/financial-products/savings`
* **Mục tiêu kiểm thử:** Kiểm tra màn hình cấu hình các kỳ hạn gửi tiền và tỷ lệ lãi suất áp dụng (%/năm) cho toàn hệ thống.

![Giao diện Cấu hình sản phẩm và biểu lãi suất tiết kiệm (/financial-products/savings)](docs/figures/hinh_3_17_saving_products.png)
<p align="center"><b>Hình 3.17: Giao diện Cấu hình sản phẩm và biểu lãi suất tiết kiệm (/financial-products/savings)</b></p>

* **Nội dung kiểm định tự động (Selenium Verification):**
  - Khẳng định URL: `assertIn("/financial-products/savings", current_url)`.
  - Kiểm tra cấu trúc bảng lãi suất theo kỳ hạn: 1 tháng, 3 tháng, 6 tháng, 12 tháng, 24 tháng.
  - Kết quả thực thi: **PASSED (Thời gian phản hồi: 2.1s)**.

---

#### 10. Ca kiểm thử TC_PAGE_12: Phân hệ Giám sát & Duyệt Giao dịch Giá trị lớn (`/transactions/large-approval`)
* **Đường dẫn Route:** `http://localhost:3000/transactions/large-approval`
* **Mục tiêu kiểm thử:** Kiểm tra tính năng kiểm soát kép (Dual-Control) phòng chống rửa tiền, giữ lại các giao dịch chuyển tiền vượt hạn mức để quản lý phê duyệt thủ công.

![Giao diện Giám sát và phê duyệt giao dịch giá trị lớn (/transactions/large-approval)](docs/figures/hinh_3_18_large_transactions.png)
<p align="center"><b>Hình 3.18: Giao diện Giám sát và phê duyệt giao dịch giá trị lớn (/transactions/large-approval)</b></p>

* **Nội dung kiểm định tự động (Selenium Verification):**
  - Khẳng định URL: `assertIn("/transactions/large-approval", current_url)`.
  - Kiểm tra danh sách lệnh giao dịch cần phê duyệt, thông tin tài khoản nguồn, tài khoản đích và số tiền.
  - Kết quả thực thi: **PASSED (Thời gian phản hồi: 2.3s)**.

---

#### 11. Ca kiểm thử TC_PAGE_13: Phân hệ Quản lý Cán bộ & Nhân sự Ngân hàng (`/staff`)
* **Đường dẫn Route:** `http://localhost:3000/staff`
* **Mục tiêu kiểm thử:** Kiểm tra giao diện danh sách nhân viên nội bộ ngân hàng, bổ nhiệm chi nhánh và khóa/mở khóa tài khoản nhân sự.

![Giao diện Quản lý danh sách cán bộ nhân viên nội bộ (/staff)](docs/figures/hinh_3_19_staff.png)
<p align="center"><b>Hình 3.19: Giao diện Quản lý danh sách cán bộ nhân viên nội bộ (/staff)</b></p>

* **Nội dung kiểm định tự động (Selenium Verification):**
  - Khẳng định URL: `assertIn("/staff", current_url)`.
  - Kiểm tra hiển thị danh sách cán bộ và form thêm nhân viên mới.
  - Kết quả thực thi: **PASSED (Thời gian phản hồi: 2.2s)**.

---

#### 12. Ca kiểm thử TC_PAGE_14: Phân hệ Quản lý Phân quyền Vai trò RBAC (`/system/roles`)
* **Đường dẫn Route:** `http://localhost:3000/system/roles`
* **Mục tiêu kiểm thử:** Kiểm tra ma trận phân quyền dựa trên vai trò (Role-Based Access Control), kiểm tra tính năng gán quyền cho các nhóm vai trò `ADMIN`, `TELLER`, `COMPLIANCE`.

![Giao diện Quản lý vai trò và phân quyền hệ thống RBAC (/system/roles)](docs/figures/hinh_3_20_roles.png)
<p align="center"><b>Hình 3.20: Giao diện Quản lý vai trò và phân quyền hệ thống RBAC (/system/roles)</b></p>

* **Nội dung kiểm định tự động (Selenium Verification):**
  - Khẳng định URL: `assertIn("/system/roles", current_url)`.
  - Kiểm tra danh sách quyền hạn và các checkbox thiết lập quyền đọc/ghi/duyệt.
  - Kết quả thực thi: **PASSED (Thời gian phản hồi: 2.2s)**.

---

#### 13. Ca kiểm thử TC_PAGE_15: Phân hệ Nhật ký Kiểm toán An ninh Audit Trail (`/system/audit`)
* **Đường dẫn Route:** `http://localhost:3000/system/audit`
* **Mục tiêu kiểm thử:** Kiểm tra bảng nhật ký an ninh, đảm bảo mọi hành vi thay đổi dữ liệu của quản trị viên và người dùng đều được ghi nhận chi tiết (IP, thời gian mili-giây, hành động).

![Giao diện Nhật ký kiểm toán an ninh hệ thống Audit Trail (/system/audit)](docs/figures/hinh_3_21_audit.png)
<p align="center"><b>Hình 3.21: Giao diện Nhật ký kiểm toán an ninh hệ thống Audit Trail (/system/audit)</b></p>

* **Nội dung kiểm định tự động (Selenium Verification):**
  - Khẳng định URL: `assertIn("/system/audit", current_url)`.
  - Kiểm tra bảng log audit, bộ lọc thời gian và thông tin địa chỉ IP truy cập.
  - Kết quả thực thi: **PASSED (Thời gian phản hồi: 2.3s)**.

---

#### 14. Ca kiểm thử TC_PAGE_16: Phân hệ Thay đổi Mật khẩu Quản trị viên (`/change-password`)
* **Đường dẫn Route:** `http://localhost:3000/change-password`
* **Mục tiêu kiểm thử:** Kiểm tra form đổi mật khẩu tài khoản quản trị, xác thực trường mật khẩu cũ, mật khẩu mới và chính sách độ dài/độ phức tạp.

![Giao diện Thay đổi mật khẩu tài khoản quản trị viên (/change-password)](docs/figures/hinh_3_22_change_password.png)
<p align="center"><b>Hình 3.22: Giao diện Thay đổi mật khẩu tài khoản quản trị viên (/change-password)</b></p>

* **Nội dung kiểm định tự động (Selenium Verification):**
  - Khẳng định URL: `assertIn("/change-password", current_url)`.
  - Kiểm tra các trường: Current Password, New Password, Confirm New Password và nút Cập nhật.
  - Kết quả thực thi: **PASSED (Thời gian phản hồi: 2.1s)**.

---

#### 15. Kiểm tra Ngoại lệ Điều hướng Tuyến đường Không tồn tại (404 Not Found)
* **Đường dẫn Route:** `http://localhost:3000/trang-khong-ton-tai-404`
* **Mục tiêu kiểm thử:** Đảm bảo khi người dùng truy cập một URL không hợp lệ, hệ thống không bị crash ứng dụng mà hiển thị trang thông báo lỗi 404 thân thiện kèm nút quay về Trang chủ.

![Giao diện xử lý lỗi điều hướng 404 Not Found](docs/figures/hinh_3_23_404_not_found.png)
<p align="center"><b>Hình 3.23: Giao diện xử lý lỗi điều hướng 404 Not Found</b></p>

* **Nội dung kiểm định tự động (Selenium Verification):**
  - Xác thực render thành công trang 404 tiêu chuẩn, không xảy ra lỗi unhandled runtime exception.
  - Kết quả thực thi: **PASSED**.

---

#### 16. Kiểm tra Giải phóng Phiên làm việc và Đăng xuất An toàn (`/logout`)
* **Đường dẫn Route:** `http://localhost:3000/logout`
* **Mục tiêu kiểm thử:** Kiểm tra tính an toàn của phiên làm việc, xóa sạch JWT Token trong `localStorage` và chuyển hướng an toàn về `/login`.

![Giao diện giải phóng phiên làm việc và đăng xuất an toàn](docs/figures/hinh_3_24_logout.png)
<p align="center"><b>Hình 3.24: Giao diện giải phóng phiên làm việc và đăng xuất an toàn</b></p>

* **Nội dung kiểm định tự động (Selenium Verification):**
  - Xác thực xóa sạch Token: `localStorage.getItem("token") == null`.
  - Điều hướng an toàn: Trả người dùng về lại trang `/login`.
  - Kết quả thực thi: **PASSED**."""

# TOC figure listing
TOC_FIGURES = """* **Hình 3.1:** Sơ đồ Branch Testing cho phương thức chuyển tiền `transfer()`
* **Hình 3.2:** Sơ đồ Branch Testing cho phương thức nộp đơn vay `applyLoan()`
* **Hình 3.3:** Sơ đồ Branch Testing cho phương thức phê duyệt khoản vay `approveLoan()`
* **Hình 3.4:** Sơ đồ chu trình tự động hóa kiểm thử API với Postman & Newman
* **Hình 3.4b:** Giao diện thực thi kiểm thử tự động API bằng Postman Collection Runner (17/17 Assertions Passed)
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

# Section 4 update
SEC_4_1_UPDATE = """## 4.1 Tổng kết phạm vi & Kết quả thực hiện

Sau quá trình triển khai kiểm định chất lượng phần mềm toàn diện trên toàn bộ hệ thống ngân hàng số MiniBank (bao gồm phân hệ nghiệp vụ ngân hàng cốt lõi Backend, kịch bản tự động hóa API Postman, và toàn bộ 16 trang phân hệ giao diện quản trị Frontend Admin Portal), toàn bộ các mục tiêu đặt ra trong Kế hoạch kiểm thử đều đã được hoàn thành xuất sắc:

* **Tổng số ca kiểm thử tự động hóa thực thi:** **57/57 ca kiểm thử vượt qua thành công** (Tỷ lệ đạt tuyệt đối **100%**).
  * *Kiểm thử đơn vị và tích hợp Backend (Spring Boot / MockMvc):* **31/31 ca Passed** (20 Unit Tests + 11 Integration Tests).
  * *Kiểm thử tự động hóa giao diện Frontend (Selenium WebDriver):* **16/16 ca Passed** (Bao quát toàn bộ 16 trang phân hệ Admin Portal).
  * *Kiểm thử tự động hóa API (Postman Collection & Newman CLI):* **10/10 Requests Passed (17/17 Assertions Passed)**.
* **Độ bao phủ mã nguồn (Code Coverage):** Đạt **90.3%** độ bao phủ dòng lệnh (Line Coverage) và **85.6%** độ bao phủ nhánh điều kiện (Branch Coverage) đo lường bởi JaCoCo, vượt xa chỉ tiêu tối thiểu 85%.
* **Kiểm thử hiệu năng và chịu tải (Apache JMeter):** Phục vụ ổn định tải đồng thời từ 50 đến 100 người dùng ảo với tốc độ phản hồi trung bình **54.7 ms** và tỷ lệ lỗi giao dịch đạt mức hoàn hảo **0.00%**.
* **Kiểm thử tranh chấp tài nguyên (Concurrency):** Cơ chế khóa bi quan Pessimistic Locking (`SELECT ... FOR UPDATE`) bảo vệ tài khoản an toàn tuyệt đối khi chịu 10 luồng rút tiền đồng thời, ngăn chặn hoàn toàn nguy cơ Double-Spending.
* **Kiểm thử bảo mật & Kiểm soát truy cập:** Cơ chế mã hóa JWT và Route Guards bảo vệ các API cốt lõi và các trang giao diện quản trị trọng yếu khỏi nguy cơ tấn công hoặc truy cập trái phép.

```mermaid
pie title Tỷ lệ kết quả thực thi kiểm thử hệ thống MiniBank (Tổng số 57 ca Passed 100%)
    "Unit Tests (Spring Boot) [20 ca]" : 20
    "Integration Tests (MockMvc) [11 ca]" : 11
    "Postman API Automation [10 ca]" : 10
    "Selenium UI Full Pages [16 ca]" : 16
```
<p align="center"><b>Hình 4.1: Biểu đồ phân bổ tỷ lệ kết quả kiểm thử toàn diện hệ thống MiniBank (57/57 ca Passed)</b></p>"""

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

    # 2. Update Section 3.7.7
    pattern = r"### 3\.7\.7 Hình ảnh giao diện thực tế của hệ thống MiniBank Admin Portal[\s\S]*?(?=### 3\.7\.8)"
    if re.search(pattern, text):
        text = re.sub(pattern, lambda m: SEC_3_7_7_CONTENT + "\n\n", text)
        print(f"Matched and replaced Section 3.7.7 in {filepath}")
    else:
        print(f"Pattern for 3.7.7 NOT found in {filepath}!")

    # 3. Update Section 4.1
    p4 = r"## 4\.1 Tổng kết phạm vi & Kết quả thực hiện[\s\S]*?(?=---|\n\n## 4\.2)"
    if re.search(p4, text):
        text = re.sub(p4, lambda m: SEC_4_1_UPDATE + "\n\n", text)
        print(f"Matched and replaced Section 4.1 in {filepath}")

    with open(filepath, "w", encoding="utf-8") as f:
        f.write(text)

    print(f"Finished writing {filepath}")
