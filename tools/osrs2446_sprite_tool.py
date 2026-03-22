#!/usr/bin/env python3
"""
OpenRS2 2446 sprite utility for this workspace.

Supports:
1) Exporting sprite groups from a flat cache as PNGs.
2) Importing sprite groups into the client sprite cache
   (main_file_sprites.dat / main_file_sprites.idx).

Examples:
  python tools/osrs2446_sprite_tool.py export ^
    --flat-root _ext/openrs2-2446-flat/cache ^
    --groups 6180-6231 ^
    --out-dir _tmp/donor_sprites_arceuus

  python tools/osrs2446_sprite_tool.py import ^
    --flat-root _ext/openrs2-2446-flat/cache ^
    --groups 6180-6231 ^
    --cache-dir client/Cache ^
    --target-start auto ^
    --map-json _tmp/arceuus_sprite_import_map.json
"""

from __future__ import annotations

import argparse
import bz2
import gzip
import io
import json
import os
import shutil
import struct
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable, List, Sequence, Tuple

from PIL import Image


INDEX_ID = 8
SPRITE_IDX = "main_file_sprites.idx"
SPRITE_DAT = "main_file_sprites.dat"


def parse_group_ranges(specs: Sequence[str]) -> List[int]:
    out: List[int] = []
    seen = set()
    for spec in specs:
        spec = spec.strip()
        if not spec:
            continue
        for part in spec.split(","):
            part = part.strip()
            if not part:
                continue
            if "-" in part:
                lo_s, hi_s = part.split("-", 1)
                lo = int(lo_s)
                hi = int(hi_s)
                if hi < lo:
                    raise ValueError(f"Invalid range {part}: hi < lo")
                for gid in range(lo, hi + 1):
                    if gid not in seen:
                        out.append(gid)
                        seen.add(gid)
            else:
                gid = int(part)
                if gid not in seen:
                    out.append(gid)
                    seen.add(gid)
    return out


def resolve_flat_root(root: Path) -> Path:
    root = root.resolve()
    cache = root / "cache"
    if cache.is_dir():
        return cache
    return root


def read_int_be(data: bytes, off: int) -> int:
    if off + 4 > len(data):
        raise ValueError("Buffer underflow while reading int")
    return struct.unpack_from(">I", data, off)[0]


def unpack_js5_container(container: bytes) -> bytes:
    if len(container) < 5:
        return container
    ctype = container[0]
    compressed_len = read_int_be(container, 1)
    if compressed_len < 0:
        raise ValueError("Negative compressed length")

    if ctype == 0:
        start = 5
        end = start + compressed_len
        if end > len(container):
            raise ValueError("Type-0 container truncated")
        return container[start:end]

    if len(container) < 9:
        raise ValueError("Compressed container too short")
    uncompressed_len = read_int_be(container, 5)
    start = 9
    end = start + compressed_len
    if end > len(container):
        raise ValueError("Compressed container truncated")
    payload = container[start:end]

    if ctype == 2:
        out = gzip.decompress(payload)
    elif ctype == 1:
        try:
            out = bz2.decompress(payload)
        except OSError:
            # Some JS5 bzip payloads can be missing the BZh header.
            out = bz2.decompress(b"BZh1" + payload)
    else:
        raise ValueError(f"Unsupported JS5 compression type: {ctype}")

    if uncompressed_len > 0 and len(out) != uncompressed_len:
        raise ValueError(
            f"Unexpected uncompressed length: expected {uncompressed_len}, got {len(out)}"
        )
    return out


class Buffer:
    def __init__(self, data: bytes):
        self.data = data
        self.pos = 0

    def read_u1(self) -> int:
        if self.pos >= len(self.data):
            raise ValueError("Buffer underflow u1")
        v = self.data[self.pos]
        self.pos += 1
        return v

    def read_u2(self) -> int:
        if self.pos + 2 > len(self.data):
            raise ValueError("Buffer underflow u2")
        v = (self.data[self.pos] << 8) | self.data[self.pos + 1]
        self.pos += 2
        return v

    def read_s1(self) -> int:
        v = self.read_u1()
        return v - 256 if v > 127 else v

    def read_u3(self) -> int:
        if self.pos + 3 > len(self.data):
            raise ValueError("Buffer underflow u3")
        v = (
            (self.data[self.pos] << 16)
            | (self.data[self.pos + 1] << 8)
            | self.data[self.pos + 2]
        )
        self.pos += 3
        return v

    def read_bytes(self, n: int) -> bytes:
        if self.pos + n > len(self.data):
            raise ValueError("Buffer underflow bytes")
        out = self.data[self.pos : self.pos + n]
        self.pos += n
        return out

    def set_pos(self, p: int) -> None:
        if p < 0 or p > len(self.data):
            raise ValueError("Buffer position out of range")
        self.pos = p


@dataclass
class DecodedSprite:
    group_id: int
    frame: int
    width: int
    height: int
    offset_x: int
    offset_y: int
    pixels_rgba: bytes


def parse_resize(value: str) -> Tuple[int, int]:
    parts = value.lower().split("x", 1)
    if len(parts) != 2:
        raise ValueError("Resize must be WIDTHxHEIGHT")
    w = int(parts[0].strip())
    h = int(parts[1].strip())
    if w <= 0 or h <= 0:
        raise ValueError("Resize width/height must be > 0")
    return w, h


def resize_sprite(sprite: DecodedSprite, width: int, height: int) -> DecodedSprite:
    src = Image.frombytes(
        "RGBA", (sprite.width, sprite.height), sprite.pixels_rgba, "raw", "RGBA"
    )
    resized = src.resize((width, height), Image.Resampling.LANCZOS)
    return DecodedSprite(
        group_id=sprite.group_id,
        frame=sprite.frame,
        width=width,
        height=height,
        offset_x=0,
        offset_y=0,
        pixels_rgba=resized.tobytes(),
    )


def trim_alpha_bounds(sprite: DecodedSprite, min_size: int = 1) -> DecodedSprite:
    w, h = sprite.width, sprite.height
    data = sprite.pixels_rgba
    left, top = w, h
    right, bottom = -1, -1
    for y in range(h):
        row = y * w * 4
        for x in range(w):
            a = data[row + x * 4 + 3]
            if a != 0:
                if x < left:
                    left = x
                if y < top:
                    top = y
                if x > right:
                    right = x
                if y > bottom:
                    bottom = y

    if right < left or bottom < top:
        # Fully transparent, keep a tiny placeholder.
        side = max(1, min_size)
        return DecodedSprite(
            group_id=sprite.group_id,
            frame=sprite.frame,
            width=side,
            height=side,
            offset_x=0,
            offset_y=0,
            pixels_rgba=bytes(side * side * 4),
        )

    crop_w = max(min_size, right - left + 1)
    crop_h = max(min_size, bottom - top + 1)
    out = bytearray(crop_w * crop_h * 4)
    for y in range(crop_h):
        sy = top + y
        for x in range(crop_w):
            sx = left + x
            if sx >= w or sy >= h:
                continue
            src_i = (sy * w + sx) * 4
            dst_i = (y * crop_w + x) * 4
            out[dst_i : dst_i + 4] = data[src_i : src_i + 4]

    return DecodedSprite(
        group_id=sprite.group_id,
        frame=sprite.frame,
        width=crop_w,
        height=crop_h,
        offset_x=0,
        offset_y=0,
        pixels_rgba=bytes(out),
    )


def decode_sprite_archive(group_id: int, payload: bytes) -> List[DecodedSprite]:
    if len(payload) < 2:
        raise ValueError("Sprite archive too short")

    b = Buffer(payload)
    b.set_pos(len(payload) - 2)
    sprite_count = b.read_u2()
    if sprite_count <= 0:
        raise ValueError("Sprite archive has no frames")

    b.set_pos(len(payload) - 7 - sprite_count * 8)
    max_w = b.read_u2()
    max_h = b.read_u2()
    _ = (max_w, max_h)  # not used directly, but validated by parsing.
    palette_len = b.read_u1() + 1

    off_x = [b.read_u2() for _ in range(sprite_count)]
    off_y = [b.read_u2() for _ in range(sprite_count)]
    widths = [b.read_u2() for _ in range(sprite_count)]
    heights = [b.read_u2() for _ in range(sprite_count)]

    b.set_pos(len(payload) - 7 - sprite_count * 8 - (palette_len - 1) * 3)
    palette = [0] * palette_len
    for i in range(1, palette_len):
        rgb = b.read_u3()
        if rgb == 0:
            rgb = 1
        palette[i] = rgb

    out: List[DecodedSprite] = []
    b.set_pos(0)
    for frame in range(sprite_count):
        w = widths[frame]
        h = heights[frame]
        dim = w * h
        flags = b.read_u1()
        vertical = (flags & 0x1) != 0
        has_alpha = (flags & 0x2) != 0

        idx = bytearray(dim)
        alpha = bytearray(dim)

        if not vertical:
            idx[:] = b.read_bytes(dim)
        else:
            for x in range(w):
                for y in range(h):
                    idx[y * w + x] = b.read_s1() & 0xFF

        if has_alpha:
            if not vertical:
                alpha[:] = b.read_bytes(dim)
            else:
                for x in range(w):
                    for y in range(h):
                        alpha[y * w + x] = b.read_s1() & 0xFF
        else:
            for i, pal_idx in enumerate(idx):
                if pal_idx != 0:
                    alpha[i] = 0xFF

        rgba = bytearray(dim * 4)
        for i, pal_idx in enumerate(idx):
            rgb = palette[pal_idx & 0xFF]
            a = alpha[i]
            rgba[i * 4 + 0] = (rgb >> 16) & 0xFF
            rgba[i * 4 + 1] = (rgb >> 8) & 0xFF
            rgba[i * 4 + 2] = rgb & 0xFF
            rgba[i * 4 + 3] = a & 0xFF

        out.append(
            DecodedSprite(
                group_id=group_id,
                frame=frame,
                width=w,
                height=h,
                offset_x=off_x[frame],
                offset_y=off_y[frame],
                pixels_rgba=bytes(rgba),
            )
        )
    return out


def read_group_sprite(flat_root: Path, group_id: int, frame: int) -> DecodedSprite:
    path = flat_root / str(INDEX_ID) / f"{group_id}.dat"
    if not path.exists():
        raise FileNotFoundError(f"Missing group payload: {path}")
    data = unpack_js5_container(path.read_bytes())
    sprites = decode_sprite_archive(group_id, data)
    if frame < 0 or frame >= len(sprites):
        raise IndexError(
            f"Group {group_id} frame {frame} out of range (frames={len(sprites)})"
        )
    return sprites[frame]


def rgba_to_png_bytes(sprite: DecodedSprite) -> bytes:
    img = Image.frombytes(
        "RGBA", (sprite.width, sprite.height), sprite.pixels_rgba, "raw", "RGBA"
    )
    out = io.BytesIO()
    img.save(out, format="PNG")
    return out.getvalue()


def write_png(sprite: DecodedSprite, out_path: Path) -> None:
    out_path.parent.mkdir(parents=True, exist_ok=True)
    img = Image.frombytes(
        "RGBA", (sprite.width, sprite.height), sprite.pixels_rgba, "raw", "RGBA"
    )
    img.save(out_path)


def load_sprite_idx(idx_path: Path) -> List[Tuple[int, int, int, int]]:
    data = idx_path.read_bytes()
    if len(data) % 10 != 0:
        raise ValueError(f"Invalid sprite idx size {len(data)} (not divisible by 10)")
    out: List[Tuple[int, int, int, int]] = []
    for i in range(0, len(data), 10):
        pos = (data[i] << 16) | (data[i + 1] << 8) | data[i + 2]
        length = (data[i + 3] << 16) | (data[i + 4] << 8) | data[i + 5]
        off_x = (data[i + 6] << 8) | data[i + 7]
        off_y = (data[i + 8] << 8) | data[i + 9]
        out.append((pos, length, off_x, off_y))
    return out


def encode_sprite_idx(entries: Sequence[Tuple[int, int, int, int]]) -> bytes:
    out = bytearray()
    for pos, length, off_x, off_y in entries:
        if not (0 <= pos <= 0xFFFFFF and 0 <= length <= 0xFFFFFF):
            raise ValueError(f"Sprite position/length out of 24-bit range: {pos}, {length}")
        out.extend([(pos >> 16) & 0xFF, (pos >> 8) & 0xFF, pos & 0xFF])
        out.extend([(length >> 16) & 0xFF, (length >> 8) & 0xFF, length & 0xFF])
        out.extend([(off_x >> 8) & 0xFF, off_x & 0xFF, (off_y >> 8) & 0xFF, off_y & 0xFF])
    return bytes(out)


def backup_file(path: Path) -> Path:
    backup = path.with_suffix(path.suffix + ".bak")
    shutil.copy2(path, backup)
    return backup


def do_export(args: argparse.Namespace) -> None:
    flat_root = resolve_flat_root(Path(args.flat_root))
    groups = parse_group_ranges(args.groups)
    out_dir = Path(args.out_dir).resolve()
    out_dir.mkdir(parents=True, exist_ok=True)

    metadata = []
    for group in groups:
        sprite = read_group_sprite(flat_root, group, args.frame)
        if args.trim_alpha:
            sprite = trim_alpha_bounds(sprite, min_size=max(1, args.min_size))
        if args.resize:
            rw, rh = parse_resize(args.resize)
            sprite = resize_sprite(sprite, rw, rh)
        name = f"g{group}_f{args.frame}_{sprite.width}x{sprite.height}.png"
        out_path = out_dir / name
        write_png(sprite, out_path)
        metadata.append(
            {
                "group": group,
                "frame": args.frame,
                "width": sprite.width,
                "height": sprite.height,
                "offset_x": sprite.offset_x,
                "offset_y": sprite.offset_y,
                "file": name,
            }
        )
        print(f"OK export group={group} frame={args.frame} -> {out_path}")

    if args.meta_json:
        meta_path = Path(args.meta_json).resolve()
        meta_path.parent.mkdir(parents=True, exist_ok=True)
        meta_path.write_text(json.dumps(metadata, indent=2), encoding="utf-8")
        print(f"Wrote metadata: {meta_path}")


def do_import(args: argparse.Namespace) -> None:
    flat_root = resolve_flat_root(Path(args.flat_root))
    groups = parse_group_ranges(args.groups)
    cache_dir = Path(args.cache_dir).resolve()
    idx_path = cache_dir / SPRITE_IDX
    dat_path = cache_dir / SPRITE_DAT

    if not idx_path.exists() or not dat_path.exists():
        raise FileNotFoundError(f"Missing sprite cache files in {cache_dir}")

    entries = load_sprite_idx(idx_path)
    current_count = len(entries)
    target_start = current_count if args.target_start == "auto" else int(args.target_start)
    if target_start < 0:
        raise ValueError("target-start must be >= 0")

    if args.backup:
        idx_backup = backup_file(idx_path)
        dat_backup = backup_file(dat_path)
        print(f"Backed up idx -> {idx_backup}")
        print(f"Backed up dat -> {dat_backup}")

    dat = bytearray(dat_path.read_bytes())
    mapping = []

    for offset, group in enumerate(groups):
        sprite_id = target_start + offset
        sprite = read_group_sprite(flat_root, group, args.frame)
        if args.trim_alpha:
            sprite = trim_alpha_bounds(sprite, min_size=max(1, args.min_size))
        if args.resize:
            rw, rh = parse_resize(args.resize)
            sprite = resize_sprite(sprite, rw, rh)
        png = rgba_to_png_bytes(sprite)
        pos = len(dat)
        dat.extend(png)
        entry = (pos, len(png), sprite.offset_x & 0xFFFF, sprite.offset_y & 0xFFFF)

        if sprite_id < len(entries):
            entries[sprite_id] = entry
        else:
            while len(entries) < sprite_id:
                entries.append((0, 0, 0, 0))
            entries.append(entry)

        mapping.append(
            {
                "group": group,
                "frame": args.frame,
                "sprite_id": sprite_id,
                "width": sprite.width,
                "height": sprite.height,
                "offset_x": sprite.offset_x,
                "offset_y": sprite.offset_y,
            }
        )
        print(
            f"OK import group={group} frame={args.frame} -> sprite={sprite_id} "
            f"({sprite.width}x{sprite.height}, bytes={len(png)})"
        )

    dat_path.write_bytes(bytes(dat))
    idx_path.write_bytes(encode_sprite_idx(entries))
    print(f"Updated sprite cache: {idx_path} entries={len(entries)}, {dat_path} bytes={len(dat)}")

    if args.map_json:
        map_path = Path(args.map_json).resolve()
        map_path.parent.mkdir(parents=True, exist_ok=True)
        map_path.write_text(json.dumps(mapping, indent=2), encoding="utf-8")
        print(f"Wrote mapping: {map_path}")


def build_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(description="OpenRS2 2446 sprite export/import tool")
    sub = p.add_subparsers(dest="cmd", required=True)

    exp = sub.add_parser("export", help="Export flat-cache sprite groups to PNG")
    exp.add_argument("--flat-root", required=True, help="Path to flat cache root (or root/cache)")
    exp.add_argument("--groups", nargs="+", required=True, help="Group ids/ranges, e.g. 6180-6231 6303")
    exp.add_argument("--frame", type=int, default=0, help="Sprite frame index inside each group")
    exp.add_argument(
        "--trim-alpha",
        action="store_true",
        default=False,
        help="Trim transparent borders before writing PNG",
    )
    exp.add_argument("--min-size", type=int, default=1, help="Minimum width/height when trim-alpha is enabled")
    exp.add_argument("--resize", help="Resize output sprites, format WIDTHxHEIGHT, e.g. 20x20")
    exp.add_argument("--out-dir", required=True, help="Output directory for PNG files")
    exp.add_argument("--meta-json", help="Optional metadata JSON output path")
    exp.set_defaults(func=do_export)

    imp = sub.add_parser("import", help="Import flat-cache sprites into main_file_sprites")
    imp.add_argument("--flat-root", required=True, help="Path to flat cache root (or root/cache)")
    imp.add_argument("--groups", nargs="+", required=True, help="Group ids/ranges, e.g. 6180-6231")
    imp.add_argument("--frame", type=int, default=0, help="Sprite frame index inside each group")
    imp.add_argument(
        "--trim-alpha",
        action="store_true",
        default=False,
        help="Trim transparent borders before import",
    )
    imp.add_argument("--min-size", type=int, default=1, help="Minimum width/height when trim-alpha is enabled")
    imp.add_argument("--resize", help="Resize imported sprites, format WIDTHxHEIGHT, e.g. 20x20")
    imp.add_argument("--cache-dir", required=True, help="Client cache directory containing main_file_sprites.*")
    imp.add_argument(
        "--target-start",
        default="auto",
        help="Target sprite id start, or 'auto' to append at current sprite count",
    )
    imp.add_argument("--map-json", help="Optional JSON mapping output path")
    imp.add_argument(
        "--backup",
        action="store_true",
        default=False,
        help="Create .bak copies of sprite idx/dat before writing",
    )
    imp.set_defaults(func=do_import)
    return p


def main() -> int:
    parser = build_parser()
    args = parser.parse_args()
    args.func(args)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
