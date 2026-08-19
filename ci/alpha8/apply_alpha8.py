from pathlib import Path
from PIL import Image, ImageDraw
from io import BytesIO
import json, math, re, sys, urllib.request, zipfile

root = Path(sys.argv[1])

# -----------------------------------------------------------------------------
# Version
# -----------------------------------------------------------------------------
p = root / 'gradle.properties'
t = p.read_text()
t = re.sub(r'mod_version=0\.3\.0-alpha\.7\+1\.21\.1', 'mod_version=0.3.0-alpha.8+1.21.1', t)
p.write_text(t)

# -----------------------------------------------------------------------------
# Download the official Minecraft 1.21.1 client so we can use the EXACT
# vanilla netherite silhouettes/UV layouts instead of redrawing them by eye.
# -----------------------------------------------------------------------------
MANIFEST_URL = 'https://piston-meta.mojang.com/mc/game/version_manifest_v2.json'
with urllib.request.urlopen(MANIFEST_URL, timeout=60) as r:
    manifest = json.load(r)
version_entry = next(v for v in manifest['versions'] if v['id'] == '1.21.1')
with urllib.request.urlopen(version_entry['url'], timeout=60) as r:
    version_json = json.load(r)
client_url = version_json['downloads']['client']['url']
with urllib.request.urlopen(client_url, timeout=120) as r:
    client_bytes = r.read()
client = zipfile.ZipFile(BytesIO(client_bytes))

tex_root = root / 'src/main/resources/assets/chainacobblemon/textures'
item_dir = tex_root / 'item'
armor_dir = tex_root / 'models/armor'
model_dir = root / 'src/main/resources/assets/chainacobblemon/models/item'
item_dir.mkdir(parents=True, exist_ok=True)
armor_dir.mkdir(parents=True, exist_ok=True)
model_dir.mkdir(parents=True, exist_ok=True)

# Chaina palette: black/dark plum + pink + a small gold highlight range.
PALETTE = [
    (29, 24, 34),
    (46, 36, 52),
    (69, 48, 66),
    (103, 61, 88),
    (154, 66, 108),
    (211, 82, 139),
    (242, 112, 165),
    (255, 161, 198),
    (255, 199, 220),
]
GOLD = (229, 178, 66)
GOLD_LIGHT = (255, 219, 118)


def read_png(path: str) -> Image.Image:
    with client.open(path) as f:
        return Image.open(BytesIO(f.read())).convert('RGBA')


def vanilla_candidates(*paths):
    names = set(client.namelist())
    for pth in paths:
        if pth in names:
            return pth
    raise RuntimeError('Could not find any vanilla asset candidate: ' + ', '.join(paths))


def luminance(rgb):
    r, g, b = rgb
    return 0.2126*r + 0.7152*g + 0.0722*b


def recolor_exact_shape(src: Image.Image, *, gold_accents=False) -> Image.Image:
    out = Image.new('RGBA', src.size, (0, 0, 0, 0))
    srcpx = src.load(); dst = out.load()
    opaque = []
    for y in range(src.height):
        for x in range(src.width):
            r,g,b,a = srcpx[x,y]
            if a:
                opaque.append(luminance((r,g,b)))
    if not opaque:
        return src.copy()
    lo, hi = min(opaque), max(opaque)
    span = max(1.0, hi-lo)
    for y in range(src.height):
        for x in range(src.width):
            r,g,b,a = srcpx[x,y]
            if a == 0:
                dst[x,y] = (0,0,0,0)
                continue
            lum = luminance((r,g,b))
            q = (lum-lo)/span
            idx = min(len(PALETTE)-1, max(0, int(round(q*(len(PALETTE)-1)))))
            c = PALETTE[idx]
            if gold_accents and q > 0.965:
                c = GOLD_LIGHT if q > 0.99 else GOLD
            dst[x,y] = (c[0], c[1], c[2], a)
    return out


for vanilla_name, chaina_name in [
    ('netherite_sword', 'chaina_sword'),
    ('netherite_pickaxe', 'chaina_pickaxe'),
    ('netherite_axe', 'chaina_axe'),
    ('netherite_shovel', 'chaina_shovel'),
    ('netherite_hoe', 'chaina_hoe'),
    ('netherite_helmet', 'chaina_helmet'),
    ('netherite_chestplate', 'chaina_chestplate'),
    ('netherite_leggings', 'chaina_leggings'),
    ('netherite_boots', 'chaina_boots'),
]:
    src_path = vanilla_candidates(f'assets/minecraft/textures/item/{vanilla_name}.png')
    src = read_png(src_path)
    dst = recolor_exact_shape(src, gold_accents=True)
    dst.save(item_dir / f'{chaina_name}.png')
    if list(src.getchannel('A').getdata()) != list(dst.getchannel('A').getdata()):
        raise RuntimeError(f'Alpha mask changed for {chaina_name}')

for tool in ['chaina_sword','chaina_pickaxe','chaina_axe','chaina_shovel','chaina_hoe']:
    (model_dir / f'{tool}.json').write_text(json.dumps({
        'parent': 'minecraft:item/handheld',
        'textures': {'layer0': f'chainacobblemon:item/{tool}'}
    }, indent=2))
for armor_item in ['chaina_helmet','chaina_chestplate','chaina_leggings','chaina_boots']:
    (model_dir / f'{armor_item}.json').write_text(json.dumps({
        'parent': 'minecraft:item/generated',
        'textures': {'layer0': f'chainacobblemon:item/{armor_item}'}
    }, indent=2))

layer1_path = vanilla_candidates(
    'assets/minecraft/textures/models/armor/netherite_layer_1.png',
    'assets/minecraft/textures/entity/equipment/humanoid/netherite.png',
)
layer2_path = vanilla_candidates(
    'assets/minecraft/textures/models/armor/netherite_layer_2.png',
    'assets/minecraft/textures/entity/equipment/humanoid_leggings/netherite.png',
)
vanilla_l1 = read_png(layer1_path)
vanilla_l2 = read_png(layer2_path)
chaina_l1 = recolor_exact_shape(vanilla_l1, gold_accents=True)
chaina_l2 = recolor_exact_shape(vanilla_l2, gold_accents=True)
chaina_l1.save(armor_dir / 'chaina_layer_1.png')
chaina_l2.save(armor_dir / 'chaina_layer_2.png')
if list(vanilla_l1.getchannel('A').getdata()) != list(chaina_l1.getchannel('A').getdata()):
    raise RuntimeError('Armor layer 1 alpha mask changed')
if list(vanilla_l2.getchannel('A').getdata()) != list(chaina_l2.getchannel('A').getdata()):
    raise RuntimeError('Armor layer 2 alpha mask changed')

TRANSPARENT=(0,0,0,0)
CREAM=(250,238,234,255)
PINK=(235,86,145,255)
PINK_LIGHT=(255,177,210,255)
PLUM=(62,42,64,255)
PLUM_DARK=(35,27,39,255)
GOLD_RGBA=(229,178,66,255)
GOLD_LIGHT_RGBA=(255,219,118,255)


def bell(d: ImageDraw.ImageDraw, cx: int, top: int, primary, dark):
    pts = [
        (cx,top),(cx+1,top),
        (cx-1,top+1),(cx,top+1),(cx+1,top+1),(cx+2,top+1),
        (cx-1,top+2),(cx,top+2),(cx+1,top+2),(cx+2,top+2),
        (cx,top+3),(cx+1,top+3),
    ]
    for p in pts:
        d.point(p, fill=primary)
    d.point((cx,top+4), fill=dark)


def make_ticket(kind: str, bells: int) -> Image.Image:
    im = Image.new('RGBA', (16,16), TRANSPARENT)
    d = ImageDraw.Draw(im)
    if kind == 'normal':
        body, edge, stripe, bellc, belld = CREAM, PINK, PINK_LIGHT, GOLD_RGBA, (154,105,28,255)
    elif kind == 'special':
        body, edge, stripe, bellc, belld = PLUM, PINK, PINK_LIGHT, GOLD_LIGHT_RGBA, GOLD_RGBA
    else:
        body, edge, stripe, bellc, belld = (245,221,165,255), PLUM_DARK, GOLD_RGBA, PINK_LIGHT, PINK
    d.rectangle((1,4,14,11), fill=body, outline=edge)
    d.point((1,7), fill=TRANSPARENT); d.point((14,7), fill=TRANSPARENT)
    d.line((3,5,12,5), fill=stripe)
    d.line((3,10,12,10), fill=stripe)
    starts = {1:[7], 2:[5,9], 3:[3,7,11]}[bells]
    for cx in starts:
        bell(d, cx, 5, bellc, belld)
    return im

make_ticket('normal',1).save(item_dir/'gacha_ticket.png')
make_ticket('special',2).save(item_dir/'chaina_special_banner_ticket.png')
make_ticket('treasure',3).save(item_dir/'treasure_gacha_ticket.png')

for filename, texture in [
    ('gacha_ticket.json','gacha_ticket'),
    ('chaina_special_banner_ticket.json','chaina_special_banner_ticket'),
    ('treasure_gacha_ticket.json','treasure_gacha_ticket'),
]:
    (model_dir/filename).write_text(json.dumps({
        'parent': 'minecraft:item/generated',
        'textures': {'layer0': f'chainacobblemon:item/{texture}'}
    }, indent=2))

proof = root / 'src/main/resources/assets/chainacobblemon/alpha8_vanilla_geometry_proof.json'
proof.write_text(json.dumps({
    'minecraft_version': '1.21.1',
    'tools_source': 'official vanilla netherite item textures',
    'armor_source_layer_1': layer1_path,
    'armor_source_layer_2': layer2_path,
    'geometry_rule': 'alpha masks and pixel positions preserved; RGB recolor only',
    'tickets': {'normal_bells':1,'chaina_bells':2,'treasure_bells':3,'model':'minecraft:item/generated'},
}, indent=2))

print('Applied alpha.8: exact vanilla netherite silhouettes/UVs recolored to Chaina + flat 1/2/3-bell tickets')
