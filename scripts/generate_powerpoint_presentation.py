# -*- coding: utf-8 -*-
"""
Generate a professional 16:9 widescreen PowerPoint presentation (.pptx)
for the Phenikaa University Graduation / Project Defense:
'Kiểm định và đánh giá chất lượng hệ thống ngân hàng số trực tuyến MiniBank'
"""

import os
import shutil
from pptx import Presentation
from pptx.util import Inches, Pt
from pptx.dml.color import RGBColor
from pptx.enum.text import PP_ALIGN, MSO_ANCHOR
from pptx.enum.shapes import MSO_SHAPE

def create_deck():
    prs = Presentation()
    # 16:9 widescreen
    prs.slide_width = Inches(13.333)
    prs.slide_height = Inches(7.5)
    blank_layout = prs.slide_layouts[6]

    # Colors
    NAVY = RGBColor(0x1F, 0x49, 0x7D)      # #1F497D
    DARK_BLUE = RGBColor(0x0F, 0x24, 0x3E) # #0F243E
    CYAN = RGBColor(0x00, 0x83, 0x8F)      # #00838F
    GREEN = RGBColor(0x2E, 0x7D, 0x32)     # #2E7D32
    ORANGE = RGBColor(0xE6, 0x51, 0x00)    # #E65100
    WHITE = RGBColor(0xFF, 0xFF, 0xFF)
    DARK_GRAY = RGBColor(0x33, 0x33, 0x33)
    LIGHT_GRAY = RGBColor(0x66, 0x66, 0x66)
    CARD_BG = RGBColor(0xF4, 0xF6, 0xF9)
    BORDER_COLOR = RGBColor(0xCF, 0xD8, 0xDC)

    def add_header(slide, title, category="TRƯỜNG ĐẠI HỌC PHENIKAA • KHOA CÔNG NGHỆ THÔNG TIN"):
        # Top banner
        header_box = slide.shapes.add_textbox(Inches(0.8), Inches(0.4), Inches(11.7), Inches(1.1))
        tf = header_box.text_frame
        tf.word_wrap = True
        tf.margin_left = tf.margin_top = tf.margin_right = tf.margin_bottom = 0
        
        p0 = tf.paragraphs[0]
        p0.text = category.upper()
        p0.font.size = Pt(11)
        p0.font.bold = True
        p0.font.color.rgb = CYAN
        
        p1 = tf.add_paragraph()
        p1.text = title
        p1.font.size = Pt(24)
        p1.font.bold = True
        p1.font.color.rgb = NAVY
        p1.space_before = Pt(4)

        # Thin accent line under header
        line = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(0.8), Inches(1.5), Inches(11.733), Inches(0.04))
        line.fill.solid()
        line.fill.fore_color.rgb = CYAN
        line.line.color.rgb = CYAN

    # =========================================================================
    # SLIDE 1: Title Slide
    # =========================================================================
    s1 = prs.slides.add_slide(blank_layout)
    # Background rectangle
    bg = s1.shapes.add_shape(MSO_SHAPE.RECTANGLE, 0, 0, Inches(13.333), Inches(7.5))
    bg.fill.solid()
    bg.fill.fore_color.rgb = NAVY
    bg.line.fill.background()

    # Title box
    tbox = s1.shapes.add_textbox(Inches(1.2), Inches(1.5), Inches(11.0), Inches(4.5))
    tf1 = tbox.text_frame
    tf1.word_wrap = True

    p = tf1.paragraphs[0]
    p.text = "TRƯỜNG ĐẠI HỌC PHENIKAA  •  KHOA CÔNG NGHỆ THÔNG TIN"
    p.font.size = Pt(14)
    p.font.bold = True
    p.font.color.rgb = RGBColor(0x80, 0xD8, 0xFF)
    p.space_after = Pt(16)

    p = tf1.add_paragraph()
    p.text = "BÁO CÁO KIỂM ĐỊNH & ĐÁNH GIÁ CHẤT LƯỢNG\nHỆ THỐNG NGÂN HÀNG SỐ MINIBANK"
    p.font.size = Pt(32)
    p.font.bold = True
    p.font.color.rgb = WHITE
    p.space_after = Pt(24)

    p = tf1.add_paragraph()
    p.text = "Giảng viên hướng dẫn: TS. Trịnh Thanh Bình"
    p.font.size = Pt(16)
    p.font.bold = True
    p.font.color.rgb = RGBColor(0xFF, 0xD5, 0x4F)
    p.space_after = Pt(8)

    p = tf1.add_paragraph()
    p.text = "Đơn vị đào tạo: Học kỳ I - Năm học 2025 - 2026  •  Hà Nội, 2026"
    p.font.size = Pt(13)
    p.font.color.rgb = RGBColor(0xCF, 0xD8, 0xDC)

    # =========================================================================
    # SLIDE 2: Problem Statement & Objectives
    # =========================================================================
    s2 = prs.slides.add_slide(blank_layout)
    add_header(s2, "1. Đặt Vấn Đề & Mục Tiêu Kiểm Định Chất Lượng")
    
    # 3 Cards Layout
    cards_data = [
        ("Bối Cảnh & Thách Thức", [
            "Hệ thống Fintech / Ngân hàng số yêu cầu tính chính xác tuyệt đối 100%.",
            "Lỗ hổng tranh chấp tài nguyên (Double-Spending) khi rút tiền đồng thời.",
            "Rủi ro bảo mật dữ liệu khách hàng và tấn công leo thang quyền hạn API.",
            "Áp lực thời gian phản hồi dưới 200ms khi phục vụ lưu lượng lớn."
        ], ORANGE),
        ("Mục Tiêu Kiểm Định", [
            "Thiết lập mạng lưới kiểm thử đa tầng: White-box, Black-box, API, UI.",
            "Đo lường độ bao phủ mã nguồn (Code Coverage) đạt chỉ tiêu trên 85%.",
            "Bảo vệ an toàn dữ liệu số dư bằng cơ chế khóa bi quan (Pessimistic Lock).",
            "Đảm bảo 100% các ca kiểm thử tự động đều vượt qua thành công."
        ], NAVY),
        ("Kết Quả Đạt Được", [
            "57/57 ca kiểm thử tự động hóa Passed tuyệt đối 100%.",
            "Độ bao phủ JaCoCo đạt 90.3% Line Coverage và 85.6% Branch Coverage.",
            "Hiệu năng tải JMeter đạt 54.7 ms với tỷ lệ lỗi giao dịch hoàn hảo 0.00%.",
            "Phát hiện và xử lý dứt điểm 6 lỗi logic và cấu hình bảo mật."
        ], GREEN),
    ]

    for i, (title, points, color) in enumerate(cards_data):
        left = Inches(0.8 + i * 4.0)
        c = s2.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, left, Inches(1.9), Inches(3.7), Inches(5.0))
        c.fill.solid()
        c.fill.fore_color.rgb = CARD_BG
        c.line.color.rgb = color
        c.line.width = Pt(2)

        tb = s2.shapes.add_textbox(left + Inches(0.2), Inches(2.1), Inches(3.3), Inches(4.5))
        tf = tb.text_frame
        tf.word_wrap = True
        
        p = tf.paragraphs[0]
        p.text = title
        p.font.size = Pt(17)
        p.font.bold = True
        p.font.color.rgb = color
        p.space_after = Pt(14)

        for pt in points:
            p = tf.add_paragraph()
            p.text = "• " + pt
            p.font.size = Pt(12.5)
            p.font.color.rgb = DARK_GRAY
            p.space_after = Pt(10)

    # =========================================================================
    # SLIDE 3: System Architecture
    # =========================================================================
    s3 = prs.slides.add_slide(blank_layout)
    add_header(s3, "2. Kiến Trúc Phân Tầng Hệ Thống MiniBank")

    arch_layers = [
        ("Tầng Giao Diện (Presentation Layer) - Next.js 16 / React 19", 
         "Admin Portal hiện đại, Tailwind CSS 4, quản lý phiên qua JWT trong localStorage, kết nối REST API.", Inches(1.8), CYAN),
        ("Tầng Bảo Mật & Điểm Cuối (Security & Controller Layer)", 
         "Spring Security 6, JWT Filter xác thực Stateless, phân quyền RBAC (ADMIN, USER, COMPLIANCE).", Inches(3.1), NAVY),
        ("Tầng Nghiệp Vụ Cốt Lõi (Business Logic Service Layer)", 
         "Dịch vụ tài chính: TransferService, LoanService, SavingsService, AuthService, AuditLogService.", Inches(4.4), GREEN),
        ("Tầng Dữ Liệu & Lưu Trữ (Data Access & Database Layer)", 
         "Spring Data JPA, Khóa bi quan (Pessimistic Locking). Đa môi trường: PostgreSQL 17 (Prod) & H2 (Test).", Inches(5.7), ORANGE),
    ]

    for title, desc, top, col in arch_layers:
        b = s3.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(0.8), top, Inches(11.733), Inches(1.05))
        b.fill.solid()
        b.fill.fore_color.rgb = CARD_BG
        b.line.color.rgb = col
        b.line.width = Pt(2)

        tb = s3.shapes.add_textbox(Inches(1.0), top + Inches(0.12), Inches(11.3), Inches(0.8))
        tf = tb.text_frame
        tf.word_wrap = True
        p0 = tf.paragraphs[0]
        p0.text = title
        p0.font.size = Pt(15)
        p0.font.bold = True
        p0.font.color.rgb = col

        p1 = tf.add_paragraph()
        p1.text = desc
        p1.font.size = Pt(12)
        p1.font.color.rgb = DARK_GRAY
        p1.space_before = Pt(3)

    # =========================================================================
    # SLIDE 4: Testing Matrix & Methodology
    # =========================================================================
    s4 = prs.slides.add_slide(blank_layout)
    add_header(s4, "3. Ma Trận Phương Pháp & Công Cụ Kiểm Định Chất Lượng")

    matrix_items = [
        ("Kiểm Thử Hộp Trắng", "JUnit 5 & Mockito", "Đo độ phức tạp Cyclomatic V(G), kiểm thử rẽ nhánh Branch Testing"),
        ("Đo Độ Bao Phủ Mã Nguồn", "JaCoCo Coverage", "Line Coverage đạt 90.3%, Branch Coverage đạt 85.6%"),
        ("Kiểm Thử Hộp Đen", "EP & BVA 6 Giá Trị", "Phân vùng tương đương & Phân tích giá trị biên các hàm nghiệp vụ"),
        ("Kiểm Thử Tranh Chấp", "ExecutorService Multi-thread", "Kiểm tra an toàn luồng, chống gian lận rút trùng Double-Spending"),
        ("Kiểm Thử Hiệu Năng", "Apache JMeter", "Mô phỏng 50-100 người dùng ảo, 20.000 mẫu, đo thời gian đáp ứng"),
        ("Tự Động Hóa API", "Postman & Newman CLI", "10 API, 17 Assertions, kiểm thử hồi quy tự động trong CI/CD"),
        ("Tự Động Hóa Giao Diện", "Selenium WebDriver & IDE", "16 trang phân hệ Admin Portal toàn diện, Record & Playback"),
        ("Đánh Giá Chuẩn Quốc Tế", "ISO/IEC 25010 Model", "Đánh giá 7 đặc tính cốt lõi của sản phẩm phần mềm"),
    ]

    for idx, (m_title, m_tool, m_desc) in enumerate(matrix_items):
        row = idx // 2
        col = idx % 2
        l = Inches(0.8 + col * 5.95)
        t = Inches(1.9 + row * 1.25)
        
        box = s4.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, l, t, Inches(5.75), Inches(1.1))
        box.fill.solid()
        box.fill.fore_color.rgb = CARD_BG
        box.line.color.rgb = BORDER_COLOR

        tb = s4.shapes.add_textbox(l + Inches(0.15), t + Inches(0.08), Inches(5.4), Inches(0.95))
        tf = tb.text_frame
        tf.word_wrap = True
        
        p = tf.paragraphs[0]
        p.text = f"{m_title}  •  {m_tool}"
        p.font.size = Pt(13.5)
        p.font.bold = True
        p.font.color.rgb = NAVY

        p2 = tf.add_paragraph()
        p2.text = m_desc
        p2.font.size = Pt(11)
        p2.font.color.rgb = DARK_GRAY
        p2.space_before = Pt(3)

    # =========================================================================
    # SLIDE 5: White-box Testing & JaCoCo Coverage
    # =========================================================================
    s5 = prs.slides.add_slide(blank_layout)
    add_header(s5, "4. Kiểm Thử Hộp Trắng & Độ Bao Phủ Mã Nguồn (JaCoCo)")

    # 2 Columns
    col1 = s5.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(0.8), Inches(1.9), Inches(5.7), Inches(5.0))
    col1.fill.solid()
    col1.fill.fore_color.rgb = CARD_BG
    col1.line.color.rgb = NAVY

    tb1 = s5.shapes.add_textbox(Inches(1.0), Inches(2.1), Inches(5.3), Inches(4.5))
    tf1 = tb1.text_frame
    tf1.word_wrap = True
    p = tf1.paragraphs[0]
    p.text = "Phân Tích Độ Phức Tạp Cyclomatic V(G)"
    p.font.size = Pt(17)
    p.font.bold = True
    p.font.color.rgb = NAVY
    p.space_after = Pt(10)

    points1 = [
        "Xây dựng Đồ thị dòng điều khiển (CFG) phương thức transfer().",
        "Công thức McCabe: V(G) = E - N + 2P = 11 - 9 + 2(1) = 4.",
        "Thiết kế 4 ca kiểm thử độc lập bao phủ 100% nhánh rẽ (True/False):",
        "  • Path 1: Số tiền không hợp lệ (amount <= 0).",
        "  • Path 2: Tài khoản nguồn không đủ số dư.",
        "  • Path 3: Không tìm thấy tài khoản đích hợp lệ.",
        "  • Path 4: Giao dịch thành công, trừ/cộng tiền và ghi nhật ký.",
    ]
    for pt in points1:
        p = tf1.add_paragraph()
        p.text = pt
        p.font.size = Pt(12)
        p.font.color.rgb = DARK_GRAY
        p.space_after = Pt(6)

    # Right column: JaCoCo stats
    col2 = s5.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(6.8), Inches(1.9), Inches(5.7), Inches(5.0))
    col2.fill.solid()
    col2.fill.fore_color.rgb = CARD_BG
    col2.line.color.rgb = GREEN

    tb2 = s5.shapes.add_textbox(Inches(7.0), Inches(2.1), Inches(5.3), Inches(4.5))
    tf2 = tb2.text_frame
    tf2.word_wrap = True
    p = tf2.paragraphs[0]
    p.text = "Kết Quả Đo Lường JaCoCo Coverage"
    p.font.size = Pt(17)
    p.font.bold = True
    p.font.color.rgb = GREEN
    p.space_after = Pt(10)

    stats = [
        ("Line Coverage (Độ bao phủ dòng lệnh)", "90.3%", "Vượt xa chỉ tiêu tối thiểu 85%"),
        ("Branch Coverage (Độ bao phủ nhánh rẽ)", "85.6%", "Bao quát toàn bộ các cấu trúc if-else"),
        ("Tổng số ca kiểm thử Backend", "31 / 31 ca Passed", "20 Unit Tests + 11 Integration Tests"),
        ("Môi trường cơ sở dữ liệu", "H2 In-Memory DB", "Chạy siêu tốc trong RAM, cô lập an toàn"),
    ]
    for label, val, sub in stats:
        p = tf2.add_paragraph()
        p.text = f"{label}: {val}"
        p.font.size = Pt(13)
        p.font.bold = True
        p.font.color.rgb = DARK_BLUE
        
        p_sub = tf2.add_paragraph()
        p_sub.text = f"  ({sub})"
        p_sub.font.size = Pt(11)
        p_sub.font.color.rgb = LIGHT_GRAY
        p_sub.space_after = Pt(8)

    # =========================================================================
    # SLIDE 6: Black-box Testing (EP & BVA)
    # =========================================================================
    s6 = prs.slides.add_slide(blank_layout)
    add_header(s6, "5. Kiểm Thử Hộp Đen Nghiệp Vụ Cốt Lõi (EP & BVA)")

    bb_items = [
        ("Phân Vùng Tương Đương (EP)", [
            "Miền không hợp lệ 1: Số tiền âm (amount < 0) -> Bị từ chối ngay lập tức.",
            "Miền biên ranh giới: Số tiền bằng 0 -> Phát hiện lỗi BUG-01 và đã sửa điều kiện.",
            "Miền hợp lệ: 1 <= amount <= Số dư khả dụng -> Giao dịch chuyển tiền thành công.",
            "Miền không hợp lệ 2: Số tiền vượt số dư khả dụng -> Bị từ chối do thiếu tiền.",
        ], Inches(0.8)),
        ("Phân Tích Giá Trị Biên (BVA 6 Giá Trị)", [
            "Xét 6 giá trị xung quanh biên Min & Max: [min-1, min, min+1, max-1, max, max+1].",
            "Biên Min (Số tiền = 1 VNĐ): Kiểm thử giao dịch hạn mức nhỏ nhất.",
            "Biên Max (Số dư = 10.000.000 VNĐ): Kiểm thử giao dịch rút trọn số dư.",
            "Biên vượt (Số dư + 1 VNĐ): Kiểm thử chặn giao dịch vượt quá 1 đồng.",
        ], Inches(6.8))
    ]

    for title, pts, l in bb_items:
        c = s6.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, l, Inches(1.9), Inches(5.7), Inches(5.0))
        c.fill.solid()
        c.fill.fore_color.rgb = CARD_BG
        c.line.color.rgb = NAVY
        c.line.width = Pt(2)

        tb = s6.shapes.add_textbox(l + Inches(0.2), Inches(2.1), Inches(5.3), Inches(4.5))
        tf = tb.text_frame
        tf.word_wrap = True
        p = tf.paragraphs[0]
        p.text = title
        p.font.size = Pt(17)
        p.font.bold = True
        p.font.color.rgb = NAVY
        p.space_after = Pt(14)

        for pt in pts:
            p = tf.add_paragraph()
            p.text = "• " + pt
            p.font.size = Pt(12)
            p.font.color.rgb = DARK_GRAY
            p.space_after = Pt(12)

    # =========================================================================
    # SLIDE 7: Concurrency & Double-Spending Prevention
    # =========================================================================
    s7 = prs.slides.add_slide(blank_layout)
    add_header(s7, "6. Kiểm Thử Tranh Chấp Đồng Thời (Pessimistic Locking)")

    # Problem vs Solution layout
    p_box = s7.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(0.8), Inches(1.9), Inches(5.7), Inches(5.0))
    p_box.fill.solid()
    p_box.fill.fore_color.rgb = CARD_BG
    p_box.line.color.rgb = ORANGE
    p_box.line.width = Pt(2)

    tb = s7.shapes.add_textbox(Inches(1.0), Inches(2.1), Inches(5.3), Inches(4.5))
    tf = tb.text_frame
    tf.word_wrap = True
    p = tf.paragraphs[0]
    p.text = "Lỗ Hổng Tranh Chấp Race Condition"
    p.font.size = Pt(17)
    p.font.bold = True
    p.font.color.rgb = ORANGE
    p.space_after = Pt(12)

    pts = [
        "Hiểm họa tài chính: Kẻ gian gửi nhiều lệnh rút tiền trong cùng 1 mili-giây.",
        "Thiết kế kiểm thử: Tài khoản có 10.000.000 VNĐ.",
        "Phát lệnh đồng thời: 10 luồng, mỗi luồng rút 2.000.000 VNĐ qua CountDownLatch.",
        "Rủi ro nếu không có khóa: Cả 10 luồng cùng đọc số dư cũ, gây lỗi rút quá tiền (Double-Spending) thất thoát 10 triệu đồng.",
    ]
    for pt in pts:
        p = tf.add_paragraph()
        p.text = "• " + pt
        p.font.size = Pt(12)
        p.font.color.rgb = DARK_GRAY
        p.space_after = Pt(10)

    # Solution
    s_box = s7.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(6.8), Inches(1.9), Inches(5.7), Inches(5.0))
    s_box.fill.solid()
    s_box.fill.fore_color.rgb = CARD_BG
    s_box.line.color.rgb = GREEN
    s_box.line.width = Pt(2)

    tb2 = s7.shapes.add_textbox(Inches(7.0), Inches(2.1), Inches(5.3), Inches(4.5))
    tf2 = tb2.text_frame
    tf2.word_wrap = True
    p = tf2.paragraphs[0]
    p.text = "Giải Pháp Phòng Thủ: Khóa Bi Quan CSDL"
    p.font.size = Pt(17)
    p.font.bold = True
    p.font.color.rgb = GREEN
    p.space_after = Pt(12)

    pts2 = [
        "Áp dụng khóa bi quan @Lock(LockModeType.PESSIMISTIC_WRITE).",
        "Sinh câu lệnh SQL: SELECT ... FOR UPDATE khóa hàng dữ liệu.",
        "Cưỡng chế tuần tự hóa các giao dịch rút tiền trong hàng đợi.",
        "KẾT QUẢ THỰC NGHIỆM:",
        "  ✔ Đúng 5 giao dịch đầu tiên thành công (Trừ đủ 10 triệu).",
        "  ✔ 5 giao dịch sau bị từ chối do số dư không đủ.",
        "  ✔ Số dư cuối cùng chính xác tuyệt đối = 0 VNĐ.",
    ]
    for pt in pts2:
        p = tf2.add_paragraph()
        p.text = pt
        p.font.size = Pt(12)
        p.font.color.rgb = DARK_GRAY
        p.space_after = Pt(6)

    # =========================================================================
    # SLIDE 8: Performance Testing (JMeter)
    # =========================================================================
    s8 = prs.slides.add_slide(blank_layout)
    add_header(s8, "7. Kiểm Thử Tải & Năng Lực Hiệu Năng (Apache JMeter)")

    j_stats = [
        ("POST /api/v1/auth/login", "5.000", "65 ms", "0.00%", "238.4 req/s"),
        ("GET /api/v1/accounts/my-balance", "5.000", "24 ms", "0.00%", "412.8 req/s"),
        ("POST /api/v1/transfers", "5.000", "92 ms", "0.00%", "184.2 req/s"),
        ("GET /api/v1/transactions/history", "5.000", "38 ms", "0.00%", "325.6 req/s"),
    ]

    # Summary Card Top
    top_c = s8.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(0.8), Inches(1.9), Inches(11.733), Inches(1.2))
    top_c.fill.solid()
    top_c.fill.fore_color.rgb = CARD_BG
    top_c.line.color.rgb = CYAN
    top_c.line.width = Pt(2)

    tb = s8.shapes.add_textbox(Inches(1.0), Inches(2.05), Inches(11.3), Inches(0.9))
    tf = tb.text_frame
    tf.word_wrap = True
    p = tf.paragraphs[0]
    p.text = "TỔNG HỢP HIỆU NĂNG TOÀN HỆ THỐNG: 20.000 GIAO DỊCH DƯỚI TẢI 100 VU"
    p.font.size = Pt(15)
    p.font.bold = True
    p.font.color.rgb = NAVY

    p2 = tf.add_paragraph()
    p2.text = "Thời gian phản hồi trung bình: 54.7 ms (Chuẩn < 200 ms)  •  Tỷ lệ lỗi: 0.00%  •  Thông lượng: 290.25 req/s"
    p2.font.size = Pt(13)
    p2.font.bold = True
    p2.font.color.rgb = GREEN
    p2.space_before = Pt(4)

    # 4 API cards
    for idx, (api, samp, lat, err, thr) in enumerate(j_stats):
        top_pos = Inches(3.3 + idx * 0.95)
        b = s8.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(0.8), top_pos, Inches(11.733), Inches(0.85))
        b.fill.solid()
        b.fill.fore_color.rgb = CARD_BG
        b.line.color.rgb = BORDER_COLOR

        tb = s8.shapes.add_textbox(Inches(1.0), top_pos + Inches(0.12), Inches(11.3), Inches(0.6))
        tf = tb.text_frame
        tf.word_wrap = True
        p = tf.paragraphs[0]
        p.text = f"{api}  |  Mẫu: {samp}  |  Độ trễ TB: {lat}  |  Lỗi: {err}  |  Thông lượng: {thr}"
        p.font.size = Pt(12.5)
        p.font.bold = True
        p.font.color.rgb = DARK_BLUE

    # =========================================================================
    # SLIDE 9: Postman & Newman API Automation
    # =========================================================================
    s9 = prs.slides.add_slide(blank_layout)
    add_header(s9, "8. Tự Động Hóa Kiểm Thử API (Postman / Newman)")

    # Left: Explanation & Results
    tb = s9.shapes.add_textbox(Inches(0.8), Inches(1.9), Inches(5.8), Inches(5.0))
    tf = tb.text_frame
    tf.word_wrap = True
    p = tf.paragraphs[0]
    p.text = "Bộ Kịch Bản Tự Động Hóa API Postman"
    p.font.size = Pt(16)
    p.font.bold = True
    p.font.color.rgb = NAVY
    p.space_after = Pt(10)

    p_points = [
        "Gồm 10 API cốt lõi chia thành 4 phân nhóm chức năng.",
        "17 điều kiện Assertions kiểm tra Status Code, JWT, JSON Schema.",
        "KẾT QUẢ THỰC NGHIỆM:",
        "  ✔ 10/10 Requests Passed tuyệt đối 100%.",
        "  ✔ 17/17 Assertions Passed (0 thất bại).",
        "  ✔ Thời gian phản hồi trung bình: 38 ms.",
        "  ✔ Thời gian chạy toàn bộ bộ test: 1s 348ms.",
        "Chạy tự động không cần cài đặt qua Newman CLI trên CI/CD:",
        "cmd /c 'npx newman run MiniBank_API_Automation.postman_collection.json'",
    ]
    for pt in p_points:
        p = tf.add_paragraph()
        p.text = pt
        p.font.size = Pt(11.5)
        p.font.color.rgb = DARK_GRAY
        p.space_after = Pt(4)

    # Right: Embedded Runner Image
    img_runner = r"d:\Code_Hoc\Kiemthu_MiniBank\docs\figures\hinh_3_4b_postman_runner.png"
    if os.path.exists(img_runner):
        s9.shapes.add_picture(img_runner, Inches(6.8), Inches(1.9), width=Inches(5.7))

    # =========================================================================
    # SLIDE 10: DEMO TRANSITION 1: POSTMAN & NEWMAN
    # =========================================================================
    s10 = prs.slides.add_slide(blank_layout)
    bg10 = s10.shapes.add_shape(MSO_SHAPE.RECTANGLE, 0, 0, Inches(13.333), Inches(7.5))
    bg10.fill.solid()
    bg10.fill.fore_color.rgb = ORANGE
    bg10.line.fill.background()

    tb10 = s10.shapes.add_textbox(Inches(1.5), Inches(1.8), Inches(10.333), Inches(4.0))
    tf10 = tb10.text_frame
    tf10.word_wrap = True

    p = tf10.paragraphs[0]
    p.text = "🚀 PHẦN TRÌNH DIỄN THỰC NGHIỆM 1"
    p.font.size = Pt(18)
    p.font.bold = True
    p.font.color.rgb = RGBColor(0xFF, 0xEB, 0x3B)
    p.space_after = Pt(14)

    p = tf10.add_paragraph()
    p.text = "LIVE DEMO: KIỂM THỬ TỰ ĐỘNG HÓA API\nPOSTMAN RUNNER & NEWMAN CLI"
    p.font.size = Pt(32)
    p.font.bold = True
    p.font.color.rgb = WHITE
    p.space_after = Pt(20)

    p = tf10.add_paragraph()
    p.text = "Nội dung thực hiện:\n1. Chạy tự động Collection Runner trên Postman Desktop (17/17 Passed).\n2. Gửi request TC_API_ADM_01 kiểm tra cấp phát Token JWT (200 OK).\n3. Gửi request TC_API_SEC_01 kiểm tra chặn truy cập trái phép (401 Unauthorized).\n4. Chạy lệnh dòng lệnh tự động siêu tốc với Newman CLI trên Terminal."
    p.font.size = Pt(14)
    p.font.color.rgb = WHITE

    # =========================================================================
    # SLIDE 11: Selenium WebDriver UI Automation
    # =========================================================================
    s11 = prs.slides.add_slide(blank_layout)
    add_header(s11, "9. Kiểm Thử Giao Diện Người Dùng (Selenium WebDriver)")

    ui_col1 = [
        ("TC_PAGE_01: Đăng nhập Quản trị", "/login", "Kiểm tra form, input, submit button (2.1s)"),
        ("TC_PAGE_02: Bắt lỗi đăng nhập", "/login", "Gửi sai mật khẩu, kiểm tra báo lỗi (2.4s)"),
        ("TC_PAGE_03: Chuyển hướng Dashboard", "/", "Cấp token, chuyển trang sau đăng nhập (3.8s)"),
        ("TC_PAGE_04: Dashboard KPI", "/", "Kiểm tra render 4 thẻ số liệu tài chính (2.2s)"),
        ("TC_PAGE_05: Duyệt khoản vay", "/requests/loan-approval", "Bảng hồ sơ vay PENDING, nút duyệt (2.3s)"),
        ("TC_PAGE_06: Duyệt sổ tiết kiệm", "/requests/saving-approval", "Bảng sổ chờ giải ngân và duyệt (2.2s)"),
        ("TC_PAGE_07: Quản lý khách hàng", "/customers", "Danh sách khách hàng, tìm kiếm SĐT (2.3s)"),
        ("TC_PAGE_08: Thẩm định eKYC", "/customers/kyc", "Đối soát CMND/CCCD và ảnh chân dung (2.2s)"),
    ]
    ui_col2 = [
        ("TC_PAGE_09: Hồ sơ chứng từ", "/customers/documents", "Lưu trữ và tải về hợp đồng tín dụng (2.1s)"),
        ("TC_PAGE_10: Sản phẩm vay vốn", "/financial-products/loans", "Cấu hình gói vay, hạn mức, lãi suất (2.2s)"),
        ("TC_PAGE_11: Sản phẩm tiết kiệm", "/financial-products/savings", "Biểu lãi suất các kỳ hạn 1-24 tháng (2.1s)"),
        ("TC_PAGE_12: Duyệt giao dịch lớn", "/transactions/large-approval", "Kiểm soát kép Dual-Control AML (2.3s)"),
        ("TC_PAGE_13: Quản lý cán bộ", "/staff", "Danh sách nhân sự, phân công chi nhánh (2.2s)"),
        ("TC_PAGE_14: Phân quyền RBAC", "/system/roles", "Ma trận quyền ADMIN, TELLER, COMPLIANCE (2.2s)"),
        ("TC_PAGE_15: Nhật ký kiểm toán", "/system/audit", "Audit Trail ghi nhận IP, thời gian (2.3s)"),
        ("TC_PAGE_16: Đổi mật khẩu", "/change-password", "Chính sách mật khẩu phức tạp ngân hàng (2.1s)"),
    ]

    for idx, (t, r, d) in enumerate(ui_col1):
        top_pos = Inches(1.8 + idx * 0.65)
        tb = s11.shapes.add_textbox(Inches(0.8), top_pos, Inches(5.8), Inches(0.6))
        tf = tb.text_frame
        tf.word_wrap = True
        p = tf.paragraphs[0]
        p.text = f"✔ {t} ({r}) - {d}"
        p.font.size = Pt(10.5)
        p.font.color.rgb = DARK_BLUE

    for idx, (t, r, d) in enumerate(ui_col2):
        top_pos = Inches(1.8 + idx * 0.65)
        tb = s11.shapes.add_textbox(Inches(6.8), top_pos, Inches(5.8), Inches(0.6))
        tf = tb.text_frame
        tf.word_wrap = True
        p = tf.paragraphs[0]
        p.text = f"✔ {t} ({r}) - {d}"
        p.font.size = Pt(10.5)
        p.font.color.rgb = DARK_BLUE

    # Footer banner on slide 11
    foot = s11.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(0.8), Inches(6.8), Inches(11.733), Inches(0.5))
    foot.fill.solid()
    foot.fill.fore_color.rgb = GREEN
    foot.line.fill.background()
    tb_f = s11.shapes.add_textbox(Inches(1.0), Inches(6.85), Inches(11.3), Inches(0.4))
    p = tb_f.text_frame.paragraphs[0]
    p.text = "KẾT QUẢ THỰC THI TỰ ĐỘNG: 16/16 TRANG PASSED TRONG 72.95 GIÂY (PASS RATE 100%)"
    p.font.size = Pt(12)
    p.font.bold = True
    p.font.color.rgb = WHITE

    # =========================================================================
    # SLIDE 12: Visual UI Dashboards & Modules
    # =========================================================================
    s12 = prs.slides.add_slide(blank_layout)
    add_header(s12, "10. Giao Diện Trực Quan Các Phân Hệ Cốt Lõi (Admin Portal)")

    img_dash = r"d:\Code_Hoc\Kiemthu_MiniBank\docs\figures\hinh_3_9_dashboard.png"
    img_loan = r"d:\Code_Hoc\Kiemthu_MiniBank\docs\figures\hinh_3_10_loan_approval.png"
    if os.path.exists(img_dash) and os.path.exists(img_loan):
        s12.shapes.add_picture(img_dash, Inches(0.8), Inches(1.9), width=Inches(5.7))
        s12.shapes.add_picture(img_loan, Inches(6.8), Inches(1.9), width=Inches(5.7))

    cap1 = s12.shapes.add_textbox(Inches(0.8), Inches(5.3), Inches(5.7), Inches(0.5))
    cap1.text_frame.paragraphs[0].text = "Hình 3.9: Bảng điều khiển quản trị trung tâm (Dashboard /)"
    cap1.text_frame.paragraphs[0].font.size = Pt(11)
    cap1.text_frame.paragraphs[0].font.bold = True
    cap1.text_frame.paragraphs[0].font.color.rgb = NAVY

    cap2 = s12.shapes.add_textbox(Inches(6.8), Inches(5.3), Inches(5.7), Inches(0.5))
    cap2.text_frame.paragraphs[0].text = "Hình 3.10: Phê duyệt hồ sơ vay vốn (/requests/loan-approval)"
    cap2.text_frame.paragraphs[0].font.size = Pt(11)
    cap2.text_frame.paragraphs[0].font.bold = True
    cap2.text_frame.paragraphs[0].font.color.rgb = NAVY

    # =========================================================================
    # SLIDE 13: DEMO TRANSITION 2: SELENIUM & SELENIUM IDE
    # =========================================================================
    s13 = prs.slides.add_slide(blank_layout)
    bg13 = s13.shapes.add_shape(MSO_SHAPE.RECTANGLE, 0, 0, Inches(13.333), Inches(7.5))
    bg13.fill.solid()
    bg13.fill.fore_color.rgb = CYAN
    bg13.line.fill.background()

    tb13 = s13.shapes.add_textbox(Inches(1.5), Inches(1.8), Inches(10.333), Inches(4.0))
    tf13 = tb13.text_frame
    tf13.word_wrap = True

    p = tf13.paragraphs[0]
    p.text = "🚀 PHẦN TRÌNH DIỄN THỰC NGHIỆM 2"
    p.font.size = Pt(18)
    p.font.bold = True
    p.font.color.rgb = RGBColor(0xFF, 0xEB, 0x3B)
    p.space_after = Pt(14)

    p = tf13.add_paragraph()
    p.text = "LIVE DEMO: KIỂM THỬ TỰ ĐỘNG HÓA GIAO DIỆN\nSELENIUM WEBDRIVER & SELENIUM IDE"
    p.font.size = Pt(32)
    p.font.bold = True
    p.font.color.rgb = WHITE
    p.space_after = Pt(20)

    p = tf13.add_paragraph()
    p.text = "Nội dung thực hiện:\n1. Thực thi kịch bản Python tự động kiểm thử 16 trang giao diện Admin Portal.\n2. Mở tiện ích Selenium IDE Runner (.side) quan sát Record & Playback trực quan.\n3. Xác minh tự động điền form, kiểm tra thẻ DOM và điều hướng an toàn."
    p.font.size = Pt(14)
    p.font.color.rgb = WHITE

    # =========================================================================
    # SLIDE 14: Defect Log & Resolution
    # =========================================================================
    s14 = prs.slides.add_slide(blank_layout)
    add_header(s14, "11. Nhật Ký Phát Hiện & Xử Lý Lỗi Phần Mềm (Defect Log)")

    bugs = [
        ("BUG-01", "Chuyển tiền", "Cho phép chuyển số tiền bằng 0", "CRITICAL", "Cập nhật điều kiện if (amount <= 0)"),
        ("BUG-02", "Tín dụng", "Cho phép nộp đơn xin vay số tiền âm", "MAJOR", "Bổ sung tiền kiểm tra loanAmount <= 0"),
        ("BUG-03", "Tín dụng", "Duyệt vay nhưng không cộng tiền giải ngân", "CRITICAL", "Bổ sung cộng số dư tài khoản người vay"),
        ("BUG-04", "Tài khoản", "Double-Spending khi rút tiền đồng thời", "BLOCKER", "Áp dụng Khóa bi quan PESSIMISTIC_WRITE"),
        ("BUG-05", "Giao diện", "Trang Audit Log cho phép truy cập nặc danh", "HIGH", "Tích hợp Client-side Route Guard"),
        ("BUG-06", "Kết nối", "Fallback port 8081 gây lỗi kết nối API", "MEDIUM", "Chuẩn hóa trỏ về cổng backend 8080"),
    ]

    for idx, (b_id, mod, desc, sev, fix) in enumerate(bugs):
        t_pos = Inches(1.8 + idx * 0.85)
        box = s14.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(0.8), t_pos, Inches(11.733), Inches(0.75))
        box.fill.solid()
        box.fill.fore_color.rgb = CARD_BG
        col = ORANGE if sev in ["CRITICAL", "BLOCKER"] else CYAN
        box.line.color.rgb = col
        box.line.width = Pt(1.5)

        tb = s14.shapes.add_textbox(Inches(1.0), t_pos + Inches(0.08), Inches(11.3), Inches(0.6))
        tf = tb.text_frame
        tf.word_wrap = True
        p = tf.paragraphs[0]
        p.text = f"[{b_id}] ({sev}) {mod}: {desc}  ➔  Giải pháp: {fix} [TRẠNG THÁI: CLOSED ✔]"
        p.font.size = Pt(12)
        p.font.bold = True
        p.font.color.rgb = DARK_BLUE

    # =========================================================================
    # SLIDE 15: ISO/IEC 25010 Quality Evaluation
    # =========================================================================
    s15 = prs.slides.add_slide(blank_layout)
    add_header(s15, "12. Đánh Giá Chất Lượng Theo Chuẩn ISO/IEC 25010")

    iso_items = [
        ("1. Tính Phù Hợp Chức Năng", "5.0 / 5.0", "57/57 ca test tự động thành công (Pass Rate 100%)"),
        ("2. Hiệu Quả Hoạt Động", "4.8 / 5.0", "Thời gian đáp ứng TB 54.7 ms dưới tải 100 người dùng ảo"),
        ("3. Tính Tương Thích", "4.7 / 5.0", "Hiển thị chuẩn mực trên Chrome, Edge, Firefox, responsive"),
        ("4. Tính Khả Dụng", "4.8 / 5.0", "Giao diện tối giản hiện đại, cán bộ làm quen dưới 5 phút"),
        ("5. Độ Tin Cậy", "5.0 / 5.0", "Khóa bi quan ngăn 100% Double-Spending, tỷ lệ lỗi 0.00%"),
        ("6. Tính Bảo Mật", "4.9 / 5.0", "Mật khẩu mã hóa BCrypt, JWT Bearer Token, Route Guard"),
        ("7. Khả Năng Bảo Trì", "4.9 / 5.0", "Mã nguồn phân tầng chuẩn mực, JaCoCo Coverage đạt 90.3%"),
    ]

    for idx, (name, score, desc) in enumerate(iso_items):
        t_pos = Inches(1.8 + idx * 0.72)
        box = s15.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(0.8), t_pos, Inches(11.733), Inches(0.65))
        box.fill.solid()
        box.fill.fore_color.rgb = CARD_BG
        box.line.color.rgb = GREEN
        box.line.width = Pt(1.5)

        tb = s15.shapes.add_textbox(Inches(1.0), t_pos + Inches(0.06), Inches(11.3), Inches(0.5))
        tf = tb.text_frame
        tf.word_wrap = True
        p = tf.paragraphs[0]
        p.text = f"{name}:  ĐIỂM ĐÁNH GIÁ: {score}  |  {desc}"
        p.font.size = Pt(12)
        p.font.bold = True
        p.font.color.rgb = DARK_BLUE

    # =========================================================================
    # SLIDE 16: Summary & Future Work
    # =========================================================================
    s16 = prs.slides.add_slide(blank_layout)
    add_header(s16, "13. Tổng Kết Thành Quả & Định Hướng Phát Triển")

    c1 = s16.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(0.8), Inches(1.9), Inches(5.7), Inches(5.0))
    c1.fill.solid()
    c1.fill.fore_color.rgb = CARD_BG
    c1.line.color.rgb = GREEN
    c1.line.width = Pt(2)

    tb1 = s16.shapes.add_textbox(Inches(1.0), Inches(2.1), Inches(5.3), Inches(4.5))
    tf1 = tb1.text_frame
    tf1.word_wrap = True
    p = tf1.paragraphs[0]
    p.text = "Thành Quả Đạt Được"
    p.font.size = Pt(17)
    p.font.bold = True
    p.font.color.rgb = GREEN
    p.space_after = Pt(12)

    achieve = [
        "Hoàn thành 100% mục tiêu đề tài theo chuẩn Đại học Phenikaa.",
        "Thiết lập thành công 57 ca kiểm thử tự động đạt tỷ lệ 100% Passed.",
        "Độ bao phủ mã nguồn JaCoCo đạt 90.3% dòng lệnh và 85.6% nhánh rẽ.",
        "Hiệu năng chịu tải đạt tốc độ 54.7 ms với tỷ lệ lỗi giao dịch 0.00%.",
        "Xử lý dứt điểm 6 lỗi quan trọng, đóng 100% các defect phát hiện.",
    ]
    for pt in achieve:
        p = tf1.add_paragraph()
        p.text = "✔ " + pt
        p.font.size = Pt(12.5)
        p.font.color.rgb = DARK_GRAY
        p.space_after = Pt(10)

    c2 = s16.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(6.8), Inches(1.9), Inches(5.7), Inches(5.0))
    c2.fill.solid()
    c2.fill.fore_color.rgb = CARD_BG
    c2.line.color.rgb = NAVY
    c2.line.width = Pt(2)

    tb2 = s16.shapes.add_textbox(Inches(7.0), Inches(2.1), Inches(5.3), Inches(4.5))
    tf2 = tb2.text_frame
    tf2.word_wrap = True
    p = tf2.paragraphs[0]
    p.text = "Định Hướng Phát Triển Tiếp Theo"
    p.font.size = Pt(17)
    p.font.bold = True
    p.font.color.rgb = NAVY
    p.space_after = Pt(12)

    future = [
        "Tích hợp đường ống CI/CD tự động (GitHub Actions / Jenkins).",
        "Mở rộng kịch bản kiểm thử API nâng cao cho toàn bộ luồng nghiệp vụ.",
        "Nâng cấp xác thực hai yếu tố (2FA OTP) & sinh trắc học eKYC nâng cao.",
        "Triển khai Redis Cache phân tán đáp ứng hàng triệu người dùng.",
        "Kiểm thử giao diện chéo trình duyệt qua Selenium Grid.",
    ]
    for pt in future:
        p = tf2.add_paragraph()
        p.text = "➔ " + pt
        p.font.size = Pt(12.5)
        p.font.color.rgb = DARK_GRAY
        p.space_after = Pt(10)

    # =========================================================================
    # SLIDE 17: Closing & Q&A
    # =========================================================================
    s17 = prs.slides.add_slide(blank_layout)
    bg17 = s17.shapes.add_shape(MSO_SHAPE.RECTANGLE, 0, 0, Inches(13.333), Inches(7.5))
    bg17.fill.solid()
    bg17.fill.fore_color.rgb = NAVY
    bg17.line.fill.background()

    tb17 = s17.shapes.add_textbox(Inches(1.5), Inches(2.0), Inches(10.333), Inches(3.5))
    tf17 = tb17.text_frame
    tf17.word_wrap = True

    p = tf17.paragraphs[0]
    p.text = "XIN TRÂN TRỌNG CẢM ƠN THẦY CÔ VÀ CÁC BẠN!"
    p.font.size = Pt(32)
    p.font.bold = True
    p.font.color.rgb = WHITE
    p.space_after = Pt(20)

    p = tf17.add_paragraph()
    p.text = "Kính chúc Quý Thầy/Cô dồi dào sức khỏe và công tác tốt!\nNhóm chúng em sẵn sàng lắng nghe các ý kiến đóng góp và câu hỏi từ Hội đồng."
    p.font.size = Pt(16)
    p.font.color.rgb = RGBColor(0x80, 0xD8, 0xFF)
    p.space_after = Pt(24)

    p = tf17.add_paragraph()
    p.text = "PHIÊN HỎI ĐÁP & PHẢN BIỆN (Q&A SESSION)"
    p.font.size = Pt(20)
    p.font.bold = True
    p.font.color.rgb = RGBColor(0xFF, 0xD5, 0x4F)

    # Save presentations
    out_paths = [
        r"d:\Code_Hoc\Kiemthu_MiniBank\SLIDE_THUYET_TRINH_MINIBANK.pptx",
        r"d:\Code_Hoc\Kiemthu_MiniBank\docs\presentation\SLIDE_THUYET_TRINH_MINIBANK.pptx",
        r"C:\Users\pc\.gemini\antigravity\brain\829d45e9-1360-42d7-95d8-c1b176e3d1d4\SLIDE_THUYET_TRINH_MINIBANK.pptx"
    ]
    for p in out_paths:
        os.makedirs(os.path.dirname(p), exist_ok=True)
        prs.save(p)
        print(f"Generated successfully: {p} ({os.path.getsize(p)} bytes)")

if __name__ == "__main__":
    create_deck()
