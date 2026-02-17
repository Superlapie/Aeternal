#!/usr/bin/env python3
"""Check idx1 models"""
import os
import gzip
import bz2

def read_int(data, offset):
    return ((data[offset] & 0xFF) << 24) | ((data[offset + 1] & 0xFF) << 16) | ((data[offset + 2] & 0xFF) << 8) | (data[offset + 3] & 0xFF)

def read_unsigned_short(data, offset):
    return ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF)

def read_unsigned_triple(data, offset):
    return ((data[offset] & 0xFF) << 16) | ((data[offset + 1] & 0xFF) << 8) | (data[offset + 2] & 0xFF)

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

# Read idx1 to check what models are stored
idx1_path = 'client/Cache/main_file_cache.idx1'
with open(idx1_path, 'rb') as f:
    idx1_data = f.read()

# Each entry is 6 bytes: 3 bytes for sector, 3 bytes for length
# Check model 39506
model_id = 39506
idx_offset = model_id * 6
if idx_offset + 6 <= len(idx1_data):
    sector = read_unsigned_triple(idx1_data, idx_offset)
    length = read_unsigned_triple(idx1_data, idx_offset + 3)
    print(f'Model {model_id}: sector={sector}, length={length}')
else:
    print(f'Model {model_id}: index out of range (idx1 size: {len(idx1_data)})')

# Check how many models are in idx1
num_models = len(idx1_data) // 6
print(f'Total models in idx1: {num_models}')

# Check a few known models
for mid in [39506, 39571, 39572, 39573, 1, 100, 1000]:
    idx_offset = mid * 6
    if idx_offset + 6 <= len(idx1_data):
        sector = read_unsigned_triple(idx1_data, idx_offset)
        length = read_unsigned_triple(idx1_data, idx_offset + 3)
        if sector > 0 or length > 0:
            print(f'Model {mid}: sector={sector}, length={length}')
