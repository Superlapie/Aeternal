#!/usr/bin/env python3
"""Check model sizes"""
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

# Check model sizes for perilous moon models
models_dir = '_ext/moon_cache/cache/7'
moon_models = [39506, 39571, 39572, 39573, 39574, 39575, 39576, 39577, 39578, 39579, 39580, 39581]

print('Model sizes from moon_cache:')
for model_id in moon_models:
    model_path = os.path.join(models_dir, f'{model_id}.dat')
    if os.path.exists(model_path):
        with open(model_path, 'rb') as f:
            raw_data = f.read()
        decompressed = unpack_js5_container(raw_data)
        print(f'  {model_id}: {len(raw_data)} -> {len(decompressed)} bytes')
    else:
        print(f'  {model_id}: NOT FOUND')
