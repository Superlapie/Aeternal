#!/usr/bin/env python3
"""Check models in main cache"""
import os
import gzip

def read_int(data, offset):
    return ((data[offset] & 0xFF) << 24) | ((data[offset + 1] & 0xFF) << 16) | ((data[offset + 2] & 0xFF) << 8) | (data[offset + 3] & 0xFF)

def read_unsigned_triple(data, offset):
    return ((data[offset] & 0xFF) << 16) | ((data[offset + 1] & 0xFF) << 8) | (data[offset + 2] & 0xFF)

# Read idx1 and dat
idx1_path = 'client/Cache/main_file_cache.idx1'
dat_path = 'client/Cache/main_file_cache.dat'

with open(idx1_path, 'rb') as f:
    idx1_data = f.read()

with open(dat_path, 'rb') as f:
    dat_data = f.read()

# Check all moon models in main cache
moon_models = [39506, 39571, 39572, 39573, 39574, 39575, 39576, 39577, 39578, 39579, 39580, 39581]

print('Models in main cache:')
for model_id in moon_models:
    idx_offset = model_id * 6
    if idx_offset + 6 <= len(idx1_data):
        size = read_unsigned_triple(idx1_data, idx_offset)
        sector = read_unsigned_triple(idx1_data, idx_offset + 3)
        if size > 0 and sector > 0:
            print(f'  {model_id}: size={size}, sector={sector}')
        else:
            print(f'  {model_id}: EMPTY (size={size}, sector={sector})')
    else:
        print(f'  {model_id}: OUT OF RANGE')
