#!/usr/bin/env python3
"""
Find model files in the cache that might be perilous moon equipment models.
We'll look for models in the 39500-40000 range and check their sizes.
"""
import os
import struct
import gzip as gzplib
import bz2

def decompress_model(data):
    """Decompress a model file"""
    if len(data) < 5:
        return None
    
    compression = data[0]
    length = struct.unpack('>I', data[1:5])[0]
    
    if compression == 0:
        return data[5:5+length]
    elif compression == 1:  # BZIP2
        try:
            compressed = b'BZh1' + data[9:]
            return bz2.decompress(compressed)
        except:
            return None
    elif compression == 2:  # GZIP
        try:
            return gzplib.decompress(data[9:])
        except:
            return None
    return None

def get_model_info(data):
    """Get basic info about a model"""
    decompressed = decompress_model(data)
    if decompressed is None:
        return None
    
    # Check last 2 bytes for format
    last_two = decompressed[-2:] if len(decompressed) >= 2 else b'\x00\x00'
    format_marker = struct.unpack('>h', last_two)[0]
    
    return {
        'compressed_size': len(data),
        'decompressed_size': len(decompressed),
        'format_marker': format_marker,
    }

# Check models in the openrs2-2446-flat cache
cache_path = './_ext/openrs2-2446-flat/cache/1'

if os.path.exists(cache_path):
    print("=== Model files in cache (looking for moon equipment models) ===")
    print()
    
    # Get all model files
    model_files = []
    for filename in os.listdir(cache_path):
        if filename.endswith('.dat'):
            model_id = int(filename.replace('.dat', ''))
            model_files.append(model_id)
    
    model_files.sort()
    print(f"Total models: {len(model_files)}")
    print(f"Model ID range: {min(model_files)} to {max(model_files)}")
    
    # Look for models in the 39500-40000 range (where moon models might be)
    print()
    print("=== Models in 39500-40000 range ===")
    
    for model_id in range(39500, 40000):
        filepath = os.path.join(cache_path, f'{model_id}.dat')
        if os.path.exists(filepath):
            with open(filepath, 'rb') as f:
                data = f.read()
            
            info = get_model_info(data)
            if info:
                print(f"Model {model_id}: compressed={info['compressed_size']}, decompressed={info['decompressed_size']}, format={info['format_marker']}")
    
    # Also check the models currently mapped in MoonModelLoader
    print()
    print("=== Currently mapped models ===")
    mapped_models = [39506, 39571, 39572, 39573, 39574, 39575, 39576, 39577, 39578, 39579, 39580, 39581]
    
    for model_id in mapped_models:
        filepath = os.path.join(cache_path, f'{model_id}.dat')
        if os.path.exists(filepath):
            with open(filepath, 'rb') as f:
                data = f.read()
            
            info = get_model_info(data)
            if info:
                print(f"Model {model_id}: compressed={info['compressed_size']}, decompressed={info['decompressed_size']}, format={info['format_marker']}")
        else:
            print(f"Model {model_id}: NOT FOUND")
    
    # Look for larger models that might be equipment (equipment models are usually 5000+ bytes)
    print()
    print("=== Large models (potential equipment) in 39000-40000 range ===")
    
    large_models = []
    for model_id in range(39000, 40000):
        filepath = os.path.join(cache_path, f'{model_id}.dat')
        if os.path.exists(filepath):
            with open(filepath, 'rb') as f:
                data = f.read()
            
            info = get_model_info(data)
            if info and info['decompressed_size'] > 5000:
                large_models.append((model_id, info['decompressed_size']))
    
    large_models.sort(key=lambda x: -x[1])  # Sort by size descending
    for model_id, size in large_models[:30]:
        print(f"Model {model_id}: {size} bytes")
