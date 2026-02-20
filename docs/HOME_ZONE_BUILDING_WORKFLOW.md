# Home-Zone Building Workflow

This is the exact workflow used to build and iterate the custom home-zone building in the blank expanded map area.

## 1) Copy a real building pattern
- Source region was decoded from existing server clipping object archives (`server/data/clipping/maps/*.dat`).
- A complete building footprint was selected from existing map data.
- Object set was copied with original:
  - `id`
  - `type` (wall/decoration/ground-object shape)
  - `face` (rotation)
- The footprint was translated into home-zone coordinates and injected into:
  - `server/data/definitions/object_spawns.json`

Why this works:
- Real structures in OSRS rely on mixed object types (`0/2/3/10/22` etc), not only `type=10`.
- Preserving those types/rotations is what makes the structure look and behave like an authentic building.

## 2) Keep the process reproducible
- Script: `tools/home_zone_layout_apply.py`
- Purpose:
  - Applies current home-zone layout updates safely.
  - Moves Portal Nexus beside the building.
  - Adds interior magical accents.
  - Adds corner pillars and grave perimeter.
  - Removes object id `158` models in home-zone range.

Run:
```powershell
python tools/home_zone_layout_apply.py
```

## 3) Bank booth reliability (global hardening)
- Added a universal "bank booth/chest" interaction normalizer in:
  - `server/game/src/main/java/com/elvarg/game/definition/ObjectDefinition.java`
  - `client/src/main/java/com/runescape/cache/def/ObjectDefinition.java`
- Added centralized bank open fallback in:
  - `server/game/src/main/java/com/elvarg/net/packet/impl/ObjectActionPacketListener.java`

Result:
- Any object definition that resolves as bank-like now consistently exposes and handles `Bank` interaction.

## 4) Validation checklist
- JSON parses cleanly.
- Nexus object id (`11357`) exists at intended coordinates.
- Building footprint remains in target area.
- No portal-frame/portal-space objects are introduced.
- Home-zone edits stay inside the intended local area around home teleport.
