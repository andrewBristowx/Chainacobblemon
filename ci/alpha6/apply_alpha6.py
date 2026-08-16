from pathlib import Path
import json, sys
from PIL import Image, ImageDraw, ImageFont

root=Path(sys.argv[1])
client=root/'src/client/java/com/andrewbristowx/chainacobblemon/client'
assets=root/'src/main/resources/assets/chainacobblemon'
item=assets/'textures/item'; armor=assets/'textures/models/armor'

# UI micro-fixes agreed with the user.
p=client/'rewards/DailyRewardScreen.java'
s=p.read_text(encoding='utf-8')
s=s.replace('''        fillRef(context, 1222, 690, 250, 2, 0xFF5A4E54);\n        drawCenteredPx(context, snapshot.eligible ? "NO PIERDAS TU RACHA" : "REGRESA CUANDO EL CONTADOR TERMINE",\n                cx, panelY + ry(725), 0xFFFFC8D0);\n''','''        fillRef(context, 1222, 690, 250, 2, 0xFF5A4E54);\n        if (snapshot.eligible) {\n            drawCenteredPx(context, "NO PIERDAS TU RACHA", cx, panelY + ry(724), 0xFFFFC8D0);\n        } else {\n            drawCenteredPx(context, "REGRESA CUANDO", cx, panelY + ry(716), 0xFFFFC8D0);\n            drawCenteredPx(context, "EL CONTADOR TERMINE", cx, panelY + ry(742), 0xFFFFC8D0);\n        }\n''')
p.write_text(s, encoding='utf-8')

p=client/'gacha/GachaScreen.java'
s=p.read_text(encoding='utf-8')
s=s.replace('''drawCentered(context, "TIRAR ×1", panelX + rx(layout.oneCenterX()),\n                panelY + ry(layout.buttonTitleY()), TEXT);''','''drawCentered(context, "TIRAR ×1", panelX + rx(layout.oneCenterX()),\n                panelY + ry(layout.buttonTitleY()), 0xFFFFF4F7);''')
s=s.replace('''drawCentered(context, "TIRAR ×10", panelX + rx(layout.tenCenterX()),\n                panelY + ry(layout.buttonTitleY()), 0xFF2F2924);''','''drawCentered(context, "TIRAR ×10", panelX + rx(layout.tenCenterX()),\n                panelY + ry(layout.buttonTitleY()), 0xFFFFF4E8);''')
s=s.replace('''drawCentered(context, "10 TICKETS", panelX + rx(layout.tenCenterX()),\n                panelY + ry(layout.buttonCostY()), 0xFF4A3823);''','''drawCentered(context, "10 TICKETS", panelX + rx(layout.tenCenterX()),\n                panelY + ry(layout.buttonCostY()), 0xFFFFE0A1);''')
s=s.replace('''                242, 654, 320, 92, 813,\n                402, 814, 836, 872,''','''                248, 648, 320, 92, 813,\n                408, 808, 836, 872,''')
p.write_text(s, encoding='utf-8')

# Chaina gasha: replace the cat topper with a golden bell in the actual GeckoLib geometry.
gp=assets/'geo/chaina_gacha_machine.geo.json'; g=json.loads(gp.read_text(encoding='utf-8'))
for b in g['minecraft:geometry'][0]['bones']:
    if b.get('name')=='chaina_cat':
        b['name']='chaina_bell'
        b['cubes']=[
            {'origin':[-.35,31,-.35],'size':[.7,1.1,.7],'uv':[80,0]},
            {'origin':[-.9,30.35,-.9],'size':[1.8,.75,1.8],'uv':[64,0]},
            {'origin':[-1.55,27.65,-1.55],'size':[3.1,2.95,3.1],'uv':[64,0]},
            {'origin':[-1.1,27.2,-1.1],'size':[2.2,.55,2.2],'uv':[96,0]},
            {'origin':[-.4,26.85,-.4],'size':[.8,.55,.8],'uv':[112,16]},
            {'origin':[-.95,30.2,-.18],'size':[1.9,.18,.36],'uv':[32,0]},
            {'origin':[-.18,29.65,-.95],'size':[.36,.18,1.9],'uv':[32,0]},
        ]
        break
gp.write_text(json.dumps(g,separators=(',',':')), encoding='utf-8')

# Armor geometry: Minecraft/netherite-like chunky plates, still using the existing GeckoLib armor hooks.
def cube(origin,size,uv,inflate=.0):
    c={'origin':origin,'size':size,'uv':uv}
    if inflate: c['inflate']=inflate
    return c
bones=[
 {'name':'armorHead','pivot':[0,24,0],'cubes':[cube([-4.5,23.5,-4.5],[9,9,9],[0,0],.15),cube([-4.8,28.8,-4.8],[9.6,2.1,9.6],[32,0],.1)]},
 {'name':'armorBody','pivot':[0,24,0],'cubes':[cube([-4.4,12,-2.7],[8.8,12,5.4],[32,32],.12),cube([-5.0,19.4,-3.0],[10,3.2,6],[64,0],.12),cube([-3.6,12.2,-3.05],[7.2,4.0,.55],[96,0],.05)]},
 {'name':'armorRightArm','pivot':[-5,22,0],'cubes':[cube([-8.3,12,-2.6],[4.7,11,5.2],[64,32],.12),cube([-8.7,19.6,-3],[5.4,3.0,6],[96,32],.08)]},
 {'name':'armorLeftArm','pivot':[5,22,0],'cubes':[cube([3.6,12,-2.6],[4.7,11,5.2],[64,32],.12),cube([3.3,19.6,-3],[5.4,3.0,6],[96,32],.08)]},
 {'name':'armorRightLeg','pivot':[-1.9,12,0],'cubes':[cube([-4.2,5,-2.55],[4.7,7,5.1],[32,64],.1),cube([-4.45,9.1,-2.8],[5.2,2.2,5.6],[64,64],.08)]},
 {'name':'armorLeftLeg','pivot':[1.9,12,0],'cubes':[cube([-.5,5,-2.55],[4.7,7,5.1],[32,64],.1),cube([-.75,9.1,-2.8],[5.2,2.2,5.6],[64,64],.08)]},
 {'name':'armorRightBoot','pivot':[-1.9,12,0],'cubes':[cube([-4.4,0,-2.8],[5.1,5.2,5.6],[96,64],.12),cube([-4.65,.1,-3.55],[5.6,2.0,6.7],[112,64],.08)]},
 {'name':'armorLeftBoot','pivot':[1.9,12,0],'cubes':[cube([-.7,0,-2.8],[5.1,5.2,5.6],[96,64],.12),cube([-.95,.1,-3.55],[5.6,2.0,6.7],[112,64],.08)]},
]
armor_geo={'format_version':'1.12.0','minecraft:geometry':[{'description':{'identifier':'geometry.chainacobblemon.chaina_armor','texture_width':256,'texture_height':256,'visible_bounds_width':3.5,'visible_bounds_height':3.5,'visible_bounds_offset':[0,1.45,0]},'bones':bones}]}
(assets/'geo/armor/chaina_armor.geo.json').write_text(json.dumps(armor_geo,separators=(',',':')),encoding='utf-8')

DARK=(39,28,42,255); MID=(82,52,86,255); PINK=(244,84,132,255); LPINK=(255,181,209,255); GOLD=(234,178,73,255); DGOLD=(160,108,28,255); SILVER=(232,228,233,255); WHITE=(250,246,248,255); CREAM=(249,241,233,255)
font_path=str(assets/'font/pixelify_sans.ttf')
try:
    f96=ImageFont.truetype(font_path,96); f54=ImageFont.truetype(font_path,54); f34=ImageFont.truetype(font_path,34)
except Exception:
    f96=f54=f34=ImageFont.load_default()

def ticket(size,bg,border,title,sub,right,accent,premium=False):
    W,H=size; im=Image.new('RGBA',size,(0,0,0,0)); d=ImageDraw.Draw(im); m=10
    d.rounded_rectangle((m,m,W-m,H-m),radius=28,fill=bg,outline=border,width=8)
    hr=26; cx=W//2; d.ellipse((cx-hr,H//2-hr,cx+hr,H//2+hr),fill=(0,0,0,0))
    for x in (42,W-42):
        for y in (42,H-42): d.line((x-12,y,x+12,y),fill=border,width=4); d.line((x,y-12,x,y+12),fill=border,width=4)
    bx,by=150,H//2; d.ellipse((bx-60,by-60,bx+60,by+60),fill=accent,outline=border,width=8)
    d.rectangle((bx-12,by-32,bx+12,by-8),fill=GOLD); d.pieslice((bx-48,by-18,bx+48,by+56),180,360,fill=GOLD,outline=DGOLD)
    d.rectangle((bx-34,by+16,bx+34,by+28),fill=GOLD); d.ellipse((bx-10,by+26,bx+10,by+46),fill=DGOLD); d.rectangle((bx-4,by+28,bx+4,by+58),fill=DGOLD)
    d.text((260,120),title,font=f96,fill=border); d.text((260,230),sub,font=f54,fill=accent if not premium else GOLD); d.text((260,H-120),'CHAINACOBBLEMON',font=f34,fill=border)
    d.rounded_rectangle((W-620,70,W-60,H-70),radius=18,fill=(255,255,255,18 if premium else 30),outline=border,width=4)
    y=110
    for ln in right.split('\n'): d.text((W-590,y),ln,font=f34 if len(ln)>18 else f54,fill=WHITE if premium else MID); y+=54
    return im

ticket((2048,512),CREAM,DGOLD,'Ticket Gasha','Estándar','1 ticket\nUsa en la\nmáquina gasha\nnormal',PINK).save(item/'gacha_ticket.png')
ticket((2048,320),(45,25,52,255),PINK,'Ticket Chaina','Especial','Banner especial\nEvento Chaina\nPity legendario',LPINK,True).save(item/'chaina_special_banner_ticket.png')
ticket((2048,320),(244,232,240,255),DGOLD,'Ticket','Tesoros de Chaina','Equipamiento\nexclusivo\nTesoros y piezas\npremium',GOLD).save(item/'treasure_gacha_ticket.png')

def save16(name,draw):
    im=Image.new('RGBA',(16,16),(0,0,0,0)); d=ImageDraw.Draw(im); draw(d); im.save(item/name)
def pts(d,p,c):
    for q in p:d.point(q,fill=c)
def ln(d,a,b,c): d.line((a,b),fill=c,width=1)

save16('chaina_helmet.png',lambda d:(d.rectangle((3,3,12,5),fill=DARK),d.rectangle((2,6,13,9),fill=MID),d.rectangle((3,10,5,12),fill=MID),d.rectangle((10,10,12,12),fill=MID),d.rectangle((4,4,11,4),fill=LPINK),d.rectangle((7,2,8,2),fill=GOLD),d.rectangle((6,7,9,7),fill=PINK)))
save16('chaina_chestplate.png',lambda d:(d.rectangle((3,2,12,4),fill=DARK),d.rectangle((2,5,13,8),fill=MID),d.rectangle((4,9,11,13),fill=MID),d.rectangle((2,5,4,10),fill=MID),d.rectangle((11,5,13,10),fill=MID),d.rectangle((5,4,10,4),fill=LPINK),d.rectangle((7,5,8,11),fill=PINK),d.rectangle((6,8,9,9),fill=GOLD)))
save16('chaina_leggings.png',lambda d:(d.rectangle((4,2,11,4),fill=DARK),d.rectangle((3,5,12,7),fill=MID),d.rectangle((4,8,7,13),fill=MID),d.rectangle((8,8,11,13),fill=MID),d.rectangle((6,4,9,4),fill=LPINK),d.rectangle((7,5,8,12),fill=PINK)))
save16('chaina_boots.png',lambda d:(d.rectangle((3,5,6,11),fill=MID),d.rectangle((9,5,12,11),fill=MID),d.rectangle((2,11,7,13),fill=DARK),d.rectangle((8,11,13,13),fill=DARK),d.rectangle((4,6,5,9),fill=LPINK),d.rectangle((10,6,11,9),fill=LPINK),d.rectangle((4,12,5,12),fill=GOLD),d.rectangle((10,12,11,12),fill=GOLD)))
save16('chaina_sword.png',lambda d:(ln(d,(3,13),(12,4),DARK),ln(d,(4,13),(13,4),MID),pts(d,[(8,8),(9,7),(10,6),(11,5),(12,4),(7,9),(6,10)],LPINK),pts(d,[(7,10),(8,9),(9,8)],SILVER),pts(d,[(5,11),(4,12)],GOLD),pts(d,[(5,9),(6,8),(7,7)],PINK)))
save16('chaina_pickaxe.png',lambda d:(ln(d,(4,13),(10,7),DARK),ln(d,(5,13),(11,7),MID),ln(d,(6,5),(11,5),LPINK),ln(d,(5,6),(12,6),PINK),ln(d,(10,4),(12,2),LPINK),pts(d,[(8,9),(7,10),(6,11)],GOLD)))
save16('chaina_axe.png',lambda d:(ln(d,(4,13),(10,7),DARK),ln(d,(5,13),(11,7),MID),d.polygon([(7,6),(11,4),(12,7),(9,10),(6,9)],fill=LPINK,outline=DARK),pts(d,[(8,7),(9,6),(10,6)],SILVER),pts(d,[(7,10),(6,11)],GOLD)))
save16('chaina_shovel.png',lambda d:(ln(d,(4,13),(10,7),DARK),ln(d,(5,13),(11,7),MID),d.polygon([(9,5),(12,7),(10,10),(7,8)],fill=LPINK,outline=DARK),pts(d,[(9,6),(10,7)],SILVER),pts(d,[(7,10)],GOLD)))
save16('chaina_hoe.png',lambda d:(ln(d,(4,13),(10,7),DARK),ln(d,(5,13),(11,7),MID),ln(d,(8,5),(12,5),LPINK),ln(d,(8,6),(11,6),PINK),ln(d,(11,7),(12,8),LPINK),pts(d,[(7,10),(6,11)],GOLD)))

# Generic 3D tool palette atlas.
im=Image.new('RGBA',(16,16),DARK); d=ImageDraw.Draw(im); d.rectangle((1,1,14,14),fill=MID); d.rectangle((3,3,12,5),fill=LPINK); d.rectangle((3,7,12,9),fill=PINK); d.rectangle((3,11,12,12),fill=GOLD); im.save(item/'chaina_tools_3d.png')

# Machine inventory icons: keep the machine silhouette, replace the mascot head by a bell.
for nm,tint in [('chaina_gacha_machine.png',PINK),('treasure_gacha_machine.png',GOLD)]:
    im=Image.open(item/nm).convert('RGBA'); d=ImageDraw.Draw(im); d.rectangle((36,0,92,28),fill=(0,0,0,0)); d.rectangle((48,20,80,27),fill=tint)
    d.rectangle((61,2,67,5),fill=DGOLD); d.pieslice((52,5,76,22),180,360,fill=GOLD,outline=DARK); d.rectangle((56,15,72,19),fill=GOLD); d.ellipse((60,17,68,24),fill=DGOLD); d.rectangle((63,18,65,29),fill=DGOLD); im.save(item/nm)

# Build a clean Chaina netherite-like armor texture atlas for the simplified geometry.
im=Image.new('RGBA',(256,256),(0,0,0,0)); d=ImageDraw.Draw(im)
for y in range(0,128,32):
    for x in range(0,160,32):
        base=DARK if (x//32+y//32)%2==0 else MID
        d.rectangle((x,y,x+31,y+31),fill=base)
        d.rectangle((x+3,y+3,x+28,y+7),fill=LPINK)
        d.rectangle((x+7,y+11,x+24,y+15),fill=PINK)
        d.rectangle((x+12,y+19,x+19,y+24),fill=GOLD)
im.save(armor/'chaina_gecko_armor.png')
# Layer textures retained for fallback/vanilla contexts, dark netherite-like with Chaina accents.
for nm in ('chaina_layer_1.png','chaina_layer_2.png'):
    im=Image.new('RGBA',(64,32),(0,0,0,0)); d=ImageDraw.Draw(im)
    d.rectangle((0,0,63,31),fill=DARK); d.rectangle((2,2,61,5),fill=MID); d.rectangle((8,8,55,11),fill=PINK); d.rectangle((14,14,49,17),fill=LPINK); d.rectangle((28,20,35,24),fill=GOLD); im.save(armor/nm)

# Version.
props=root/'gradle.properties'; t=props.read_text(encoding='utf-8').replace('mod_version=0.3.0-alpha.5+1.21.1','mod_version=0.3.0-alpha.6+1.21.1'); props.write_text(t,encoding='utf-8')
print('alpha6 visual/UI patch applied')
