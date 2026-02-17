#!/usr/bin/env python3
"""
Debug the archive format to understand the structure
"""
import struct
import bz2

def read_unsigned_byte(data, offset):
    return data[offset], offset + 1

def read_unsigned_short(data, offset):
    return struct.unpack('>H', data[offset:offset+2])[0], offset + 2

def read_int(data, offset):
    return struct.unpack('>i', data[offset:offset+4])[0], offset + 4

# Decompress the archive
with open('./_ext/moon_cache/cache/2/10.dat', 'rb') as f:
    data = f.read()

compression = data[0]
length = struct.unpack('>I', data[1:5])[0]

# Decompress
compressed = b'BZh1' + data[9:]
decompressed = bz2.decompress(compressed)

print(f"Decompressed size: {len(decompressed)}")
print(f"First 100 bytes: {' '.join(f'{b:02x}' for b in decompressed[:100])}")

# Try different archive formats
offset = 0

# Format 1: Simple file count + files
print("\n=== Format 1: file_count (2 bytes) ===")
file_count, offset = read_unsigned_short(decompressed, 0)
print(f"File count: {file_count}")

# Format 2: Try reading as individual files without count
print("\n=== Format 2: Direct file reading ===")
offset = 0
# Try reading first few bytes as file sizes
for i in range(5):
    # Try 2-byte size
    size2, _ = read_unsigned_short(decompressed, offset)
    # Try 4-byte size
    size4, _ = read_int(decompressed, offset)
    print(f"Offset {offset}: 2-byte size = {size2}, 4-byte size = {size4}")
    offset += 1

# Format 3: Check if it's a JS5 archive format
print("\n=== Format 3: JS5 archive format ===")
# JS5 archives have:
# - 2 bytes: number of files
# - Then for each file: 2-byte delta-encoded file ID
# - Then for each file: 4-byte size (or 2-byte if small)
offset = 0
file_count, offset = read_unsigned_short(decompressed, offset)
print(f"File count: {file_count}")

# Read file IDs (delta encoded)
file_ids = []
file_id = 0
for i in range(min(file_count, 10)):
    delta, offset = read_unsigned_short(decompressed, offset)
    file_id += delta
    file_ids.append(file_id)
print(f"First 10 file IDs: {file_ids}")

# Read sizes
sizes = []
for i in range(min(file_count, 10)):
    size, offset = read_unsigned_short(decompressed, offset)
    sizes.append(size)
print(f"First 10 sizes: {sizes}")

# Check if sizes make sense
total_size = sum(sizes)
print(f"Sum of first 10 sizes: {total_size}")
print(f"Remaining data: {len(decompressed) - offset}")

# Format 4: Check if it's a different archive format (whirpool/darkstar format)
print("\n=== Format 4: Check for whirlpool format ===")
# Some caches use a different format where files are stored with their IDs
offset = 0
while offset < min(100, len(decompressed)):
    # Try reading file ID + size
    file_id, offset = read_unsigned_short(decompressed, offset)
    size, offset = read_unsigned_short(decompressed, offset)
    print(f"File ID: {file_id}, Size: {size}")
    if size > 10000:  # Likely wrong format
        print("Size too large, probably wrong format")
        break
    offset += size
    if offset > 50:
        break
