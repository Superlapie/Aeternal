#!/usr/bin/env python3
"""Read model data from cache"""
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

print(f'Model {model_id}: size={size}, sector={sector}')

# Read all sectors for this model
all_data = bytearray()
current_sector = sector
part = 0

while current_sector != 0:
    sector_offset = current_sector * 520
    
    file_id = ((dat_data[sector_offset] & 0xFF) << 8) | (dat_data[sector_offset + 1] & 0xFF)
    part_num = ((dat_data[sector_offset + 2] & 0xFF) << 8) | (dat_data[sector_offset + 3] & 0xFF)
    next_sector = read_unsigned_triple(dat_data, sector_offset + 4)
    store_index = dat_data[sector_offset + 7] & 0xFF
    
    print(f'  Sector {current_sector}: file_id={file_id}, part={part_num}, next={next_sector}, store={store_index}')
    
    # Read data (512 bytes per sector)
    data_start = sector_offset + 8
    remaining = size - len(all_data)
    read_size = min(512, remaining)
    all_data.extend(dat_data[data_start:data_start + read_size])
    
    current_sector = next_sector
    part += 1
    
    if part > 1000:  # Safety limit
        break

print(f'Total data read: {len(all_data)} bytes')
print(f'First 20 bytes: {bytes(all_data[:20]).hex(" ")}')

# Decompress GZIP
if all_data[0] == 0x1F and all_data[1] == 0x8B:
    decompressed = gzip.decompress(bytes(all_data))
    print(f'Decompressed size: {len(decompressed)} bytes')
    print(f'Decompressed first 20 bytes: {decompressed[:20].hex(" ")}')