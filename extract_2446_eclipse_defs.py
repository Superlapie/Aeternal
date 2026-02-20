#!/usr/bin/env python3
"""
Extract exact 2446 Eclipse sequence/spotanim definition files from an OpenRS2 flat cache.

Outputs:
  client/Cache/e2446_seq_<id>.dat
  client/Cache/e2446_spot_<id>.dat
"""

from __future__ import annotations

import bz2
import gzip
import json
import struct
from pathlib import Path


FLAT_CACHE = Path("_ext/openrs2-2446-flat/cache")
OUT_DIR = Path("client/Cache")
ATLATL_JSON = Path("_ext/animations_export_8_itemsATLATL.json")
VOIDWAKER_JSON = Path("_ext/animations_export_4_itemsVOIDWAKER.json")
YAMA_JSON = Path("_ext/animations_export_62_itemsYAMA.json")
NOX_JSON = Path("_ext/NoxAnimations.json")

SEQ_IDS = [
    10815, 10818, 10819,
    11051, 11052, 11053, 11055,
    11057, 11058, 11059, 11060, 11061, 11062, 11063, 11064,
    11240, 11275, 11463, 11464,
    11455, 11587,
    11338, 11339, 11340, 11342, 11345, 11346, 11350, 11352, 11355, 11358,
    12140, 12141,
]
SPOT_IDS = [2709, 2710, 2711, 2712, 2795, 2796, 2797, 2798, 2914, 3017, 3030]


class Reader:
    def __init__(self, data: bytes):
        self.data = data
        self.pos = 0

    def u8(self) -> int:
        v = self.data[self.pos]
        self.pos += 1
        return v

    def u16(self) -> int:
        v = struct.unpack_from(">H", self.data, self.pos)[0]
        self.pos += 2
        return v

    def i32(self) -> int:
        v = struct.unpack_from(">i", self.data, self.pos)[0]
        self.pos += 4
        return v

    def u32(self) -> int:
        v = struct.unpack_from(">I", self.data, self.pos)[0]
        self.pos += 4
        return v

    def big_smart(self) -> int:
        if self.data[self.pos] < 128:
            return self.u16()
        return self.i32() & 0x7FFFFFFF


def decompress_container(container: bytes) -> bytes:
    if len(container) < 5:
        raise ValueError("container too short")
    ctype = container[0]
    clen = struct.unpack_from(">I", container, 1)[0]
    if ctype == 0:
        start = 5
        return container[start:start + clen]

    if len(container) < 9:
        raise ValueError("compressed container too short")
    ulen = struct.unpack_from(">I", container, 5)[0]
    block = container[9:9 + clen]

    if ctype == 1:
        # Jagex bzip2 omits BZh1 header.
        raw = bz2.decompress(b"BZh1" + block)
    elif ctype == 2:
        raw = gzip.decompress(block)
    else:
        raise ValueError(f"unsupported compression type {ctype}")

    if len(raw) != ulen:
        raise ValueError(f"decompressed len mismatch {len(raw)} != {ulen}")
    return raw


def load_index2_reference() -> dict[int, list[int]]:
    ref_container = (FLAT_CACHE / "255" / "2.dat").read_bytes()
    ref = decompress_container(ref_container)

    r = Reader(ref)
    protocol = r.u8()
    if protocol < 5 or protocol > 7:
        raise ValueError(f"unsupported index protocol {protocol}")

    if protocol >= 6:
        _index_revision = r.i32()

    flags = r.u8()
    named = (flags & 1) != 0
    sized = (flags & 4) != 0

    archive_count = r.big_smart() if protocol >= 7 else r.u16()
    archive_ids: list[int] = []
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
            _ = r.i32()  # compressed size
            _ = r.i32()  # decompressed size

    for _ in range(archive_count):
        _ = r.i32()  # revision

    file_counts: list[int] = []
    for _ in range(archive_count):
        file_counts.append(r.big_smart() if protocol >= 7 else r.u16())

    archive_files: dict[int, list[int]] = {}
    for idx, aid in enumerate(archive_ids):
        cnt = file_counts[idx]
        files: list[int] = []
        last_f = 0
        for _ in range(cnt):
            delta = r.big_smart() if protocol >= 7 else r.u16()
            last_f += delta
            files.append(last_f)
        archive_files[aid] = files

    if named:
        for idx, aid in enumerate(archive_ids):
            for _ in range(file_counts[idx]):
                _ = r.i32()  # file name hash

    return archive_files


def split_group_files(group_data: bytes, file_ids: list[int]) -> dict[int, bytes]:
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


def extract_archive(index_id: int, archive_id: int, wanted_ids: list[int], file_map: dict[int, list[int]], prefix: str) -> None:
    group_container = (FLAT_CACHE / str(index_id) / f"{archive_id}.dat").read_bytes()
    group_data = decompress_container(group_container)
    group_file_ids = file_map[archive_id]
    files = split_group_files(group_data, group_file_ids)

    for wid in wanted_ids:
        payload = files.get(wid)
        if payload is None:
            print(f"MISS {prefix} {wid}")
            continue
        out = OUT_DIR / f"e2446_{prefix}_{wid}.dat"
        out.write_bytes(payload)
        print(f"OK   {out} ({len(payload)} bytes)")


def main() -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    file_map = load_index2_reference()
    extract_archive(2, 12, SEQ_IDS, file_map, "seq")
    extract_archive(2, 13, SPOT_IDS, file_map, "spot")
    write_json_backed_sequences(ATLATL_JSON)
    write_json_backed_sequences(VOIDWAKER_JSON)
    write_json_backed_sequences(YAMA_JSON)
    write_json_backed_sequences(NOX_JSON)


def write_json_backed_sequences(json_path: Path) -> None:
    if not json_path.exists():
        return

    try:
        entries = json.loads(json_path.read_text(encoding="utf-8"))
    except Exception as ex:
        print(f"WARN could not parse {json_path}: {ex}")
        return

    for entry in entries:
        seq_id = int(entry.get("id", -1))
        frame_ids = entry.get("frameIDs") or []
        frame_lengths = entry.get("frameLengths") or []
        if seq_id < 0 or not frame_ids or len(frame_ids) != len(frame_lengths):
            continue

        payload = bytearray()
        frame_count = len(frame_ids)

        # Opcode 1: frame table (317/OSRS-compatible format used by this client).
        payload.append(1)
        payload += struct.pack(">H", frame_count)
        for length in frame_lengths:
            payload += struct.pack(">H", int(length))
        for frame_id in frame_ids:
            payload += struct.pack(">H", int(frame_id) & 0xFFFF)
        for frame_id in frame_ids:
            payload += struct.pack(">H", (int(frame_id) >> 16) & 0xFFFF)

        if bool(entry.get("stretches", False)):
            payload.append(4)

        forced_priority = int(entry.get("forcedPriority", -1))
        if forced_priority >= 0:
            payload.append(5)
            payload.append(forced_priority & 0xFF)

        # Hand-item overrides from modern caches can break on 317-style appearance
        # assembly; rely on equipped weapon model instead.

        max_loops = int(entry.get("maxLoops", -1))
        if max_loops >= 0:
            payload.append(8)
            payload.append(max_loops & 0xFF)

        # Terminator.
        payload.append(0)

        out = OUT_DIR / f"e2446_seq_{seq_id}.dat"
        out.write_bytes(bytes(payload))
        print(f"JSON {out} ({len(payload)} bytes)")

if __name__ == "__main__":
    main()
