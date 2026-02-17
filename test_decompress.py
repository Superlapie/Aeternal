import gzip
import zlib
import os
import struct

path = '_ext/openrs2-2446-flat/cache/7/39578.dat'
with open(path, 'rb') as f:
    data = f.read()

offset = data.find(b'\x1f\x8b\x08')
if offset != -1:
    d = zlib.decompressobj(wbits=31)
    decomp = d.decompress(data[offset:])
    print(f'Decompressed size: {len(decomp)}')
    
    # Standard OSRS Type 3 footer is often at end-26 or end-28?
    # Actually, it's 2+2+1+1+1+1+1+1+1+1 + 2+2+2+2+2+2 = 22 bytes + some padding?
    # Wait, end-26 starts with UShort numVertices.
    footer = decomp[-26:]
    print('Last 10 bytes:', decomp[-10:].hex(' '))
    
    # 08 d3 0f 44 00 01 ff 01 01 00 01 00 08 36 07 25 08 20 18 6a 00 00 08 d3 ff fd
    # vals[0] = 0x08D3 = 2259 (numVertices)
    # vals[1] = 0x0F44 = 3908 (numTriangles)
    # vals[2] = 0x00 = 0 (nTextures)
    # flags = 0x01 = 1
    # ...
    
    num_v = struct.unpack('>H', footer[0:2])[0]
    num_t = struct.unpack('>H', footer[2:4])[0]
    print(f'Vertices: {num_v}, Triangles: {num_t}')
