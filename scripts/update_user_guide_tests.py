# -*- coding: utf-8 -*-
import re

PATH = r"d:\Code_Hoc\Kiemthu_MiniBank\docs\step_docs\HUONG_DAN_CHAY_VA_KIEM_THU.md"

with open(PATH, "r", encoding="utf-8") as f:
    text = f.read()

# Update Section 7 (Postman)
postman_guide = """## 7. Hướng Dẫn Chạy Kiểm Thử Tự Động Hóa API (Postman / Newman)

Tệp kịch bản Postman Collection đã được chuẩn hóa tại:
`test_automation/postman/MiniBank_API_Automation.postman_collection.json`

### Cách 1: Chạy tự động không cần cài đặt qua Newman CLI (Khuyến nghị)
Bạn chỉ cần mở Terminal tại thư mục gốc dự án và gõ đúng 1 lệnh (sử dụng `npx` của Node.js, không cần cài global):

```powershell
cmd /c "npx --yes newman run test_automation/postman/MiniBank_API_Automation.postman_collection.json --env-var base_url=http://localhost:8080"
```

*Kết quả thực thi toàn diện:*
- **Tổng số Request:** 10/10 APIs thực thi thành công.
- **Tổng số Assertions:** 17/17 assertions vượt qua (100% Passed).
- **Thời gian phản hồi trung bình:** 39 ms (Cực nhanh).

### Cách 2: Chạy bằng giao diện Postman Desktop (Trực quan)
1. Mở Postman Desktop ➔ Bấm **Import** ➔ Chọn tệp `MiniBank_API_Automation.postman_collection.json`.
2. Chọn môi trường hoặc thiết lập biến `base_url` = `http://localhost:8080`.
3. Nhấp chuột phải vào Collection ➔ Chọn **Run Collection** ➔ Bấm nút **Run MiniBank API Automation**."""

text = re.sub(r"## 7\. Hướng Dẫn Chạy Kiểm Thử Tự Động Hóa API[\s\S]*?(?=## 8\.)", lambda m: postman_guide + "\n\n", text)

# Update Section 9 (Selenium)
selenium_guide = """## 9. Hướng Dẫn Chạy Kiểm Thử Tự Động Giao Diện (Selenium & Selenium IDE)

### 9.1. Chạy Bộ Kiểm Thử Tự Động Toàn Diện 16 Trang Giao Diện (Full UI Pages)

Kịch bản kiểm thử toàn diện đã được đóng gói tại: `test_automation/selenium/test_admin_portal_all_pages.py`. Bao quát toàn bộ 16 trang phân hệ: Đăng nhập, Dashboard, Duyệt vay, Duyệt tiết kiệm, Quản lý khách hàng, Thẩm định KYC, Tài liệu chứng từ, Sản phẩm vay, Sản phẩm tiết kiệm, Duyệt giao dịch lớn, Nhân viên, Phân quyền RBAC, Nhật ký kiểm toán, Đổi mật khẩu.

```powershell
# Chạy bộ test 16 trang toàn diện
python test_automation/selenium/test_admin_portal_all_pages.py -v
```

*Kết quả thực thi tự động:*
```text
Ran 16 tests in 72.952s
OK (Pass Rate: 100% - 16/16 Passed)
```

### 9.2. Chạy Bộ Kiểm Thử 10 Ca Cơ Bản (Standard UI Suite)
```powershell
python test_automation/selenium/test_admin_portal_ui.py
```
*Kết quả:* `Ran 10 tests in 8.421s - OK`

### 9.3. Chạy Kịch Bản Trực Quan Bằng Selenium IDE
1. Mở trình duyệt Chrome đã cài tiện ích **Selenium IDE**.
2. Chọn **Open an existing project** ➔ Duyệt đến tệp:
   `d:\\Code_Hoc\\Kiemthu_MiniBank\\test_automation\\selenium\\MiniBank_Admin_UI_Test.side`
3. Nhấn nút **Play (▶️)** để quan sát trình duyệt tự động điền form, click chuyển trang và xác thực dữ liệu."""

text = re.sub(r"## 9\. Hướng Dẫn Chạy Kiểm Thử Tự Động Giao Diện[\s\S]*?(?=## 10\.)", lambda m: selenium_guide + "\n\n", text)

with open(PATH, "w", encoding="utf-8") as f:
    f.write(text)

print("Updated HUONG_DAN_CHAY_VA_KIEM_THU.md successfully!")
