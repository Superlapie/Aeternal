#!/usr/bin/env python3
"""
Reusable OpenRS2 2446 flat-cache NPC parser/importer.

Features:
1) Parses index 2 / archive 9 NPC definitions from a flat cache.
2) Finds NPCs by ids and/or keyword match on decoded names.
3) Exports decoded NPC defs to JSON.
4) Optionally imports referenced model ids into the client cache via FlatModelImportTool.

Usage examples:
  python tools/osrs2446_npc_tool.py --find-keywords thrall ghost skeleton zombie
  python tools/osrs2446_npc_tool.py --ids 10864,10865,10866 --export _tmp/npc_ids.json
  python tools/osrs2446_npc_tool.py --find-keywords thrall --import-models
"""

from __future__ import annotations

import argparse
import bz2
import gzip
import json
import struct
import subprocess
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, List, Optional, Tuple


REPO_ROOT = Path(__file__).resolve().parents[1]
DEFAULT_FLAT = REPO_ROOT / "_ext" / "openrs2-2446-flat" / "cache"
DEFAULT_CLIENT_CACHE = REPO_ROOT / "client" / "Cache"
DEFAULT_EXPORT = REPO_ROOT / "_tmp" / "npc_2446_scan.json"


@dataclass
class Reader:
    data: bytes
    pos: int = 0

    def u8(self) -> int:
        if self.pos >= len(self.data):
            raise IndexError("u8 past end")
        v = self.data[self.pos]
        self.pos += 1
        return v

    def u16(self) -> int:
        if self.pos + 2 > len(self.data):
            raise IndexError("u16 past end")
        v = struct.unpack_from(">H", self.data, self.pos)[0]
        self.pos += 2
        return v

    def i32(self) -> int:
        if self.pos + 4 > len(self.data):
            raise IndexError("i32 past end")
        v = struct.unpack_from(">i", self.data, self.pos)[0]
        self.pos += 4
        return v

    def big_smart(self) -> int:
        if self.pos >= len(self.data):
            raise IndexError("big_smart past end")
        if self.data[self.pos] < 128:
            return self.u16()
        return self.i32() & 0x7FFFFFFF


def decompress_js5_container(container: bytes) -> bytes:
    if len(container) < 5:
        raise ValueError("container too short")
    ctype = container[0]
    clen = struct.unpack_from(">I", container, 1)[0]
    if ctype == 0:
        return container[5:5 + clen]
    if len(container) < 9:
        raise ValueError("compressed container too short")
    block = container[9:9 + clen]
    if ctype == 1:
        return bz2.decompress(b"BZh1" + block)
    if ctype == 2:
        return gzip.decompress(block)
    raise ValueError(f"unsupported JS5 compression type {ctype}")


def load_index2_reference(flat_cache_root: Path) -> Dict[int, List[int]]:
    ref_container = (flat_cache_root / "255" / "2.dat").read_bytes()
    ref = decompress_js5_container(ref_container)

    r = Reader(ref)
    protocol = r.u8()
    if protocol < 5 or protocol > 7:
        raise ValueError(f"unsupported index protocol {protocol}")
    if protocol >= 6:
        _ = r.i32()  # index revision
    flags = r.u8()
    named = (flags & 1) != 0
    sized = (flags & 4) != 0

    archive_count = r.big_smart() if protocol >= 7 else r.u16()
    archive_ids: List[int] = []
    last = 0
    for _ in range(archive_count):
        delta = r.big_smart() if protocol >= 7 else r.u16()
        last += delta
        archive_ids.append(last)

    if named:
        for _ in range(archive_count):
            _ = r.i32()
    for _ in range(archive_count):
        _ = r.i32()  # crc
    if sized:
        for _ in range(archive_count):
            _ = r.i32()
            _ = r.i32()
    for _ in range(archive_count):
        _ = r.i32()  # revision

    file_counts = [r.big_smart() if protocol >= 7 else r.u16() for _ in range(archive_count)]
    archive_files: Dict[int, List[int]] = {}
    for idx, aid in enumerate(archive_ids):
        cnt = file_counts[idx]
        files: List[int] = []
        last_f = 0
        for _ in range(cnt):
            delta = r.big_smart() if protocol >= 7 else r.u16()
            last_f += delta
            files.append(last_f)
        archive_files[aid] = files
    return archive_files


def split_group_files(group_data: bytes, file_ids: List[int]) -> Dict[int, bytes]:
    if len(file_ids) == 1:
        return {file_ids[0]: group_data}

    files_count = len(file_ids)
    chunks = group_data[-1]
    table_pos = len(group_data) - 1 - chunks * files_count * 4
    table = Reader(group_data[table_pos:len(group_data) - 1])

    chunk_sizes = [[0] * chunks for _ in range(files_count)]
    file_sizes = [0] * files_count
    for c in range(chunks):
        cumulative = 0
        for f in range(files_count):
            delta = table.i32()
            cumulative += delta
            chunk_sizes[f][c] = cumulative
            file_sizes[f] += cumulative

    file_data = [bytearray(sz) for sz in file_sizes]
    file_offsets = [0] * files_count
    pos = 0
    for c in range(chunks):
        for f in range(files_count):
            sz = chunk_sizes[f][c]
            file_data[f][file_offsets[f]:file_offsets[f] + sz] = group_data[pos:pos + sz]
            file_offsets[f] += sz
            pos += sz

    return {fid: bytes(file_data[idx]) for idx, fid in enumerate(file_ids)}


def rs_string(r: Reader) -> str:
    out: List[str] = []
    while True:
        b = r.u8()
        if b == 0:
            break
        out.append(chr(b))
    return "".join(out)


def parse_npc_def(blob: bytes) -> Dict[str, object]:
    r = Reader(blob)
    out: Dict[str, object] = {}
    while r.pos < len(blob):
        op = r.u8()
        if op == 0:
            break
        try:
            if op == 1:
                n = r.u8()
                out["models"] = [r.u16() for _ in range(n)]
            elif op == 2:
                out["name"] = rs_string(r)
            elif op == 12:
                out["size"] = r.u8()
            elif op == 13:
                out["standAnim"] = r.u16()
            elif op == 14:
                out["walkAnim"] = r.u16()
            elif op == 15:
                out["turnLeftAnim"] = r.u16()
            elif op == 16:
                out["turnRightAnim"] = r.u16()
            elif op == 17:
                out["walkAnim"] = r.u16()
                out["walkBackAnim"] = r.u16()
                out["walkLeftAnim"] = r.u16()
                out["walkRightAnim"] = r.u16()
            elif op == 18:
                out["category"] = r.u16()
            elif 30 <= op < 35:
                out[f"action{op - 30}"] = rs_string(r)
            elif op == 40:
                n = r.u8()
                out["recol"] = [[r.u16(), r.u16()] for _ in range(n)]
            elif op == 41:
                n = r.u8()
                out["retex"] = [[r.u16(), r.u16()] for _ in range(n)]
            elif op == 60:
                n = r.u8()
                out["chatModels"] = [r.u16() for _ in range(n)]
            elif op == 93:
                out["minimapDot"] = False
            elif op == 95:
                out["combatLevel"] = r.u16()
            elif op == 97:
                out["scaleXZ"] = r.u16()
            elif op == 98:
                out["scaleY"] = r.u16()
            elif op == 99:
                out["priorityRender"] = True
            elif op == 100:
                out["lightModifier"] = struct.unpack(">b", bytes([r.u8()]))[0]
            elif op == 101:
                out["shadowModifier"] = struct.unpack(">b", bytes([r.u8()]))[0]
            elif op == 102:
                out["headIcon"] = r.u16()
            elif op == 103:
                out["degreesToTurn"] = r.u16()
            elif op in (106, 118):
                r.u16()
                r.u16()
                if op == 118:
                    r.u16()
                n = r.u8()
                for _ in range(n + 1):
                    r.u16()
            elif op == 107:
                out["clickable"] = False
            elif op == 109:
                out["rotationFlag"] = False
            elif op == 111:
                out["follower"] = True
            elif op == 114:
                out["runAnim"] = r.u16()
            elif op == 115:
                out["runAnim"] = r.u16()
                out["runBackAnim"] = r.u16()
                out["runLeftAnim"] = r.u16()
                out["runRightAnim"] = r.u16()
            elif op == 116:
                out["crawlAnim"] = r.u16()
            elif op == 117:
                out["crawlAnim"] = r.u16()
                out["crawlBackAnim"] = r.u16()
                out["crawlLeftAnim"] = r.u16()
                out["crawlRightAnim"] = r.u16()
            elif op in (74, 75, 76, 77, 78, 79):
                out[f"param{op}"] = r.u16()
            elif op == 122:
                out["pet"] = True
            elif op == 123:
                out["lowPriorityFollowerOps"] = True
            elif op == 249:
                n = r.u8()
                for _ in range(n):
                    is_str = r.u8() == 1
                    r.u16()
                    if is_str:
                        rs_string(r)
                    else:
                        r.i32()
            else:
                out["unknownOpcode"] = op
                break
        except Exception:
            out["parseErrorAtOpcode"] = op
            break
    return out


def decode_all_npcs(flat_cache_root: Path) -> Dict[int, Dict[str, object]]:
    files_by_archive = load_index2_reference(flat_cache_root)
    npc_group_file = flat_cache_root / "2" / "9.dat"
    if not npc_group_file.exists():
        raise FileNotFoundError(f"Missing NPC archive group: {npc_group_file}")
    group = decompress_js5_container(npc_group_file.read_bytes())
    npc_file_ids = files_by_archive.get(9)
    if not npc_file_ids:
        raise RuntimeError("Archive 9 not found in index 2 reference")
    split = split_group_files(group, npc_file_ids)
    out: Dict[int, Dict[str, object]] = {}
    for npc_id, blob in split.items():
        out[npc_id] = parse_npc_def(blob)
    return out


def find_matches(
    defs: Dict[int, Dict[str, object]],
    ids: Optional[List[int]],
    keywords: Optional[List[str]],
    categories: Optional[List[int]],
) -> Dict[int, Dict[str, object]]:
    matched: Dict[int, Dict[str, object]] = {}
    id_set = set(ids or [])
    kw = [k.lower() for k in (keywords or []) if k.strip()]
    cat_set = set(categories or [])

    for npc_id, d in defs.items():
        name = str(d.get("name", ""))
        keep = False
        if id_set and npc_id in id_set:
            keep = True
        if kw and name:
            low = name.lower()
            if any(k in low for k in kw):
                keep = True
        if cat_set:
            category = d.get("category")
            if isinstance(category, int) and category in cat_set:
                keep = True
        if keep:
            matched[npc_id] = d
    return dict(sorted(matched.items(), key=lambda kv: kv[0]))


def collect_model_ids(defs: Dict[int, Dict[str, object]]) -> List[int]:
    ids = set()
    for d in defs.values():
        for m in d.get("models", []) or []:
            if isinstance(m, int):
                ids.add(m)
        for m in d.get("chatModels", []) or []:
            if isinstance(m, int):
                ids.add(m)
    return sorted(ids)


def run_model_import(flat_cache_root: Path, client_cache_dir: Path, model_ids: List[int]) -> None:
    if not model_ids:
        print("No model IDs to import.")
        return
    csv = ",".join(str(x) for x in model_ids)
    cmd = [
        ".\\gradlew.bat",
        "run",
        "--args=" + f"\"{flat_cache_root}\" \"{client_cache_dir}\" \"{csv}\"",
    ]
    print("Running model import via FlatModelImportTool...")
    subprocess.run(
        " ".join(cmd),
        cwd=str(REPO_ROOT / "client"),
        shell=True,
        check=True,
    )


def parse_csv_ids(raw: str) -> List[int]:
    out = []
    for p in raw.split(","):
        p = p.strip()
        if p:
            out.append(int(p))
    return out


def main() -> None:
    parser = argparse.ArgumentParser(description="Parse/search/import NPC defs from OpenRS2 2446 flat cache.")
    parser.add_argument("--flat-cache", type=Path, default=DEFAULT_FLAT, help="Path to flat cache root (expects /255 and /2 folders).")
    parser.add_argument("--client-cache", type=Path, default=DEFAULT_CLIENT_CACHE, help="Path to target client Cache dir.")
    parser.add_argument("--ids", type=str, default="", help="Comma-separated NPC ids to include.")
    parser.add_argument("--categories", type=str, default="", help="Comma-separated category ids to include.")
    parser.add_argument("--find-keywords", nargs="*", default=[], help="Keyword filters against decoded NPC names.")
    parser.add_argument("--export", type=Path, default=DEFAULT_EXPORT, help="Output JSON path.")
    parser.add_argument("--import-models", action="store_true", help="Import referenced model/chatModel ids into client cache.")
    parser.add_argument("--print", action="store_true", dest="do_print", help="Print matched NPCs to stdout.")
    args = parser.parse_args()

    flat_root = args.flat_cache
    if (flat_root / "cache").exists():
        flat_root = flat_root / "cache"
    if not flat_root.exists():
        raise FileNotFoundError(f"Flat cache root not found: {flat_root}")

    ids = parse_csv_ids(args.ids) if args.ids else []
    categories = parse_csv_ids(args.categories) if args.categories else []
    keywords = args.find_keywords or []

    all_defs = decode_all_npcs(flat_root)
    matched = find_matches(
        all_defs,
        ids=ids if ids else None,
        keywords=keywords if keywords else None,
        categories=categories if categories else None,
    )

    payload = {
        "flat_cache": str(flat_root),
        "match_count": len(matched),
        "ids": list(matched.keys()),
        "npcs": matched,
    }
    args.export.parent.mkdir(parents=True, exist_ok=True)
    args.export.write_text(json.dumps(payload, indent=2), encoding="utf-8")
    print(f"Wrote {args.export} ({len(matched)} matches)")

    if args.do_print:
        for npc_id, d in matched.items():
            print(f"{npc_id}: {d.get('name', '<unnamed>')} models={d.get('models', [])}")

    if args.import_models:
        model_ids = collect_model_ids(matched)
        print(f"Model IDs to import: {len(model_ids)}")
        run_model_import(flat_root, args.client_cache, model_ids)


if __name__ == "__main__":
    main()
