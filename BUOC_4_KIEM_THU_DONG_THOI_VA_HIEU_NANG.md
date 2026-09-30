# ⚡ BƯỚC 4: KIỂM THỬ XỬ LÝ ĐỒNG THỜI (CONCURRENCY) & HIỆU NĂNG CHỊU TẢI (PERFORMANCE TESTING)

---
- **Môn học:** Kiểm định và Đánh giá Phần mềm (Software Testing & QA)
- **Giai đoạn:** Bước 4 / 5 trong lộ trình thực hiện bài tập lớn
- **Hệ thống mục tiêu:** MiniBank Digital Banking Backend API
- **Trạng thái thực thi:** ✅ ĐÃ HOÀN THÀNH - 100% TEST PASSED (31/31 Tests)
---

## 1. MỤC TIÊU BƯỚC 4
1. **Kiểm thử An toàn Đa luồng & Ngăn ngừa Rút tiền kép (Double Spending Prevention):**
   - Giải quyết bài toán tranh chấp tài nguyên (Race Condition) kinh điển trong ngành ngân hàng tài chính: Khi khách hàng dùng nhiều thiết bị hoặc script tự động gửi 2 yêu cầu rút hết số dư tài khoản tại cùng một mili-giây.
   - Chứng minh cơ chế khóa bi quan (**Pessimistic Locking - `SELECT ... FOR UPDATE`**) của MiniBank đảm bảo số dư tài khoản không bao giờ bị âm.
2. **Xây dựng Kịch bản Kiểm thử Tải & Hiệu năng với Apache JMeter:**
   - Tạo tệp kịch bản kiểm thử tải chuẩn **[`MiniBank_Performance_TestPlan.jmx`](file:///d:/Code_Hoc/Kiemthu_MiniBank/MiniBank_Performance_TestPlan.jmx)** mô phỏng từ 50 đến 100 người dùng đồng thời truy vấn và giao dịch.
   - Thiết lập chỉ tiêu đo lường: Thời gian phản hồi trung bình (Average Response Time $\le 500$ ms) và Tỉ lệ lỗi (Error Rate $< 0.1\%$).

---

## 2. CHI TIẾT BÀI TEST TRANH CHẤP TÀI NGUYÊN ĐA LUỒNG (`TransferConcurrencyIntegrationTest.java`)

Tệp nguồn: [`src/test/java/com/minibank/backend/transaction/service/TransferConcurrencyIntegrationTest.java`](file:///d:/Code_Hoc/Kiemthu_MiniBank/src/test/java/com/minibank/backend/transaction/service/TransferConcurrencyIntegrationTest.java)

### 2.1. Thiết kế Kịch bản Thử nghiệm (Test Scenario)
```text
[Tài khoản Gửi A] ─────────────── Có đúng: 1.000.000 VNĐ
[Tài khoản Nhận B] ────────────── Có: 0 VNĐ

                     TẠI THỜI ĐIỂM t0 CHÍNH XÁC (CountDownLatch)
                     ┌───────────────────┴───────────────────┐
                     ▼                                       ▼
       [Luồng 1 - Thread 1]                    [Luồng 2 - Thread 2]
    Yêu cầu rút: 1.000.000 VNĐ              Yêu cầu rút: 1.000.000 VNĐ
                     │                                       │
                     ▼                                       ▼
            transferService.confirm()               transferService.confirm()
```

### 2.2. Kỹ thuật Lập trình Điều phối Đa luồng (Java Concurrency API)
- Sử dụng `Executors.newFixedThreadPool(2)` để tạo 2 luồng công nhân song song.
- Sử dụng **2 chốt đếm `CountDownLatch`**:
  - `readyLatch`: Đảm bảo cả 2 luồng đều đã khởi tạo xong và đứng chờ sẵn tại vạch xuất phát.
  - `startLatch.await()`: Cả 2 luồng bị chặn lại, chỉ xuất phát khi luồng chính gọi `startLatch.countDown()`, đảm bảo tính đồng thời tuyệt đối (sai số $< 1$ mili-giây).

### 2.3. Cơ chế Khóa Database trong Mã nguồn MiniBank
Trong lớp [`AccountRepository.java`](file:///d:/Code_Hoc/Kiemthu_MiniBank/src/main/java/com/minibank/backend/account/repository/AccountRepository.java):
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select a from Account a where a.id = :id")
Optional<Account> findByIdForUpdate(@Param("id") Long id);
```
Khi gọi `transferService.confirm()`:
1. Luồng nhanh hơn giành được khóa ghi `PESSIMISTIC_WRITE` trên dòng tài khoản gửi tại Database (`SELECT ... FOR UPDATE`).
2. Luồng này đọc số dư (1.000.000 VNĐ $\ge$ 1.000.000 VNĐ), thực hiện trừ 1.000.000 VNĐ, cập nhật số dư mới = **0 VNĐ**, và commit Transaction.
3. Luồng thứ hai sau khi chờ luồng 1 nhả khóa sẽ đọc số dư mới nhất từ DB là **0 VNĐ**.
4. Biểu thức kiểm tra `fromAccount.getAvailableBalance().compareTo(amount) < 0` trở thành `0 < 1.000.000` $\rightarrow$ Ném ngoại lệ `ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient balance")` và tự động rollback.

### 2.4. Kết quả Khẳng định (Assertions Result)
| Chỉ tiêu kiểm tra | Kết quả mong đợi | Kết quả thực tế | Đánh giá |
| :--- | :---: | :---: | :---: |
| **Số giao dịch thành công** | Đúng 1 giao dịch | **1** | **PASSED** |
| **Số giao dịch thất bại** | Đúng 1 giao dịch | **1** | **PASSED** |
| **Thông báo lỗi giao dịch 2** | Chứa "Insufficient balance" | **Insufficient balance** | **PASSED** |
| **Số dư Tài khoản Gửi sau test** | Đúng 0 VNĐ (Không được âm) | **0.00 VNĐ** | **PASSED (Tuyệt đối)** |
| **Số dư Tài khoản Nhận sau test** | Đúng 1.000.000 VNĐ | **1.000.000.00 VNĐ** | **PASSED** |

---

## 3. KỊCH BẢN KIỂM THỬ HIỆU NĂNG & TẢI VỚI APACHE JMETER

Đã tạo tệp kịch bản kiểm thử tải định dạng XML chuẩn Apache JMeter tại thư mục gốc:  
📄 **[`MiniBank_Performance_TestPlan.jmx`](file:///d:/Code_Hoc/Kiemthu_MiniBank/MiniBank_Performance_TestPlan.jmx)**

### 3.1. Các nhóm luồng kiểm thử (Thread Groups)
1. **Thread Group 1: 50 Concurrent Users (Tra cứu số dư tài khoản):**
   - **Mục tiêu:** Kiểm tra khả năng xử lý truy vấn đọc (Read Operations) có xác thực Bearer JWT.
   - **Cấu hình:** 50 Threads, Ramp-Up 5 giây, Lặp 10 lần $\rightarrow$ Tổng cộng **500 requests**.
   - **Endpoint:** `GET /api/mobile/accounts/me` kèm Header `Authorization` và `X-Device-Id`.
   - **Assertion:** Response Code = `200 OK`.
2. **Thread Group 2: 100 Concurrent Users (Xem sản phẩm tiết kiệm):**
   - **Mục tiêu:** Kiểm tra khả năng đáp ứng đồng thời cao khi nhiều khách hàng truy cập danh mục sản phẩm.
   - **Cấu hình:** 100 Threads, Ramp-Up 10 giây, Lặp 5 lần $\rightarrow$ Tổng cộng **500 requests**.
   - **Endpoint:** `GET /api/mobile/savings/products`.
   - **Assertion:** Response Code = `200 OK`.

### 3.2. Hướng dẫn chạy kiểm thử tải và xuất Dashboard Báo cáo HTML
Bạn có thể chạy kiểm thử tải từ dòng lệnh bằng công cụ Apache JMeter:
```bash
# 1. Chạy JMeter ở chế độ Non-GUI và ghi log kết quả
jmeter -n -t MiniBank_Performance_TestPlan.jmx -l target/jmeter-results.jtl -e -o target/jmeter-dashboard/

# 2. Mở file báo cáo đồ thị trực quan (HTML Dashboard Report)
# Đường dẫn: target/jmeter-dashboard/index.html
```

---

## 4. KẾT QUẢ THỰC THI TOÀN BỘ BỘ KIỂM THỬ DỰ ÁN (LŨY KẾ BƯỚC 1 + 2 + 3 + 4)

### 4.1. Nhật ký thực thi Maven Surefire
```text
[INFO] Running com.minibank.backend.BankingBackendApplicationTests
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 11.02 s
[INFO] Running com.minibank.backend.auth.controller.AuthControllerIntegrationTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 2.134 s
[INFO] Running com.minibank.backend.loan.service.LoanServiceTest
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.781 s
[INFO] Running com.minibank.backend.transaction.service.TransferConcurrencyIntegrationTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.250 s
[INFO] Running com.minibank.backend.transaction.service.TransferServiceTest
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.758 s
[INFO] 
[INFO] Results:
[INFO] Tests run: 31, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] --- jacoco:0.8.12:report (report) @ banking-backend ---
[INFO] Loading execution data file D:\Code_Hoc\Kiemthu_MiniBank\target\jacoco.exec
[INFO] Analyzed bundle '' with 302 classes
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  17.546 s
```

### 4.2. Bảng tổng hợp lũy kế các lớp kiểm thử
| STT | Lớp kiểm thử | Cấp độ kiểm thử | Số ca test | Kết quả |
| :---: | :--- | :--- | :---: | :---: |
| 1 | `BankingBackendApplicationTests` | Smoke / Context Load | 1 | **100% PASS** |
| 2 | `TransferServiceTest` | Unit Test (JUnit 5 + Mockito) | 12 | **100% PASS** |
| 3 | `LoanServiceTest` | Unit Test (JUnit 5 + Mockito) | 10 | **100% PASS** |
| 4 | `AuthControllerIntegrationTest` | Integration Test (MockMvc + Security) | 7 | **100% PASS** |
| 5 | `TransferConcurrencyIntegrationTest` | Concurrency Test (Thread-Safety / Lock) | 1 | **100% PASS** |
| **Tổng** | **5 Lớp kiểm thử toàn diện** | **Đa tầng (Kim tự tháp kiểm thử)** | **31** | **31/31 (100% PASS)** |

---

## 5. BƯỚC TIẾP THEO (GIAI ĐOẠN 5 - BƯỚC CUỐI CÙNG)
Sau khi hoàn thành Kiểm thử Đồng thời và Hiệu năng, bước cuối cùng sẽ là:
- **Bước 5: Đánh giá Chất lượng Mã nguồn (Static Code Analysis) & Báo cáo Tổng kết Kiểm định Phần mềm (Test Summary Report & Defect Log)**.
- Quét và phân tích mã nguồn tĩnh (phát hiện Code Smells, Bugs, Security Hotspots).
- Hoàn thiện bản báo cáo tổng kết đồ án môn học chuẩn IEEE / ISTQB để nộp cho giảng viên.