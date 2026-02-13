# OSRSRSPS Codebase Deep Dive

This document is a practical engineering playbook for this repository.
It is written for humans and for weaker automation agents that need explicit, step-by-step instructions.

## 1) What this repo is

This is a monorepo with:
- `server/`: Elvarg-based game server (Java + Kotlin modules `game` and `plugin`)
- `client/`: Java client (317 protocol/client base with custom compatibility patches)
- `server/data/`: server runtime data (definitions, clipping, saves)
- root scripts: `run.ps1`, `run.cmd` for simplified launch/stop

## 2) Fast start and operational commands

From repo root:

```powershell
.\run server   # Start server only
.\run client   # Start client only
.\run all      # Start server then client
.\run stop     # Stop gradle daemons + kill RSPS ports
```

What `run.ps1` does:
- Server runs with `:game:run` and excludes plugin Kotlin compile/jar in this flow.
- Client runs `gradlew run` in `client/`.
- Stop mode kills listeners on `43595` (game) and `43580` (update server).

## 3) High-level architecture

### 3.1 Runtime ownership model

- Server owns:
  - combat logic, NPC behavior, drops, teleports, commands, rights, persistence
  - region clipping for movement/projectile checks (`server/data/clipping`)
- Client owns:
  - rendering, cache decode, map/object/floor visual decode
  - local definitions from cache archives and optional external overrides

### 3.2 Module map

- `server/settings.gradle.kts`:
  - includes `:game`, `:plugin`
- `server/game/build.gradle.kts`:
  - `mainClass = com.elvarg.Server`
  - runtime includes `:plugin`
- `client/build.gradle.kts`:
  - `mainClass = com.runescape.GameWindow`

## 4) Server boot flow (exact wiring)

Entry path:
1. `server/game/src/main/java/com/elvarg/Server.java`
2. `new GameBuilder().initialize()`
3. `EventManager.postAndWait(ServerBootEvent)`
4. `new NetworkBuilder().initialize(NetworkConstants.GAME_PORT)`
5. `EventManager.post(ServerStartedEvent)`

Boot internals:
- `GameBuilder.initialize()`:
  - `Systems.init()`
  - `RegionManager.init()` (loads clipping map_index and map files on demand)
  - background definition loaders:
    - object spawns
    - items
    - shops
    - NPC definitions
    - NPC drops
    - NPC spawns
    - ground items
  - plugin system init (`PluginManager.INSTANCE.init()`)
  - game loop start (`new GameEngine().init()`)

Game loop:
- `GameEngine` runs every `600ms` (from `GameConstants.GAME_ENGINE_PROCESSING_CYCLE_RATE`)
- `World.process()` handles:
  - tasks
  - minigames
  - ground items
  - player/NPC add/remove queues
  - process entities
  - update synchronization and packet flush

## 5) Network and packet pipeline

Connection and decoding:
- `NetworkBuilder` sets Netty bootstrap
- `ChannelEventHandler` receives either:
  - `LoginDetailsMessage` -> `PlayerSession.finalizeLogin(...)`
  - `Packet` -> queued to player session

Login gating:
- `LoginResponses.evaluate(...)` checks:
  - world full
  - updating state
  - name validity
  - duplicates online
  - punishment lists
  - persisted account load/creation

Packet dispatch:
- `PacketConstants.PACKETS[opcode]` maps opcodes to listeners
- `PlayerSession.processPackets()` executes listeners each cycle

Examples:
- commands: opcode `103` -> `CommandPacketListener`
- teleport menu: opcode `183` -> `TeleportPacketListener`
- NPC options/attack: `72/155/131/17/21/18` -> `NPCOptionPacketListener`
- object interactions: `132/252/70/234` -> `ObjectActionPacketListener`

## 6) Core data files and what they control

Server JSON definitions under `server/data/definitions/`:
- `items.json`: server item definitions
- `npc_defs.json`: server NPC stats/flags
- `npc_drops.json`: drop tables
- `npc_spawns.json`: global NPC placement
- `object_spawns.json`: server-spawned objects
- `ground_items.json`: static ground items
- `shops.json`: shop inventories

Server clipping data:
- `server/data/clipping/map_index`
- `server/data/clipping/maps/*.dat`
- loaded by `RegionManager`

Player persistence:
- `GameConstants.PLAYER_PERSISTENCE = JSONFilePlayerPersistence`
- save path: `server/data/saves/characters/<Username>.json`
- includes rights, location, skills, inventory, banks, drop-rate multiplier, etc.

## 7) Area and instance architecture

Base classes:
- `Area`: default behavior (single combat, pvp rules, object hooks)
- `PrivateArea extends Area`: per-instance membership and lifecycle

Manager:
- `AreaManager.process(Mobile)`:
  - keeps entities in `PrivateArea` if still inside boundaries
  - assigns/removes global areas otherwise
  - updates multi-combat icon
  - enforces private-area combat isolation (`attacker.getPrivateArea() == target.getPrivateArea()`)

Private area lifetime:
- `PrivateArea.postLeave(...)` removes entity and destroys area when empty
- destroy removes NPCs, objects, private ground items

## 8) Commands system (`::` commands)

- Packet listener: `CommandPacketListener`
- Registry: `CommandManager.commands`
- Rights gate happens in each command via `canUse(player)`

Important examples already wired:
- `::commands` (player)
- `::droprate` (player)
- `::setdroprate` (developer)
- `::onehit`/`::oneshot` (developer)
- `::id <query>` (developer/owner according to command implementation)

If adding a command:
1. Create class in `server/game/src/main/java/com/elvarg/game/model/commands/impl/`
2. Implement `execute(...)` and `canUse(...)`
3. Register alias in `CommandManager.loadCommands()`
4. Reload command list or restart

## 9) Teleport wiring

Client:
- Teleport UI sends type/index pair

Server:
1. `TeleportPacketListener` reads type + index
2. Matches `Teleportable.values()` by `(type, index)`
3. `TeleportHandler.checkReqs(...)`
4. `TeleportHandler.teleport(...)`

To add or fix a teleport:
- edit `server/game/src/main/java/com/elvarg/game/model/teleportation/Teleportable.java`
- ensure destination has valid map/clipping and object/NPC wiring

## 10) Nightmare implementation map (current)

Core files:
- Area config: `server/game/src/main/java/com/elvarg/game/model/areas/impl/NightmareArea.java`
- Encounter transition: `server/game/src/main/java/com/elvarg/game/content/bosses/nightmare/NightmareEncounter.java`
- Boss NPC: `server/game/src/main/java/com/elvarg/game/entity/impl/npc/impl/Nightmare.java`
- Combat method: `server/game/src/main/java/com/elvarg/game/content/combat/method/impl/npcs/NightmareCombatMethod.java`
- Sanctuary trigger click: `server/game/src/main/java/com/elvarg/net/packet/impl/NPCOptionPacketListener.java`
- Trigger spawn: `server/data/definitions/npc_spawns.json` NPC `9433`
- Client custom NPC render rules: `client/src/main/java/com/runescape/cache/def/NpcDefinition.java`
- Fade overlay interfaces: `client/src/main/java/com/runescape/graphics/widget/Widget.java`

Current positional behavior:
- Suspended wake tile (`9433`) is at `3806,9758,1`
- Instanced spawn now uses the exact wake tile (same tile)

## 11) Client cache architecture and overrides

Client cache root:
- `client/Cache/` (from `Configuration.CACHE_DIRECTORY`)

Main stores:
- `main_file_cache.dat` or `main_file_cache.dat2`
- `main_file_cache.idx0..idx4`

Store usage in this codebase:
- idx1: models
- idx2: animations
- idx3: sounds/midi
- idx4: maps

External file overrides currently supported:
- `client/Cache/map_index` (loaded by `ResourceProvider`)
- `client/Cache/loc.dat` + `client/Cache/loc.idx` (loaded by `ObjectDefinition.init`)
- `client/Cache/flo.dat` (loaded by `FloorDefinition.init`)

Important: client `npc.dat/npc.idx` are loaded from cache archive; there is no direct external `npc.dat` override in this branch.

## 12) Model/NPC/Map import guide (detailed, deterministic)

This is the preferred workflow. Follow exactly.

### 12.1 Preflight safety checklist (do this first)

1. Stop running processes:
```powershell
.\run stop
```
2. Backup target client cache:
```powershell
Copy-Item -Recurse -Force client\Cache client\Cache.backup.
```
3. Backup server clipping:
```powershell
Copy-Item -Recurse -Force server\data\clipping server\data\clipping.backup.
```
4. Verify clean JSON syntax in definitions (especially `npc_defs.json`):
- file must begin with `[` and be a valid array
- no BOM or accidental plain string content

### 12.2 Import model data from OpenRS2 flat cache

Use when donor is OpenRS2 extracted flat format (`cache/7/<id>.dat`).

Command:
```powershell
cd client
.\import_models_from_flat.ps1 -FlatCacheRoot "D:\path\to\flat\cache" -TargetCacheDir "./Cache" -Ids "39070,39182"
```

What it does:
- compiles client classes
- runs `FlatModelImportTool`
- unpacks JS5 container payload
- re-gzips payload for this client format
- writes to target idx1

When to use this:
- you get `Not in GZIP format` or raw container style inputs from donor

### 12.3 Merge model data from another 317-style donor cache

Use when donor already has compatible idx model archives.

Command:
```powershell
cd client
.\merge_models.ps1 -DonorCacheDir "D:\path\to\donor\" -TargetCacheDir "./Cache" -Ids "39182,39184"
```

For OSRS dat2 donor where models are idx7:
```powershell
cd client
./gradlew compileJava
java -cp build/classes/java/main com.runescape.tools.ModelMergeTool "D:\donor\" "D:\CodingProjects\OSRSRSPS\client\Cache\" "39182,39184" 7
```

### 12.4 Import map regions safely (client + server alignment)

Use `MapRegionImportTool` so server clipping and client map_index stay consistent.

Command template:
```powershell
cd client
./gradlew compileJava
java -cp build/classes/java/main com.runescape.tools.MapRegionImportTool `
  "D:\donor\" `
  "D:\CodingProjects\OSRSRSPS\client\Cache\" `
  "D:\CodingProjects\OSRSRSPS\server\data\clipping\map_index" `
  "D:\CodingProjects\OSRSRSPS\server\data\clipping\maps" `
  "14948,14949"
```

This copies:
- selected map archives into client idx4
- same archives into server clipping maps directory
- merged `map_index` into server clipping and client external override

### 12.5 Copy config archives (loc/flo/etc) when needed

Copy full archive ids between cache stores:
```powershell
cd client
./gradlew compileJava
java -cp build/classes/java/main com.runescape.tools.CacheArchiveCopyTool "D:\donor\" "D:\CodingProjects\OSRSRSPS\client\Cache\" 0 "2,5"
```

Extract specific config files for inspection:
```powershell
cd client
./gradlew compileJava
java -cp build/classes/java/main com.runescape.tools.ConfigExtractTool "D:\CodingProjects\OSRSRSPS\client\Cache\" "loc.dat,loc.idx,flo.dat" "D:\temp\config_extract"
```

### 12.6 Add a new NPC end-to-end

Server side:
1. Add server definition in `server/data/definitions/npc_defs.json` with correct:
   - `id`, `name`, `attackable`, `hitpoints`, animations/stats
2. Add spawn in `server/data/definitions/npc_spawns.json`.
3. If custom behavior needed, create class in:
   - `server/game/src/main/java/com/elvarg/game/entity/impl/npc/impl/`
4. Annotate class with `@Ids({...})` so `NPC.create(...)` can instantiate it.
5. Wire combat method in NPC subclass if not default melee.

Client side:
1. Ensure model ids exist in client cache idx1.
2. If NPC id is outside `npc.dat` range, patch `NpcDefinition.lookup(...)` fallback (`applyCustomDefinition`) with:
   - `modelId[]`
   - action menu strings
   - stand/walk/turn animations
3. If render fallback is needed for unstable donor models, add candidate pool logic similar to Nightmare fallback path.

Validation:
- login and spawn area
- right-click option visible
- attack packet triggers
- no `model is incomplete or missing` spam

### 12.7 Add/repair a map area end-to-end

1. Import required regions with `MapRegionImportTool`.
2. Confirm `client/Cache/map_index` exists and includes new region ids.
3. Confirm `server/data/clipping/map_index` contains same region ids.
4. Confirm `server/data/clipping/maps/<archive>.dat` files exist.
5. If object/floor colors are wrong, ensure `client/Cache/loc.dat+loc.idx` and `client/Cache/flo.dat` are valid and not mixed with incompatible revision.
6. Teleport into area and verify:
   - terrain appears (no black void/triangles)
   - object geometry appears at correct plane
   - clipping/movement works

## 13) Troubleshooting matrix

### Symptom: `Failed to unzip model [id] type = 0` + `Not in GZIP format`
Likely cause:
- model archive in idx1 is raw JS5 payload, not gzipped for this client
Fix:
- import with `FlatModelImportTool` or re-gzip payload before writing

### Symptom: `Corrupt map archive skipped [id = X]` + `invalid distance too far back`
Likely cause:
- bad/partial gzip payload for map archive
Fix:
- re-import affected archive from known-good donor
- verify donor/target store index is correct (`idx4` for maps)

### Symptom: world loads with black triangular floor
Likely cause:
- missing/incompatible floor/object config (`flo.dat`, `loc.dat/loc.idx`) or plane mismatch
Fix:
- restore known-good `flo.dat` and `loc.dat/loc.idx`
- ensure map regions are imported from compatible revision set

### Symptom: infinite `Loading - please wait`
Likely cause:
- unresolved map/object archives during load
Fix:
- this client has a 15s fallback in `getMapLoadingState()`; if still stuck, region set is severely inconsistent
- verify region entries and archives exist in map_index + idx4 + server clipping

### Symptom: `invalid opcode: ...` spam from map decode
Likely cause:
- wrong terrain decode mode used for imported region format
Fix:
- verify extended terrain routing (`Client.useExtendedTerrainFormat`)
- avoid mixing archives from incompatible map encodings

### Symptom: magenta leaves / broken object colors
Likely cause:
- incompatible overlay/underlay definitions (`flo.dat`) or object defs mismatch
Fix:
- revert to known-good floor/object config set matching cache

### Symptom: `Expected BEGIN_ARRAY but was STRING` loading `npc_defs.json`
Likely cause:
- JSON file replaced with invalid text/string
Fix:
- restore valid JSON array format in `server/data/definitions/npc_defs.json`

### Symptom: `model is incomplete or missing [type = 1] [id = ...]`
Likely cause:
- missing animation model data / incompatible model references
Fix:
- import missing archives for the correct store
- clamp/disable invalid animation references in defs when needed

## 14) Adding boss content safely (repeatable pattern)

1. Add teleport destination (`Teleportable.java`).
2. Add entry trigger (NPC or object in spawns).
3. Add area class (`Area` or `PrivateArea`) with boundaries + multi setting.
4. Add encounter orchestrator (enter/leave transitions, spawn, cleanup).
5. Add NPC class + combat method.
6. Add client NPC definition patch if id/model not in `npc.dat`.
7. Validate packet interactions:
   - object click opcode route
   - NPC option/attack route
8. Validate drop routing and walkable drop tile logic if boss has special death location.

## 15) Build and validation commands

From root:
```powershell
.\run stop
.\run all
```

Server-only compile sanity:
```powershell
cd server
./gradlew :game:compileJava -x :plugin:compileKotlin
```

Client tool compile:
```powershell
cd client
./gradlew compileJava
```

## 16) AI operator checklist (strict)

When modifying content, follow this order:
1. Read target systems first (listener -> area -> entity -> client def).
2. Change server behavior before client visuals.
3. Keep cache import changes atomic:
   - backup
   - import one group
   - run client
   - verify logs
4. If login fails or world is black:
   - stop immediately
   - restore cache backup
   - re-import in smaller batches
5. Do not blend random donor assets from different revisions without explicit compatibility checks.
6. After stable point, commit with message including:
   - files changed
   - imported archive/model ids
   - tested teleports/bosses

## 17) Most important file index

Server core:
- `server/game/src/main/java/com/elvarg/Server.java`
- `server/game/src/main/java/com/elvarg/game/GameBuilder.java`
- `server/game/src/main/java/com/elvarg/game/World.java`
- `server/game/src/main/java/com/elvarg/net/packet/PacketConstants.java`

Server content wiring:
- `server/game/src/main/java/com/elvarg/net/packet/impl/TeleportPacketListener.java`
- `server/game/src/main/java/com/elvarg/net/packet/impl/NPCOptionPacketListener.java`
- `server/game/src/main/java/com/elvarg/net/packet/impl/ObjectActionPacketListener.java`
- `server/game/src/main/java/com/elvarg/game/model/commands/CommandManager.java`

Nightmare-specific:
- `server/game/src/main/java/com/elvarg/game/model/areas/impl/NightmareArea.java`
- `server/game/src/main/java/com/elvarg/game/content/bosses/nightmare/NightmareEncounter.java`
- `server/game/src/main/java/com/elvarg/game/entity/impl/npc/impl/Nightmare.java`
- `server/game/src/main/java/com/elvarg/game/content/combat/method/impl/npcs/NightmareCombatMethod.java`

Client core/cache:
- `client/src/main/java/com/runescape/GameWindow.java`
- `client/src/main/java/com/runescape/Client.java`
- `client/src/main/java/com/runescape/cache/ResourceProvider.java`
- `client/src/main/java/com/runescape/cache/def/ObjectDefinition.java`
- `client/src/main/java/com/runescape/cache/def/FloorDefinition.java`
- `client/src/main/java/com/runescape/cache/def/NpcDefinition.java`

Client import tools:
- `client/src/main/java/com/runescape/tools/FlatModelImportTool.java`
- `client/src/main/java/com/runescape/tools/ModelMergeTool.java`
- `client/src/main/java/com/runescape/tools/MapRegionImportTool.java`
- `client/src/main/java/com/runescape/tools/CacheArchiveCopyTool.java`
- `client/src/main/java/com/runescape/tools/ConfigExtractTool.java`

Scripts:
- `run.ps1`
- `client/import_models_from_flat.ps1`
- `client/merge_models.ps1`

---

If you follow this guide exactly, you can add new NPCs, new models, and new map regions without destabilizing the whole client/server pair.
