# -*- coding: utf-8 -*-
"""
Update report files with Group 6 info and the real pie chart image & table in Section 4.1:
- Nhóm 6: Nguyễn Văn Thành (23010191) & Hà Xuân Đại (23010494)
- Section 4.1: Embed hinh_4_1_pie_chart.png and add the breakdown table
"""

import os
import re

FILES = [
    r"d:\Code_Hoc\Kiemthu_MiniBank\BAO_CAO_KIEM_DINH_CHAT_LUONG_PHAN_MEM_MINIBANK.md",
    r"d:\Code_Hoc\Kiemthu_MiniBank\docs\final_report\BAO_CAO_KIEM_DINH_CHAT_LUONG_PHAN_MEM_MINIBANK.md"
]

PIE_CHART_SECTION = """![Biểu đồ phân bổ tỷ lệ kết quả kiểm thử toàn diện hệ thống MiniBank (57 ca Passed 100%)](docs/figures/hinh_4_1_pie_chart.png)
<p align="center"><b>Hình 4.1: Biểu đồ phân bổ tỷ lệ kết quả kiểm thử toàn diện hệ thống MiniBank (57 ca Passed 100%)</b></p>

#### Bảng tổng hợp phân bổ 57 ca kiểm thử theo loại hình kiểm thử:

<p align="center"><b>Bảng 4.0: Bảng tổng hợp phân bổ ca kiểm thử tự động hóa hệ thống MiniBank</b></p>

| Loại kiểm thử | Số ca thực thi | Tỷ lệ (%) | Trạng thái thực nghiệm |
| :--- | :---: | :---: | :---: |
| **Unit Tests (Spring Boot)** | 20 | 35.09% | **100% Passed (20/20)** |
| **Integration Tests (MockMvc)** | 11 | 19.30% | **100% Passed (11/11)** |
| **Postman API Automation** | 10 | 17.54% | **100% Passed (10/10)** |
| **Selenium UI Full Pages** | 16 | 28.07% | **100% Passed (16/16)** |
| **TỔNG HỢP TOÀN BỘ HỆ THỐNG** | **57** | **100.0%** | **100% Passed (57/57)** |
"""

for fp in FILES:
    if not os.path.exists(fp):
        continue
    with open(fp, "r", encoding="utf-8") as f:
        text = f.read()

    # 1. Update Group Info in Front matter
    text = text.replace(
        "* **Nhóm sinh viên thực hiện:** Nhóm Kiểm thử Phần mềm Chuyên sâu",
        "* **Nhóm sinh viên thực hiện:** Nhóm 6: Nguyễn Văn Thành (MSV: 23010191) & Hà Xuân Đại (MSV: 23010494)"
    )

    # 2. Update Section 4.1 pie chart
    pattern_chart = r"```mermaid\s+pie title Tỷ lệ kết quả thực thi[\s\S]*?<p align=\"center\"><b>Hình 4\.1:[\s\S]*?</b></p>"
    if re.search(pattern_chart, text):
        text = re.sub(pattern_chart, lambda m: PIE_CHART_SECTION, text)
        print(f"Replaced Mermaid Pie with Real Chart Image in {fp}")
    else:
        print(f"Mermaid pie pattern not found in {fp}")

    with open(fp, "w", encoding="utf-8") as f:
        f.write(text)

    print(f"Updated {fp} successfully!")
