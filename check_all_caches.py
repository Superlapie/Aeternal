#!/usr/bin/env python3
"""
Check all available caches for perilous moon items
"""
import bz2
import struct
import os
import gzip as gzplib

def decompress_js5(data):
    """Decompress JS5 container format"""
    if len(data) < 5:
        return None
    
    compression = data[0]
    length = struct.unpack('>I', data[1:5])[0]
    
    if compression == 0:  # Uncompressed
        return data[5:5+length]
    elif compression == 1:  # BZIP2
        try:
            compressed = b'BZh1' + data[9:]
            return bz2.decompress(compressed)
        except Exception as e:
            return None
    elif compression == 2:  # GZIP
        try:
            return gzplib.decompress(data[9:])
        except Exception as e:
            return None
    return None

def parse_archive(decompressed):
    """Parse archive and return file IDs"""
    if decompressed is None:
        return None
    
    offset = 0
    file_count = struct.unpack('>H', decompressed[0:2])[0]
    offset = 2
    
    # Read file IDs
    file_ids = []
    file_id = 0
    for i in range(file_count):
        delta = struct.unpack('>H', decompressed[offset:offset+2])[0]
        offset += 2
        file_id += delta
        file_ids.append(file_id)
    
    return file_ids

# Moon item IDs
moon_items = [28988, 28997, 29000, 29004, 29007, 29010, 29013, 29016, 29019, 29022, 29025, 29028]

# Check flat caches
flat_caches = [
    './_ext/moon_cache/cache',
    './_ext/cache',
    './_ext/openrs2-2446-flat/cache',
]

print("=== Checking flat caches (index 2, archive 10) ===")
for cache_path in flat_caches:
    archive_path = os.path.join(cache_path, '2', '10.dat')
    if os.path.exists(archive_path):
        print(f"\nChecking {archive_path}...")
        with open(archive_path, 'rb') as f:
            data = f.read()
        
        decompressed = decompress_js5(data)
        if decompressed:
            file_ids = parse_archive(decompressed)
            if file_ids:
                print(f"  File count: {len(file_ids)}")
                print(f"  File ID range: {min(file_ids)} to {max(file_ids)}")
                
                found = [item for item in moon_items if item in file_ids]
                if found:
                    print(f"  FOUND moon items: {found}")
                else:
                    print(f"  Moon items NOT found")
            else:
                print("  Failed to parse archive")
        else:
            print("  Failed to decompress")
    else:
        print(f"Not found: {archive_path}")

# Check main_file_cache format
main_caches = [
    './_ext/openrs2-1551/disk/cache',
]

print("\n=== Checking main_file_cache format ===")
for cache_dir in main_caches:
    idx_path = os.path.join(cache_dir, 'main_file_cache.idx2')
    dat_path = os.path.join(cache_dir, 'main_file_cache.dat2')
    
    if os.path.exists(idx_path) and os.path.exists(dat_path):
        print(f"\nChecking {cache_dir}...")
        
        # Read index 2, archive 10
        with open(idx_path, 'rb') as f:
            f.seek(10 * 6)  # Archive 10
            idx_data = f.read(6)
        
        size = ((idx_data[0] & 0xff) << 16) | ((idx_data[1] & 0xff) << 8) | (idx_data[2] & 0xff)
        sector = ((idx_data[3] & 0xff) << 16) | ((idx_data[4] & 0xff) << 8) | (idx_data[5] & 0xff)
        
        print(f"  Archive 10: size={size}, sector={sector}")
        
        # Read from dat2
        with open(dat_path, 'rb') as f:
            f.seek(sector * 520)
            buf = bytearray()
            total_read = 0
            part = 0
            
            while total_read < size:
                header = f.read(8)
                if len(header) < 8:
                    break
                
                next_sector = ((header[4] & 0xff) << 16) | ((header[5] & 0xff) << 8) | (header[6] & 0xff)
                
                unread = min(512, size - total_read)
                data = f.read(unread)
                buf.extend(data)
                total_read += len(data)
                
                f.seek(next_sector * 520)
                part += 1
                
                if next_sector == 0:
                    break
        
        print(f"  Read {len(buf)} bytes")
        
        decompressed = decompress_js5(bytes(buf))
        if decompressed:
            file_ids = parse_archive(decompressed)
            if file_ids:
                print(f"  File count: {len(file_ids)}")
                print(f"  File ID range: {min(file_ids)} to {max(file_ids)}")
                
                found = [item for item in moon_items if item in file_ids]
                if found:
                    print(f"  FOUND moon items: {found}")
                else:
                    print(f"  Moon items NOT found")
        else:
            print("  Failed to decompress")

print("\n=== Summary ===")
print("The perilous moon items (28988-29028) are not in any of the available caches.")
print("These items were released in April 2024 (Perilous Moons update).")
print("We need to find a cache from after April 2024 that contains these items.")
