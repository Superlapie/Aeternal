#!/usr/bin/env python3
"""
Find the correct perilous moon equipment item IDs and model IDs from the external cache.
"""
import os
import gzip
import struct

def read_int(data, offset):
    return ((data[offset] & 0xFF) << 24) | ((data[offset + 1] & 0xFF) << 16) | ((data[offset + 2] & 0xFF) << 8) | (data[offset + 3] & 0xFF)

def read_unsigned_short(data, offset):
    return ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF)

def read_unsigned_byte(data, offset):
    return data[offset] & 0xFF

def unpack_js5_container(data):
    if len(data) < 5:
        return data
    container_type = data[0] & 0xFF
    compressed_len = read_int(data, 1)
    
    if container_type == 0:
        return data[5:5+compressed_len]
    elif container_type == 2:
        return gzip.decompress(data[9:9+compressed_len])
    else:
        return data

# Perilous Moon equipment item IDs from OSRS Wiki
# Eclipse Moon set
ECLIPSE_MOON_HELM = 29529
ECLIPSE_MOON_CHESTPLATE = 29530
ECLIPSE_MOON_TASSETS = 29531
ECLIPSE_ATLATL = 29532

# Blood Moon set
BLOOD_MOON_HELM = 29533
BLOOD_MOON_CHESTPLATE = 29534
BLOOD_MOON_TASSETS = 29535
DUAL_MACUAHUITL = 29536

# Blue Moon set
BLUE_MOON_HELM = 29537
BLUE_MOON_CHESTPLATE = 29538
BLUE_MOON_TASSETS = 29539
BLUE_MOON_SPEAR = 29540

MOON_ITEMS = [
    ECLIPSE_MOON_HELM, ECLIPSE_MOON_CHESTPLATE, ECLIPSE_MOON_TASSETS, ECLIPSE_ATLATL,
    BLOOD_MOON_HELM, BLOOD_MOON_CHESTPLATE, BLOOD_MOON_TASSETS, DUAL_MACUAHUITL,
    BLUE_MOON_HELM, BLUE_MOON_CHESTPLATE, BLUE_MOON_TASSETS, BLUE_MOON_SPEAR
]

ITEM_NAMES = {
    ECLIPSE_MOON_HELM: "Eclipse moon helm",
    ECLIPSE_MOON_CHESTPLATE: "Eclipse moon chestplate",
    ECLIPSE_MOON_TASSETS: "Eclipse moon tassets",
    ECLIPSE_ATLATL: "Eclipse atlatl",
    BLOOD_MOON_HELM: "Blood moon helm",
    BLOOD_MOON_CHESTPLATE: "Blood moon chestplate",
    BLOOD_MOON_TASSETS: "Blood moon tassets",
    DUAL_MACUAHUITL: "Dual macuahuitl",
    BLUE_MOON_HELM: "Blue moon helm",
    BLUE_MOON_CHESTPLATE: "Blue moon chestplate",
    BLUE_MOON_TASSETS: "Blue moon tassets",
    BLUE_MOON_SPEAR: "Blue moon spear"
}

def parse_item_definition(data, item_id):
    """Parse item definition to extract model IDs"""
    if len(data) < 2:
        return None
    
    offset = 0
    result = {
        'inventory_model': -1,
        'male_model_1': -1,
        'male_model_2': -1,
        'female_model_1': -1,
        'female_model_2': -1,
        'name': None
    }
    
    try:
        while offset < len(data):
            opcode = read_unsigned_byte(data, offset)
            offset += 1
            
            if opcode == 0:
                break
            elif opcode == 1:  # inventory model
                result['inventory_model'] = read_unsigned_short(data, offset)
                offset += 2
            elif opcode == 2:  # name
                name_len = read_unsigned_byte(data, offset)
                offset += 1
                result['name'] = data[offset:offset+name_len].decode('latin-1')
                offset += name_len
            elif opcode == 5:  # male model 1
                result['male_model_1'] = read_unsigned_short(data, offset)
                offset += 2
            elif opcode == 6:  # male model 2
                result['male_model_2'] = read_unsigned_short(data, offset)
                offset += 2
            elif opcode == 7:  # female model 1
                result['female_model_1'] = read_unsigned_short(data, offset)
                offset += 2
            elif opcode == 8:  # female model 2
                result['female_model_2'] = read_unsigned_short(data, offset)
                offset += 2
            else:
                # Skip unknown opcodes - this is simplified
                break
    except Exception as e:
        print(f"Error parsing item {item_id}: {e}")
        return None
    
    return result

def main():
    cache_dir = '_ext/moon_cache/cache'
    
    # Read items archive (index 2, archive 10)
    items_path = os.path.join(cache_dir, '2', '10.dat')
    if not os.path.exists(items_path):
        print(f"Items archive not found: {items_path}")
        return
    
    with open(items_path, 'rb') as f:
        raw_data = f.read()
    
    unpacked = unpack_js5_container(raw_data)
    print(f"Items archive: {len(raw_data)} -> {len(unpacked)} bytes")
    
    # The unpacked data contains all item definitions
    # First 2 bytes are the number of items
    num_items = read_unsigned_short(unpacked, 0)
    print(f"Number of items: {num_items}")
    
    # Following are offsets for each item definition
    offsets = []
    offset_pos = 2
    for i in range(num_items):
        if offset_pos + 2 > len(unpacked):
            break
        item_offset = read_unsigned_short(unpacked, offset_pos)
        offsets.append(item_offset)
        offset_pos += 2
    
    print(f"Read {len(offsets)} item offsets")
    
    # Check if moon items are in range
    max_item_id = max(MOON_ITEMS)
    if max_item_id >= len(offsets):
        print(f"Warning: Max moon item ID {max_item_id} >= number of items {len(offsets)}")
    
    # Parse moon item definitions
    print("\n=== Moon Equipment Item Definitions ===")
    for item_id in MOON_ITEMS:
        if item_id < len(offsets):
            item_offset = offsets[item_id]
            if item_id + 1 < len(offsets):
                next_offset = offsets[item_id + 1]
            else:
                next_offset = len(unpacked)
            
            item_data = unpacked[item_offset:next_offset]
            result = parse_item_definition(item_data, item_id)
            
            if result:
                print(f"\n{ITEM_NAMES.get(item_id, f'Item {item_id}')}:")
                print(f"  Inventory model: {result['inventory_model']}")
                print(f"  Male model 1: {result['male_model_1']}")
                print(f"  Male model 2: {result['male_model_2']}")
                print(f"  Female model 1: {result['female_model_1']}")
                print(f"  Female model 2: {result['female_model_2']}")
                print(f"  Name: {result['name']}")
        else:
            print(f"\n{ITEM_NAMES.get(item_id, f'Item {item_id}')}: NOT IN CACHE")

if __name__ == '__main__':
    main()
