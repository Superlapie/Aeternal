# OSRS Strict Validation (2026-03-18)

## Scope
- Strict source-backed validation pass for high-impact starter-area NPC accuracy and duplicate integrity.
- Target files: `server/data/definitions/npc_spawns.json`.

## OSRS Source Anchors
- Hans: https://oldschool.runescape.wiki/w/Hans
  - Wiki map anchor includes `3212,3219` (Lumbridge Castle courtyard area).
- Lumbridge Guide: https://oldschool.runescape.wiki/w/Lumbridge_Guide
  - Wiki map anchor explicitly shows `x=3238`, `y=3220`.

## Changes Applied
1. Removed duplicate/incorrect Lumbridge Hans imports:
   - Removed `(id=3077, x=3219, y=3212, z=0)`
   - Removed `(id=3077, x=3219, y=3224, z=0)`
   - Kept canonical Lumbridge Hans `(id=3077, x=3222, y=3221, z=0)`

2. Replaced off-location Lumbridge Guide imports with OSRS-anchored placement:
   - Removed `(id=306, x=3231, y=3232, z=0)`
   - Removed `(id=306, x=3232, y=3232, z=0)`
   - Added `(id=306, x=3238, y=3220, z=0, radius=3)`

3. Fixed malformed coordinate typo:
   - Updated Giant spider `(id=3018)` from `x=217, y=9890` to `x=3217, y=9890`

## Post-Validation Integrity Checks
- Exact duplicate spawn entries `(id,x,y,z)`: `0`
- Same-name same-tile overlaps: `1`
  - Remaining overlap is a fishing-spot pair on one tile (intentional multi-spot compatibility).
- Missing NPC definition references in spawns: `0`
- Required fishing spot IDs present: all required IDs present.

## Notes
- This pass is strict where OSRS Wiki coordinate anchors are explicit.
- For NPCs/pages without explicit per-spawn coordinates, validation remains name/area-level until a tile-accurate source dataset is imported.
