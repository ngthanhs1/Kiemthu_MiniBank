# -*- coding: utf-8 -*-
"""
High-polish converter from Markdown to Microsoft Word (.docx)
Tailored for Phenikaa University academic graduation and project reports.
Includes:
- Professional Cover Page with elegant typography and border framing.
- Styled Headings (Heading 1 with page breaks and accent colors, Heading 2, Heading 3).
- Professional Academic Tables with Navy Blue (#1F497D) headers, white bold text, alternating row shading (#F9FAFC), and subtle borders.
- Diagram Callout Boxes and Embedded PNG Images with centered captions.
- Clean formatting: Times New Roman, 1.25 line spacing, 3.0cm left margin for binding.
"""

import os
import re
import docx
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_ALIGN_VERTICAL
from docx.oxml import OxmlElement, parse_xml
from docx.oxml.ns import nsdecls, qn

MD_FILE = r"d:\Code_Hoc\Kiemthu_MiniBank\BAO_CAO_KIEM_DINH_CHAT_LUONG_PHAN_MEM_MINIBANK.md"
DOCX_FILE = r"d:\Code_Hoc\Kiemthu_MiniBank\BAO_CAO_KIEM_DINH_CHAT_LUONG_PHAN_MEM_MINIBANK.docx"
ARTIFACT_DOCX = r"C:\Users\pc\.gemini\antigravity\brain\829d45e9-1360-42d7-95d8-c1b176e3d1d4\BAO_CAO_KIEM_DINH_CHAT_LUONG_PHAN_MEM_MINIBANK.docx"
IMG_DIR = r"d:\Code_Hoc\Kiemthu_MiniBank\diagram_images"

def set_cell_background(cell, hex_color):
    tcPr = cell._element.get_or_add_tcPr()
    shd = parse_xml(f'<w:shd {nsdecls("w")} w:fill="{hex_color}"/>')
    tcPr.append(shd)

def set_cell_margins(cell, top=100, bottom=100, left=140, right=140):
    tcPr = cell._element.get_or_add_tcPr()
    tcMar = OxmlElement('w:tcMar')
    for m, val in [('top', top), ('bottom', bottom), ('left', left), ('right', right)]:
        node = OxmlElement(f'w:{m}')
        node.set(qn('w:w'), str(val))
        node.set(qn('w:type'), 'dxa')
        tcMar.append(node)
    tcPr.append(tcMar)

def set_table_borders(table, color="B0C4DE", sz="4", val="single"):
    tblPr = table._element.xpath('w:tblPr')
    if tblPr:
        borders = parse_xml(
            f'<w:tblBorders {nsdecls("w")}>'
            f'  <w:top w:val="{val}" w:sz="{sz}" w:space="0" w:color="{color}"/>'
            f'  <w:left w:val="{val}" w:sz="{sz}" w:space="0" w:color="{color}"/>'
            f'  <w:bottom w:val="{val}" w:sz="{sz}" w:space="0" w:color="{color}"/>'
            f'  <w:right w:val="{val}" w:sz="{sz}" w:space="0" w:color="{color}"/>'
            f'  <w:insideH w:val="{val}" w:sz="{sz}" w:space="0" w:color="{color}"/>'
            f'  <w:insideV w:val="{val}" w:sz="{sz}" w:space="0" w:color="{color}"/>'
            f'</w:tblBorders>'
        )
        tblPr[0].append(borders)

def format_inlines(paragraph, text, font_size=12, default_bold=False, default_italic=False, text_color=None):
    """Parses bold, italic, code backticks, and clean KaTeX math strings into docx runs."""
    # Replace math markers
    clean_text = text.replace("$$", "").replace("$", "")
    
    # Split by bold markers
    parts = re.split(r'(\*\*.*?\*\*)', clean_text)
    for part in parts:
        if not part:
            continue
        if part.startswith("**") and part.endswith("**"):
            sub_text = part[2:-2]
            r = paragraph.add_run(sub_text)
            r.font.bold = True
            r.font.italic = default_italic
        else:
            # Check for inline code `code`
            sub_parts = re.split(r'(`.*?`)', part)
            for sp in sub_parts:
                if not sp:
                    continue
                if sp.startswith("`") and sp.endswith("`"):
                    r = paragraph.add_run(sp[1:-1])
                    r.font.name = "Consolas"
                    r.font.size = Pt(font_size - 1)
                    r.font.color.rgb = RGBColor(0xA0, 0x20, 0x20)
                else:
                    r = paragraph.add_run(sp)
                    r.font.bold = default_bold
                    r.font.italic = default_italic
                    if text_color:
                        r.font.color.rgb = text_color
        r.font.name = "Times New Roman"
        r.font.size = Pt(font_size)

def build_docx():
    print("Reading markdown file...")
    with open(MD_FILE, "r", encoding="utf-8") as f:
        md_text = f.read()

    doc = docx.Document()

    # 1. Page Setup (A4, 3cm left margin for binding, 2cm top/bottom/right)
    for s in doc.sections:
        s.page_width = Inches(8.27)
        s.page_height = Inches(11.69)
        s.top_margin = Inches(0.79)
        s.bottom_margin = Inches(0.79)
        s.left_margin = Inches(1.18)   # 3.0 cm
        s.right_margin = Inches(0.79)

    # 2. Setup Default Normal Style
    normal = doc.styles['Normal']
    normal.font.name = 'Times New Roman'
    normal.font.size = Pt(12.5)
    normal.font.color.rgb = RGBColor(0x20, 0x20, 0x20)
    normal.paragraph_format.line_spacing = 1.25
    normal.paragraph_format.space_after = Pt(4)
    normal.paragraph_format.space_before = Pt(0)
    normal.paragraph_format.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY

    # 3. Build Cover Page
    p_uni = doc.add_paragraph()
    p_uni.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_uni.paragraph_format.space_before = Pt(12)
    p_uni.paragraph_format.space_after = Pt(2)
    r_uni = p_uni.add_run("TRƯỜNG ĐẠI HỌC PHENIKAA\nKHOA CÔNG NGHỆ THÔNG TIN")
    r_uni.font.name = "Times New Roman"
    r_uni.font.bold = True
    r_uni.font.size = Pt(14)
    r_uni.font.color.rgb = RGBColor(0x1F, 0x49, 0x7D)

    p_star = doc.add_paragraph()
    p_star.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_star.paragraph_format.space_before = Pt(2)
    p_star.paragraph_format.space_after = Pt(28)
    r_star = p_star.add_run("―" * 25)
    r_star.font.color.rgb = RGBColor(0x1F, 0x49, 0x7D)

    p_report = doc.add_paragraph()
    p_report.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_report.paragraph_format.space_after = Pt(6)
    r_rep = p_report.add_run("BÁO CÁO BÀI TẬP LỚN MÔN HỌC")
    r_rep.font.name = "Times New Roman"
    r_rep.font.bold = True
    r_rep.font.size = Pt(16)
    r_rep.font.color.rgb = RGBColor(0xC0, 0x00, 0x00) # Dark red highlight

    p_course = doc.add_paragraph()
    p_course.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_course.paragraph_format.space_after = Pt(24)
    r_crs = p_course.add_run("HỌC PHẦN: KIỂM ĐỊNH VÀ ĐÁNH GIÁ CHẤT LƯỢNG PHẦN MỀM\n(SE3020 / IT3020)")
    r_crs.font.name = "Times New Roman"
    r_crs.font.bold = True
    r_crs.font.size = Pt(13)

    p_subject_lbl = doc.add_paragraph()
    p_subject_lbl.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_subject_lbl.paragraph_format.space_after = Pt(4)
    r_lbl = p_subject_lbl.add_run("ĐỀ TÀI NGHIÊN CỨU & THỰC HÀNH:")
    r_lbl.font.name = "Times New Roman"
    r_lbl.font.italic = True
    r_lbl.font.size = Pt(12)

    p_title = doc.add_paragraph()
    p_title.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_title.paragraph_format.space_after = Pt(36)
    r_title = p_title.add_run("KIỂM ĐỊNH VÀ ĐÁNH GIÁ CHẤT LƯỢNG\nHỆ THỐNG NGÂN HÀNG SỐ TRỰC TUYẾN MINIBANK\n(DIGITAL BANKING CORE BACKEND)")
    r_title.font.name = "Times New Roman"
    r_title.font.bold = True
    r_title.font.size = Pt(17)
    r_title.font.color.rgb = RGBColor(0x1F, 0x49, 0x7D)

    p_info = doc.add_paragraph()
    p_info.alignment = WD_ALIGN_PARAGRAPH.LEFT
    p_info.paragraph_format.left_indent = Inches(1.5)
    p_info.paragraph_format.space_after = Pt(36)
    p_info.paragraph_format.line_spacing = 1.3
    
    info_text = (
        "• Giảng viên hướng dẫn:\tTS. Trịnh Thanh Bình\n"
        "• Học kỳ / Năm học:\tHọc kỳ I - Năm học 2025 - 2026\n"
        "• Ngành đào tạo:\t\tKỹ thuật Phần mềm / CNTT\n"
        "• Hệ đào tạo:\t\tĐại học chính quy\n"
        "• Nhóm sinh viên:\t\tNhóm Kiểm thử Phần mềm Chuyên sâu"
    )
    for line in info_text.splitlines():
        parts = line.split(":\t")
        r_label = p_info.add_run(parts[0] + ":\t")
        r_label.font.name = "Times New Roman"
        r_label.font.bold = True
        r_label.font.size = Pt(12)
        if len(parts) > 1:
            r_val = p_info.add_run(parts[1] + "\n")
            r_val.font.name = "Times New Roman"
            r_val.font.size = Pt(12)

    p_loc = doc.add_paragraph()
    p_loc.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_loc.paragraph_format.space_before = Pt(30)
    r_loc = p_loc.add_run("Hà Nội, Năm 2026")
    r_loc.font.name = "Times New Roman"
    r_loc.font.bold = True
    r_loc.font.size = Pt(12.5)

    doc.add_page_break()

    # 4. Parse content lines starting after front matter
    lines = md_text.splitlines()
    total = len(lines)
    idx = 0
    mermaid_counter = 0
    in_code = False
    code_lines = []
    code_lang = ""
    in_table = False
    table_rows = []

    # Skip initial raw markdown cover header lines up to first section
    while idx < total and not lines[idx].startswith("## BẢNG PHÂN CÔNG NHIỆM VỤ"):
        idx += 1

    print(f"Parsing content from line {idx} to {total}...")

    while idx < total:
        line = lines[idx]

        # Ignore raw html tags that might leak
        if line.strip() in ["<p align=\"center\">", "</p>"] or "<img " in line:
            idx += 1
            continue

        # Check for code blocks
        if line.startswith("```"):
            if not in_code:
                in_code = True
                code_lang = line[3:].strip()
                code_lines = []
                idx += 1
                continue
            else:
                in_code = False
                if code_lang == "mermaid":
                    mermaid_counter += 1
                    fig_path = os.path.join(IMG_DIR, f"fig_{mermaid_counter:02d}.png")
                    if os.path.exists(fig_path) and os.path.getsize(fig_path) > 1000:
                        p_img = doc.add_paragraph()
                        p_img.alignment = WD_ALIGN_PARAGRAPH.CENTER
                        p_img.paragraph_format.space_before = Pt(8)
                        p_img.paragraph_format.space_after = Pt(2)
                        run = p_img.add_run()
                        run.add_picture(fig_path, width=Inches(5.8))
                    else:
                        # Clean stylized diagram callout
                        tbl = doc.add_table(rows=1, cols=1)
                        tbl.alignment = WD_TABLE_ALIGNMENT.CENTER
                        set_cell_background(tbl.rows[0].cells[0], "F4F6F9")
                        set_cell_margins(tbl.rows[0].cells[0], 100, 100, 140, 140)
                        set_table_borders(tbl, "B0C4DE", sz="5")
                        p_code = tbl.rows[0].cells[0].paragraphs[0]
                        p_code.paragraph_format.line_spacing = 1.05
                        p_code.paragraph_format.space_after = Pt(0)
                        r_hdr = p_code.add_run(f"[Sơ đồ cấu trúc Mermaid #{mermaid_counter}]\n")
                        r_hdr.font.name = "Consolas"
                        r_hdr.font.bold = True
                        r_hdr.font.size = Pt(9.5)
                        r_hdr.font.color.rgb = RGBColor(0x1F, 0x49, 0x7D)
                        r_body = p_code.add_run("\n".join(code_lines))
                        r_body.font.name = "Consolas"
                        r_body.font.size = Pt(9.0)
                        r_body.font.color.rgb = RGBColor(0x33, 0x44, 0x66)
                else:
                    # Regular source code block
                    tbl = doc.add_table(rows=1, cols=1)
                    tbl.alignment = WD_TABLE_ALIGNMENT.CENTER
                    set_cell_background(tbl.rows[0].cells[0], "F8F9FA")
                    set_cell_margins(tbl.rows[0].cells[0], 90, 90, 140, 140)
                    set_table_borders(tbl, "D0D5DD", sz="4")
                    p_code = tbl.rows[0].cells[0].paragraphs[0]
                    p_code.paragraph_format.line_spacing = 1.05
                    p_code.paragraph_format.space_after = Pt(0)
                    r_code = p_code.add_run("\n".join(code_lines))
                    r_code.font.name = "Consolas"
                    r_code.font.size = Pt(9.5)
                    r_code.font.color.rgb = RGBColor(0x24, 0x29, 0x2F)
                
                idx += 1
                continue

        if in_code:
            code_lines.append(line)
            idx += 1
            continue

        # Check for Markdown Tables
        if line.strip().startswith("|") and line.strip().endswith("|"):
            table_rows.append([c.strip() for c in line.strip().strip("|").split("|")])
            in_table = True
            idx += 1
            continue
        elif in_table:
            in_table = False
            valid_rows = [r for r in table_rows if not all(re.match(r'^:?-+:?$', c) for c in r)]
            if valid_rows:
                num_cols = max(len(r) for r in valid_rows)
                t = doc.add_table(rows=len(valid_rows), cols=num_cols)
                t.alignment = WD_TABLE_ALIGNMENT.CENTER
                set_table_borders(t, "B8C4CE", sz="4")
                
                for r_idx, row_data in enumerate(valid_rows):
                    row = t.rows[r_idx]
                    is_header = (r_idx == 0)
                    for c_idx in range(num_cols):
                        cell = row.cells[c_idx]
                        cell.vertical_alignment = WD_ALIGN_VERTICAL.CENTER
                        set_cell_margins(cell, top=80, bottom=80, left=110, right=110)
                        val = row_data[c_idx] if c_idx < len(row_data) else ""
                        val = val.replace("<br>", "\n").replace("<br/>", "\n")
                        
                        p = cell.paragraphs[0]
                        p.paragraph_format.space_before = Pt(1)
                        p.paragraph_format.space_after = Pt(1)
                        p.paragraph_format.line_spacing = 1.15
                        
                        if is_header:
                            set_cell_background(cell, "1F497D")
                            format_inlines(p, val, font_size=10.5, default_bold=True, text_color=RGBColor(0xFF, 0xFF, 0xFF))
                            p.alignment = WD_ALIGN_PARAGRAPH.CENTER
                        else:
                            if r_idx % 2 == 1:
                                set_cell_background(cell, "F9FAFC")
                            format_inlines(p, val, font_size=10)
                            # Align numbers or short codes center
                            if len(val) <= 10 and not val.startswith("UC"):
                                p.alignment = WD_ALIGN_PARAGRAPH.CENTER
                            else:
                                p.alignment = WD_ALIGN_PARAGRAPH.LEFT
                
                p_sp = doc.add_paragraph()
                p_sp.paragraph_format.space_after = Pt(4)
                p_sp.paragraph_format.space_before = Pt(0)

            table_rows = []

        # Check for Figure or Table Captions
        cap_match = re.search(r'<p align="center"><b>((?:Hình|Bảng)\s+\d+\.\d+:[^<]+)</b></p>', line)
        if cap_match:
            cap_text = cap_match.group(1)
            p_cap = doc.add_paragraph()
            p_cap.alignment = WD_ALIGN_PARAGRAPH.CENTER
            p_cap.paragraph_format.space_before = Pt(4)
            p_cap.paragraph_format.space_after = Pt(8)
            r_cap = p_cap.add_run(cap_text)
            r_cap.font.name = "Times New Roman"
            r_cap.font.bold = True
            r_cap.font.size = Pt(11)
            if cap_text.startswith("Hình"):
                r_cap.font.color.rgb = RGBColor(0x1F, 0x49, 0x7D)
            else:
                r_cap.font.color.rgb = RGBColor(0x22, 0x22, 0x22)
            idx += 1
            continue

        # Check for Section Headings
        if line.startswith("# PHẦN"):
            doc.add_page_break()
            h = doc.add_paragraph()
            h.paragraph_format.space_before = Pt(16)
            h.paragraph_format.space_after = Pt(10)
            r = h.add_run(line[2:].strip())
            r.font.name = "Times New Roman"
            r.font.bold = True
            r.font.size = Pt(15.5)
            r.font.color.rgb = RGBColor(0x1F, 0x49, 0x7D)
            idx += 1
            continue
        elif line.startswith("# "):
            h = doc.add_paragraph()
            h.paragraph_format.space_before = Pt(14)
            h.paragraph_format.space_after = Pt(8)
            r = h.add_run(line[2:].strip())
            r.font.name = "Times New Roman"
            r.font.bold = True
            r.font.size = Pt(14.5)
            r.font.color.rgb = RGBColor(0x1F, 0x49, 0x7D)
            idx += 1
            continue
        elif line.startswith("## "):
            h = doc.add_paragraph()
            h.paragraph_format.space_before = Pt(12)
            h.paragraph_format.space_after = Pt(6)
            r = h.add_run(line[3:].strip())
            r.font.name = "Times New Roman"
            r.font.bold = True
            r.font.size = Pt(13.5)
            r.font.color.rgb = RGBColor(0x2E, 0x75, 0xB6)
            idx += 1
            continue
        elif line.startswith("### "):
            h = doc.add_paragraph()
            h.paragraph_format.space_before = Pt(8)
            h.paragraph_format.space_after = Pt(4)
            r = h.add_run(line[4:].strip())
            r.font.name = "Times New Roman"
            r.font.bold = True
            r.font.size = Pt(12.5)
            r.font.color.rgb = RGBColor(0x33, 0x33, 0x33)
            idx += 1
            continue
        elif line.startswith("#### "):
            h = doc.add_paragraph()
            h.paragraph_format.space_before = Pt(6)
            h.paragraph_format.space_after = Pt(2)
            r = h.add_run(line[5:].strip())
            r.font.name = "Times New Roman"
            r.font.bold = True
            r.font.italic = True
            r.font.size = Pt(12)
            r.font.color.rgb = RGBColor(0x44, 0x44, 0x44)
            idx += 1
            continue

        # Check for Horizontal Rules (---)
        if line.strip() == "---":
            # Add page break before major parts
            p_div = doc.add_paragraph()
            p_div.paragraph_format.space_before = Pt(2)
            p_div.paragraph_format.space_after = Pt(2)
            idx += 1
            continue

        # Check for Bullet List
        if line.strip().startswith("* ") or line.strip().startswith("- "):
            p = doc.add_paragraph(style='List Bullet')
            p.paragraph_format.space_after = Pt(2)
            p.paragraph_format.line_spacing = 1.25
            p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
            content = line.strip()[2:]
            format_inlines(p, content, font_size=12)
            idx += 1
            continue

        # Regular text paragraph
        if line.strip():
            p = doc.add_paragraph()
            p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
            format_inlines(p, line.strip(), font_size=12.5)

        idx += 1

    print("Saving highly-polished .docx file...")
    doc.save(DOCX_FILE)
    print(f"Generated successfully: {DOCX_FILE} ({os.path.getsize(DOCX_FILE)} bytes)")

    import shutil
    shutil.copyfile(DOCX_FILE, ARTIFACT_DOCX)
    print(f"Copied to artifact directory: {ARTIFACT_DOCX}")

if __name__ == "__main__":
    build_docx()
