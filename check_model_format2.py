#!/usr/bin/env python3
"""Check model format"""
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

# Read model 39506 data
model_id = 39506
idx_offset = model_id * 6
size = read_unsigned_triple(idx1_data, idx_offset)
sector = read_unsigned_triple(idx1_data, idx_offset + 3)

# Read all sectors for this model
all_data = bytearray()
current_sector = sector
part = 0

while current_sector != 0:
    sector_offset = current_sector * 520
    next_sector = read_unsigned_triple(dat_data, sector_offset + 4)
    
    data_start = sector_offset + 8
    remaining = size - len(all_data)
    read_size = min(512, remaining)
    all_data.extend(dat_data[data_start:data_start + read_size])
    
    current_sector = next_sector
    part += 1
    if part > 1000:
        break

# Decompress GZIP
decompressed = gzip.decompress(bytes(all_data))

# Check last 2 bytes for model format detection
print('Decompressed size:', len(decompressed), 'bytes')
print('Last 2 bytes:', format(decompressed[-2], '02x'), format(decompressed[-1], '02x'))
print('Last 2 bytes (signed):', decompressed[-2], decompressed[-1])
print('Last 18 bytes:', decompressed[-18:].hex(' '))