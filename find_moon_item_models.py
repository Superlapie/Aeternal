#!/usr/bin/env python3
"""
Find the correct model IDs for perilous moon equipment by reading item definitions
from the external cache (openrs2-2446-flat).
"""
import os
import struct
import zlib
import gzip

# Item IDs for perilous moon equipment
MOON_ITEMS = {
    28988: "Blue moon spear",
    28997: "Dual macuahuitl",
    29000: "Eclipse atlatl",
    29004: "Eclipse moon chestplate",
    29007: "Eclipse moon tassets",
    29010: "Eclipse moon helm",
    29013: "Blue moon chestplate",
    29016: "Blue moon tassets",
    29019: "Blue moon helm",
    29022: "Blood moon chestplate",
    29025: "Blood moon tassets",
    29028: "Blood moon helm",
}

# External cache path
CACHE_PATH = './_ext/openrs2-2446-flat/cache'

def read_index(archive_id):
    """Read index file for an archive"""
    idx_path = os.path.join(CACHE_PATH, str(archive_id))
    if not os.path.exists(idx_path):
        return None
    
    # Index files in flat cache are individual .dat files
    # We need to find the index data
    return idx_path

def decode_item_definition(data, item_id):
    """Decode an item definition to find model IDs"""
    if data is None or len(data) < 2:
        return None
    
    # Item definitions are encoded using Runescape's format
    # The format uses a buffer with opcodes
    
    result = {
        'id': item_id,
        'model_id': None,
        'male_model': None,
        'female_model': None,
        'male_dialogue_model': None,
        'female_dialogue_model': None,
        'name': None,
    }
    
    try:
        # Decode the item definition
        # Format: starts with data length, then opcodes
        pos = 0
        
        # Try to decode as RS2 item definition
        while pos < len(data):
            if pos >= len(data):
                break
            
            opcode = data[pos] & 0xFF
            pos += 1
            
            if opcode == 0:
                break
            elif opcode == 1:  # Model ID
                if pos + 2 < len(data):
                    result['model_id'] = ((data[pos] & 0xFF) << 16) | ((data[pos+1] & 0xFF) << 8) | (data[pos+2] & 0xFF)
                    pos += 3
            elif opcode == 2:  # Name
                if pos < len(data):
                    name_len = data[pos] & 0xFF
                    pos += 1
                    if pos + name_len <= len(data):
                        result['name'] = data[pos:pos+name_len].decode('utf-8', errors='replace')
                        pos += name_len
            elif opcode == 3:  # Stackable
                pass
            elif opcode == 4:  # Model scale
                if pos + 1 < len(data):
                    pos += 2
            elif opcode == 5:  # Model rotation
                if pos + 1 < len(data):
                    pos += 2
            elif opcode == 6:  # Model translation
                if pos + 1 < len(data):
                    pos += 2
            elif opcode == 7:  # Wear position
                if pos + 1 < len(data):
                    pos += 2
            elif opcode == 8:  # Members
                pass
            elif opcode == 11:  # Stack count
                if pos + 1 < len(data):
                    pos += 2
            elif opcode == 12:  # Ground actions
                pass
            elif opcode == 16:  # Note ID
                pass
            elif opcode == 23:  # Male model
                if pos + 2 < len(data):
                    result['male_model'] = ((data[pos] & 0xFF) << 16) | ((data[pos+1] & 0xFF) << 8) | (data[pos+2] & 0xFF)
                    pos += 3
            elif opcode == 24:  # Male model offset
                if pos + 1 < len(data):
                    pos += 2
            elif opcode == 25:  # Female model
                if pos + 2 < len(data):
                    result['female_model'] = ((data[pos] & 0xFF) << 16) | ((data[pos+1] & 0xFF) << 8) | (data[pos+2] & 0xFF)
                    pos += 3
            elif opcode == 26:  # Female model offset
                if pos + 1 < len(data):
                    pos += 2
            elif opcode >= 30 and opcode < 35:  # Ground actions
                if pos < len(data):
                    action_len = data[pos] & 0xFF
                    pos += 1 + action_len
            elif opcode >= 35 and opcode < 40:  # Inventory actions
                if pos < len(data):
                    action_len = data[pos] & 0xFF
                    pos += 1 + action_len
            elif opcode == 40:  # Color replacements
                if pos < len(data):
                    count = data[pos] & 0xFF
                    pos += 1 + count * 4
            elif opcode == 41:  # Texture replacements
                if pos < len(data):
                    count = data[pos] & 0xFF
                    pos += 1 + count * 4
            elif opcode == 42:  # Shift click action
                if pos + 1 < len(data):
                    pos += 1
            elif opcode == 65:  # Unnoted
                pass
            elif opcode == 78:  # Male dialogue model
                if pos + 2 < len(data):
                    result['male_dialogue_model'] = ((data[pos] & 0xFF) << 16) | ((data[pos+1] & 0xFF) << 8) | (data[pos+2] & 0xFF)
                    pos += 3
            elif opcode == 79:  # Female dialogue model
                if pos + 2 < len(data):
                    result['female_dialogue_model'] = ((data[pos] & 0xFF) << 16) | ((data[pos+1] & 0xFF) << 8) | (data[pos+2] & 0xFF)
                    pos += 3
            elif opcode == 90:  # Male model 2
                if pos + 2 < len(data):
                    pos += 3
            elif opcode == 91:  # Female model 2
                if pos + 2 < len(data):
                    pos += 3
            elif opcode == 92:  # Male model 3
                if pos + 2 < len(data):
                    pos += 3
            elif opcode == 93:  # Female model 3
                if pos + 2 < len(data):
                    pos += 3
            elif opcode == 94:  # Category
                if pos + 1 < len(data):
                    pos += 2
            elif opcode == 95:  # Model rotation 2
                if pos + 1 < len(data):
                    pos += 2
            elif opcode == 97:  # Noted ID
                if pos + 1 < len(data):
                    pos += 2
            elif opcode == 98:  # Noted template
                if pos + 1 < len(data):
                    pos += 2
            elif opcode >= 100 and opcode < 110:  # Count objects
                if pos + 3 < len(data):
                    pos += 4
            elif opcode == 110:  # Model scale 2
                if pos + 1 < len(data):
                    pos += 2
            elif opcode == 111:  # Model translation 2
                if pos + 1 < len(data):
                    pos += 2
            elif opcode == 112:  # Model translation 3
                if pos + 1 < len(data):
                    pos += 2
            elif opcode == 113:  # Light modifier
                if pos + 1 < len(data):
                    pos += 1
            elif opcode == 114:  # Shadow modifier
                if pos + 1 < len(data):
                    pos += 1
            elif opcode == 115:  # Team
                if pos + 1 < len(data):
                    pos += 2
            elif opcode == 139:  # Bought item ID
                if pos + 1 < len(data):
                    pos += 2
            elif opcode == 140:  # Sold item ID
                if pos + 1 < len(data):
                    pos += 2
            elif opcode == 148:  # Placeholder ID
                if pos + 1 < len(data):
                    pos += 2
            elif opcode == 149:  # Placeholder template
                if pos + 1 < len(data):
                    pos += 2
            elif opcode == 249:  # Params
                if pos < len(data):
                    count = data[pos] & 0xFF
                    pos += 1
                    for _ in range(count):
                        is_string = data[pos] & 0xFF
                        pos += 1
                        key = ((data[pos] & 0xFF) << 8) | (data[pos+1] & 0xFF)
                        pos += 2
                        if is_string == 1:
                            str_len = data[pos] & 0xFF
                            pos += 1 + str_len
                        else:
                            pos += 4
            else:
                # Unknown opcode, try to skip
                print(f"Unknown opcode {opcode} at position {pos-1} for item {item_id}")
                break
    
    except Exception as e:
        print(f"Error decoding item {item_id}: {e}")
    
    return result

def decompress_archive_data(data):
    """Decompress archive data (JS5 format)"""
    if data is None or len(data) < 5:
        return None
    
    # Check compression type
    compression = data[0] & 0xFF
    if compression == 0:  # No compression
        return data[5:]
    elif compression == 1:  # BZIP2
        import bz2
        return bz2.decompress(data[9:])
    elif compression == 2:  # GZIP
        return zlib.decompress(data[9:], 15 + 16)
    elif compression == 3:  # LZMA
        return None  # Not implemented
    else:
        return None

def main():
    print("=== Finding perilous moon item model IDs ===")
    print()
    
    # In the flat cache, items are in archive 10 (config archive)
    # The flat cache structure is: cache/archive_id/file_id.dat
    
    # First, let's check if we can find the config archive
    config_path = os.path.join(CACHE_PATH, '10')
    if not os.path.exists(config_path):
        print("Config archive (10) not found")
        return
    
    # In flat cache, items are typically in a specific file within the config archive
    # Let's look for item definitions
    
    # The items archive is typically file 10 within the config archive
    # But in flat cache format, it might be different
    
    # Let's try to find item definition files
    # In OSRS cache, items are in archive 10, file 10
    
    # Check for the items file
    items_file = os.path.join(config_path, '10.dat')
    if os.path.exists(items_file):
        print(f"Found items file: {items_file}")
        with open(items_file, 'rb') as f:
            data = f.read()
        print(f"Items file size: {len(data)} bytes")
        
        # Try to decompress
        decompressed = decompress_archive_data(data)
        if decompressed:
            print(f"Decompressed size: {len(decompressed)} bytes")
            
            # Parse item definitions
            # The format is: count (2 bytes), then delta-encoded IDs, then definitions
            pos = 0
            count = ((decompressed[pos] & 0xFF) << 8) | (decompressed[pos+1] & 0xFF)
            pos += 2
            print(f"Item count: {count}")
            
            # Read item IDs (delta encoded)
            item_ids = []
            current_id = 0
            for _ in range(count):
                delta = 0
                shift = 0
                while True:
                    b = decompressed[pos] & 0xFF
                    pos += 1
                    delta |= (b & 0x7F) << shift
                    shift += 7
                    if (b & 0x80) == 0:
                        break
                current_id += delta
                item_ids.append(current_id)
            
            print(f"First 20 item IDs: {item_ids[:20]}")
            print(f"Last 20 item IDs: {item_ids[-20:]}")
            
            # Check if our moon items are in the list
            for item_id in MOON_ITEMS.keys():
                if item_id in item_ids:
                    print(f"Found item {item_id} ({MOON_ITEMS[item_id]}) at index {item_ids.index(item_id)}")
                else:
                    print(f"Item {item_id} ({MOON_ITEMS[item_id]}) NOT FOUND")
    
    # Also check for individual item files in the flat cache
    print()
    print("=== Checking for individual item files ===")
    for item_id, name in MOON_ITEMS.items():
        item_file = os.path.join(config_path, f'{item_id}.dat')
        if os.path.exists(item_file):
            with open(item_file, 'rb') as f:
                data = f.read()
            print(f"Found item {item_id} ({name}): {len(data)} bytes")
            
            # Try to decode
            result = decode_item_definition(data, item_id)
            if result:
                print(f"  Model ID: {result.get('model_id')}")
                print(f"  Male model: {result.get('male_model')}")
                print(f"  Female model: {result.get('female_model')}")
        else:
            print(f"Item {item_id} ({name}) file not found")

if __name__ == '__main__':
    main()
