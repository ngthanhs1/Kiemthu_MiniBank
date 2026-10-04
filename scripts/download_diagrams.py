# -*- coding: utf-8 -*-
"""
Script to extract and download all 26 Mermaid diagrams as PNG images using mermaid.ink.
Caches downloaded images in 'diagram_images/' directory.
"""

import os
import re
import urllib.request
import base64
import time
import sys

# Ensure UTF-8 output if possible, else replace
if sys.stdout.encoding != 'utf-8':
    try:
        sys.stdout.reconfigure(encoding='utf-8', errors='replace')
    except Exception:
        pass

MD_FILE = r"d:\Code_Hoc\Kiemthu_MiniBank\BAO_CAO_KIEM_DINH_CHAT_LUONG_PHAN_MEM_MINIBANK.md"
IMG_DIR = r"d:\Code_Hoc\Kiemthu_MiniBank\diagram_images"

os.makedirs(IMG_DIR, exist_ok=True)

with open(MD_FILE, "r", encoding="utf-8") as f:
    text = f.read()

pattern = re.compile(r'```mermaid(.*?)```\s*(?:<p align="center"><b>(Hình \d+\.\d+:[^<]+)</b></p>)?', re.DOTALL)
matches = pattern.findall(text)

print(f"Found {len(matches)} Mermaid diagram blocks.")

success_count = 0
failed_count = 0

for idx, (code, caption) in enumerate(matches, 1):
    fig_name = f"fig_{idx:02d}.png"
    img_path = os.path.join(IMG_DIR, fig_name)
    
    clean_code = code.strip()
    
    if os.path.exists(img_path) and os.path.getsize(img_path) > 1000:
        success_count += 1
        continue
        
    try:
        encoded = base64.b64encode(clean_code.encode("utf-8")).decode("ascii")
        url = f"https://mermaid.ink/img/{encoded}"
        
        req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"})
        with urllib.request.urlopen(req, timeout=20) as resp:
            data = resp.read()
            with open(img_path, "wb") as f_out:
                f_out.write(data)
        success_count += 1
        time.sleep(0.3)
    except Exception as e:
        print(f"Warning: Failed to fetch image for block {idx}: {e}")
        failed_count += 1

print(f"Done downloading diagrams. Success: {success_count}, Failed: {failed_count}")
