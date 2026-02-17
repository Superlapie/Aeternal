#!/usr/bin/env python3
"""
Find the correct worn model IDs for perilous moon equipment by analyzing model sizes.
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

# Known correct models
known_models = {
    39506: ("Eclipse moon helm", 44162),
    39571: ("Eclipse moon chestplate", 17644),
    39572: ("Eclipse moon tassets", 9059),
    39573: ("Eclipse atlatl", 7196),
    39574: ("Blood moon helm", 13106),
    39575: ("Blood moon chestplate", 17944),
}

# Models that are wrong (too small)
wrong_models = {
    39576: ("Blood moon tassets", 1971),
    39577: ("Dual macuahuitl", 1746),
    39578: ("Blue moon helm", 1746),
    39579: ("Blue moon chestplate", 1746),
    39580: ("Blue moon tassets", 1746),
    39581: ("Blue moon spear", 2340),
}

# Expected sizes for missing models
expected_sizes = {
    "Blood moon tassets": (8000, 15000),
    "Dual macuahuitl": (7000, 15000),
    "Blue moon helm": (8000, 15000),
    "Blue moon chestplate": (15000, 30000),
    "Blue moon tassets": (8000, 15000),
    "Blue moon spear": (7000, 15000),
}

print("=== Scanning for correct perilous moon worn models ===")
print()

# Scan models in 39500-39600 range
models_in_range = []
for model_id in range(39500, 39600):
    model_data = get_model_data(model_id)
    if model_data is None:
        continue
    
    decompressed = decompress_model(model_data)
    if decompressed:
        models_in_range.append((model_id, len(model_data), len(decompressed)))

# Sort by model ID
models_in_range.sort(key=lambda x: x[0])

print("All models in 39500-39600 range:")
for model_id, comp_size, decomp_size in models_in_range:
    status = ""
    if model_id in known_models:
        status = f" -> {known_models[model_id][0]} (CORRECT)"
    elif model_id in wrong_models:
        status = f" -> {wrong_models[model_id][0]} (WRONG - too small)"
    elif decomp_size > 5000:
        status = " -> POTENTIAL WORN MODEL"
    
    print(f"  {model_id}: {decomp_size} bytes{status}")

print()
print("=== Potential replacement models ===")
print()

# Find models that could be replacements
for name, (min_size, max_size) in expected_sizes.items():
    print(f"Looking for {name} (expected {min_size}-{max_size} bytes):")
    for model_id, comp_size, decomp_size in models_in_range:
        if min_size <= decomp_size <= max_size and model_id not in known_models and model_id not in wrong_models:
            print(f"  {model_id}: {decomp_size} bytes")
    print()

dat_file.close()
