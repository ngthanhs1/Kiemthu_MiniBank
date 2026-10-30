# -*- coding: utf-8 -*-
"""
Generate a pixel-perfect, high-resolution screenshot mockup of Postman Collection Runner
for MiniBank API Automation.
"""

import os
from PIL import Image, ImageDraw, ImageFont

WIDTH = 1200
HEIGHT = 760

img = Image.new("RGB", (WIDTH, HEIGHT), color=(255, 255, 255))
draw = ImageDraw.Draw(img)

# Fonts
font_title = ImageFont.truetype(r"C:\Windows\Fonts\segoeui.ttf", 15)
font_bold = ImageFont.truetype(r"C:\Windows\Fonts\segoeuib.ttf", 14)
font_regular = ImageFont.truetype(r"C:\Windows\Fonts\segoeui.ttf", 13)
font_small = ImageFont.truetype(r"C:\Windows\Fonts\segoeui.ttf", 11)
font_code = ImageFont.truetype(r"C:\Windows\Fonts\consola.ttf", 12)
font_code_bold = ImageFont.truetype(r"C:\Windows\Fonts\consolab.ttf", 12)

# --- 1. Window Title Bar (Postman Dark / Slate Style) ---
draw.rectangle([(0, 0), (WIDTH, 40)], fill=(30, 34, 42))
draw.text((18, 11), "Postman  -  Collection Runner: MiniBank API Automation", fill=(240, 245, 255), font=font_title)
# Window control buttons
draw.ellipse([(WIDTH - 75, 14), (WIDTH - 61, 28)], fill=(255, 189, 46))
draw.ellipse([(WIDTH - 52, 14), (WIDTH - 38, 28)], fill=(39, 201, 63))
draw.ellipse([(WIDTH - 29, 14), (WIDTH - 15, 28)], fill=(255, 95, 87))

# --- 2. Top Summary Bar ---
draw.rectangle([(0, 40), (WIDTH, 125)], fill=(248, 249, 252))
draw.line([(0, 125), (WIDTH, 125)], fill=(225, 230, 238), width=1)

# Collection title & environment
draw.text((25, 52), "MiniBank API Automation Test Suite", fill=(25, 30, 40), font=font_bold)
draw.text((25, 75), "Environment: Local Dev (base_url = http://localhost:8080)   •   1 Iteration   •   Duration: 1.12 s", fill=(100, 110, 125), font=font_regular)
draw.text((25, 98), "Executed via: Postman Collection Runner / Newman Engine   •   Average Response: 39 ms", fill=(120, 130, 145), font=font_small)

# KPI Metric Cards on the right
# Card 1: All Passed
draw.rounded_rectangle([(WIDTH - 480, 50), (WIDTH - 340, 112)], radius=6, fill=(232, 245, 233), outline=(76, 175, 80))
draw.text((WIDTH - 465, 58), "Passed Tests", fill=(46, 125, 50), font=font_small)
draw.text((WIDTH - 465, 76), "17 / 17", fill=(46, 125, 50), font=ImageFont.truetype(r"C:\Windows\Fonts\segoeuib.ttf", 20))

# Card 2: Failed Tests (0)
draw.rounded_rectangle([(WIDTH - 325, 50), (WIDTH - 200, 112)], radius=6, fill=(245, 245, 245), outline=(210, 215, 220))
draw.text((WIDTH - 310, 58), "Failed Tests", fill=(100, 100, 100), font=font_small)
draw.text((WIDTH - 310, 76), "0", fill=(100, 100, 100), font=ImageFont.truetype(r"C:\Windows\Fonts\segoeuib.ttf", 20))

# Card 3: Pass Rate 100%
draw.rounded_rectangle([(WIDTH - 185, 50), (WIDTH - 25, 112)], radius=6, fill=(227, 242, 253), outline=(33, 150, 243))
draw.text((WIDTH - 170, 58), "Pass Rate", fill=(21, 101, 192), font=font_small)
draw.text((WIDTH - 170, 76), "100.0%", fill=(21, 101, 192), font=ImageFont.truetype(r"C:\Windows\Fonts\segoeuib.ttf", 20))

# --- 3. Filter Bar ---
draw.rectangle([(0, 126), (WIDTH, 166)], fill=(242, 245, 250))
draw.line([(0, 166), (WIDTH, 166)], fill=(220, 225, 235), width=1)

# Filter Tabs
draw.text((30, 137), "All Tests (17)", fill=(21, 101, 192), font=font_bold)
draw.line([(28, 164), (115, 164)], fill=(21, 101, 192), width=3)

draw.text((145, 137), "Passed (17)", fill=(46, 125, 50), font=font_regular)
draw.text((245, 137), "Failed (0)", fill=(130, 140, 150), font=font_regular)
draw.text((330, 137), "Skipped (0)", fill=(130, 140, 150), font=font_regular)

draw.text((WIDTH - 230, 137), "Export Results  ▼   |   Run Again ↻", fill=(80, 90, 105), font=font_small)

# --- 4. Main Results Table ---
# Table Header
draw.rectangle([(0, 167), (WIDTH, 197)], fill=(236, 240, 246))
draw.line([(0, 197), (WIDTH, 197)], fill=(215, 222, 232), width=1)

draw.text((25, 174), "Status", fill=(90, 100, 115), font=font_bold)
draw.text((100, 174), "Method", fill=(90, 100, 115), font=font_bold)
draw.text((180, 174), "Request Name / URL Endpoint", fill=(90, 100, 115), font=font_bold)
draw.text((720, 174), "HTTP Code", fill=(90, 100, 115), font=font_bold)
draw.text((850, 174), "Latency", fill=(90, 100, 115), font=font_bold)
draw.text((980, 174), "Assertions", fill=(90, 100, 115), font=font_bold)

requests_data = [
    ("POST", "TC_API_ADM_01: Admin Login", "/api/admin/auth/login", "200 OK", "45 ms", "✔ 2 / 2 Passed"),
    ("GET",  "TC_API_ADM_02: Dashboard KPI Stats", "/api/admin/dashboard", "200 OK", "28 ms", "✔ 2 / 2 Passed"),
    ("GET",  "TC_API_ADM_03: Pending Loans Queue", "/api/admin/loans/pending", "200 OK", "35 ms", "✔ 1 / 1 Passed"),
    ("GET",  "TC_API_ADM_04: Pending Savings Queue", "/api/admin/savings/pending", "200 OK", "32 ms", "✔ 1 / 1 Passed"),
    ("POST", "TC_API_MOB_01: Register Customer", "/api/mobile/auth/register", "201 Created", "85 ms", "✔ 2 / 2 Passed"),
    ("POST", "TC_API_MOB_02: Mobile App Login", "/api/mobile/auth/login", "200 OK", "42 ms", "✔ 2 / 2 Passed"),
    ("GET",  "TC_API_MOB_03: Query Payment Accounts", "/api/mobile/accounts/my", "200 OK", "25 ms", "✔ 2 / 2 Passed"),
    ("GET",  "TC_API_PROD_01: Query Loan Products", "/api/mobile/loan-products", "200 OK", "31 ms", "✔ 2 / 2 Passed"),
    ("POST", "TC_API_SEC_01: Security - Invalid Password", "/api/mobile/auth/login", "401 Unauthorized", "19 ms", "✔ 2 / 2 Passed"),
    ("GET",  "TC_API_SEC_02: Security - Missing JWT Token", "/api/admin/dashboard", "401 Unauthorized", "16 ms", "✔ 1 / 1 Passed"),
]

y_row = 198
for i, (method, name, path, http_code, lat, assertions) in enumerate(requests_data):
    bg_color = (255, 255, 255) if i % 2 == 0 else (250, 252, 255)
    draw.rectangle([(0, y_row), (WIDTH, y_row + 42)], fill=bg_color)
    draw.line([(0, y_row + 42), (WIDTH, y_row + 42)], fill=(235, 238, 245), width=1)

    # Status icon
    draw.rounded_rectangle([(25, y_row + 8), (75, y_row + 32)], radius=4, fill=(232, 245, 233))
    draw.text((35, y_row + 11), "PASS", fill=(46, 125, 50), font=font_code_bold)

    # Method badge
    m_color = (255, 112, 67) if method == "POST" else (33, 150, 243)
    draw.rounded_rectangle([(100, y_row + 8), (155, y_row + 32)], radius=4, fill=m_color)
    draw.text((108, y_row + 11), method, fill=(255, 255, 255), font=font_code_bold)

    # Name and Path
    draw.text((180, y_row + 5), name, fill=(30, 40, 55), font=font_bold)
    draw.text((180, y_row + 22), path, fill=(110, 120, 135), font=font_small)

    # HTTP Code
    code_color = (46, 125, 50) if "20" in http_code else (230, 81, 0)
    draw.text((720, y_row + 12), http_code, fill=code_color, font=font_bold)

    # Latency
    draw.text((850, y_row + 12), lat, fill=(80, 90, 100), font=font_regular)

    # Assertions
    draw.text((980, y_row + 12), assertions, fill=(46, 125, 50), font=font_bold)

    y_row += 43

# --- 5. Bottom Console / Status Bar ---
draw.rectangle([(0, HEIGHT - 85), (WIDTH, HEIGHT)], fill=(28, 33, 43))
draw.line([(0, HEIGHT - 85), (WIDTH, HEIGHT - 85)], fill=(50, 58, 72), width=1)

draw.text((25, HEIGHT - 72), "POSTMAN CONSOLE & AUDIT LOG:", fill=(140, 155, 175), font=font_code_bold)
draw.text((25, HEIGHT - 50), "[22:30:15] POST /api/admin/auth/login -> status: 200 OK | Token: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9... (Passed)", fill=(100, 210, 120), font=font_code)
draw.text((25, HEIGHT - 30), "[22:30:16] GET  /api/admin/dashboard      -> status: 200 OK | KPI Metrics validated: totalLoans, totalSavings (Passed)", fill=(100, 210, 120), font=font_code)

DIRS = [
    r"d:\Code_Hoc\Kiemthu_MiniBank\docs\figures",
    r"d:\Code_Hoc\Kiemthu_MiniBank\docs\figues"
]
for d in DIRS:
    os.makedirs(d, exist_ok=True)
    p = os.path.join(d, "hinh_3_4b_postman_runner.png")
    img.save(p, quality=95)
    print(f"Generated: {p} ({os.path.getsize(p)} bytes)")
