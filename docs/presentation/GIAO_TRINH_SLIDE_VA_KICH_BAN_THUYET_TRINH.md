# GIÁO TRÌNH SLIDE & KỊCH BẢN THUYẾT TRÌNH BẢO VỆ ĐỀ TÀI
## ĐỀ TÀI: KIỂM ĐỊNH VÀ ĐÁNH GIÁ CHẤT LƯỢNG HỆ THỐNG NGÂN HÀNG SỐ TRỰC TUYẾN MINIBANK

* **Cơ quan chủ quản:** TRƯỜNG ĐẠI HỌC PHENIKAA - KHOA CÔNG NGHỆ THÔNG TIN
* **Giảng viên hướng dẫn:** TS. Trịnh Thanh Bình
* **Học phần:** Kiểm định và Đánh giá chất lượng phần mềm
* **Thời lượng thuyết trình đề xuất:** 15 – 20 phút (Thuyết trình Slide: 10 phút; Live Demo Postman & Selenium: 5 phút; Q&A: 5 phút)

---

## MỤC LỤC KỊCH BẢN THUYẾT TRÌNH

| Số Slide | Tiêu Đề Slide | Thời Lượng | Hoạt Động Trọng Tâm |
|:---:|:---|:---:|:---|
| **Slide 01** | Trang Bìa & Giới Thiệu Đề Tài | 1.0 phút | Giới thiệu thành viên, giảng viên hướng dẫn và tên đề tài |
| **Slide 02** | Đặt Vấn Đề & Mục Tiêu Đề Tài | 1.0 phút | Nhu cầu chất lượng phần mềm ngân hàng, rủi ro tài chính |
| **Slide 03** | Kiến Trúc Hệ Thống MiniBank | 1.0 phút | Mô hình phân tầng Spring Boot 3, Next.js 16, PostgreSQL / H2 |
| **Slide 04** | Ma Trận Phương Pháp & Công Cụ Kiểm Thử | 1.0 phút | Tổng quan 6 phương pháp kiểm định chuẩn quốc tế |
| **Slide 05** | Kiểm Thử Hộp Trắng & Độ Bao Phủ JaCoCo | 1.5 phút | Độ phức tạp Cyclomatic V(G), Branch Testing, Coverage 90.3% |
| **Slide 06** | Kiểm Thử Hộp Đen Nghiệp Vụ Cốt Lõi | 1.0 phút | Phân vùng tương đương (EP), Giá trị biên (BVA 6 giá trị) |
| **Slide 07** | Kiểm Thử Đồng Thời (Concurrency) | 1.5 phút | Race Condition, Double-Spending, Khóa bi quan Pessimistic Lock |
| **Slide 08** | Kiểm Thử Hiệu Năng & Tải (Apache JMeter) | 1.0 phút | 20.000 mẫu, tải 100 VU, độ trễ 54.7 ms, Error 0.00% |
| **Slide 09** | Tự Động Hóa Kiểm Thử API (Postman / Newman) | 1.0 phút | 10 API, 17 Assertions, Pass Rate 100%, độ trễ 38 ms |
| **Slide 10** | **[LIVE DEMO 1] THỰC NGHIỆM API POSTMAN & NEWMAN** | **2.5 phút** | **Chuyển màn hình Demo Postman Desktop & Newman CLI** |
| **Slide 11** | Kiểm Thử Giao Diện Người Dùng (Selenium WebDriver) | 1.0 phút | Phạm vi 16 trang phân hệ Admin Portal toàn diện |
| **Slide 12** | Giao Diện Trực Quan Các Phân Hệ Cốt Lõi | 1.0 phút | Màn hình Đăng nhập, Dashboard, Duyệt vay, Tiết kiệm, Audit |
| **Slide 13** | **[LIVE DEMO 2] THỰC NGHIỆM GIAO DIỆN SELENIUM & IDE** | **2.5 phút** | **Chuyển màn hình Demo Selenium WebDriver & Selenium IDE** |
| **Slide 14** | Nhật Ký Phát Hiện & Xử Lý Lỗi (Defect Log) | 1.0 phút | 6 lỗi nghiêm trọng (BUG-01 đến BUG-06) đã xử lý triệt để |
| **Slide 15** | Đánh Giá Chất Lượng Theo Chuẩn ISO/IEC 25010 | 1.0 phút | 7 đặc tính chất lượng phần mềm đạt mức Xuất sắc |
| **Slide 16** | Tổng Kết Kết Quả & Định Hướng Phát Triển | 1.0 phút | 57/57 ca test Passed 100%, hoàn thành mục tiêu đồ án |
| **Slide 17** | Lời Cảm Ơn & Phiên Hỏi Đáp (Q&A) | - | Sẵn sàng giải đáp câu hỏi của Hội đồng bảo vệ |

---

## CHI TIẾT NỘI DUNG VÀ LỜI THOẠI TỪNG SLIDE (SPEAKER NOTES)

```
================================================================================
SLIDE 01: TRANG BÌA & GIỚI THIỆU ĐỀ TÀI
================================================================================
[Nội dung hiển thị trên Slide]:
- Logo Trường Đại học Phenikaa & Khoa Công nghệ Thông tin.
- Tên đề tài: BÁO CÁO KIỂM ĐỊNH VÀ ĐÁNH GIÁ CHẤT LƯỢNG HỆ THỐNG NGÂN HÀNG SỐ TRỰC TUYẾN MINIBANK
- Giảng viên hướng dẫn: TS. Trịnh Thanh Bình
- Nhóm sinh viên thực hiện: Nhóm Kiểm thử Phần mềm Chuyên sâu
- Chuyên ngành: Kỹ thuật Phần mềm / Công nghệ Thông tin
- Năm học: 2025 - 2026

[Kịch bản lời thoại của người thuyết trình - 60 giây]:
"Kính thưa Thầy/Cô trong Hội đồng chấm đề tài, cùng toàn thể các bạn sinh viên!
Hôm nay, nhóm chúng em xin phép được trình bày báo cáo tổng kết đề tài: 'Kiểm định và đánh giá chất lượng hệ thống ngân hàng số trực tuyến MiniBank', dưới sự hướng dẫn khoa học tận tình của Thầy TS. Trịnh Thanh Bình.
Trong bối cảnh chuyển đổi số ngành tài chính - ngân hàng đang diễn ra mạnh mẽ, chất lượng phần mềm không chỉ quyết định trải nghiệm người dùng mà còn là yếu tố sống còn liên quan trực tiếp đến an toàn tài sản của khách hàng và tính ổn định của hệ thống tài chính. Trong buổi báo cáo ngày hôm nay, nhóm em sẽ trình bày toàn bộ quy trình kiểm định chất lượng chuyên sâu từ mã nguồn Backend đến giao diện Frontend, cùng các phần trình diễn thực nghiệm trực tiếp ngay trên hệ thống."
```

---

```
================================================================================
SLIDE 02: ĐẶT VẤN ĐỀ & MỤC TIÊU KIỂM ĐỊNH
================================================================================
[Nội dung hiển thị trên Slide]:
- Bối cảnh: Hệ thống Fintech / Digital Banking đòi hỏi tính chính xác tuyệt đối và bảo mật nghiêm ngặt.
- Thách thức lớn:
  + Lỗ hổng tranh chấp tài nguyên đồng thời (Double-Spending / Race Condition).
  + Rủi ro bảo mật truy cập API trái phép và leo thang đặc quyền.
  + Đảm bảo thời gian phản hồi dưới 200ms khi chịu tải cao.
- Mục tiêu kiểm định:
  + Độ bao phủ mã nguồn (Code Coverage) đạt trên 85%.
  + Đảm bảo 100% các ca kiểm thử chức năng tự động đều Passed.
  + Kiểm soát toàn diện từ tầng Data, Service, Controller API đến UI Frontend.

[Kịch bản lời thoại của người thuyết trình - 60 giây]:
"Thưa Thầy/Cô, một lỗi phần mềm thông thường có thể chỉ gây gián đoạn hiển thị, nhưng một lỗi logic trong hệ thống ngân hàng có thể dẫn đến việc tài khoản bị trừ tiền sai, tiền bị rút trùng lặp (Double-Spending), hoặc rò rỉ dữ liệu tài chính nhạy cảm.
Chính vì vậy, mục tiêu tối thượng của đề tài này là xây dựng một hệ thống kiểm định đa tầng, kết hợp hài hòa giữa kiểm thử hộp trắng, kiểm thử hộp đen, kiểm thử chịu tải, kiểm thử tự động hóa API và kiểm thử tự động hóa giao diện, nhằm phát hiện sớm mọi rủi ro tiềm ẩn ngay từ giai đoạn phát triển và đảm bảo độ tin cậy tuyệt đối cho hệ thống MiniBank."
```

---

```
================================================================================
SLIDE 03: KIẾN TRÚC HỆ THỐNG MINIBANK
================================================================================
[Nội dung hiển thị trên Slide]:
- Sơ đồ kiến trúc 3 tầng chuẩn công nghiệp (Hình 1.3 trong Báo cáo):
  + Presentation Layer: Admin Portal xây dựng trên Next.js 16, React 19, Tailwind CSS 4.
  + Business Logic Layer: Spring Boot 3.2, Spring Security 6, JWT, Java 17.
  + Data Access Layer: Spring Data JPA / Hibernate, Pessimistic Locking.
  + Database: PostgreSQL 17 (Production) và H2 In-Memory Database (Test Profile độc lập).

[Kịch bản lời thoại của người thuyết trình - 60 giây]:
"Để phục vụ công tác kiểm thử chuẩn mực, hệ thống MiniBank được thiết kế theo kiến trúc phân tầng rành mạch:
- Tầng Frontend là Cổng quản trị Admin Portal hiện đại bằng Next.js 16 kết nối thông qua giao thức RESTful API bảo mật bằng JWT.
- Tầng Backend sử dụng Spring Boot 3 và Java 17, áp dụng chặt chẽ các mẫu thiết kế Service Layer và Repository Pattern.
- Đặc biệt, ở tầng dữ liệu, hệ thống hỗ trợ cơ chế đa môi trường: môi trường kiểm thử chạy hoàn toàn trên H2 In-Memory Database giúp 31 ca test tự động của Backend chạy độc lập, siêu tốc và cô lập dữ liệu; trong khi môi trường vận hành kết nối tới cơ sở dữ liệu PostgreSQL 17 mạnh mẽ."
```

---

```
================================================================================
SLIDE 04: MA TRẬN PHƯƠNG PHÁP & CÔNG CỤ KIỂM THỬ
================================================================================
[Nội dung hiển thị trên Slide]:
- Bảng ma trận kiểm định toàn diện:
  1. Kiểm thử Hộp Trắng (White-box): JUnit 5, Mockito, JaCoCo Coverage.
  2. Kiểm thử Hộp Đen (Black-box): Phân tích phân vùng tương đương (EP), Giá trị biên (BVA).
  3. Kiểm thử Tranh chấp (Concurrency): ExecutorService, CountDownLatch, Pessimistic Lock.
  4. Kiểm thử Hiệu năng (Performance): Apache JMeter (Mô phỏng 50 - 100 người dùng ảo).
  5. Kiểm thử Tự động hóa API: Postman Collection & Newman CLI.
  6. Kiểm thử Tự động hóa Giao diện: Selenium WebDriver (Python) & Selenium IDE.

[Kịch bản lời thoại của người thuyết trình - 60 giây]:
"Nhằm đạt được chất lượng toàn diện, nhóm chúng em đã áp dụng đầy đủ 6 phương pháp kiểm định hiện đại nhất trong công nghiệp:
Từ việc soi chiếu cấu trúc bên trong mã nguồn bằng kỹ thuật Kiểm thử hộp trắng và đo độ phức tạp Cyclomatic, đến kiểm thử hành vi nghiệp vụ người dùng bằng Kiểm thử hộp đen; từ kiểm thử an toàn luồng trong môi trường đa nhiệm đến bắn tải mô phỏng hàng ngàn giao dịch; và cuối cùng là tự động hóa 100% kịch bản kiểm thử API bằng Postman/Newman và kiểm thử 16 trang giao diện bằng Selenium."
```

---

```
================================================================================
SLIDE 05: KIỂM THỬ HỘP TRẮNG & ĐỘ BAO PHỦ JACOCO
================================================================================
[Nội dung hiển thị trên Slide]:
- Đồ thị dòng điều khiển (CFG) phương thức chuyển tiền `transfer()`.
- Phân tích độ phức tạp Cyclomatic: V(G) = E - N + 2P = 11 - 9 + 2 = 4 (4 đường cơ sở độc lập).
- Kỹ thuật Branch Coverage bao phủ 100% các nhánh rẽ điều kiện (T/F).
- Kết quả đo lường với công cụ JaCoCo:
  + Line Coverage (Bao phủ dòng lệnh): 90.3% (Vượt chỉ tiêu 85%).
  + Branch Coverage (Bao phủ nhánh): 85.6%.
  + Tổng số ca kiểm thử đơn vị & tích hợp: 31/31 ca Passed.

[Kịch bản lời thoại của người thuyết trình - 90 giây]:
"Đi sâu vào phương thức nghiệp vụ trọng yếu nhất là chuyển tiền `transfer()`, nhóm đã vẽ Đồ thị dòng điều khiển CFG và xác định độ phức tạp Cyclomatic V(G) bằng 4. Nghĩa là có tối thiểu 4 đường dẫn độc lập qua hàm: từ trường hợp nợ không đủ số dư, tài khoản nhận không hợp lệ, đến chuyển tiền thành công có tính phí.
Nhóm đã thiết kế bộ kiểm thử JUnit 5 bao phủ toàn bộ 4 nhánh điều kiện này. Khi kích hoạt công cụ đo lường JaCoCo, hệ thống đạt độ bao phủ dòng lệnh lên tới 90.3% và độ bao phủ nhánh rẽ là 85.6%, vượt xa tiêu chuẩn 85% mà đề tài đặt ra. Điều này chứng minh mã nguồn Backend đã được kiểm tra cẩn trọng đến từng dòng lệnh."
```

---

```
================================================================================
SLIDE 06: KIỂM THỬ HỘP ĐEN NGHIỆP VỤ CỐT LÕI
================================================================================
[Nội dung hiển thị trên Slide]:
- Kỹ thuật Phân vùng tương đương (Equivalence Partitioning - EP):
  + Chia miền giá trị số tiền chuyển thành: Miền không hợp lệ âm ($<0$), Miền biên ranh giới ($=0$), Miền hợp lệ ($1 \le x \le Balance$), Miền vượt số dư ($>Balance$).
- Kỹ thuật Phân tích giá trị biên chuẩn (BVA 6 giá trị):
  + Điểm biên xét: $min-1, min, min+1, max-1, max, max+1$.
- Ứng dụng trên các phân hệ: Gửi đơn vay vốn tín dụng, Phê duyệt hạn mức vay, Mở sổ tiết kiệm và tính lãi suất kỳ hạn.

[Kịch bản lời thoại của người thuyết trình - 60 giây]:
"Ở cấp độ nghiệp vụ, nhóm đã áp dụng triệt để hai kỹ thuật kiểm thử hộp đen kinh điển là Phân vùng tương đương và Phân tích giá trị biên.
Ví dụ đối với chức năng chuyển tiền: nhóm phân chia thành các vùng số tiền âm, số tiền bằng không, số tiền hợp lệ và số tiền vượt số dư. Chính nhờ kỹ thuật phân tích biên khắt khe này, nhóm đã phát hiện ra lỗ hổng BUG-01 khi câu lệnh kiểm tra ban đầu chỉ chặn số âm mà bỏ lọt trường hợp người dùng chuyển số tiền đúng bằng 0 VNĐ, từ đó đã kịp thời sửa đổi mã nguồn."
```

---

```
================================================================================
SLIDE 07: KIỂM THỬ ĐỒNG THỜI & CHỐNG DOUBLE-SPENDING
================================================================================
[Nội dung hiển thị trên Slide]:
- Kịch bản hiểm họa: Lỗ hổng tranh chấp Race Condition khi tài khoản có 10.000.000 VNĐ nhưng chịu 10 luồng rút tiền đồng thời, mỗi luồng rút 2.000.000 VNĐ.
- Thiết kế kiểm thử: Sử dụng `ExecutorService(10 luồng)` và chốt chặn đồng pha `CountDownLatch`.
- Giải pháp phòng thủ: Khóa bi quan tầng CSDL `@Lock(LockModeType.PESSIMISTIC_WRITE)` (`SELECT ... FOR UPDATE`).
- Kết quả thực nghiệm: Đúng 5 luồng rút tiền thành công, 5 luồng bị từ chối do hết số dư. Số dư cuối cùng chính xác tuyệt đối bằng 0 VNĐ.

[Kịch bản lời thoại của người thuyết trình - 90 giây]:
"Một trong những điểm nhấn kỹ thuật quan trọng nhất của đề tài là Kiểm thử tranh chấp đồng thời (Concurrency Test).
Trong ngân hàng, hiểm họa 'Double-Spending' xảy ra khi kẻ gian lợi dụng mạng lag để bấm rút tiền cùng lúc từ nhiều thiết bị. Nhóm đã viết kịch bản đa luồng mô phỏng 10 luồng rút tiền phát lệnh đồng thời trong cùng một mili-giây trên tài khoản có 10 triệu đồng.
Nếu dùng truy vấn thông thường, hệ thống sẽ bị lỗi rút lố tiền. Nhóm đã áp dụng cơ chế Khóa bi quan Pessimistic Write Lock ở tầng cơ sở dữ liệu. Kết quả kiểm thử thực tế chứng minh: đúng 5 giao dịch đầu tiên được xử lý tuần tự thành công, 5 giao dịch sau bị chặn đứng vì số dư không đủ, số dư cuối cùng về đúng 0 VNĐ, tài khoản được bảo vệ toàn vẹn tuyệt đối."
```

---

```
================================================================================
SLIDE 08: KIỂM THỬ HIỆU NĂNG & CHỊU TẢI (APACHE JMETER)
================================================================================
[Nội dung hiển thị trên Slide]:
- Kịch bản kiểm thử tải: 50 - 100 người dùng ảo (Virtual Users), lặp 100 chu kỳ, Think Time 100ms.
- Tổng số mẫu thực thi: 20.000 giao dịch liên tục.
- Kết quả đo lường:
  + Thời gian phản hồi trung bình: 54.7 ms (Yêu cầu đặt ra: < 200 ms).
  + Tỷ lệ lỗi giao dịch (Error %): Hoàn hảo 0.00%.
  + Thông lượng hệ thống (Throughput): 290.25 requests/giây.
  + Mức tiêu thụ tài nguyên: CPU < 35%, RAM ổn định quanh 450 MB.

[Kịch bản lời thoại của người thuyết trình - 60 giây]:
"Tiếp theo là kiểm định về mặt Hiệu năng và Năng lực chịu tải bằng công cụ chuẩn công nghiệp Apache JMeter.
Nhóm đã cấu hình kịch bản Thread Group bắn tải 20.000 mẫu giao dịch qua 4 API cốt lõi: Đăng nhập, Tra cứu số dư, Chuyển tiền và Lịch sử giao dịch.
Kết quả đo lường đạt mức xuất sắc: Thời gian phản hồi trung bình toàn hệ thống chỉ 54.7 mili-giây, thông lượng xử lý đạt gần 300 yêu cầu/giây và đặc biệt tỷ lệ lỗi giao dịch đạt mức tuyệt đối 0.00%."
```

---

```
================================================================================
SLIDE 09: TỰ ĐỘNG HÓA KIỂM THỬ API (POSTMAN / NEWMAN)
================================================================================
[Nội dung hiển thị trên Slide]:
- Bộ kịch bản tự động hóa Postman Collection (`MiniBank_API_Automation`):
  + Bao gồm 10 API trải rộng trên 4 phân nhóm: Admin Portal, Mobile Auth, Sản phẩm dịch vụ, và Bảo mật.
  + Thiết lập 17 điều kiện khẳng định (Assertions) tự động xác thực mã trạng thái, Bearer Token, JSON schema.
- Kết quả thực nghiệm:
  + 10/10 Requests thực thi thành công.
  + 17/17 Assertions Passed (Tỷ lệ đạt tuyệt đối 100%).
  + Thời gian đáp ứng trung bình: 38 ms.
  + Hỗ trợ chạy không cần cài đặt qua lệnh Newman CLI trên môi trường CI/CD.

[Kịch bản lời thoại của người thuyết trình - 60 giây]:
"Để phục vụ kiểm thử hồi quy tự động trong đường ống tích hợp liên tục CI/CD, nhóm đã xây dựng bộ kịch bản tự động hóa API trên Postman và đóng gói để chạy qua công cụ dòng lệnh Newman CLI.
Bộ test kiểm tra toàn diện 10 API then chốt với 17 điều kiện Assertion. Kết quả thực nghiệm cho thấy 100% assertions đều vượt qua thành công với độ trễ cực nhanh chỉ 38 mili-giây.
Và ngay sau đây, nhóm em xin phép được trình diễn trực tiếp phần kiểm thử API trên Postman và Newman CLI!"
```

---

```
================================================================================
SLIDE 10: [CHUYỂN TIẾP DEMO 1] LIVE DEMO KIỂM THỬ TỰ ĐỘNG API POSTMAN
================================================================================
[Nội dung hiển thị trên Slide]:
- Biểu tượng LIVE DEMO: AUTOMATED API TESTING DEMONSTRATION
- Các nội dung sẽ thực hiện trình diễn:
  1. Chạy tự động trọn bộ 10 API qua Postman Collection Runner.
  2. Bắn yêu cầu đơn lẻ và kiểm tra cấu trúc JWT Bearer Token tại `TC_API_ADM_01`.
  3. Kiểm tra tính bảo mật chặn nặc danh `TC_API_SEC_01` (HTTP 401 Unauthorized).
  4. Thực thi dòng lệnh siêu tốc qua Newman CLI trên Terminal.

[Kịch bản lời thoại của người thuyết trình khi thao tác Live Demo - 2.5 phút]:
(Người thuyết trình nhấn Alt + Tab chuyển sang cửa sổ Postman Desktop):

"Kính thưa Thầy/Cô, trên màn hình hiện tại là giao diện Postman Desktop chứa bộ sưu tập 'MiniBank API Automation Test Suite'.
- Bước 1: Em nhấp chuột phải vào Collection và bấm 'Run collection'. Giữ nguyên 10 API và bấm nút 'Run MiniBank API Automation'. Như Thầy/Cô có thể quan sát, chỉ sau hơn 1 giây, toàn bộ 17 điều kiện kiểm tra đã hiển thị màu xanh lá: Passed 17, Failed 0, thời gian phản hồi trung bình 38ms.
- Bước 2: Em mở chi tiết request 'TC_API_ADM_01: Admin Login' và bấm Send. Máy chủ lập tức phản hồi mã trạng thái 200 OK trong 118ms, kèm theo chuỗi Bearer Token mã hóa JWT và danh sách quyền hạn Quản trị viên.
- Bước 3: Để kiểm tra tính bảo mật, em chuyển sang request 'TC_API_SEC_01: Protected API without Token' và bấm Send. Vì không có Token xác thực, bộ lọc Spring Security đã chặn đứng yêu cầu và trả về lỗi 401 Unauthorized trong vòng 3ms.
- Bước 4: Cuối cùng, để chứng minh khả năng chạy tự động trong CI/CD, em chuyển sang cửa sổ PowerShell và gõ lệnh:
  cmd /c 'npx --yes newman run test_automation/postman/MiniBank_API_Automation.postman_collection.json --env-var base_url=http://localhost:8080'
  Bảng kết quả tổng hợp của Newman in ra khung ASCII tuyệt đẹp với 10/10 requests thành công và 17/17 assertions Passed!"

(Người thuyết trình nhấn Alt + Tab quay trở lại Slide trình chiếu).
```

---

```
================================================================================
SLIDE 11: KIỂM THỬ GIAO DIỆN NGƯỜI DÙNG (SELENIUM WEBDRIVER)
================================================================================
[Nội dung hiển thị trên Slide]:
- Bộ kiểm thử giao diện tự động hóa với Selenium WebDriver (Python Unittest).
- Bao quát toàn diện 16 trang và phân hệ quản trị trên Admin Portal:
  + TC_PAGE_01 - 03: Xác thực đăng nhập, bắt lỗi form, chuyển hướng Dashboard.
  + TC_PAGE_04 - 06: Dashboard KPI, Duyệt khoản vay, Duyệt sổ tiết kiệm.
  + TC_PAGE_07 - 09: Quản lý khách hàng, Thẩm định eKYC, Hồ sơ chứng từ.
  + TC_PAGE_10 - 12: Cấu hình sản phẩm vay, Tiết kiệm, Duyệt giao dịch lớn.
  + TC_PAGE_13 - 16: Quản lý cán bộ, Phân quyền RBAC, Audit log, Đổi mật khẩu, 404 & Logout.
- Kết quả thực thi tự động: 16/16 trang Passed trong 72.95s (Tỷ lệ đạt 100%).

[Kịch bản lời thoại của người thuyết trình - 60 giây]:
"Sau khi tầng API đã ổn định, nhóm tiến hành kiểm thử tự động tầng giao diện người dùng (End-to-End UI Testing) để đảm bảo trải nghiệm người dùng thực tế không bị lỗi.
Thay vì chỉ kiểm tra một vài trang cơ bản, nhóm đã xây dựng kịch bản kiểm thử toàn diện trên toàn bộ 16 trang phân hệ của Cổng quản trị MiniBank Admin Portal.
Kịch bản Selenium tự động hóa thao tác người dùng: từ điền form, kiểm tra validation, click chuyển trang, xác minh thẻ DOM, đến kiểm tra tính năng thẩm định hồ sơ vay và nhật ký kiểm toán. Toàn bộ 16 ca kiểm thử giao diện đều chạy thành công 100% trong 72.95 giây."
```

---

```
================================================================================
SLIDE 12: GIAO DIỆN TRỰC QUAN CÁC PHÂN HỆ CỐT LÕI
================================================================================
[Nội dung hiển thị trên Slide]:
- Lưới hiển thị các hình ảnh chụp thực tế tiêu biểu từ hệ thống:
  + Hình 1: Màn hình Đăng nhập Quản trị (`/login`).
  + Hình 2: Bảng điều khiển KPI Dashboard trung tâm (`/`).
  + Hình 3: Thẩm định & Phê duyệt hồ sơ vay tín dụng (`/requests/loan-approval`).
  + Hình 4: Phê duyệt sổ tiết kiệm có kỳ hạn (`/requests/saving-approval`).
  + Hình 5: Thẩm định giấy tờ định danh eKYC (`/customers/kyc`).
  + Hình 6: Nhật ký kiểm toán an ninh Audit Trail (`/system/audit`).

[Kịch bản lời thoại của người thuyết trình - 60 giây]:
"Trên màn hình là hình ảnh thực tế được thu thập trực tiếp trong quá trình Selenium chạy kiểm thử:
Cổng quản trị được thiết kế theo phong cách giao diện ngân hàng hiện đại (Clean Modern UI), phân cấp menu rành mạch:
- Cán bộ tín dụng có thể trực tiếp thẩm định hồ sơ vay và phê duyệt mở sổ tiết kiệm với các thông tin minh bạch về hạn mức và lãi suất.
- Bộ phận kiểm soát có thể tra cứu toàn bộ dấu vết truy cập trong Nhật ký kiểm toán an ninh Audit Trail.
Và tiếp theo, em xin phép được trình diễn phần kiểm thử giao diện tự động bằng Selenium WebDriver và tiện ích Selenium IDE!"
```

---

```
================================================================================
SLIDE 13: [CHUYỂN TIẾP DEMO 2] LIVE DEMO KIỂM THỬ GIAO DIỆN SELENIUM
================================================================================
[Nội dung hiển thị trên Slide]:
- Biểu tượng LIVE DEMO: AUTOMATED UI TESTING DEMONSTRATION
- Các nội dung sẽ thực hiện trình diễn:
  1. Chạy bộ kiểm thử tự động hóa 16 trang Selenium qua dòng lệnh Python.
  2. Mở dự án `MiniBank_Admin_UI_Test.side` trên tiện ích Selenium IDE Runner.
  3. Quan sát trình duyệt tự động điền thông tin, click chuyển trang và xác thực dữ liệu.

[Kịch bản lời thoại của người thuyết trình khi thao tác Live Demo - 2.5 phút]:
(Người thuyết trình mở Terminal):

"Kính thưa Thầy/Cô, bây giờ em sẽ thực thi bộ kiểm thử tự động hóa giao diện:
- Bước 1: Trong Terminal, em gõ lệnh:
  python test_automation/selenium/test_admin_portal_all_pages.py -v
  Hệ thống sẽ khởi tạo trình duyệt Chrome Headless, tự động đăng nhập với tài khoản Admin, và lần lượt duyệt qua kiểm tra DOM của cả 16 phân hệ: từ Login, Dashboard, Loan Approval, Saving Approval đến Quản lý khách hàng, eKYC, Audit Log. Các bài test lần lượt in ra trạng thái 'ok' và kết thúc với kết quả hoàn hảo: Ran 16 tests - OK!
- Bước 2: Để Hội đồng có thể quan sát trực quan từng thao tác tương tác người dùng, em xin phép mở tiện ích Selenium IDE trên Chrome và nạp tệp kịch bản 'MiniBank_Admin_UI_Test.side'.
  Em bấm nút Play (▶️) để chạy toàn bộ kịch bản Record & Playback: Thầy/Cô có thể thấy trình duyệt tự động mở trang /login, tự động gõ 'admin', gõ mật khẩu 'admin123', click Đăng nhập, kiểm tra hiển thị Dashboard, sau đó click sang trang Phê duyệt khoản vay và thực hiện đăng xuất an toàn. Mọi lệnh assertElementPresent và assertText đều trả về màu xanh Passed!"

(Người thuyết trình chuyển lại Slide trình chiếu).
```

---

```
================================================================================
SLIDE 14: NHẬT KÝ PHÁT HIỆN & XỬ LÝ LỖI (DEFECT LOG)
================================================================================
[Nội dung hiển thị trên Slide]:
- Bảng tổng hợp 6 lỗi quan trọng phát hiện trong quá trình kiểm thử (Bảng 4.1):
  + BUG-01 (CRITICAL): Cho phép chuyển tiền bằng 0 -> Đã sửa điều kiện `amount <= 0` (CLOSED).
  + BUG-02 (MAJOR): Cho phép nộp đơn xin vay số tiền âm -> Đã bổ sung tiền kiểm tra `loanAmount <= 0` (CLOSED).
  + BUG-03 (CRITICAL): Duyệt vay nhưng không cộng tiền giải ngân -> Đã bổ sung cập nhật số dư (CLOSED).
  + BUG-04 (BLOCKER): Tranh chấp Double-Spending khi rút tiền đồng thời -> Đã áp dụng Khóa bi quan (CLOSED).
  + BUG-05 (HIGH): Trang Audit Log cho phép truy cập khi chưa xác thực -> Bổ sung Client Route Guard (CLOSED).
  + BUG-06 (MEDIUM): Fallback API Port 8081 gây lỗi kết nối -> Chuẩn hóa về cổng 8080 (CLOSED).
- Sơ đồ chu trình xử lý lỗi (Defect Life Cycle): 100% lỗi đã được Đóng (CLOSED).

[Kịch bản lời thoại của người thuyết trình - 60 giây]:
"Mục đích cao nhất của kiểm định là tìm ra lỗi và nâng cao chất lượng phần mềm.
Trong suốt quá trình kiểm thử, nhóm đã phát hiện và ghi nhận 6 lỗi quan trọng từ mức độ Trung bình đến Nghiêm trọng:
- Đáng chú ý nhất là lỗi BUG-03 khi cán bộ duyệt vay xong thì hệ thống chỉ đổi trạng thái mà quên cộng tiền vào tài khoản người vay;
- Và lỗi BUG-04 tranh chấp số dư khi rút tiền đồng thời.
Nhóm đã phân tích nguyên nhân gốc rễ, sửa đổi mã nguồn và chạy kiểm thử hồi quy xác nhận toàn bộ 6 lỗi đều đã được giải quyết dứt điểm, trạng thái hiện tại là CLOSED 100%."
```

---

```
================================================================================
SLIDE 15: ĐÁNH GIÁ CHẤT LƯỢNG THEO CHUẨN ISO/IEC 25010
================================================================================
[Nội dung hiển thị trên Slide]:
- Đánh giá hệ thống MiniBank dựa trên 7 đặc tính cốt lõi của ISO/IEC 25010:
  1. Tính phù hợp chức năng (Functional Suitability): 5.0/5 - 57/57 ca test tự động Passed 100%.
  2. Hiệu quả hoạt động (Performance Efficiency): 4.8/5 - Độ trễ trung bình 54.7 ms dưới tải cao.
  3. Tính tương thích (Compatibility): 4.7/5 - Tương thích hoàn hảo Chrome, Edge, Firefox.
  4. Tính khả dụng (Usability): 4.8/5 - Giao diện trực quan, đào tạo sử dụng dưới 5 phút.
  5. Độ tin cậy (Reliability): 5.0/5 - Khóa bi quan chống Double-Spending, tỷ lệ lỗi 0.00%.
  6. Tính bảo mật (Security): 4.9/5 - Mật khẩu mã hóa BCrypt, JWT Bearer Token, Route Guard.
  7. Khả năng bảo trì (Maintainability): 4.9/5 - Kiến trúc phân tầng, JaCoCo Coverage 90.3%.

[Kịch bản lời thoại của người thuyết trình - 60 giây]:
"Để đưa ra kết luận khách quan, nhóm đã đối chiếu toàn bộ kết quả kiểm định với khung tiêu chuẩn quốc tế ISO/IEC 25010.
Hệ thống MiniBank được đánh giá Xuất sắc trên cả 7 đặc tính chất lượng phần mềm: từ Tính phù hợp chức năng đạt 5/5, Độ tin cậy đạt 5/5, đến Tính bảo mật đạt 4.9/5. Đây là minh chứng rõ ràng cho việc hệ thống đã đạt độ chín muồi cao về mặt kỹ thuật và hoàn toàn sẵn sàng cho môi trường ứng dụng thực tế."
```

---

```
================================================================================
SLIDE 16: TỔNG KẾT KẾT QUẢ & ĐỊNH HƯỚNG PHÁT TRIỂN
================================================================================
[Nội dung hiển thị trên Slide]:
- Tổng kết thành quả đạt được:
  + Hoàn thành 100% mục tiêu đề cương nghiên cứu môn học theo chuẩn Đại học Phenikaa.
  + Đạt tỷ lệ 57/57 ca kiểm thử tự động hóa thành công (100% Pass Rate).
  + Độ bao phủ mã nguồn JaCoCo đạt 90.3% Line Coverage và 85.6% Branch Coverage.
  + Tỷ lệ lỗi chịu tải JMeter đạt 0.00% với tốc độ phản hồi 54.7 ms.
  + Đã phát hiện và sửa dứt điểm 6 lỗi logic và bảo mật.
- Đề xuất mở rộng trong tương lai:
  + Tích hợp đường ống CI/CD tự động hóa (GitHub Actions / Jenkins).
  + Nâng cấp cơ chế xác thực đa yếu tố sinh trắc học và 2FA OTP thời gian thực.
  + Triển khai Redis Cache phân tán tăng cường hiệu năng xử lý hàng triệu giao dịch.

[Kịch bản lời thoại của người thuyết trình - 60 giây]:
"Tóm lại, đề tài đã hoàn thành xuất sắc tất cả các mục tiêu đề ra: xây dựng bộ hồ sơ tài liệu kỹ thuật hoàn chỉnh từ SRS, Test Plan đến Test Report; thiết lập thành công mạng lưới 57 ca kiểm thử tự động hóa với độ bao phủ mã nguồn trên 90%.
Trong giai đoạn tiếp theo, nhóm định hướng sẽ tích hợp bộ kiểm thử này vào đường ống GitHub Actions CI/CD để tự động chạy kiểm thử hồi quy sau mỗi đợt đẩy mã, đồng thời nghiên cứu bổ sung thêm lớp bảo mật xác thực hai yếu tố 2FA."
```

---

```
================================================================================
SLIDE 17: LỜI CẢM ƠN & PHIÊN HỎI ĐÁP (Q&A)
================================================================================
[Nội dung hiển thị trên Slide]:
- Dòng chữ lớn: "XIN CHÂN THÀNH CẢM ƠN THẦY CÔ VÀ CÁC BẠN ĐÃ LẮNG NGHE!"
- Thông tin liên hệ nhóm sinh viên.
- Sẵn sàng chuyển sang phần HỎI ĐÁP & PHẢN BIỆN (Q&A Session).

[Kịch bản lời thoại của người thuyết trình - 30 giây]:
"Bài trình bày báo cáo của nhóm chúng em đến đây là kết thúc. Chúng em xin trân trọng cảm ơn Thầy TS. Trịnh Thanh Bình đã luôn đồng hành, định hướng và giúp đỡ chúng em trong suốt quá trình thực hiện đề tài.
Chúng em rất mong nhận được những ý kiến đóng góp quý báu và các câu hỏi từ Quý Thầy/Cô trong Hội đồng để đề tài được hoàn thiện tốt hơn nữa. Em xin trân trọng cảm ơn!"
```

---

## BỘ CÂU HỎI PHẢN BIỆN THƯỜNG GẶP (Q&A CHEAT SHEET) CHO BUỔI BẢO VỆ

| Câu Hỏi Của Hội Đồng | Hướng Dẫn Trả Lời Ngắn Gọn & Thuyết Phục |
|:---|:---|
| **1. Tại sao nhóm lại chọn Khóa bi quan (Pessimistic Lock) thay vì Khóa lạc quan (Optimistic Lock) để chống Double-Spending?** | *"Thưa Thầy/Cô, trong hệ thống tài chính ngân hàng, tiền bạc yêu cầu tính nhất quán tức thời (Strict Consistency). Khóa lạc quan sử dụng trường version, khi có 10 luồng cùng rút tiền thì 9 luồng sẽ bị ném ngoại lệ `OptimisticLockException` và buộc người dùng phải thử lại giao dịch, gây trải nghiệm rất xấu. Khóa bi quan `PESSIMISTIC_WRITE` khóa bản ghi ngay tại câu lệnh `SELECT ... FOR UPDATE`, ép các giao dịch phải xếp hàng thực hiện tuần tự, đảm bảo số dư luôn chính xác tuyệt đối mà không cần người dùng thao tác lại."* |
| **2. Làm thế nào để đảm bảo 31 ca kiểm thử Backend không làm hỏng dữ liệu trong Database thật?** | *"Dạ thưa Thầy/Cô, hệ thống được cấu hình đa môi trường (Spring Profiles). Khi chạy lệnh `mvn test`, hệ thống kích hoạt profile `test` chạy trên cơ sở dữ liệu ảo trong RAM là **H2 In-Memory Database** với cơ chế tự tạo bảng và tự hủy sau khi test xong. Nhờ đó, dữ liệu trên PostgreSQL thật hoàn toàn không bị ảnh hưởng và tốc độ thực thi kiểm thử chỉ mất vài giây."* |
| **3. Bộ kiểm thử Postman có 10 API, tại sao nhóm không chụp ảnh kết quả của cả 10 lần gửi?** | *"Dạ thưa Thầy/Cô, màn hình Postman Collection Runner là minh chứng quan trọng và đầy đủ nhất vì nó đã tự động duyệt và ghi nhận toàn bộ 10/10 requests với 17/17 assertions Passed. Ba ảnh chi tiết đính kèm đại diện cho 3 nhóm tính chất cốt lõi: Xác thực cấp quyền (Positive), Truy xuất dữ liệu nghiệp vụ (Data flow), và Chặn truy cập trái phép khi thiếu Token (Negative/Security). Việc chụp cả 10 lần bấm Send sẽ gây trùng lặp giao diện mà không mang lại thêm giá trị kỹ thuật mới."* |
| **4. Tại sao lại cần cả Selenium WebDriver lẫn Selenium IDE?** | *"Dạ thưa Thầy/Cô, hai công cụ này bổ trợ cho nhau: Selenium WebDriver viết bằng Python phục vụ tự động hóa không đầu (Headless mode) để tích hợp vào đường ống CI/CD tự động; trong khi Selenium IDE là công cụ Record & Playback trực quan, giúp các chuyên viên phân tích nghiệp vụ (BA) hoặc Tester mới dễ dàng ghi lại và quan sát từng bước thao tác người dùng mà không cần lập trình phức tạp."* |
