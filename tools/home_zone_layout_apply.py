import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SPAWNS_PATH = ROOT / "server" / "data" / "definitions" / "object_spawns.json"

# Home-zone center/range
HOME_CENTER_X = 5856
HOME_CENTER_Y = 5920
HOME_RADIUS = 75  # 150x150 total area

# Existing copied building footprint
BUILD_MIN_X, BUILD_MAX_X = 5847, 5864
BUILD_MIN_Y, BUILD_MAX_Y = 5937, 5954


def within_home_zone(entry):
    x = entry["position"]["x"]
    y = entry["position"]["y"]
    z = entry["position"]["z"]
    return (
        z == 0
        and HOME_CENTER_X - HOME_RADIUS <= x <= HOME_CENTER_X + HOME_RADIUS
        and HOME_CENTER_Y - HOME_RADIUS <= y <= HOME_CENTER_Y + HOME_RADIUS
    )


def key(entry):
    p = entry["position"]
    return (entry["id"], entry["type"], entry["face"], p["x"], p["y"], p["z"])


def spawn(obj_id, x, y, z=0, typ=10, face=0):
    return {"face": face, "type": typ, "id": obj_id, "position": {"x": x, "y": y, "z": z}}


def main():
    entries = json.loads(SPAWNS_PATH.read_text())

    # 1) Remove all id 158 models from home-zone scope.
    entries = [e for e in entries if not (within_home_zone(e) and e["id"] == 158)]

    # 2) Clear old portal-nexus cluster from its old south location.
    old_portal_min_x, old_portal_max_x = 5848, 5864
    old_portal_min_y, old_portal_max_y = 5890, 5914
    entries = [
        e
        for e in entries
        if not (
            e["position"]["z"] == 0
            and old_portal_min_x <= e["position"]["x"] <= old_portal_max_x
            and old_portal_min_y <= e["position"]["y"] <= old_portal_max_y
            and e["id"] in {11357, 2004, 5072, 7016, 7017, 4090, 4091, 4092, 5558}
        )
    ]

    additions = []

    # Remove explicit props the latest pass no longer wants.
    entries = [
        e for e in entries
        if not (
            within_home_zone(e)
            and (
                e["id"] == 611  # picnic bench
                or e["id"] == 9355  # undead combat dummy
                or (e["id"] == 159 and e["position"]["x"] == 5866 and e["position"]["y"] == 5958)
                or (
                    e["id"] in {7017, 5072}
                    and 5864 <= e["position"]["x"] <= 5872
                    and 5942 <= e["position"]["y"] <= 5952
                )
            )
        )
    ]

    # 3) Move nexus next to the building (east side).
    nx, ny = 5868, 5946
    additions.extend(
        [
            spawn(11357, nx, ny),
            spawn(2004, nx - 2, ny),
            spawn(7016, nx, ny - 2),
            spawn(4090, nx - 2, ny - 2),
            spawn(4091, nx + 2, ny - 2),
            spawn(4092, nx, ny - 4),
            spawn(5558, nx, ny + 3),
        ]
    )

    # Clear any old corner pillar variants near the building.
    entries = [
        e for e in entries
        if not (
            e["position"]["z"] == 0
            and e["id"] in {6283, 6284, 6285, 6286}
            and BUILD_MIN_X - 3 <= e["position"]["x"] <= BUILD_MAX_X + 3
            and BUILD_MIN_Y - 3 <= e["position"]["y"] <= BUILD_MAX_Y + 3
        )
    ]

    # 4) Uniform corner pillars: id 6283, 2 tiles out from each corner.
    additions.extend(
        [
            spawn(6283, BUILD_MIN_X - 2, BUILD_MIN_Y - 2),
            spawn(6283, BUILD_MAX_X + 2, BUILD_MIN_Y - 2),
            spawn(6283, BUILD_MIN_X - 2, BUILD_MAX_Y + 2),
            spawn(6283, BUILD_MAX_X + 2, BUILD_MAX_Y + 2),
        ]
    )

    # Remove old crystal props from interior.
    entries = [
        e for e in entries
        if not (
            e["position"]["z"] == 0
            and BUILD_MIN_X <= e["position"]["x"] <= BUILD_MAX_X
            and BUILD_MIN_Y <= e["position"]["y"] <= BUILD_MAX_Y
            and e["id"] in {589, 2459, 9751}
        )
    ]

    # 5) Interior magic/aura accents (keep existing interior, add only).
    additions.extend(
        [
            spawn(11141, 5855, 5945),  # Magic spell
            spawn(908, 5851, 5943),    # Magical symbol
            spawn(3361, 5860, 5949),   # Orb of Light
        ]
    )

    # 6) Semi-surrounding grave belt with mixed grave styles.
    grave_set = [
        (400, 5845, 5936), (401, 5849, 5936), (402, 5853, 5936), (403, 5857, 5936), (404, 5861, 5936),
        (405, 5865, 5936), (406, 5845, 5941), (9354, 5865, 5941), (9357, 5845, 5946), (9356, 5865, 5946),
        (9359, 5845, 5951), (9360, 5865, 5951), (14765, 5849, 5956), (14766, 5853, 5956), (14767, 5857, 5956),
        (14768, 5861, 5956), (28434, 5846, 5955), (28439, 5864, 5955), (10049, 5850, 5955), (10053, 5860, 5955),
    ]
    additions.extend(spawn(obj_id, x, y) for (obj_id, x, y) in grave_set)

    seen = {key(e) for e in entries}
    for e in additions:
        k = key(e)
        if k not in seen:
            entries.append(e)
            seen.add(k)

    SPAWNS_PATH.write_text(json.dumps(entries, indent=4) + "\n")
    print(f"Updated {SPAWNS_PATH}")
    print(f"Total spawns: {len(entries)}")


if __name__ == "__main__":
    main()
