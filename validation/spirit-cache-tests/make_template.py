import gzip, struct
from pathlib import Path

def s(v):
    b = v.encode()
    return struct.pack('>H', len(b)) + b

def tag(t, n, payload):
    return bytes([t]) + s(n) + payload

raw = bytes([10, 0, 0])
raw += tag(3, 'DataVersion', struct.pack('>i', 3955))
raw += tag(9, 'size', bytes([3]) + struct.pack('>4i', 3, 7, 5, 7))
raw += tag(9, 'palette', bytes([10]) + struct.pack('>i', 1) + tag(8, 'Name', s('minecraft:air')) + bytes([0]))
raw += tag(9, 'blocks', bytes([10]) + struct.pack('>i', 0))
raw += tag(9, 'entities', bytes([10]) + struct.pack('>i', 0))
raw += bytes([0])
target = Path('validation/spirit-cache-tests/resources/data/neoforge/structure/empty.nbt')
target.parent.mkdir(parents=True, exist_ok=True)
target.write_bytes(gzip.compress(raw))
