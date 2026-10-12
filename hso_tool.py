#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
HSO STUDIO & RADAR MANAGER - Giao diện HUD chuyên nghiệp cho Hiệp Sĩ Online (HSO v403)
Phiên bản Mini HUD Tinh Gọn:
- 1 Nút Màn Hình (Popup chọn / tự nhập kích thước)
- 1 Nút Auto Đánh (Bật/Tắt)
- 1 Nút Tốc Độ (Luân phiên x1.0, x1.5, x2.0, x3.0)
- 1 Nút Auto Nhặt Đồ (Bật/Tắt)
- Danh sách quái vật dạng Thanh Máu (HP Bar) trực quan pixel-art
- Giao diện nhỏ gọn (740x480), chuẩn phong cách game Hiệp Sĩ Online.
"""

import os
import sys
import subprocess
import json
import time
from pathlib import Path

import tkinter as tk
from tkinter import ttk, messagebox
from PIL import Image, ImageTk, ImageDraw

BASE_DIR = Path(__file__).resolve().parent
JAR_TARGET = BASE_DIR / "KnightOnline_v403.jar"
JAR_ORIG = BASE_DIR / "KnightOnline_v403.jar.orig"
CONFIG_FILE = BASE_DIR / "tool_config.json"
MICROEMU_JAR = BASE_DIR / "microemulator-3.0.0.jar"
RES_DIR = BASE_DIR / "jar_resources"
STATE_FILE = BASE_DIR / "client_state.json"
CMD_FILE = BASE_DIR / "client_command.json"
CMD_DIR = BASE_DIR / "client_commands"

JAVA_EXEC = "/usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java"
if not os.path.exists(JAVA_EXEC):
    JAVA_EXEC = "java"


def sort_monsters_for_panel(monsters, player, focus_id, pinned_id):
    px, py = player.get("x", 0), player.get("y", 0)

    def key(monster):
        monster_id = monster.get("id")
        priority = (0 if monster_id == pinned_id else
                    1 if monster_id == focus_id else
                    2 if monster.get("hp", 0) > 0 else 3)
        distance = (monster.get("x", 0) - px) ** 2 + (monster.get("y", 0) - py) ** 2
        return priority, distance, monster_id

    return sorted(monsters, key=key)


def farm_profile_key(map_id, area):
    return f"{int(map_id)}:{int(area)}"


def farm_profile_matches(profile, monster):
    return profile is not None and (
        profile.get("all", False) or
        monster.get("name") in profile.get("types", []) or
        monster.get("id") in profile.get("ids", []))


def resolve_farm_ids(profile, monsters, include_dead=False):
    if profile is None:
        return []
    return sorted(m["id"] for m in monsters
                  if (include_dead or m.get("hp", 0) > 0) and farm_profile_matches(profile, m))


def radar_projection(width, height, map_width, map_height):
    """Return the whole-map rectangle and a shared pixel-to-canvas scale."""
    map_width = max(1, map_width)
    map_height = max(1, map_height)
    scale = min((width - 16) / map_width, (height - 31) / map_height)
    draw_width, draw_height = map_width * scale, map_height * scale
    return (width - draw_width) / 2, 7 + (height - 31 - draw_height) / 2, scale


def classify_radar_npc(name):
    name = (name or "").strip().lower()
    if name == "khu":
        return "zone"
    if "dịch chuyển" in name or "dich chuyen" in name or "teleport" in name:
        return "stone"
    return "npc"


def farm_profile_from_selection(monsters, selected_ids):
    groups = {}
    for monster in monsters:
        groups.setdefault(monster.get("name", "Mob"), set()).add(monster["id"])
    all_ids = {monster["id"] for monster in monsters}
    selected_ids = set(selected_ids) & all_ids
    if all_ids and selected_ids == all_ids:
        return {"all": True, "types": [], "ids": []}
    selected_types = sorted(name for name, ids in groups.items()
                            if name != "Chưa rõ loại" and ids <= selected_ids)
    covered_ids = set().union(*(groups[name] for name in selected_types)) if selected_types else set()
    return {"all": False, "types": selected_types, "ids": sorted(selected_ids - covered_ids)}

# Bảng màu chuẩn HUD Hiệp Sĩ Online
COLOR_BG_DARK = "#140a06"        # Nền tối viền ngoài
COLOR_PANEL_BG = "#22130d"       # Nền panel gỗ chính
COLOR_INNER_BG = "#160c07"       # Nền khung con / danh sách
COLOR_BORDER_GOLD = "#c8963e"    # Viền vàng kim
COLOR_BORDER_LIGHT = "#f5cb6c"   # Viền vàng sáng
COLOR_BORDER_DARK = "#3d1b10"    # Viền nâu gỗ sẫm
COLOR_TEXT_GOLD = "#ffdf79"      # Chữ vàng HSO
COLOR_TEXT_WHITE = "#ede8df"     # Chữ trắng ngà
COLOR_TEXT_MUTED = "#96806e"     # Chữ phụ màu cát
COLOR_ACTIVE_BG = "#6e2912"      # Nền nút đang chọn
COLOR_ACTIVE_BORDER = "#ffd778"  # Viền nút đang chọn
COLOR_HP_RED = "#d42a2a"         # Máu đỏ
COLOR_MP_BLUE = "#2a70d4"        # Mana xanh
COLOR_ONLINE = "#4ce66a"         # Xanh online
COLOR_OFFLINE = "#9e8874"        # Xám offline

# Font chữ đồng bộ với Hiệp Sĩ Online (Tahoma in-game, không in đậm)
FONT_FAMILY = "Tahoma"

# Các mức tốc độ di chuyển (Dải an toàn với bước tăng 0.2x)
SPEED_LEVELS = ["1.0", "1.2", "1.4", "1.6", "1.8", "2.0"]

DEFAULT_CONFIG = {
    "width": 800,
    "height": 480,
    "preset": "800x480",
    "fps_60": True,
    "speed": "1.0",
    "auto_attack": False,
    "auto_pickup": False
}


def load_config():
    if CONFIG_FILE.exists():
        try:
            with open(CONFIG_FILE, "r", encoding="utf-8") as f:
                return {**DEFAULT_CONFIG, **json.load(f)}
        except Exception:
            pass
    return DEFAULT_CONFIG.copy()


def save_config(cfg):
    try:
        with open(CONFIG_FILE, "w", encoding="utf-8") as f:
            json.dump(cfg, f, indent=4, ensure_ascii=False)
        return True
    except Exception:
        return False


class HsoButtonRenderer:
    """Renderer nút bấm chuẩn gỗ viền vàng Hiệp Sĩ Online (không phụ thuộc alpha Canvas X11)"""
    def __init__(self):
        self._cache = {}

    def get_photo(self, w, h, is_pressed=False, is_selected=False, is_hover=False, is_disabled=False, bg_color=(30, 17, 9)):
        key = (w, h, is_pressed, is_selected, is_hover, is_disabled, bg_color)
        if key in self._cache:
            return self._cache[key]

        w = max(10, w)
        h = max(10, h)
        im = Image.new("RGB", (w, h), bg_color)
        draw = ImageDraw.Draw(im)

        if is_disabled:
            top_c = (30, 15, 9)
            bot_c = (20, 10, 6)
            border_gold = (75, 52, 28)
            border_outer = (22, 10, 5)
            highlight_c = (38, 19, 12)
        elif is_selected:
            top_c = (118, 45, 22)
            bot_c = (82, 28, 14)
            border_gold = (200, 150, 62)
            border_outer = (35, 14, 6)
            highlight_c = (140, 65, 30)
        elif is_pressed:
            top_c = (38, 14, 6)
            bot_c = (58, 22, 10)
            border_gold = (160, 110, 40)
            border_outer = (25, 8, 4)
            highlight_c = (48, 18, 8)
        elif is_hover:
            top_c = (86, 38, 22)
            bot_c = (60, 25, 13)
            border_gold = (245, 203, 108)
            border_outer = (45, 18, 8)
            highlight_c = (130, 60, 35)
        else:
            top_c = (64, 27, 15)
            bot_c = (46, 18, 9)
            border_gold = (200, 150, 62)
            border_outer = (35, 14, 6)
            highlight_c = (105, 48, 26)

        # Vẽ gradient gỗ
        for y in range(h):
            r = int(top_c[0] + (bot_c[0] - top_c[0]) * y / max(1, h - 1))
            g = int(top_c[1] + (bot_c[1] - top_c[1]) * y / max(1, h - 1))
            b = int(top_c[2] + (bot_c[2] - top_c[2]) * y / max(1, h - 1))
            draw.line([(0, y), (w - 1, y)], fill=(r, g, b))

        # Viền ngoài đen gỗ
        draw.rectangle([0, 0, w - 1, h - 1], outline=border_outer)
        # Viền kim loại vàng
        draw.rectangle([1, 1, w - 2, h - 2], outline=border_gold)
        # Vệt sáng nổi 3D mép trên
        draw.line([(2, 2), (w - 3, 2)], fill=highlight_c)

        photo = ImageTk.PhotoImage(im)
        self._cache[key] = photo
        return photo


class HsoPanelRenderer:
    """Quản lý các hoa văn góc vàng (tab0, tab1, tab2) - Khử viền trắng triệt để"""
    def __init__(self, res_dir):
        self.res_dir = Path(res_dir)
        self.corner_tl = None
        self.corner_tr = None
        self.crown_img = None

        hdr_bg = (44, 22, 12, 255)  # Màu #2c160c của header
        try:
            t0_path = self.res_dir / "x1" / "interface" / "tab0.png"
            if not t0_path.exists():
                t0_path = BASE_DIR / "assets" / "x1" / "interface" / "tab0.png"
            t2_path = self.res_dir / "x1" / "interface" / "tab2.png"
            if not t2_path.exists():
                t2_path = BASE_DIR / "assets" / "x1" / "interface" / "tab2.png"

            if t0_path.exists():
                im0 = Image.open(t0_path).convert("RGBA").resize((16, 16), Image.Resampling.BILINEAR)
                bg0 = Image.new("RGBA", (16, 16), hdr_bg)
                res0 = Image.alpha_composite(bg0, im0).convert("RGB")
                self.corner_tl = ImageTk.PhotoImage(res0)
                self.corner_tr = ImageTk.PhotoImage(res0.transpose(Image.FLIP_LEFT_RIGHT))

            if t2_path.exists():
                im2 = Image.open(t2_path).convert("RGBA").resize((16, 9), Image.Resampling.BILINEAR)
                bg2 = Image.new("RGBA", (16, 9), hdr_bg)
                res2 = Image.alpha_composite(bg2, im2).convert("RGB")
                self.crown_img = ImageTk.PhotoImage(res2)
        except Exception:
            pass


class HsoButton(tk.Canvas):
    """Nút bấm chuẩn đồ họa in-game Hiệp Sĩ Online"""
    def __init__(self, parent, text, command=None, width=120, height=22, 
                 renderer=None, is_selected=False, font_size=7, gold_text=False, is_disabled=False, **kwargs):
        bg = parent.cget("bg") if hasattr(parent, "cget") else COLOR_PANEL_BG
        super().__init__(parent, width=width, height=height, bg=bg, highlightthickness=0, **kwargs)
        self.text = text
        self.command = command
        self.w = width
        self.h = height
        self.renderer = renderer
        self.is_selected = is_selected
        self.is_pressed = False
        self.is_hover = False
        self.is_disabled = is_disabled
        self.font_size = font_size
        self.gold_text = gold_text
        self._photo_ref = None

        self.bind("<Configure>", self.on_configure)
        self.bind("<Enter>", self.on_enter)
        self.bind("<Leave>", self.on_leave)
        self.bind("<Button-1>", self.on_press)
        self.bind("<ButtonRelease-1>", self.on_release)
        self.redraw()

    def on_configure(self, event):
        if event.width > 10 and event.width != self.w:
            self.w = event.width
            self.redraw()

    def redraw(self):
        self.delete("all")
        if self.renderer:
            photo = self.renderer.get_photo(
                self.w, self.h, 
                is_pressed=self.is_pressed, 
                is_selected=self.is_selected, 
                is_hover=self.is_hover,
                is_disabled=self.is_disabled
            )
            self._photo_ref = photo
            self.create_image(0, 0, image=photo, anchor="nw")
        else:
            bg = "#22120a" if self.is_disabled else (COLOR_ACTIVE_BG if (self.is_selected or self.is_pressed) else "#502816")
            bd = "#4a2c1a" if self.is_disabled else COLOR_BORDER_GOLD
            self.create_rectangle(1, 1, self.w - 1, self.h - 1, fill=bg, outline=bd)

        offset = 1 if (self.is_pressed and not self.is_disabled) else 0
        cx = self.w // 2 + offset
        cy = self.h // 2 + offset

        # Đổ bóng chữ đen
        shadow_fg = "#080302" if self.is_disabled else "#120502"
        self.create_text(cx + 1, cy + 1, text=self.text, fill=shadow_fg, font=(FONT_FAMILY, self.font_size), anchor="center")
        if self.is_disabled:
            fg = "#6a4e3c"
        elif self.gold_text or self.is_selected:
            fg = "#fff0a0"
        else:
            fg = "#ede8df"
        self.create_text(cx, cy, text=self.text, fill=fg, font=(FONT_FAMILY, self.font_size), anchor="center")

    def on_enter(self, event):
        if self.is_disabled:
            return
        self.is_hover = True
        self.redraw()

    def on_leave(self, event):
        if self.is_disabled:
            return
        self.is_hover = False
        self.redraw()

    def on_press(self, event):
        if self.is_disabled:
            return
        self.is_pressed = True
        self.redraw()

    def on_release(self, event):
        if self.is_disabled:
            return
        if self.is_pressed:
            self.is_pressed = False
            self.redraw()
            if self.command:
                self.command()

    def set_disabled(self, disabled):
        self.is_disabled = bool(disabled)
        if self.is_disabled:
            self.is_pressed = False
            self.is_hover = False
        self.redraw()

    def set_selected(self, val):
        self.is_selected = bool(val)
        self.redraw()

    def set_text(self, text):
        self.text = text
        self.redraw()

    def configure(self, **kwargs):
        if "text" in kwargs:
            self.text = kwargs["text"]
        if "gold_text" in kwargs:
            self.gold_text = kwargs["gold_text"]
        if "is_disabled" in kwargs:
            self.set_disabled(kwargs["is_disabled"])
        self.redraw()


class HsoPanel(tk.Frame):
    """Khung bảng viền vàng đồng bộ với nút bấm - Hỗ trợ tiêu đề đơn hoặc tab đôi"""
    def __init__(self, parent, title="", tabs=None, on_tab_change=None, **kwargs):
        super().__init__(parent, bg=COLOR_BORDER_GOLD, bd=1, relief="solid")

        self.container = tk.Frame(self, bg="#1e1109")
        self.container.pack(fill="both", expand=True, padx=1, pady=1)

        self.title = title
        self.tabs = list(tabs) if tabs else None
        self.active_tab_idx = 0
        self.on_tab_change = on_tab_change

        if self.title or self.tabs:
            self.hdr_canvas = tk.Canvas(self.container, height=18, bg="#2c160c", highlightthickness=0)
            self.hdr_canvas.pack(fill="x", side="top")
            self.hdr_canvas.bind("<Configure>", lambda e: self._redraw_header())
            if self.tabs:
                self.hdr_canvas.bind("<Button-1>", self._on_header_click)
            tk.Frame(self.container, bg=COLOR_BORDER_GOLD, height=1).pack(fill="x", side="top")

        self.body = tk.Frame(self.container, bg="#1e1109")
        self.body.pack(fill="both", expand=True, padx=3, pady=3)

    def set_title(self, title):
        self.title = str(title)
        if hasattr(self, "hdr_canvas"):
            self._redraw_header()

    def set_tab_title(self, idx, text):
        if self.tabs and 0 <= idx < len(self.tabs):
            self.tabs[idx] = text
            self._redraw_header()

    def _on_header_click(self, event):
        if not self.tabs or len(self.tabs) < 2:
            return
        w = self.hdr_canvas.winfo_width()
        idx = 0 if event.x < w // 2 else 1
        if idx != self.active_tab_idx:
            self.active_tab_idx = idx
            self._redraw_header()
            if self.on_tab_change:
                self.on_tab_change(idx)

    def _redraw_header(self):
        c = self.hdr_canvas
        w = c.winfo_width()
        c.delete("all")
        if w < 10:
            return

        if self.tabs and len(self.tabs) >= 2:
            mid = w // 2
            # Tab 0 (Bên trái: MONSTER)
            bg0 = "#381c10" if self.active_tab_idx == 0 else "#1c0b05"
            fg0 = "#ffdf79" if self.active_tab_idx == 0 else "#96806e"
            c.create_rectangle(0, 0, mid, 18, fill=bg0, outline="")
            c.create_text(mid // 2 + 1, 10, text=self.tabs[0], fill="#120602", font=(FONT_FAMILY, 7), anchor="center")
            c.create_text(mid // 2, 9, text=self.tabs[0], fill=fg0, font=(FONT_FAMILY, 7), anchor="center")

            # Tab 1 (Bên phải: PLAYER)
            bg1 = "#381c10" if self.active_tab_idx == 1 else "#1c0b05"
            fg1 = "#ffdf79" if self.active_tab_idx == 1 else "#96806e"
            c.create_rectangle(mid, 0, w, 18, fill=bg1, outline="")
            c.create_text(mid + (w - mid) // 2 + 1, 10, text=self.tabs[1], fill="#120602", font=(FONT_FAMILY, 7), anchor="center")
            c.create_text(mid + (w - mid) // 2, 9, text=self.tabs[1], fill=fg1, font=(FONT_FAMILY, 7), anchor="center")

            # Đường chia 2 tab
            c.create_line(mid, 0, mid, 18, fill=COLOR_BORDER_GOLD)
        else:
            c.create_rectangle(0, 0, w, 18, fill="#2c160c", outline="")
            ty = 9
            c.create_text(w // 2 + 1, ty + 1, text=self.title, fill="#120602", font=(FONT_FAMILY, 7), anchor="center")
            c.create_text(w // 2, ty, text=self.title, fill="#ffdf79", font=(FONT_FAMILY, 7), anchor="center")


class HsoStudioApp:
    def __init__(self, root):
        self.root = root
        self.root.title("Knight Online - HSO Tool")

        # Kích thước siêu gọn nhẹ
        self.root.geometry("560x365")
        self.root.minsize(520, 330)
        self.root.configure(bg=COLOR_BG_DARK)

        self.config = load_config()
        self.config["auto_attack"] = False
        self.game_proc = None
        self.game_started_ns = None
        self.state_data = {}
        self.last_farm_signature = None
        self.last_farm_mode = "off"
        self.last_farm_damage_results = 0
        self.pending_zone_change = None

        self.btn_renderer = HsoButtonRenderer()
        self.logo_photo = None
        self.load_assets()

        # Xây dựng các khu vực
        self.build_top_bar()
        self.build_bottom_bar()
        self.mid_area = tk.Frame(self.root, bg=COLOR_BG_DARK)
        self.mid_area.pack(fill="both", expand=True, padx=3, pady=3)
        self.build_left_sidebar()
        self.build_right_sidebar()
        self.build_center_content()
        self.clear_live_state()

        # Vòng lặp cập nhật IPC
        self.poll_ipc_loop()

    def load_assets(self):
        try:
            logo_path = RES_DIR / "x1" / "interface" / "logo.png"
            if not logo_path.exists():
                logo_path = RES_DIR / "x1" / "logo.png"

            if logo_path.exists():
                im_logo = Image.open(logo_path).convert("RGBA")
                lw, lh = im_logo.size
                new_w = 34
                new_h = int(lh * (34.0 / lw))
                im_logo = im_logo.resize((new_w, new_h), Image.Resampling.BILINEAR)
                bg_logo = Image.new("RGBA", (new_w, new_h), (34, 19, 13, 255))
                res_logo = Image.alpha_composite(bg_logo, im_logo).convert("RGB")
                self.logo_photo = ImageTk.PhotoImage(res_logo)

            icon_path = RES_DIR / "icon.png"
            if icon_path.exists():
                self.root.iconphoto(True, ImageTk.PhotoImage(file=str(icon_path)))
        except Exception:
            pass

    # ==============================================================
    # 1. TOP BAR (HEADER GỌN NHẸ)
    # ==============================================================
    def build_top_bar(self):
        top_frame = tk.Frame(self.root, bg=COLOR_BORDER_GOLD, bd=1, relief="solid")
        top_frame.pack(fill="x", padx=3, pady=(3, 0))

        inner_top = tk.Frame(top_frame, bg=COLOR_PANEL_BG)
        inner_top.pack(fill="x", padx=1, pady=1)

        # Logo nhỏ gọn
        logo_box = tk.Frame(inner_top, bg=COLOR_PANEL_BG)
        logo_box.pack(side="left", padx=(3, 3), pady=2)
        if self.logo_photo:
            tk.Label(logo_box, image=self.logo_photo, bg=COLOR_PANEL_BG, bd=0).pack()
        else:
            tk.Label(logo_box, text="HSO", font=(FONT_FAMILY, 8), fg=COLOR_TEXT_GOLD, bg=COLOR_PANEL_BG).pack()

        # Tên công cụ và mô tả ngay sau Logo
        brand_box = tk.Frame(inner_top, bg=COLOR_PANEL_BG)
        brand_box.pack(side="left", padx=(2, 0), pady=1)

        tk.Label(
            brand_box,
            text="Anv TOOL",
            font=(FONT_FAMILY, 8),
            fg=COLOR_TEXT_GOLD,
            bg=COLOR_PANEL_BG
        ).pack(anchor="w")

        tk.Label(
            brand_box,
            text="HSO Assistant & Automation",
            font=(FONT_FAMILY, 6),
            fg=COLOR_TEXT_MUTED,
            bg=COLOR_PANEL_BG
        ).pack(anchor="w")

        # Giữ dummy labels để an toàn tương thích ngược
        self.lbl_char_name = tk.Label(inner_top)
        self.lbl_char_lv = tk.Label(inner_top)
        self.lbl_hp_val = tk.Label(inner_top)
        self.lbl_mp_val = tk.Label(inner_top)

        # Trạng thái Online
        status_box = tk.Frame(inner_top, bg=COLOR_PANEL_BG)
        status_box.pack(side="right", padx=(0, 3), pady=3)

        self.lbl_client_status = tk.Label(
            status_box,
            text="OFFLINE",
            font=(FONT_FAMILY, 7),
            fg=COLOR_OFFLINE,
            bg=COLOR_PANEL_BG
        )
        self.lbl_client_status.pack(anchor="e")

        self.lbl_in_game_status = tk.Label(
            status_box,
            text="Login Screen",
            font=(FONT_FAMILY, 7),
            fg=COLOR_TEXT_MUTED,
            bg=COLOR_PANEL_BG
        )
        self.lbl_in_game_status.pack(anchor="e", pady=(3, 0))

    # ==============================================================
    # 2. BOTTOM BAR (TỐI ƯU GỌN GÀNG)
    # ==============================================================
    def build_bottom_bar(self):
        bot_frame = tk.Frame(self.root, bg=COLOR_BORDER_GOLD, bd=1, relief="solid")
        bot_frame.pack(fill="x", side="bottom", padx=3, pady=(0, 3))

        inner_bot = tk.Frame(bot_frame, bg=COLOR_PANEL_BG)
        inner_bot.pack(fill="x", padx=1, pady=1)

        b_content = tk.Frame(inner_bot, bg=COLOR_PANEL_BG)
        b_content.pack(fill="x", padx=3, pady=3)

        # FPS 60 bên trái
        self.btn_fps = HsoButton(
            b_content,
            text="60 FPS: ON" if self.config.get("fps_60", True) else "60 FPS: OFF",
            font_size=7,
            width=76,
            height=20,
            gold_text=True,
            renderer=self.btn_renderer,
            command=self.toggle_fps
        )
        self.btn_fps.pack(side="left")

        # Info
        tk.Label(
            b_content,
            text="Java 8 • J2ME",
            font=(FONT_FAMILY, 7),
            fg=COLOR_TEXT_MUTED,
            bg=COLOR_PANEL_BG
        ).pack(side="left", padx=(6, 0))

        # Nút Play / Stop Game gộp làm 1, đặt ở góc phải
        is_running = bool(self.game_proc and self.game_proc.poll() is None)
        self.btn_game_toggle = HsoButton(
            b_content,
            text="Stop Game" if is_running else "Play Game",
            font_size=8,
            width=85,
            height=20,
            gold_text=True,
            renderer=self.btn_renderer,
            command=self.toggle_game
        )
        self.btn_game_toggle.pack(side="right")

    # ==============================================================
    # 3. LEFT SIDEBAR (LOCATION & MONSTERS/PLAYERS)
    # ==============================================================
    def build_left_sidebar(self):
        left_container = tk.Frame(self.mid_area, bg=COLOR_BG_DARK, width=148)
        left_container.pack(side="left", fill="y", padx=(0, 3))
        left_container.pack_propagate(False)

        # LOCATION
        p_map = HsoPanel(left_container, title="LOCATION")
        p_map.pack(fill="x", pady=(0, 3))
        c_map = p_map.body

        r_m1 = tk.Frame(c_map, bg="#1e1109")
        r_m1.pack(fill="x", pady=(0, 3))
        tk.Label(r_m1, text="Map:", font=(FONT_FAMILY, 7), fg=COLOR_TEXT_MUTED, bg="#1e1109").pack(side="left")
        self.lbl_map_name = tk.Label(r_m1, text="Connecting...", font=(FONT_FAMILY, 7), fg=COLOR_TEXT_GOLD, bg="#1e1109")
        self.lbl_map_name.pack(side="left", padx=(3, 0))

        r_m2 = tk.Frame(c_map, bg="#1e1109")
        r_m2.pack(fill="x", pady=(0, 3))
        self.lbl_zone_val = tk.Label(r_m2, text="Zone: --", font=(FONT_FAMILY, 7), fg=COLOR_TEXT_WHITE, bg="#1e1109")
        self.lbl_zone_val.pack(side="left")

        cz_row = tk.Frame(c_map, bg="#1e1109")
        cz_row.pack(fill="x", pady=0)

        tk.Label(cz_row, text="Change:", font=(FONT_FAMILY, 7), fg=COLOR_TEXT_MUTED, bg="#1e1109").pack(side="left")

        # Nút số khu hiện tại: bấm vào mở modal chọn khu
        self.btn_zone_select = HsoButton(
            cz_row,
            text="1",
            font_size=7,
            width=36,
            height=20,
            gold_text=True,
            renderer=self.btn_renderer,
            command=self.open_zone_modal
        )
        self.btn_zone_select.pack(side="left", padx=(3, 0))

        btn_v_vang = HsoButton(
            cz_row,
            text="Lowest",
            font_size=7,
            width=54,
            height=20,
            gold_text=True,
            renderer=self.btn_renderer,
            command=self.send_auto_min_area
        )
        btn_v_vang.pack(side="left", padx=(3, 0))

        # MONSTERS / PLAYERS DUAL TAB PANEL
        self.active_mon_tab = 0
        self.p_mon = HsoPanel(
            left_container,
            tabs=["MONSTERS", "PLAYER (0)"],
            on_tab_change=self.on_mon_tab_change
        )
        self.p_mon.pack(fill="both", expand=True)
        c_mon = self.p_mon.body

        list_container = tk.Frame(c_mon, bg=COLOR_INNER_BG)
        list_container.pack(fill="both", expand=True)

        self.canvas_mon = tk.Canvas(list_container, bg=COLOR_INNER_BG, highlightthickness=0)
        self.mon_scrollbar = ttk.Scrollbar(list_container, orient="vertical", command=self.canvas_mon.yview)

        self.canvas_mon.configure(yscrollcommand=self.mon_scrollbar.set)
        self.canvas_mon.pack(side="left", fill="both", expand=True)
        self.mon_scrollbar.pack(side="right", fill="y")

        self.canvas_mon.bind("<Configure>", lambda e: self.refresh_mon_panel())
        self.canvas_mon.bind("<MouseWheel>", lambda e: self.canvas_mon.yview_scroll(int(-1 * (e.delta / 120)), "units"))
        self.canvas_mon.bind("<Button-4>", lambda e: self.canvas_mon.yview_scroll(-1, "units"))
        self.canvas_mon.bind("<Button-5>", lambda e: self.canvas_mon.yview_scroll(1, "units"))
        self.canvas_mon.bind("<Button-1>", self.pin_monster_from_panel)
        self.monster_rows = []

    # ==============================================================
    # POPUP DIALOG CHỌN ĐỘ PHÂN GIẢI / TỰ NHẬP
    # ==============================================================
    def open_res_dialog(self):
        dlg = tk.Toplevel(self.root)
        dlg.title("Display Settings")
        dlg.geometry("230x250")
        dlg.resizable(False, False)
        dlg.configure(bg=COLOR_BG_DARK)
        dlg.transient(self.root)
        dlg.grab_set()

        x = self.root.winfo_x() + (self.root.winfo_width() - 230) // 2
        y = self.root.winfo_y() + (self.root.winfo_height() - 250) // 2
        dlg.geometry(f"+{x}+{y}")

        panel = HsoPanel(dlg, title="SCREEN RESOLUTION")
        panel.pack(fill="both", expand=True, padx=3, pady=3)
        body = panel.body

        presets = [
            ("320x240", "320 x 240 (Tiny)"),
            ("360x240", "360 x 240 (Mini 3:2)"),
            ("480x320", "480 x 320 (Classic)"),
            ("640x360", "640 x 360 (16:9)"),
            ("800x480", "800 x 480 (Default PC)"),
            ("1024x600", "1024 x 600 (Wide HD)")
        ]

        def on_select_preset(val):
            w_str, h_str = val.split("x")
            self.set_resolution(int(w_str), int(h_str), val)
            dlg.destroy()

        cur_preset = self.config.get("preset", "800x480")

        for val, title in presets:
            btn = HsoButton(
                body,
                text=title,
                font_size=7,
                width=200,
                height=21,
                is_selected=(val == cur_preset),
                renderer=self.btn_renderer,
                command=lambda v=val: on_select_preset(v)
            )
            btn.pack(fill="x", pady=1)

        tk.Frame(body, bg="#3d1b10", height=1).pack(fill="x", pady=3)

        custom_row = tk.Frame(body, bg="#1e1109")
        custom_row.pack(fill="x", pady=2)
        tk.Label(custom_row, text="Custom:", font=(FONT_FAMILY, 7), fg=COLOR_TEXT_MUTED, bg="#1e1109").pack(side="left")

        ent_w = tk.Entry(custom_row, width=3, bg="#b88a4c", fg="#ffffff", insertbackground="#ffffff", bd=1, relief="solid", justify="center", font=(FONT_FAMILY, 7))
        ent_w.insert(0, str(self.config.get("width", 800)))
        ent_w.pack(side="left", padx=2)

        tk.Label(custom_row, text="x", font=(FONT_FAMILY, 7), fg=COLOR_TEXT_GOLD, bg="#1e1109").pack(side="left")

        ent_h = tk.Entry(custom_row, width=3, bg="#b88a4c", fg="#ffffff", insertbackground="#ffffff", bd=1, relief="solid", justify="center", font=(FONT_FAMILY, 7))
        ent_h.insert(0, str(self.config.get("height", 480)))
        ent_h.pack(side="left", padx=2)

        def apply_custom():
            try:
                cw = int(ent_w.get().strip())
                ch = int(ent_h.get().strip())
                if cw < 200 or ch < 150:
                    messagebox.showerror("Error", "Minimum size is 200x150!", parent=dlg)
                    return
                self.set_resolution(cw, ch, f"{cw}x{ch}")
                dlg.destroy()
            except Exception:
                messagebox.showerror("Error", "Invalid dimensions!", parent=dlg)

        btn_apply = HsoButton(
            custom_row,
            text="Save",
            font_size=7,
            width=36,
            height=20,
            renderer=self.btn_renderer,
            command=apply_custom
        )
        btn_apply.pack(side="left", padx=3)

    def set_resolution(self, w, h, preset_name):
        self.config["width"] = w
        self.config["height"] = h
        self.config["preset"] = preset_name
        save_config(self.config)
        self.btn_screen.set_text(f"Display: {w}x{h}")
        self.log_msg(f"Display resolution set: {w}x{h}")

    # ==============================================================
    # CÁC NÚT TÍNH NĂNG (GỬI LỆNH IPC)
    # ==============================================================
    def toggle_auto_attack(self):
        cur = self.config.get("auto_attack", False)
        new_val = not cur
        if new_val:
            self.sync_farm_targets(force=True)
        self.config["auto_attack"] = new_val
        save_config(self.config)

        self.btn_auto_attack.set_selected(new_val)
        self.btn_auto_attack.configure(gold_text=new_val)
        self.btn_auto_attack.set_text(f"Auto Attack: {'ON' if new_val else 'OFF'}")

        try:
            self.write_command({"action": "auto_attack", "enabled": new_val})
        except OSError as e:
            self.log_msg("Không gửi được lệnh Auto Attack: " + str(e))
        self.log_msg(f"Auto Attack: {'ON' if new_val else 'OFF'}")

    def decrease_speed(self):
        cur = str(self.config.get("speed", "1.0"))
        try:
            idx = SPEED_LEVELS.index(cur)
        except Exception:
            idx = 0
        if idx > 0:
            self.set_speed(SPEED_LEVELS[idx - 1])

    def increase_speed(self):
        cur = str(self.config.get("speed", "1.0"))
        try:
            idx = SPEED_LEVELS.index(cur)
        except Exception:
            idx = 0
        if idx < len(SPEED_LEVELS) - 1:
            self.set_speed(SPEED_LEVELS[idx + 1])

    def set_speed(self, new_spd):
        self.config["speed"] = new_spd
        save_config(self.config)

        self.update_speed_ui()

        try:
            self.write_command({"action": "speed", "speed": float(new_spd)})
        except OSError as e:
            self.log_msg("Không gửi được lệnh tốc độ: " + str(e))
        self.log_msg(f"Speed: {new_spd}x")

    def update_speed_ui(self):
        cur_spd = str(self.config.get("speed", "1.0"))
        try:
            idx = SPEED_LEVELS.index(cur_spd)
        except Exception:
            idx = 0
            cur_spd = SPEED_LEVELS[0]

        if hasattr(self, "btn_speed_disp"):
            self.btn_speed_disp.set_text(f"Speed: {cur_spd}x")
            self.btn_speed_disp.configure(gold_text=(cur_spd != "1.0"))

        if hasattr(self, "btn_speed_down"):
            self.btn_speed_down.set_disabled(idx <= 0)

        if hasattr(self, "btn_speed_up"):
            self.btn_speed_up.set_disabled(idx >= len(SPEED_LEVELS) - 1)

    def toggle_auto_pickup(self):
        cur = self.config.get("auto_pickup", False)
        new_val = not cur
        self.config["auto_pickup"] = new_val
        save_config(self.config)

        self.btn_auto_pickup.set_selected(new_val)
        self.btn_auto_pickup.configure(gold_text=new_val)
        self.btn_auto_pickup.set_text(f"Auto Loot: {'ON' if new_val else 'OFF'}")

        try:
            self.write_command({"action": "auto_pickup", "enabled": new_val})
        except OSError as e:
            self.log_msg("Không gửi được lệnh Auto Loot: " + str(e))
        self.log_msg(f"Auto Loot: {'ON' if new_val else 'OFF'}")

    # ==============================================================
    # ==============================================================
    # 4. RIGHT SIDEBAR (PLAYER INFO & CONTROLS)
    # ==============================================================
    def build_right_sidebar(self):
        right_container = tk.Frame(self.mid_area, bg=COLOR_BG_DARK, width=148)
        right_container.pack(side="right", fill="y", padx=(3, 0))
        right_container.pack_propagate(False)

        # 1. PLAYER PANEL (ID/TÊN NHÂN VẬT & 3 THANH HP, MP, EXP)
        self.p_char = HsoPanel(right_container, title="Offline")
        self.p_char.pack(fill="x", pady=(0, 3))
        c_char = self.p_char.body

        # Canvas vẽ 3 thanh HP, MP, EXP
        self.canvas_player_bars = tk.Canvas(c_char, bg="#1e1109", height=42, highlightthickness=0)
        self.canvas_player_bars.pack(fill="x", expand=True)
        self.canvas_player_bars.bind("<Configure>", lambda e: self.redraw_player_bars())

        # 2. CONTROLS PANEL
        p_func = HsoPanel(right_container, title="CONTROLS")
        p_func.pack(fill="both", expand=True)
        c_func = p_func.body

        cur_w = self.config.get("width", 800)
        cur_h = self.config.get("height", 480)
        self.btn_screen = HsoButton(
            c_func,
            text=f"Display: {cur_w}x{cur_h}",
            font_size=7,
            width=96,
            height=20,
            renderer=self.btn_renderer,
            command=self.open_res_dialog
        )
        self.btn_screen.pack(fill="x", pady=(0, 3))

        is_attack = self.config.get("auto_attack", False)
        self.btn_auto_attack = HsoButton(
            c_func,
            text=f"Auto Attack: {'ON' if is_attack else 'OFF'}",
            font_size=7,
            width=96,
            height=20,
            is_selected=is_attack,
            gold_text=is_attack,
            renderer=self.btn_renderer,
            command=self.toggle_auto_attack
        )
        self.btn_auto_attack.pack(fill="x", pady=(0, 3))

        self.btn_auto_farm = HsoButton(
            c_func, text="Auto Fam: Cấu hình", font_size=7,
            width=96, height=20, renderer=self.btn_renderer,
            command=self.open_auto_farm_modal
        )
        self.btn_auto_farm.pack(fill="x", pady=(0, 3))

        self.btn_unpin = HsoButton(
            c_func, text="Bỏ ghim mục tiêu", font_size=7,
            width=96, height=20, renderer=self.btn_renderer,
            command=self.unpin_monster
        )
        self.btn_unpin.pack(fill="x", pady=(0, 3))
        self.btn_unpin.set_disabled(True)

        # Row Speed: [-] [Speed: 1.0x] [+]
        row_speed = tk.Frame(c_func, bg="#1e1109")
        row_speed.pack(fill="x", pady=(0, 3))

        cur_spd = str(self.config.get("speed", "1.0"))
        if cur_spd not in SPEED_LEVELS:
            cur_spd = "1.0"

        self.btn_speed_down = HsoButton(
            row_speed,
            text="-",
            font_size=8,
            width=22,
            height=20,
            renderer=self.btn_renderer,
            command=self.decrease_speed
        )
        self.btn_speed_down.pack(side="left", padx=(0, 2))

        self.btn_speed_up = HsoButton(
            row_speed,
            text="+",
            font_size=8,
            width=22,
            height=20,
            renderer=self.btn_renderer,
            command=self.increase_speed
        )
        self.btn_speed_up.pack(side="right", padx=(2, 0))

        self.btn_speed_disp = HsoButton(
            row_speed,
            text=f"Speed: {cur_spd}x",
            font_size=7,
            height=20,
            renderer=self.btn_renderer,
            gold_text=(cur_spd != "1.0"),
            command=None
        )
        self.btn_speed_disp.pack(side="left", fill="x", expand=True)

        self.update_speed_ui()

        is_pickup = self.config.get("auto_pickup", False)
        self.btn_auto_pickup = HsoButton(
            c_func,
            text=f"Auto Loot: {'ON' if is_pickup else 'OFF'}",
            font_size=7,
            width=96,
            height=20,
            is_selected=is_pickup,
            gold_text=is_pickup,
            renderer=self.btn_renderer,
            command=self.toggle_auto_pickup
        )
        self.btn_auto_pickup.pack(fill="x", pady=0)

    def redraw_player_bars(self):
        c = self.canvas_player_bars
        c.delete("all")
        w = c.winfo_width()
        if w < 10:
            w = 136

        p = self.state_data.get("player", {})
        hp = max(0, p.get("hp", 0))
        max_hp = max(1, p.get("maxHp", 1))
        mp = max(0, p.get("mp", 0))
        max_mp = max(1, p.get("maxmp", max(1, p.get("maxMp", 1))))
        exp_val = p.get("exp", 0)

        hp_pct = min(1.0, max(0.0, float(hp) / float(max_hp)))
        mp_pct = min(1.0, max(0.0, float(mp) / float(max_mp)))
        # exp_val is in tenths of a percent (e.g. 250 -> 25.0%)
        exp_pct = min(1.0, max(0.0, float(exp_val) / 1000.0))
        exp_str = f"EXP: {exp_val / 10.0:.1f}%"

        bh = 10
        pad_x = 2
        mid_x = w // 2

        # 1. HP BAR (Red)
        y1 = 1
        c.create_rectangle(pad_x, y1, w - pad_x, y1 + bh, fill="#1c0b05", outline="#5a1a15")
        fill_hp = int((w - 2 * pad_x - 2) * hp_pct)
        if fill_hp > 0:
            c.create_rectangle(pad_x + 1, y1 + 1, pad_x + 1 + fill_hp, y1 + bh - 1, fill=COLOR_HP_RED, outline="")
            c.create_rectangle(pad_x + 1, y1 + 1, pad_x + 1 + fill_hp, y1 + 2, fill="#ff7a7a", outline="")
        hp_str = f"HP: {hp}/{max_hp}"
        c.create_text(mid_x + 1, y1 + bh // 2 + 1, text=hp_str, fill="#000000", font=(FONT_FAMILY, 6), anchor="center")
        c.create_text(mid_x, y1 + bh // 2, text=hp_str, fill="#ffffff", font=(FONT_FAMILY, 6), anchor="center")

        # 2. MP BAR (Blue)
        y2 = 15
        c.create_rectangle(pad_x, y2, w - pad_x, y2 + bh, fill="#0b101c", outline="#15305a")
        fill_mp = int((w - 2 * pad_x - 2) * mp_pct)
        if fill_mp > 0:
            c.create_rectangle(pad_x + 1, y2 + 1, pad_x + 1 + fill_mp, y2 + bh - 1, fill=COLOR_MP_BLUE, outline="")
            c.create_rectangle(pad_x + 1, y2 + 1, pad_x + 1 + fill_mp, y2 + 2, fill="#7fb3d5", outline="")
        mp_str = f"MP: {mp}/{max_mp}"
        c.create_text(mid_x + 1, y2 + bh // 2 + 1, text=mp_str, fill="#000000", font=(FONT_FAMILY, 6), anchor="center")
        c.create_text(mid_x, y2 + bh // 2, text=mp_str, fill="#ffffff", font=(FONT_FAMILY, 6), anchor="center")

        # 3. EXP BAR (Green - Xanh lá)
        y3 = 29
        c.create_rectangle(pad_x, y3, w - pad_x, y3 + bh, fill="#0b1c0e", outline="#145a32")
        fill_exp = int((w - 2 * pad_x - 2) * exp_pct)
        if fill_exp > 0:
            c.create_rectangle(pad_x + 1, y3 + 1, pad_x + 1 + fill_exp, y3 + bh - 1, fill="#27ae60", outline="")
            c.create_rectangle(pad_x + 1, y3 + 1, pad_x + 1 + fill_exp, y3 + 2, fill="#58d68d", outline="")
        c.create_text(mid_x + 1, y3 + bh // 2 + 1, text=exp_str, fill="#000000", font=(FONT_FAMILY, 6), anchor="center")
        c.create_text(mid_x, y3 + bh // 2, text=exp_str, fill="#ffffff", font=(FONT_FAMILY, 6), anchor="center")

    def on_mon_tab_change(self, idx):
        self.active_mon_tab = idx
        self.refresh_mon_panel()

    def pin_monster_from_panel(self, event):
        if self.active_mon_tab != 0 or not self.state_data.get("in_game"):
            return
        y = self.canvas_mon.canvasy(event.y)
        for top, bottom, monster_id in self.monster_rows:
            if top <= y < bottom:
                if monster_id == self.state_data.get("pinned_monster_id", -1):
                    self.unpin_monster()
                    return
                try:
                    self.write_command({
                        "action": "pin_monster", "id": monster_id,
                        "map_id": self.state_data["map_id"], "area": self.state_data["area"]
                    })
                    self.log_msg(f"Đang ghim quái ID {monster_id}...")
                except (OSError, KeyError) as e:
                    self.log_msg("Không ghim được quái: " + str(e))
                return

    def unpin_monster(self):
        try:
            self.write_command({"action": "unpin_monster"})
            self.log_msg("Đang bỏ ghim mục tiêu...")
        except OSError as e:
            self.log_msg("Không bỏ ghim được: " + str(e))

    def sync_farm_targets(self, data=None, force=False):
        data = data or self.state_data
        if not data.get("in_game"):
            self.last_farm_signature = None
            return
        map_id, area = data.get("map_id", 0), data.get("area", 0)
        profile = self.config.get("auto_farm", {}).get(farm_profile_key(map_id, area))
        ids = resolve_farm_ids(profile, data.get("map_monsters", data.get("monsters", [])),
                               include_dead=True)
        if hasattr(self, "btn_auto_farm"):
            self.btn_auto_farm.set_selected(profile is not None)
            self.btn_auto_farm.set_text(f"Auto Fam: {len(ids)} quái" if profile is not None
                                        else "Auto Fam: Cấu hình")
        signature = (map_id, area, profile is not None, tuple(ids))
        if not force and signature == self.last_farm_signature:
            return
        try:
            self.write_command({
                "action": "farm_targets", "map_id": map_id, "area": area,
                "configured": 1 if profile is not None else 0,
                "ids": ";".join(str(monster_id) for monster_id in ids)
            })
            self.last_farm_signature = signature
        except OSError as e:
            self.log_msg("Không đồng bộ được Auto Fam: " + str(e))

    def open_auto_farm_modal(self):
        data = self.state_data
        if not data.get("in_game"):
            messagebox.showinfo("Auto Fam", "Hãy vào map game trước khi chọn quái.")
            return
        old_dialog = getattr(self, "auto_farm_dialog", None)
        if old_dialog is not None and old_dialog.winfo_exists():
            old_dialog.lift()
            return

        map_id, area = data["map_id"], data["area"]
        profile_key = farm_profile_key(map_id, area)
        profile = self.config.get("auto_farm", {}).get(profile_key)
        try:
            self.write_command({"action": "scan_monsters"})
        except OSError as e:
            self.log_msg("Không bắt đầu được quét quái: " + str(e))

        dlg = tk.Toplevel(self.root)
        self.auto_farm_dialog = dlg
        dlg.title("Auto Fam")
        dlg.geometry("365x430")
        dlg.minsize(300, 320)
        dlg.configure(bg=COLOR_BG_DARK)
        dlg.transient(self.root)
        dlg.grab_set()

        panel = HsoPanel(dlg, title="AUTO FAM - CHỌN QUÁI")
        panel.pack(fill="both", expand=True, padx=4, pady=4)
        body = panel.body
        title_var = tk.StringVar(value="")
        status_var = tk.StringVar(value="")
        selected_count = tk.StringVar(value="")
        tk.Label(body, textvariable=title_var, font=(FONT_FAMILY, 8),
                 fg=COLOR_TEXT_GOLD, bg="#1e1109").pack(anchor="w", pady=(0, 3))
        tk.Label(body, textvariable=status_var, font=(FONT_FAMILY, 7),
                 fg=COLOR_TEXT_MUTED, bg="#1e1109").pack(anchor="w")

        all_var = tk.BooleanVar(value=False)
        monster_vars = {}
        group_vars = {}
        group_labels = {}
        group_expanded = {}
        groups = {}
        monsters_by_id = {}
        health_canvases = {}
        roster_signature = None
        selection_touched = False

        def refresh_checks():
            for name, members in groups.items():
                chosen = sum(monster_vars[m["id"]].get() for m in members)
                group_vars[name].set(chosen == len(members))
                group_labels[name].set(f"{name} ({chosen}/{len(members)})")
            chosen_total = sum(var.get() for var in monster_vars.values())
            all_var.set(bool(monster_vars) and chosen_total == len(monster_vars))
            selected_count.set(f"Đã chọn {chosen_total}/{len(monster_vars)} quái")

        def set_all(value):
            nonlocal selection_touched
            selection_touched = True
            for var in monster_vars.values():
                var.set(value)
            refresh_checks()

        tk.Checkbutton(body, text="Chọn tất cả quái trong map", variable=all_var,
                       command=lambda: set_all(all_var.get()), font=(FONT_FAMILY, 8),
                       bg="#1e1109", fg=COLOR_TEXT_GOLD, selectcolor=COLOR_INNER_BG,
                       activebackground="#1e1109", activeforeground=COLOR_TEXT_GOLD).pack(anchor="w")
        tk.Label(body, textvariable=selected_count, font=(FONT_FAMILY, 7),
                 bg="#1e1109", fg=COLOR_TEXT_MUTED).pack(anchor="w", pady=(0, 4))

        bottom = tk.Frame(body, bg="#1e1109")
        bottom.pack(side="bottom", fill="x", pady=(5, 0))
        list_box = tk.Frame(body, bg=COLOR_INNER_BG)
        list_box.pack(fill="both", expand=True)
        canvas = tk.Canvas(list_box, bg=COLOR_INNER_BG, highlightthickness=0)
        scrollbar = ttk.Scrollbar(list_box, orient="vertical", command=canvas.yview)
        canvas.configure(yscrollcommand=scrollbar.set)
        canvas.pack(side="left", fill="both", expand=True)
        scrollbar.pack(side="right", fill="y")
        items = tk.Frame(canvas, bg=COLOR_INNER_BG)
        window = canvas.create_window((0, 0), window=items, anchor="nw")
        items.bind("<Configure>", lambda event: canvas.configure(scrollregion=canvas.bbox("all")))
        canvas.bind("<Configure>", lambda event: canvas.itemconfigure(window, width=event.width))
        canvas.bind("<MouseWheel>", lambda event: canvas.yview_scroll(int(-event.delta / 120), "units"))
        canvas.bind("<Button-4>", lambda event: canvas.yview_scroll(-1, "units"))
        canvas.bind("<Button-5>", lambda event: canvas.yview_scroll(1, "units"))

        def paint_health_row(monster_id, current_data):
            bar = health_canvases.get(monster_id)
            monster = monsters_by_id.get(monster_id)
            if bar is None or monster is None:
                return
            width = max(180, bar.winfo_width())
            hp = max(0, monster.get("hp", 0))
            max_hp = max(1, monster.get("maxHp", 1))
            fill = min(1.0, hp / max_hp)
            player = current_data.get("player", {})
            distance = int(((monster.get("x", 0) - player.get("x", 0)) ** 2 +
                            (monster.get("y", 0) - player.get("y", 0)) ** 2) ** 0.5)
            bar_width = min(118, max(82, int(width * 0.43)))
            left, right = width - bar_width - 3, width - 3
            bar.delete("all")
            bar.create_text(3, 8, text=f"#{monster_id} · {distance}px",
                            fill=COLOR_TEXT_WHITE, font=(FONT_FAMILY, 7), anchor="w")
            bar.create_rectangle(left, 2, right, 14, fill="#1c0b05", outline="#5a3118")
            if fill > 0:
                filled_right = left + 1 + int((bar_width - 2) * fill)
                bar.create_rectangle(left + 1, 3, filled_right, 13,
                                     fill=COLOR_HP_RED, outline="")
                bar.create_line(left + 1, 4, filled_right, 4, fill="#ff7a7a")
            bar.create_text((left + right) / 2 + 1, 9, text=f"{hp}/{max_hp}",
                            fill="#000000", font=(FONT_FAMILY, 6))
            bar.create_text((left + right) / 2, 8, text=f"{hp}/{max_hp}",
                            fill="#ffffff", font=(FONT_FAMILY, 6))

        def refresh_modal(current_data):
            nonlocal roster_signature, monsters_by_id, groups
            if not dlg.winfo_exists():
                return
            if (current_data.get("map_id"), current_data.get("area")) != (map_id, area):
                close_dialog()
                return
            roster = current_data.get("map_monsters", current_data.get("monsters", []))
            current = {m["id"]: m for m in roster}
            signature = tuple(sorted((m["id"], m.get("name", "")) for m in current.values()))
            progress = current_data.get("monster_scan_progress", 0)
            limit = current_data.get("monster_scan_limit", 512)
            if current_data.get("monster_scan_active"):
                status_var.set(f"Đang hỏi ID quái: {progress}/{limit} · Server trả {len(current)}")
            elif current_data.get("monster_scan_done"):
                status_var.set(f"Đã hỏi ID 0-{limit - 1} · Server trả {len(current)} quái")
            else:
                status_var.set(f"Đang chuẩn bị quét · Hiện có {len(current)} quái")
            title_var.set(f"{current_data.get('map_name', 'Map')} · Khu {area + 1}")
            monsters_by_id = current
            if signature == roster_signature:
                for monster_id in health_canvases:
                    paint_health_row(monster_id, current_data)
                return

            old_all = all_var.get()
            old_groups = {name: var.get() for name, var in group_vars.items()}
            for monster in current.values():
                monster_id = monster["id"]
                if monster_id not in monster_vars:
                    selected = (old_all or old_groups.get(monster.get("name", "Mob"), False)) \
                        if selection_touched else farm_profile_matches(profile, monster)
                    monster_vars[monster_id] = tk.BooleanVar(value=selected)
            groups = {}
            for monster in current.values():
                groups.setdefault(monster.get("name", "Mob"), []).append(monster)
            previous_expanded = {name: value.get() for name, value in group_expanded.items()}
            group_vars.clear()
            group_labels.clear()
            group_expanded.clear()
            for child in items.winfo_children():
                child.destroy()
            health_canvases.clear()
            if not current:
                tk.Label(items, text="Đang đợi danh sách quái từ game...",
                         bg=COLOR_INNER_BG, fg=COLOR_TEXT_MUTED,
                         font=(FONT_FAMILY, 8)).pack(padx=5, pady=8)

            px = current_data.get("player", {}).get("x", 0)
            py = current_data.get("player", {}).get("y", 0)
            for name in sorted(groups):
                members = sorted(groups[name], key=lambda m: (
                    (m.get("x", 0) - px) ** 2 + (m.get("y", 0) - py) ** 2, m["id"]))
                group_vars[name] = tk.BooleanVar(value=False)
                group_labels[name] = tk.StringVar(value="")
                group_expanded[name] = tk.BooleanVar(value=previous_expanded.get(name, False))
                header = tk.Frame(items, bg="#382014")
                header.pack(fill="x", pady=(3, 1))
                children = tk.Frame(items, bg=COLOR_INNER_BG)
                arrow = tk.Label(header, text="▼" if group_expanded[name].get() else "▶",
                                 width=2, font=(FONT_FAMILY, 8), bg="#382014", fg=COLOR_TEXT_GOLD)
                arrow.pack(side="left")

                def toggle_expanded(group_name=name, group_children=children,
                                    group_header=header, group_arrow=arrow):
                    expanded = not group_expanded[group_name].get()
                    group_expanded[group_name].set(expanded)
                    group_arrow.configure(text="▼" if expanded else "▶")
                    if expanded:
                        group_children.pack(fill="x", after=group_header)
                    else:
                        group_children.pack_forget()

                arrow.bind("<Button-1>", lambda event, command=toggle_expanded: command())

                def toggle_group(group_name=name):
                    nonlocal selection_touched
                    selection_touched = True
                    for monster in groups[group_name]:
                        monster_vars[monster["id"]].set(group_vars[group_name].get())
                    refresh_checks()

                tk.Checkbutton(header, textvariable=group_labels[name], variable=group_vars[name],
                               command=toggle_group, font=(FONT_FAMILY, 8), anchor="w",
                               bg="#382014", fg=COLOR_TEXT_GOLD, selectcolor=COLOR_INNER_BG,
                               activebackground="#382014", activeforeground=COLOR_TEXT_GOLD).pack(
                                   side="left", fill="x", expand=True)
                for monster in members:
                    monster_id = monster["id"]
                    def toggle_monster(selected_id=monster_id, from_bar=False):
                        nonlocal selection_touched
                        if from_bar:
                            monster_vars[selected_id].set(not monster_vars[selected_id].get())
                        selection_touched = True
                        refresh_checks()
                    row = tk.Frame(children, bg=COLOR_INNER_BG)
                    row.pack(fill="x", padx=(12, 0))
                    tk.Checkbutton(row, text="", variable=monster_vars[monster_id],
                                   command=toggle_monster, width=2, padx=0,
                                   font=(FONT_FAMILY, 7), anchor="w", bg=COLOR_INNER_BG,
                                   fg=COLOR_TEXT_WHITE, selectcolor=COLOR_INNER_BG,
                                   activebackground=COLOR_INNER_BG,
                                   activeforeground=COLOR_TEXT_WHITE).pack(side="left")
                    health_bar = tk.Canvas(row, height=16, bg=COLOR_INNER_BG,
                                           highlightthickness=0, bd=0, cursor="hand2")
                    health_bar.pack(side="left", fill="x", expand=True)
                    health_canvases[monster_id] = health_bar
                    health_bar.bind("<Configure>",
                                    lambda event, selected_id=monster_id:
                                    paint_health_row(selected_id, self.state_data))
                    health_bar.bind("<Button-1>",
                                    lambda event, selected_id=monster_id:
                                    toggle_monster(selected_id, from_bar=True))
                    paint_health_row(monster_id, current_data)
                if group_expanded[name].get():
                    children.pack(fill="x")
            roster_signature = signature
            refresh_checks()

        def close_dialog():
            self.auto_farm_refresh = None
            self.auto_farm_dialog = None
            dlg.destroy()

        def save_farm():
            if (self.state_data.get("map_id"), self.state_data.get("area")) != (map_id, area):
                messagebox.showerror("Auto Fam", "Đã đổi map hoặc khu. Hãy mở lại Auto Fam để quét mới.", parent=dlg)
                return
            chosen = {monster_id for monster_id, var in monster_vars.items() if var.get()}
            new_profile = farm_profile_from_selection(list(monsters_by_id.values()), chosen)
            farm_profiles = self.config.setdefault("auto_farm", {})
            previous_profile = farm_profiles.get(profile_key)
            farm_profiles[profile_key] = new_profile
            if not save_config(self.config):
                if previous_profile is None:
                    del farm_profiles[profile_key]
                else:
                    farm_profiles[profile_key] = previous_profile
                messagebox.showerror("Auto Fam", "Không lưu được cấu hình Auto Fam.", parent=dlg)
                return
            self.sync_farm_targets(force=True)
            self.log_msg(f"Đã lưu Auto Fam: {len(chosen)} quái ở map {map_id}, khu {area + 1}.")
            close_dialog()

        HsoButton(bottom, text="Tất cả", font_size=7, width=58, height=20,
                  renderer=self.btn_renderer, command=lambda: set_all(True)).pack(side="left", padx=(0, 3))
        HsoButton(bottom, text="Bỏ chọn", font_size=7, width=58, height=20,
                  renderer=self.btn_renderer, command=lambda: set_all(False)).pack(side="left")
        HsoButton(bottom, text="Đóng", font_size=7, width=48, height=20,
                  renderer=self.btn_renderer, command=close_dialog).pack(side="right")
        HsoButton(bottom, text="Lưu", font_size=7, width=48, height=20,
                  gold_text=True, renderer=self.btn_renderer, command=save_farm).pack(side="right", padx=(0, 3))
        dlg.protocol("WM_DELETE_WINDOW", close_dialog)
        self.auto_farm_refresh = refresh_modal
        refresh_modal(data)

    def refresh_mon_panel(self):
        if getattr(self, "active_mon_tab", 0) == 0:
            self.render_monster_hp_bars(self.state_data.get("monsters", []))
        else:
            self.render_players_list(self.state_data.get("players", []))

    def render_players_list(self, players):
        c = self.canvas_mon
        c.delete("all")

        cw = c.winfo_width()
        if cw < 30:
            cw = 136

        if not players:
            c.create_text(
                cw // 2, 30,
                text="No other players",
                fill=COLOR_TEXT_MUTED,
                font=(FONT_FAMILY, 7),
                justify="center",
                anchor="center"
            )
            c.configure(scrollregion=(0, 0, cw, 60))
            return

        item_h = 13
        pad_y = 3
        y_offset = 1

        for pl in players:
            p_name = pl.get("name", "Player")
            p_lv = pl.get("lv", "--")
            mid_y = y_offset + item_h // 2

            c.create_text(
                3, mid_y,
                text=p_name,
                fill=COLOR_ONLINE,
                font=(FONT_FAMILY, 7),
                anchor="w"
            )
            c.create_text(
                cw - 3, mid_y,
                text=f"Lv. {p_lv}" if p_lv != "--" else "",
                fill=COLOR_TEXT_GOLD,
                font=(FONT_FAMILY, 7),
                anchor="e"
            )
            y_offset += item_h + pad_y

        c.configure(scrollregion=(0, 0, cw, y_offset + 4))

    def render_monster_hp_bars(self, monsters):
        c = self.canvas_mon
        c.delete("all")
        self.monster_rows = []

        cw = c.winfo_width()
        if cw < 30:
            cw = 136

        if not monsters:
            c.create_text(
                cw // 2, 30,
                text="No monsters in area",
                fill=COLOR_TEXT_MUTED,
                font=(FONT_FAMILY, 7),
                justify="center",
                anchor="center"
            )
            c.configure(scrollregion=(0, 0, cw, 60))
            return

        item_h = 13
        pad_y = 3
        y_offset = 1
        bar_w = min(60, max(46, int(cw * 0.40)))
        bh = 10

        focus_id = self.state_data.get("focus_monster_id", -1)
        pinned_id = self.state_data.get("pinned_monster_id", -1)
        monsters = sort_monsters_for_panel(monsters, self.state_data.get("player", {}), focus_id, pinned_id)

        for m in monsters:
            m_id = m.get("id", 0)
            m_name = m.get("name", "Mob")
            hp = max(0, m.get("hp", 0))
            max_hp = max(1, m.get("maxHp", 1))
            pct = min(1.0, max(0.0, float(hp) / float(max_hp)))

            mid_y = y_offset + item_h // 2

            is_pinned = m_id == pinned_id
            is_focused = m_id == focus_id
            if is_pinned or is_focused:
                c.create_rectangle(0, y_offset, cw, y_offset + item_h + pad_y,
                                   fill="#593014" if is_pinned else "#382318", outline="")
            if is_pinned or is_focused:
                c.create_polygon(3, mid_y - 4, 3, mid_y + 4, 10, mid_y,
                                 fill="#ffe36e" if is_pinned else "#59d8ff",
                                 outline="#1b1008")

            # Tên quái gọn gàng bên trái (cách lề 3px)
            c.create_text(
                13 if is_pinned or is_focused else 3, mid_y,
                text=f"{m_name[:10]} #{m_id}",
                fill="#70ff91" if is_pinned else ("#9ee9ff" if is_focused else COLOR_TEXT_GOLD),
                font=(FONT_FAMILY, 7),
                anchor="w"
            )

            # Thanh máu mini bên phải trên cùng 1 dòng (cách lề 3px)
            bx1 = cw - bar_w - 3
            bx2 = cw - 3
            by = mid_y - bh // 2

            c.create_rectangle(bx1, by, bx2, by + bh, fill="#1c0b05", outline="#5a3118")

            fill_w = int((bar_w - 2) * pct)
            if fill_w > 0:
                c.create_rectangle(bx1 + 1, by + 1, bx1 + 1 + fill_w, by + bh - 1, fill=COLOR_HP_RED, outline="")
                c.create_rectangle(bx1 + 1, by + 1, bx1 + 1 + fill_w, by + 2, fill="#ff7a7a", outline="")

            hp_str = f"{hp}/{max_hp}"
            mid_x = bx1 + bar_w // 2
            c.create_text(mid_x + 1, mid_y + 1, text=hp_str, fill="#000000", font=(FONT_FAMILY, 6), anchor="center")
            c.create_text(mid_x, mid_y, text=hp_str, fill="#ffffff", font=(FONT_FAMILY, 6), anchor="center")

            self.monster_rows.append((y_offset, y_offset + item_h + pad_y, m_id))
            y_offset += item_h + pad_y

        c.configure(scrollregion=(0, 0, cw, y_offset + 4))

    # ==============================================================
    # 5. CENTER CONTENT (RADAR & LOGS)
    # ==============================================================
    def build_center_content(self):
        center_container = tk.Frame(self.mid_area, bg=COLOR_BG_DARK)
        center_container.pack(side="left", fill="both", expand=True)

        # LOGS ở dưới radar, căn chuẩn đáy
        p_log = HsoPanel(center_container, title="LOGS")
        p_log.pack(side="bottom", fill="x", pady=(3, 0))
        c_log = p_log.body

        self.txt_log = tk.Text(
            c_log,
            height=4,
            bg=COLOR_INNER_BG,
            fg=COLOR_TEXT_WHITE,
            bd=0,
            highlightthickness=0,
            font=(FONT_FAMILY, 7),
            wrap="word"
        )
        self.txt_log.pack(fill="both", expand=True)

        # RADAR ở trên, nhỏ gọn hơn
        p_radar = HsoPanel(center_container, title="RADAR")
        p_radar.pack(side="top", fill="both", expand=True)
        c_radar = p_radar.body

        self.radar_canvas = tk.Canvas(c_radar, bg="#0d0704", highlightthickness=0)
        self.radar_canvas.pack(fill="both", expand=True)
        self.radar_canvas.bind("<Configure>", lambda e: self.redraw_radar())

        self.log_msg("Ready.")

    def log_msg(self, msg):
        t = time.strftime("[%H:%M:%S] ")
        full = t + msg
        print(full)
        if hasattr(self, "txt_log") and self.txt_log:
            try:
                self.txt_log.insert(tk.END, full + "\n")
                self.txt_log.see(tk.END)
            except Exception:
                pass

    # ==============================================================
    # MODAL CHỌN KHU VỰC
    # ==============================================================
    def open_zone_modal(self):
        if not self.state_data.get("areas"):
            try:
                self.write_command({"action": "request_areas"})
            except OSError as e:
                self.log_msg("Không lấy được danh sách khu: " + str(e))

        dlg = tk.Toplevel(self.root)
        dlg.title("Select Zone")
        dlg.geometry("220x290")
        dlg.resizable(False, False)
        dlg.configure(bg=COLOR_BG_DARK)
        dlg.transient(self.root)

        # Căn giữa theo cửa sổ chính
        x = self.root.winfo_rootx() + max(10, (self.root.winfo_width() - 220) // 2)
        y = self.root.winfo_rooty() + max(10, (self.root.winfo_height() - 290) // 2)
        dlg.geometry(f"+{x}+{y}")

        panel = HsoPanel(dlg, title="SELECT ZONE")
        panel.pack(fill="both", expand=True, padx=3, pady=3)
        body = panel.body

        map_name = self.state_data.get("map_name", "Current Map")
        cur_area = self.state_data.get("area", 0) + 1
        max_area = self.state_data.get("max_area", 6)

        info_lbl = tk.Label(
            body,
            text=f"{map_name} (Zone {cur_area}/{max_area})",
            font=(FONT_FAMILY, 7),
            fg=COLOR_TEXT_GOLD,
            bg="#1e1109"
        )
        info_lbl.pack(pady=(0, 3))

        # Nút đáy modal (pack side=bottom trước để luôn hiển thị ở đáy)
        b_frame = tk.Frame(body, bg="#1e1109")
        b_frame.pack(side="bottom", fill="x", pady=(3, 0))

        btn_low = HsoButton(
            b_frame,
            text="Lowest",
            font_size=7,
            width=54,
            height=20,
            gold_text=True,
            renderer=self.btn_renderer,
            command=lambda: (self.send_auto_min_area(), dlg.destroy())
        )
        btn_low.pack(side="left")

        btn_close = HsoButton(
            b_frame,
            text="Close",
            font_size=7,
            width=50,
            height=20,
            renderer=self.btn_renderer,
            command=dlg.destroy
        )
        btn_close.pack(side="right")

        list_container = tk.Frame(body, bg=COLOR_INNER_BG)
        list_container.pack(fill="both", expand=True, pady=(0, 3))

        canvas = tk.Canvas(list_container, bg=COLOR_INNER_BG, highlightthickness=0)
        scrollbar = ttk.Scrollbar(list_container, orient="vertical", command=canvas.yview)
        canvas.configure(yscrollcommand=scrollbar.set)
        canvas.pack(side="left", fill="both", expand=True)
        scrollbar.pack(side="right", fill="y")

        inner_list = tk.Frame(canvas, bg=COLOR_INNER_BG)
        win_id = canvas.create_window((0, 0), window=inner_list, anchor="nw")

        def on_inner_cfg(event):
            canvas.configure(scrollregion=canvas.bbox("all"))
            canvas.itemconfig(win_id, width=canvas.winfo_width())

        canvas.bind("<Configure>", lambda e: canvas.itemconfig(win_id, width=e.width))
        inner_list.bind("<Configure>", on_inner_cfg)

        canvas.bind("<MouseWheel>", lambda e: canvas.yview_scroll(int(-1 * (e.delta / 120)), "units"))
        canvas.bind("<Button-4>", lambda e: canvas.yview_scroll(-1, "units"))
        canvas.bind("<Button-5>", lambda e: canvas.yview_scroll(1, "units"))

        areas_data = self.state_data.get("areas", [])
        if not areas_data and max_area > 0:
            areas_data = [{"area": i + 1, "status": -1} for i in range(max_area)]

        def pick_zone(z_idx, is_current=False, is_full=False):
            if is_current:
                self.log_msg(f"Already in Zone {z_idx + 1}!")
                dlg.destroy()
                return
            if is_full:
                self.log_msg(f"Zone {z_idx + 1} is FULL (Đầy)!")
                return
            self.send_change_area(z_idx)
            dlg.destroy()

        for a_info in areas_data:
            z_num = a_info.get("area", 1)
            st = a_info.get("status", -1)
            is_cur = (z_num == cur_area)
            is_full = (st >= 2)

            if st == 0:
                st_text = "Empty"
                st_color = COLOR_ONLINE
            elif st == 1:
                st_text = "Normal"
                st_color = COLOR_TEXT_GOLD
            elif st >= 2:
                st_text = "Full"
                st_color = COLOR_HP_RED
            else:
                st_text = "--"
                st_color = COLOR_TEXT_MUTED

            row_btn = tk.Frame(inner_list, bg="#2c160c" if is_cur else "#1e1109", bd=1, relief="solid")
            row_btn.configure(highlightbackground=COLOR_BORDER_GOLD if is_cur else "#3a1d0f", highlightthickness=1)
            row_btn.pack(fill="x", padx=2, pady=1)

            z_lbl = tk.Label(
                row_btn,
                text=f"Zone {z_num}" + (" (Current)" if is_cur else ""),
                font=(FONT_FAMILY, 7),
                fg=COLOR_TEXT_GOLD if is_cur else COLOR_TEXT_WHITE,
                bg="#2c160c" if is_cur else "#1e1109"
            )
            z_lbl.pack(side="left", padx=4, pady=2)

            s_lbl = tk.Label(
                row_btn,
                text=st_text,
                font=(FONT_FAMILY, 7),
                fg=st_color,
                bg="#2c160c" if is_cur else "#1e1109"
            )
            s_lbl.pack(side="right", padx=4, pady=2)

            for w in (row_btn, z_lbl, s_lbl):
                w.bind("<Button-1>", lambda e, idx=z_num-1, cur=is_cur, full=is_full: pick_zone(idx, cur, full))
                w.bind("<Enter>", lambda e, b=row_btn: b.configure(bg="#3a1d0f"))
                w.bind("<Leave>", lambda e, b=row_btn, cur=is_cur: b.configure(bg="#2c160c" if cur else "#1e1109"))

    # ==============================================================
    # GỬI LỆNH ĐỔI KHU
    # ==============================================================
    def write_command(self, command):
        CMD_DIR.mkdir(exist_ok=True)
        sequence = getattr(self, "command_sequence", 0)
        self.command_sequence = sequence + 1
        name = f"{time.time_ns():020d}-{os.getpid()}-{sequence:06d}"
        temp_file = CMD_DIR / f"{name}.tmp"
        final_file = CMD_DIR / f"{name}.json"
        with open(temp_file, "w", encoding="utf-8") as f:
            json.dump(command, f)
        os.replace(temp_file, final_file)

    def send_change_area(self, target_area=0):
        try:
            target_area = max(0, int(target_area))
            self.write_command({"action": "change_area", "area": target_area})
            self.pending_zone_change = (target_area, self.state_data.get("area"), time.monotonic())
            self.log_msg(f"Đang yêu cầu đổi sang Zone {target_area + 1}...")
        except Exception as e:
            self.log_msg("Error: " + str(e))

    def send_auto_min_area(self):
        try:
            self.write_command({"action": "auto_min_area"})
            self.pending_zone_change = (None, self.state_data.get("area"), time.monotonic())
            self.log_msg("Đang yêu cầu đổi sang khu vắng nhất...")
        except Exception as e:
            self.log_msg("Error: " + str(e))

    # ==============================================================
    # VÀO GAME / ĐÓNG GAME / 60 FPS / REPACK
    # ==============================================================
    def toggle_fps(self):
        cur = self.config.get("fps_60", True)
        new_val = not cur
        self.config["fps_60"] = new_val
        save_config(self.config)
        self.btn_fps.set_text("60 FPS: ON" if new_val else "60 FPS: OFF")
        self.log_msg(f"60 FPS: {'ON' if new_val else 'OFF'}")

    def launch_game(self):
        if self.game_proc and self.game_proc.poll() is None:
            messagebox.showinfo("Notice", "Game is already running!")
            return

        for required_file in (JAR_TARGET, MICROEMU_JAR):
            if not required_file.is_file():
                messagebox.showerror("Error", f"Không tìm thấy file game: {required_file}")
                return

        w = int(self.config.get("width", 800))
        h = int(self.config.get("height", 480))

        self.update_microemulator_config(w, h)

        cmd = [
            JAVA_EXEC,
            "-jar",
            str(MICROEMU_JAR),
            "--resizableDevice",
            str(w),
            str(h),
            str(JAR_TARGET)
        ]

        try:
            # Chỉ nhận dữ liệu IPC do lần mở game này ghi ra.
            self.game_started_ns = time.time_ns()
            try:
                CMD_FILE.unlink()
            except FileNotFoundError:
                pass
            if CMD_DIR.exists():
                for old_command in CMD_DIR.glob("*.json"):
                    old_command.unlink()
            self.game_proc = subprocess.Popen(cmd, cwd=str(BASE_DIR))
            self.lbl_client_status.configure(text=f"ONLINE (PID {self.game_proc.pid})", fg=COLOR_ONLINE)
            if hasattr(self, "btn_game_toggle"):
                self.btn_game_toggle.set_text("Stop Game")
            self.log_msg(f"Đã mở {JAR_TARGET.name}: {w}x{h} (PID {self.game_proc.pid})")
        except Exception as e:
            self.game_started_ns = None
            messagebox.showerror("Error", f"Failed to start game:\n{e}")

    def toggle_game(self):
        if self.game_proc and self.game_proc.poll() is None:
            self.stop_game()
        else:
            self.launch_game()

    def stop_game(self):
        if self.game_proc and self.game_proc.poll() is None:
            try:
                self.game_proc.terminate()
            except Exception:
                pass
            self.game_proc = None
            self.game_started_ns = None
            self.lbl_client_status.configure(text="OFFLINE", fg=COLOR_OFFLINE)
            if hasattr(self, "btn_game_toggle"):
                self.btn_game_toggle.set_text("Play Game")
            self.clear_live_state()
            self.log_msg("Game closed.")
        else:
            if hasattr(self, "btn_game_toggle"):
                self.btn_game_toggle.set_text("Play Game")

    def repack_jar(self):
        try:
            bin_dir = BASE_DIR / "bin"
            bin_dir.mkdir(exist_ok=True)
            src_files = list((BASE_DIR / "src_java").rglob("*.java"))
            if not src_files:
                messagebox.showerror("Error", "No Java sources found!")
                return
            sources_arg_file = bin_dir / "sources.txt"
            with open(sources_arg_file, "w", encoding="utf-8") as f:
                for sf in src_files:
                    f.write(f'"{sf}"\n')

            cp = f"{MICROEMU_JAR}:{BASE_DIR / 'HSO301' / 'lib' / 'cldc_1.1.jar'}:{BASE_DIR / 'HSO301' / 'lib' / 'midp_2.1.jar'}"
            cmd = ["javac", "-cp", cp, "-source", "1.8", "-target", "1.8", "-d", str(bin_dir), f"@{sources_arg_file}"]
            subprocess.run(cmd, cwd=str(BASE_DIR), check=True)
            subprocess.run(["jar", "uf", str(JAR_TARGET), "-C", str(bin_dir), "."], cwd=str(BASE_DIR), check=True)
            self.log_msg(f"Compiled {len(src_files)} Java files & updated JAR!")
            messagebox.showinfo("Success", f"Successfully compiled {len(src_files)} Java sources and repacked JAR!")
        except Exception as e:
            messagebox.showerror("Error", f"Repack error:\n{e}")

    def update_microemulator_config(self, w, h):
        cfg_dir = Path.home() / ".microemulator"
        cfg_dir.mkdir(parents=True, exist_ok=True)
        xml = f"""<config>
	<files>
		<recent maxCapacity="10">
			<midlet>
				<name>KnightOnline Midlet Suite</name>
				<url>file://{BASE_DIR / 'KnightOnline_v403.jar'}</url>
			</midlet>
		</recent>
	</files>
	<devices>
		<device default="true">
			<name>Resizable device</name>
			<descriptor>org/microemu/device/resizable/device.xml</descriptor>
			<rectangle>
				<x>0</x>
				<y>0</y>
				<width>{w}</width>
				<height>{h}</height>
			</rectangle>
		</device>
	</devices>
	<windows>
		<main onstart="true">
			<x>100</x>
			<y>100</y>
			<width>{w + 16}</width>
			<height>{h + 58}</height>
		</main>
	</windows>
</config>"""
        try:
            with open(cfg_dir / "config2.xml", "w", encoding="utf-8") as f:
                f.write(xml)
        except Exception:
            pass

    # ==============================================================
    # VÒNG LẶP ĐỒNG BỘ DỮ LIỆU THỜI GIAN THỰC (400ms)
    # ==============================================================
    def clear_live_state(self):
        dialog = getattr(self, "auto_farm_dialog", None)
        if dialog is not None and dialog.winfo_exists():
            dialog.destroy()
        self.auto_farm_dialog = None
        self.auto_farm_refresh = None
        self.state_data = {}
        self.pending_zone_change = None
        self.last_farm_signature = None
        self.last_farm_mode = "off"
        self.last_farm_damage_results = 0
        self.config["auto_attack"] = False
        self.btn_auto_attack.set_selected(False)
        self.btn_auto_attack.configure(gold_text=False)
        self.btn_auto_attack.set_text("Auto Attack: OFF")
        self.lbl_in_game_status.configure(text="Waiting for game", fg=COLOR_TEXT_MUTED)
        self.lbl_char_name.configure(text="Offline")
        self.lbl_char_lv.configure(text="")
        self.lbl_map_name.configure(text="--")
        self.lbl_zone_val.configure(text="Zone: --")
        self.btn_zone_select.set_text("--")
        self.btn_auto_farm.set_text("Auto Fam: Cấu hình")
        self.btn_auto_farm.set_selected(False)
        self.btn_unpin.set_disabled(True)
        self.p_char.set_title("Offline")
        self.p_mon.set_tab_title(1, "PLAYER (0)")
        self.redraw_player_bars()
        self.refresh_mon_panel()
        self.redraw_radar()

    def poll_ipc_loop(self):
        if self.game_proc:
            if self.game_proc.poll() is not None:
                exit_code = self.game_proc.returncode
                self.game_proc = None
                self.game_started_ns = None
                self.lbl_client_status.configure(text="OFFLINE", fg=COLOR_OFFLINE)
                if hasattr(self, "btn_game_toggle"):
                    self.btn_game_toggle.set_text("Play Game")
                self.clear_live_state()
                self.log_msg(f"Game closed (exit code {exit_code}).")

        if self.game_proc:
            try:
                if STATE_FILE.stat().st_mtime_ns >= self.game_started_ns:
                    with open(STATE_FILE, "r", encoding="utf-8") as f:
                        data = json.load(f)
                    self.update_live_data(data)
            except (OSError, ValueError):
                pass

        self.root.after(400, self.poll_ipc_loop)

    def update_live_data(self, data):
        previous_pin = self.state_data.get("pinned_monster_id", -1)
        self.state_data = data
        in_game = data.get("in_game", False)

        current_pin = data.get("pinned_monster_id", -1) if in_game else -1
        self.btn_unpin.set_disabled(current_pin < 0)
        if current_pin != previous_pin:
            if current_pin >= 0:
                self.log_msg(f"Đã ghim quái ID {current_pin}.")
            elif previous_pin >= 0:
                self.log_msg("Đã bỏ ghim mục tiêu.")

        if self.pending_zone_change:
            target, old_area, started = self.pending_zone_change
            area = data.get("area") if in_game else None
            if area is not None and (area == target if target is not None else area != old_area):
                self.log_msg(f"Đã đổi sang Zone {area + 1}.")
                self.pending_zone_change = None
            elif time.monotonic() - started > 8:
                self.log_msg("Chưa nhận được xác nhận đổi khu từ game.")
                self.pending_zone_change = None

        if in_game:
            self.sync_farm_targets(data)
            farm_mode = data.get("farm_mode", "off")
            if farm_mode == "normal" and self.last_farm_mode != "normal":
                self.log_msg("Auto Fam: chỉ gửi đánh thường (skill 0) tới các quái đã chọn.")
            self.last_farm_mode = farm_mode
            farm_damage_results = data.get("farm_damage_results", 0)
            if farm_damage_results > 0 and self.last_farm_damage_results == 0:
                self.log_msg(f"Auto Fam: đã nhận sát thương từ server "
                             f"({farm_damage_results} lượt, {data.get('farm_distinct_hits', 0)} quái).")
            self.last_farm_damage_results = farm_damage_results
            if self.auto_farm_refresh is not None:
                self.auto_farm_refresh(data)
            self.lbl_in_game_status.configure(text="IN GAME", fg=COLOR_ONLINE)

            p = data.get("player", {})
            p_name = p.get('name', '--')
            p_lv = p.get('lv', '--')
            self.lbl_char_name.configure(text=p_name)
            self.lbl_char_lv.configure(text=f"(cấp {p_lv})" if p_lv != '--' else "")
            self.lbl_hp_val.configure(text=f"{p.get('hp', 0)}/{p.get('maxHp', 0)}")
            self.lbl_mp_val.configure(text=f"{p.get('mp', 0)}/{p.get('maxMp', 0)}")

            if hasattr(self, "p_char"):
                p_title = f"{p_name} ({p_lv})" if (p_lv != '--' and p_lv != '') else p_name
                self.p_char.set_title(p_title)
            if hasattr(self, "redraw_player_bars"):
                self.redraw_player_bars()

            map_name = data.get("map_name", "Unknown")
            map_id = data.get("map_id", 0)
            self.lbl_map_name.configure(text=f"{map_name} (#{map_id})")

            area = data.get("area", 0) + 1
            max_area = data.get("max_area", 10)
            self.lbl_zone_val.configure(text=f"Zone: {area}/{max_area}")
            if hasattr(self, "btn_zone_select"):
                self.btn_zone_select.set_text(str(area))

            if "auto_attack" in data and hasattr(self, "btn_auto_attack"):
                is_atk = bool(data["auto_attack"])
                if is_atk != self.config.get("auto_attack", False):
                    self.config["auto_attack"] = is_atk
                    self.btn_auto_attack.set_selected(is_atk)
                    self.btn_auto_attack.configure(gold_text=is_atk)
                    self.btn_auto_attack.set_text(f"Auto Attack: {'ON' if is_atk else 'OFF'}")

            if "speed" in data and hasattr(self, "update_speed_ui"):
                try:
                    live_spd = f"{float(data['speed']):.1f}"
                    if live_spd in SPEED_LEVELS and live_spd != str(self.config.get("speed", "1.0")):
                        self.config["speed"] = live_spd
                        self.update_speed_ui()
                except Exception:
                    pass

            players = data.get("players", [])
            p_count = data.get("players_count", len(players))
            if hasattr(self, "p_mon"):
                self.p_mon.set_tab_title(1, f"PLAYER ({p_count})")

            self.refresh_mon_panel()
            self.redraw_radar()
        else:
            self.lbl_in_game_status.configure(text="Login Screen", fg=COLOR_TEXT_MUTED)
            self.lbl_char_name.configure(text="Offline")
            self.lbl_char_lv.configure(text="")
            if hasattr(self, "p_char"):
                self.p_char.set_title("Offline")
            if hasattr(self, "redraw_player_bars"):
                self.redraw_player_bars()

    def redraw_radar(self):
        c = self.radar_canvas
        c.delete("all")
        w = c.winfo_width()
        h = c.winfo_height()
        if w < 35 or h < 40:
            return

        cx, cy = w // 2, h // 2
        if not self.state_data.get("in_game", False):
            c.create_rectangle(2, 2, w - 2, h - 2, outline=COLOR_BORDER_DARK)
            c.create_text(cx, cy, text="RADAR READY\nWaiting for map...", fill=COLOR_TEXT_MUTED, font=(FONT_FAMILY, 7), justify="center", anchor="center")
            return

        data = self.state_data
        player = data.get("player", {})
        monsters = data.get("map_monsters") or data.get("monsters", [])
        points = ([player] + monsters + data.get("players", []) +
                  data.get("npcs", []) + data.get("waypoints", []))
        map_width = data.get("map_width") or max((p.get("x", 0) for p in points), default=0) + 24
        map_height = data.get("map_height") or max((p.get("y", 0) for p in points), default=0) + 24
        ox, oy, scale = radar_projection(w, h, map_width, map_height)
        right, bottom = ox + map_width * scale, oy + map_height * scale

        def position(obj):
            x = max(0, min(map_width, obj.get("x", 0)))
            y = max(0, min(map_height, obj.get("y", 0)))
            return ox + x * scale, oy + y * scale

        c.create_rectangle(ox, oy, right, bottom, outline=COLOR_BORDER_GOLD)
        c.create_line((ox + right) / 2, oy, (ox + right) / 2, bottom,
                      fill=COLOR_BORDER_DARK, dash=(2, 4))
        c.create_line(ox, (oy + bottom) / 2, right, (oy + bottom) / 2,
                      fill=COLOR_BORDER_DARK, dash=(2, 4))

        profile = self.config.get("auto_farm", {}).get(
            farm_profile_key(data.get("map_id", 0), data.get("area", 0)))
        pinned_id = data.get("pinned_monster_id", -1)
        focus_id = data.get("focus_monster_id", -1)
        for monster in monsters:
            x, y = position(monster)
            selected = farm_profile_matches(profile, monster)
            focused = monster.get("id") in (pinned_id, focus_id)
            color = (COLOR_BORDER_LIGHT if focused else
                     COLOR_TEXT_GOLD if selected else
                     COLOR_HP_RED if monster.get("hp", 0) > 0 else COLOR_TEXT_MUTED)
            radius = 3 if focused else 2
            c.create_oval(x - radius, y - radius, x + radius, y + radius,
                          fill=color, outline="")

        for waypoint in data.get("waypoints", []):
            x, y = position(waypoint)
            c.create_polygon(x, y - 5, x + 4, y, x, y + 5, x - 4, y,
                             fill="#b58cff", outline="#ffffff")

        for npc in data.get("npcs", []):
            kind = classify_radar_npc(npc.get("name"))
            x, y = position(npc)
            if kind == "zone":
                c.create_polygon(x, y - 6, x + 5, y + 4, x - 5, y + 4,
                                 fill="#ff9b45", outline="#ffffff")
            elif kind == "stone":
                c.create_polygon(x, y - 6, x + 5, y, x, y + 6, x - 5, y,
                                 fill="#60dfff", outline="#ffffff")

        for other in data.get("players", []):
            x, y = position(other)
            c.create_oval(x - 3, y - 3, x + 3, y + 3,
                          fill="#ffd700", outline="#ffffff")

        px, py = position(player)
        c.create_oval(px - 4, py - 4, px + 4, py + 4,
                      fill=COLOR_ONLINE, outline="#ffffff", width=1)
        legend_y = min(h - 23, bottom + 3)
        for x, y, label, color in (
                (8, legend_y, "● Quái", COLOR_HP_RED),
                (87, legend_y, "◆ Lối đi", "#b58cff"),
                (170, legend_y, "▲ Khu", "#ff9b45"),
                (8, legend_y + 11, "◆ Đá dịch chuyển", "#60dfff"),
                (170, legend_y + 11, "● Bạn", COLOR_ONLINE)):
            c.create_text(x, y, text=label, fill=color,
                          font=(FONT_FAMILY, 7), anchor="nw")


def main():
    root = tk.Tk()
    app = HsoStudioApp(root)
    root.mainloop()


if __name__ == "__main__":
    main()
