import json
from pathlib import Path

ROOT = Path(r"d:/CodingProjects/OSRSRSPS")
OSRS_NPCS = ROOT / "osrs_npcs.json"
NPC_DEFS = ROOT / "server/data/definitions/npc_defs.json"
SPAWNS_OUT = ROOT / "server/data/definitions/npc_spawns.json"
CLIP_MAPS = ROOT / "server/data/clipping/maps"


def to_facing(direction: int) -> str:
    # OSRS yaw: 0=S, 512=W, 1024=N, 1536=E.
    sector = ((int(direction) + 128) % 2048) // 256
    return {
        0: "SOUTH",
        1: "SOUTH_WEST",
        2: "WEST",
        3: "NORTH_WEST",
        4: "NORTH",
        5: "NORTH_EAST",
        6: "EAST",
        7: "SOUTH_EAST",
    }[sector]


def region_id(x: int, y: int) -> int:
    return ((x >> 6) << 8) | (y >> 6)


def main() -> None:
    defs = json.loads(NPC_DEFS.read_text(encoding="utf-8"))
    valid_ids = {int(d["id"]) for d in defs if isinstance(d, dict) and "id" in d}

    valid_regions = set()
    for f in CLIP_MAPS.glob("*.dat"):
        try:
            valid_regions.add(int(f.stem))
        except ValueError:
            pass

    entries = json.loads(OSRS_NPCS.read_text(encoding="utf-8"))

    out = []
    seen = set()
    stats = {
        "total": 0,
        "kept": 0,
        "skip_bad": 0,
        "skip_id": 0,
        "skip_region": 0,
        "skip_coords": 0,
        "skip_plane": 0,
        "dupes": 0,
    }

    for e in entries:
        stats["total"] += 1
        try:
            npc_id = int(e.get("id"))
            x = int(e.get("x"))
            y = int(e.get("y"))
            z = int(e.get("z", e.get("p", 0)))
            radius = int(e.get("radius", 0))
            direction = int(e.get("direction", 0))
        except Exception:
            stats["skip_bad"] += 1
            continue

        # 317 client limit and existing defs only.
        if npc_id < 0 or npc_id > 8195 or npc_id not in valid_ids:
            stats["skip_id"] += 1
            continue

        if z < 0 or z > 3:
            stats["skip_plane"] += 1
            continue

        if x < 0 or y < 0 or x >= 16384 or y >= 16384:
            stats["skip_coords"] += 1
            continue

        rid = region_id(x, y)
        if rid not in valid_regions:
            stats["skip_region"] += 1
            continue

        rec = {
            "facing": to_facing(direction),
            "radius": max(0, min(radius, 5)),
            "id": npc_id,
            "position": {"x": x, "y": y, "z": z},
            "description": e.get("name", "OSRS import"),
        }

        key = (rec["id"], x, y, z, rec["facing"], rec["radius"])
        if key in seen:
            stats["dupes"] += 1
            continue

        seen.add(key)
        out.append(rec)
        stats["kept"] += 1

    SPAWNS_OUT.write_text(json.dumps(out, indent=4), encoding="utf-8")

    print("Rebuilt npc_spawns.json")
    for k, v in stats.items():
        print(f"{k}: {v}")


if __name__ == "__main__":
    main()
