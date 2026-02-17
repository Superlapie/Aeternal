#!/usr/bin/env python3
"""
Verify that the replacement models exist in the cache.
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

# New model mappings
new_mappings = {
    31000: (39506, "Eclipse moon helm"),
    31001: (39571, "Eclipse moon chestplate"),
    31002: (39572, "Eclipse moon tassets"),
    31003: (39573, "Eclipse atlatl"),
    31004: (39574, "Blood moon helm"),
    31005: (39575, "Blood moon chestplate"),
    31006: (39317, "Blood moon tassets (REPLACEMENT)"),
    31007: (39316, "Dual macuahuitl (REPLACEMENT)"),
    31008: (39447, "Blue moon helm (REPLACEMENT)"),
    31009: (39309, "Blue moon chestplate (REPLACEMENT)"),
    31010: (39512, "Blue moon tassets (REPLACEMENT)"),
    31011: (39511, "Blue moon spear (REPLACEMENT)"),
}

print("=== Verifying replacement models ===")
print()

for item_id, (model_id, name) in new_mappings.items():
    model_data = get_model_data(model_id)
    if model_data is None:
        print(f"MISSING: Item {item_id} ({name}) -> Model {model_id}: NOT FOUND")
        continue
    
    decompressed = decompress_model(model_data)
    if decompressed is None:
        print(f"ERROR: Item {item_id} ({name}) -> Model {model_id}: Failed to decompress")
        continue
    
    status = "OK" if len(decompressed) > 5000 else "WARNING (small)"
    print(f"Item {item_id} ({name}) -> Model {model_id}: {len(decompressed)} bytes [{status}]")

dat_file.close()
