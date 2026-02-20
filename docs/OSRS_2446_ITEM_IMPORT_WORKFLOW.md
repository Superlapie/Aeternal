# OSRS 2446 Item Import Workflow (Client + Server)

This is the exact workflow used for Noxious/Araxyte/Rancour in this repo.

## 1) Source of truth

Use only:
- `_ext/openrs2-2446-flat/cache`
- `_ext/item_*_complete.json`

Do not manually guess IDs if JSON exists.

## 2) Extract item data from JSON

For each item, copy:
- item id
- inventory model id
- male/female wear model ids
- note/placeholder ids
- 2D icon transforms (`zoom2d`, `xan2d`, `yan2d`, `zan2d`, `xOffset2d`, `yOffset2d`)
- actions (`Wear`/`Wield`/material `Take`)

## 3) Import models into client cache (idx1)

From `client/`:

```powershell
.\import_models_from_flat.ps1 -FlatCacheRoot ..\_ext\openrs2-2446-flat\cache -TargetCacheDir .\Cache -Ids "comma,separated,model,ids"
```

## 4) Client item overrides

Edit:
- `client/src/main/java/com/runescape/cache/def/ItemDefinition.java`

Add switch cases for:
- real item id
- note id (if present)
- placeholder id (if present)

Set exact model IDs/transforms from JSON.  
For materials: base from a simple item and keep `Take` on ground action.  
For weapons/armor: base from similar existing item, then override models/transforms explicitly.

## 5) Server item definitions

Edit:
- `server/data/definitions/items.json`

Add/replace entries for all related IDs:
- main item
- note
- placeholder
- component materials

For weapons, include:
- `weaponInterface`
- `equipmentType`
- bonuses/requirements
- attack block/movement animations

## 6) Combat wiring (if weapon has custom behavior)

Files:
- `server/game/src/main/java/com/elvarg/game/entity/impl/player/Player.java`
- `server/game/src/main/java/com/elvarg/game/content/combat/CombatSpecial.java`
- `server/game/src/main/java/com/elvarg/game/content/combat/method/impl/specials/`

Typical changes:
- weapon-specific attack speed override
- weapon-specific attack animation override
- special registration (drain %, interface)
- special combat method class (anim/gfx/effects)

## 7) 2446 sequence/spot overrides for client

Edit:
- `extract_2446_eclipse_defs.py`

Add needed IDs to:
- `SEQ_IDS`
- `SPOT_IDS`

Generate files:

```powershell
python extract_2446_eclipse_defs.py
```

This writes:
- `client/Cache/e2446_seq_<id>.dat`
- `client/Cache/e2446_spot_<id>.dat`

## 8) Enable sequence IDs in client

Edit:
- `client/src/main/java/com/runescape/cache/anim/Animation.java`

Add sequence IDs to:
- `ALLOWED_2446_OVERRIDES`
- `ensureEclipseSlots()` id list

## 9) Frame-group loading (critical)

Edit:
- `client/src/main/java/com/runescape/cache/anim/Frame.java`

If animations render as no-op, add their frame groups to `allowed2446Groups` (for groups >= 4000).

Notes:
- low groups (< 4000) are alias-remapped by `Animation.remapLowFrameGroupsToAliases`.
- high groups (> 4000) usually need explicit allowlist.

## 10) Skeletal/maya sequence fallback rule

Some OSRS IDs are skeletal stubs in this client path (no classic frame table).  
Symptom: sequence id exists but no visible movement/animation.

Fix:
1. inspect nearby sequence IDs in 2446 archive,
2. choose classic-playable replacements,
3. wire server movement anims to those fallback IDs.

Example used for Noxious halberd:
- requested movement: `11591/11593/11595` (skeletal stub here)
- final stable movement used: base halberd rig `809/1146/1210` from server item defs (no floating)

## 11) Special-animation compatibility rule (important)

Do not assume a 2446 sequence is valid for player-body playback in this client.

Observed with Nox:
- `11516` (`vfx_noxious_halberd_spec`) and `11587` produced warped playback / idle contamination
- `11524` is a crafting/fletching-style human sequence, not the combat special pose

Final stable spec behavior:
- use a known-compatible halberd player anim for the special start (`1203`)
- keep custom impact graphic if available (`2914`)

If a "real" 2446 special anim warps:
1. keep the real impact gfx/model path,
2. swap only the attacker animation to a compatible base anim,
3. never ship warped anims just because IDs exist.

## 12) Spotanim dependency rule

Spotanims can depend on sequence IDs that are not otherwise loaded.

Example:
- spot `2914` depends on anim `11455`
- if `11455` is missing, client logs:
  - `Skipped invalid 2446 spotanim override 2914 (missing anim 11455)`

Fix:
- include both IDs in extraction and allowlists:
  - `extract_2446_eclipse_defs.py`: `SPOT_IDS += 2914`, `SEQ_IDS += 11455`
  - `Animation.java`: include `11455` in `ALLOWED_2446_OVERRIDES` and `ensureEclipseSlots`

## 13) Client safety guard for incoming animation masks

Player update masks may send animation IDs that are in range but unresolved (`null` entry in `Animation.animations[]`).

Without guard this crashes in:
- `Client.appendPlayerUpdateMask(...)`

Required fix:
- validate both incoming and current animation IDs are non-null before reading `replayMode` / `forcedPriority`
- if invalid, coerce to `-1`

NPC path already had this safety pattern; player path must match it.

## 14) Cache override hygiene

After changing extraction lists, stale generated overrides can force old behavior.

Always clean/recreate when troubleshooting:
- `client/Cache/e2446_seq_*.dat`
- `client/Cache/e2446_spot_*.dat`

And verify map index integrity if maps/models suddenly regress:
- `client/Cache/map_index`
- `server/data/clipping/map_index`

Corrupted map index symptoms include:
- missing/corrupt map archive logs
- unrelated visual regressions.

## 15) Build/verify

Client:
```powershell
cd client
.\gradlew.bat compileJava
```

Server:
```powershell
cd server
.\gradlew.bat :game:compileJava -x :plugin:compileKotlin -x :plugin:jar
```

Then run game and validate:
- inventory sprite
- ground item model
- equipped model
- attack animation
- special animation/gfx
- stand/walk/run
