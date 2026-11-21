#!/usr/bin/env python3
"""
Pixel-art branding generator for SenseESP.

Matches the exact visual language and structural geometry of Forged Relics'
approved original branding:
- Banner (`assets/banner.png`): 1600x400 RGB
  - Deep abyssal-emerald atmospheric canvas (`#060d0b`) with soft radial aura around
    the right-side sprite and sparse crisp star/mote field (1px, 2px, and 4-point glints).
  - Pure `#ffffff` stepped 5px frame (`[22..1577, 22..377]`) with 26x26 corner blocks,
    outer paired `14x4` telemetry ticks, and 2px inner emerald hairline rails (`y=38..39`,
    `y=360..361`, `x=48..1551`).
  - Flat, crisp 5x7 pixel wordmark (`SENSE` in emerald `#5eead4`/`#34d399` or warm gold +
    `ESP` in gold `#d9b36a`) at `14x` pixel scale (`98px` tall), zero fake gradients,
    zero subtitle/pill clutter.
  - Right-side large unframed All-Seeing Emerald Eye + gold ESP target brackets sprite
    (`10x` crisp NEAREST pixel scale = `300x240`) with soft multi-pass silhouette rim/glow
    and subtle warm gold dust motes.
- Icon (`assets/icon.png` 512x512 & mod icon 128x128):
  - Matching dark-abyssal canvas, pure `#ffffff` stepped frame, 4 gold corner crosses,
    outer white telemetry ticks, thin inner emerald hairline, and centered glowing
    Emerald Eye + gold ESP brackets sprite.
"""

from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter
import math
import random

ROOT = Path(__file__).resolve().parents[1]
OUT_512 = ROOT / "assets" / "icon.png"
OUT_BANNER = ROOT / "assets" / "banner.png"
OUT_128 = ROOT / "common" / "src" / "main" / "resources" / "assets" / "espmod" / "icon.png"


PIXEL_FONT = {
    "A": ["01110", "10001", "10001", "11111", "10001", "10001", "10001"],
    "B": ["11110", "10001", "10001", "11110", "10001", "10001", "11110"],
    "C": ["01110", "10001", "10000", "10000", "10000", "10001", "01110"],
    "D": ["11110", "10001", "10001", "10001", "10001", "10001", "11110"],
    "E": ["11111", "10000", "10000", "11110", "10000", "10000", "11111"],
    "F": ["11111", "10000", "10000", "11110", "10000", "10000", "10000"],
    "G": ["01110", "10001", "10000", "10111", "10001", "10001", "01110"],
    "H": ["10001", "10001", "10001", "11111", "10001", "10001", "10001"],
    "I": ["11111", "00100", "00100", "00100", "00100", "00100", "11111"],
    "L": ["10000", "10000", "10000", "10000", "10000", "10000", "11111"],
    "N": ["10001", "11001", "10101", "10101", "10011", "10001", "10001"],
    "O": ["01110", "10001", "10001", "10001", "10001", "10001", "01110"],
    "P": ["11110", "10001", "10001", "11110", "10000", "10000", "10000"],
    "R": ["11110", "10001", "10001", "11110", "10100", "10010", "10001"],
    "S": ["01111", "10000", "10000", "01110", "00001", "00001", "11110"],
    " ": ["00000"] * 7,
}


def build_eye_sprite_22x16() -> Image.Image:
    """Hand-crafted 22x16 pixel-art All-Seeing Emerald Eye with sleek 2px vertical slit pupil."""
    PAL = {
        "o": (5, 11, 10, 255),      # Crisp near-black obsidian-jade outline
        "k": (11, 38, 33, 255),     # Deepest teal corner shadow
        "d": (17, 70, 58, 255),     # Outer dark sea-jade
        "t": (27, 114, 88, 255),    # Rich jade mid-dark
        "m": (44, 168, 116, 255),   # Vibrant emerald midtone
        "g": (86, 224, 148, 255),   # Bright emerald-mint glow
        "l": (174, 248, 196, 255),  # Luminous mint iris core
        "s": (212, 253, 232, 255),  # Soft mint secondary highlight
        "w": (248, 255, 252, 255),  # Crisp specular glint
        "p": (4, 9, 9, 255),        # Abyssal vertical slit pupil
        "q": (14, 46, 38, 255),     # Subtle pupil taper tip
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
        for x, ch in enumerate(row):
            if ch != ".":
                px[x, y] = PAL[ch]
    return im


def build_esp_brackets_30x24() -> Image.Image:
    """Crisp beveled gold ESP target corner brackets on a 30x24 native pixel grid."""
    W, H = 30, 24
    im = Image.new("RGBA", (W, H), (0, 0, 0, 0))

    OUT = (5, 10, 10, 255)
    GOLD_SH = (158, 108, 38, 255)
    GOLD_MD = (217, 179, 106, 255)  # Matches Forged Relics #d9b36a warm gold!
    GOLD_HI = (244, 216, 148, 255)
    GOLD_SP = (255, 244, 204, 255)

    tl = Image.new("RGBA", (7, 7), (0, 0, 0, 0))
    tp = tl.load()
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


def build_combined_sprite_native() -> Image.Image:
    """Combines the 22x16 Emerald Eye centered inside the 30x24 Gold ESP Brackets."""
    canvas = Image.new("RGBA", (30, 24), (0, 0, 0, 0))
    canvas.alpha_composite(build_esp_brackets_30x24(), (0, 0))
    canvas.alpha_composite(build_eye_sprite_22x16(), (4, 4))
    return canvas


def composite_sprite_with_aura(
    target: Image.Image,
    sprite_native: Image.Image,
    scale: int,
    xy: tuple[int, int],
) -> None:
    """
    Renders the crisp NEAREST-scaled sprite with the exact Forged Relics style
    tight silhouette rim + soft atmospheric outer halo (never blurring the sprite itself).
    """
    pad = 4
    padded = Image.new(
        "RGBA",
        (sprite_native.width + pad * 2, sprite_native.height + pad * 2),
        (0, 0, 0, 0),
    )
    padded.alpha_composite(sprite_native, (pad, pad))
    scaled = padded.resize(
        (padded.width * scale, padded.height * scale),
        Image.Resampling.NEAREST,
    )
    mask = scaled.getchannel("A")
    px = xy[0] - pad * scale
    py = xy[1] - pad * scale

    # Soft outer emerald-jade bloom (like Forged Relics' #604b7e / #362959 sword halo)
    halo_wide = Image.new("RGBA", scaled.size, (38, 128, 96, 255))
    halo_wide_a = mask.filter(ImageFilter.MaxFilter(9)).filter(ImageFilter.GaussianBlur(scale * 1.35))
    halo_wide.putalpha(halo_wide_a.point(lambda a: min(255, int(a * 0.55))))
    target.alpha_composite(halo_wide, (px, py))

    # Tight silhouette rim (3px around crisp pixel edges)
    rim_px = max(3, (scale // 3) | 1)
    rim = Image.new("RGBA", scaled.size, (94, 214, 166, 255))
    rim_a = mask.filter(ImageFilter.MaxFilter(rim_px * 2 + 1)).filter(ImageFilter.GaussianBlur(1.4))
    rim.putalpha(rim_a.point(lambda a: min(255, int(a * 0.72))))
    target.alpha_composite(rim, (px, py))

    # Crisp unblurred pixel sprite on top
    target.alpha_composite(scaled, (px, py))


def draw_starfield(
    draw: ImageDraw.ImageDraw,
    w: int,
    h: int,
    seed: int,
    exclude_boxes: list[tuple[int, int, int, int]],
    star_count: int,
    sparkle_coords: list[tuple[int, int, int]],
) -> None:
    rng = random.Random(seed)

    def in_excluded(x: int, y: int) -> bool:
        for x0, y0, x1, y1 in exclude_boxes:
            if x0 <= x <= x1 and y0 <= y <= y1:
                return True
        return False

    palette = [
        (112, 146, 138),
        (134, 168, 158),
        (164, 198, 188),
        (204, 232, 224),
        (142, 136, 112),
    ]
    placed = 0
    attempts = 0
    while placed < star_count and attempts < star_count * 12:
        attempts += 1
        x = rng.randint(14, w - 15)
        y = rng.randint(12, h - 13)
        if in_excluded(x, y):
            continue
        col = palette[rng.randrange(len(palette))]
        sz = 2 if rng.random() < 0.22 else 1
        if sz == 1:
            draw.point((x, y), fill=col)
        else:
            draw.rectangle([x, y, x + 1, y + 1], fill=col)
        placed += 1

    # Crisp 4-point cross sparkles (exact Forged Relics cross geometry)
    for cx, cy, arm in sparkle_coords:
        if in_excluded(cx, cy):
            continue
        ray_col = (122, 164, 154)
        mid_col = (186, 226, 214)
        draw.line([(cx - arm, cy), (cx + arm, cy)], fill=ray_col)
        draw.line([(cx, cy - arm), (cx, cy + arm)], fill=ray_col)
        draw.rectangle([cx - 1, cy - 1, cx + 1, cy + 1], fill=mid_col)
        draw.point((cx, cy), fill=(255, 255, 255))


def draw_flat_word(
    draw: ImageDraw.ImageDraw,
    text: str,
    x0: int,
    y0: int,
    block: int,
    color: tuple[int, int, int],
) -> int:
    """Draws crisp flat 5x7 pixel typography (zero outline, zero gradient) like Forged Relics."""
    cx = x0
    for idx, ch in enumerate(text.upper()):
        rows = PIXEL_FONT[ch]
        for r, row in enumerate(rows):
            for c, bit in enumerate(row):
                if bit == "1":
                    px0 = cx + c * block
                    py0 = y0 + r * block
                    draw.rectangle([px0, py0, px0 + block - 1, py0 + block - 1], fill=color)
        cx += 6 * block
    return cx - block - x0


def generate_banner() -> None:
    W, H = 1600, 400
    im = Image.new("RGBA", (W, H), (7, 13, 12, 255))
    px = im.load()

    # Subtle deep night-emerald atmospheric glow centered behind right-side sprite (x=1296, y=200)
    # Left side is rich obsidian-pine (#070d0c) warming smoothly toward deep sea-jade (#10261f) around the sprite,
    # matching Forged Relics' #090817 -> #221941 atmospheric depth.
    sx_center, sy_center = 1296.0, 200.0
    for y in range(H):
        dy = (y - sy_center) / 205.0
        dy2 = dy * dy
        for x in range(W):
            dx = (x - sx_center) / 370.0
            dist = math.sqrt(dx * dx + dy2)
            glow = max(0.0, 1.0 - dist)
            glow2 = glow * glow
            h_ramp = max(0.0, min(1.0, (x - 360.0) / 1060.0)) * (1.0 - 0.32 * abs(y - 200.0) / 200.0)
            r = int(7 + 6 * h_ramp + 12 * glow2)
            g = int(13 + 16 * h_ramp + 34 * glow2)
            b = int(12 + 13 * h_ramp + 26 * glow2)
            px[x, y] = (r, g, b, 255)

    d = ImageDraw.Draw(im)

    # Wordmark geometry: 14px block -> height = 7 * 14 = 98px (y=151..248)
    # "SENSE" = 29 blocks = 406px; gap = 84px; "ESP" = 17 blocks = 238px -> total = 728px (x=176..904)
    BLOCK = 14
    word_y = (H - 7 * BLOCK) // 2  # 151
    sense_x = 176
    sense_w = 29 * BLOCK  # 406
    esp_x = sense_x + sense_w + 84  # 666
    esp_w = 17 * BLOCK  # 238

    # Sprite geometry: 30x24 at 10x = 300x240, placed at x=1136..1436, y=80..320
    SPRITE_SCALE = 10
    sprite_x = 1136
    sprite_y = (H - 24 * SPRITE_SCALE) // 2  # 80

    # Sparse crisp stars + 4-point glints (avoiding wordmark, frame rails, and sprite core)
    exclude = [
        (sense_x - 24, word_y - 22, esp_x + esp_w + 28, word_y + 7 * BLOCK + 22),
        (sprite_x - 18, sprite_y - 18, sprite_x + 300 + 18, sprite_y + 240 + 18),
        (18, 18, 54, 54), (W - 55, 18, W - 19, 54),
        (18, H - 55, 54, H - 19), (W - 55, H - 55, W - 19, H - 19),
    ]
    sparkles = [
        (72, 62, 5),
        (318, 76, 4),
        (566, 60, 6),
        (942, 92, 5),
        (1062, 296, 5),
        (884, 332, 4),
        (244, 326, 4),
        (1518, 114, 5),
        (1494, 308, 4),
    ]
    draw_starfield(d, W, H, seed=1507, exclude_boxes=exclude, star_count=88, sparkle_coords=sparkles)

    # Subtle warm amber-gold dust motes drifting near top-left of the ESP brackets (like Forged Relics' guard motes)
    mote_rng = random.Random(421)
    mote_cols = [(217, 179, 106), (235, 198, 128), (184, 138, 74), (146, 218, 182)]
    for _ in range(18):
        mx = mote_rng.randint(sprite_x - 28, sprite_x + 76)
        my = mote_rng.randint(sprite_y - 16, sprite_y + 62)
        mc = mote_cols[mote_rng.randrange(len(mote_cols))]
        if mote_rng.random() < 0.45:
            d.rectangle([mx, my, mx + 1, my + 1], fill=mc)
        else:
            d.point((mx, my), fill=mc)

    # Exact Forged Relics border geometry on 1600x400:
    WHITE = (255, 255, 255, 255)
    RAIL = (52, 168, 126, 255)  # Subtle, refined emerald hairline matching #6f52b8 saturation/value

    # 1. Outer 5px white frame [22..1577, 22..377]
    d.rectangle([22, 22, 1577, 26], fill=WHITE)
    d.rectangle([22, 373, 1577, 377], fill=WHITE)
    d.rectangle([22, 22, 26, 377], fill=WHITE)
    d.rectangle([1573, 22, 1577, 377], fill=WHITE)

    # 2. Solid 26x26 corner blocks
    d.rectangle([22, 22, 47, 47], fill=WHITE)
    d.rectangle([1552, 22, 1577, 47], fill=WHITE)
    d.rectangle([22, 352, 47, 377], fill=WHITE)
    d.rectangle([1552, 352, 1577, 377], fill=WHITE)

    # 3. Outer paired 14x4px white telemetry ticks
    for x0, x1 in [(70, 83), (96, 109), (1490, 1503), (1516, 1529)]:
        d.rectangle([x0, 10, x1, 13], fill=WHITE)
        d.rectangle([x0, 386, x1, 389], fill=WHITE)
    for y0, y1 in [(70, 83), (96, 109), (290, 303), (316, 329)]:
        d.rectangle([10, y0, 13, y1], fill=WHITE)
        d.rectangle([1586, y0, 1589, y1], fill=WHITE)

    # 4. Inner 2px top & bottom horizontal accent rails (x=48..1551, y=38..39 & y=360..361)
    d.rectangle([48, 38, 1551, 39], fill=RAIL)
    d.rectangle([48, 360, 1551, 361], fill=RAIL)

    # 5. Right-side unframed Emerald Eye + Gold ESP Brackets hero sprite with soft aura
    sprite = build_combined_sprite_native()
    composite_sprite_with_aura(im, sprite, scale=SPRITE_SCALE, xy=(sprite_x, sprite_y))

    # 6. Flat two-tone pixel wordmark: warm gold "SENSE" + cool mint-emerald "ESP"
    #    (paralleling Forged Relics' warm gold #d9b36a "FORGED" + cool lavender #c2aef5 "RELICS")
    GOLD_WORD = (217, 179, 106)   # #d9b36a
    MINT_WORD = (134, 232, 190)   # #86e8be (balanced pastel-emerald counterpart to #c2aef5)
    draw_flat_word(d, "SENSE", sense_x, word_y, BLOCK, GOLD_WORD)
    draw_flat_word(d, "ESP", esp_x, word_y, BLOCK, MINT_WORD)

    OUT_BANNER.parent.mkdir(parents=True, exist_ok=True)
    im.convert("RGB").save(OUT_BANNER)


def generate_icon() -> None:
    """Generates matching 512x512 Modrinth icon and 128x128 Fabric mod icon."""
    S = 512
    im = Image.new("RGBA", (S, S), (7, 11, 13, 255))
    px = im.load()

    for y in range(S):
        dy = (y - 256.0) / 220.0
        for x in range(S):
            dx = (x - 256.0) / 220.0
            glow = max(0.0, 1.0 - math.hypot(dx, dy))
            g2 = glow * glow
            px[x, y] = (
                int(7 + 10 * g2),
                int(10 + 26 * g2),
                int(13 + 21 * g2),
                255,
            )

    d = ImageDraw.Draw(im)

    # Sparse stars inside 512x512 icon
    exclude = [
        (96, 116, 416, 396),
        (0, 0, 56, 56), (S - 57, 0, S - 1, 56),
        (0, S - 57, 56, S - 1), (S - 57, S - 57, S - 1, S - 1),
    ]
    sparkles = [(116, 108, 5), (398, 118, 4), (108, 396, 4), (404, 404, 5)]
    draw_starfield(d, S, S, seed=909, exclude_boxes=exclude, star_count=34, sparkle_coords=sparkles)

    WHITE = (255, 255, 255, 255)
    GOLD = (217, 179, 106, 255)
    RAIL = (52, 168, 126, 255)

    # Stepped white frame [36..475] (9px thick) + 28x28 corner blocks (matching Forged Relics icon)
    d.rectangle([36, 36, 475, 44], fill=WHITE)
    d.rectangle([36, 467, 475, 475], fill=WHITE)
    d.rectangle([36, 36, 44, 475], fill=WHITE)
    d.rectangle([467, 36, 475, 475], fill=WHITE)

    d.rectangle([36, 36, 63, 63], fill=WHITE)
    d.rectangle([448, 36, 475, 63], fill=WHITE)
    d.rectangle([36, 448, 63, 475], fill=WHITE)
    d.rectangle([448, 448, 475, 475], fill=WHITE)

    # Inner 2px emerald hairline rails
    d.rectangle([68, 52, 443, 53], fill=RAIL)
    d.rectangle([68, 458, 443, 459], fill=RAIL)
    d.rectangle([52, 68, 53, 443], fill=RAIL)
    d.rectangle([458, 68, 459, 443], fill=RAIL)

    # Outer white paired telemetry dashes
    for a, b in [(64, 75), (82, 93), (418, 429), (436, 447)]:
        d.rectangle([a, 24, b, 27], fill=WHITE)
        d.rectangle([a, 484, b, 487], fill=WHITE)
        d.rectangle([24, a, 27, b], fill=WHITE)
        d.rectangle([484, a, 487, b], fill=WHITE)

    # Four outer gold pixel crosses in corners (matching Forged Relics icon's 4 gold corner crosses)
    for cx, cy in [(22, 22), (S - 23, 22), (22, S - 23), (S - 23, S - 23)]:
        d.rectangle([cx - 8, cy - 2, cx + 8, cy + 2], fill=GOLD)
        d.rectangle([cx - 2, cy - 8, cx + 2, cy + 8], fill=GOLD)
        d.rectangle([cx - 5, cy - 5, cx + 5, cy + 5], fill=GOLD)

    # Centered sprite (30x24 at 11x = 330x264)
    sprite = build_combined_sprite_native()
    sc = 11
    composite_sprite_with_aura(
        im,
        sprite,
        scale=sc,
        xy=((S - 30 * sc) // 2, (S - 24 * sc) // 2),
    )

    OUT_512.parent.mkdir(parents=True, exist_ok=True)
    OUT_128.parent.mkdir(parents=True, exist_ok=True)
    im.convert("RGB").save(OUT_512)
    im.resize((128, 128), Image.Resampling.LANCZOS).convert("RGB").save(OUT_128)


if __name__ == "__main__":
    generate_banner()
    generate_icon()
    print(f"Generated {OUT_BANNER}, {OUT_512}, and {OUT_128}")
