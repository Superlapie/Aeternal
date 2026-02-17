#!/usr/bin/env python3
"""Check model data in cache"""
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

print('DAT file size:', len(dat_data), 'bytes')

# Check model 39506
model_id = 39506
idx_offset = model_id * 6
sector = read_unsigned_triple(idx1_data, idx_offset)
length = read_unsigned_triple(idx1_data, idx_offset + 3)

print(f'Model {model_id}: sector={sector}, length={length}')

# Read the model data from the dat file
# Each sector is 520 bytes
sector_offset = sector * 520
print(f'Sector offset: {sector_offset}')

if sector_offset < len(dat_data):
    # Read the header
    header = dat_data[sector_offset:sector_offset+8]
    print(f'Header: {header.hex(" ")}')
    
    # The first 8 bytes are: 4 bytes for length, 4 bytes for file ID
    stored_length = read_int(dat_data, sector_offset)
    stored_id = read_int(dat_data, sector_offset + 4)
    print(f'Stored length: {stored_length}, stored ID: {stored_id}')
    
    # Read the first few bytes of the data
    data_start = sector_offset + 8
    first_bytes = dat_data[data_start:data_start+20]
    print(f'First 20 bytes of data: {first_bytes.hex(" ")}')
