#!/usr/bin/env python3
"""
Extract item definitions from main_file_cache format (like openrs2-1551)
Using the same format as FileStore.java
"""
import struct
import bz2
import gzip as gzplib
import os

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

def read_unsigned_byte(data, offset):
    return data[offset], offset + 1

def read_unsigned_short(data, offset):
    return struct.unpack('>H', data[offset:offset+2])[0], offset + 2

def read_int(data, offset):
    return struct.unpack('>i', data[offset:offset+4])[0], offset + 4

def read_24bit_int(data, offset):
    return (data[offset] << 16) | (data[offset+1] << 8) | data[offset+2], offset + 3

def read_string(data, offset):
    end = offset
    while end < len(data) and data[end] != 0:
        end += 1
    return data[offset:end].decode('latin-1'), end + 1

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
            print(f"BZIP2 error: {e}")
            return None
    elif compression == 2:  # GZIP
        try:
            return gzplib.decompress(data[9:])
        except Exception as e:
            print(f"GZIP error: {e}")
            return None
    return None

class MainCache:
    def __init__(self, cache_dir):
        self.cache_dir = cache_dir
        self.dat_file = None
        self.idx_files = {}
        self._open()
    
    def _open(self):
        dat_path = os.path.join(self.cache_dir, "main_file_cache.dat2")
        if os.path.exists(dat_path):
            self.dat_file = open(dat_path, 'rb')
            print(f"Opened dat2 file: {dat_path}")
    
    def decompress(self, store_index, file_id):
        """Read a file from the cache using FileStore format"""
        idx_path = os.path.join(self.cache_dir, f"main_file_cache.idx{store_index}")
        if not os.path.exists(idx_path):
            print(f"Index file not found: {idx_path}")
            return None
        
        if self.dat_file is None:
            print("Dat file not open")
            return None
        
        with open(idx_path, 'rb') as idx_file:
            # Each index entry is 6 bytes
            idx_file.seek(file_id * 6)
            idx_data = idx_file.read(6)
            
            if len(idx_data) < 6:
                return None
            
            # 3 bytes size, 3 bytes sector
            size = ((idx_data[0] & 0xff) << 16) | ((idx_data[1] & 0xff) << 8) | (idx_data[2] & 0xff)
            sector = ((idx_data[3] & 0xff) << 16) | ((idx_data[4] & 0xff) << 8) | (idx_data[5] & 0xff)
            
            if sector <= 0:
                return None
            
            print(f"File {file_id}: size={size}, sector={sector}")
            
            # Read from dat2
            buf = bytearray()
            total_read = 0
            part = 0
            
            while total_read < size:
                self.dat_file.seek(sector * 520)
                
                unread = size - total_read
                if unread > 512:
                    unread = 512
                
                # Read header + data
                header = self.dat_file.read(8)
                if len(header) < 8:
                    break
                
                current_id = ((header[0] & 0xff) << 8) | (header[1] & 0xff)
                current_part = ((header[2] & 0xff) << 8) | (header[3] & 0xff)
                next_sector = ((header[4] & 0xff) << 16) | ((header[5] & 0xff) << 8) | (header[6] & 0xff)
                current_store = header[7] & 0xff
                
                print(f"  Part {part}: id={current_id}, part={current_part}, next={next_sector}, store={current_store}")
                
                if current_id != file_id or current_part != part or current_store != store_index:
                    print(f"  Mismatch! Expected id={file_id}, part={part}, store={store_index}")
                    return None
                
                # Read data
                data = self.dat_file.read(unread)
                buf.extend(data)
                total_read += len(data)
                
                sector = next_sector
                part += 1
                
                if sector == 0:
                    break
            
            return bytes(buf)
    
    def read_archive_files(self, data):
        """Parse archive into individual files"""
        if data is None:
            return {}
        
        # Decompress first
        decompressed = decompress_js5(data)
        if decompressed is None:
            print("Failed to decompress archive")
            return {}
        
        print(f"Decompressed size: {len(decompressed)}")
        
        # Parse archive format
        offset = 0
        file_count, offset = read_unsigned_short(decompressed, offset)
        print(f"File count: {file_count}")
        
        # Read file IDs (delta encoded)
        file_ids = []
        file_id = 0
        for _ in range(file_count):
            delta, offset = read_unsigned_short(decompressed, offset)
            file_id += delta
            file_ids.append(file_id)
        
        print(f"File ID range: {min(file_ids)} to {max(file_ids)}")
        
        # Read file sizes
        file_sizes = []
        for _ in range(file_count):
            size, offset = read_unsigned_short(decompressed, offset)
            file_sizes.append(size)
        
        # Read file data
        files = {}
        for fid, size in zip(file_ids, file_sizes):
            file_data = decompressed[offset:offset+size]
            offset += size
            files[fid] = file_data
        
        return files

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
            _, offset = read_unsigned_short(data, offset)
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
    
    return item_def

def main():
    cache_dir = "./_ext/openrs2-1551/disk/cache"
    
    print(f"Loading cache from {cache_dir}")
    cache = MainCache(cache_dir)
    
    # Index 2 contains config archives
    # Archive 10 contains items
    print("\nReading archive 10 (items) from index 2...")
    archive_data = cache.decompress(2, 10)
    
    if archive_data is None:
        print("Failed to read archive")
        return
    
    print(f"Archive data size: {len(archive_data)}")
    
    # Parse archive files
    files = cache.read_archive_files(archive_data)
    print(f"Total files in archive: {len(files)}")
    
    # Find moon items
    print("\n=== Moon Equipment Model IDs ===")
    for item_id, name in sorted(MOON_ITEMS.items()):
        if item_id in files:
            item_def = decode_item_definition(item_id, files[item_id])
            print(f"\nItem {item_id}: {item_def['name']}")
            print(f"  inventoryModel: {item_def['inventoryModel']}")
            print(f"  maleModel0: {item_def['maleModel0']}")
            print(f"  maleModel1: {item_def['maleModel1']}")
            print(f"  maleModel2: {item_def['maleModel2']}")
            print(f"  femaleModel0: {item_def['femaleModel0']}")
            print(f"  femaleModel1: {item_def['femaleModel1']}")
            print(f"  femaleModel2: {item_def['femaleModel2']}")
            print(f"  maleHeadModel: {item_def['maleHeadModel']}")
            print(f"  femaleHeadModel: {item_def['femaleHeadModel']}")
        else:
            print(f"\nItem {item_id}: {name} - NOT FOUND IN ARCHIVE")
    
    # Print summary
    print("\n\n=== SUMMARY: Java mappings ===")
    print("// Moon equipment model IDs (from openrs2-1551 cache)")
    for item_id, name in sorted(MOON_ITEMS.items()):
        if item_id in files:
            item_def = decode_item_definition(item_id, files[item_id])
            # Use maleModel0 for worn equipment, inventoryModel for inventory
            model_id = item_def['maleModel0'] if item_def['maleModel0'] != -1 else item_def['inventoryModel']
            print(f"MOON_MODEL_MAPPINGS.put({item_id}, {model_id}); // {item_def['name']}")

if __name__ == "__main__":
    main()