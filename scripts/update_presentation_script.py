# -*- coding: utf-8 -*-
"""
Update presentation syllabus and script with detailed 4-part Postman demo
and exact Selenium terminal commands.
"""

path = r"d:\Code_Hoc\Kiemthu_MiniBank\docs\presentation\GIAO_TRINH_SLIDE_VA_KICH_BAN_THUYET_TRINH.md"

with open(path, "r", encoding="utf-8") as f:
    text = f.read()

# 1. Update Slide 10 Script
old_slide_10_anchor = "SLIDE 10: [CHUYỂN TIẾP DEMO 1] LIVE DEMO KIỂM THỬ TỰ ĐỘNG API POSTMAN"
old_slide_11_anchor = "SLIDE 11: KIỂM THỬ GIAO DIỆN NGƯỜI DÙNG (SELENIUM WEBDRIVER)"

idx10 = text.find(old_slide_10_anchor)
idx11 = text.find(old_slide_11_anchor)

if idx10 != -1 and idx11 != -1:
    # Find start of block before slide 10
    start_block = text.rfind("```", 0, idx10)
    end_block = text.rfind("```", idx10, idx11) + 3

    new_slide_10_block = """```
================================================================================
SLIDE 10: [CHUYỂN TIẾP DEMO 1] LIVE DEMO KIỂM THỬ TỰ ĐỘNG API POSTMAN
================================================================================
[Nội dung hiển thị trên Slide]:
- Biểu tượng LIVE DEMO: AUTOMATED API TESTING DEMONSTRATION
- Các nội dung sẽ thực hiện trình diễn:
  1. Chạy tự động trọn bộ 10 API qua Postman Collection Runner (17/17 Assertions Passed).
  2. Bắn yêu cầu đơn lẻ TC_API_ADM_01 kiểm tra xác thực & cấp JWT Bearer Token (200 OK).
  3. Bắn yêu cầu đơn lẻ TC_API_ADM_02 truy xuất số liệu KPI Dashboard thời gian thực (200 OK).
  4. Bắn yêu cầu tiêu cực TC_API_SEC_01 kiểm tra chặn truy cập nặc danh không có token (401 Unauthorized).
  (Bổ trợ: Thực thi dòng lệnh tự động siêu tốc qua Newman CLI trên Terminal).

[Kịch bản lời thoại của người thuyết trình khi thao tác Live Demo - 2.5 phút]:
(Người thuyết trình nhấn Alt + Tab chuyển sang cửa sổ Postman Desktop):

"Kính thưa Thầy/Cô và Hội đồng, sau đây nhóm em xin phép được trình diễn trực tiếp kịch bản kiểm thử tự động hóa API trên công cụ Postman:

- Bước 1 [Collection Runner - Tổng quan toàn bộ bộ test]:
  'Đầu tiên, em mở bộ sưu tập MiniBank API Automation Test Suite. Bộ test này bao gồm 10 API then chốt thuộc 4 phân nhóm: Quản trị, Xác thực Mobile, Sản phẩm ngân hàng và An toàn bảo mật.
  Em nhấp chuột vào nút 'Run collection', giữ nguyên 10 API với 17 Assertions và bấm 'Run MiniBank API Automation'.
  Như Thầy/Cô có thể quan sát trực tiếp trên màn hình, chỉ sau 1.3 giây, bộ Runner đã hoàn thành xuất sắc 10/10 requests với toàn bộ 17/17 điều kiện Assertions đều hiển thị màu xanh lá Passed! Thời gian phản hồi trung bình chỉ 38 mili-giây, không có bất kỳ lỗi nào xảy ra.'

- Bước 2 [Request TC_API_ADM_01 - Đăng nhập Quản trị & Cấp phát JWT Token]:
  'Tiếp theo, em mở chi tiết request thứ nhất: TC_API_ADM_01 - Admin Login. Đây là phương thức POST gửi tới endpoint /api/admin/auth/login với định danh là admin và mật khẩu admin123.
  Em bấm nút Send! Kết quả trả về mã HTTP 200 OK trong 118 mili-giây. Tại phần Body định dạng JSON, hệ thống trả về tokenType là Bearer cùng chuỗi accessToken được mã hóa chuẩn JWT. Đồng thời, đối tượng user trả về đúng vai trò ADMIN, SUPER_ADMIN cùng đầy đủ danh sách quyền hạn quản trị như Xem dashboard, Phê duyệt khoản vay và Cấu hình lãi suất.'

- Bước 3 [Request TC_API_ADM_02 - Truy xuất số liệu KPI Dashboard]:
  'Bước thứ 3, em kiểm tra khả năng truy xuất dữ liệu nghiệp vụ bằng request TC_API_ADM_02 - Get Dashboard Stats qua phương thức GET tới /api/admin/dashboard. Request này đã được tự động kế thừa chuỗi Bearer Token vừa đăng nhập ở bước trước.
  Em bấm Send! Máy chủ phản hồi mã HTTP 200 OK chỉ trong 48 mili-giây. Dữ liệu trả về cấu trúc JSON chứa đầy đủ các chỉ số KPI tài chính thời gian thực: số lượng khách hàng, số hồ sơ eKYC đang chờ xử lý, số lượng tài khoản hoạt động và các khoản vay đang chờ phê duyệt. Điều này chứng minh tầng Service và Database xử lý truy vấn dữ liệu hoàn toàn chính xác.'

- Bước 4 [Request TC_API_SEC_01 - Kiểm thử bảo mật tiêu cực (Negative Test)]:
  'Cuối cùng, một khía cạnh tối quan trọng trong kiểm thử ngân hàng là An toàn bảo mật. Em mở request TC_API_SEC_01 - Protected API without Token. Ở request này, em cố tình truy cập vào endpoint thông tin tài khoản cá nhân /api/mobile/accounts/me nhưng không đính kèm bất kỳ Token xác thực nào.
  Em bấm Send! Như Thầy/Cô thấy trên màn hình, máy chủ lập tức từ chối truy cập và trả về mã lỗi HTTP 401 Unauthorized chỉ trong 2 mili-giây! Bộ lọc JwtAuthenticationFilter của Spring Security đã ngay lập tức đánh chặn request nặc danh tại cổng vào, ngăn chặn hoàn toàn nguy cơ rò rỉ dữ liệu trái phép.'

- (Phần bổ trợ chuyên nghiệp nếu Thầy/Cô yêu cầu xem CI/CD):
  'Đặc biệt, để kiểm thử tự động trong quy trình tích hợp liên tục CI/CD, nhóm đã cấu hình Newman CLI. Khi gõ lệnh npx newman run ... trên Terminal, toàn bộ 10 ca kiểm thử này sẽ chạy ngầm và in bảng tổng kết tự động chỉ trong chưa đầy 1 giây!'"

(Người thuyết trình nhấn Alt + Tab quay trở lại Slide trình chiếu).
```"""

    text = text[:start_block] + new_slide_10_block + text[end_block:]

# 2. Update Slide 13 Script with exact terminal commands
old_slide_13_anchor = "SLIDE 13: [CHUYỂN TIẾP DEMO 2] LIVE DEMO KIỂM THỬ GIAO DIỆN SELENIUM"
old_slide_14_anchor = "SLIDE 14: PHÂN BỔ 57 CA KIỂM THỬ & KẾT QUẢ TỔNG HỢP (PHẦN IV)"

idx13 = text.find(old_slide_13_anchor)
idx14 = text.find(old_slide_14_anchor)

if idx13 != -1 and idx14 != -1:
    start_block_13 = text.rfind("```", 0, idx13)
    end_block_13 = text.rfind("```", idx13, idx14) + 3

    new_slide_13_block = """```
================================================================================
SLIDE 13: [CHUYỂN TIẾP DEMO 2] LIVE DEMO KIỂM THỬ GIAO DIỆN SELENIUM
================================================================================
[Nội dung hiển thị trên Slide]:
- Biểu tượng LIVE DEMO: AUTOMATED UI TESTING DEMONSTRATION
- Các nội dung sẽ thực hiện trình diễn:
  1. Chạy bộ kiểm thử tự động hóa 16 trang Selenium qua dòng lệnh Python Terminal.
  2. Mở dự án `MiniBank_Admin_UI_Test.side` trên tiện ích Selenium IDE Runner.
  3. Trình diễn khả năng tự động đăng nhập, render dữ liệu và kiểm tra thẻ DOM.

[Kịch bản lời thoại của người thuyết trình khi thao tác Live Demo - 2.5 phút]:
(Người thuyết trình mở Terminal / PowerShell):

"Kính thưa Thầy/Cô, bây giờ em xin phép thực thi trực tiếp bộ kiểm thử tự động hóa giao diện Selenium trên Terminal:

- Bước 1 [Chạy kiểm thử dòng lệnh trên Terminal]:
  'Trước khi chạy, hệ thống Frontend Admin Portal đang hoạt động tại cổng 3000 và Backend Spring Boot đang chạy tại cổng 8080.
  Tại thư mục gốc của dự án, em gõ câu lệnh:
  python test_automation/selenium/test_admin_portal_all_pages.py -v

  Ngay khi bấm Enter, trình điều khiển Chrome WebDriver tự động khởi chạy và thực thi tuần tự 16 ca kiểm thử giao diện:
  + Từ TC_PAGE_01 kiểm tra form đăng nhập và bắt lỗi sai mật khẩu,
  + Đến TC_PAGE_04 kiểm tra render 4 thẻ chỉ số tài chính trên Dashboard,
  + Tiếp tục tự động kiểm tra giao diện Thẩm định hồ sơ vay, Duyệt sổ tiết kiệm, Danh sách khách hàng, eKYC,
  + Cho tới Cấu hình sản phẩm, Quản lý cán bộ, Phân quyền RBAC và Nhật ký kiểm toán an ninh Audit Trail.
  Mỗi ca kiểm thử hoàn tất đều in ra chữ 'ok'. Sau khoảng 72 giây, toàn bộ 16 ca kiểm thử đều hoàn thành với kết quả: Ran 16 tests - OK!'

- Bước 2 [Trình diễn trực quan Record & Playback trên Selenium IDE]:
  'Tiếp theo, để Hội đồng có thể quan sát trực tiếp từng thao tác click chuột và gõ phím của bot kiểm thử, em xin mở tệp kịch bản MiniBank_Admin_UI_Test.side trên tiện ích Selenium IDE.
  Em bấm nút Run all tests: Trình duyệt tự động mở trang /login, tự động điền 'admin' và 'admin123', nhấn nút Đăng nhập, kiểm tra sự tồn tại của tiêu đề Dashboard, rồi tự động điều hướng sang trang Phê duyệt khoản vay và Đăng xuất an toàn.
  Mọi assertion assertElementPresent và assertText đều trả về màu xanh lá hoàn hảo!'\"

(Người thuyết trình nhấn Alt + Tab quay trở lại Slide trình chiếu).
```"""

    text = text[:start_block_13] + new_slide_13_block + text[end_block_13:]

with open(path, "w", encoding="utf-8") as f:
    f.write(text)

print("Updated syllabus markdown successfully!")
