# Changelog

## 1.5.0

- Added **Pulse Ring** and **Spiral** animated 3D highlight styles alongside `Filled`, `Corners`, and `Wireframe`.
- Added **Crosshair Target HUD** inspector card with smooth health interpolation, absorption bar, distance, and armor readout.
- Added **Tactical Mini-Radar** (`48`–`120 px`) with FOV cone, range grid, and category-colored target blips.
- Added **Off-Screen Direction Pointers** around the crosshair for nearby tracked entities outside the camera view.
- Added **Crimson Hit-Flash** (`hurtTime`) and crosshair lock-on highlight accents on both 3D and 2D overlays.
- Upgraded glass panel styling, smooth HP transitions, and absorption bars across nametags, vertical HP bars, and status HUD.
- Expanded `scripts/draw_branding.py` to procedurally generate the pixel-art project banner and HUD layout preview.

## 1.4.0

- Added full support for every stable Minecraft release from `1.21` through `26.3` (`1.21`–`1.21.11`, `26.1`–`26.1.2`, `26.2`, `26.3`).
- Redesigned 2D HUD overlay with bordered nametag cards, category accent bars, vertical health bars, stack counts for dropped items, and smooth distance fade.
- Added toggleable Status HUD pill (`U` by default) showing active mode (`FULL` vs `LOS`) and tracked target count, with corner placement settings.
- Added 3D box style presets (`Filled`, `Wireframe`, `Corners`), configurable line thickness, and fill opacity.
- Added separate filters and custom colors for villagers & tamed pets, boats & minecarts, chests/barrels/hoppers, ender chests, shulker boxes, and spawners/vaults.
- Colored vanilla entity outlines using configured category colors in singleplayer and OP (`level >= 2`) sessions.
- Added keybinds for opening the config screen (`O`) and toggling the status HUD (`U`), keeping `K` to toggle SenseESP.
- Updated mod icon and added English (`en_us`) and Russian (`ru_ru`) translations.
