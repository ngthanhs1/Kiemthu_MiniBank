# -*- coding: utf-8 -*-
"""
Script to embed the newly captured real UI screenshots (Figures 3.8 to 3.12)
into BAO_CAO_KIEM_DINH_CHAT_LUONG_PHAN_MEM_MINIBANK.md.
"""

import os
import re

MD_FILES = [
    r"d:\Code_Hoc\Kiemthu_MiniBank\BAO_CAO_KIEM_DINH_CHAT_LUONG_PHAN_MEM_MINIBANK.md",
    r"d:\Code_Hoc\Kiemthu_MiniBank\docs\final_report\BAO_CAO_KIEM_DINH_CHAT_LUONG_PHAN_MEM_MINIBANK.md",
    r"C:\Users\pc\.gemini\antigravity\brain\829d45e9-1360-42d7-95d8-c1b176e3d1d4\BAO_CAO_KIEM_DINH_CHAT_LUONG_PHAN_MEM_MINIBANK.md"
]

def update_file(path):
    if not os.path.exists(path):
        return
    with open(path, "r", encoding="utf-8") as f:
        text = f.read()

    # 1. Hình 3.8
    old_38 = re.search(r'```text\s*\+\-+\+[\s\S]*?MINIBANK ADMIN PORTAL[\s\S]*?\+\-+\+\s*```\s*<p align="center"><b>Hình 3\.8:[^<]+</b></p>', text)
    if old_38:
        new_38 = '![Giao diện Đăng nhập hệ thống MiniBank Admin Portal (/login)](docs/figures/hinh_3_8_login.png)\n<p align="center"><b>Hình 3.8: Giao diện Đăng nhập hệ thống MiniBank Admin Portal (/login)</b></p>'
        text = text[:old_38.start()] + new_38 + text[old_38.end():]
        print(f"Replaced Hình 3.8 in {os.path.basename(path)}")

    # 2. Hình 3.9
    old_39 = re.search(r'```text\s*\+\-+\+[\s\S]*?\[MiniBank Logo\]\s*Tổng quan[\s\S]*?\+\-+\+\s*```\s*<p align="center"><b>Hình 3\.9:[^<]+</b></p>', text)
    if old_39:
        new_39 = '![Bảng điều khiển quản trị trung tâm (Admin Dashboard /)](docs/figures/hinh_3_9_dashboard.png)\n<p align="center"><b>Hình 3.9: Bảng điều khiển quản trị trung tâm (Admin Dashboard /)</b></p>'
        text = text[:old_39.start()] + new_39 + text[old_39.end():]
        print(f"Replaced Hình 3.9 in {os.path.basename(path)}")

    # 3. Hình 3.10
    old_310 = re.search(r'```text\s*\+\-+\+[\s\S]*?DANH SÁCH HỒ SƠ YÊU CẦU VAY VỐN[\s\S]*?\+\-+\+\s*```\s*<p align="center"><b>Hình 3\.10:[^<]+</b></p>', text)
    if old_310:
        new_310 = '![Giao diện Thẩm định & Phê duyệt yêu cầu vay vốn tín dụng (/requests/loan-approval)](docs/figures/hinh_3_10_loan_approval.png)\n<p align="center"><b>Hình 3.10: Giao diện Thẩm định & Phê duyệt yêu cầu vay vốn tín dụng (/requests/loan-approval)</b></p>'
        text = text[:old_310.start()] + new_310 + text[old_310.end():]
        print(f"Replaced Hình 3.10 in {os.path.basename(path)}")

    # 4. Hình 3.11
    old_311 = re.search(r'```text\s*\+\-+\+[\s\S]*?DANH SÁCH YÊU CẦU MỞ SỔ TIẾT KIỆM[\s\S]*?\+\-+\+\s*```\s*<p align="center"><b>Hình 3\.11:[^<]+</b></p>', text)
    if old_311:
        new_311 = '![Giao diện Phê duyệt mở sổ tiết kiệm có kỳ hạn (/requests/saving-approval)](docs/figures/hinh_3_11_saving_approval.png)\n<p align="center"><b>Hình 3.11: Giao diện Phê duyệt mở sổ tiết kiệm có kỳ hạn (/requests/saving-approval)</b></p>'
        text = text[:old_311.start()] + new_311 + text[old_311.end():]
        print(f"Replaced Hình 3.11 in {os.path.basename(path)}")

    # 5. Hình 3.12
    old_312 = re.search(r'```text\s*\+\-+\+[\s\S]*?Selenium IDE\s*-\s*MiniBank_Admin_UI_Test\.side[\s\S]*?\+\-+\+\s*```\s*<p align="center"><b>Hình 3\.12:[^<]+</b></p>', text)
    if old_312:
        new_312 = '![Giao diện thực thi kịch bản kiểm thử trên Selenium IDE runner (MiniBank_Admin_UI_Test.side)](docs/figures/hinh_3_12_selenium_ide.png)\n<p align="center"><b>Hình 3.12: Giao diện thực thi kịch bản kiểm thử trên Selenium IDE runner (MiniBank_Admin_UI_Test.side)</b></p>'
        text = text[:old_312.start()] + new_312 + text[old_312.end():]
        print(f"Replaced Hình 3.12 in {os.path.basename(path)}")

    with open(path, "w", encoding="utf-8") as f:
        f.write(text)

for p in MD_FILES:
    update_file(p)

print("Done embedding real figures into markdown files!")
