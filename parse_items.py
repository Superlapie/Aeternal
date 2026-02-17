#!/usr/bin/env python3
"""
Parse item definitions from decompressed cache to find model IDs for perilous moon items.
"""
import struct
import os

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
            _, offset = read_unsigned_short(data, offset)
        elif opcode == 8:  # yOffset2d
            _, offset = read_unsigned_short(data, offset)
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

def parse_archive(data):
    """Parse the decompressed archive to extract item definitions"""
    items = {}
    
    # Parse archive - contains multiple files
    offset = 0
    
    # Read file count and delta-encoded file IDs
    file_count, offset = read_unsigned_short(data, offset)
    print(f"File count: {file_count}")
    
    # Read file IDs (delta encoded)
    file_ids = []
    file_id = 0
    for _ in range(file_count):
        delta, offset = read_unsigned_short(data, offset)
        file_id += delta
        file_ids.append(file_id)
    
    print(f"File ID range: {min(file_ids)} to {max(file_ids)}")
    
    # Read file sizes
    file_sizes = []
    for _ in range(file_count):
        size, offset = read_unsigned_short(data, offset)
        file_sizes.append(size)
    
    print(f"Total size from headers: {sum(file_sizes)}")
    
    # Read file data
    moon_items_found = 0
    for i, (fid, size) in enumerate(zip(file_ids, file_sizes)):
        file_data = data[offset:offset+size]
        offset += size
        
        if fid in MOON_ITEMS:
            moon_items_found += 1
            item_def = decode_item_definition(fid, file_data)
            items[fid] = item_def
            print(f"\n=== Found item {fid}: {item_def['name']} ===")
            print(f"  inventoryModel: {item_def['inventoryModel']}")
            print(f"  maleModel0: {item_def['maleModel0']}")
            print(f"  maleModel1: {item_def['maleModel1']}")
            print(f"  maleModel2: {item_def['maleModel2']}")
            print(f"  femaleModel0: {item_def['femaleModel0']}")
            print(f"  femaleModel1: {item_def['femaleModel1']}")
            print(f"  femaleModel2: {item_def['femaleModel2']}")
            print(f"  maleHeadModel: {item_def['maleHeadModel']}")
            print(f"  femaleHeadModel: {item_def['femaleHeadModel']}")
    
    print(f"\nTotal moon items found: {moon_items_found}")
    return items

def main():
    # Read decompressed data
    with open('./_tmp_items.bin', 'rb') as f:
        data = f.read()
    
    print(f"Decompressed data size: {len(data)}")
    items = parse_archive(data)
    
    # Print summary
    print("\n\n=== SUMMARY ===")
    print("Item ID -> Model ID mappings for moon equipment:")
    print("// Male equipment models (worn)")
    for item_id, name in sorted(MOON_ITEMS.items()):
        if item_id in items:
            item = items[item_id]
            # Use maleModel0 for equipment, or inventoryModel if not equipable
            model_id = item['maleModel0'] if item['maleModel0'] != -1 else item['inventoryModel']
            print(f"MOON_MODEL_MAPPINGS.put({item_id}, {model_id}); // {name}")

if __name__ == "__main__":
    main()