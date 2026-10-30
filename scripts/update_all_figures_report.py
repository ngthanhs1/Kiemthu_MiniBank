# -*- coding: utf-8 -*-
"""
Update BAO_CAO_KIEM_DINH_CHAT_LUONG_PHAN_MEM_MINIBANK.md with all 16 figures:
- Hình 3.4b: Postman Runner
- Hình 3.8 to 3.11: Login, Dashboard, Loan Approval, Saving Approval
- Hình 3.12: Selenium IDE Runner
- Hình 3.13 to 3.22: Customers, KYC, Documents, Loan Products, Saving Products, Large Transactions, Staff, Roles, Audit, Change Password
Also updates TOC List of Figures.
"""

import os
import re

FILES = [
    r"d:\Code_Hoc\Kiemthu_MiniBank\BAO_CAO_KIEM_DINH_CHAT_LUONG_PHAN_MEM_MINIBANK.md",
    r"d:\Code_Hoc\Kiemthu_MiniBank\docs\final_report\BAO_CAO_KIEM_DINH_CHAT_LUONG_PHAN_MEM_MINIBANK.md",
]

# Section 3.7.7 replacement with all 14 pages
FULL_UI_PAGES_MD = """### 3.7.7 Hình ảnh trực quan và đánh giá kiểm thử giao diện các phân hệ Admin Portal

Bộ kiểm thử Selenium WebDriver tự động đã tiến hành rà soát, ghi nhận hình ảnh thực tế và kiểm tra tính toàn vẹn của toàn bộ các màn hình phân hệ giao diện trong hệ thống quản trị ngân hàng số MiniBank:

#### 1. Màn hình Đăng nhập Quản trị viên (`/login`):
Giao diện đăng nhập hiện đại với phông chữ Inter chuẩn quốc tế, bố cục hộp thoại căn giữa, hỗ trợ kiểm tra tính hợp lệ dữ liệu (Client-side validation) và hiển thị thông báo an toàn:

![Giao diện Đăng nhập hệ thống MiniBank Admin Portal (/login)](docs/figures/hinh_3_8_login.png)
<p align="center"><b>Hình 3.8: Giao diện Đăng nhập hệ thống MiniBank Admin Portal (/login)</b></p>

*Đánh giá kiểm thử:*
- Đăng nhập với tài khoản `admin` / `admin123` chuyển hướng thành công đến Dashboard trong thời gian 1.2s.
- Cơ chế Route Guard bảo vệ các trang bên trong, nếu chưa đăng nhập sẽ tự động chuyển hướng về `/login`.

#### 2. Bảng điều khiển Quản trị trung tâm (Dashboard `/`):
Cung cấp các thẻ thống kê trực quan (Tổng số dư tiền gửi, Tổng số khoản vay đã giải ngân, Hồ sơ vay cần phê duyệt, Sổ tiết kiệm đang chờ duyệt) kèm biểu đồ phân bổ dòng tiền:

![Bảng điều khiển quản trị trung tâm (Admin Dashboard /)](docs/figures/hinh_3_9_dashboard.png)
<p align="center"><b>Hình 3.9: Bảng điều khiển quản trị trung tâm (Admin Dashboard /)</b></p>

*Đánh giá kiểm thử:*
- Dữ liệu KPI được đồng bộ thời gian thực từ API `GET /api/admin/dashboard`.
- Các thẻ widget hiển thị rõ ràng, layout co giãn đáp ứng chuẩn Responsive Design.

#### 3. Màn hình Thẩm định và Phê duyệt Khoản vay (`/requests/loan-approval`):
Cho phép cán bộ tín dụng tra cứu thông tin người vay, số tiền vay, kỳ hạn, lãi suất đề xuất, và thực hiện phê duyệt giải ngân hoặc từ chối hồ sơ:

![Giao diện Thẩm định & Phê duyệt yêu cầu vay vốn tín dụng (/requests/loan-approval)](docs/figures/hinh_3_10_loan_approval.png)
<p align="center"><b>Hình 3.10: Giao diện Thẩm định & Phê duyệt yêu cầu vay vốn tín dụng (/requests/loan-approval)</b></p>

*Đánh giá kiểm thử:*
- Bảng hiển thị danh sách hồ sơ với các cột mã khách hàng, số tiền, kỳ hạn, trạng thái `PENDING`.
- Thao tác bấm nút Phê duyệt kích hoạt Dialog xác nhận an toàn, ngăn chặn việc click nhầm.

#### 4. Màn hình Phê duyệt Mở sổ tiết kiệm (`/requests/saving-approval`):
Hiển thị danh sách khách hàng gửi tiết kiệm trực tuyến, cho phép kích hoạt hợp đồng tiền gửi có kỳ hạn:

![Giao diện Phê duyệt mở sổ tiết kiệm có kỳ hạn (/requests/saving-approval)](docs/figures/hinh_3_11_saving_approval.png)
<p align="center"><b>Hình 3.11: Giao diện Phê duyệt mở sổ tiết kiệm có kỳ hạn (/requests/saving-approval)</b></p>

*Đánh giá kiểm thử:*
- Xác minh tính chính xác của lãi suất áp dụng và ngày đáo hạn dự kiến.
- Cập nhật tức thời trạng thái sang `ACTIVE` và ghi nhận giao dịch vào tài khoản khách hàng.

#### 5. Màn hình Quản lý Danh mục Khách hàng (`/customers`):
Quản lý tập trung toàn bộ danh sách khách hàng trong hệ thống ngân hàng, bao gồm họ tên, số điện thoại định danh, trạng thái tài khoản, và lịch sử giao dịch:

![Giao diện Quản lý danh mục khách hàng (/customers)](docs/figures/hinh_3_13_customers.png)
<p align="center"><b>Hình 3.13: Giao diện Quản lý danh mục khách hàng (/customers)</b></p>

*Đánh giá kiểm thử:*
- Bảng dữ liệu hỗ trợ tìm kiếm theo số điện thoại hoặc mã định danh.
- Hiển thị badge trạng thái trực quan: `ACTIVE`, `LOCKED`, hoặc `UNVERIFIED`.

#### 6. Màn hình Thẩm định Định danh Điện tử eKYC (`/customers/kyc`):
Phân hệ tiếp nhận và xử lý hồ sơ định danh điện tử của khách hàng bao gồm hình ảnh CMND/CCCD mặt trước, mặt sau và ảnh chân dung đối soát khuôn mặt:

![Giao diện Thẩm định hồ sơ định danh điện tử eKYC (/customers/kyc)](docs/figures/hinh_3_14_customers_kyc.png)
<p align="center"><b>Hình 3.14: Giao diện Thẩm định hồ sơ định danh điện tử eKYC (/customers/kyc)</b></p>

*Đánh giá kiểm thử:*
- Kiểm tra tính toàn vẹn của tệp ảnh giấy tờ tùy thân, hỗ trợ zoom xem chi tiết chứng từ.
- Cung cấp hai tùy chọn xử lý: "Phê duyệt định danh" hoặc "Yêu cầu bổ sung chứng từ".

#### 7. Màn hình Quản lý Hồ sơ Chứng từ Điện tử (`/customers/documents`):
Quản lý lưu trữ hợp đồng tín dụng điện tử, biểu mẫu cam kết, và các giấy tờ thế chấp liên quan đến giao dịch ngân hàng:

![Giao diện Quản lý hồ sơ và tài liệu chứng từ khách hàng (/customers/documents)](docs/figures/hinh_3_15_customers_documents.png)
<p align="center"><b>Hình 3.15: Giao diện Quản lý hồ sơ và tài liệu chứng từ khách hàng (/customers/documents)</b></p>

*Đánh giá kiểm thử:*
- Kiểm tra tính năng tải về và xem trước tài liệu PDF trực tuyến.
- Phân loại rõ ràng chứng từ theo mã khách hàng và mã hợp đồng tín dụng.

#### 8. Màn hình Cấu hình Sản phẩm Vay vốn Tín dụng (`/financial-products/loans`):
Cho phép ban điều hành thiết lập các gói vay (Vay tiêu dùng tín chấp, Vay mua nhà, Vay sản xuất kinh doanh) với hạn mức và lãi suất quy định:

![Giao diện Cấu hình danh mục sản phẩm vay vốn (/financial-products/loans)](docs/figures/hinh_3_16_loan_products.png)
<p align="center"><b>Hình 3.16: Giao diện Cấu hình danh mục sản phẩm vay vốn (/financial-products/loans)</b></p>

*Đánh giá kiểm thử:*
- Form tạo mới sản phẩm kiểm tra hợp lệ các trường: Hạn mức tối thiểu/tối đa, thời hạn vay và biên độ lãi suất.
- Cập nhật trạng thái kích hoạt/tạm dừng gói vay mà không làm gián đoạn các hợp đồng đang thực hiện.

#### 9. Màn hình Cấu hình Sản phẩm & Biểu lãi suất Tiết kiệm (`/financial-products/savings`):
Cung cấp bảng biểu cấu hình các kỳ hạn gửi tiết kiệm (Không kỳ hạn, 1 tháng, 3 tháng, 6 tháng, 12 tháng, 24 tháng) cùng tỷ lệ lãi suất phần trăm năm (%/năm):

![Giao diện Cấu hình sản phẩm và biểu lãi suất tiết kiệm (/financial-products/savings)](docs/figures/hinh_3_17_saving_products.png)
<p align="center"><b>Hình 3.17: Giao diện Cấu hình sản phẩm và biểu lãi suất tiết kiệm (/financial-products/savings)</b></p>

*Đánh giá kiểm thử:*
- Kiểm tra độ chính xác của bảng tỷ lệ lãi suất theo từng kỳ hạn.
- Đảm bảo cơ chế tự động tính lãi theo ngày chính xác tuyệt đối với thuật toán chuẩn ngân hàng.

#### 10. Màn hình Giám sát & Duyệt Giao dịch Giá trị lớn (`/transactions/large-approval`):
Hệ thống kiểm soát tuân thủ và phòng chống rửa tiền (AML), tự động giữ lại các giao dịch chuyển tiền vượt hạn mức quy định (ví dụ: trên 50.000.000 VNĐ) để cán bộ kiểm soát phê duyệt thủ công:

![Giao diện Giám sát và phê duyệt giao dịch giá trị lớn (/transactions/large-approval)](docs/figures/hinh_3_18_large_transactions.png)
<p align="center"><b>Hình 3.18: Giao diện Giám sát và phê duyệt giao dịch giá trị lớn (/transactions/large-approval)</b></p>

*Đánh giá kiểm thử:*
- Cơ chế Dual-Control (Kiểm soát kép): Giao dịch chỉ được giải tỏa số dư khi có phê duyệt từ cấp quản lý.
- Ghi nhận chi tiết thông tin tài khoản nguồn, tài khoản đích, số tiền, và thời điểm phát sinh.

#### 11. Màn hình Quản lý Cán bộ & Nhân viên Ngân hàng (`/staff`):
Quản lý danh sách nhân sự nội bộ của hệ thống ngân hàng, phân công chi nhánh, phòng ban và chức danh công tác:

![Giao diện Quản lý danh sách cán bộ nhân viên nội bộ (/staff)](docs/figures/hinh_3_19_staff.png)
<p align="center"><b>Hình 3.19: Giao diện Quản lý danh sách cán bộ nhân viên nội bộ (/staff)</b></p>

*Đánh giá kiểm thử:*
- Cho phép tạo mới tài khoản cán bộ, khóa tài khoản tạm thời khi nhân viên nghỉ phép hoặc luân chuyển.
- Tự động gán quyền truy cập tương ứng với chức vụ được bổ nhiệm.

#### 12. Màn hình Phân quyền Vai trò Hệ thống RBAC (`/system/roles`):
Bảng ma trận phân quyền dựa trên vai trò (Role-Based Access Control - RBAC), phân tách rành mạch các quyền hạn giữa Quản trị viên (`ADMIN`), Giao dịch viên (`TELLER`), và Chuyên viên kiểm soát (`COMPLIANCE`):

![Giao diện Quản lý vai trò và phân quyền hệ thống RBAC (/system/roles)](docs/figures/hinh_3_20_roles.png)
<p align="center"><b>Hình 3.20: Giao diện Quản lý vai trò và phân quyền hệ thống RBAC (/system/roles)</b></p>

*Đánh giá kiểm thử:*
- Kiểm tra ma trận checkbox phân quyền theo từng menu chức năng và từng hành vi: Xem (Read), Thêm (Create), Sửa (Update), Duyệt (Approve).
- Ngăn ngừa tình trạng leo thang đặc quyền (Privilege Escalation).

#### 13. Màn hình Nhật ký Kiểm toán An ninh Audit Trail (`/system/audit`):
Lưu trữ toàn diện và không thể chỉnh sửa mọi dấu vết thao tác của người dùng và cán bộ quản trị: địa chỉ IP, thời gian chính xác đến mili-giây, hành động thực hiện và dữ liệu trước/sau biến động:

![Giao diện Nhật ký kiểm toán an ninh hệ thống Audit Trail (/system/audit)](docs/figures/hinh_3_21_audit.png)
<p align="center"><b>Hình 3.21: Giao diện Nhật ký kiểm toán an ninh hệ thống Audit Trail (/system/audit)</b></p>

*Đánh giá kiểm thử:*
- Đảm bảo 100% các thao tác nhạy cảm (Đăng nhập, Duyệt vay, Khóa tài khoản) đều được ghi nhận vào bảng `audit_logs`.
- Hỗ trợ bộ lọc tìm kiếm theo cán bộ thực hiện và khoảng thời gian phát sinh sự kiện.

#### 14. Màn hình Thay đổi Mật khẩu Quản trị viên (`/change-password`):
Form cập nhật mật khẩu an toàn cho cán bộ quản trị, bắt buộc tuân thủ chính sách mật khẩu phức tạp:

![Giao diện Thay đổi mật khẩu tài khoản quản trị viên (/change-password)](docs/figures/hinh_3_22_change_password.png)
<p align="center"><b>Hình 3.22: Giao diện Thay đổi mật khẩu tài khoản quản trị viên (/change-password)</b></p>

*Đánh giá kiểm thử:*
- Kiểm tra tính hợp lệ của mật khẩu cũ trước khi cho phép cập nhật mật khẩu mới.
- Bắt buộc mật khẩu mới có độ dài tối thiểu 8 ký tự, bao gồm chữ hoa, chữ thường và chữ số."""

# TOC update block
TOC_FIGURES = """* **Hình 3.1:** Sơ đồ Branch Testing cho phương thức chuyển tiền `transfer()`
* **Hình 3.2:** Sơ đồ Branch Testing cho phương thức nộp đơn vay `applyLoan()`
* **Hình 3.3:** Sơ đồ Branch Testing cho phương thức phê duyệt khoản vay `approveLoan()`
* **Hình 3.4:** Sơ đồ chu trình tự động hóa kiểm thử API với Postman & Newman
* **Hình 3.4b:** Giao diện thực thi kiểm thử tự động API bằng Postman Collection Runner (17/17 Assertions Passed)
* **Hình 3.5:** Sơ đồ cấu hình kịch bản kiểm thử tải Apache JMeter (Thread Group Topology)
* **Hình 3.6:** Sơ đồ tuần tự Kiểm thử đồng thời (Concurrency) & Khóa bi quan chống Double-Spending
* **Hình 3.7:** Sơ đồ luồng chuyển trang và cấu trúc phân hệ giao diện MiniBank Admin Portal
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
* **Hình 3.22:** Giao diện Thay đổi mật khẩu tài khoản quản trị viên (/change-password)"""

# Postman image embed block to be added to Section 3.4.2
POSTMAN_RUNNER_EMBED = """![Giao diện thực thi kiểm thử tự động API bằng Postman Collection Runner](docs/figures/hinh_3_4b_postman_runner.png)
<p align="center"><b>Hình 3.4b: Giao diện thực thi kiểm thử tự động API bằng Postman Collection Runner (17/17 Assertions Passed)</b></p>
"""

for filepath in FILES:
    if not os.path.exists(filepath):
        continue
    with open(filepath, "r", encoding="utf-8") as f:
        content = f.read()

    # 1. Update TOC figures
    content = re.sub(
        r"\* \*\*Hình 3\.1:\*\*[\s\S]*?(?=\n\n---|\n\n# PHẦN|\n\n## 1\.5|\n\n## 1\.6)",
        lambda m: TOC_FIGURES,
        content
    )

    # 2. Add Hình 3.4b into Section 3.4.2 if not already present
    if "hinh_3_4b_postman_runner.png" not in content:
        content = re.sub(
            r"(#### Nhật ký thực thi thực tế từ Newman CLI[\s\S]*?```text[\s\S]*?```)",
            lambda m: m.group(1) + "\n\n" + POSTMAN_RUNNER_EMBED,
            content
        )

    # 3. Replace Section 3.7.7
    pattern = r"### 3\.7\.7 Hình ảnh trực quan và đánh giá kiểm thử giao diện các phân hệ Admin Portal[\s\S]*?(?=### 3\.7\.8)"
    content = re.sub(pattern, lambda m: FULL_UI_PAGES_MD + "\n\n", content)

    with open(filepath, "w", encoding="utf-8") as f:
        f.write(content)

    print(f"Updated {filepath} successfully!")
