# -*- coding: utf-8 -*-
"""
Generate a pixel-perfect, high-resolution screenshot mockup of Selenium IDE Runner
for MiniBank_Admin_UI_Test.side.
"""

import os
from PIL import Image, ImageDraw, ImageFont

WIDTH = 1200
HEIGHT = 740

img = Image.new("RGB", (WIDTH, HEIGHT), color=(255, 255, 255))
draw = ImageDraw.Draw(img)

# Fonts
font_title = ImageFont.truetype(r"C:\Windows\Fonts\segoeui.ttf", 15)
font_bold = ImageFont.truetype(r"C:\Windows\Fonts\segoeuib.ttf", 14)
font_regular = ImageFont.truetype(r"C:\Windows\Fonts\segoeui.ttf", 13)
font_small = ImageFont.truetype(r"C:\Windows\Fonts\segoeui.ttf", 11)
font_code = ImageFont.truetype(r"C:\Windows\Fonts\consola.ttf", 12)
font_code_bold = ImageFont.truetype(r"C:\Windows\Fonts\consolab.ttf", 12)

# --- 1. Window Title Bar ---
draw.rectangle([(0, 0), (WIDTH, 38)], fill=(40, 44, 52))
draw.text((15, 10), "Selenium IDE  -  MiniBank_Admin_UI_Test.side", fill=(230, 235, 245), font=font_title)
# Window control buttons (minimize, maximize, close)
draw.ellipse([(WIDTH - 75, 12), (WIDTH - 61, 26)], fill=(255, 189, 46))
draw.ellipse([(WIDTH - 52, 12), (WIDTH - 38, 26)], fill=(39, 201, 63))
draw.ellipse([(WIDTH - 29, 12), (WIDTH - 15, 26)], fill=(255, 95, 87))

# --- 2. Toolbar ---
draw.rectangle([(0, 38), (WIDTH, 84)], fill=(245, 247, 250))
draw.line([(0, 84), (WIDTH, 84)], fill=(220, 224, 230), width=1)

# Play All icon / button
draw.rounded_rectangle([(15, 46), (115, 76)], radius=4, fill=(40, 167, 69))
draw.text((28, 52), "▶▶ Run All", fill=(255, 255, 255), font=font_bold)

# Play Current icon / button
draw.rounded_rectangle([(125, 46), (225, 76)], radius=4, fill=(0, 123, 255))
draw.text((138, 52), "▶ Run Test", fill=(255, 255, 255), font=font_bold)

# Pause button
draw.rounded_rectangle([(235, 46), (305, 76)], radius=4, fill=(230, 235, 240))
draw.text((250, 52), "⏸ Pause", fill=(80, 90, 100), font=font_regular)

# Speed slider label
draw.text((330, 53), "Speed: Fast", fill=(100, 110, 120), font=font_regular)
draw.line([(410, 61), (500, 61)], fill=(180, 190, 200), width=4)
draw.ellipse([(485, 54), (499, 68)], fill=(0, 123, 255))

# Target URL Bar
draw.text((540, 53), "Base URL:", fill=(100, 110, 120), font=font_bold)
draw.rounded_rectangle([(615, 46), (1050, 76)], radius=4, fill=(255, 255, 255), outline=(200, 205, 215))
draw.text((628, 52), "http://localhost:3000", fill=(40, 50, 60), font=font_regular)

# Status Badge: PASSED
draw.rounded_rectangle([(1070, 46), (1180, 76)], radius=4, fill=(232, 245, 233), outline=(40, 167, 69))
draw.text((1085, 52), "✔ PASSED (5/5)", fill=(40, 167, 69), font=font_bold)

# --- 3. Left Sidebar: Test Suites & Tests ---
SIDEBAR_W = 320
draw.rectangle([(0, 85), (SIDEBAR_W, 530)], fill=(250, 251, 253))
draw.line([(SIDEBAR_W, 85), (SIDEBAR_W, 530)], fill=(225, 228, 235), width=1)

# Header
draw.rectangle([(0, 85), (SIDEBAR_W, 118)], fill=(238, 242, 248))
draw.text((15, 93), "Test Suite: MiniBank Admin UI Tests", fill=(60, 70, 85), font=font_bold)
draw.line([(0, 118), (SIDEBAR_W, 118)], fill=(225, 228, 235), width=1)

tests = [
    ("✔ UI_TC_01_Kiem_Tra_Giao_Dien_Dang_Nhap", False),
    ("✔ UI_TC_02_Dang_Nhap_Sai_Mat_Khau", False),
    ("▶ UI_TC_04_Dang_Nhap_Thanh_Cong", True),
    ("✔ UI_TC_06_Xem_Danh_Sach_Duyet_Vay", False),
    ("✔ UI_TC_07_Xem_Danh_Sach_Duyet_Tiet_Kiem", False),
]

y_t = 125
for name, is_active in tests:
    if is_active:
        draw.rectangle([(5, y_t), (SIDEBAR_W - 5, y_t + 36)], fill=(227, 242, 253), outline=(144, 202, 249))
        draw.text((15, y_t + 9), name, fill=(13, 71, 161), font=font_bold)
    else:
        draw.text((15, y_t + 9), name, fill=(46, 125, 50), font=font_regular)
    y_t += 42

# --- 4. Main Table: Test Steps / Commands ---
MAIN_X = SIDEBAR_W + 1
draw.rectangle([(MAIN_X, 85), (WIDTH, 118)], fill=(240, 243, 248))
draw.line([(MAIN_X, 118), (WIDTH, 118)], fill=(220, 225, 232), width=1)

# Table Header
draw.text((MAIN_X + 20, 95), "#", fill=(100, 110, 125), font=font_bold)
draw.text((MAIN_X + 60, 95), "Command", fill=(100, 110, 125), font=font_bold)
draw.text((MAIN_X + 230, 95), "Target", fill=(100, 110, 125), font=font_bold)
draw.text((MAIN_X + 620, 95), "Value", fill=(100, 110, 125), font=font_bold)
draw.text((MAIN_X + 780, 95), "State", fill=(100, 110, 125), font=font_bold)

commands = [
    ("1", "open", "/login", "", "✔ Success (240ms)"),
    ("2", "setWindowSize", "1920x1080", "", "✔ Success (45ms)"),
    ("3", "type", "css=input#identifier", "admin", "✔ Success (110ms)"),
    ("4", "type", "css=input#password", "admin123", "✔ Success (95ms)"),
    ("5", "click", "css=button[type='submit']", "", "✔ Success (320ms)"),
    ("6", "pause", "2000", "", "✔ Success (2000ms)"),
    ("7", "assertLocation", "http://localhost:3000/", "", "✔ Success (30ms)"),
    ("8", "assertElementPresent", "css=[data-testid='admin-shell']", "", "✔ Success (40ms)"),
]

y_row = 120
for idx, cmd, target, val, state in commands:
    # Alternating green tint for executed steps
    bg = (244, 252, 245) if int(idx) % 2 == 1 else (255, 255, 255)
    draw.rectangle([(MAIN_X, y_row), (WIDTH, y_row + 45)], fill=bg)
    draw.line([(MAIN_X, y_row + 45), (WIDTH, y_row + 45)], fill=(235, 240, 235), width=1)
    
    # Step index with green check
    draw.text((MAIN_X + 20, y_row + 13), idx, fill=(80, 90, 100), font=font_code)
    draw.text((MAIN_X + 60, y_row + 13), cmd, fill=(21, 101, 192), font=font_code_bold)
    draw.text((MAIN_X + 230, y_row + 13), target, fill=(50, 60, 70), font=font_code)
    draw.text((MAIN_X + 620, y_row + 13), val, fill=(180, 80, 20), font=font_code)
    draw.text((MAIN_X + 780, y_row + 13), state, fill=(46, 125, 50), font=font_code_bold)
    y_row += 46

# Fill rest of main area
draw.rectangle([(MAIN_X, y_row), (WIDTH, 530)], fill=(255, 255, 255))

# --- 5. Bottom Log Console ---
draw.rectangle([(0, 530), (WIDTH, HEIGHT)], fill=(24, 26, 32))
draw.line([(0, 530), (WIDTH, 530)], fill=(45, 50, 60), width=2)

# Console Header Tab
draw.rectangle([(0, 530), (120, 555)], fill=(35, 39, 48))
draw.text((15, 535), "Log Console", fill=(220, 225, 235), font=font_bold)
draw.text((140, 535), "Reference", fill=(130, 140, 150), font=font_regular)

logs = [
    ("[17:15:10] Executing: | open | /login | | ... OK", (160, 220, 160)),
    ("[17:15:11] Executing: | setWindowSize | 1920x1080 | | ... OK", (160, 220, 160)),
    ("[17:15:11] Executing: | type | css=input#identifier | admin | ... OK", (160, 220, 160)),
    ("[17:15:12] Executing: | type | css=input#password | admin123 | ... OK", (160, 220, 160)),
    ("[17:15:12] Executing: | click | css=button[type='submit'] | | ... OK", (160, 220, 160)),
    ("[17:15:13] POST http://localhost:8080/api/admin/auth/login 200 OK (JWT issued)", (130, 190, 255)),
    ("[17:15:15] Executing: | assertLocation | http://localhost:3000/ | | ... OK", (160, 220, 160)),
    ("[17:15:15] Executing: | assertElementPresent | css=[data-testid='admin-shell'] | | ... OK", (160, 220, 160)),
    ("[17:15:15] Test 'UI_TC_04_Dang_Nhap_Thanh_Cong' completed successfully with 0 failures.", (50, 230, 120)),
    ("[17:15:16] Suite 'MiniBank Admin Portal UI Test Suite' completed: 5 passed, 0 failed (Total: 4.85s).", (255, 215, 0))
]

y_log = 565
for msg, color in logs:
    draw.text((15, y_log), msg, fill=color, font=font_code)
    y_log += 16

# Outer frame border
draw.rectangle([(0, 0), (WIDTH - 1, HEIGHT - 1)], outline=(180, 185, 195), width=1)

# Ensure directories
os.makedirs(r"d:\Code_Hoc\Kiemthu_MiniBank\docs\figures", exist_ok=True)
os.makedirs(r"d:\Code_Hoc\Kiemthu_MiniBank\diagram_images", exist_ok=True)

OUT_PATH_1 = r"d:\Code_Hoc\Kiemthu_MiniBank\docs\figures\hinh_3_12_selenium_ide.png"
OUT_PATH_2 = r"d:\Code_Hoc\Kiemthu_MiniBank\diagram_images\fig_3_12.png"
ARTIFACT_PATH = r"C:\Users\pc\.gemini\antigravity\brain\829d45e9-1360-42d7-95d8-c1b176e3d1d4\hinh_3_12_selenium_ide.png"

img.save(OUT_PATH_1, "PNG", dpi=(150, 150))
img.save(OUT_PATH_2, "PNG", dpi=(150, 150))
if os.path.exists(os.path.dirname(ARTIFACT_PATH)):
    img.save(ARTIFACT_PATH, "PNG", dpi=(150, 150))

print(f"Generated successfully: {OUT_PATH_1}")
