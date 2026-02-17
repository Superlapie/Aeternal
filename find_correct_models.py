#!/usr/bin/env python3
"""
Find the correct model IDs for perilous moon equipment by analyzing model sizes
and looking for models that match the expected sizes for equipment.
"""
import os
import struct
import zlib

# Read main cache
main_cache_path = './client/Cache'
idx1_path = os.path.join(main_cache_path, 'main_file_cache.idx1')
dat_path = os.path.join(main_cache_path, 'main_file_cache.dat')

with open(idx1_path, 'rb') as f:
    idx_data = f.read()

dat_file = open(dat_path, 'rb')

def get_model_data(model_id):
    """Get model data from cache"""
    entry_offset = model_id * 6
    if entry_offset + 6 > len(idx_data):
        return None
    
    size = ((idx_data[entry_offset] & 0xff) << 16) | ((idx_data[entry_offset+1] & 0xff) << 8) | (idx_data[entry_offset+2] & 0xff)
    sector = ((idx_data[entry_offset+3] & 0xff) << 16) | ((idx_data[entry_offset+4] & 0xff) << 8) | (idx_data[entry_offset+5] & 0xff)
    
    if size == 0 or sector == 0:
        return None
    
    # Read the model data
    model_data = bytearray()
    remaining = size
    current_sector = sector
    
    while remaining > 0:
        dat_file.seek(current_sector * 520)
        header = dat_file.read(8)
        if len(header) < 8:
            break
        
        next_sector = ((header[4] & 0xff) << 16) | ((header[5] & 0xff) << 8) | (header[6] & 0xff)
        
        read_size = min(512, remaining)
        chunk = dat_file.read(read_size)
        model_data.extend(chunk)
        remaining -= len(chunk)
        
        current_sector = next_sector
        if current_sector == 0:
            break
    
    return bytes(model_data)

def decompress_model(model_data):
    """Decompress model data"""
    try:
        return zlib.decompress(model_data, 15 + 16)
    except:
        return None

# Scan for all models in 39000-40000 range
print("=== Scanning models in 39000-40000 range ===")
print()

models_info = []
for model_id in range(39000, 40000):
    model_data = get_model_data(model_id)
    if model_data is None:
        continue
    
    decompressed = decompress_model(model_data)
    if decompressed:
        models_info.append((model_id, len(model_data), len(decompressed)))

# Sort by decompressed size
models_info.sort(key=lambda x: -x[2])

print(f"Found {len(models_info)} models in range")
print()

# Group by size ranges
print("=== Models by size range ===")
print()

# Very large models (>30000 bytes) - likely full body armor or weapons
print("VERY LARGE (>30000 bytes) - Full body armor, large weapons:")
for model_id, comp_size, decomp_size in models_info:
    if decomp_size > 30000:
        print(f"  Model {model_id}: {decomp_size} bytes")

print()

# Large models (15000-30000 bytes) - likely chestplates
print("LARGE (15000-30000 bytes) - Chestplates:")
for model_id, comp_size, decomp_size in models_info:
    if 15000 <= decomp_size <= 30000:
        print(f"  Model {model_id}: {decomp_size} bytes")

print()

# Medium models (8000-15000 bytes) - likely helms, tassets, weapons
print("MEDIUM (8000-15000 bytes) - Helms, tassets, weapons:")
for model_id, comp_size, decomp_size in models_info:
    if 8000 <= decomp_size < 15000:
        print(f"  Model {model_id}: {decomp_size} bytes")

print()

# Small models (3000-8000 bytes) - likely smaller weapons
print("SMALL (3000-8000 bytes) - Smaller weapons:")
for model_id, comp_size, decomp_size in models_info:
    if 3000 <= decomp_size < 8000:
        print(f"  Model {model_id}: {decomp_size} bytes")

print()

# Very small models (<3000 bytes) - likely icons
print("VERY SMALL (<3000 bytes) - Likely inventory icons:")
for model_id, comp_size, decomp_size in models_info:
    if decomp_size < 3000:
        print(f"  Model {model_id}: {decomp_size} bytes")

# Print the currently mapped models
print()
print("=== Currently mapped models ===")
mapped_models = [39506, 39571, 39572, 39573, 39574, 39575, 39576, 39577, 39578, 39579, 39580, 39581]
for model_id in mapped_models:
    for info in models_info:
        if info[0] == model_id:
            print(f"Model {model_id}: compressed={info[1]}, decompressed={info[2]}")
            break

dat_file.close()
