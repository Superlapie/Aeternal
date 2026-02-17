#!/usr/bin/env python3
"""
Extract item definitions from OSRS cache to find model IDs for perilous moon items.
"""
import os
import struct
import gzip
import bz2
from pathlib import Path

# Item IDs for perilous moon equipment (from RuneLite ItemID.java)
MOON_ITEMS = {
    28988: "Blue moon spear (FROSTMOON_SPEAR)",
    28997: "Dual macuahuitl",
    29000: "Eclipse atlatl",
    29004: "Eclipse moon chestplate",
    29007: "Eclipse moon tassets",
    29010: "Eclipse moon helm",
    29013: "Blue moon chestplate (FROST_MOON_CHESTPLATE)",
    29016: "Blue moon tassets (FROST_MOON_TASSETS)",
    29019: "Blue moon helm (FROST_MOON_HELM)",
    29022: "Blood moon chestplate",
    29025: "Blood moon tassets",
    29028: "Blood moon helm",
}

def read_unsigned_byte(data, offset):
    return data[offset], offset + 1

def read_unsigned_short(data, offset):
    return struct.unpack('>H', data[offset:offset+2])[0], offset + 2

def read_short(data, offset):
    return struct.unpack('>h', data[offset:offset+2])[0], offset + 2

def read_int(data, offset):
    return struct.unpack('>i', data[offset:offset+4])[0], offset + 4

def read_24bit_int(data, offset):
    return (data[offset] << 16) | (data[offset+1] << 8) | data[offset+2], offset + 3

def read_string(data, offset):
    end = offset
    while end < len(data) and data[end] != 0:
        end += 1
    return data[offset:end].decode('latin-1'), end + 1

def decompress_archive(data):
    """Decompress JS5 container format"""
    if len(data) < 5:
        return None
    
    compression = data[0]
    length = struct.unpack('>I', data[1:5])[0]
    
    if compression == 0:  # Uncompressed
        return data[5:5+length]
    elif compression == 1:  # BZIP2
        try:
            # BZIP2 has a magic header we need to add
            compressed_data = b'BZh' + data[9:5+length+4]
            return bz2.decompress(compressed_data)
        except:
            return None
    elif compression == 2:  # GZIP
        try:
            return gzip.decompress(data[9:5+length+4])
        except:
            return None
    return None

def decode_item_definition(item_id, data):
    """Decode item definition from raw data"""
    offset = 0
    item_def = {
        'id': item_id,
        'name': 'null',
        'inventoryModel': 0,
        'maleModel0': -1,
        'maleModel1': -1,
        'maleModel2': -1,
        'femaleModel0': -1,
        'femaleModel1': -1,
        'femaleModel2': -1,
        'maleHeadModel': -1,
        'femaleHeadModel': -1,
    }
    
    while offset < len(data):
        opcode, offset = read_unsigned_byte(data, offset)
        if opcode == 0:
            break
        
        if opcode == 1:  # inventoryModel
            item_def['inventoryModel'], offset = read_unsigned_short(data, offset)
        elif opcode == 2:  # name
            item_def['name'], offset = read_string(data, offset)
        elif opcode == 3:  # examine
            _, offset = read_string(data, offset)
        elif opcode == 4:  # zoom2d
            _, offset = read_unsigned_short(data, offset)
        elif opcode == 5:  # xan2d
            _, offset = read_unsigned_short(data, offset)
        elif opcode == 6:  # yan2d
            _, offset = read_unsigned_short(data, offset)
        elif opcode == 7:  # xOffset2d
            val, offset = read_unsigned_short(data, offset)
        elif opcode == 8:  # yOffset2d
            val, offset = read_unsigned_short(data, offset)
        elif opcode == 11:  # stackable
            pass
        elif opcode == 12:  # cost
            _, offset = read_int(data, offset)
        elif opcode == 13:  # wearPos1
            _, offset = read_unsigned_byte(data, offset)
        elif opcode == 14:  # wearPos2
            _, offset = read_unsigned_byte(data, offset)
        elif opcode == 16:  # members
            pass
        elif opcode == 23:  # maleModel0
            item_def['maleModel0'], offset = read_unsigned_short(data, offset)
            _, offset = read_unsigned_byte(data, offset)  # maleOffset
        elif opcode == 24:  # maleModel1
            item_def['maleModel1'], offset = read_unsigned_short(data, offset)
        elif opcode == 25:  # femaleModel0
            item_def['femaleModel0'], offset = read_unsigned_short(data, offset)
            _, offset = read_unsigned_byte(data, offset)  # femaleOffset
        elif opcode == 26:  # femaleModel1
            item_def['femaleModel1'], offset = read_unsigned_short(data, offset)
        elif opcode == 27:  # wearPos3
            _, offset = read_unsigned_byte(data, offset)
        elif opcode >= 30 and opcode < 35:  # options
            _, offset = read_string(data, offset)
        elif opcode >= 35 and opcode < 40:  # interfaceOptions
            _, offset = read_string(data, offset)
        elif opcode == 40:  # colorFind/colorReplace
            count, offset = read_unsigned_byte(data, offset)
            for _ in range(count):
                _, offset = read_unsigned_short(data, offset)
                _, offset = read_unsigned_short(data, offset)
        elif opcode == 41:  # textureFind/textureReplace
            count, offset = read_unsigned_byte(data, offset)
            for _ in range(count):
                _, offset = read_unsigned_short(data, offset)
                _, offset = read_unsigned_short(data, offset)
        elif opcode == 42:  # shiftClickDropIndex
            _, offset = read_unsigned_byte(data, offset)
        elif opcode == 65:  # isTradeable
            pass
        elif opcode == 75:  # weight
            _, offset = read_short(data, offset)
        elif opcode == 78:  # maleModel2
            item_def['maleModel2'], offset = read_unsigned_short(data, offset)
        elif opcode == 79:  # femaleModel2
            item_def['femaleModel2'], offset = read_unsigned_short(data, offset)
        elif opcode == 90:  # maleHeadModel
            item_def['maleHeadModel'], offset = read_unsigned_short(data, offset)
        elif opcode == 91:  # femaleHeadModel
            item_def['femaleHeadModel'], offset = read_unsigned_short(data, offset)
        elif opcode == 92:  # maleHeadModel2
            item_def['maleHeadModel2'], offset = read_unsigned_short(data, offset)
        elif opcode == 93:  # femaleHeadModel2
            item_def['femaleHeadModel2'], offset = read_unsigned_short(data, offset)
        elif opcode == 94:  # category
            _, offset = read_unsigned_short(data, offset)
        elif opcode == 95:  # zan2d
            _, offset = read_unsigned_short(data, offset)
        elif opcode == 97:  # notedID
            _, offset = read_unsigned_short(data, offset)
        elif opcode == 98:  # notedTemplate
            _, offset = read_unsigned_short(data, offset)
        elif opcode >= 100 and opcode < 110:  # countObj/countCo
            _, offset = read_unsigned_short(data, offset)
            _, offset = read_unsigned_short(data, offset)
        elif opcode == 110:  # resizeX
            _, offset = read_unsigned_short(data, offset)
        elif opcode == 111:  # resizeY
            _, offset = read_unsigned_short(data, offset)
        elif opcode == 112:  # resizeZ
            _, offset = read_unsigned_short(data, offset)
        elif opcode == 113:  # ambient
            _, offset = read_unsigned_byte(data, offset)
        elif opcode == 114:  # contrast
            _, offset = read_unsigned_byte(data, offset)
        elif opcode == 115:  # team
            _, offset = read_unsigned_byte(data, offset)
        elif opcode == 139:  # boughtId
            _, offset = read_unsigned_short(data, offset)
        elif opcode == 140:  # boughtTemplateId
            _, offset = read_unsigned_short(data, offset)
        elif opcode == 148:  # placeholderId
            _, offset = read_unsigned_short(data, offset)
        elif opcode == 149:  # placeholderTemplateId
            _, offset = read_unsigned_short(data, offset)
        elif opcode == 249:  # params
            length, offset = read_unsigned_byte(data, offset)
            for _ in range(length):
                is_string, offset = read_unsigned_byte(data, offset)
                _, offset = read_24bit_int(data, offset)
                if is_string == 1:
                    _, offset = read_string(data, offset)
                else:
                    _, offset = read_int(data, offset)
        else:
            print(f"  Unknown opcode {opcode} at offset {offset-1}")
            break
    
    return item_def

def load_from_cache(cache_path):
    """Load item definitions from a flat cache (index 2, archive 10)"""
    items = {}
    
    # Try flat cache format (index/archive/file)
    idx2_path = Path(cache_path) / "2"
    archive_path = idx2_path / "10.dat"
    
    if not archive_path.exists():
        print(f"Archive not found: {archive_path}")
        return items
    
    # Read archive 10 (items)
    with open(archive_path, 'rb') as f:
        archive_data = f.read()
    
    # Decompress
    decompressed = decompress_archive(archive_data)
    if decompressed is None:
        print("Failed to decompress archive")
        return items
    
    print(f"Decompressed archive: {len(decompressed)} bytes")
    
    # Parse archive - contains multiple files
    offset = 0
    
    # Read file count and delta-encoded file IDs
    file_count, offset = read_unsigned_short(decompressed, offset)
    print(f"File count: {file_count}")
    
    # Read file IDs (delta encoded)
    file_ids = []
    file_id = 0
    for _ in range(file_count):
        delta, offset = read_unsigned_short(decompressed, offset)
        file_id += delta
        file_ids.append(file_id)
    
    # Read file sizes
    file_sizes = []
    for _ in range(file_count):
        size, offset = read_unsigned_short(decompressed, offset)
        file_sizes.append(size)
    
    # Read file data
    for i, (file_id, size) in enumerate(zip(file_ids, file_sizes)):
        file_data = decompressed[offset:offset+size]
        offset += size
        
        if file_id in MOON_ITEMS:
            item_def = decode_item_definition(file_id, file_data)
            items[file_id] = item_def
            print(f"\nFound item {file_id}: {item_def['name']}")
            print(f"  inventoryModel: {item_def['inventoryModel']}")
            print(f"  maleModel0: {item_def['maleModel0']}")
            print(f"  maleModel1: {item_def['maleModel1']}")
            print(f"  maleModel2: {item_def['maleModel2']}")
            print(f"  femaleModel0: {item_def['femaleModel0']}")
            print(f"  femaleModel1: {item_def['femaleModel1']}")
            print(f"  femaleModel2: {item_def['femaleModel2']}")
            print(f"  maleHeadModel: {item_def['maleHeadModel']}")
            print(f"  femaleHeadModel: {item_def['femaleHeadModel']}")
    
    return items

def main():
    cache_paths = [
        "./_ext/moon_cache/cache",
        "./_ext/cache",
        "./_ext/openrs2-2446-flat/cache",
    ]
    
    for cache_path in cache_paths:
        if os.path.exists(cache_path):
            print(f"\n=== Trying cache: {cache_path} ===")
            items = load_from_cache(cache_path)
            if items:
                print(f"\n=== Found {len(items)} moon items ===")
                break

if __name__ == "__main__":
    main()
