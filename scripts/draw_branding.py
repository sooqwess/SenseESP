#!/usr/bin/env python3
"""
Pixel-art branding generator for SenseESP.

Visual language:
- Banner (`assets/banner.png`): 1600x400 RGB
  - Deep abyssal-emerald atmospheric canvas (`#060d0b`) with soft radial aura around
    the right-side sprite and sparse crisp star/mote field (1px, 2px, and 4-point glints).
  - Pure `#ffffff` stepped 5px frame (`[22..1577, 22..377]`) with 26x26 corner blocks,
    outer paired `14x4` telemetry ticks, and 2px inner emerald hairline rails (`y=38..39`,
    `y=360..361`, `x=48..1551`).
  - Flat, crisp 5x7 pixel wordmark (`SENSE` in warm gold `#d9b36a` + `ESP` in mint
    `#86e8be`) at `14x` pixel scale (`98px` tall), zero fake gradients, zero clutter.
  - Right-side hero sprite: authentic vanilla Eye of Ender (JAPPA palette) with a crisp
    1-texel Spectral Arrow outline (`#ffffff` specular / `#ffff77` spectral yellow /
    `#dab930` gold / `#cf8c27` amber), rendered at `15x` NEAREST (`240x240`) with a
    soft spectral-gold aura on the dark banner canvas.
- Icon (`assets/icon.png` 512x512 & mod icon 128x128): pure vanilla item sprite on a
  100% transparent background — zero frames, zero stars, zero blur. The native `16x16`
  grid is scaled with exact integer `NEAREST` (`32x` -> 512, `8x` -> 128) so pixels stay
  razor-sharp with zero semi-transparent halo on light/dark Modrinth & ModMenu UIs.
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


def build_spectral_eye_16x16() -> Image.Image:
    """Authentic vanilla Eye of Ender (JAPPA palette) + 1-texel Spectral Arrow outline.

    The eye body is pixel-identical to vanilla `item/ender_eye.png` (16x16, bounds
    x=2..14, y=2..14). The outline is a crisp 8-neighbor dilation in the exact
    `item/spectral_arrow.png` palette: `#ffffff` top specular, `#ffff77` spectral
    yellow main rim, `#dab930` gold bottom/right, `#cf8c27` amber deepest shadow.
    Strictly `alpha=0` outside the silhouette — zero blur, zero halo.
    """
    PAL = {
        # Spectral Arrow outline
        "W": (255, 255, 255, 255),  # ffffff specular
        "Y": (255, 255, 119, 255),  # ffff77 spectral yellow
        "G": (218, 185, 48, 255),   # dab930 spectral gold
        "A": (207, 140, 39, 255),   # cf8c27 amber shadow
        # Vanilla Eye of Ender (JAPPA)
        "t": (49, 99, 100, 255),    # 316364 teal edge
        "d": (30, 72, 53, 255),     # 1e4835 dark jade
        "m": (78, 131, 134, 255),   # 4e8386 mid teal
        "s": (101, 155, 125, 255),  # 659b7d seafoam sclera
        "c": (203, 252, 221, 255),  # cbfcdd mint specular
        "l": (181, 228, 90, 255),   # b5e45a iris lime
        "i": (136, 195, 90, 255),   # 88c35a iris green
        "j": (151, 209, 107, 255),  # 97d16b iris green
        "e": (113, 172, 73, 255),   # 71ac49 iris emerald
        "n": (87, 145, 67, 255),    # 579143 iris shadow
        "k": (16, 44, 49, 255),     # 102c31 deep shadow rim
        "p": (27, 43, 16, 255),     # 1b2b10 vertical slit pupil
    }

    GRID = [
        "................",  # 00
        "......WWWWW.....",  # 01
        "....WWYtddYYY...",  # 02
        "...YYttmmmddYY..",  # 03
        "..YYtmssssstdYG.",  # 04
        "..YdmscliesstdG.",  # 05
        ".YYdscljdnesskGG",  # 06
        ".YdtsljdpdnestkG",  # 07
        ".YdteeidpdnestkG",  # 08
        ".YdtsendpdieetkG",  # 09
        ".YYdmseidjemskGG",  # 10
        "..YdtmsenemetkG.",  # 11
        "..YYktmmmmstkYG.",  # 12
        "...GGkktttkkGG..",  # 13
        "....GGGkkkAAA...",  # 14
        "......AAAAA.....",  # 15
    ]

    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = im.load()
    for y, row in enumerate(GRID):
        assert len(row) == 16, f"row {y} has {len(row)} chars"
        for x, ch in enumerate(row):
            if ch != ".":
                px[x, y] = PAL[ch]
    return im


def composite_sprite_with_aura(
    target: Image.Image,
    sprite_native: Image.Image,
    scale: int,
    xy: tuple[int, int],
) -> None:
    """
    Renders the crisp NEAREST-scaled sprite with a soft spectral-gold aura
    (banner only — never used for the transparent icon). The sprite itself
    is never blurred; only the halo behind it is soft.
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

    # Soft outer spectral-gold bloom
    halo_wide = Image.new("RGBA", scaled.size, (218, 185, 48, 255))
    halo_wide_a = mask.filter(ImageFilter.MaxFilter(9)).filter(ImageFilter.GaussianBlur(scale * 1.35))
    halo_wide.putalpha(halo_wide_a.point(lambda a: min(255, int(a * 0.50))))
    target.alpha_composite(halo_wide, (px, py))

    # Tight silhouette rim in spectral yellow
    rim_px = max(3, (scale // 3) | 1)
    rim = Image.new("RGBA", scaled.size, (255, 255, 119, 255))
    rim_a = mask.filter(ImageFilter.MaxFilter(rim_px * 2 + 1)).filter(ImageFilter.GaussianBlur(1.4))
    rim.putalpha(rim_a.point(lambda a: min(255, int(a * 0.65))))
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

    # Crisp 4-point cross sparkles
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
    """Draws crisp flat 5x7 pixel typography (zero outline, zero gradient)."""
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

    # Subtle deep night-emerald atmospheric glow centered behind right-side sprite (x=1286, y=200)
    sx_center, sy_center = 1286.0, 200.0
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
    BLOCK = 14
    word_y = (H - 7 * BLOCK) // 2  # 151
    sense_x = 176
    sense_w = 29 * BLOCK  # 406
    esp_x = sense_x + sense_w + 84  # 666
    esp_w = 17 * BLOCK  # 238

    # Sprite geometry: 16x16 at 15x = 240x240, centered at (1286, 200) -> x=1166..1406, y=80..320
    SPRITE_SCALE = 15
    sprite_x = 1286 - (16 * SPRITE_SCALE) // 2  # 1166
    sprite_y = (H - 16 * SPRITE_SCALE) // 2  # 80

    # Sparse crisp stars + 4-point glints (avoiding wordmark, frame rails, and sprite core)
    exclude = [
        (sense_x - 24, word_y - 22, esp_x + esp_w + 28, word_y + 7 * BLOCK + 22),
        (sprite_x - 18, sprite_y - 18, sprite_x + 240 + 18, sprite_y + 240 + 18),
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

    # Subtle warm spectral-gold dust motes near the hero sprite
    mote_rng = random.Random(421)
    mote_cols = [(255, 255, 119), (218, 185, 48), (235, 198, 128), (184, 138, 74)]
    for _ in range(18):
        mx = mote_rng.randint(sprite_x - 28, sprite_x + 76)
        my = mote_rng.randint(sprite_y - 16, sprite_y + 62)
        mc = mote_cols[mote_rng.randrange(len(mote_cols))]
        if mote_rng.random() < 0.45:
            d.rectangle([mx, my, mx + 1, my + 1], fill=mc)
        else:
            d.point((mx, my), fill=mc)

    WHITE = (255, 255, 255, 255)
    RAIL = (52, 168, 126, 255)

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

    # 5. Right-side Spectral Eye hero sprite with soft spectral aura
    sprite = build_spectral_eye_16x16()
    composite_sprite_with_aura(im, sprite, scale=SPRITE_SCALE, xy=(sprite_x, sprite_y))

    # 6. Flat two-tone pixel wordmark: warm gold "SENSE" + cool mint-emerald "ESP"
    GOLD_WORD = (217, 179, 106)   # #d9b36a
    MINT_WORD = (134, 232, 190)   # #86e8be
    draw_flat_word(d, "SENSE", sense_x, word_y, BLOCK, GOLD_WORD)
    draw_flat_word(d, "ESP", esp_x, word_y, BLOCK, MINT_WORD)

    OUT_BANNER.parent.mkdir(parents=True, exist_ok=True)
    im.convert("RGB").save(OUT_BANNER)


def generate_icon() -> None:
    """Generates pure transparent vanilla-item icons (512x512 & 128x128, RGBA, NEAREST)."""
    sprite = build_spectral_eye_16x16()
    icon_512 = sprite.resize((512, 512), Image.Resampling.NEAREST)
    icon_128 = sprite.resize((128, 128), Image.Resampling.NEAREST)

    OUT_512.parent.mkdir(parents=True, exist_ok=True)
    OUT_128.parent.mkdir(parents=True, exist_ok=True)
    icon_512.save(OUT_512)
    icon_128.save(OUT_128)


if __name__ == "__main__":
    generate_banner()
    generate_icon()
    print(f"Generated {OUT_BANNER}, {OUT_512}, and {OUT_128}")
