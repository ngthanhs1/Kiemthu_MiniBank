# -*- coding: utf-8 -*-
import os
import sys
import time
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.chrome.options import Options

if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding='utf-8')
    except Exception:
        pass

BASE_URL = "http://localhost:3000"
DIRS = [
    r"d:\Code_Hoc\Kiemthu_MiniBank\docs\figures",
    r"d:\Code_Hoc\Kiemthu_MiniBank\docs\figues"
]

opts = Options()
opts.add_argument("--headless=new")
opts.add_argument("--window-size=1920,1080")
opts.add_argument("--disable-gpu")
opts.add_argument("--no-sandbox")

driver = webdriver.Chrome(options=opts)
driver.implicitly_wait(5)

def save_to_both(filename):
    for d in DIRS:
        target = os.path.join(d, filename)
        driver.save_screenshot(target)
        print(f"Saved: {target} ({os.path.getsize(target)} bytes)")

try:
    # 1. 404 Not Found Page
    print("Capturing 404 Page...")
    driver.get(f"{BASE_URL}/trang-khong-ton-tai-404")
    time.sleep(2)
    save_to_both("hinh_3_23_404_not_found.png")

    # 2. Login as admin
    print("Logging in...")
    driver.get(f"{BASE_URL}/login")
    time.sleep(1)
    inputs = driver.find_elements(By.TAG_NAME, "input")
    if len(inputs) >= 2:
        inputs[0].clear()
        inputs[0].send_keys("admin")
        inputs[1].clear()
        inputs[1].send_keys("admin123")
        driver.find_element(By.CSS_SELECTOR, "button[type='submit']").click()
        time.sleep(2)

    # 3. Logout action
    print("Logging out...")
    driver.get(f"{BASE_URL}/logout")
    time.sleep(2)
    save_to_both("hinh_3_24_logout.png")

    print("Extra screenshots captured successfully!")

finally:
    driver.quit()
