# -*- coding: utf-8 -*-
import os
import re

FILES = [
    r"d:\Code_Hoc\Kiemthu_MiniBank\BAO_CAO_KIEM_DINH_CHAT_LUONG_PHAN_MEM_MINIBANK.md",
    r"d:\Code_Hoc\Kiemthu_MiniBank\docs\final_report\BAO_CAO_KIEM_DINH_CHAT_LUONG_PHAN_MEM_MINIBANK.md"
]

for fp in FILES:
    if not os.path.exists(fp):
        continue
    with open(fp, "r", encoding="utf-8") as f:
        text = f.read()

    # 1. Update ISO 25010 functional suitability line
    text = text.replace(
        "* Toàn bộ 41 ca kiểm thử chức năng (31 Backend + 10 Frontend) đều vượt qua thành công với kết quả chính xác tuyệt đối.",
        "* Toàn bộ 57 ca kiểm thử tự động hóa (31 ca Backend, 10 ca API Postman/Newman và 16 ca giao diện Selenium WebDriver) đều vượt qua thành công với kết quả chính xác tuyệt đối (Tỷ lệ đạt 100%)."
    )

    # 2. Update conclusion item 3
    text = text.replace(
        "3. Đã phát hiện và xử lý dứt điểm 6 lỗi logic nghiêm trọng, nâng tỷ lệ đạt ca kiểm thử lên **100% (41/41 ca test)** và độ bao phủ mã nguồn lên **90.3%**.",
        "3. Đã phát hiện và xử lý dứt điểm 6 lỗi logic và bảo mật nghiêm trọng, nâng tỷ lệ đạt ca kiểm thử lên **100% (57/57 ca test tự động)** và độ bao phủ mã nguồn lên **90.3%**."
    )

    # 3. Update CI/CD recommendation item 1
    text = text.replace(
        "1. **Tích hợp đường ống CI/CD tự động:** Thiết lập GitHub Actions hoặc Jenkins Pipeline để tự động kích hoạt bộ kiểm thử 41 ca test và kiểm tra chất lượng mã nguồn (SonarQube) sau mỗi lệnh đẩy mã (Git Push).",
        "1. **Tích hợp đường ống CI/CD tự động:** Thiết lập GitHub Actions hoặc Jenkins Pipeline để tự động kích hoạt toàn bộ 57 ca kiểm thử tự động (Maven test, Newman CLI, Selenium Headless) và kiểm tra chất lượng mã nguồn (SonarQube) sau mỗi lệnh đẩy mã (Git Push)."
    )

    with open(fp, "w", encoding="utf-8") as f:
        f.write(text)
    print(f"Updated Section 4 in {fp} successfully!")
