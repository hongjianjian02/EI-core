import gzip, io, json, struct, zipfile
from pathlib import Path

def read_nbt(raw):
    f = io.BytesIO(gzip.decompress(raw) if raw[:2] == bytes([31, 139]) else raw)
    def number(fmt):
        return struct.unpack('>' + fmt, f.read(struct.calcsize(fmt)))[0]
    def string():
        return f.read(number('H')).decode('utf8')
    def value(t):
        if t in (1, 2, 3, 4, 5, 6): return number({1:'b',2:'h',3:'i',4:'q',5:'f',6:'d'}[t])
        if t == 7: return list(f.read(number('i')))
        if t == 8: return string()
        if t == 9:
            kind, size = number('b'), number('i')
            return [value(kind) for _ in range(size)]
        if t == 10:
            out = {}
            while (kind := number('b')) != 0:
                name = string()
                out[name] = value(kind)
            return out
        if t in (11, 12): return [number('i' if t == 11 else 'q') for _ in range(number('i'))]
        raise ValueError(t)
    t = number('b')
    string()
    return value(t)

evidence = []
with zipfile.ZipFile('libs/neovitae.jar') as z:
    for name in z.namelist():
        if name.endswith('.nbt'):
            data = read_nbt(z.read(name))
            palette = data.get('palette', [])
            for block in data.get('blocks', []):
                if palette and palette[block['state']]['Name'] == 'neovitae:spirit_cache':
                    evidence.append(dict(structure=name, position=block['pos'], nbt=block.get('nbt', {})))
    tables = sorted({b['nbt'].get('LootTable') for b in evidence} - {None})
    result = dict(caches=evidence, tables={t:json.loads(z.read('data/' + t.replace(':','/loot_table/') + '.json')) for t in tables})
Path('output/mod-analysis/neovitae/spirit_cache_structures.json').write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding='utf8')
print('Structures with caches:', len({e['structure'] for e in evidence}), 'Caches:', len(evidence), 'Tables:', tables)
