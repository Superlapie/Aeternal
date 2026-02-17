#!/usr/bin/env python3
import bz2
import struct

# Check the moon_cache flat files
cache_path = './_ext/moon_cache/cache/2/10.dat'
with open(cache_path, 'rb') as f:
    data = f.read()

compression = data[0]
length = struct.unpack('>I', data[1:5])[0]
print(f'Compression: {compression}')
print(f'Length: {length}')

# Decompress
compressed = b'BZh1' + data[9:]
decompressed = bz2.decompress(compressed)
print(f'Decompressed size: {len(decompressed)}')

# Parse archive
offset = 0
file_count = struct.unpack('>H', decompressed[0:2])[0]
offset = 2
print(f'File count: {file_count}')

# Read file IDs
file_ids = []
file_id = 0
for i in range(file_count):
    delta = struct.unpack('>H', decompressed[offset:offset+2])[0]
    offset += 2
    file_id += delta
    file_ids.append(file_id)

print(f'File ID range: {min(file_ids)} to {max(file_ids)}')
print(f'First 20 file IDs: {file_ids[:20]}')
print(f'Last 20 file IDs: {file_ids[-20:]}')

# Check if moon items are in range
moon_items = [28988, 28997, 29000, 29004, 29007, 29010, 29013, 29016, 29019, 29022, 29025, 29028]
for item_id in moon_items:
    if item_id in file_ids:
        print(f'Item {item_id}: FOUND!')
    else:
        # Find closest
        closest = min(file_ids, key=lambda x: abs(x - item_id))
        print(f'Item {item_id}: not found, closest is {closest}')
