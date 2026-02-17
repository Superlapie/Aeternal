import os

roots = ['_ext/openrs2-2446-flat/cache/2', '_ext/openrs2-2446-flat/cache/10']
targets = [b'Eclipse', b'Blood moon', b'Blue moon']

for root in roots:
    for filename in os.listdir(root):
        path = os.path.join(root, filename)
        with open(path, 'rb') as f:
            data = f.read()
            for t in targets:
                if t in data:
                    print(f'Found {t} in {path}')
