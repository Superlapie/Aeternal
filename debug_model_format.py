#!/usr/bin/env python3
"""
Debug model format in main cache - models are stored as raw GZIP
"""
import os
import struct
import zlib
import gzip
import io

# Read main cache
main_cache_path = './client/Cache'
idx1_path = os.path.join(main_cache_path, 'main_file_cache.idx1')
dat_path = os.path.join(main_cache_path, 'main_file_cache.dat')

with open(idx1_path, 'rb') as f:
    idx_data = f.read()

dat_file = open(dat_path, 'rb')

# Check currently mapped models
mapped_models = [39506, 39571, 39572, 39573, 39574, 39575, 39576, 39577, 39578, 39579, 39580, 39581]

print("=== Perilous Moon Models in Main Cache ===")
print()

for model_id in mapped_models:
    entry_offset = model_id * 6
    size = ((idx_data[entry_offset] & 0xff) << 16) | ((idx_data[entry_offset+1] & 0xff) << 8) | (idx_data[entry_offset+2] & 0xff)
    sector = ((idx_data[entry_offset+3] & 0xff) << 16) | ((idx_data[entry_offset+4] & 0xff) << 8) | (idx_data[entry_offset+5] & 0xff)
    
    if size == 0 or sector == 0:
        print(f"Model {model_id}: NOT IN CACHE")
        continue
    
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
    
    # The data is raw GZIP (starts with 1f 8b)
    try:
        decompressed = zlib.decompress(bytes(model_data), 15 + 16)
        last_two = struct.unpack('>h', decompressed[-2:])[0]
        print(f"Model {model_id}: compressed={size}, decompressed={len(decompressed)}, format={last_two}")
    except Exception as e:
        # Try with gzip module
        try:
            buf = io.BytesIO(bytes(model_data))
            with gzip.GzipFile(fileobj=buf, mode='rb') as f:
                decompressed = f.read()
            last_two = struct.unpack('>h', decompressed[-2:])[0]
            print(f"Model {model_id}: compressed={size}, decompressed={len(decompressed)}, format={last_two}")
        except Exception as e2:
            print(f"Model {model_id}: compressed={size}, FAILED: {e2}")

# Now scan for large models
print()
print("=== Scanning for large models in 39000-40000 range ===")

large_models = []
for model_id in range(39000, 40000):
    entry_offset = model_id * 6
    if entry_offset + 6 > len(idx_data):
        continue
    
    size = ((idx_data[entry_offset] & 0xff) << 16) | ((idx_data[entry_offset+1] & 0xff) << 8) | (idx_data[entry_offset+2] & 0xff)
    sector = ((idx_data[entry_offset+3] & 0xff) << 16) | ((idx_data[entry_offset+4] & 0xff) << 8) | (idx_data[entry_offset+5] & 0xff)
    
    if size == 0 or sector == 0:
        continue
    
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
    
    # Decompress
    try:
        decompressed = zlib.decompress(bytes(model_data), 15 + 16)
        if len(decompressed) > 5000:
            large_models.append((model_id, size, len(decompressed)))
    except:
        pass

large_models.sort(key=lambda x: -x[2])
print(f"\nFound {len(large_models)} large models (>5000 bytes decompressed)")
print(f"{'Model ID':<10} {'Compressed':<12} {'Decompressed':<12}")
print("-" * 40)
for model_id, comp_size, decomp_size in large_models[:30]:
    print(f"{model_id:<10} {comp_size:<12} {decomp_size:<12}")

dat_file.close()
