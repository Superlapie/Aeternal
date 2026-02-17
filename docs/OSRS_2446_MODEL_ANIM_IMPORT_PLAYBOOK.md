# OSRS 2446 Model/Animation Import Playbook

This is the operational guide for importing new OSRS items (models, animations, spotanims, and combat wiring) into this repo without breaking rendering or movement.

Scope:
- Cache source is **2446 only**.
- Covers the exact pipeline used for **Eclipse Atlatl set** and **Voidwaker**.

---

## 1. Core Rule

Use only:
- `_ext/openrs2-2446-flat/cache`
- JSON exports in `_ext/` (e.g. `animations_export_*.json`, `item_*_complete.json`)

Do not mix model/anim/gfx IDs from other revisions.

---

## 2. Where Things Live

Server:
- Item/combat stats + requirements + weapon interface:
  - `server/data/definitions/items.json`
- Special attack mapping:
  - `server/game/src/main/java/com/elvarg/game/content/combat/CombatSpecial.java`
- Special attack behavior:
  - `server/game/src/main/java/com/elvarg/game/content/combat/method/impl/specials/`

Client:
- Item visual overrides:
  - `client/src/main/java/com/runescape/cache/def/ItemDefinition.java`
- Animation defs load + external seq override support:
  - `client/src/main/java/com/runescape/cache/anim/Animation.java`
- Spotanim defs load + external spot override support:
  - `client/src/main/java/com/runescape/cache/anim/Graphic.java`
- Frame group loading (critical for rendering seq frames):
  - `client/src/main/java/com/runescape/cache/anim/Frame.java`
- Model import/probe tools:
  - `client/src/main/java/com/runescape/tools/FlatModelImportTool.java`
  - `client/src/main/java/com/runescape/tools/ModelDecodeProbe.java`

Generated override files:
- `client/Cache/e2446_seq_<id>.dat`
- `client/Cache/e2446_spot_<id>.dat`

Extractor script:
- `extract_2446_eclipse_defs.py`

---

## 3. High-Level Pipeline

1. Gather IDs from 2446 exports (`_ext/*.json`).
2. Ensure models decode in local client cache (`idx1`).
3. Generate seq/spot overrides into `client/Cache` from flat cache + JSON exports.
4. Make client load those IDs safely (Animation/Graphic slot guards + Frame group allowlist).
5. Wire server combat logic and item defs.
6. Validate compile and in-game render.

---

## 4. Model Import Workflow

### 4.1 Import model IDs from 2446 flat cache

From `client/`:

```powershell
.\import_models_from_flat.ps1 -FlatCacheRoot ..\_ext\openrs2-2446-flat\cache -TargetCacheDir .\Cache -Ids "47422,47212,47217,44174"
```

Use the IDs from `item_*_complete.json`:
- inventory model
- male/female equipped models
- any vfx model used by spotanims

### 4.2 Verify decode immediately

```powershell
java -cp build/classes/java/main com.runescape.tools.ModelDecodeProbe .\Cache 47422,47212,47217,44174
```

Expected:
- `OK ...` for each model.

If you see `EOFException`/gzip errors:
- model payload in cache is bad or unimported.
- re-import the IDs with `import_models_from_flat.ps1`.

---

## 5. Sequence/Spotanim Override Workflow

### 5.1 Generate override files

From repo root:

```powershell
python extract_2446_eclipse_defs.py
```

This script currently:
- Extracts raw seq/spot defs from 2446 flat cache.
- Writes `client/Cache/e2446_seq_*.dat` and `e2446_spot_*.dat`.
- Applies JSON-backed sequence payloads for Atlatl and Voidwaker exports.

### 5.2 Current IDs covered by script

Atlatl/Eclipse:
- seq: `10815,10818,10819,11051,11052,11053,11055,11057,11058,11059,11060,11061,11062,11063,11064`
- spot: `2709,2710,2711,2712,2795,2796,2797,2798`

Voidwaker:
- seq: `11240,11275,11463,11464`
- spot: `3017,3030`

---

## 6. Critical Client Rendering Guards

### 6.1 Animation/Graphic slot guards

If IDs are above base cache length, client must expand arrays:
- `Animation.ensureEclipseSlots()`
- `Graphic.ensureEclipseSlots()`

Missing this causes:
- null animations/graphics
- silent no-render behavior

### 6.2 Frame group allowlist (most common blocker)

`Frame.java` only attempts dynamic 2446 frame-group loads for allowed groups.
If a sequence renders nothing, add its frame-group archive ID to:

- `allowed2446Groups` in `Frame.java`

Example added for Voidwaker:
- `4172` (`11464`)
- `4173` (`11463`)
- `4275` (`11275`)
- `4276` (`11240`)

Symptoms when missing:
- spec logic executes but animation/gfx appears absent.

---

## 7. Client Item Override Workflow

For custom IDs or extended items, add item switch cases in:
- `client/src/main/java/com/runescape/cache/def/ItemDefinition.java`

Set at minimum:
- `name`
- `actions[1]` (`Wield`/`Wear`)
- `inventory_model`
- equipped models
- display transforms (`modelZoom`, `rotation_x`, `rotation_y`, offsets) if icon looks wrong

Example done for Voidwaker:
- item `27690` + note `27691`

---

## 8. Server Wiring Workflow

1. Add/update item in `server/data/definitions/items.json`:
- `weaponInterface`
- `equipmentType`
- `doubleHanded`
- `bonuses`
- `requirements`

2. Register special in `CombatSpecial.java`:
- item ID
- drain %
- multipliers
- combat method class
- expected weapon interface

3. Implement combat method in `.../specials/`:
- start animation
- cast/impact gfx
- damage behavior

4. Add to spawn list if needed:
- `GameConstants.ALLOWED_SPAWNS`

---

## 9. Known Working IDs (Current State)

Eclipse Atlatl:
- weapon: `29000`
- dart: `28991`
- atlatl attack/special seqs: `11057`, `11060` (+ related vfx seqs/spotanims)

Eclipse proc/burn visuals:
- spotanims in use include `2710`, `2711`, `2712` (depending on chosen behavior)
- burn tick hitsplat configured server-side as yellow in current implementation

Voidwaker:
- weapon: `27690`
- spec animation: `11275`
- spec cast gfx: `3030`
- spec impact gfx: `3017`

---

## 10. Build/Validation Commands

Server compile:

```powershell
cd server
.\gradlew.bat :game:compileJava -x :plugin:compileKotlin -x :plugin:jar
```

Client compile:

```powershell
cd client
.\gradlew.bat compileJava
```

Run both from root:

```powershell
.\run stop
.\run all
```

---

## 11. Troubleshooting Matrix

### Item invisible in inventory
- Check model import + decode probe.
- Check client `ItemDefinition` override has correct `inventory_model`.

### Item wearable but no/wrong animation
- Confirm server sends correct anim ID.
- Confirm `e2446_seq_<id>.dat` exists.
- Confirm `Animation.ensureEclipseSlots()` includes that ID.
- Confirm `Frame.allowed2446Groups` includes the sequence’s frame group.

### GFX not rendering
- Confirm `e2446_spot_<id>.dat` exists.
- Confirm spot’s model ID is imported/decodable.
- Confirm `Graphic.ensureEclipseSlots()` includes that spot ID.

### Movement/idle breaks (floating/disfigured)
- Usually caused by bad render animation IDs or invalid seq overrides.
- Revert to known-good render anim set, then reintroduce IDs one-by-one.

### Black-screen/login stalls after animation edits
- Often from invalid overrides or corrupted model payloads.
- Re-run extractor/import tools and validate decode/compile before launching.

---

## 12. Recommended Safe Process for New OSRS Item

1. Parse `item_*_complete.json` for model IDs + transforms.
2. Import all required models and run decode probe.
3. Add client item override and verify inventory icon first.
4. Add seq/spot IDs to extractor + regenerate `e2446_*` files.
5. Add Animation/Graphic slot guards and Frame allowlist groups.
6. Wire server item defs and combat method.
7. Compile server/client.
8. Test in-game in this order:
   - inventory icon
   - equip visuals
   - idle/walk/run
   - normal attack
   - special attack

