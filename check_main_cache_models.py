#!/usr/bin/env python3
"""
Check if perilous moon models are in the main cache
"""
import os
import struct
import zlib

def decompress_model(data):
    """Decompress a model file"""
    if len(data) < 5:
        return None
    
    compression = data[0]
    length = struct.unpack('>I', data[1:5])[0]
    
    if compression == 0:
        return data[5:5+length]
    elif compression == 1:  # BZIP2
        import bz2
        try:
            compressed = b'BZh1' + data[9:]
            return bz2.decompress(compressed)
        except:
            return None
    elif compression == 2:  # GZIP
        try:
            return zlib.decompress(data[9:], 15 + 16)
        except:
            return None
    return None

# Check main cache
main_cache_path = './client/Cache'

print("=== Checking main cache for perilous moon models ===")
print()

# Check if idx1 exists (models)
idx1_path = os.path.join(main_cache_path, 'main_file_cache.idx1')
dat_path = os.path.join(main_cache_path, 'main_file_cache.dat')

if not os.path.exists(dat_path):
    dat_path = os.path.join(main_cache_path, 'main_file_cache.dat2')

print(f"DAT file: {dat_path}")
print(f"IDX1 file: {idx1_path}")

if os.path.exists(idx1_path) and os.path.exists(dat_path):
    with open(idx1_path, 'rb') as f:
        idx_data = f.read()
    
    # Each index entry is 6 bytes
    num_entries = len(idx_data) // 6
    print(f"Number of index entries: {num_entries}")
    
    # Check for perilous moon models
    mapped_models = [39506, 39571, 39572, 39573, 39574, 39575, 39576, 39577, 39578, 39579, 39580, 39581]
    
    for model_id in mapped_models:
        entry_offset = model_id * 6
        if entry_offset + 6 <= len(idx_data):
            size = ((idx_data[entry_offset] & 0xff) << 16) | ((idx_data[entry_offset+1] & 0xff) << 8) | (idx_data[entry_offset+2] & 0xff)
            sector = ((idx_data[entry_offset+3] & 0xff) << 16) | ((idx_data[entry_offset+4] & 0xff) << 8) | (idx_data[entry_offset+5] & 0xff)
            
            if size > 0 and sector > 0:
                print(f"Model {model_id}: FOUND in main cache (size={size}, sector={sector})")
            else:
                print(f"Model {model_id}: NOT in main cache (size=0, sector=0)")
        else:
            print(f"Model {model_id}: Index entry out of range")
else:
    print("Main cache files not found")

# Check if models are in idx7 (custom store)
idx7_path = os.path.join(main_cache_path, 'main_file_cache.idx7')
print()
print(f"IDX7 file: {idx7_path}")

if os.path.exists(idx7_path):
    with open(idx7_path, 'rb') as f:
        idx7_data = f.read()
    
    num_entries = len(idx7_data) // 6
    print(f"Number of IDX7 entries: {num_entries}")
    
    for model_id in mapped_models:
        entry_offset = model_id * 6
        if entry_offset + 6 <= len(idx7_data):
            size = ((idx7_data[entry_offset] & 0xff) << 16) | ((idx7_data[entry_offset+1] & 0xff) << 8) | (idx7_data[entry_offset+2] & 0xff)
            sector = ((idx7_data[entry_offset+3] & 0xff) << 16) | ((idx7_data[entry_offset+4] & 0xff) << 8) | (idx7_data[entry_offset+5] & 0xff)
            
            if size > 0 and sector > 0:
                print(f"Model {model_id}: FOUND in idx7 (size={size}, sector={sector})")
            else:
                print(f"Model {model_id}: NOT in idx7")
        else:
            print(f"Model {model_id}: IDX7 entry out of range")
