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
import math
from pathlib import Path

# Kích hoạt DPI-Aware trên Windows để giao diện sắc nét 100%, không bị mờ nhòe do Windows DWM scale
if sys.platform == "win32":
    try:
        import ctypes
        ctypes.windll.shcore.SetProcessDpiAwareness(1)
    except Exception:
        try:
            ctypes.windll.user32.SetProcessDPIAware()
        except Exception:
            pass

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

def resolve_jdk_tool(tool_name):
    java_home = os.environ.get("JAVA_HOME")
    if java_home:
        ext = ".exe" if sys.platform == "win32" else ""
        cand = Path(java_home) / "bin" / f"{tool_name}{ext}"
        if cand.exists():
            return str(cand)
    import shutil
    w = shutil.which(tool_name)
    if w:
        return w
    return tool_name


def resolve_java_exec():
    linux_java = "/usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java"
    if sys.platform != "win32" and os.path.exists(linux_java):
        return linux_java
    return resolve_jdk_tool("java")


JAVA_EXEC = resolve_java_exec()
JAVAC_EXEC = resolve_jdk_tool("javac")
JAR_EXEC = resolve_jdk_tool("jar")


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


def sort_players_for_panel(players, player, focus_id, pinned_id):
    px, py = player.get("x", 0), player.get("y", 0)

    def key(p):
        p_id = p.get("id")
        priority = (0 if p_id == pinned_id else
                    1 if p_id == focus_id else
                    2 if p.get("hp", 0) > 0 else 3)
        distance = (p.get("x", 0) - px) ** 2 + (p.get("y", 0) - py) ** 2
        return priority, distance, p_id

    return sorted(players, key=key)


def farm_profile_key(map_id, area):
    return f"{int(map_id)}:{int(area)}"


def get_map_farm_profile(config, map_id, area=None):
    auto_farm = config.get("auto_farm", {})
    if not auto_farm:
        return None
    try:
        m_id = int(map_id)
    except (ValueError, TypeError):
        return None
    if area is not None:
        try:
            p = auto_farm.get(farm_profile_key(m_id, int(area)))
            if p is not None:
                return p
        except (ValueError, TypeError):
            pass
    return auto_farm.get(f"{m_id}:*") or auto_farm.get(str(m_id))


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


def compact_mob_name(name):
    """Rút gọn tên quái siêu ngắn để hiển thị vừa vặn trong grid 2 cột."""
    name = (name or "").strip()
    if not name or name == "Chưa rõ loại":
        return "Mob"
    if name.lower().startswith("quái "):
        name = name[5:].strip()
    parts = name.split()
    if not parts:
        return "Mob"
    if len(parts) >= 2 and parts[0] in ("Bọ", "Ốc", "Cây", "Ma", "Thằn", "Đầu", "Mặt"):
        return (parts[0] + " " + parts[1])[:8]
    return parts[0][:6]


def format_mini_hp(hp):
    """Định dạng số HP siêu gọn (ví dụ 1.2k, 15k, 1.5M) vừa vặn thanh máu mini."""
    hp = max(0, int(hp))
    if hp >= 1_000_000:
        return f"{hp / 1_000_000:.1f}M"
    if hp >= 10_000:
        return f"{hp // 1_000}k"
    if hp >= 1_000:
        k = hp / 1000.0
        return f"{k:.1f}k" if (hp % 1000 >= 100) else f"{int(k)}k"
    return str(hp)


def radar_projection(width, height, map_width, map_height):
    """Return the whole-map rectangle and a shared pixel-to-canvas scale with compact padding."""
    map_width = max(1, map_width)
    map_height = max(1, map_height)
    scale = min((width - 32) / map_width, (height - 34) / map_height)
    draw_width, draw_height = map_width * scale, map_height * scale
    return (width - draw_width) / 2, 6 + (height - 24 - draw_height) / 2, scale


def classify_radar_npc(name):
    name = (name or "").strip().lower()
    if name == "khu":
        return "zone"
    if "dịch chuyển" in name or "dich chuyen" in name or "teleport" in name:
        return "stone"
    return "npc"


def farm_profile_from_selection(monsters, selected_ids):
    if not selected_ids:
        return None
    groups = {}
    for monster in monsters:
        groups.setdefault(monster.get("name", "Mob"), set()).add(monster["id"])
    all_ids = {monster["id"] for monster in monsters}
    selected_ids = set(selected_ids) & all_ids
    if not selected_ids:
        return None
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
    "auto_pickup": False,
    "auto_sell": False,
    "farm_sync": True,
    "monster_view_mode": "grid",
    "auto_loot_filter": {
        "white": True,
        "blue": True,
        "yellow": True,
        "purple": True,
        "orange": True,
        "green": True,
        "hp": True,
        "mp": True,
        "gold": True,
        "material": True,
        "quest": True,
    },
    "auto_sell_filter": {
        "white": True,
        "blue": True,
        "yellow": False,
        "purple": False,
        "orange": False,
        "green": False,
        "material": False,
        "tier0_only": True,
        "unlocked_only": True,
    }
}


def load_config():
    if CONFIG_FILE.exists():
        try:
            with open(CONFIG_FILE, "r", encoding="utf-8") as f:
                loaded = json.load(f)
                merged = {**DEFAULT_CONFIG, **loaded}
                if "auto_loot_filter" in loaded and isinstance(loaded["auto_loot_filter"], dict):
                    merged["auto_loot_filter"] = {**DEFAULT_CONFIG["auto_loot_filter"], **loaded["auto_loot_filter"]}
                if "auto_sell_filter" in loaded and isinstance(loaded["auto_sell_filter"], dict):
                    merged["auto_sell_filter"] = {**DEFAULT_CONFIG["auto_sell_filter"], **loaded["auto_sell_filter"]}
                return merged
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
        elif is_pressed:
            top_c = (38, 14, 6)
            bot_c = (58, 22, 10)
            border_gold = (160, 110, 40)
            border_outer = (25, 8, 4)
            highlight_c = (48, 18, 8)
        elif is_selected:
            # Active/Selected state rực rỡ, sáng rõ hơn với viền vàng kim loại lấp lánh
            if is_hover:
                top_c = (195, 90, 32)
                bot_c = (135, 56, 20)
                border_gold = (255, 235, 110)
                border_outer = (70, 30, 12)
                highlight_c = (255, 160, 70)
            else:
                top_c = (175, 78, 25)
                bot_c = (120, 48, 16)
                border_gold = (255, 215, 80)
                border_outer = (60, 25, 10)
                highlight_c = (235, 135, 55)
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

        if self.is_disabled:
            fg = "#6a4e3c"
        elif self.is_selected:
            fg = "#ffffff"
        elif self.gold_text:
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
        n_tabs = len(self.tabs)
        tab_w = w / n_tabs
        idx = min(n_tabs - 1, max(0, int(event.x / tab_w)))
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
            n_tabs = len(self.tabs)
            tab_w = w / n_tabs
            for i, tab_title in enumerate(self.tabs):
                x1 = int(i * tab_w)
                x2 = int((i + 1) * tab_w) if i < n_tabs - 1 else w
                is_act = (i == self.active_tab_idx)
                bg = "#381c10" if is_act else "#1c0b05"
                fg = "#ffdf79" if is_act else "#96806e"
                c.create_rectangle(x1, 0, x2, 18, fill=bg, outline="")
                c.create_text((x1 + x2) // 2, 9, text=tab_title, fill=fg,
                              font=(FONT_FAMILY, 7 if n_tabs > 2 else 8), anchor="center")
                if i > 0:
                    c.create_line(x1, 0, x1, 18, fill=COLOR_BORDER_GOLD)
        else:
            c.create_rectangle(0, 0, w, 18, fill="#2c160c", outline="")
            ty = 9
            c.create_text(w // 2, ty, text=self.title, fill="#ffdf79", font=(FONT_FAMILY, 8), anchor="center")


class HsoMarquee(tk.Frame):
    """Băng thông báo chữ chạy ngang phong cách game Hiệp Sĩ Online"""
    def __init__(self, parent, height=20, speed=2, **kwargs):
        super().__init__(parent, bg=COLOR_BORDER_GOLD, bd=1, relief="solid")
        self.height = height
        self.speed = speed
        self._text = ""
        self._fg = COLOR_TEXT_GOLD
        self._is_alert = False
        self._x = 0
        self._text_width = 0
        self._timer = None
        self._flash_tick = 0

        self.canvas = tk.Canvas(self, height=self.height - 2, bg="#160b06", highlightthickness=0)
        self.canvas.pack(fill="both", expand=True)
        self._item = self.canvas.create_text(
            0, (self.height - 2) // 2,
            text="",
            fill=self._fg,
            font=(FONT_FAMILY, 8, "bold"),
            anchor="w"
        )
        self.canvas.bind("<Configure>", self._on_resize)
        self._start()

    def _on_resize(self, event):
        if self._x == 0 or self._x > event.width:
            self._x = event.width

    def set_message(self, text, fg=None, is_alert=False):
        fg = fg or COLOR_TEXT_GOLD
        if text != self._text or fg != self._fg or is_alert != self._is_alert:
            old_empty = not bool(self._text)
            self._text = text
            self._fg = fg
            self._is_alert = is_alert
            self.canvas.itemconfig(self._item, text=self._text, fill=self._fg)
            bbox = self.canvas.bbox(self._item)
            if bbox:
                self._text_width = bbox[2] - bbox[0]
            else:
                self._text_width = len(text) * 7
            w = self.canvas.winfo_width()
            if w > 10 and (old_empty or self._x > w):
                self._x = w

    def _start(self):
        self._tick()

    def _tick(self):
        try:
            if not self.winfo_exists():
                return
            w = self.canvas.winfo_width()
            if w > 10 and self._text:
                self._x -= self.speed
                if self._x + self._text_width < 0:
                    self._x = w + 10
                y = (self.height - 2) // 2
                self.canvas.coords(self._item, self._x, y)

                if self._is_alert:
                    self._flash_tick = (self._flash_tick + 1) % 16
                    if self._flash_tick < 8:
                        self.canvas.itemconfig(self._item, fill="#ff3b30")
                        self.configure(bg="#ff3b30")
                    else:
                        self.canvas.itemconfig(self._item, fill="#ffd60a")
                        self.configure(bg=COLOR_BORDER_GOLD)
                else:
                    self.canvas.itemconfig(self._item, fill=self._fg)
                    self.configure(bg=COLOR_BORDER_GOLD)
            self._timer = self.after(50, self._tick)
        except Exception:
            pass

    def destroy(self):
        if self._timer:
            try:
                self.after_cancel(self._timer)
            except Exception:
                pass
            self._timer = None
        super().destroy()


class HsoConfirmModal:
    """Modal xác nhận hành động nguy hiểm/nhạy cảm (Bảo vệ tài khoản tránh bị tố cáo/ban nick)"""
    def __init__(self, parent, title, message, on_confirm, on_cancel=None, renderer=None):
        self.dlg = tk.Toplevel(parent)
        self.dlg.title(title)
        self.dlg.configure(bg=COLOR_BG_DARK)
        self.dlg.resizable(False, False)
        self.dlg.transient(parent)
        self.dlg.grab_set()

        w, h = 380, 210
        px = parent.winfo_rootx() + max(0, (parent.winfo_width() - w) // 2)
        py = parent.winfo_rooty() + max(0, (parent.winfo_height() - h) // 2)
        self.dlg.geometry(f"{w}x{h}+{px}+{py}")

        panel = HsoPanel(self.dlg, title=title)
        panel.pack(fill="both", expand=True, padx=4, pady=4)
        body = panel.body

        msg_frame = tk.Frame(body, bg="#1e1109")
        msg_frame.pack(fill="both", expand=True, padx=8, pady=(8, 4))

        lbl = tk.Label(
            msg_frame,
            text=message,
            font=(FONT_FAMILY, 8),
            fg="#ffdf79",
            bg="#1e1109",
            justify="left",
            wraplength=340
        )
        lbl.pack(fill="both", expand=True)

        btn_row = tk.Frame(body, bg="#1e1109")
        btn_row.pack(side="bottom", fill="x", pady=(4, 4), padx=6)

        def _do_confirm():
            self.dlg.destroy()
            if on_confirm:
                on_confirm()

        def _do_cancel():
            self.dlg.destroy()
            if on_cancel:
                on_cancel()

        # Nút Hủy bỏ (Mặc định - An toàn)
        HsoButton(
            btn_row,
            text="HỦY BỎ (AN TOÀN)",
            font_size=8,
            width=140,
            height=24,
            renderer=renderer,
            command=_do_cancel
        ).pack(side="left")

        # Nút Xác nhận (Mạo hiểm)
        HsoButton(
            btn_row,
            text="XÁC NHẬN BẬT",
            font_size=8,
            width=110,
            height=24,
            gold_text=True,
            renderer=renderer,
            command=_do_confirm
        ).pack(side="right")

        self.dlg.protocol("WM_DELETE_WINDOW", _do_cancel)


class HsoStudioApp:
    def __init__(self, root):
        self.root = root
        self.root.title("Knight Online - HSO Tool")

        # Kích thước chuẩn nét cao (High-DPI)
        self.root.geometry("680x470")
        self.root.minsize(560, 410)
        self.root.configure(bg=COLOR_BG_DARK)

        self.config = load_config()
        self.config["auto_attack"] = False
        self.game_proc = None
        self.game_started_ns = None
        self.state_data = {}
        self.cached_player = {}
        self.pk_cooldown_end = 0.0
        self.pk_timer_id = None
        self.last_farm_signature = None
        self.last_farm_mode = "off"
        self.last_farm_damage_results = 0
        self.pending_zone_change = None
        self.last_map_id = None
        self.farm_paused_by_safety = False

        self.btn_renderer = HsoButtonRenderer()
        self.logo_photo = None
        self.load_assets()

        # Xây dựng các khu vực
        self.build_top_bar()
        self.build_marquee_top()
        self.build_bottom_bar()
        self.build_marquee_bottom()
        self.mid_area = tk.Frame(self.root, bg=COLOR_BG_DARK)
        self.mid_area.pack(fill="both", expand=True, padx=3, pady=3)
        self.build_left_sidebar()
        self.build_right_sidebar()
        self.build_center_content()
        self.clear_live_state()

        # Vòng lặp cập nhật IPC và đếm ngược hồi sinh mượt mà 0.1s
        self.poll_ipc_loop()
        self.countdown_tick_loop()

        # Dọn dẹp tiến trình khi đóng app
        self.root.protocol("WM_DELETE_WINDOW", self.on_close)

    def on_close(self):
        self.stop_game()
        self.root.destroy()

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
            font=(FONT_FAMILY, 9, "bold"),
            fg=COLOR_TEXT_GOLD,
            bg=COLOR_PANEL_BG
        ).pack(anchor="w")

        tk.Label(
            brand_box,
            text="HSO Assistant & Automation",
            font=(FONT_FAMILY, 7),
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
            font=(FONT_FAMILY, 8, "bold"),
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
        self.lbl_in_game_status.pack(anchor="e", pady=(2, 0))

    def build_marquee_top(self):
        self.marquee_top = HsoMarquee(self.root, height=20, speed=2)
        self.marquee_top.pack(fill="x", padx=3, pady=(2, 0))
        self.marquee_top.set_message("⚡ KNIGHT ONLINE - HSO TOOL • CHỜ KẾT NỐI VÀO GAME • SẴN SÀNG HỖ TRỢ ⚡", fg=COLOR_TEXT_MUTED)

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
            font_size=8,
            width=82,
            height=23,
            gold_text=True,
            renderer=self.btn_renderer,
            command=self.toggle_fps
        )
        self.btn_fps.pack(side="left")

        # Info
        tk.Label(
            b_content,
            text="Java 8 • J2ME",
            font=(FONT_FAMILY, 8),
            fg=COLOR_TEXT_MUTED,
            bg=COLOR_PANEL_BG
        ).pack(side="left", padx=(8, 0))

        # Nút Play / Stop Game gộp làm 1, đặt ở góc phải
        is_running = bool(self.game_proc and self.game_proc.poll() is None)
        self.btn_game_toggle = HsoButton(
            b_content,
            text="Stop Game" if is_running else "Play Game",
            font_size=8,
            width=92,
            height=23,
            gold_text=True,
            renderer=self.btn_renderer,
            command=self.toggle_game
        )
        self.btn_game_toggle.pack(side="right")

    def build_marquee_bottom(self):
        self.marquee_bottom = HsoMarquee(self.root, height=20, speed=2)
        self.marquee_bottom.pack(fill="x", side="bottom", padx=3, pady=(0, 2))
        self.marquee_bottom.set_message("⚡ HỆ THỐNG AN TOÀN ĐANG TRỰC CHIẾN • TỰ ĐỘNG PHÁT HIỆN BOT & NGƯỜI LẠ ⚡", fg=COLOR_TEXT_MUTED)

    # ==============================================================
    # 3. LEFT SIDEBAR (LOCATION & MONSTERS/PLAYERS)
    # ==============================================================
    def build_left_sidebar(self):
        left_container = tk.Frame(self.mid_area, bg=COLOR_BG_DARK, width=176)
        left_container.pack(side="left", fill="y", padx=(0, 3))
        left_container.pack_propagate(False)

        # LOCATION
        p_map = HsoPanel(left_container, title="LOCATION")
        p_map.pack(fill="x", pady=(0, 3))
        c_map = p_map.body

        r_m1 = tk.Frame(c_map, bg="#1e1109")
        r_m1.pack(fill="x", pady=(0, 3))
        tk.Label(r_m1, text="Map:", font=(FONT_FAMILY, 8), fg=COLOR_TEXT_MUTED, bg="#1e1109").pack(side="left")
        self.lbl_map_name = tk.Label(r_m1, text="Connecting...", font=(FONT_FAMILY, 8), fg=COLOR_TEXT_GOLD, bg="#1e1109")
        self.lbl_map_name.pack(side="left", padx=(3, 0))

        r_m2 = tk.Frame(c_map, bg="#1e1109")
        r_m2.pack(fill="x", pady=(0, 3))
        self.lbl_zone_val = tk.Label(r_m2, text="Zone: --", font=(FONT_FAMILY, 8), fg=COLOR_TEXT_WHITE, bg="#1e1109")
        self.lbl_zone_val.pack(side="left")

        cz_row = tk.Frame(c_map, bg="#1e1109")
        cz_row.pack(fill="x", pady=0)

        tk.Label(cz_row, text="Change:", font=(FONT_FAMILY, 8), fg=COLOR_TEXT_MUTED, bg="#1e1109").pack(side="left")

        # Nút số khu hiện tại: bấm vào mở modal chọn khu
        self.btn_zone_select = HsoButton(
            cz_row,
            text="1",
            font_size=8,
            width=38,
            height=23,
            gold_text=True,
            renderer=self.btn_renderer,
            command=self.open_zone_modal
        )
        self.btn_zone_select.pack(side="left", padx=(3, 0))

        btn_v_vang = HsoButton(
            cz_row,
            text="Lowest",
            font_size=8,
            width=58,
            height=23,
            gold_text=True,
            renderer=self.btn_renderer,
            command=self.send_auto_min_area
        )
        btn_v_vang.pack(side="left", padx=(3, 0))

        # MONSTERS / PLAYERS / NPC TRIPLE TAB PANEL
        self.active_mon_tab = 0
        self.p_mon = HsoPanel(
            left_container,
            tabs=["MON (0)", "PL (0)", "NPC (0)"],
            on_tab_change=self.on_mon_tab_change
        )
        self.p_mon.pack(fill="both", expand=True)
        c_mon = self.p_mon.body

        self.list_container = tk.Frame(c_mon, bg=COLOR_INNER_BG)
        self.list_container.pack(fill="both", expand=True)

        self.canvas_mon = tk.Canvas(self.list_container, bg=COLOR_INNER_BG, highlightthickness=0)
        self.mon_scrollbar = ttk.Scrollbar(self.list_container, orient="vertical", command=self.canvas_mon.yview)

        self.canvas_mon.configure(yscrollcommand=self.mon_scrollbar.set)
        self.canvas_mon.pack(side="left", fill="both", expand=True)
        self.mon_scrollbar.pack(side="right", fill="y")

        self.canvas_mon.bind("<Configure>", lambda e: self.refresh_mon_panel())
        self.canvas_mon.bind("<MouseWheel>", lambda e: self.canvas_mon.yview_scroll(int(-1 * (e.delta / 120)), "units"))
        self.canvas_mon.bind("<Button-4>", lambda e: self.canvas_mon.yview_scroll(-1, "units"))
        self.canvas_mon.bind("<Button-5>", lambda e: self.canvas_mon.yview_scroll(1, "units"))
        self.canvas_mon.bind("<Button-1>", self.pin_monster_from_panel)
        self.monster_rows = []
        self.monster_grid_cells = []

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
            data = self.state_data
            map_id = data.get("map_id", 0)
            area = data.get("area", 0)
            other_count = data.get("other_players_count", len(data.get("players", [])))

            # Nếu có người chơi khác (> 0 người) trong khu
            if other_count > 0:
                players = data.get("players", [])
                p_names = ", ".join([p.get("name", "Người lạ") for p in players[:3]])
                msg = (
                    f"CẢNH BÁO AN TOÀN!\n\n"
                    f"Khu vực hiện đang có {other_count} người chơi khác ({p_names}).\n"
                    f"Bật Auto khi có người trong khu vực có nguy cơ bị người khác quan sát / quay clip tố cáo khóa nick!\n\n"
                    f"Bạn có chắc chắn muốn mạo hiểm bật Auto Attack không?"
                )
                HsoConfirmModal(
                    self.root,
                    title="CẢNH BÁO: CÓ NGƯỜI TRONG KHU",
                    message=msg,
                    on_confirm=lambda: self._execute_toggle_auto_attack(True),
                    on_cancel=None,
                    renderer=self.btn_renderer
                )
                return

        self._execute_toggle_auto_attack(new_val)

    def _execute_toggle_auto_attack(self, enabled):
        if enabled:
            self.sync_farm_targets(force=True)
        self.config["auto_attack"] = enabled
        save_config(self.config)

        self.btn_auto_attack.set_selected(enabled)
        self.btn_auto_attack.configure(gold_text=enabled)
        self.btn_auto_attack.set_text(f"Auto Attack: {'ON' if enabled else 'OFF'}")

        try:
            self.write_command({"action": "auto_attack", "enabled": enabled})
        except OSError as e:
            self.log_msg("Không gửi được lệnh Auto Attack: " + str(e))
        self.log_msg(f"Auto Attack: {'ON' if enabled else 'OFF'}")

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
        if hasattr(self, "btn_auto_loot_opt"):
            self.btn_auto_loot_opt.set_selected(new_val)
            self.btn_auto_loot_opt.configure(gold_text=new_val)

        loot_filter = self.config.get("auto_loot_filter", DEFAULT_CONFIG["auto_loot_filter"])
        cmd = {"action": "auto_pickup", "enabled": new_val}
        cmd.update(loot_filter)
        try:
            self.write_command(cmd)
        except OSError as e:
            self.log_msg("Không gửi được lệnh Auto Loot: " + str(e))
        self.log_msg(f"Auto Loot: {'ON' if new_val else 'OFF'}")

    def send_loot_filter_command(self):
        loot_filter = self.config.get("auto_loot_filter", DEFAULT_CONFIG["auto_loot_filter"])
        cmd = {"action": "loot_filter"}
        cmd.update(loot_filter)
        try:
            self.write_command(cmd)
        except OSError as e:
            self.log_msg("Không gửi được lệnh bộ lọc nhặt đồ: " + str(e))

    def toggle_auto_sell(self):
        cur = self.config.get("auto_sell", False)
        new_val = not cur
        self.config["auto_sell"] = new_val
        save_config(self.config)

        self.btn_auto_sell.set_selected(new_val)
        self.btn_auto_sell.configure(gold_text=new_val)
        self.btn_auto_sell.set_text(f"Auto Sell: {'ON' if new_val else 'OFF'}")
        if hasattr(self, "btn_auto_sell_opt"):
            self.btn_auto_sell_opt.set_selected(new_val)
            self.btn_auto_sell_opt.configure(gold_text=new_val)

        sell_filter = self.config.get("auto_sell_filter", DEFAULT_CONFIG["auto_sell_filter"])
        cmd = {"action": "auto_sell", "enabled": new_val}
        cmd.update(sell_filter)
        try:
            self.write_command(cmd)
        except OSError as e:
            self.log_msg("Không gửi được lệnh Auto Sell: " + str(e))
        self.log_msg(f"Auto Sell: {'ON' if new_val else 'OFF'}")

    def send_sell_filter_command(self):
        sell_filter = self.config.get("auto_sell_filter", DEFAULT_CONFIG["auto_sell_filter"])
        cmd = {"action": "sell_filter"}
        cmd.update(sell_filter)
        try:
            self.write_command(cmd)
        except OSError as e:
            self.log_msg("Không gửi được lệnh bộ lọc bán đồ: " + str(e))

    def send_sell_now_command(self):
        sell_filter = self.config.get("auto_sell_filter", DEFAULT_CONFIG["auto_sell_filter"])
        cmd = {"action": "sell_now"}
        cmd.update(sell_filter)
        try:
            self.write_command(cmd)
            self.log_msg("Đã gửi yêu cầu Bán Ngay các món đồ thỏa điều kiện!")
        except OSError as e:
            self.log_msg("Không gửi được lệnh Bán Ngay: " + str(e))

    def send_sell_single_item(self, item_id, item_cat=3, item_name=""):
        cmd = {"action": "sell_single_item", "id": item_id, "cat": item_cat}
        try:
            self.write_command(cmd)
            self.log_msg(f"Đã gửi lệnh bán từng cái: {item_name} (ID: {item_id})")
        except OSError as e:
            self.log_msg("Không gửi được lệnh bán: " + str(e))

    # ==============================================================
    # ==============================================================
    # 4. RIGHT SIDEBAR (PLAYER INFO & CONTROLS)
    # ==============================================================
    def build_right_sidebar(self):
        right_container = tk.Frame(self.mid_area, bg=COLOR_BG_DARK, width=160)
        right_container.pack(side="right", fill="y", padx=(3, 0))
        right_container.pack_propagate(False)

        # 1. PLAYER PANEL (ID/TÊN NHÂN VẬT & 3 THANH HP, MP, EXP)
        self.p_char = HsoPanel(right_container, title="Offline")
        self.p_char.pack(fill="x", pady=(0, 3))
        c_char = self.p_char.body

        # Canvas vẽ 3 thanh HP, MP, EXP
        self.canvas_player_bars = tk.Canvas(c_char, bg="#1e1109", height=48, highlightthickness=0)
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
            font_size=8,
            width=96,
            height=23,
            renderer=self.btn_renderer,
            command=self.open_res_dialog
        )
        self.btn_screen.pack(fill="x", pady=(0, 3))

        # Hàng Auto Attack và Tùy chọn Auto Fam (⚙)
        row_attack_farm = tk.Frame(c_func, bg="#1e1109")
        row_attack_farm.pack(fill="x", pady=(0, 3))

        # Nút icon Option Auto Fam ở bên trái
        self.btn_auto_farm_opt = HsoButton(
            row_attack_farm,
            text="⚙",
            font_size=10,
            width=26,
            height=23,
            renderer=self.btn_renderer,
            command=self.open_auto_farm_modal
        )
        self.btn_auto_farm_opt.pack(side="left", padx=(0, 2))
        self.btn_auto_farm = self.btn_auto_farm_opt  # Alias tương thích

        # Nút Auto Attack ở bên phải của hàng
        is_attack = self.config.get("auto_attack", False)
        self.btn_auto_attack = HsoButton(
            row_attack_farm,
            text=f"Auto Attack: {'ON' if is_attack else 'OFF'}",
            font_size=8,
            height=23,
            is_selected=is_attack,
            gold_text=is_attack,
            renderer=self.btn_renderer,
            command=self.toggle_auto_attack
        )
        self.btn_auto_attack.pack(side="left", fill="x", expand=True)

        self.btn_unpin = HsoButton(
            c_func, text="Bỏ ghim mục tiêu", font_size=8,
            width=96, height=23, renderer=self.btn_renderer,
            command=self.unpin_target
        )
        self.btn_unpin.pack(fill="x", pady=(0, 3))
        self.btn_unpin.set_disabled(True)

        self.btn_pk = HsoButton(
            c_func, text="Đồ Sát: TẮT", font_size=8,
            width=96, height=23, renderer=self.btn_renderer,
            command=self.toggle_pk
        )
        self.btn_pk.pack(fill="x", pady=(0, 3))

        # Row Speed: [-] [Speed: 1.0x] [+]
        row_speed = tk.Frame(c_func, bg="#1e1109")
        row_speed.pack(fill="x", pady=(0, 3))

        cur_spd = str(self.config.get("speed", "1.0"))
        if cur_spd not in SPEED_LEVELS:
            cur_spd = "1.0"

        self.btn_speed_down = HsoButton(
            row_speed,
            text="-",
            font_size=9,
            width=24,
            height=23,
            renderer=self.btn_renderer,
            command=self.decrease_speed
        )
        self.btn_speed_down.pack(side="left", padx=(0, 2))

        self.btn_speed_up = HsoButton(
            row_speed,
            text="+",
            font_size=9,
            width=24,
            height=23,
            renderer=self.btn_renderer,
            command=self.increase_speed
        )
        self.btn_speed_up.pack(side="right", padx=(2, 0))

        self.btn_speed_disp = HsoButton(
            row_speed,
            text=f"Speed: {cur_spd}x",
            font_size=8,
            height=23,
            renderer=self.btn_renderer,
            gold_text=(cur_spd != "1.0"),
            command=None
        )
        self.btn_speed_disp.pack(side="left", fill="x", expand=True)

        self.update_speed_ui()

        # Hàng Auto Loot và Tùy chọn Lọc đồ (⚙)
        row_loot = tk.Frame(c_func, bg="#1e1109")
        row_loot.pack(fill="x", pady=(0, 3))

        is_pickup = self.config.get("auto_pickup", False)
        self.btn_auto_loot_opt = HsoButton(
            row_loot,
            text="⚙",
            font_size=10,
            width=26,
            height=23,
            is_selected=is_pickup,
            gold_text=is_pickup,
            renderer=self.btn_renderer,
            command=self.open_auto_loot_modal
        )
        self.btn_auto_loot_opt.pack(side="left", padx=(0, 2))

        self.btn_auto_pickup = HsoButton(
            row_loot,
            text=f"Auto Loot: {'ON' if is_pickup else 'OFF'}",
            font_size=8,
            height=23,
            is_selected=is_pickup,
            gold_text=is_pickup,
            renderer=self.btn_renderer,
            command=self.toggle_auto_pickup
        )
        self.btn_auto_pickup.pack(side="left", fill="x", expand=True)

        # Hàng Auto Bán Đồ và Tùy chọn Bán (⚙)
        row_sell = tk.Frame(c_func, bg="#1e1109")
        row_sell.pack(fill="x", pady=0)

        is_sell = self.config.get("auto_sell", False)
        self.btn_auto_sell_opt = HsoButton(
            row_sell,
            text="⚙",
            font_size=10,
            width=26,
            height=23,
            is_selected=is_sell,
            gold_text=is_sell,
            renderer=self.btn_renderer,
            command=self.open_auto_sell_modal
        )
        self.btn_auto_sell_opt.pack(side="left", padx=(0, 2))

        self.btn_auto_sell = HsoButton(
            row_sell,
            text=f"Auto Sell: {'ON' if is_sell else 'OFF'}",
            font_size=8,
            height=23,
            is_selected=is_sell,
            gold_text=is_sell,
            renderer=self.btn_renderer,
            command=self.toggle_auto_sell
        )
        self.btn_auto_sell.pack(side="left", fill="x", expand=True)

    def redraw_player_bars(self):
        c = self.canvas_player_bars
        c.delete("all")
        w = c.winfo_width()
        if w < 10:
            w = 148

        p = self.state_data.get("player") or getattr(self, "cached_player", {}) or {}
        hp = max(0, p.get("hp", 0))
        max_hp = max(1, p.get("maxHp", 1))
        mp = max(0, p.get("mp", 0))
        max_mp = max(1, p.get("maxMp") or p.get("maxmp", 1))
        exp_val = p.get("exp", 0)

        hp_pct = min(1.0, max(0.0, float(hp) / float(max_hp)))
        mp_pct = min(1.0, max(0.0, float(mp) / float(max_mp)))
        # exp_val is in tenths of a percent (e.g. 250 -> 25.0%)
        exp_pct = min(1.0, max(0.0, float(exp_val) / 1000.0))
        exp_str = f"EXP: {exp_val / 10.0:.1f}%"

        bh = 12
        pad_x = 2
        mid_x = w // 2

        # 1. HP BAR (Red)
        y1 = 2
        c.create_rectangle(pad_x, y1, w - pad_x, y1 + bh, fill="#1c0b05", outline="#5a1a15")
        fill_hp = int((w - 2 * pad_x - 2) * hp_pct)
        if fill_hp > 0:
            c.create_rectangle(pad_x + 1, y1 + 1, pad_x + 1 + fill_hp, y1 + bh - 1, fill=COLOR_HP_RED, outline="")
            c.create_rectangle(pad_x + 1, y1 + 1, pad_x + 1 + fill_hp, y1 + 2, fill="#ff7a7a", outline="")
        hp_str = f"HP: {hp}/{max_hp}"
        c.create_text(mid_x, y1 + bh // 2, text=hp_str, fill="#ffffff", font=(FONT_FAMILY, 7), anchor="center")

        # 2. MP BAR (Blue)
        y2 = 17
        c.create_rectangle(pad_x, y2, w - pad_x, y2 + bh, fill="#0b101c", outline="#15305a")
        fill_mp = int((w - 2 * pad_x - 2) * mp_pct)
        if fill_mp > 0:
            c.create_rectangle(pad_x + 1, y2 + 1, pad_x + 1 + fill_mp, y2 + bh - 1, fill=COLOR_MP_BLUE, outline="")
            c.create_rectangle(pad_x + 1, y2 + 1, pad_x + 1 + fill_mp, y2 + 2, fill="#7fb3d5", outline="")
        mp_str = f"MP: {mp}/{max_mp}"
        c.create_text(mid_x, y2 + bh // 2, text=mp_str, fill="#ffffff", font=(FONT_FAMILY, 7), anchor="center")

        # 3. EXP BAR (Green - Xanh lá)
        y3 = 32
        c.create_rectangle(pad_x, y3, w - pad_x, y3 + bh, fill="#0b1c0e", outline="#145a32")
        fill_exp = int((w - 2 * pad_x - 2) * exp_pct)
        if fill_exp > 0:
            c.create_rectangle(pad_x + 1, y3 + 1, pad_x + 1 + fill_exp, y3 + bh - 1, fill="#27ae60", outline="")
            c.create_rectangle(pad_x + 1, y3 + 1, pad_x + 1 + fill_exp, y3 + 2, fill="#58d68d", outline="")
        c.create_text(mid_x, y3 + bh // 2, text=exp_str, fill="#ffffff", font=(FONT_FAMILY, 7), anchor="center")

    def on_mon_tab_change(self, idx):
        self.active_mon_tab = idx
        self.refresh_mon_panel()

    def pin_monster_from_panel(self, event):
        if not self.state_data.get("in_game"):
            return
        y = self.canvas_mon.canvasy(event.y)
        x = event.x
        cw = self.canvas_mon.winfo_width()
        if cw < 30:
            cw = 136

        if self.active_mon_tab == 0:
            for x1, y1, x2, y2, monster_id in getattr(self, "monster_grid_cells", []):
                if x1 <= x < x2 and y1 <= y < y2:
                    if monster_id == self.state_data.get("pinned_monster_id", -1):
                        self.unpin_target()
                        return
                    try:
                        self.write_command({
                            "action": "pin_monster", "id": monster_id,
                            "map_id": self.state_data.get("map_id", 0),
                            "area": self.state_data.get("area", 0)
                        })
                        self.log_msg(f"Đang ghim quái ID {monster_id}...")
                    except (OSError, KeyError) as e:
                        self.log_msg("Không ghim được quái: " + str(e))
                    return
        elif self.active_mon_tab == 1:
            for top, bottom, p_id, p_name in getattr(self, "player_rows", []):
                if top <= y < bottom:
                    if p_id == self.state_data.get("pinned_player_id", -1):
                        self.unpin_target()
                        return
                    self.pin_player(p_id, p_name)
                    return
        elif self.active_mon_tab == 2:
            for top, bottom, npc in getattr(self, "npc_rows", []):
                if top <= y < bottom:
                    nx, ny = npc.get("x", 0), npc.get("y", 0)
                    self.log_msg(f"📍 NPC: {npc.get('name')} tại ({nx}, {ny})")
                    return

    def pin_player(self, player_id, player_name=""):
        try:
            self.write_command({
                "action": "pin_player", "id": player_id,
                "map_id": self.state_data.get("map_id", 0),
                "area": self.state_data.get("area", 0)
            })
            name_str = f" ({player_name})" if player_name else ""
            self.log_msg(f"📌 Đã ghim người chơi ID {player_id}{name_str}!")
        except (OSError, KeyError) as e:
            self.log_msg("Không ghim được người chơi: " + str(e))

    def attack_pinned_target(self):
        pinned_pl = self.state_data.get("pinned_player_id", -1)
        pinned_mon = self.state_data.get("pinned_monster_id", -1)
        if pinned_pl != -1:
            try:
                self.write_command({"action": "attack_player", "id": pinned_pl})
                self.log_msg(f"⚔️ Đang tấn công người chơi ID {pinned_pl}...")
            except OSError as e:
                self.log_msg("Lỗi gửi lệnh tấn công: " + str(e))
        elif pinned_mon >= 0:
            try:
                self.write_command({"action": "attack_monster", "id": pinned_mon})
                self.log_msg(f"⚔️ Đang tấn công quái ID {pinned_mon}...")
            except OSError as e:
                self.log_msg("Lỗi gửi lệnh tấn công: " + str(e))
        else:
            self.log_msg("Chưa ghim mục tiêu nào để tấn công!")

    def unpin_target(self):
        try:
            self.write_command({"action": "unpin_target"})
            self.log_msg("Đang bỏ ghim mục tiêu...")
        except OSError as e:
            self.log_msg("Không bỏ ghim được: " + str(e))

    def unpin_monster(self):
        self.unpin_target()

    def toggle_pk(self):
        if self.pk_cooldown_end and time.monotonic() < self.pk_cooldown_end:
            remaining = int(math.ceil(self.pk_cooldown_end - time.monotonic()))
            self.log_msg(f"⏳ Đồ Sát đang trong thời gian giãn cách ({remaining}s còn lại).")
            return
        try:
            my_pk = self.state_data.get("player", {}).get("typePk", -1)
            new_pk = -1 if my_pk == 0 else 0
            self.write_command({"action": "set_pk", "type_pk": new_pk})
            if new_pk == 0:
                self.log_msg("⚔️ Đang bật chế độ ĐỒ SÁT (giãn cách 3 phút)...")
            else:
                self.log_msg("🕊️ Đang tắt Đồ Sát, chuyển về Hòa Bình (giãn cách 3 phút)...")

            # Khởi động đếm ngược 180s (3 phút) và vô hiệu hóa nút
            self.pk_cooldown_end = time.monotonic() + 180.0
            self.update_pk_countdown()
        except (OSError, KeyError) as e:
            self.log_msg("Không gửi lệnh đổi PK được: " + str(e))

    def update_pk_countdown(self):
        if not hasattr(self, "btn_pk"):
            return
        now = time.monotonic()
        remaining = int(math.ceil(self.pk_cooldown_end - now))
        if remaining > 0:
            is_pk = (self.state_data.get("player", {}).get("typePk", -1) == 0)
            status = "BẬT" if is_pk else "TẮT"
            self.btn_pk.set_text(f"Đồ Sát: {status} ({remaining}s)")
            self.btn_pk.set_disabled(True)
            if self.pk_timer_id is not None:
                try:
                    self.root.after_cancel(self.pk_timer_id)
                except Exception:
                    pass
            self.pk_timer_id = self.root.after(1000, self.update_pk_countdown)
        else:
            self.pk_cooldown_end = 0.0
            self.pk_timer_id = None
            is_pk = (self.state_data.get("player", {}).get("typePk", -1) == 0)
            status = "BẬT" if is_pk else "TẮT"
            self.btn_pk.set_text(f"Đồ Sát: {status}")
            self.btn_pk.set_selected(is_pk)
            self.btn_pk.configure(gold_text=is_pk)
            self.btn_pk.set_disabled(False)

    def attack_player(self, player_id, player_name=""):
        try:
            self.write_command({
                "action": "attack_player", "id": player_id
            })
            name_str = f" {player_name}" if player_name else ""
            self.log_msg(f"⚔️ PK từ xa: Đang tấn công người chơi{name_str} (ID {player_id})...")
        except (OSError, KeyError) as e:
            self.log_msg("Không gửi lệnh tấn công được: " + str(e))

    def attack_current_target(self):
        if not self.state_data.get("in_game"):
            self.log_msg("Chưa vào game.")
            return
        pinned_pl = self.state_data.get("pinned_player_id", -1)
        if pinned_pl != -1:
            self.attack_player(pinned_pl)
            return
        focus_id = self.state_data.get("focus_target_id", -1)
        if focus_id != -1:
            self.attack_player(focus_id)
            return
        players = self.state_data.get("players", [])
        if players:
            me = self.state_data.get("player", {})
            mx, my = me.get("x", 0), me.get("y", 0)
            closest = min(players, key=lambda p: (p.get("x", 0) - mx)**2 + (p.get("y", 0) - my)**2)
            self.attack_player(closest.get("id", 0), closest.get("name", ""))
            return
        self.log_msg("Không có người chơi nào trong khu để tấn công.")

    def sync_farm_targets(self, data=None, force=False):
        data = data or self.state_data
        if not data.get("in_game"):
            self.last_farm_signature = None
            return
        map_id, area = data.get("map_id", 0), data.get("area", 0)
        other_count = data.get("other_players_count", len(data.get("players", [])))

        saved_profile = get_map_farm_profile(self.config, map_id, area)
        active_profile = saved_profile if other_count == 0 else None

        ids = resolve_farm_ids(active_profile, data.get("map_monsters", data.get("monsters", [])),
                               include_dead=True)
        is_configured = (active_profile is not None and len(ids) > 0)
        if hasattr(self, "btn_auto_farm_opt"):
            self.btn_auto_farm_opt.set_selected(is_configured)
            self.btn_auto_farm_opt.configure(gold_text=is_configured)
        farm_sync = self.config.get("farm_sync", True)
        signature = (map_id, area, is_configured, farm_sync, tuple(ids if is_configured else ()))
        if not force and signature == self.last_farm_signature:
            return
        try:
            self.write_command({
                "action": "farm_targets", "map_id": map_id, "area": area,
                "configured": 1 if is_configured else 0,
                "sync": 1 if farm_sync else 0,
                "ids": ";".join(str(monster_id) for monster_id in ids) if is_configured else ""
            })
        except OSError as e:
            self.log_msg("Không đồng bộ được Auto Fam: " + str(e))

    def open_auto_loot_modal(self):
        old_dialog = getattr(self, "auto_loot_dialog", None)
        if old_dialog is not None and old_dialog.winfo_exists():
            old_dialog.lift()
            return

        dlg = tk.Toplevel(self.root)
        self.auto_loot_dialog = dlg
        dlg.title("Cấu hình Auto Loot")
        dlg.geometry("380x490")
        dlg.minsize(360, 460)
        dlg.configure(bg=COLOR_BG_DARK)
        dlg.transient(self.root)
        dlg.grab_set()

        try:
            x = self.root.winfo_x() + (self.root.winfo_width() - 380) // 2
            y = self.root.winfo_y() + (self.root.winfo_height() - 490) // 2
            dlg.geometry(f"+{max(10, x)}+{max(10, y)}")
        except Exception:
            pass

        panel = HsoPanel(dlg, title="CẤU HÌNH AUTO LOOT - LỌC ĐỒ")
        panel.pack(fill="both", expand=True, padx=4, pady=4)
        body = panel.body

        cur_filter = self.config.get("auto_loot_filter", {})
        default_filter = DEFAULT_CONFIG["auto_loot_filter"]

        filter_vars = {}
        for k in default_filter:
            filter_vars[k] = tk.BooleanVar(value=bool(cur_filter.get(k, default_filter[k])))

        # Subtitle & status
        sub_frame = tk.Frame(body, bg="#1e1109")
        sub_frame.pack(fill="x", pady=(0, 4))

        tk.Label(
            sub_frame,
            text="Chọn các loại vật phẩm được phép tự động nhặt:",
            font=(FONT_FAMILY, 8),
            fg=COLOR_TEXT_GOLD,
            bg="#1e1109"
        ).pack(anchor="w")

        summary_var = tk.StringVar()
        def update_summary():
            count = sum(1 for v in filter_vars.values() if v.get())
            summary_var.set(f"Đang chọn: {count}/{len(filter_vars)} loại")

        tk.Label(
            sub_frame,
            textvariable=summary_var,
            font=(FONT_FAMILY, 7),
            fg=COLOR_TEXT_MUTED,
            bg="#1e1109"
        ).pack(anchor="w")

        # Quick select toolbar
        quick_bar = tk.Frame(body, bg="#1e1109")
        quick_bar.pack(fill="x", pady=(0, 6))

        def set_all_loot(val):
            for v in filter_vars.values():
                v.set(val)
            update_summary()

        btn_select_all = HsoButton(
            quick_bar,
            text="Chọn tất cả",
            font_size=7,
            width=80,
            height=20,
            renderer=self.btn_renderer,
            command=lambda: set_all_loot(True)
        )
        btn_select_all.pack(side="left", padx=(0, 4))

        btn_deselect_all = HsoButton(
            quick_bar,
            text="Bỏ chọn hết",
            font_size=7,
            width=80,
            height=20,
            renderer=self.btn_renderer,
            command=lambda: set_all_loot(False)
        )
        btn_deselect_all.pack(side="left")

        # Container box
        content_box = tk.Frame(body, bg=COLOR_INNER_BG, bd=1, relief="solid")
        content_box.pack(fill="both", expand=True, pady=(0, 6))

        # --- SECTION 1: TRANG BỊ ---
        lbl_sec1 = tk.Label(
            content_box,
            text="  ⚔ TRANG BỊ THEO PHẨM CẤP / MÀU  ",
            font=(FONT_FAMILY, 8, "bold"),
            fg=COLOR_TEXT_GOLD,
            bg="#2a160d"
        )
        lbl_sec1.pack(fill="x", pady=(2, 2))

        f_equip = tk.Frame(content_box, bg=COLOR_INNER_BG)
        f_equip.pack(fill="x", padx=8, pady=2)

        equip_items = [
            ("white", "⚪ Đồ Trắng (Thường)", "#ede8df"),
            ("blue", "🔵 Đồ Xanh (Đặc biệt)", "#5dade2"),
            ("yellow", "🟡 Đồ Vàng (Hiếm)", "#f4d03f"),
            ("purple", "🟣 Đồ Tím (Huyền bí)", "#bb8fce"),
            ("orange", "🟠 Đồ Cam (Sử thi)", "#eb984e"),
            ("green", "🟢 Đồ Xanh lá (Hoàn mỹ)", "#58d68d"),
        ]
        for idx, (key, text, col) in enumerate(equip_items):
            r = idx // 2
            c = idx % 2
            cb = tk.Checkbutton(
                f_equip,
                text=text,
                variable=filter_vars[key],
                command=update_summary,
                font=(FONT_FAMILY, 8),
                bg=COLOR_INNER_BG,
                fg=col,
                selectcolor="#2a160d",
                activebackground=COLOR_INNER_BG,
                activeforeground=col,
                bd=0,
                highlightthickness=0
            )
            cb.grid(row=r, column=c, sticky="w", padx=4, pady=2)
        f_equip.grid_columnconfigure(0, weight=1)
        f_equip.grid_columnconfigure(1, weight=1)

        # --- SECTION 2: HỒI PHỤC & TIỀN TỆ ---
        lbl_sec2 = tk.Label(
            content_box,
            text="  🧪 HỒI PHỤC & TIỀN TỆ  ",
            font=(FONT_FAMILY, 8, "bold"),
            fg=COLOR_TEXT_GOLD,
            bg="#2a160d"
        )
        lbl_sec2.pack(fill="x", pady=(6, 2))

        f_potions = tk.Frame(content_box, bg=COLOR_INNER_BG)
        f_potions.pack(fill="x", padx=8, pady=2)

        potion_items = [
            ("hp", "🩸 Bình Máu (HP)", "#ff6b6b"),
            ("mp", "💧 Bình Mana (MP)", "#5dade2"),
            ("gold", "💰 Tiền Vàng (Xu)", "#f5b041"),
        ]
        for idx, (key, text, col) in enumerate(potion_items):
            cb = tk.Checkbutton(
                f_potions,
                text=text,
                variable=filter_vars[key],
                command=update_summary,
                font=(FONT_FAMILY, 8),
                bg=COLOR_INNER_BG,
                fg=col,
                selectcolor="#2a160d",
                activebackground=COLOR_INNER_BG,
                activeforeground=col,
                bd=0,
                highlightthickness=0
            )
            cb.grid(row=0, column=idx, sticky="w", padx=2, pady=2)
        for i in range(3):
            f_potions.grid_columnconfigure(i, weight=1)

        # --- SECTION 3: NGUYÊN LIỆU & NHIỆM VỤ ---
        lbl_sec3 = tk.Label(
            content_box,
            text="  📦 NGUYÊN LIỆU & KHÁC  ",
            font=(FONT_FAMILY, 8, "bold"),
            fg=COLOR_TEXT_GOLD,
            bg="#2a160d"
        )
        lbl_sec3.pack(fill="x", pady=(6, 2))

        f_misc = tk.Frame(content_box, bg=COLOR_INNER_BG)
        f_misc.pack(fill="x", padx=8, pady=2)

        misc_items = [
            ("material", "💎 Nguyên liệu & Đá", "#a569bd"),
            ("quest", "📜 Vật phẩm Nhiệm vụ", "#e59866"),
        ]
        for idx, (key, text, col) in enumerate(misc_items):
            cb = tk.Checkbutton(
                f_misc,
                text=text,
                variable=filter_vars[key],
                command=update_summary,
                font=(FONT_FAMILY, 8),
                bg=COLOR_INNER_BG,
                fg=col,
                selectcolor="#2a160d",
                activebackground=COLOR_INNER_BG,
                activeforeground=col,
                bd=0,
                highlightthickness=0
            )
            cb.grid(row=0, column=idx, sticky="w", padx=4, pady=2)
        f_misc.grid_columnconfigure(0, weight=1)
        f_misc.grid_columnconfigure(1, weight=1)

        update_summary()

        # Bottom buttons: Hủy & Lưu
        bottom_bar = tk.Frame(body, bg="#1e1109")
        bottom_bar.pack(side="bottom", fill="x", pady=(4, 0))

        def on_cancel():
            dlg.destroy()

        def on_save():
            new_filter = {k: bool(v.get()) for k, v in filter_vars.items()}
            self.config["auto_loot_filter"] = new_filter
            save_config(self.config)
            self.send_loot_filter_command()
            self.log_msg("Đã lưu cấu hình bộ lọc Auto Loot!")
            dlg.destroy()

        btn_cancel = HsoButton(
            bottom_bar,
            text="Hủy",
            font_size=8,
            width=70,
            height=24,
            renderer=self.btn_renderer,
            command=on_cancel
        )
        btn_cancel.pack(side="left")

        btn_save = HsoButton(
            bottom_bar,
            text="LƯU CẤU HÌNH",
            font_size=8,
            width=110,
            height=24,
            is_selected=True,
            gold_text=True,
            renderer=self.btn_renderer,
            command=on_save
        )
        btn_save.pack(side="right")

    def open_auto_sell_modal(self):
        old_dialog = getattr(self, "auto_sell_dialog", None)
        if old_dialog is not None and old_dialog.winfo_exists():
            old_dialog.lift()
            return

        dlg = tk.Toplevel(self.root)
        self.auto_sell_dialog = dlg
        dlg.title("Ban Do Tu Xa & Cau Hinh Auto")
        dlg.geometry("540x600")
        dlg.minsize(480, 520)
        dlg.configure(bg=COLOR_BG_DARK)
        dlg.transient(self.root)
        try:
            dlg.grab_set()
        except Exception:
            pass

        try:
            x = self.root.winfo_x() + (self.root.winfo_width() - 540) // 2
            y = self.root.winfo_y() + (self.root.winfo_height() - 600) // 2
            dlg.geometry(f"+{max(10, x)}+{max(10, y)}")
        except Exception:
            pass

        cur_filter = self.config.get("auto_sell_filter", {})
        default_filter = DEFAULT_CONFIG["auto_sell_filter"]
        filter_vars = {}
        for k in default_filter:
            filter_vars[k] = tk.BooleanVar(value=bool(cur_filter.get(k, default_filter[k])))

        color_map = {
            0: ("[Trắng]", "#ffffff"),
            1: ("[Xanh]", "#5dade2"),
            2: ("[Vàng]", "#f4d03f"),
            3: ("[Tím]", "#bb8fce"),
            4: ("[Cam]", "#eb984e"),
            5: ("[Xanh lá]", "#58d68d")
        }

        def does_item_match_filter(it):
            if not isinstance(it, dict):
                return False
            name = it.get("name", "")
            is_locked = (it.get("lock", 0) > 0) or ("[Kho" in name) or ("[kho" in name)
            if filter_vars["unlocked_only"].get() and is_locked:
                return False

            cat = it.get("cat", 0)
            if cat == 3:  # Trang bị
                if filter_vars["tier0_only"].get() and it.get("tier", 0) != 0:
                    return False
                color = it.get("color", 0)
                color_keys = {0: "white", 1: "blue", 2: "yellow", 3: "purple", 4: "orange", 5: "green"}
                k = color_keys.get(color)
                return bool(k and filter_vars.get(k) and filter_vars[k].get())
            elif cat == 7:  # Nguyên liệu
                return bool(filter_vars.get("material") and filter_vars["material"].get())
            return False

        info_var = tk.StringVar(value="Túi: Đang tải...")
        summary_var = tk.StringVar(value="Đang tính toán...")
        list_info_var = tk.StringVar(value="Hành trang: Đang tải...")
        last_inventory_hash = [""]

        def update_summary():
            inv = self.state_data.get("inventory", [])
            p = self.state_data.get("player") or getattr(self, "cached_player", {}) or {}
            coin = p.get("coin", 0)
            inven_count = p.get("inven_count", len(inv))
            inven_max = p.get("inven_max", 42)

            match_count = 0
            match_equip = 0
            match_mat = 0

            for it in inv:
                if does_item_match_filter(it):
                    match_count += 1
                    if it.get("cat") == 3:
                        match_equip += 1
                    elif it.get("cat") == 7:
                        match_mat += 1

            info_var.set(f"Túi: {inven_count}/{inven_max} ô  |  Vàng: {coin:,} xu")
            summary_var.set(f"Đang chọn bán: {match_count} món (Trang bị: {match_equip}, NL: {match_mat})")
            list_info_var.set(f"Hành trang: {inven_count}/{inven_max} ô  |  Vàng: {coin:,} xu")

        # Container tabs
        tab_names = ["BANG DO BAN (HANH TRANG)", "BO LOC AUTO BAN"]

        def on_tab_switch(idx):
            if idx == 0:
                frame_tab_config.pack_forget()
                frame_tab_list.pack(fill="both", expand=True)
                refresh_item_list()
            else:
                frame_tab_list.pack_forget()
                frame_tab_config.pack(fill="both", expand=True)
                update_summary()

        panel = HsoPanel(dlg, tabs=tab_names, on_tab_change=on_tab_switch)
        panel.pack(fill="both", expand=True, padx=4, pady=4)
        body = panel.body

        # ==============================================================
        # TAB 0: BẢNG DANH SÁCH ĐỒ BÁN (HÀNH TRANG)
        # ==============================================================
        frame_tab_list = tk.Frame(body, bg="#1e1109")
        frame_tab_list.pack(fill="both", expand=True)

        top_list_bar = tk.Frame(frame_tab_list, bg="#1e1109")
        top_list_bar.pack(fill="x", pady=(0, 4))

        lbl_list_info = tk.Label(
            top_list_bar,
            textvariable=list_info_var,
            font=(FONT_FAMILY, 8, "bold"),
            fg="#2ecc71",
            bg="#1e1109"
        )
        lbl_list_info.pack(side="left")

        btn_refresh_list = HsoButton(
            top_list_bar,
            text="Làm mới",
            font_size=7,
            width=70,
            height=20,
            renderer=self.btn_renderer,
            command=lambda: refresh_item_list()
        )
        btn_refresh_list.pack(side="right")

        btn_sell_junk_top = HsoButton(
            top_list_bar,
            text="Bán rác lần lượt",
            font_size=7,
            width=110,
            height=20,
            gold_text=True,
            renderer=self.btn_renderer,
            command=lambda: on_sell_now()
        )
        btn_sell_junk_top.pack(side="right", padx=(0, 4))

        # Tiêu đề cột
        col_header = tk.Frame(frame_tab_list, bg="#2a160d", height=20)
        col_header.pack(fill="x", pady=(0, 2))
        tk.Label(col_header, text="  Tên vật phẩm", font=(FONT_FAMILY, 7, "bold"), fg=COLOR_TEXT_GOLD, bg="#2a160d").pack(side="left", padx=4)
        tk.Label(col_header, text="Thao tác  ", font=(FONT_FAMILY, 7, "bold"), fg=COLOR_TEXT_GOLD, bg="#2a160d").pack(side="right", padx=8)
        tk.Label(col_header, text="Trạng thái", font=(FONT_FAMILY, 7, "bold"), fg=COLOR_TEXT_GOLD, bg="#2a160d").pack(side="right", padx=16)
        tk.Label(col_header, text="Cấp / Tier", font=(FONT_FAMILY, 7, "bold"), fg=COLOR_TEXT_GOLD, bg="#2a160d").pack(side="right", padx=16)

        # Scrollable container cho danh sách đồ
        list_container = tk.Frame(frame_tab_list, bg=COLOR_INNER_BG, bd=1, relief="solid")
        list_container.pack(fill="both", expand=True)

        canvas_list = tk.Canvas(list_container, bg=COLOR_INNER_BG, highlightthickness=0)
        scrollbar_list = ttk.Scrollbar(list_container, orient="vertical", command=canvas_list.yview)
        canvas_list.configure(yscrollcommand=scrollbar_list.set)
        canvas_list.pack(side="left", fill="both", expand=True)
        scrollbar_list.pack(side="right", fill="y")

        items_frame = tk.Frame(canvas_list, bg=COLOR_INNER_BG)
        window_items = canvas_list.create_window((0, 0), window=items_frame, anchor="nw")

        def _on_frame_config(event):
            canvas_list.configure(scrollregion=canvas_list.bbox("all"))
        items_frame.bind("<Configure>", _on_frame_config)

        def _on_canvas_config(event):
            canvas_list.itemconfigure(window_items, width=event.width)
        canvas_list.bind("<Configure>", _on_canvas_config)

        def _on_mousewheel(event):
            canvas_list.yview_scroll(int(-event.delta / 120), "units")
        canvas_list.bind("<MouseWheel>", _on_mousewheel)
        canvas_list.bind("<Button-4>", lambda e: canvas_list.yview_scroll(-1, "units"))
        canvas_list.bind("<Button-5>", lambda e: canvas_list.yview_scroll(1, "units"))

        lbl_tip = tk.Label(
            frame_tab_list,
            text="Lưu ý: Bán từng món tuần tự và chờ phản hồi từ máy chủ để tránh nghẽn lệnh.",
            font=(FONT_FAMILY, 7),
            fg=COLOR_TEXT_MUTED,
            bg="#1e1109"
        )
        lbl_tip.pack(anchor="w", pady=(2, 0))

        def refresh_item_list():
            update_summary()
            inv = self.state_data.get("inventory", [])

            for child in items_frame.winfo_children():
                child.destroy()

            # Ưu tiên trang bị (cat == 3) lên đầu danh sách
            tradeable_items = sorted(
                [it for it in inv if isinstance(it, dict) and it.get("cat") in (3, 7)],
                key=lambda x: (0 if x.get("cat") == 3 else 1, x.get("color", 0))
            )

            if not tradeable_items:
                lbl_empty = tk.Label(
                    items_frame,
                    text="Không có trang bị nào trong hành trang.",
                    font=(FONT_FAMILY, 8),
                    fg=COLOR_TEXT_MUTED,
                    bg=COLOR_INNER_BG
                )
                lbl_empty.pack(pady=20)
                return

            for idx, it in enumerate(tradeable_items):
                item_id = it.get("id", 0)
                item_cat = it.get("cat", 3)
                item_name = it.get("name", f"Item #{item_id}")
                color_idx = it.get("color", 0)
                tier = it.get("tier", 0)
                level = it.get("level", 0)
                is_locked = (it.get("lock", 0) > 0) or ("[Kho" in item_name) or ("[kho" in item_name)
                matches_filter = does_item_match_filter(it)

                col_tag, col_hex = color_map.get(color_idx, ("[Khác]", "#ede8df"))

                row_bg = "#25140b" if idx % 2 == 0 else "#1c0e07"
                row_frame = tk.Frame(items_frame, bg=row_bg, height=28)
                row_frame.pack(fill="x", padx=2, pady=1)

                f_name = tk.Frame(row_frame, bg=row_bg)
                f_name.pack(side="left", padx=4, fill="y")

                lbl_col_tag = tk.Label(f_name, text=col_tag, font=(FONT_FAMILY, 7), fg=col_hex, bg=row_bg)
                lbl_col_tag.pack(side="left", padx=(0, 2))

                display_name = item_name
                for tag in (" [Khoá]", " [Khóa]", "[Khoá]", "[Khóa]"):
                    if display_name.endswith(tag):
                        display_name = display_name[:-len(tag)].rstrip()
                        break

                lbl_it_name = tk.Label(f_name, text=display_name, font=(FONT_FAMILY, 8, "bold"), fg=col_hex, bg=row_bg, anchor="w")
                lbl_it_name.pack(side="left")

                if is_locked:
                    lbl_lock_tag = tk.Label(f_name, text=" [Khoá]", font=(FONT_FAMILY, 7, "bold"), fg="#e74c3c", bg=row_bg)
                    lbl_lock_tag.pack(side="left", padx=2)

                def make_sell_handler(i_id, i_cat, i_name, locked):
                    def do_sell():
                        if locked:
                            if not messagebox.askyesno("Xác nhận bán", f"Món đồ '{i_name}' đang có KHÓA BẢO HIỂM.\nBạn có chắc muốn bán không?"):
                                return
                        self.send_sell_single_item(i_id, i_cat, i_name)
                    return do_sell

                btn_sell_one = HsoButton(
                    row_frame,
                    text="Bán",
                    font_size=7,
                    width=46,
                    height=18,
                    gold_text=True,
                    renderer=self.btn_renderer,
                    command=make_sell_handler(item_id, item_cat, item_name, is_locked)
                )
                btn_sell_one.pack(side="right", padx=6, pady=3)

                status_text = "[Sẽ bán]" if matches_filter else "[Giữ lại]"
                status_color = "#2ecc71" if matches_filter else COLOR_TEXT_MUTED
                lbl_status = tk.Label(row_frame, text=status_text, font=(FONT_FAMILY, 7), fg=status_color, bg=row_bg, width=10, anchor="e")
                lbl_status.pack(side="right", padx=6)

                tier_str = f"Lv.{level} (+{tier})" if tier > 0 else f"Lv.{level}"
                lbl_tier = tk.Label(row_frame, text=tier_str, font=(FONT_FAMILY, 7), fg="#d5dbdb", bg=row_bg, width=10, anchor="e")
                lbl_tier.pack(side="right", padx=6)

        # ==============================================================
        # TAB 1: CẤU HÌNH BỘ LỌC AUTO BÁN
        # ==============================================================
        frame_tab_config = tk.Frame(body, bg="#1e1109")

        sub_frame_cfg = tk.Frame(frame_tab_config, bg="#1e1109")
        sub_frame_cfg.pack(fill="x", pady=(0, 4))

        tk.Label(
            sub_frame_cfg,
            text="Cấu hình bộ lọc tự động bán từ xa (thu tiền Vàng):",
            font=(FONT_FAMILY, 8),
            fg=COLOR_TEXT_GOLD,
            bg="#1e1109"
        ).pack(anchor="w")

        lbl_info = tk.Label(
            sub_frame_cfg,
            textvariable=info_var,
            font=(FONT_FAMILY, 7, "bold"),
            fg="#2ecc71",
            bg="#1e1109"
        )
        lbl_info.pack(anchor="w", pady=(2, 0))

        lbl_summary = tk.Label(
            sub_frame_cfg,
            textvariable=summary_var,
            font=(FONT_FAMILY, 7),
            fg=COLOR_TEXT_MUTED,
            bg="#1e1109"
        )
        lbl_summary.pack(anchor="w")

        quick_bar = tk.Frame(frame_tab_config, bg="#1e1109")
        quick_bar.pack(fill="x", pady=(0, 6))

        def set_junk_only():
            filter_vars["white"].set(True)
            filter_vars["blue"].set(True)
            filter_vars["yellow"].set(False)
            filter_vars["purple"].set(False)
            filter_vars["orange"].set(False)
            filter_vars["green"].set(False)
            filter_vars["material"].set(False)
            update_summary()

        def set_all_sell(val):
            filter_vars["white"].set(val)
            filter_vars["blue"].set(val)
            filter_vars["yellow"].set(val)
            filter_vars["purple"].set(val)
            filter_vars["orange"].set(val)
            filter_vars["green"].set(val)
            filter_vars["material"].set(val)
            update_summary()

        btn_select_junk = HsoButton(
            quick_bar,
            text="Đồ rác (Trắng+Xanh)",
            font_size=7,
            width=110,
            height=20,
            renderer=self.btn_renderer,
            command=set_junk_only
        )
        btn_select_junk.pack(side="left", padx=(0, 4))

        btn_select_all = HsoButton(
            quick_bar,
            text="Chọn hết",
            font_size=7,
            width=65,
            height=20,
            renderer=self.btn_renderer,
            command=lambda: set_all_sell(True)
        )
        btn_select_all.pack(side="left", padx=(0, 4))

        btn_deselect_all = HsoButton(
            quick_bar,
            text="Bỏ hết",
            font_size=7,
            width=65,
            height=20,
            renderer=self.btn_renderer,
            command=lambda: set_all_sell(False)
        )
        btn_deselect_all.pack(side="left")

        content_box = tk.Frame(frame_tab_config, bg=COLOR_INNER_BG, bd=1, relief="solid")
        content_box.pack(fill="both", expand=True, pady=(0, 6))

        # SECTION 1: TRANG BỊ THEO MÀU
        lbl_sec1 = tk.Label(
            content_box,
            text="  TRANG BỊ THEO PHẨM CẤP / MÀU  ",
            font=(FONT_FAMILY, 8, "bold"),
            fg=COLOR_TEXT_GOLD,
            bg="#2a160d"
        )
        lbl_sec1.pack(fill="x", pady=(2, 2))

        f_equip = tk.Frame(content_box, bg=COLOR_INNER_BG)
        f_equip.pack(fill="x", padx=8, pady=2)

        equip_items = [
            ("white", "[Trắng] Thường", "#ede8df"),
            ("blue", "[Xanh] Đặc biệt", "#5dade2"),
            ("yellow", "[Vàng] Hiếm", "#f4d03f"),
            ("purple", "[Tím] Huyền bí", "#bb8fce"),
            ("orange", "[Cam] Sử thi", "#eb984e"),
            ("green", "[Xanh lá] Hoàn mỹ", "#58d68d"),
        ]
        for idx, (key, text, col) in enumerate(equip_items):
            r = idx // 2
            c = idx % 2
            cb = tk.Checkbutton(
                f_equip,
                text=text,
                variable=filter_vars[key],
                command=update_summary,
                font=(FONT_FAMILY, 8),
                bg=COLOR_INNER_BG,
                fg=col,
                selectcolor="#2a160d",
                activebackground=COLOR_INNER_BG,
                activeforeground=col,
                bd=0,
                highlightthickness=0
            )
            cb.grid(row=r, column=c, sticky="w", padx=4, pady=2)
        f_equip.grid_columnconfigure(0, weight=1)
        f_equip.grid_columnconfigure(1, weight=1)

        # SECTION 2: BẢO VỆ AN TOÀN
        lbl_sec2 = tk.Label(
            content_box,
            text="  TÙY CHỌN BẢO VỆ AN TOÀN  ",
            font=(FONT_FAMILY, 8, "bold"),
            fg=COLOR_TEXT_GOLD,
            bg="#2a160d"
        )
        lbl_sec2.pack(fill="x", pady=(6, 2))

        f_safe = tk.Frame(content_box, bg=COLOR_INNER_BG)
        f_safe.pack(fill="x", padx=8, pady=2)

        safe_items = [
            ("tier0_only", "Chỉ bán trang bị +0 (Bảo vệ đồ cường hóa)", "#f39c12"),
            ("unlocked_only", "Không bao giờ bán đồ có khóa bảo hiểm", "#e74c3c"),
        ]
        for key, text, col in safe_items:
            cb = tk.Checkbutton(
                f_safe,
                text=text,
                variable=filter_vars[key],
                command=update_summary,
                font=(FONT_FAMILY, 8),
                bg=COLOR_INNER_BG,
                fg=col,
                selectcolor="#2a160d",
                activebackground=COLOR_INNER_BG,
                activeforeground=col,
                bd=0,
                highlightthickness=0
            )
            cb.pack(anchor="w", padx=4, pady=2)

        # SECTION 3: NGUYÊN LIỆU
        lbl_sec3 = tk.Label(
            content_box,
            text="  NGUYÊN LIỆU & VẬT PHẨM KHÁC  ",
            font=(FONT_FAMILY, 8, "bold"),
            fg=COLOR_TEXT_GOLD,
            bg="#2a160d"
        )
        lbl_sec3.pack(fill="x", pady=(6, 2))

        f_mat = tk.Frame(content_box, bg=COLOR_INNER_BG)
        f_mat.pack(fill="x", padx=8, pady=2)

        cb_mat = tk.Checkbutton(
            f_mat,
            text="Bán nguyên liệu thường (Category 7)",
            variable=filter_vars["material"],
            command=update_summary,
            font=(FONT_FAMILY, 8),
            bg=COLOR_INNER_BG,
            fg="#a569bd",
            selectcolor="#2a160d",
            activebackground=COLOR_INNER_BG,
            activeforeground="#a569bd",
            bd=0,
            highlightthickness=0
        )
        cb_mat.pack(anchor="w", padx=4, pady=2)

        # Nạp dữ liệu lần đầu
        refresh_item_list()

        # Polling cập nhật thời gian thực khi dialog mở
        def poll_live():
            if not dlg.winfo_exists():
                return
            inv = self.state_data.get("inventory", [])
            inv_hash = f"{len(inv)}_{sum(x.get('id', 0) for x in inv if isinstance(x, dict))}"
            if inv_hash != last_inventory_hash[0]:
                last_inventory_hash[0] = inv_hash
                refresh_item_list()
            dlg.after(600, poll_live)
        poll_live()

        # ==============================================================
        # BOTTOM BAR
        # ==============================================================
        bottom_bar = tk.Frame(body, bg="#1e1109")
        bottom_bar.pack(side="bottom", fill="x", pady=(4, 0))

        def on_cancel():
            dlg.destroy()

        def on_sell_now():
            new_filter = {k: bool(v.get()) for k, v in filter_vars.items()}
            self.config["auto_sell_filter"] = new_filter
            save_config(self.config)
            self.send_sell_now_command()
            self.log_msg("Đang tiến hành bán tuần tự từng món rác...")

        def on_save():
            new_filter = {k: bool(v.get()) for k, v in filter_vars.items()}
            self.config["auto_sell_filter"] = new_filter
            save_config(self.config)
            self.send_sell_filter_command()
            self.log_msg("Đã lưu cấu hình Auto Bán Đồ!")
            dlg.destroy()

        btn_cancel = HsoButton(
            bottom_bar,
            text="Đóng",
            font_size=8,
            width=60,
            height=24,
            renderer=self.btn_renderer,
            command=on_cancel
        )
        btn_cancel.pack(side="left")

        btn_sell_now = HsoButton(
            bottom_bar,
            text="Bán rác lần lượt",
            font_size=8,
            width=135,
            height=24,
            gold_text=True,
            renderer=self.btn_renderer,
            command=on_sell_now
        )
        btn_sell_now.pack(side="left", padx=(6, 0))

        btn_save = HsoButton(
            bottom_bar,
            text="Lưu cấu hình",
            font_size=8,
            width=110,
            height=24,
            is_selected=True,
            gold_text=True,
            renderer=self.btn_renderer,
            command=on_save
        )
        btn_save.pack(side="right")

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
        profile = get_map_farm_profile(self.config, map_id, area)
        try:
            self.write_command({"action": "scan_monsters"})
        except OSError as e:
            self.log_msg("Không bắt đầu được quét quái: " + str(e))

        dlg = tk.Toplevel(self.root)
        self.auto_farm_dialog = dlg
        dlg.title("Auto Fam")
        dlg.geometry("360x440")
        dlg.minsize(330, 360)
        dlg.configure(bg=COLOR_BG_DARK)
        dlg.transient(self.root)
        dlg.grab_set()

        try:
            x = self.root.winfo_x() + (self.root.winfo_width() - 360) // 2
            y = self.root.winfo_y() + (self.root.winfo_height() - 440) // 2
            dlg.geometry(f"+{max(10, x)}+{max(10, y)}")
        except Exception:
            pass

        panel = HsoPanel(dlg, title="AUTO FAM - CHỌN QUÁI")
        panel.pack(fill="both", expand=True, padx=4, pady=4)
        body = panel.body

        # DÒNG 1: THÔNG TIN MAP & TIẾN ĐỘ QUÉT (GỌN GÀNG, 22px)
        header_top = tk.Frame(body, bg="#1a0c06", bd=1, relief="ridge")
        header_top.pack(fill="x", pady=(0, 2), padx=1)
        title_var = tk.StringVar(value="")
        status_var = tk.StringVar(value="")
        selected_count = tk.StringVar(value="Tất cả (0/0)")
        tk.Label(header_top, textvariable=title_var, font=(FONT_FAMILY, 8, "bold"),
                 fg=COLOR_TEXT_GOLD, bg="#1a0c06").pack(side="left", padx=5, pady=2)
        tk.Label(header_top, textvariable=status_var, font=(FONT_FAMILY, 7),
                 fg=COLOR_TEXT_MUTED, bg="#1a0c06").pack(side="right", padx=5, pady=2)

        # DÒNG 2: THANH ĐIỀU KHIỂN & NÚT ĐỒNG BỘ FARM (24px)
        ctrl_bar = tk.Frame(body, bg="#24120a", bd=1, relief="solid")
        ctrl_bar.pack(fill="x", pady=(0, 3), padx=1)

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
        all_groups_expanded = True

        def refresh_checks():
            for name, members in groups.items():
                chosen = sum(monster_vars[m["id"]].get() for m in members if m["id"] in monster_vars)
                group_vars[name].set(chosen == len(members) and len(members) > 0)
                group_labels[name].set(f"{name} ({chosen}/{len(members)})")
            chosen_total = sum(var.get() for var in monster_vars.values())
            all_var.set(bool(monster_vars) and chosen_total == len(monster_vars))
            selected_count.set(f"Tất cả ({chosen_total}/{len(monster_vars)})")

        def set_all(value):
            nonlocal selection_touched
            selection_touched = True
            for var in monster_vars.values():
                var.set(value)
            refresh_checks()

        # Checkbox chọn tất cả quái
        tk.Checkbutton(ctrl_bar, textvariable=selected_count, variable=all_var,
                       command=lambda: set_all(all_var.get()), font=(FONT_FAMILY, 7, "bold"),
                       bg="#24120a", fg=COLOR_TEXT_GOLD, selectcolor=COLOR_INNER_BG,
                       activebackground="#24120a", activeforeground=COLOR_TEXT_GOLD).pack(side="left", padx=4, pady=1)

        # NÚT ĐỒNG BỘ FARM (SYNC TOGGLE BUTTON)
        def update_sync_btn():
            is_sync = self.config.get("farm_sync", True)
            if is_sync:
                btn_sync.configure(text="🔄 ĐỒNG BỘ: BẬT")
                btn_sync.set_selected(True)
                btn_sync.gold_text = True
            else:
                btn_sync.configure(text="⚡ TỰ DO: RA LÀ HIT")
                btn_sync.set_selected(False)
                btn_sync.gold_text = False
            btn_sync.redraw()

        def toggle_sync_mode():
            cur = self.config.get("farm_sync", True)
            new_val = not cur
            self.config["farm_sync"] = new_val
            save_config(self.config)
            update_sync_btn()
            try:
                self.write_command({"action": "farm_sync", "enabled": 1 if new_val else 0})
            except Exception:
                pass
            self.sync_farm_targets(force=True)
            self.log_msg(f"Auto Fam: Chế độ Đồng bộ -> {'BẬT (Đợi cả đàn hồi sinh để gom đánh)' if new_val else 'TẮT (Con nào ra trước hit ngay lập tức)'}")

        btn_sync = HsoButton(
            ctrl_bar,
            text="🔄 ĐỒNG BỘ: BẬT",
            font_size=7,
            width=132,
            height=20,
            gold_text=True,
            renderer=self.btn_renderer,
            command=toggle_sync_mode
        )
        btn_sync.pack(side="right", padx=3, pady=2)
        update_sync_btn()

        # DANH SÁCH QUÁI (CANVAS SCROLLABLE)
        bottom = tk.Frame(body, bg="#1e1109")
        bottom.pack(side="bottom", fill="x", pady=(3, 0))
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
            width = max(170, bar.winfo_width())
            hp = max(0, monster.get("hp", 0))
            max_hp = max(1, monster.get("maxHp", 1))
            fill = min(1.0, hp / max_hp)
            player = current_data.get("player", {})
            distance = int(((monster.get("x", 0) - player.get("x", 0)) ** 2 +
                            (monster.get("y", 0) - player.get("y", 0)) ** 2) ** 0.5)
            bar_width = min(96, max(70, int(width * 0.38)))
            left, right = width - bar_width - 2, width - 2
            bar.delete("all")
            bar.create_text(2, 7, text=f"#{monster_id} · {distance}px",
                            fill=COLOR_TEXT_WHITE, font=(FONT_FAMILY, 7), anchor="w")
            bar.create_rectangle(left, 2, right, 13, fill="#140703", outline="#4a2212")
            if fill > 0:
                filled_right = left + 1 + int((bar_width - 2) * fill)
                bar.create_rectangle(left + 1, 3, filled_right, 12,
                                     fill=COLOR_HP_RED, outline="")
                bar.create_line(left + 1, 3, filled_right, 3, fill="#ff7373")
                txt_hp = f"{hp}/{max_hp}"
                txt_color = "#ffffff"
            else:
                death_time = monster.get("death_time", 0)
                respawn_ms = monster.get("respawn_ms", 5000)
                now_ms = time.time() * 1000
                rem = max(0.0, (death_time + respawn_ms - now_ms) / 1000.0) if death_time > 0 else 0.0
                if death_time > 0 and rem > 0:
                    txt_hp = f"HS {rem:.1f}s"
                    txt_color = "#ffe36e" if rem <= 1.0 else "#bfa15f"
                elif death_time > 0:
                    txt_hp = "0.0s"
                    txt_color = "#77ff77"
                else:
                    txt_hp = "0"
                    txt_color = "#777777"
            bar.create_text((left + right) / 2 + 1, 8, text=txt_hp,
                            fill="#000000", font=(FONT_FAMILY, 6, "bold"))
            bar.create_text((left + right) / 2, 7, text=txt_hp,
                            fill=txt_color, font=(FONT_FAMILY, 6, "bold"))

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
                status_var.set(f"Quét: {progress}/{limit} · {len(current)} quái")
            elif current_data.get("monster_scan_done"):
                status_var.set(f"Đã quét xong · {len(current)} quái")
            else:
                status_var.set(f"Sẵn sàng · {len(current)} quái")
            title_var.set(f"🗺️ {current_data.get('map_name', 'Map')} · Khu {area + 1}")
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
                group_expanded[name] = tk.BooleanVar(value=previous_expanded.get(name, True))
                header = tk.Frame(items, bg="#2e170d", bd=1, relief="ridge")
                header.pack(fill="x", pady=(2, 1))
                children = tk.Frame(items, bg=COLOR_INNER_BG)
                arrow = tk.Label(header, text="▼" if group_expanded[name].get() else "▶",
                                 width=2, font=(FONT_FAMILY, 7), bg="#2e170d", fg=COLOR_TEXT_GOLD)
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
                               command=toggle_group, font=(FONT_FAMILY, 7, "bold"), anchor="w",
                               bg="#2e170d", fg=COLOR_TEXT_GOLD, selectcolor=COLOR_INNER_BG,
                               activebackground="#2e170d", activeforeground=COLOR_TEXT_GOLD).pack(
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
                    row.pack(fill="x", padx=(10, 0), pady=0)
                    tk.Checkbutton(row, text="", variable=monster_vars[monster_id],
                                   command=toggle_monster, width=2, padx=0,
                                   font=(FONT_FAMILY, 7), anchor="w", bg=COLOR_INNER_BG,
                                   fg=COLOR_TEXT_WHITE, selectcolor=COLOR_INNER_BG,
                                   activebackground=COLOR_INNER_BG,
                                   activeforeground=COLOR_TEXT_WHITE).pack(side="left")
                    health_bar = tk.Canvas(row, height=15, bg=COLOR_INNER_BG,
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

        def toggle_all_groups():
            nonlocal all_groups_expanded, roster_signature
            all_groups_expanded = not all_groups_expanded
            for name in group_expanded:
                group_expanded[name].set(all_groups_expanded)
            roster_signature = None
            refresh_modal(self.state_data)

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

            other_count = self.state_data.get("other_players_count", len(self.state_data.get("players", [])))
            if len(chosen) > 0 and other_count > 0:
                players = self.state_data.get("players", [])
                p_names = ", ".join([p.get("name", "Người lạ") for p in players[:3]])
                msg = (
                    f"CẢNH BÁO AN TOÀN!\n\n"
                    f"Khu vực hiện đang có {other_count} người chơi khác ({p_names}).\n"
                    f"Kích hoạt gom {len(chosen)} quái 1-hit lúc này có thể bị người khác thấy quái chết hàng loạt và quay clip tố cáo!\n\n"
                    f"Bạn có chắc chắn muốn mạo hiểm kích hoạt Auto Fam không?"
                )
                HsoConfirmModal(
                    dlg,
                    title="CẢNH BÁO BẬT AUTO FAM",
                    message=msg,
                    on_confirm=lambda: do_save(new_profile, chosen),
                    on_cancel=None,
                    renderer=self.btn_renderer
                )
                return

            do_save(new_profile, chosen)

        def do_save(new_profile, chosen):
            farm_profiles = self.config.setdefault("auto_farm", {})
            previous_profile = farm_profiles.get(profile_key)
            if not chosen or new_profile is None:
                farm_profiles.pop(profile_key, None)
                farm_profiles.pop(f"{int(map_id)}:*", None)
                farm_profiles.pop(str(int(map_id)), None)
            else:
                farm_profiles[profile_key] = new_profile
                farm_profiles[f"{int(map_id)}:*"] = new_profile
                farm_profiles[str(int(map_id))] = new_profile
            if not save_config(self.config):
                if previous_profile is None:
                    farm_profiles.pop(profile_key, None)
                    farm_profiles.pop(f"{int(map_id)}:*", None)
                    farm_profiles.pop(str(int(map_id)), None)
                else:
                    farm_profiles[profile_key] = previous_profile
                messagebox.showerror("Auto Fam", "Không lưu được cấu hình Auto Fam.", parent=dlg)
                return
            self.farm_paused_by_safety = False
            self.sync_farm_targets(force=True)
            if chosen and new_profile is not None:
                self.log_msg(f"Đã lưu Auto Fam: {len(chosen)} quái cho map {map_id}.")
            else:
                self.log_msg(f"Đã tắt Auto Fam ở map {map_id} (chọn 0 quái).")
            close_dialog()

        HsoButton(bottom, text="Tất cả", font_size=7, width=48, height=20,
                  renderer=self.btn_renderer, command=lambda: set_all(True)).pack(side="left", padx=(0, 2))
        HsoButton(bottom, text="Bỏ chọn", font_size=7, width=48, height=20,
                  renderer=self.btn_renderer, command=lambda: set_all(False)).pack(side="left", padx=(0, 2))
        HsoButton(bottom, text="Mở/Thu", font_size=7, width=48, height=20,
                  renderer=self.btn_renderer, command=toggle_all_groups).pack(side="left")
        HsoButton(bottom, text="Đóng", font_size=7, width=44, height=20,
                  renderer=self.btn_renderer, command=close_dialog).pack(side="right")
        HsoButton(bottom, text="Lưu", font_size=7, width=44, height=20,
                  gold_text=True, renderer=self.btn_renderer, command=save_farm).pack(side="right", padx=(0, 2))
        dlg.protocol("WM_DELETE_WINDOW", close_dialog)
        self.auto_farm_refresh = refresh_modal
        refresh_modal(data)

    def refresh_mon_panel(self):
        tab = getattr(self, "active_mon_tab", 0)
        if tab == 0:
            monsters = self.state_data.get("map_monsters") or self.state_data.get("monsters", [])
            self.render_monster_grid(monsters)
        elif tab == 1:
            self.render_players_list(self.state_data.get("players", []))
        else:
            self.render_npcs_list(self.state_data.get("npcs", []))

    def render_players_list(self, players):
        c = self.canvas_mon
        c.delete("all")
        self.player_rows = []

        cw = c.winfo_width()
        if cw < 30:
            cw = 136

        if not players:
            c.create_text(
                cw // 2, 30,
                text="Không có người chơi khác",
                fill=COLOR_TEXT_MUTED,
                font=(FONT_FAMILY, 7),
                justify="center",
                anchor="center"
            )
            c.configure(scrollregion=(0, 0, cw, 60))
            return

        item_h = 24
        pad_y = 3
        y_offset = 1

        focus_id = self.state_data.get("focus_target_id", -1)
        pinned_id = self.state_data.get("pinned_player_id", -1)
        me = self.state_data.get("player", {})
        mx, my = me.get("x", 0), me.get("y", 0)
        players = sort_players_for_panel(players, me, focus_id, pinned_id)

        for pl in players:
            p_id = pl.get("id", 0)
            p_name = pl.get("name") or f"P_{p_id}"
            p_lv = pl.get("lv", 0)
            hp = max(0, pl.get("hp", 0))
            max_hp = max(1, pl.get("maxHp", 1))
            pct = min(1.0, max(0.0, float(hp) / float(max_hp)))
            px, py = pl.get("x", 0), pl.get("y", 0)
            dist = int(math.hypot(px - mx, py - my))

            is_pinned = (p_id == pinned_id)
            is_focused = (p_id == focus_id)

            # Nền hàng
            if is_pinned:
                c.create_rectangle(
                    0, y_offset, cw, y_offset + item_h + pad_y,
                    fill="#7a2a0a", outline="#ffd700", width=2
                )
            elif is_focused:
                c.create_rectangle(
                    0, y_offset, cw, y_offset + item_h + pad_y,
                    fill="#382318", outline="#c8963e", width=1
                )
            else:
                c.create_rectangle(
                    0, y_offset, cw, y_offset + item_h + pad_y,
                    fill="#180e07", outline=""
                )

            # Con trỏ ghim / focus
            if is_pinned or is_focused:
                c.create_polygon(
                    3, y_offset + 3, 3, y_offset + 11, 9, y_offset + 7,
                    fill="#ffd700" if is_pinned else "#59d8ff",
                    outline="#1b1008"
                )

            # Dòng 1: Tên người chơi
            text_x = 12 if (is_pinned or is_focused) else 4
            name_color = "#ffffff" if is_pinned else ("#9ee9ff" if is_focused else COLOR_ONLINE)
            pl_pk = pl.get("typePk", -1)
            pk_badge = " [PK]" if pl_pk == 0 else (" [Cờ]" if pl_pk > 0 else "")
            pin_badge = "📌 " if is_pinned else ""
            c.create_text(
                text_x, y_offset + 7,
                text=pin_badge + p_name[:9] + pk_badge,
                fill="#ffffff" if is_pinned else ("#ff5555" if pl_pk == 0 else name_color),
                font=(FONT_FAMILY, 8, "bold"),
                anchor="w"
            )

            # Lv / Dist
            lv_txt = f"Lv.{p_lv}" if p_lv > 0 else f"{dist}p"
            c.create_text(
                cw - 4, y_offset + 7,
                text=lv_txt,
                fill=COLOR_TEXT_GOLD,
                font=(FONT_FAMILY, 7),
                anchor="e"
            )

            # Dòng 2: Thanh máu đỏ mini
            bar_x1 = 4
            bar_x2 = cw - 4
            bar_w = bar_x2 - bar_x1
            bar_y1 = y_offset + 14
            bar_y2 = y_offset + 22

            c.create_rectangle(bar_x1, bar_y1, bar_x2, bar_y2, fill="#1c0b05", outline="#5a3118")
            fill_w = int((bar_w - 2) * pct)
            if fill_w > 0:
                c.create_rectangle(bar_x1 + 1, bar_y1 + 1, bar_x1 + 1 + fill_w, bar_y2 - 1, fill=COLOR_HP_RED, outline="")
                c.create_rectangle(bar_x1 + 1, bar_y1 + 1, bar_x1 + 1 + fill_w, bar_y1 + 2, fill="#ff7a7a", outline="")

            hp_str = f"{hp}/{max_hp}"
            c.create_text(
                (bar_x1 + bar_x2) // 2, (bar_y1 + bar_y2) // 2,
                text=hp_str, fill="#ffffff", font=(FONT_FAMILY, 7), anchor="center"
            )

            self.player_rows.append((y_offset, y_offset + item_h + pad_y, p_id, p_name))
            y_offset += item_h + pad_y

        c.configure(scrollregion=(0, 0, cw, y_offset + 4))

    def render_npcs_list(self, npcs):
        c = self.canvas_mon
        c.delete("all")
        self.npc_rows = []

        cw = c.winfo_width()
        if cw < 30:
            cw = 136

        if not npcs:
            c.create_text(
                cw // 2, 30,
                text="Không có NPC nào trong map",
                fill=COLOR_TEXT_MUTED,
                font=(FONT_FAMILY, 7),
                justify="center",
                anchor="center"
            )
            c.configure(scrollregion=(0, 0, cw, 60))
            return

        item_h = 22
        pad_y = 2
        y_offset = 1

        me = self.state_data.get("player", {})
        mx, my = me.get("x", 0), me.get("y", 0)
        # Sắp xếp NPC theo khoảng cách gần nhất
        npcs_sorted = sorted(npcs, key=lambda n: math.hypot(n.get("x", 0) - mx, n.get("y", 0) - my))

        for npc in npcs_sorted:
            n_id = npc.get("id", 0)
            n_name = npc.get("name", "NPC")
            nx, ny = npc.get("x", 0), npc.get("y", 0)
            dist = int(math.hypot(nx - mx, ny - my))

            # Nền hàng
            c.create_rectangle(
                2, y_offset, cw - 2, y_offset + item_h,
                fill="#161009", outline="#3d2617", width=1
            )
            # Biểu tượng NPC cyan nhỏ
            c.create_rectangle(
                6, y_offset + 6, 14, y_offset + 14,
                fill="#0abde3", outline="#ffffff"
            )
            # Tên NPC
            txt_name = f"{n_name}"
            if len(txt_name) > 13:
                txt_name = txt_name[:13] + ".."
            c.create_text(
                18, y_offset + item_h // 2,
                text=txt_name,
                fill="#e8d5b5",
                font=(FONT_FAMILY, 7, "bold"),
                anchor="w"
            )
            # Cự ly
            c.create_text(
                cw - 6, y_offset + item_h // 2,
                text=f"{dist}px",
                fill="#8f8073",
                font=(FONT_FAMILY, 6),
                anchor="e"
            )

            self.npc_rows.append((y_offset, y_offset + item_h + pad_y, npc))
            y_offset += item_h + pad_y

        c.configure(scrollregion=(0, 0, cw, max(y_offset + 4, 60)))

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
                font=(FONT_FAMILY, 8),
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

            death_time = m.get("death_time", 0)
            respawn_ms = m.get("respawn_ms", 5000)
            now_ms = time.time() * 1000
            rem = max(0.0, (death_time + respawn_ms - now_ms) / 1000.0) if death_time > 0 else 0.0
            if hp <= 0 and death_time > 0 and rem > 0:
                hp_str = f"HS {rem:.1f}s"
            elif hp <= 0 and death_time > 0:
                hp_str = "HS 0.0s"
            else:
                hp_str = f"{hp}/{max_hp}"
            mid_x = bx1 + bar_w // 2
            c.create_text(mid_x, mid_y, text=hp_str, fill="#ffffff", font=(FONT_FAMILY, 7), anchor="center")

            self.monster_rows.append((y_offset, y_offset + item_h + pad_y, m_id))
            y_offset += item_h + pad_y

        c.configure(scrollregion=(0, 0, cw, y_offset + 4))

    def render_monster_grid(self, monsters):
        c = self.canvas_mon
        c.delete("all")
        self.monster_grid_cells = []

        cw = c.winfo_width()
        if cw < 30:
            cw = 152

        if not monsters:
            c.create_text(
                cw // 2, 30,
                text="Chưa có dữ liệu quái...",
                fill=COLOR_TEXT_MUTED,
                font=(FONT_FAMILY, 7),
                justify="center",
                anchor="center"
            )
            c.configure(scrollregion=(0, 0, cw, 60))
            return

        pad_x = 2
        gap_x = 2
        col_w = max(68, (cw - pad_x * 2 - gap_x) // 2)
        cell_h = 12
        pad_y = 1
        bar_w = 34
        bar_h = 8
        y_offset = 1

        focus_id = self.state_data.get("focus_monster_id", -1)
        pinned_id = self.state_data.get("pinned_monster_id", -1)
        data = self.state_data
        profile = get_map_farm_profile(self.config, data.get("map_id", 0), data.get("area", 0))

        # Sắp xếp theo ID tăng dần để vị trí các quái cố định, dễ quan sát khi autofam
        seen = set()
        unique_monsters = []
        for m in sorted(monsters, key=lambda x: x.get("id", 0)):
            m_id = m.get("id", 0)
            if m_id not in seen:
                seen.add(m_id)
                unique_monsters.append(m)
        monsters = unique_monsters

        now_ms = time.time() * 1000

        for idx, m in enumerate(monsters):
            m_id = m.get("id", 0)
            m_name = m.get("name", "Mob")
            hp = max(0, m.get("hp", 0))
            max_hp = max(1, m.get("maxHp", 1))
            pct = min(1.0, max(0.0, float(hp) / float(max_hp)))
            is_dead = (hp <= 0)

            col = idx % 2
            row = idx // 2
            cell_x1 = pad_x if col == 0 else (pad_x + col_w + gap_x)
            cell_x2 = cell_x1 + col_w
            cell_y1 = y_offset + row * (cell_h + pad_y)
            cell_y2 = cell_y1 + cell_h
            mid_y = (cell_y1 + cell_y2) // 2

            is_pinned = (m_id == pinned_id)
            is_focused = (m_id == focus_id)
            selected = farm_profile_matches(profile, m)

            # Nền ô
            if is_pinned:
                c.create_rectangle(cell_x1, cell_y1, cell_x2, cell_y2, fill="#4a280e", outline="#ffd700", width=1)
            elif is_focused:
                c.create_rectangle(cell_x1, cell_y1, cell_x2, cell_y2, fill="#281b10", outline="#59d8ff", width=1)
            elif is_dead:
                c.create_rectangle(cell_x1, cell_y1, cell_x2, cell_y2, fill="#121212", outline="#222222", width=1)
            elif selected:
                c.create_rectangle(cell_x1, cell_y1, cell_x2, cell_y2, fill="#1c0e07", outline="#4a2c14", width=1)
            else:
                c.create_rectangle(cell_x1, cell_y1, cell_x2, cell_y2, fill="#160c06", outline="#2a1408", width=1)

            # Tên quái siêu ngắn gọn
            short_name = compact_mob_name(m_name)
            txt_name = f"#{m_id} {short_name}"
            if len(txt_name) > 8:
                txt_name = txt_name[:8]

            if is_pinned:
                name_color = "#ffd700"
            elif is_focused:
                name_color = "#9ee9ff"
            elif is_dead:
                name_color = "#555555"
            else:
                name_color = "#e8d5b5"

            c.create_text(
                cell_x1 + 3, mid_y,
                text=txt_name,
                fill=name_color,
                font=(FONT_FAMILY, 7),
                anchor="w"
            )

            # Thanh máu mini
            bx1 = cell_x2 - bar_w - 2
            bx2 = cell_x2 - 2
            by1 = mid_y - bar_h // 2
            by2 = by1 + bar_h

            if is_dead:
                c.create_rectangle(bx1, by1, bx2, by2, fill="#1e1e1e", outline="#333333")
                death_time = m.get("death_time", 0)
                respawn_ms = m.get("respawn_ms", 5000)
                rem = max(0.0, (death_time + respawn_ms - now_ms) / 1000.0) if death_time > 0 else 0.0
                if death_time > 0 and rem > 0:
                    hp_txt = f"{rem:.1f}s"
                    txt_fg = "#ffe36e" if rem <= 1.0 else "#bfa15f"
                elif death_time > 0:
                    hp_txt = "0.0s"
                    txt_fg = "#77ff77"
                else:
                    hp_txt = "0"
                    txt_fg = "#777777"
                c.create_text((bx1 + bx2) // 2, mid_y, text=hp_txt, fill=txt_fg, font=(FONT_FAMILY, 6, "bold"), anchor="center")
            else:
                c.create_rectangle(bx1, by1, bx2, by2, fill="#1c0b05", outline="#4a2211")
                fill_w = int((bar_w - 2) * pct)
                if fill_w > 0:
                    c.create_rectangle(bx1 + 1, by1 + 1, bx1 + 1 + fill_w, by2 - 1, fill=COLOR_HP_RED, outline="")
                    c.create_rectangle(bx1 + 1, by1 + 1, bx1 + 1 + fill_w, by1 + 2, fill="#ff7a7a", outline="")
                hp_txt = format_mini_hp(hp)
                c.create_text((bx1 + bx2) // 2, mid_y, text=hp_txt, fill="#ffffff", font=(FONT_FAMILY, 6), anchor="center")

            self.monster_grid_cells.append((cell_x1, cell_y1, cell_x2, cell_y2, m_id))

        total_rows = (len(monsters) + 1) // 2
        total_h = y_offset + total_rows * (cell_h + pad_y) + 4
        c.configure(scrollregion=(0, 0, cw, max(total_h, 60)))

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
            width=20,
            bg=COLOR_INNER_BG,
            fg=COLOR_TEXT_WHITE,
            bd=0,
            highlightthickness=0,
            font=(FONT_FAMILY, 8),
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
        self.radar_canvas.bind("<Motion>", self.on_radar_hover)
        self.radar_canvas.bind("<Leave>", self.on_radar_leave)

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
            "-Dsun.awt.im.active=false",
            "-Dsun.java2d.uiScale=1.0",
            "-Dawt.useSystemAAFontSettings=lcd",
            "-Dswing.aatext=true",
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
                self.game_proc.wait(timeout=2)
            except Exception:
                try:
                    self.game_proc.kill()
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
                    f.write(f'"{sf.as_posix()}"\n')

            cp = str(MICROEMU_JAR)
            cmd = [
                JAVAC_EXEC,
                "-encoding", "UTF-8",
                "-source", "8",
                "-target", "8",
                "-cp", cp,
                "-d", str(bin_dir),
                f"@{sources_arg_file}"
            ]
            self.log_msg(f"Compiling {len(src_files)} Java files...")
            subprocess.run(cmd, cwd=str(BASE_DIR), check=True)
            subprocess.run([JAR_EXEC, "uf", str(JAR_TARGET), "-C", str(bin_dir), "."], cwd=str(BASE_DIR), check=True)
            self.log_msg(f"Compiled {len(src_files)} Java files & updated JAR!")
            messagebox.showinfo("Success", f"Successfully compiled {len(src_files)} Java sources and repacked JAR!")
        except Exception as e:
            self.log_msg(f"Repack failed: {e}")
            messagebox.showerror("Error", f"Repack error:\n{e}")

    def update_microemulator_config(self, w, h):
        cfg_dir = Path.home() / ".microemulator"
        cfg_dir.mkdir(parents=True, exist_ok=True)
        jar_url = (BASE_DIR / "KnightOnline_v403.jar").resolve().as_uri()
        xml = f"""<config>
	<files>
		<recent maxCapacity="10">
			<midlet>
				<name>KnightOnline Midlet Suite</name>
				<url>{jar_url}</url>
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
        self.last_map_id = None
        self.farm_paused_by_safety = False
        self.config["auto_attack"] = False
        self.cached_player = {}
        if hasattr(self, "pk_timer_id") and self.pk_timer_id is not None:
            try:
                self.root.after_cancel(self.pk_timer_id)
            except Exception:
                pass
            self.pk_timer_id = None
        self.pk_cooldown_end = 0.0

        self.btn_auto_attack.set_selected(False)
        self.btn_auto_attack.configure(gold_text=False)
        self.btn_auto_attack.set_text("Auto Attack: OFF")
        self.lbl_in_game_status.configure(text="Waiting for game", fg=COLOR_TEXT_MUTED)
        self.lbl_char_name.configure(text="Offline")
        self.lbl_char_lv.configure(text="")
        self.lbl_hp_val.configure(text="--/--")
        self.lbl_mp_val.configure(text="--/--")
        self.lbl_map_name.configure(text="--")
        self.lbl_zone_val.configure(text="Zone: --")
        self.btn_zone_select.set_text("--")
        if hasattr(self, "btn_auto_farm_opt"):
            self.btn_auto_farm_opt.set_selected(False)
            self.btn_auto_farm_opt.configure(gold_text=False)
        self.btn_unpin.set_disabled(True)
        self.last_logged_pin_player = -1
        self.last_logged_pin_monster = -1
        if hasattr(self, "btn_pk"):
            self.btn_pk.set_text("Đồ Sát: TẮT")
            self.btn_pk.set_selected(False)
            self.btn_pk.configure(gold_text=False)
            self.btn_pk.set_disabled(False)
        if hasattr(self, "p_mon"):
            self.p_mon.set_tab_title(0, "MON (0)")
            self.p_mon.set_tab_title(1, "PL (0)")
            self.p_mon.set_tab_title(2, "NPC (0)")
        self.redraw_player_bars()
        self.refresh_mon_panel()
        self.redraw_radar()
        if hasattr(self, "marquee_top"):
            self.marquee_top.set_message("⚡ KNIGHT ONLINE - HSO TOOL • CHỜ KẾT NỐI VÀO GAME • SẴN SÀNG HỖ TRỢ ⚡", fg=COLOR_TEXT_MUTED, is_alert=False)
        if hasattr(self, "marquee_bottom"):
            self.marquee_bottom.set_message("⚡ HỆ THỐNG AN TOÀN ĐANG TRỰC CHIẾN • TỰ ĐỘNG PHÁT HIỆN BOT & NGƯỜI LẠ ⚡", fg=COLOR_TEXT_MUTED, is_alert=False)

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
                tmp_file = STATE_FILE.with_name(STATE_FILE.name + ".tmp")
                target_to_read = None
                threshold_ns = getattr(self, "game_started_ns", 0) - 2_000_000_000
                if STATE_FILE.exists() and STATE_FILE.stat().st_mtime_ns >= threshold_ns:
                    target_to_read = STATE_FILE
                if tmp_file.exists() and tmp_file.stat().st_mtime_ns >= threshold_ns:
                    if target_to_read is None or tmp_file.stat().st_mtime_ns >= target_to_read.stat().st_mtime_ns:
                        target_to_read = tmp_file

                if target_to_read:
                    with open(target_to_read, "r", encoding="utf-8") as f:
                        data = json.load(f)
                    self.update_live_data(data)
            except (OSError, ValueError):
                pass

        self.root.after(200, self.poll_ipc_loop)

    def countdown_tick_loop(self):
        try:
            if self.game_proc and getattr(self, "state_data", None):
                monsters = self.state_data.get("map_monsters") or self.state_data.get("monsters", [])
                has_dead = any(m.get("hp", 0) <= 0 and m.get("death_time", 0) > 0 for m in monsters)
                if has_dead:
                    if getattr(self, "active_mon_tab", 0) == 0:
                        self.refresh_mon_panel()
                    if getattr(self, "auto_farm_refresh", None):
                        try:
                            self.auto_farm_refresh(self.state_data)
                        except Exception:
                            pass
        except Exception:
            pass
        self.root.after(100, self.countdown_tick_loop)

    def update_live_data(self, data):
        self.state_data = data
        in_game = data.get("in_game", False)

        current_pin = data.get("pinned_monster_id", -1)
        current_pin_pl = data.get("pinned_player_id", -1)
        has_pin = (current_pin >= 0 or current_pin_pl != -1)
        self.btn_unpin.set_disabled(not has_pin)
        my_pk = data.get("player", {}).get("typePk", -1)
        if hasattr(self, "btn_pk"):
            is_pk = (my_pk == 0)
            if self.pk_cooldown_end and time.monotonic() < self.pk_cooldown_end:
                # Đang trong thời gian đếm ngược 180s: giữ nguyên đếm ngược và disabled
                self.btn_pk.set_selected(is_pk)
                self.btn_pk.configure(gold_text=is_pk)
            else:
                if is_pk:
                    self.btn_pk.set_text("Đồ Sát: BẬT")
                    self.btn_pk.set_selected(True)
                    self.btn_pk.configure(gold_text=True)
                else:
                    self.btn_pk.set_text("Đồ Sát: TẮT")
                    self.btn_pk.set_selected(False)
                    self.btn_pk.configure(gold_text=False)
                self.btn_pk.set_disabled(False)

        if in_game:
            last_m = getattr(self, "last_logged_pin_monster", -1)
            if current_pin != last_m:
                self.last_logged_pin_monster = current_pin
                if current_pin >= 0:
                    self.log_msg(f"Đã ghim quái ID {current_pin}.")
                elif last_m >= 0 and current_pin_pl == -1:
                    self.log_msg("Đã bỏ ghim mục tiêu.")

            last_pl = getattr(self, "last_logged_pin_player", -1)
            if current_pin_pl != last_pl:
                self.last_logged_pin_player = current_pin_pl
                if current_pin_pl != -1:
                    self.log_msg(f"📌 Đã ghim người chơi ID {current_pin_pl}.")
                elif last_pl != -1 and current_pin < 0:
                    self.log_msg("Đã bỏ ghim người chơi.")

        if self.pending_zone_change:
            target, old_area, started = self.pending_zone_change
            area = data.get("area") if in_game else None
            if area is not None and (area == target if target is not None else area != old_area):
                self.log_msg(f"Đã đổi sang Zone {area + 1}.")
                self.pending_zone_change = None
            elif time.monotonic() - started > 8:
                self.log_msg("Chưa nhận được xác nhận đổi khu từ game.")
                self.pending_zone_change = None

        other_count = data.get("other_players_count", len(data.get("players", [])))
        players = data.get("players", [])
        bot_detected = bool(data.get("bot_detected", False))

        if in_game:
            map_id = data.get("map_id", 0)
            area = data.get("area", 0)
            saved_profile = get_map_farm_profile(self.config, map_id, area)
            map_changed = (self.last_map_id is not None and self.last_map_id != map_id)
            prev_map_id = self.last_map_id
            self.last_map_id = map_id

            # TRƯỜNG HỢP 1: Chuyển sang map khác (hoặc quay lại map đã cấu hình fam)
            if map_changed:
                if saved_profile is None:
                    # Ra map khác chưa có cấu hình fam -> Tắt fam trên map mới
                    self.farm_paused_by_safety = False
                    self.log_msg(f"Đã sang map #{map_id}: Chưa cấu hình Auto Fam cho map này (tạm tắt fam).")
                else:
                    # Vào lại map đã có cấu hình fam!
                    if other_count == 0:
                        # An toàn (0 người) -> Tự động bật lại Auto Fam & Auto Attack!
                        if not self.config.get("auto_attack", False):
                            self.config["auto_attack"] = True
                            save_config(self.config)
                            try:
                                self.write_command({"action": "auto_attack", "enabled": True})
                            except OSError:
                                pass
                            self.btn_auto_attack.set_selected(True)
                            self.btn_auto_attack.configure(gold_text=True)
                            self.btn_auto_attack.set_text("Auto Attack: ON")
                        self.farm_paused_by_safety = False
                        self.log_msg(f"✅ Trở lại map #{map_id} (0 người): Tự động kích hoạt lại Auto Fam & Auto Attack!")
                    else:
                        # Có người -> Vẫn lưu cấu hình fam nhưng TẮT fam, TẮT luôn Auto Attack!
                        if self.config.get("auto_attack", False):
                            self.config["auto_attack"] = False
                            save_config(self.config)
                            try:
                                self.write_command({"action": "auto_attack", "enabled": False})
                            except OSError:
                                pass
                            self.btn_auto_attack.set_selected(False)
                            self.btn_auto_attack.configure(gold_text=False)
                            self.btn_auto_attack.set_text("Auto Attack: OFF")
                        self.farm_paused_by_safety = True
                        p_names = ", ".join([p.get("name", "Người lạ") for p in players[:3]])
                        self.log_msg(f"🚨 [AN TOÀN] Trở lại map #{map_id} nhưng có {other_count} người ({p_names}): TẮT Auto Fam & TẮT Auto Attack (vẫn giữ cấu hình fam).")

            # TRƯỜNG HỢP 2: Đang ở trong map có cấu hình farm, trạng thái an toàn / người chơi thay đổi
            elif saved_profile is not None:
                if other_count > 0 and not self.farm_paused_by_safety:
                    # Có người chơi bước vào khu -> Tắt fam, tắt luôn auto atk (vẫn giữ cấu hình)
                    self.farm_paused_by_safety = True
                    if self.config.get("auto_attack", False):
                        self.config["auto_attack"] = False
                        save_config(self.config)
                        try:
                            self.write_command({"action": "auto_attack", "enabled": False})
                        except OSError:
                            pass
                        self.btn_auto_attack.set_selected(False)
                        self.btn_auto_attack.configure(gold_text=False)
                        self.btn_auto_attack.set_text("Auto Attack: OFF")
                    p_names = ", ".join([p.get("name", "Người lạ") for p in players[:3]])
                    self.log_msg(f"🚨 [AN TOÀN] Có {other_count} người ({p_names}) vào khu! ĐÃ TẮT Auto Fam & TẮT Auto Attack để bảo vệ nick (cấu hình fam vẫn lưu).")

                elif other_count == 0 and self.farm_paused_by_safety:
                    # Người chơi đã rời đi, khu trở lại an toàn -> Bật lại fam & auto atk
                    self.farm_paused_by_safety = False
                    if not self.config.get("auto_attack", False):
                        self.config["auto_attack"] = True
                        save_config(self.config)
                        try:
                            self.write_command({"action": "auto_attack", "enabled": True})
                        except OSError:
                            pass
                        self.btn_auto_attack.set_selected(True)
                        self.btn_auto_attack.configure(gold_text=True)
                        self.btn_auto_attack.set_text("Auto Attack: ON")
                    self.log_msg("✅ [AN TOÀN] Khu vực an toàn (0 người). Tự động bật lại Auto Fam & Auto Attack!")

            self.sync_farm_targets(data)
            farm_mode = data.get("farm_mode", "off")
            if farm_mode == "normal" and self.last_farm_mode != "normal":
                self.log_msg("Auto Fam: chỉ gửi đánh thường (skill 0) tới các quái đã chọn.")
            self.last_farm_mode = farm_mode
            farm_damage_results = data.get("farm_damage_results", 0)
            if farm_damage_results > self.last_farm_damage_results:
                hits = data.get('farm_distinct_hits', 0)
                self.log_msg(f"Auto Fam: 1-hit đã đánh trúng {hits} quái ({farm_damage_results} lượt ST). Đón đầu nhịp hồi sinh.")
                self.last_farm_damage_results = farm_damage_results
            elif farm_damage_results == 0:
                self.last_farm_damage_results = 0
            if self.auto_farm_refresh is not None:
                self.auto_farm_refresh(data)
            self.lbl_in_game_status.configure(text="IN GAME", fg=COLOR_ONLINE)

            p = data.get("player")
            if p and p.get("name") and p.get("name") != "unname":
                self.cached_player = dict(p)
            elif getattr(self, "cached_player", None):
                p = self.cached_player
                data["player"] = p
            else:
                p = p or {}

            p_name = p.get('name', '--')
            p_lv = p.get('lv', '--')
            self.lbl_char_name.configure(text=p_name)
            self.lbl_char_lv.configure(text=f"(cấp {p_lv})" if p_lv != '--' else "")

            p_hp = p.get('hp', 0)
            p_max_hp = p.get('maxHp', 0)
            p_mp = p.get('mp', 0)
            p_max_mp = p.get('maxMp') or p.get('maxmp', 0)

            self.lbl_hp_val.configure(text=f"{p_hp}/{p_max_hp}")
            self.lbl_mp_val.configure(text=f"{p_mp}/{p_max_mp}")

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

            if "auto_pickup" in data and hasattr(self, "btn_auto_pickup"):
                is_pkp = bool(data["auto_pickup"])
                if is_pkp != self.config.get("auto_pickup", False):
                    self.config["auto_pickup"] = is_pkp
                    self.btn_auto_pickup.set_selected(is_pkp)
                    self.btn_auto_pickup.configure(gold_text=is_pkp)
                    self.btn_auto_pickup.set_text(f"Auto Loot: {'ON' if is_pkp else 'OFF'}")
                    if hasattr(self, "btn_auto_loot_opt"):
                        self.btn_auto_loot_opt.set_selected(is_pkp)
                        self.btn_auto_loot_opt.configure(gold_text=is_pkp)

            if "auto_sell" in data and hasattr(self, "btn_auto_sell"):
                is_as = bool(data["auto_sell"])
                if is_as != self.config.get("auto_sell", False):
                    self.config["auto_sell"] = is_as
                    self.btn_auto_sell.set_selected(is_as)
                    self.btn_auto_sell.configure(gold_text=is_as)
                    self.btn_auto_sell.set_text(f"Auto Sell: {'ON' if is_as else 'OFF'}")
                    if hasattr(self, "btn_auto_sell_opt"):
                        self.btn_auto_sell_opt.set_selected(is_as)
                        self.btn_auto_sell_opt.configure(gold_text=is_as)

            if "speed" in data and hasattr(self, "update_speed_ui"):
                try:
                    live_spd = f"{float(data['speed']):.1f}"
                    if live_spd in SPEED_LEVELS and live_spd != str(self.config.get("speed", "1.0")):
                        self.config["speed"] = live_spd
                        self.update_speed_ui()
                except Exception:
                    pass

            p_count = data.get("players_count", len(players))
            npc_count = len(data.get("npcs", []))
            if hasattr(self, "p_mon"):
                m_all = data.get("map_monsters") or data.get("monsters", [])
                self.p_mon.set_tab_title(0, f"MON ({len(m_all)})")
                self.p_mon.set_tab_title(1, f"PL ({p_count})")
                self.p_mon.set_tab_title(2, f"NPC ({npc_count})")

            self.refresh_mon_panel()
            self.redraw_radar()
        else:
            self.lbl_in_game_status.configure(text="Login Screen", fg=COLOR_TEXT_MUTED)
            self.lbl_char_name.configure(text="Offline")
            self.lbl_char_lv.configure(text="")
            self.lbl_hp_val.configure(text="--/--")
            self.lbl_mp_val.configure(text="--/--")
            if hasattr(self, "p_char"):
                self.p_char.set_title("Offline")
            if hasattr(self, "redraw_player_bars"):
                self.redraw_player_bars()

        # CẬP NHẬT CHỮ CHẠY NGANG (MARQUEE) TRÊN VÀ DƯỚI
        if hasattr(self, "marquee_top") and hasattr(self, "marquee_bottom"):
            if not in_game:
                top_msg = "⚡ KNIGHT ONLINE - HSO TOOL • CHỜ KẾT NỐI VÀO GAME • SẴN SÀNG HỖ TRỢ ⚡"
                bot_msg = "⚡ HỆ THỐNG AN TOÀN ĐANG TRỰC CHIẾN • TỰ ĐỘNG PHÁT HIỆN BOT & NGƯỜI LẠ ⚡"
                self.marquee_top.set_message(top_msg, fg=COLOR_TEXT_MUTED, is_alert=False)
                self.marquee_bottom.set_message(bot_msg, fg=COLOR_TEXT_MUTED, is_alert=False)
            elif bot_detected:
                top_msg = "🚨 CẢNH BÁO BOT XUẤT HIỆN: PHÁT HIỆN CAPTCHA BOT TRONG MAP! VUI LÒNG KIỂM TRA MÀN HÌNH GAME NGAY ĐỂ TRÁNH BỊ KHÓA ACC! 🚨"
                bot_msg = "⚠️ BOT CAPTCHA ĐANG TỒN TẠI TRONG MAP • ĐÃ TẠM DỪNG GOM QUÁI • TRẢ LỜI CAPTCHA TRÊN MÀN HÌNH GAME ĐỂ AN TOÀN! ⚠️"
                self.marquee_top.set_message(top_msg, fg="#ff3b30", is_alert=True)
                self.marquee_bottom.set_message(bot_msg, fg="#ff3b30", is_alert=True)
            elif other_count > 0:
                top_msg = f"⚠️ CẢNH BÁO AN TOÀN: CÓ {other_count} NGƯỜI CHƠI TRONG KHU! AUTO FAM GOM QUÁI ĐÃ TẮT • CHỈ ĐÁNH THƯỜNG TỪNG CON! ⚠️"
                bot_msg = f"🛡️ CHẾ ĐỘ BẢO VỆ: Đang có {other_count} người trong khu • Đang đánh từng con bình thường để tránh bị soi acc 🛡️"
                self.marquee_top.set_message(top_msg, fg="#ff9500", is_alert=True)
                self.marquee_bottom.set_message(bot_msg, fg="#ff9500", is_alert=False)
            else:
                map_name = data.get("map_name", "Map")
                area_num = data.get("area", 0) + 1
                top_msg = f"✨ KHU VỰC AN TOÀN: 0 người chơi khác • Không có Bot • {map_name} (Khu {area_num}) • SẴN SÀNG AUTO FAM 1-HIT ✨"
                bot_msg = "⚔️ KNIGHT ONLINE AUTO PRO • Tự động gom quái toàn map • Tự ngắt tức thì khi có người lạ vào khu ⚔️"
                self.marquee_top.set_message(top_msg, fg="#ffd700", is_alert=False)
                self.marquee_bottom.set_message(bot_msg, fg="#ffd700", is_alert=False)

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

        profile = get_map_farm_profile(self.config, data.get("map_id", 0), data.get("area", 0))
        pinned_id = data.get("pinned_monster_id", -1)
        focus_id = data.get("focus_monster_id", -1)
        now_ms = time.time() * 1000
        self.radar_hover_items = []

        for monster in monsters:
            x, y = position(monster)
            hp = monster.get("hp", 0)
            is_dead = (hp <= 0)
            focused = monster.get("id") in (pinned_id, focus_id)
            if is_dead:
                # Quái chết: màu xám
                c.create_oval(x - 2, y - 2, x + 2, y + 2,
                              fill="#555555", outline="")
            elif focused:
                # Quái đang ghim hoặc focus: chấm đỏ với viền trắng tròn bao quanh nổi bật (không tô vàng)
                c.create_oval(x - 4, y - 4, x + 4, y + 4,
                              outline="#ffffff", width=1.5)
                c.create_oval(x - 2, y - 2, x + 2, y + 2,
                              fill=COLOR_HP_RED, outline="")
            else:
                # Quái sống: luôn là màu đỏ thuần
                c.create_oval(x - 2, y - 2, x + 2, y + 2,
                              fill=COLOR_HP_RED, outline="")
            m_name = monster.get("name", "Quái")
            m_id = monster.get("id", 0)
            self.radar_hover_items.append((x, y, f"[Quái] #{m_id} {m_name} ({hp} HP)", COLOR_HP_RED))

        for waypoint in data.get("waypoints", []):
            x, y = position(waypoint)
            c.create_polygon(x, y - 5, x + 4, y, x, y + 5, x - 4, y,
                             fill="#b58cff", outline="#ffffff")
            wp_name = waypoint.get("name", "Cổng")
            self.radar_hover_items.append((x, y, f"[Cổng] {wp_name}", "#b58cff"))

        for npc in data.get("npcs", []):
            kind = classify_radar_npc(npc.get("name"))
            x, y = position(npc)
            npc_name = npc.get("name", "NPC")
            if kind == "zone":
                c.create_polygon(x, y - 6, x + 5, y + 4, x - 5, y + 4,
                                 fill="#ff9b45", outline="#ffffff")
                self.radar_hover_items.append((x, y, f"[Khu] {npc_name}", "#ff9b45"))
            elif kind == "stone":
                c.create_polygon(x, y - 6, x + 5, y, x, y + 6, x - 5, y,
                                 fill="#60dfff", outline="#ffffff")
                self.radar_hover_items.append((x, y, f"[Đá] {npc_name}", "#60dfff"))
            else:
                # NPC nhân vật / chức năng (Aman, Lisa, Hammer, Doubar, Zoro, v.v.)
                c.create_rectangle(x - 3, y - 3, x + 3, y + 3,
                                   fill="#0abde3", outline="#ffffff")
                self.radar_hover_items.append((x, y, f"[NPC] {npc_name}", "#0abde3"))

        pinned_pl_id = data.get("pinned_player_id", -1)
        for other in data.get("players", []):
            x, y = position(other)
            is_pinned_pl = (other.get("id") == pinned_pl_id)
            if is_pinned_pl:
                c.create_oval(x - 6, y - 6, x + 6, y + 6, outline="#ff3333", width=2)
                c.create_oval(x - 3, y - 3, x + 3, y + 3, fill="#ff4d4d", outline="#ffffff")
            else:
                c.create_oval(x - 3, y - 3, x + 3, y + 3,
                              fill="#ffd700", outline="#ffffff")
            p_name = other.get("name", "Người chơi")
            p_lv = other.get("lv", 0)
            self.radar_hover_items.append((x, y, f"[Người] {p_name} (Lv {p_lv})", "#ffd700"))

        px, py = position(player)
        c.create_oval(px - 4, py - 4, px + 4, py + 4,
                      fill=COLOR_ONLINE, outline="#ffffff", width=1)
        my_name = player.get("name", "Tôi")
        my_lv = player.get("lv", 0)
        self.radar_hover_items.append((px, py, f"[Bạn] {my_name} (Lv {my_lv})", COLOR_ONLINE))

        legend_items = [
            ("● Quái", COLOR_HP_RED),
            ("● Chết", "#666666"),
            ("◆ Cổng", "#b58cff"),
            ("▲ Khu", "#ff9b45"),
            ("◆ Đá", "#60dfff"),
            ("■ NPC", "#0abde3"),
            ("● Người", "#ffd700"),
            ("● Bạn", COLOR_ONLINE)
        ]
        ly = max(bottom + 4, h - 11)
        pad = 12
        n_items = len(legend_items)
        step = (w - 2 * pad) / max(1, n_items - 1)
        for idx, (label, color) in enumerate(legend_items):
            lx = pad + idx * step
            c.create_text(lx, ly, text=label, fill=color,
                          font=(FONT_FAMILY, 6, "bold"), anchor="center")

    def on_radar_hover(self, event):
        c = self.radar_canvas
        c.delete("radar_tooltip")
        if not getattr(self, "radar_hover_items", None):
            return
        mx, my = event.x, event.y
        closest = None
        min_dist = 12.0
        for x, y, text, color in self.radar_hover_items:
            dist = math.hypot(x - mx, y - my)
            if dist < min_dist:
                min_dist = dist
                closest = (x, y, text, color)
        if closest:
            x, y, text, color = closest
            # Vẽ vòng tròn highlight quanh điểm được rê chuột
            c.create_oval(x - 6, y - 6, x + 6, y + 6, outline=color, width=1.5, tags="radar_tooltip")
            w = c.winfo_width()
            box_w = max(50, len(text) * 5 + 12)
            tx = max(4, min(w - box_w - 4, x + 8))
            ty = max(4, y - 14)
            # Nền chữ tooltip
            c.create_rectangle(tx, ty, tx + box_w, ty + 14,
                               fill="#160c06", outline=color, width=1, tags="radar_tooltip")
            c.create_text(tx + 4, ty + 7, text=text, fill="#ffffff",
                          font=(FONT_FAMILY, 6, "bold"), anchor="w", tags="radar_tooltip")

    def on_radar_leave(self, event):
        self.radar_canvas.delete("radar_tooltip")


def main():
    root = tk.Tk()
    app = HsoStudioApp(root)
    root.mainloop()


if __name__ == "__main__":
    main()
