#!/usr/bin/env python3
"""Find moon model IDs"""
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

# Check all model files in the moon_cache to find the perilous moon models
models_dir = '_ext/moon_cache/cache/7'
model_files = [f for f in os.listdir(models_dir) if f.endswith('.dat')]

# Find models in the 39500-39600 range
moon_model_files = []
for f in model_files:
    model_id = int(f.replace('.dat', ''))
    if 39500 <= model_id <= 39600:
        moon_model_files.append(model_id)

moon_model_files.sort()
print('Found', len(moon_model_files), 'models in range 39500-39600')
print('Model IDs:', moon_model_files[:30], '...')