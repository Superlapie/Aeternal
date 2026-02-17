import os

path = '_ext/openrs2-2446-flat/cache/7/39506.dat'
with open(path, 'rb') as f:
    f.seek(0)
    data = f.read(100)
    print('Hex:', data.hex(' '))
