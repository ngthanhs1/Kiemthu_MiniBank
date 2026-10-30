# -*- coding: utf-8 -*-
import os
import time
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.chrome.options import Options

BASE_URL = "http://localhost:3000"
FIG_DIR = r"d:\Code_Hoc\Kiemthu_MiniBank\docs\figures"
os.makedirs(FIG_DIR, exist_ok=True)

opts = Options()
opts.add_argument("--headless=new")
opts.add_argument("--window-size=1920,1080")
opts.add_argument("--disable-gpu")
opts.add_argument("--no-sandbox")

driver = webdriver.Chrome(options=opts)
driver.implicitly_wait(5)

try:
    # 1. Login Page Screenshot
    print("Capturing 1: Login Page...")
    driver.get(f"{BASE_URL}/login")
    time.sleep(2)
    p_login = os.path.join(FIG_DIR, "hinh_3_8_login.png")
    driver.save_screenshot(p_login)
    print(f"Saved: {p_login}")

    # 2. Perform Login
    print("Performing Login...")
    inputs = driver.find_elements(By.TAG_NAME, "input")
    if len(inputs) >= 2:
        inputs[0].clear()
        inputs[0].send_keys("admin")
        inputs[1].clear()
        inputs[1].send_keys("admin123")
        btn = driver.find_element(By.CSS_SELECTOR, "button[type='submit']")
        btn.click()
        time.sleep(3)
        print("Login clicked, current URL:", driver.current_url)

    # 3. Dashboard Screenshot
    print("Capturing 2: Dashboard...")
    p_dash = os.path.join(FIG_DIR, "hinh_3_9_dashboard.png")
    driver.save_screenshot(p_dash)
    print(f"Saved: {p_dash}")

    # 4. Loan Approval Screenshot
    print("Capturing 3: Loan Approval...")
    driver.get(f"{BASE_URL}/requests/loan-approval")
    time.sleep(2)
    p_loan = os.path.join(FIG_DIR, "hinh_3_10_loan_approval.png")
    driver.save_screenshot(p_loan)
    print(f"Saved: {p_loan}")

    # 5. Saving Approval Screenshot
    print("Capturing 4: Saving Approval...")
    driver.get(f"{BASE_URL}/requests/saving-approval")
    time.sleep(2)
    p_saving = os.path.join(FIG_DIR, "hinh_3_11_saving_approval.png")
    driver.save_screenshot(p_saving)
    print(f"Saved: {p_saving}")

    print("ALL REAL SCREENSHOTS CAPTURED!")

finally:
    driver.quit()
