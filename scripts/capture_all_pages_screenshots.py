# -*- coding: utf-8 -*-
import os
import sys
import time

if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding='utf-8')
    except Exception:
        pass

from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.chrome.options import Options

BASE_URL = "http://localhost:3000"
DIRS = [
    r"d:\Code_Hoc\Kiemthu_MiniBank\docs\figures",
    r"d:\Code_Hoc\Kiemthu_MiniBank\docs\figues"
]
for d in DIRS:
    os.makedirs(d, exist_ok=True)

opts = Options()
opts.add_argument("--headless=new")
opts.add_argument("--window-size=1920,1080")
opts.add_argument("--disable-gpu")
opts.add_argument("--no-sandbox")

driver = webdriver.Chrome(options=opts)
driver.implicitly_wait(5)

pages = [
    ("hinh_3_8_login.png", "/login", "Trang Đăng nhập Quản trị", False),
    ("hinh_3_9_dashboard.png", "/", "Bảng điều khiển Tổng quan (Dashboard)", True),
    ("hinh_3_10_loan_approval.png", "/requests/loan-approval", "Phê duyệt hồ sơ vay", True),
    ("hinh_3_11_saving_approval.png", "/requests/saving-approval", "Phê duyệt sổ tiết kiệm", True),
    ("hinh_3_13_customers.png", "/customers", "Danh sách khách hàng", True),
    ("hinh_3_14_customers_kyc.png", "/customers/kyc", "Thẩm định định danh KYC", True),
    ("hinh_3_15_customers_documents.png", "/customers/documents", "Quản lý chứng từ pháp lý", True),
    ("hinh_3_16_loan_products.png", "/financial-products/loans", "Quản lý sản phẩm vay", True),
    ("hinh_3_17_saving_products.png", "/financial-products/savings", "Quản lý sản phẩm tiết kiệm", True),
    ("hinh_3_18_large_transactions.png", "/transactions/large-approval", "Duyệt giao dịch giá trị lớn", True),
    ("hinh_3_19_staff.png", "/staff", "Quản lý nhân viên cán bộ", True),
    ("hinh_3_20_roles.png", "/system/roles", "Phân quyền vai trò RBAC", True),
    ("hinh_3_21_audit.png", "/system/audit", "Nhật ký kiểm toán hệ thống", True),
    ("hinh_3_22_change_password.png", "/change-password", "Đổi mật khẩu quản trị", True),
]

def save_to_both(filename):
    for d in DIRS:
        target = os.path.join(d, filename)
        driver.save_screenshot(target)
        print(f"Saved: {target} ({os.path.getsize(target)} bytes)")

try:
    # 1. Login Page
    print("--- 1. Capturing Login Page ---")
    driver.get(f"{BASE_URL}/login")
    time.sleep(2)
    save_to_both("hinh_3_8_login.png")

    # Perform Login
    print("--- 2. Performing Login ---")
    inputs = driver.find_elements(By.TAG_NAME, "input")
    if len(inputs) >= 2:
        inputs[0].clear()
        inputs[0].send_keys("admin")
        inputs[1].clear()
        inputs[1].send_keys("admin123")
        btn = driver.find_element(By.CSS_SELECTOR, "button[type='submit']")
        btn.click()
        time.sleep(3)
        print("Logged in, current URL:", driver.current_url)

    # Capture remaining pages
    for filename, path, desc, req_login in pages[1:]:
        print(f"--- Capturing: {desc} ({path}) ---")
        driver.get(f"{BASE_URL}{path}")
        time.sleep(2)
        save_to_both(filename)

    print("ALL 14 UI PAGES SCREENSHOTS CAPTURED SUCCESSFULLY!")

finally:
    driver.quit()
