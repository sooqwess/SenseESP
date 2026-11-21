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
    print(f"Generated {OUT_128} (128x128) and {OUT_512} (512x512 NEAREST)")


if __name__ == "__main__":
    generate()
