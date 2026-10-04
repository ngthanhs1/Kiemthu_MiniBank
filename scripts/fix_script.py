# -*- coding: utf-8 -*-
with open(r'd:\Code_Hoc\Kiemthu_MiniBank\generate_perfect_report.py', 'r', encoding='utf-8') as f:
    c = f.read()

# Fix Figure order in Part III
# In 3.4 API automation -> Hình 3.4
c = c.replace('Hình 3.6: Sơ đồ chu trình tự động hóa kiểm thử API với Postman & Newman', 'Hình 3.4: Sơ đồ chu trình tự động hóa kiểm thử API với Postman & Newman')
# In 3.6 Concurrency -> Hình 3.6
c = c.replace('Hình 3.4: Sơ đồ tuần tự Kiểm thử đồng thời (Concurrency) & Khóa bi quan chống Double-Spending', 'Hình 3.6: Sơ đồ tuần tự Kiểm thử đồng thời (Concurrency) & Khóa bi quan chống Double-Spending')

# Fix Table 1.1 to 1.8 headers
uc_names = [
    (1, "UC01 - Đăng ký tài khoản người dùng mới (Register)"),
    (2, "UC02 - Đăng nhập hệ thống & Cấp phát JWT Token (Login)"),
    (3, "UC03 - Chuyển tiền nội bộ (Internal Transfer)"),
    (4, "UC04 - Mở sổ tiết kiệm trực tuyến (Open Savings Book)"),
    (5, "UC05 - Tất toán sổ tiết kiệm (Settle Savings Book)"),
    (6, "UC06 - Nộp đơn xin cấp hạn mức vay vốn (Apply Loan)"),
    (7, "UC07 - Phê duyệt hoặc Từ chối khoản vay (Approve/Reject Loan)"),
    (8, "UC08 - Xem sao kê và tra cứu lịch sử giao dịch (View Transactions)")
]

for idx, name in uc_names:
    old_h = f"### Bảng 1.{idx}: Đặc tả {name.split(' - ')[0]}"
    # find lines starting with this
    for line in c.splitlines():
        if f"Bảng 1.{idx}: Đặc tả UC0{idx}" in line:
            new_line = f'<p align="center"><b>Bảng 1.{idx}: Đặc tả Use Case {name}</b></p>'
            c = c.replace(line, new_line)

# Fix Table 3.2 caption
c = c.replace('<b>Bảng 3.2: Bảng phân tích chi tiết lỗi phát hiện và giải pháp sửa mã nguồn</b>', '<b>Bảng 3.2: Bảng phân tích chi tiết lỗi phát hiện và giải pháp sửa mã nguồn (BUG-01 đến BUG-06)</b>')

# Fix TOC Danh muc Hinh anh
toc_old = """* **Hình 3.4:** Sơ đồ tuần tự Kiểm thử đồng thời (Concurrency) & Khóa bi quan chống Double-Spending
* **Hình 3.5:** Sơ đồ cấu hình kịch bản kiểm thử tải Apache JMeter (Thread Group Topology)
* **Hình 3.6:** Sơ đồ chu trình tự động hóa kiểm thử API với Postman & Newman"""

toc_new = """* **Hình 3.4:** Sơ đồ chu trình tự động hóa kiểm thử API với Postman & Newman
* **Hình 3.5:** Sơ đồ cấu hình kịch bản kiểm thử tải Apache JMeter (Thread Group Topology)
* **Hình 3.6:** Sơ đồ tuần tự Kiểm thử đồng thời (Concurrency) & Khóa bi quan chống Double-Spending"""

c = c.replace(toc_old, toc_new)

with open(r'd:\Code_Hoc\Kiemthu_MiniBank\generate_perfect_report.py', 'w', encoding='utf-8') as f:
    f.write(c)

print('Updated generate_perfect_report.py successfully')
