#!/usr/bin/env python3
"""Check model data in cache properly"""
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

# Check model 39506
model_id = 39506
idx_offset = model_id * 6
size = read_unsigned_triple(idx1_data, idx_offset)
sector = read_unsigned_triple(idx1_data, idx_offset + 3)

print(f'Model {model_id}: size={size}, sector={sector}')

# Read the model data properly
sector_offset = sector * 520
print(f'Sector offset: {sector_offset}')

# Read the first sector header
# Format: 2 bytes file ID, 2 bytes part, 3 bytes next sector, 1 byte store index
file_id = ((dat_data[sector_offset] & 0xFF) << 8) | (dat_data[sector_offset + 1] & 0xFF)
part = ((dat_data[sector_offset + 2] & 0xFF) << 8) | (dat_data[sector_offset + 3] & 0xFF)
next_sector = read_unsigned_triple(dat_data, sector_offset + 4)
store_index = dat_data[sector_offset + 7] & 0xFF

print(f'File ID: {file_id}, Part: {part}, Next sector: {next_sector}, Store index: {store_index}')

# Read the first 512 bytes of data
data_start = sector_offset + 8
first_data = dat_data[data_start:data_start + 20]
print(f'First 20 bytes of data: {first_data.hex(" ")}')
