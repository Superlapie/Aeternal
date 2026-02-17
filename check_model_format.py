#!/usr/bin/env python3
"""
Check the perilous moon models in the moon_cache/cache/7 directory
"""
import os
import struct
import gzip as gzplib
import bz2
import io
import zlib

# Check models in the moon_cache/cache/7 directory
cache_path = './_ext/moon_cache/cache/7'

print("=== Perilous Moon Models in moon_cache/cache/7 ===")
print()

# Models currently mapped in MoonModelLoader
mapped_models = [39506, 39571, 39572, 39573, 39574, 39575, 39576, 39577, 39578, 39579, 39580, 39581]

for model_id in mapped_models:
    filepath = os.path.join(cache_path, f'{model_id}.dat')
    if os.path.exists(filepath):
        with open(filepath, 'rb') as f:
            data = f.read()
        
        print(f"Model {model_id}:")
        print(f"  File size: {len(data)}")
        
        # Check compression type
        compression = data[0]
        length = struct.unpack('>I', data[1:5])[0]
        print(f"  Compression: {compression}")
        print(f"  Length field: {length}")
        
        # Try to decompress
        decompressed = None
        if compression == 2:  # GZIP
            # GZIP data starts at offset 9
            gzip_data = data[9:]
            
            # Check if it starts with GZIP magic
            print(f"  GZIP data first 10 bytes: {' '.join(f'{b:02x}' for b in gzip_data[:10])}")
            
            # Try decompressing with zlib (handles both raw deflate and gzip)
            try:
                # Try with wbits=15+16 for gzip
                decompressed = zlib.decompress(gzip_data, 15 + 16)
                print(f"  ZLIB gzip decompressed: {len(decompressed)} bytes")
            except Exception as e:
                print(f"  ZLIB gzip failed: {e}")
                
                # Try raw deflate
                try:
                    decompressed = zlib.decompress(gzip_data, -15)
                    print(f"  ZLIB raw deflate decompressed: {len(decompressed)} bytes")
                except Exception as e2:
                    print(f"  ZLIB raw deflate failed: {e2}")
                    
                    # Try auto-detect
                    try:
                        decompressed = zlib.decompress(gzip_data, 15 + 32)
                        print(f"  ZLIB auto-detect decompressed: {len(decompressed)} bytes")
                    except Exception as e3:
                        print(f"  ZLIB auto-detect failed: {e3}")
        
        if decompressed:
            # Check last 2 bytes for format
            last_two = decompressed[-2:] if len(decompressed) >= 2 else b'\x00\x00'
            format_marker = struct.unpack('>h', last_two)[0]
            print(f"  Format marker: {format_marker}")
            print(f"  Last 10 bytes: {' '.join(f'{b:02x}' for b in decompressed[-10:])}")
    else:
        print(f"Model {model_id}: NOT FOUND")
    print()
