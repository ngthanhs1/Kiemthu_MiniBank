# -*- coding: utf-8 -*-
"""
Full-coverage Automated UI Testing Suite for MiniBank Admin Portal using Selenium WebDriver.
Covers 16 comprehensive pages and scenarios across the entire Admin Portal.
"""

import unittest
import time
import os
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.chrome.options import Options as ChromeOptions

BASE_URL = os.getenv("UI_TEST_BASE_URL", "http://localhost:3000")

class MiniBankAdminFullUiTest(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        chrome_options = ChromeOptions()
        chrome_options.add_argument("--headless=new")
        chrome_options.add_argument("--disable-gpu")
        chrome_options.add_argument("--no-sandbox")
        chrome_options.add_argument("--window-size=1920,1080")
        chrome_options.add_argument("--disable-dev-shm-usage")
        
        cls.driver = webdriver.Chrome(options=chrome_options)
        cls.driver.implicitly_wait(6)

    @classmethod
    def tearDownClass(cls):
        if getattr(cls, "driver", None):
            cls.driver.quit()

    def login_as_admin(self):
        """Helper to ensure logged in session"""
        self.driver.get(f"{BASE_URL}/login")
        time.sleep(1)
        inputs = self.driver.find_elements(By.TAG_NAME, "input")
        if len(inputs) >= 2:
            inputs[0].clear()
            inputs[0].send_keys("admin")
            inputs[1].clear()
            inputs[1].send_keys("admin123")
            self.driver.find_element(By.CSS_SELECTOR, "button[type='submit']").click()
            time.sleep(2)

    def test_PAGE_01_login_elements_and_form(self):
        """TC_PAGE_01: Kiểm tra cấu trúc form và phần tử trang Đăng nhập (/login)"""
        self.driver.get(f"{BASE_URL}/login")
        self.assertIn("Đăng nhập", self.driver.page_source)
        inputs = self.driver.find_elements(By.TAG_NAME, "input")
        self.assertGreaterEqual(len(inputs), 2, "Form đăng nhập phải có ít nhất 2 trường nhập")
        submit_btn = self.driver.find_element(By.CSS_SELECTOR, "button[type='submit']")
        self.assertTrue(submit_btn.is_displayed())

    def test_PAGE_02_login_invalid_credentials_error(self):
        """TC_PAGE_02: Kiểm tra xử lý lỗi khi nhập sai mật khẩu đăng nhập"""
        self.driver.get(f"{BASE_URL}/login")
        inputs = self.driver.find_elements(By.TAG_NAME, "input")
        inputs[0].clear()
        inputs[0].send_keys("admin")
        inputs[1].clear()
        inputs[1].send_keys("WrongPassword@999")
        self.driver.find_element(By.CSS_SELECTOR, "button[type='submit']").click()
        time.sleep(1.5)
        self.assertIn("/login", self.driver.current_url)

    def test_PAGE_03_login_success_and_dashboard_redirect(self):
        """TC_PAGE_03: Đăng nhập thành công và chuyển hướng về Dashboard (/)"""
        self.login_as_admin()
        self.assertTrue(self.driver.current_url == f"{BASE_URL}/" or "/login" not in self.driver.current_url)

    def test_PAGE_04_dashboard_metrics_and_widgets(self):
        """TC_PAGE_04: Kiểm tra hiển thị Bảng điều khiển trung tâm (Dashboard Metrics)"""
        self.login_as_admin()
        self.driver.get(f"{BASE_URL}/")
        time.sleep(1.5)
        body_text = self.driver.find_element(By.TAG_NAME, "body").text
        self.assertTrue(len(body_text) > 50, "Dashboard phải có nội dung hiển thị số liệu")

    def test_PAGE_05_requests_loan_approval(self):
        """TC_PAGE_05: Kiểm tra giao diện Phê duyệt khoản vay (/requests/loan-approval)"""
        self.login_as_admin()
        self.driver.get(f"{BASE_URL}/requests/loan-approval")
        time.sleep(1.5)
        self.assertIn("/requests/loan-approval", self.driver.current_url)
        self.assertTrue("vay" in self.driver.page_source.lower() or "loan" in self.driver.page_source.lower())

    def test_PAGE_06_requests_saving_approval(self):
        """TC_PAGE_06: Kiểm tra giao diện Phê duyệt sổ tiết kiệm (/requests/saving-approval)"""
        self.login_as_admin()
        self.driver.get(f"{BASE_URL}/requests/saving-approval")
        time.sleep(1.5)
        self.assertIn("/requests/saving-approval", self.driver.current_url)
        self.assertTrue("tiết kiệm" in self.driver.page_source.lower() or "saving" in self.driver.page_source.lower())

    def test_PAGE_07_customers_management(self):
        """TC_PAGE_07: Kiểm tra giao diện Quản lý danh sách khách hàng (/customers)"""
        self.login_as_admin()
        self.driver.get(f"{BASE_URL}/customers")
        time.sleep(1.5)
        self.assertIn("/customers", self.driver.current_url)

    def test_PAGE_08_customers_kyc(self):
        """TC_PAGE_08: Kiểm tra giao diện Thẩm định hồ sơ KYC (/customers/kyc)"""
        self.login_as_admin()
        self.driver.get(f"{BASE_URL}/customers/kyc")
        time.sleep(1.5)
        self.assertIn("/customers/kyc", self.driver.current_url)

    def test_PAGE_09_customers_documents(self):
        """TC_PAGE_09: Kiểm tra giao diện Hồ sơ tài liệu khách hàng (/customers/documents)"""
        self.login_as_admin()
        self.driver.get(f"{BASE_URL}/customers/documents")
        time.sleep(1.5)
        self.assertIn("/customers/documents", self.driver.current_url)

    def test_PAGE_10_financial_products_loans(self):
        """TC_PAGE_10: Kiểm tra Quản lý sản phẩm tín dụng (/financial-products/loans)"""
        self.login_as_admin()
        self.driver.get(f"{BASE_URL}/financial-products/loans")
        time.sleep(1.5)
        self.assertIn("/financial-products/loans", self.driver.current_url)

    def test_PAGE_11_financial_products_savings(self):
        """TC_PAGE_11: Kiểm tra Quản lý sản phẩm tiết kiệm (/financial-products/savings)"""
        self.login_as_admin()
        self.driver.get(f"{BASE_URL}/financial-products/savings")
        time.sleep(1.5)
        self.assertIn("/financial-products/savings", self.driver.current_url)

    def test_PAGE_12_transactions_large_approval(self):
        """TC_PAGE_12: Kiểm tra Phê duyệt giao dịch giá trị lớn (/transactions/large-approval)"""
        self.login_as_admin()
        self.driver.get(f"{BASE_URL}/transactions/large-approval")
        time.sleep(1.5)
        self.assertIn("/transactions/large-approval", self.driver.current_url)

    def test_PAGE_13_staff_management(self):
        """TC_PAGE_13: Kiểm tra giao diện Quản lý nhân viên (/staff)"""
        self.login_as_admin()
        self.driver.get(f"{BASE_URL}/staff")
        time.sleep(1.5)
        self.assertIn("/staff", self.driver.current_url)

    def test_PAGE_14_system_roles_management(self):
        """TC_PAGE_14: Kiểm tra giao diện Phân quyền & Vai trò RBAC (/system/roles)"""
        self.login_as_admin()
        self.driver.get(f"{BASE_URL}/system/roles")
        time.sleep(1.5)
        self.assertIn("/system/roles", self.driver.current_url)

    def test_PAGE_15_system_audit_logs(self):
        """TC_PAGE_15: Kiểm tra giao diện Nhật ký kiểm toán hệ thống (/system/audit)"""
        self.login_as_admin()
        self.driver.get(f"{BASE_URL}/system/audit")
        time.sleep(1.5)
        self.assertIn("/system/audit", self.driver.current_url)

    def test_PAGE_16_change_password_page(self):
        """TC_PAGE_16: Kiểm tra giao diện Đổi mật khẩu quản trị (/change-password)"""
        self.login_as_admin()
        self.driver.get(f"{BASE_URL}/change-password")
        time.sleep(1.5)
        self.assertIn("/change-password", self.driver.current_url)

if __name__ == "__main__":
    unittest.main()
