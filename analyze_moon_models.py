#!/usr/bin/env python3
"""Analyze moon models to find correct IDs"""
import os
import gzip
import bz2

def read_int(data, offset):
    return ((data[offset] & 0xFF) << 24) | ((data[offset + 1] & 0xFF) << 16) | ((data[offset + 2] & 0xFF) << 8) | (data[offset + 3] & 0xFF)

def unpack_js5_container(data):
    if len(data) < 5:
        return data
    container_type = data[0] & 0xFF
    compressed_len = read_int(data, 1)
    
    if container_type == 0:
        return data[5:5+compressed_len]
    elif container_type == 1:
        compressed_data = data[9:9+compressed_len]
        for level in [1, 9, 8, 7, 6, 5, 4, 3, 2]:
            try:
                return bz2.decompress(b'BZh' + bytes([ord(str(level))]) + compressed_data)
            except:
                pass
        raise Exception('Could not decompress BZIP2 data')
    elif container_type == 2:
        return gzip.decompress(data[9:9+compressed_len])
    else:
        return data

# Find all models in the 39500-39600 range and their sizes
models_dir = '_ext/moon_cache/cache/7'
model_info = []

for i in range(39500, 39600):
    model_path = os.path.join(models_dir, f'{i}.dat')
    if os.path.exists(model_path):
        with open(model_path, 'rb') as f:
            raw_data = f.read()
        decompressed = unpack_js5_container(raw_data)
        model_info.append((i, len(decompressed)))

# Sort by size descending to find the largest models (likely equipment)
model_info.sort(key=lambda x: x[1], reverse=True)

print('Top 30 largest models in 39500-39600 range:')
for model_id, size in model_info[:30]:
    print(f'  {model_id}: {size} bytes')

print()
print('All models sorted by ID:')
model_info.sort(key=lambda x: x[0])
for model_id, size in model_info:
    print(f'  {model_id}: {size} bytes')
