#!/usr/bin/env python3
"""
Find the correct worn model IDs for perilous moon equipment by searching a wider range.
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

# Known correct models (worn equipment)
known_worn_models = {
    39506: ("Eclipse moon helm", 44162),
    39571: ("Eclipse moon chestplate", 17644),
    39572: ("Eclipse moon tassets", 9059),
    39573: ("Eclipse atlatl", 7196),
    39574: ("Blood moon helm", 13106),
    39575: ("Blood moon chestplate", 17944),
}

# Models that are wrong (inventory icons)
inventory_icon_models = {
    39576: ("Blood moon tassets icon", 1971),
    39577: ("Dual macuahuitl icon", 1746),
    39578: ("Blue moon helm icon", 1746),
    39579: ("Blue moon chestplate icon", 1746),
    39580: ("Blue moon tassets icon", 1746),
    39581: ("Blue moon spear icon", 2340),
}

print("=== Scanning for perilous moon worn models in wider range ===")
print()

# Scan models in 39000-40000 range for potential worn models
all_models = []
for model_id in range(39000, 40000):
    model_data = get_model_data(model_id)
    if model_data is None:
        continue
    
    decompressed = decompress_model(model_data)
    if decompressed:
        all_models.append((model_id, len(model_data), len(decompressed)))

# Sort by decompressed size (descending)
all_models.sort(key=lambda x: -x[2])

print("Top 50 largest models (potential worn equipment):")
for model_id, comp_size, decomp_size in all_models[:50]:
    status = ""
    if model_id in known_worn_models:
        status = f" -> {known_worn_models[model_id][0]} (KNOWN WORN)"
    elif model_id in inventory_icon_models:
        status = f" -> {inventory_icon_models[model_id][0]} (INVENTORY ICON)"
    print(f"  {model_id}: {decomp_size} bytes{status}")

print()
print("=== Looking for models similar to known worn models ===")
print()

# Find models similar in size to known worn models
# Blood moon tassets should be similar to Eclipse moon tassets (9059 bytes)
# Blue moon helm should be similar to Blood moon helm (13106 bytes)
# Blue moon chestplate should be similar to Eclipse moon chestplate (17644 bytes)
# Blue moon tassets should be similar to Eclipse moon tassets (9059 bytes)
# Blue moon spear should be similar to Eclipse atlatl (7196 bytes)
# Dual macuahuitl should be similar to Eclipse atlatl (7196 bytes)

target_sizes = {
    "Blood moon tassets": 9059,
    "Dual macuahuitl": 7196,
    "Blue moon helm": 13106,
    "Blue moon chestplate": 17644,
    "Blue moon tassets": 9059,
    "Blue moon spear": 7196,
}

for name, target_size in target_sizes.items():
    print(f"Looking for {name} (target ~{target_size} bytes):")
    found = []
    for model_id, comp_size, decomp_size in all_models:
        if model_id in known_worn_models or model_id in inventory_icon_models:
            continue
        # Allow 30% variance
        if abs(decomp_size - target_size) < target_size * 0.3:
            found.append((model_id, decomp_size))
    
    for model_id, size in sorted(found, key=lambda x: abs(x[1] - target_size))[:5]:
        print(f"  {model_id}: {size} bytes (diff: {abs(size - target_size)})")
    print()

dat_file.close()
