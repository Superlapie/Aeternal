#!/usr/bin/env python3
"""Check items range in cache"""
import os
import gzip
import bz2

def read_int(data, offset):
    return ((data[offset] & 0xFF) << 24) | ((data[offset + 1] & 0xFF) << 16) | ((data[offset + 2] & 0xFF) << 8) | (data[offset + 3] & 0xFF)

def read_unsigned_short(data, offset):
    return ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF)

def unpack_js5_container(data):
    if len(data) < 5:
        return data
    container_type = data[0] & 0xFF
    compressed_len = read_int(data, 1)
    
    if container_type == 0:
        return data[5:5+compressed_len]
    elif container_type == 1:
        compressed_data = data[9:9+compressed_len]
        for level in [1, 9, 8, 7, 6, 5, 4, 3, 2]:
            try:
                return bz2.decompress(b'BZh' + bytes([ord(str(level))]) + compressed_data)
            except:
                pass
        raise Exception('Could not decompress BZIP2 data')
    elif container_type == 2:
        return gzip.decompress(data[9:9+compressed_len])
    else:
        return data

# Check the openrs2-2446-flat cache which should have more items
items_path = '_ext/openrs2-2446-flat/cache/2/10.dat'
with open(items_path, 'rb') as f:
    raw_data = f.read()

decompressed = unpack_js5_container(raw_data)
print('openrs2-2446-flat items:', len(decompressed), 'bytes')

num_items = read_unsigned_short(decompressed, 0)
print('Number of items:', num_items)

# Check if perilous moon items are in range
print('Perilous moon item IDs: 29529-29540')
print('Max item ID in cache:', num_items - 1)