#!/usr/bin/env python3
import bz2
import struct
import os

# Try different caches
cache_paths = [
    './_ext/moon_cache/cache/2/10.dat',
    './_ext/cache/2/10.dat',
]

for cache_path in cache_paths:
    if not os.path.exists(cache_path):
        print(f'Not found: {cache_path}')
        continue
    
    print(f'\n=== Processing: {cache_path} ===')
    
    with open(cache_path, 'rb') as f:
        data = f.read()
    
    compression = data[0]
    length = struct.unpack('>I', data[1:5])[0]
    print(f'Compression: {compression}')
    print(f'Length: {length}')
    print(f'Data size: {len(data)}')
    print(f'First 30 bytes: {" ".join(f"{b:02x}" for b in data[:30])}')
    
    if compression == 1:  # BZIP2
        # Try different BZIP2 approaches
        print('\nTrying BZIP2 decompression...')
        
        # Approach 1: Add BZh header
        try:
            compressed = b'BZh' + data[5:]
            decompressed = bz2.decompress(compressed)
            print(f'Approach 1 (BZh + data[5:]) SUCCESS: {len(decompressed)} bytes')
            with open('./_tmp_items.bin', 'wb') as out:
                out.write(decompressed)
            print('Saved to _tmp_items.bin')
            break
        except Exception as e:
            print(f'Approach 1 failed: {e}')
        
        # Approach 2: data[5:] directly
        try:
            decompressed = bz2.decompress(data[5:])
            print(f'Approach 2 (data[5:]) SUCCESS: {len(decompressed)} bytes')
            break
        except Exception as e:
            print(f'Approach 2 failed: {e}')
        
        # Approach 3: data[9:] (skip revision)
        try:
            decompressed = bz2.decompress(data[9:])
            print(f'Approach 3 (data[9:]) SUCCESS: {len(decompressed)} bytes')
            break
        except Exception as e:
            print(f'Approach 3 failed: {e}')
            
        # Approach 4: BZh1 (compression level 1)
        try:
            compressed = b'BZh1' + data[9:]
            decompressed = bz2.decompress(compressed)
            print(f'Approach 4 (BZh1 + data[9:]) SUCCESS: {len(decompressed)} bytes')
            # Save to file
            with open('./_tmp_items.bin', 'wb') as out:
                out.write(decompressed)
            print('Saved to _tmp_items.bin')
            break
        except Exception as e:
            print(f'Approach 4 failed: {e}')
    
    elif compression == 2:  # GZIP
        import gzip
        print('\nTrying GZIP decompression...')
        try:
            decompressed = gzip.decompress(data[9:])
            print(f'GZIP SUCCESS: {len(decompressed)} bytes')
            break
        except Exception as e:
            print(f'GZIP failed: {e}')
    
    elif compression == 0:  # Uncompressed
        print('\nUncompressed data')
        decompressed = data[5:5+length]
        print(f'Data size: {len(decompressed)} bytes')
        break
