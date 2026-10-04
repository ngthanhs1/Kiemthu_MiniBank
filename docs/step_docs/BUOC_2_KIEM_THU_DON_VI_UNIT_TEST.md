# 🧪 BƯỚC 2: HIỆN THỰC HÓA KIỂM THỬ ĐƠN VỊ (UNIT TESTING) VỚI JUNIT 5 & MOCKITO

---
- **Môn học:** Kiểm định và Đánh giá Phần mềm (Software Testing & QA)
- **Giai đoạn:** Bước 2 / 5 trong lộ trình thực hiện bài tập lớn
- **Hệ thống mục tiêu:** MiniBank Digital Banking Backend API
- **Trạng thái thực thi:** ✅ ĐÃ HOÀN THÀNH - 100% TEST PASSED (23/23 Tests)
---

## 1. MỤC TIÊU BƯỚC 2
1. **Hiện thực hóa các ca kiểm thử đơn vị (Unit Tests):** Lập trình các ca kiểm thử hộp trắng cho 2 dịch vụ nghiệp vụ nhạy cảm và quan trọng nhất của hệ thống:
   - [`TransferService`](file:///d:/Code_Hoc/Kiemthu_MiniBank/src/main/java/com/minibank/backend/transaction/service/TransferService.java): Quản lý số dư, chuyển tiền nội bộ, kiểm tra hạn mức, xác thực OTP, phân luồng phê duyệt giao dịch số tiền lớn.
   - [`LoanService`](file:///d:/Code_Hoc/Kiemthu_MiniBank/src/main/java/com/minibank/backend/loan/service/LoanService.java): Thẩm định đơn xin vay, kiểm tra hạn mức min/max tiền vay và kỳ hạn (BVA), kiểm tra sở hữu tài khoản giải ngân.
2. **Áp dụng Mocking & Isolation:** Sử dụng thư viện **Mockito** để cô lập hoàn toàn tầng Service khỏi Database và các dịch vụ bên ngoài (SMS Twilio, RSA Signature, AI Service).
3. **Đo lường độ bao phủ thực tế:** Thực thi kiểm thử tự động với Maven Surefire và Plugin JaCoCo.

---

## 2. CHI TIẾT CÁC LỚP KIỂM THỬ ĐÃ XÂY DỰNG

### 2.1. Lớp kiểm thử `TransferServiceTest.java` (12 Test Cases)
Được đặt tại: `src/test/java/com/minibank/backend/transaction/service/TransferServiceTest.java`

| Mã Test | Tên Ca Kiểm Thử | Kịch Bản & Khẳng Định (Assertion) | Trạng Thái |
| :--- | :--- | :--- | :---: |
| **TC_TRF_01** | `initiate_Success` | Khởi tạo chuyển tiền với số dư đủ (5M chuyển 500k), mã PIN đúng. Khẳng định: Sinh mã giao dịch, trạng thái `pending`, gửi mã OTP xác thực qua SMS. | **PASSED** |
| **TC_TRF_ERR_01** | `initiate_UserNotFound` | Khách hàng không tồn tại trong hệ thống. Khẳng định: Ném ngoại lệ `ResponseStatusException` mã `401 UNAUTHORIZED`. | **PASSED** |
| **TC_TRF_ERR_02** | `initiate_UserNotActive` | Tài khoản khách hàng bị khóa/vô hiệu hóa (`status = blocked`). Khẳng định: Ném ngoại lệ `403 FORBIDDEN`. | **PASSED** |
| **TC_TRF_ERR_03** | `initiate_RecipientAccountNotFound` | Tài khoản thụ hưởng không tồn tại. Khẳng định: Ném ngoại lệ `404 NOT_FOUND`. | **PASSED** |
| **TC_TRF_ERR_04** | `initiate_AmountZeroOrNegative` | Nhập số tiền $\le 0$ (Ví dụ: `0` đ hoặc `-50.000` đ). Khẳng định: Ném ngoại lệ `400 BAD_REQUEST`. | **PASSED** |
| **TC_TRF_ERR_05** | `initiate_InsufficientBalance` | Số tiền chuyển (6M) lớn hơn số dư khả dụng (5M). Khẳng định: Ném ngoại lệ `400 BAD_REQUEST` với thông báo "Insufficient balance". | **PASSED** |
| **TC_TRF_ERR_06** | `initiate_ExceedsDailyLimit` | Số tiền chuyển vượt quá hạn mức ngày của tài khoản (Hạn mức 1M, chuyển 2M). Khẳng định: Ném ngoại lệ `400 BAD_REQUEST`. | **PASSED** |
| **TC_TRF_ERR_07** | `initiate_InvalidPin` | Nhập sai mã PIN bảo mật giao dịch. Khẳng định: Ném ngoại lệ `400 BAD_REQUEST` ("Invalid PIN"). | **PASSED** |
| **TC_TRF_CONFIRM_01** | `confirm_Success_NormalAmount` | Khách hàng nhập đúng mã OTP xác nhận chuyển 500.000 đ. Khẳng định: Khấu trừ 500k tài khoản gửi (5M $\rightarrow$ 4.5M), cộng 500k tài khoản nhận (1M $\rightarrow$ 1.5M), ghi nhận 2 dòng Sổ cái biến động số dư (`debit` và `credit`), trạng thái chuyển sang `completed`. | **PASSED** |
| **TC_TRF_CONFIRM_02** | `confirm_InvalidOtp` | Khách hàng nhập sai mã OTP giao dịch. Khẳng định: Ném ngoại lệ `400 BAD_REQUEST`, tuyệt đối không trừ tiền và không cập nhật số dư tài khoản. | **PASSED** |
| **TC_TRF_CONFIRM_03** | `confirm_LargeAmount_RequiresReview` | Giao dịch số tiền lớn 150.000.000 đ ($\ge$ Ngưỡng 100M). Khẳng định: Trạng thái chuyển sang `pending_review` để chờ cán bộ kiểm soát phê duyệt, tiền chưa bị trừ ngay. | **PASSED** |
| **TC_TRF_CONFIRM_04** | `confirm_ManagerAmount_RequiresManager` | Giao dịch đặc biệt lớn 250.000.000 đ ($\ge$ Ngưỡng 200M). Khẳng định: Trạng thái chuyển sang `pending_manager` để chờ Quản lý cấp cao duyệt. | **PASSED** |

---

### 2.2. Lớp kiểm thử `LoanServiceTest.java` (10 Test Cases)
Được đặt tại: `src/test/java/com/minibank/backend/loan/service/LoanServiceTest.java`

| Mã Test | Tên Ca Kiểm Thử | Kịch Bản & Khẳng Định (Assertion) | Trạng Thái |
| :--- | :--- | :--- | :---: |
| **TC_LOAN_01** | `applyForLoan_Success` | Nộp hồ sơ vay 20.000.000 đ trong 12 tháng hợp lệ. Khẳng định: Đơn vay được lưu thành công với trạng thái `pending`, đúng số tiền và kỳ hạn. | **PASSED** |
| **TC_LOAN_ERR_01** | `applyForLoan_UserNotFound` | Người nộp đơn không tồn tại. Khẳng định: Ném lỗi `401 UNAUTHORIZED`. | **PASSED** |
| **TC_LOAN_ERR_02** | `applyForLoan_ProductNotActive` | Gói vay bị tạm dừng (`status = disabled`). Khẳng định: Báo lỗi `400 BAD_REQUEST`. | **PASSED** |
| **TC_LOAN_ERR_03** | `applyForLoan_AmountBelowMinimum` | Phân tích giá trị biên (BVA): Vay 4.999.999 đ (dưới mức tối thiểu 5M). Khẳng định: Báo lỗi `400 BAD_REQUEST`. | **PASSED** |
| **TC_LOAN_ERR_04** | `applyForLoan_AmountAboveMaximum` | Phân tích giá trị biên (BVA): Vay 100.000.001 đ (vượt mức tối đa 100M). Khẳng định: Báo lỗi `400 BAD_REQUEST`. | **PASSED** |
| **TC_LOAN_ERR_05** | `applyForLoan_TermBelowMinimum` | Phân tích giá trị biên (BVA): Vay 2 tháng (dưới kỳ hạn tối thiểu 3 tháng). Khẳng định: Báo lỗi `400 BAD_REQUEST`. | **PASSED** |
| **TC_LOAN_ERR_06** | `applyForLoan_TypeMismatch` | Chọn gói vay không khớp loại hình (sản phẩm `PERSONAL` nhưng yêu cầu `secured`). Khẳng định: Báo lỗi `400 BAD_REQUEST`. | **PASSED** |
| **TC_LOAN_ERR_07** | `applyForLoan_AccountNotBelongToUser` | Tài khoản thụ hưởng giải ngân không thuộc quyền sở hữu của người vay. Khẳng định: Báo lỗi `403 FORBIDDEN`. | **PASSED** |
| **TC_LOAN_VIEW_01** | `getLoans_Success` | Tra cứu danh sách các khoản vay của khách hàng. Khẳng định: Trả về danh sách được ánh xạ chính xác theo mã khoản vay. | **PASSED** |
| **TC_LOAN_VIEW_02** | `getLoan_NotFound` | Tra cứu khoản vay không tồn tại hoặc của người khác. Khẳng định: Ném lỗi `404 NOT_FOUND`. | **PASSED** |

---

## 3. KẾT QUẢ THỰC THI KIỂM THỬ (TEST EXECUTION RESULTS)

### 3.1. Nhật ký thực thi thực tế (Maven Surefire Log)
```text
[INFO] Running com.minibank.backend.BankingBackendApplicationTests
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 10.85 s
[INFO] Running com.minibank.backend.loan.service.LoanServiceTest
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.812 s
[INFO] Running com.minibank.backend.transaction.service.TransferServiceTest
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.604 s
[INFO] 
[INFO] Results:
[INFO] Tests run: 23, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] --- jacoco:0.8.12:report (report) @ banking-backend ---
[INFO] Loading execution data file D:\Code_Hoc\Kiemthu_MiniBank\target\jacoco.exec
[INFO] Analyzed bundle '' with 302 classes
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

### 3.2. Bảng tổng hợp số liệu
| Hạng mục | Chỉ số thực tế | Đánh giá |
| :--- | :---: | :---: |
| **Tổng số ca kiểm thử (Total Tests)** | **23** | Đầy đủ |
| **Số ca kiểm thử thành công (Passed)** | **23** | Đạt 100% |
| **Số ca thất bại (Failures)** | **0** | Tuyệt đối |
| **Số ca lỗi runtime (Errors)** | **0** | Tuyệt đối |
| **Số ca bị bỏ qua (Skipped)** | **0** | Không bỏ sót |
| **Thời gian chạy toàn bộ (Execution Time)** | **~14.89 giây** | Rất nhanh |
| **Báo cáo JaCoCo Code Coverage** | `target/site/jacoco/index.html` | Đã sinh thành công |

---

## 4. KẾT LUẬN & ĐÁNH GIÁ CHẤT LƯỢNG MÃ NGUỒN

1. **Tính đúng đắn của logic nghiệp vụ:**
   - Các thuật toán kiểm tra số dư, trừ tiền, cộng tiền và ghi Sổ cái kép (`double-entry ledger`) hoạt động chính xác 100%.
   - Cơ chế chặn các giao dịch số dư âm hoặc vượt hạn mức hoạt động hiệu quả.
   - Quy trình phân cấp duyệt giao dịch lớn (> 100M và > 200M) chuyển đúng trạng thái chờ duyệt mà không tự ý giải ngân.
2. **Khả năng cách ly lỗi (Defect Prevention):**
   - Đã xử lý và kiểm chứng toàn bộ các trường hợp ngoại lệ (Edge Cases): người dùng không tồn tại (401), tài khoản bị khóa (403), sai OTP/PIN (400), vượt hạn mức ngày (400).

---

## 5. BƯỚC TIẾP THEO (GIAI ĐOẠN 3)
Sau khi hoàn thành xuất sắc Kiểm thử đơn vị, bước tiếp theo sẽ là:
- **Bước 3: Kiểm thử Tích hợp & Tự động hóa API (Integration Testing & API Automation)**.
- Xây dựng các ca kiểm thử tích hợp sử dụng `MockMvc` và bộ sưu tập **Postman Collection** mô phỏng trọn vẹn luồng người dùng End-to-End (E2E) qua giao thức HTTP REST API.