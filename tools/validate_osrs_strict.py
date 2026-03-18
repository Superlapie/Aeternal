#!/usr/bin/env python3
import json
from collections import Counter, defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SPAWNS = ROOT / 'server' / 'data' / 'definitions' / 'npc_spawns.json'
DEFS = ROOT / 'server' / 'data' / 'definitions' / 'npc_defs.json'

# OSRS Wiki anchor checks (tile-accurate anchors only)
ANCHORS = [
    {
        'name': 'Lumbridge Guide',
        'id': 306,
        'x': 3238,
        'y': 3220,
        'z': 0,
        'source': 'https://oldschool.runescape.wiki/w/Lumbridge_Guide'
    },
    {
        'name': 'Hans',
        'id': 3077,
        'x': 3212,
        'y': 3219,
        'z': 0,
        'tolerance': 12,
        'source': 'https://oldschool.runescape.wiki/w/Hans'
    },
]

REQUIRED_FISHING_IDS = [1497,1498,1499,1500,1506,1508,1509,1511,1512,3913,3914,3915,4079,4080,4081,4082]


def manhattan(a, b):
    return abs(a['x'] - b['x']) + abs(a['y'] - b['y'])


def main():
    spawns = json.loads(SPAWNS.read_text())
    defs = {d['id']: d.get('name', f"id:{d['id']}") for d in json.loads(DEFS.read_text())}

    exact = Counter((e['id'], e['position']['x'], e['position']['y'], e['position']['z']) for e in spawns)
    exact_dup_rows = sum(v - 1 for v in exact.values() if v > 1)

    same_name_tile = defaultdict(int)
    for e in spawns:
        p = e['position']
        same_name_tile[(defs.get(e['id'], str(e['id'])), p['x'], p['y'], p['z'])] += 1
    same_name_dup_rows = sum(v - 1 for v in same_name_tile.values() if v > 1)

    missing_defs = sum(1 for e in spawns if e['id'] not in defs)

    print('== OSRS STRICT VALIDATION ==')
    print(f'spawn_total={len(spawns)}')
    print(f'exact_duplicate_rows={exact_dup_rows}')
    print(f'same_name_same_tile_rows={same_name_dup_rows}')
    print(f'spawns_missing_definition={missing_defs}')

    print('\n-- Anchor checks --')
    ok = True
    for a in ANCHORS:
        candidates = [e for e in spawns if e['id'] == a['id'] and e['position']['z'] == a['z']]
        if not candidates:
            ok = False
            print(f"FAIL: {a['name']} ({a['id']}) missing entirely | {a['source']}")
            continue
        if 'tolerance' in a:
            best = min(manhattan(e['position'], a) for e in candidates)
            if best <= a['tolerance']:
                print(f"PASS: {a['name']} within tolerance {a['tolerance']} (best={best})")
            else:
                ok = False
                print(f"FAIL: {a['name']} too far from anchor (best={best}, tol={a['tolerance']}) | {a['source']}")
        else:
            hit = any(e['position']['x'] == a['x'] and e['position']['y'] == a['y'] for e in candidates)
            if hit:
                print(f"PASS: {a['name']} exact tile ({a['x']},{a['y']},{a['z']})")
            else:
                ok = False
                print(f"FAIL: {a['name']} missing exact tile ({a['x']},{a['y']},{a['z']}) | {a['source']}")

    print('\n-- Fishing spot id coverage --')
    c = Counter(e['id'] for e in spawns)
    missing = [i for i in REQUIRED_FISHING_IDS if c[i] == 0]
    if missing:
        ok = False
        print('FAIL: missing fishing IDs:', missing)
    else:
        print('PASS: all required fishing IDs present')

    print('\nresult=' + ('PASS' if ok else 'FAIL'))


if __name__ == '__main__':
    main()
