# World Content Audit

Generated: 2026-03-18T07:25:29.400Z

## Spawn Snapshot
- NPC spawns: **5236**
- Unique spawned NPC IDs: **1267**
- Object spawns (dynamic): **69034**
- Missing NPC IDs (from `server/npcnames.txt`): **6931**

## Core Presence Counts
- Goblin: **364**
- Cow: **126**
- Cow calf: **25**
- Chicken: **113**
- Fishing spot: **45**
- Banker: **71**
- Man: **119**
- Woman: **29**
- Guard: **195**

## Fishing Spot Spawn Coverage
| NPC ID | Name | Spawn Count |
|---:|---|---:|
| 1497 | Fishing spot | 0 |
| 1498 | Fishing spot | 0 |
| 1499 | Fishing spot | 0 |
| 1500 | Fishing spot | 0 |
| 1506 | Fishing spot | 2 |
| 1508 | Fishing spot | 0 |
| 1509 | Fishing spot | 0 |
| 1511 | Fishing spot | 0 |
| 1512 | Fishing spot | 0 |
| 3913 | Fishing spot | 3 |
| 3914 | Fishing spot | 1 |
| 3915 | Fishing spot | 4 |
| 4079 | Fishing spot | 0 |
| 4080 | Fishing spot | 0 |
| 4081 | Fishing spot | 0 |
| 4082 | Fishing spot | 2 |

## Notes
- Source merges in this branch: Rune-Server OSRS dump, rsmod Lumbridge subset, Elvarg fork delta, Elderscape OSRS spawn files.
- Imports are ID-validated against your npc definitions and deduped by exact (id,x,y,z).
- Mining rocks are mostly map-embedded objects in cache and handled by mining registry/object interaction logic.
- Full missing NPC list is in `docs/MISSING_NPC_SPAWNS.md`.
