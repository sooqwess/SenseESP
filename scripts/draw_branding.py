#!/usr/bin/env python3
"""
Procedural pixel-art branding generator for SenseESP.

Draws a native 128x128 pixel-art composition (upscaled 4x with NEAREST to 512x512):
- Hybrid tactical scanner frame combining beveled dark obsidian-slate pixel tiles,
  crisp stepped white L-corners, emerald inner HUD hairline, outer telemetry dashes,
  and corner reticle crosses.
- Hand-crafted 22x16 All-Seeing Emerald Eye sprite (upscaled 3x = 66x48) with
  layered jade/emerald iris shading, sleek vertical slit pupil, and crisp mint glint.
- Crisp beveled gold ESP corner brackets with tight 1px dark jade rim, leaving
  generous dark emerald space around the sprite.
"""

from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter
import math

ROOT = Path(__file__).resolve().parents[1]
OUT_512 = ROOT / "assets" / "icon.png"
OUT_BANNER = ROOT / "assets" / "banner.png"
OUT_SHOWCASE = ROOT / "assets" / "hud_showcase.png"
OUT_128 = ROOT / "common" / "src" / "main" / "resources" / "assets" / "espmod" / "icon.png"


def make_background(w: int, h: int) -> Image.Image:
    im = Image.new("RGBA", (w, h))
    p = im.load()
    for y in range(h):
        for x in range(w):
            if x < 11 or x >= w - 11 or y < 11 or y >= h - 11:
                bx, by = x // 2, y // 2
                mod = ((bx * 5 + by * 3) % 4) - 1
                p[x, y] = (10 + mod, 15 + mod * 2, 20 + mod * 2, 255)
            else:
                r = math.hypot((x - w * 0.50) / (w * 0.52), (y - h * 0.50) / (h * 0.52))
                v = max(0, 6 - int(r * 6))
                dither = 1 if ((x + y) % 2 == 0 and int(r * 12) % 2 == 1) else 0
                vv = v + dither
                p[x, y] = (5 + vv, 10 + vv * 4, 13 + vv * 3, 255)
    return im


def draw_scanner_frame(im: Image.Image) -> None:
    d = ImageDraw.Draw(im)
    W, H = im.size  # 128x128

    # 1. Outer beveled tile rim + segmented slate plates
    d.rectangle([0, 0, W - 1, H - 1], outline="#070c10")
    d.line([(1, 1), (W - 2, 1)], fill="#243a40")
    d.line([(1, 1), (1, H - 2)], fill="#243a40")
    d.line([(1, H - 2), (W - 2, H - 2)], fill="#0b1418")
    d.line([(W - 2, 1), (W - 2, H - 2)], fill="#0b1418")

    for seg in [28, 44, 60, 76, 92]:
        d.rectangle([seg, 2, seg + 7, 5], fill="#142428")
        d.line([(seg, 2), (seg + 7, 2)], fill="#213c42")
        d.rectangle([seg, H - 6, seg + 7, H - 3], fill="#111f23")
        d.rectangle([2, seg, 5, seg + 7], fill="#142428")
        d.rectangle([W - 6, seg, W - 3, seg + 7], fill="#111f23")

    # 2. Outer corner emerald pixel crosses
    for cx, cy in [(5, 5), (W - 7, 5), (5, H - 7), (W - 7, H - 7)]:
        d.rectangle([cx - 2, cy, cx + 3, cy + 1], fill="#2ec47a")
        d.rectangle([cx, cy - 2, cx + 1, cy + 3], fill="#2ec47a")
        d.rectangle([cx, cy, cx + 1, cy + 1], fill="#7ef7b8")
        d.point((cx - 2, cy + 1), fill="#145939")
        d.point((cx + 3, cy + 1), fill="#145939")

    # 3. Crisp white telemetry dashes along outer margin
    WHITE = "#eafcf4"
    WHITE_SHADOW = "#7ab8a0"
    for a, b in [(16, 20), (23, 26), (W - 27, W - 24), (W - 21, W - 17)]:
        d.rectangle([a, 5, b, 6], fill=WHITE)
        d.line([(a, 7), (b, 7)], fill="#091316")
        d.rectangle([a, H - 7, b, H - 6], fill=WHITE)
        d.rectangle([5, a, 6, b], fill=WHITE)
        d.rectangle([W - 7, a, W - 6, b], fill=WHITE)

    # 4. Stepped white L-corner frame at inset 10..117
    d.rectangle([9, 9, W - 10, H - 10], outline="#060e0d")
    d.rectangle([10, 10, W - 11, 11], fill=WHITE)
    d.rectangle([10, H - 12, W - 11, H - 11], fill=WHITE)
    d.rectangle([10, 10, 11, H - 11], fill=WHITE)
    d.rectangle([W - 12, 10, W - 11, H - 11], fill=WHITE)
    d.line([(12, 12), (W - 13, 12)], fill=WHITE_SHADOW)
    d.line([(12, 12), (12, H - 13)], fill=WHITE_SHADOW)

    for cx, cy, sx, sy in [(10, 10, 1, 1), (W - 11, 10, -1, 1), (10, H - 11, 1, -1), (W - 11, H - 11, -1, -1)]:
        for ox in range(6):
            for oy in range(6):
                if (ox < 4 and oy < 4) or (ox < 6 and oy < 2) or (ox < 2 and oy < 6):
                    d.point((cx + ox * sx, cy + oy * sy), fill=WHITE)

    # 5. Inner emerald hairline scanner frame at inset 15..112
    EM_LINE = "#2fd483"
    d.line([(19, 15), (W - 20, 15)], fill=EM_LINE)
    d.line([(19, H - 16), (W - 20, H - 16)], fill=EM_LINE)
    d.line([(15, 19), (15, H - 20)], fill=EM_LINE)
    d.line([(W - 16, 19), (W - 16, H - 20)], fill=EM_LINE)

    mid = W // 2
    d.line([(mid - 4, 15), (mid + 3, 15)], fill="#9efcd0")
    d.line([(mid - 1, 16), (mid, 16)], fill="#9efcd0")
    d.line([(mid - 4, H - 16), (mid + 3, H - 16)], fill="#9efcd0")
    d.line([(mid - 1, H - 17), (mid, H - 17)], fill="#9efcd0")
    d.line([(15, mid - 4), (15, mid + 3)], fill="#9efcd0")
    d.line([(16, mid - 1), (16, mid)], fill="#9efcd0")
    d.line([(W - 16, mid - 4), (W - 16, mid + 3)], fill="#9efcd0")
    d.line([(W - 17, mid - 1), (W - 17, mid)], fill="#9efcd0")

    for x0, y0 in [(17, 17), (W - 18, 17), (17, H - 18), (W - 18, H - 18)]:
        d.point((x0, y0), fill="#186b44")


def build_eye_sprite_22x16() -> Image.Image:
    """Hand-crafted 22x16 pixel-art All-Seeing Emerald Eye with sleek 2px vertical slit pupil."""
    PAL = {
        "o": (6, 15, 13, 255),      # Dark obsidian-jade outline
        "k": (12, 42, 36, 255),     # Deepest teal corner shadow
        "d": (18, 74, 62, 255),     # Outer dark sea-jade
        "t": (28, 118, 92, 255),    # Rich jade mid-dark
        "m": (44, 168, 114, 255),   # Vibrant emerald midtone
        "g": (82, 222, 138, 255),   # Bright emerald-lime glow
        "l": (176, 250, 184, 255),  # Luminous chartreuse-mint iris core
        "s": (204, 253, 228, 255),  # Soft mint secondary highlight
        "w": (246, 255, 250, 255),  # Pure mint-white specular highlight
        "p": (5, 12, 11, 255),      # Abyssal vertical slit pupil
        "q": (14, 48, 38, 255),     # Subtle pupil taper tip
    }

    GRID = [
        ".......oooooooo.......",  # 00
        ".....ookddttddkoo.....",  # 01
        "...ookdtwwsggmtdkoo...",  # 02
        "..okdtwwssgllgmtdtko..",  # 03
        ".okdtwwssglqqlgmtdtko.",  # 04
        ".odtwssggllppllgmtdko.",  # 05
        "ookdtsggllqppqllgmtkoo",  # 06
        "okdtmgllllpppplllgmdko",  # 07
        "okdtmgllllpppplllgmdko",  # 08
        "ookdtmggllqppqllgmtkoo",  # 09
        ".odtmggglllppllgmtdko.",  # 10
        ".okdtmgggglqqlgmtdtko.",  # 11
        "..okdtmggggllgmtdtko..",  # 12
        "...ookdttmmmmttdkoo...",  # 13
        ".....ookddttddkoo.....",  # 14
        ".......oooooooo.......",  # 15
    ]

    im = Image.new("RGBA", (22, 16), (0, 0, 0, 0))
    px = im.load()
    for y, row in enumerate(GRID):
        assert len(row) == 22, f"Row {y} width {len(row)} != 22"
        for x, ch in enumerate(row):
            if ch != ".":
                px[x, y] = PAL[ch]
    return im


def build_esp_brackets_native() -> Image.Image:
    """
    Crisp beveled gold ESP target corner brackets on a 30x24 native pixel grid
    (upscaled 3x = 90x72 on the 128x128 canvas).
    """
    W, H = 30, 24
    im = Image.new("RGBA", (W, H), (0, 0, 0, 0))

    OUT = (6, 15, 13, 255)
    GOLD_SH = (176, 114, 24, 255)
    GOLD_MD = (242, 188, 56, 255)
    GOLD_HI = (255, 236, 138, 255)
    GOLD_SP = (255, 252, 220, 255)

    tl = Image.new("RGBA", (7, 7), (0, 0, 0, 0))
    tp = tl.load()
    # Gold L: arm length 5 (1..5), thickness 2 (1..2)
    for y in range(1, 6):
        for x in range(1, 6):
            if y <= 2 or x <= 2:
                tp[x, y] = GOLD_MD
    for i in range(1, 6):
        tp[i, 1] = GOLD_HI
        tp[1, i] = GOLD_HI
    tp[1, 1] = GOLD_SP
    tp[2, 1] = GOLD_SP
    tp[1, 2] = GOLD_SP
    for i in range(2, 6):
        tp[i, 2] = GOLD_SH
        tp[2, i] = GOLD_SH
    tp[2, 2] = GOLD_MD

    outlined = tl.copy()
    op = outlined.load()
    for y in range(7):
        for x in range(7):
            if tp[x, y][3] == 0:
                for ox in (-1, 0, 1):
                    for oy in (-1, 0, 1):
                        nx, ny = x + ox, y + oy
                        if 0 <= nx < 7 and 0 <= ny < 7 and tp[nx, ny][3] > 0:
                            op[x, y] = OUT
                            break

    im.alpha_composite(outlined, (0, 0))
    im.alpha_composite(outlined.transpose(Image.Transpose.FLIP_LEFT_RIGHT), (W - 7, 0))
    im.alpha_composite(outlined.transpose(Image.Transpose.FLIP_TOP_BOTTOM), (0, H - 7))
    im.alpha_composite(outlined.transpose(Image.Transpose.ROTATE_180), (W - 7, H - 7))
    return im


def draw_sparkles(im: Image.Image) -> None:
    d = ImageDraw.Draw(im)

    def plus_sparkle(cx: int, cy: int, bright: bool = True) -> None:
        c_arm = "#5ef0a8" if bright else "#2ea872"
        c_core = "#eafff4" if bright else "#9efad0"
        d.line([(cx - 2, cy), (cx + 2, cy)], fill=c_arm)
        d.line([(cx, cy - 2), (cx, cy + 2)], fill=c_arm)
        d.point((cx, cy), fill=c_core)

    plus_sparkle(42, 22, True)
    plus_sparkle(86, 22, False)
    plus_sparkle(21, 64, True)
    plus_sparkle(106, 64, True)
    plus_sparkle(40, 105, False)
    plus_sparkle(86, 105, True)

    motes = [
        (26, 24, "#2d8a62"), (101, 24, "#4be39b"),
        (24, 98, "#4be39b"), (102, 98, "#2d8a62"),
        (63, 23, "#256e4e"), (64, 104, "#256e4e"),
    ]
    for x, y, col in motes:
        d.rectangle([x, y, x + 1, y + 1], fill=col)


def generate() -> None:
    im = make_background(128, 128)
    draw_scanner_frame(im)
    draw_sparkles(im)

    SCALE = 3
    # Pad native sprites by 2px so MaxFilter(5) / MaxFilter(3) rims never clip at canvas edges
    def pad_rgba(src: Image.Image, pad: int) -> Image.Image:
        out = Image.new("RGBA", (src.width + pad * 2, src.height + pad * 2), (0, 0, 0, 0))
        out.alpha_composite(src, (pad, pad))
        return out

    eye_native = pad_rgba(build_eye_sprite_22x16(), 1)  # 24x18
    eye_scaled = eye_native.resize((eye_native.width * SCALE, eye_native.height * SCALE), Image.Resampling.NEAREST)  # 72x54
    eye_xy = ((128 - eye_scaled.width) // 2, (128 - eye_scaled.height) // 2)  # (28, 37)

    brackets_native = pad_rgba(build_esp_brackets_native(), 1)  # 32x26
    brackets_scaled = brackets_native.resize((brackets_native.width * SCALE, brackets_native.height * SCALE), Image.Resampling.NEAREST)  # 96x78
    br_xy = ((128 - brackets_scaled.width) // 2, (128 - brackets_scaled.height) // 2)  # (16, 25)

    # Tight, subtle 1px-on-128-grid emerald rim around full padded silhouettes (never clips!)
    eye_mask = eye_scaled.getchannel("A")
    eye_aura_outer = Image.new("RGBA", eye_scaled.size, "#145238")
    eye_aura_outer.putalpha(eye_mask.filter(ImageFilter.MaxFilter(5)).point(lambda v: v * 95 // 255))
    im.alpha_composite(eye_aura_outer, eye_xy)

    eye_rim = Image.new("RGBA", eye_scaled.size, "#2ad484")
    eye_rim.putalpha(eye_mask.filter(ImageFilter.MaxFilter(3)).point(lambda v: v * 175 // 255))
    im.alpha_composite(eye_rim, eye_xy)

    br_mask = brackets_scaled.getchannel("A")
    br_rim = Image.new("RGBA", brackets_scaled.size, "#21a868")
    br_rim.putalpha(br_mask.filter(ImageFilter.MaxFilter(3)).point(lambda v: v * 165 // 255))
    im.alpha_composite(br_rim, br_xy)

    im.alpha_composite(brackets_scaled, br_xy)
    im.alpha_composite(eye_scaled, eye_xy)

    OUT_128.parent.mkdir(parents=True, exist_ok=True)
    OUT_512.parent.mkdir(parents=True, exist_ok=True)

    im.save(OUT_128)
    im.resize((512, 512), Image.Resampling.NEAREST).save(OUT_512)
    generate_banner(im)
    generate_hud_showcase()
    print(f"Generated {OUT_128}, {OUT_512}, {OUT_BANNER}, and {OUT_SHOWCASE}")


PIXEL_FONT = {
    "A": ["01110", "11011", "10001", "11111", "10001", "10001", "10001"],
    "B": ["11110", "10011", "10001", "11110", "10001", "10011", "11110"],
    "C": ["01111", "11000", "10000", "10000", "10000", "11000", "01111"],
    "D": ["11110", "10011", "10001", "10001", "10001", "10011", "11110"],
    "E": ["11111", "10000", "10000", "11110", "10000", "10000", "11111"],
    "F": ["11111", "10000", "10000", "11110", "10000", "10000", "10000"],
    "G": ["01111", "11000", "10000", "10111", "10001", "11001", "01111"],
    "H": ["10001", "10001", "10001", "11111", "10001", "10001", "10001"],
    "I": ["11111", "00100", "00100", "00100", "00100", "00100", "11111"],
    "K": ["10001", "10010", "10100", "11000", "10100", "10010", "10001"],
    "L": ["10000", "10000", "10000", "10000", "10000", "10000", "11111"],
    "M": ["10001", "11011", "11111", "10101", "10001", "10001", "10001"],
    "N": ["10001", "11001", "11001", "10101", "10011", "10011", "10001"],
    "O": ["01110", "11011", "10001", "10001", "10001", "11011", "01110"],
    "P": ["11110", "10011", "10001", "11110", "10000", "10000", "10000"],
    "R": ["11110", "10011", "10001", "11110", "10100", "10010", "10001"],
    "S": ["01111", "11000", "10000", "01110", "00001", "00011", "11110"],
    "T": ["11111", "00100", "00100", "00100", "00100", "00100", "00100"],
    "U": ["10001", "10001", "10001", "10001", "10001", "11011", "01110"],
    "V": ["10001", "10001", "10001", "10001", "01010", "01110", "00100"],
    "W": ["10001", "10001", "10001", "10101", "11111", "11011", "10001"],
    "X": ["10001", "01010", "00100", "00100", "00100", "01010", "10001"],
    "Y": ["10001", "01010", "00100", "00100", "00100", "00100", "00100"],
    "0": ["01110", "10011", "10101", "10101", "11001", "11001", "01110"],
    "1": ["00100", "01100", "00100", "00100", "00100", "00100", "01110"],
    "2": ["01110", "10001", "00001", "00110", "01000", "10000", "11111"],
    "3": ["11110", "00001", "00001", "01110", "00001", "00001", "11110"],
    "4": ["10010", "10010", "10010", "11111", "00010", "00010", "00010"],
    "5": ["11111", "10000", "11110", "00001", "00001", "10001", "01110"],
    "6": ["01110", "10000", "11110", "10001", "10001", "10001", "01110"],
    "7": ["11111", "00001", "00010", "00100", "01000", "01000", "01000"],
    "8": ["01110", "10001", "10001", "01110", "10001", "10001", "01110"],
    "9": ["01110", "10001", "10001", "01111", "00001", "00001", "01110"],
    ".": ["00000", "00000", "00000", "00000", "00000", "01100", "01100"],
    "-": ["00000", "00000", "00000", "11111", "00000", "00000", "00000"],
    "/": ["00001", "00010", "00100", "00100", "01000", "10000", "00000"],
    ":": ["00000", "01100", "01100", "00000", "01100", "01100", "00000"],
    "+": ["00000", "00100", "00100", "11111", "00100", "00100", "00000"],
    " ": ["00000"] * 7,
}


def pixel_word_mask(text: str, scale: int) -> Image.Image:
    text = text.upper()
    w = max(1, len(text) * 6 - 1)
    mask = Image.new("L", (w, 7), 0)
    md = ImageDraw.Draw(mask)
    for idx, ch in enumerate(text):
        rows = PIXEL_FONT.get(ch, PIXEL_FONT[" "])
        for y, row in enumerate(rows):
            for x, bit in enumerate(row):
                if bit == "1":
                    md.point((idx * 6 + x, y), fill=255)
    return mask.resize((w * scale, 7 * scale), Image.Resampling.NEAREST)


def draw_pixel_text(canvas: Image.Image, text: str, xy: tuple[int, int], scale: int, top_col: tuple[int, int, int], bot_col: tuple[int, int, int], outline_col: str = "#050b0e") -> int:
    mask = pixel_word_mask(text, scale)
    shadow = Image.new("RGBA", (mask.width + 4, mask.height + 4), (0, 0, 0, 0))
    padded = Image.new("L", (mask.width + 4, mask.height + 4), 0)
    padded.paste(mask, (2, 2))
    sh_layer = Image.new("RGBA", padded.size, outline_col)
    sh_layer.putalpha(padded.filter(ImageFilter.MaxFilter(3)))
    canvas.alpha_composite(sh_layer, (xy[0] - 2, xy[1] - 1))

    grad = Image.new("RGBA", mask.size)
    gd = ImageDraw.Draw(grad)
    for yy in range(mask.height):
        t = yy / max(1, mask.height - 1)
        c = (
            int(top_col[0] + (bot_col[0] - top_col[0]) * t),
            int(top_col[1] + (bot_col[1] - top_col[1]) * t),
            int(top_col[2] + (bot_col[2] - top_col[2]) * t),
            255,
        )
        gd.line([(0, yy), (mask.width, yy)], fill=c)
    grad.putalpha(mask)
    canvas.alpha_composite(grad, xy)
    return mask.width


def generate_banner(icon_128: Image.Image) -> None:
    """Procedural 480x160 pixel-art banner upscaled 4x NEAREST to 1920x640."""
    W, H = 480, 160
    banner = Image.new("RGBA", (W, H))
    px = banner.load()
    for y in range(H):
        for x in range(W):
            if x < 8 or x >= W - 8 or y < 8 or y >= H - 8:
                mod = (((x // 2) * 5 + (y // 2) * 3) % 4) - 1
                px[x, y] = (9 + mod, 15 + mod * 2, 19 + mod * 2, 255)
            else:
                r = math.hypot((x - 165) / 290.0, (y - 80) / 115.0)
                v = max(0, 7 - int(r * 7))
                grid = 1 if (x % 16 == 0 or y % 16 == 0) else 0
                px[x, y] = (5 + v + grid, 11 + v * 3 + grid * 3, 15 + v * 3 + grid * 2, 255)

    b = ImageDraw.Draw(banner)
    # Outer beveled frame + crisp white stepped L-corners + emerald scanner rails
    b.rectangle([0, 0, W - 1, H - 1], outline="#050a0d")
    b.line([(1, 1), (W - 2, 1)], fill="#213a40")
    b.line([(1, H - 2), (W - 2, H - 2)], fill="#0b1518")

    WHITE = "#eafcf4"
    b.rectangle([8, 8, W - 9, 9], fill=WHITE)
    b.rectangle([8, H - 10, W - 9, H - 9], fill=WHITE)
    b.rectangle([8, 8, 9, H - 9], fill=WHITE)
    b.rectangle([W - 10, 8, W - 9, H - 9], fill=WHITE)

    b.line([(14, 12), (W - 15, 12)], fill="#2fd483")
    b.line([(14, H - 13), (W - 15, H - 13)], fill="#2fd483")

    # Corner emerald crosses
    for cx, cy in [(4, 4), (W - 5, 4), (4, H - 5), (W - 5, H - 5)]:
        b.line([(cx - 1, cy), (cx + 1, cy)], fill="#34d399")
        b.line([(cx, cy - 1), (cx, cy + 1)], fill="#34d399")
        b.point((cx, cy), fill="#a7f3d0")

    # Decorative tactical radar silhouette on far right side (clear of title & subtitle!)
    rcx, rcy = 432, 80
    b.rectangle([rcx - 32, rcy - 50, rcx + 32, rcy + 50], fill="#071217", outline="#1c4a3e")
    b.rectangle([rcx - 16, rcy - 24, rcx + 16, rcy + 24], outline="#153830")
    b.line([(rcx - 29, rcy), (rcx + 29, rcy)], fill="#14332d")
    b.line([(rcx, rcy - 47), (rcx, rcy + 47)], fill="#14332d")
    for step in range(4, 44, 2):
        sp = (step * 4) // 9
        b.point((rcx - sp, rcy - step), fill="#26738c")
        b.point((rcx + sp, rcy - step), fill="#26738c")
    for dx, dy, col in [(-12, -26, "#ff4d5a"), (14, -11, "#38d6fc"), (-18, 19, "#ffd15c"), (9, 30, "#c084fc")]:
        b.rectangle([rcx + dx - 1, rcy + dy - 1, rcx + dx + 1, rcy + dy + 1], fill=col)
    b.rectangle([rcx - 1, rcy - 1, rcx + 1, rcy + 1], fill="#eafcf4")

    # Composite 128x128 icon on the left
    banner.alpha_composite(icon_128, (20, 16))

    # Crisp pixel title: SENSE + ESP (scale 4 so nothing touches the right radar)
    w_sense = draw_pixel_text(banner, "SENSE", (162, 28), 4, (238, 255, 246), (82, 222, 148))
    draw_pixel_text(banner, "ESP", (162 + w_sense + 8, 28), 4, (255, 246, 178), (242, 182, 52))

    # Subtitle & 3 clean feature pills (fits inside 162..392 without touching the radar)
    draw_pixel_text(banner, "TACTICAL RADAR + TARGET HUD", (163, 74), 1, (208, 244, 228), (134, 186, 170))
    b.line([(163, 91), (388, 91)], fill="#1c4a3e")

    badges = [
        ("FABRIC 1.21-26.3", "#112b26", "#2fd483", (180, 252, 216)),
        ("RADAR", "#162432", "#38d6fc", (186, 238, 255)),
        ("FAIR LOS", "#292112", "#f5b841", (255, 232, 168)),
    ]
    bx = 162
    by = 104
    for label, bg_hex, bd_hex, fg_rgb in badges:
        mask = pixel_word_mask(label, 1)
        pw = mask.width + 10
        ph = 15
        if bx + pw < 394:
            b.rectangle([bx, by, bx + pw, by + ph], fill=bg_hex, outline=bd_hex)
            draw_pixel_text(banner, label, (bx + 5, by + 4), 1, fg_rgb, fg_rgb)
            bx += pw + 6

    banner.resize((1920, 640), Image.Resampling.NEAREST).save(OUT_BANNER)


def generate_hud_showcase() -> None:
    """Procedural 420x236 pixel-art HUD preview upscaled 3x NEAREST to 1260x708."""
    W, H = 420, 236
    im = Image.new("RGBA", (W, H))
    px = im.load()
    # Stylized dark cavern / deepslate horizon backdrop
    for y in range(H):
        for x in range(W):
            horizon = 126
            if y < horizon:
                t = y / horizon
                px[x, y] = (int(8 + 10 * t), int(14 + 18 * t), int(22 + 24 * t), 255)
            else:
                t = (y - horizon) / (H - horizon)
                checker = 2 if (((x // 12) + (y // 8)) % 2 == 0) else 0
                px[x, y] = (int(12 + 8 * t) + checker, int(18 + 10 * t) + checker, int(24 + 12 * t) + checker, 255)

    d = ImageDraw.Draw(im)
    # Perspective grid lines on floor
    for gx in range(-160, 580, 40):
        d.line([(210 + (gx - 210) // 4, 126), (gx, H - 1)], fill="#162632")
    for gy in [136, 150, 168, 192, 220]:
        d.line([(0, gy), (W - 1, gy)], fill="#15232e")

    # 1. Status HUD Pill (top-left)
    d.rectangle([8, 8, 150, 23], fill="#080e16", outline="#1e3538")
    d.rectangle([8, 8, 10, 23], fill="#34d399")
    draw_pixel_text(im, "SENSEESP", (14, 12), 1, (241, 245, 249), (203, 213, 225))
    d.rectangle([68, 10, 100, 21], fill="#113328", outline="#34d399")
    draw_pixel_text(im, "FULL", (73, 12), 1, (110, 242, 178), (52, 211, 153), outline_col="#113328")
    draw_pixel_text(im, "4 TGT", (107, 12), 1, (148, 163, 184), (148, 163, 184))

    # 2. Tactical Mini-Radar below Status Pill
    rx, ry, rs = 8, 27, 64
    d.rectangle([rx, ry, rx + rs, ry + rs], fill="#070d14", outline="#1e3a34")
    d.line([(rx + 1, ry + 1), (rx + rs - 1, ry + 1)], fill="#246b56")
    rcx, rcy = rx + rs // 2, ry + rs // 2
    d.line([(rx + 3, rcy), (rx + rs - 3, rcy)], fill="#1c2c3b")
    d.line([(rcx, ry + 3), (rcx, ry + rs - 3)], fill="#1c2c3b")
    d.rectangle([rcx - 14, rcy - 14, rcx + 14, rcy + 14], outline="#14382e")
    for i in range(3, 27, 2):
        sp = (i * 5) // 8
        d.point((rcx - sp, rcy - i), fill="#266885")
        d.point((rcx + sp, rcy - i), fill="#266885")
    # Blips
    d.rectangle([rcx - 2, rcy - 18, rcx + 2, rcy - 14], fill="#ffffff")
    d.rectangle([rcx - 1, rcy - 17, rcx + 1, rcy - 15], fill="#ff4d5a")
    d.rectangle([rcx + 15, rcy - 8, rcx + 17, rcy - 6], fill="#38d6fc")
    d.rectangle([rcx - 18, rcy + 11, rcx - 16, rcy + 13], fill="#c084fc")
    d.rectangle([rcx - 1, rcy - 1, rcx + 1, rcy + 1], fill="#34d399")

    # 3. Center Locked Target: 3D Pulse Ring + 2D Corner Brackets + Vertical HP Bar + Nametag Card
    tx1, ty1, tx2, ty2 = 186, 66, 234, 152
    # Silhouette mob figure inside
    d.rectangle([202, 74, 218, 90], fill="#1b2e28", outline="#28453c")
    d.rectangle([200, 91, 220, 124], fill="#182924", outline="#254038")
    d.rectangle([202, 125, 208, 148], fill="#14221e")
    d.rectangle([212, 125, 218, 148], fill="#14221e")
    # Animated Pulse Ring ellipse around target
    d.ellipse([178, 104, 242, 122], outline="#ff858e", width=2)
    d.ellipse([178, 142, 242, 156], outline="#8f2934", width=1)
    # 2D Locked Corner Brackets
    for cx0, cy0, sx, sy in [(tx1, ty1, 1, 1), (tx2, ty1, -1, 1), (tx1, ty2, 1, -1), (tx2, ty2, -1, -1)]:
        d.line([(cx0, cy0), (cx0 + 11 * sx, cy0)], fill="#ff9ea6", width=2)
        d.line([(cx0, cy0), (cx0, cy0 + 11 * sy)], fill="#ff9ea6", width=2)
    # Smooth Vertical HP bar on left
    d.rectangle([tx1 - 6, ty1, tx1 - 3, ty2], fill="#060a12")
    d.rectangle([tx1 - 5, ty1 + 18, tx1 - 4, ty2 - 1], fill="#4ade80")
    # Nametag card above target
    ncw = pixel_word_mask("VINDICATOR 12HP 8M", 1).width + 14
    ncx, ncy = 210 - ncw // 2, 47
    d.rectangle([ncx, ncy, ncx + ncw, ncy + 14], fill="#080e16", outline="#1e3246")
    d.rectangle([ncx, ncy, ncx + 2, ncy + 14], fill="#ff4d5a")
    draw_pixel_text(im, "VINDICATOR 12HP 8M", (ncx + 7, ncy + 4), 1, (248, 250, 252), (186, 230, 210))

    # 4. Secondary Player target on right (smooth double-helix Spiral style)
    px1, py1, px2, py2 = 304, 78, 340, 144
    d.rectangle([315, 84, 329, 98], fill="#182636", outline="#28425c")
    d.rectangle([313, 99, 331, 140], fill="#14202e", outline="#22384f")
    spiral_a = [(int(322 + math.sin(i * 0.42) * 18), 140 - i * 3) for i in range(19)]
    spiral_b = [(int(322 - math.sin(i * 0.42) * 18), 140 - i * 3) for i in range(19)]
    d.line(spiral_b, fill="#1d6d86", width=1)
    d.line(spiral_a, fill="#38d6fc", width=2)
    acw = pixel_word_mask("ALEX 19M", 1).width + 14
    acx = 322 - acw // 2
    d.rectangle([acx, 61, acx + acw, 75], fill="#080e16", outline="#1e3246")
    d.rectangle([acx, 61, acx + 2, 75], fill="#38d6fc")
    draw_pixel_text(im, "ALEX 19M", (acx + 7, 65), 1, (241, 245, 249), (148, 212, 248))

    # 5. Storage Chest Corner Box on left (SP/OP)
    bx1, by1, bx2, by2 = 88, 118, 118, 146
    d.rectangle([bx1 + 3, by1 + 3, bx2 - 3, by2 - 3], fill="#332411", outline="#f5b841")
    for cx0, cy0, sx, sy in [(bx1, by1, 1, 1), (bx2, by1, -1, 1), (bx1, by2, 1, -1), (bx2, by2, -1, -1)]:
        d.line([(cx0, cy0), (cx0 + 7 * sx, cy0)], fill="#ffd15c", width=2)
        d.line([(cx0, cy0), (cx0, cy0 + 7 * sy)], fill="#ffd15c", width=2)

    # 6. Crosshair + Target HUD Inspector Card below crosshair
    d.line([(207, 118), (213, 118)], fill="#eafcf4")
    d.line([(210, 115), (210, 121)], fill="#eafcf4")

    cx_card, cy_card, cw, ch = 142, 168, 136, 34
    d.rectangle([cx_card, cy_card, cx_card + cw, cy_card + ch], fill="#080e16", outline="#1e3246")
    d.line([(cx_card + 1, cy_card + 1), (cx_card + cw - 1, cy_card + 1)], fill="#2b4865")
    d.rectangle([cx_card, cy_card, cx_card + 2, cy_card + ch], fill="#ff4d5a")
    draw_pixel_text(im, "VINDICATOR", (cx_card + 7, cy_card + 5), 1, (248, 250, 252), (226, 232, 240))
    draw_pixel_text(im, "8.4M 4ARM", (cx_card + 78, cy_card + 5), 1, (148, 163, 184), (148, 163, 184))
    d.rectangle([cx_card + 7, cy_card + 16, cx_card + cw - 7, cy_card + 21], fill="#04070d")
    d.rectangle([cx_card + 7, cy_card + 16, cx_card + 102, cy_card + 21], fill="#34d399")
    d.line([(cx_card + 7, cy_card + 16), (cx_card + 102, cy_card + 16)], fill="#a7f3d0")
    draw_pixel_text(im, "19.0 / 24 HP", (cx_card + 7, cy_card + 24), 1, (203, 213, 225), (148, 163, 184))

    im.resize((W * 3, H * 3), Image.Resampling.NEAREST).save(OUT_SHOWCASE)


if __name__ == "__main__":
    generate()
