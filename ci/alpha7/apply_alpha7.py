from pathlib import Path
from PIL import Image, ImageDraw
import re, sys

root = Path(sys.argv[1])

# Version
p = root / 'gradle.properties'
t = p.read_text()
t = re.sub(r'mod_version=0\.3\.0-alpha\.6\+1\.21\.1', 'mod_version=0.3.0-alpha.7+1.21.1', t)
p.write_text(t)

# Vanilla armor rendering: keep stats/material, remove custom Gecko silhouette.
p = root / 'src/main/java/com/andrewbristowx/chainacobblemon/registry/ModRegistries.java'
t = p.read_text()
t = t.replace('import com.andrewbristowx.chainacobblemon.armor.ChainaGeoArmorItem;\n', '')
t = t.replace('new ChainaGeoArmorItem(', 'new ArmorItem(')
p.write_text(t)

p = root / 'src/client/java/com/andrewbristowx/chainacobblemon/client/ChainacobblemonClient.java'
t = p.read_text()
for s in [
    'import com.andrewbristowx.chainacobblemon.client.render.ChainaArmorGeoRenderer;\n',
    'import com.andrewbristowx.chainacobblemon.armor.ChainaGeoArmorPiece;\n',
    'import net.minecraft.client.render.entity.model.BipedEntityModel;\n',
    'import net.minecraft.entity.EquipmentSlot;\n',
    'import net.minecraft.entity.LivingEntity;\n',
    'import net.minecraft.item.ItemStack;\n',
    'import software.bernie.geckolib.animatable.client.GeoRenderProvider;\n',
    'import software.bernie.geckolib.renderer.GeoArmorRenderer;\n',
    'import org.jetbrains.annotations.Nullable;\n',
]:
    t = t.replace(s, '')
for s in [
    '        registerChainaArmorRenderer(ModRegistries.CHAINA_HELMET);\n',
    '        registerChainaArmorRenderer(ModRegistries.CHAINA_CHESTPLATE);\n',
    '        registerChainaArmorRenderer(ModRegistries.CHAINA_LEGGINGS);\n',
    '        registerChainaArmorRenderer(ModRegistries.CHAINA_BOOTS);\n',
]:
    t = t.replace(s, '')
t = re.sub(r'\n    private static void registerChainaArmorRenderer\(net\.minecraft\.item\.Item item\) \{.*?\n    \}\n', '\n', t, flags=re.S)
p.write_text(t)

tex = root / 'src/main/resources/assets/chainacobblemon/textures'
item = tex / 'item'
armor = tex / 'models/armor'
DARK=(48,44,58,255); EDGE=(28,26,34,255); MID=(90,86,104,255)
PINK=(236,110,170,255); LPINK=(255,188,220,255); GOLD=(230,180,70,255)
CREAM=(248,238,232,255); WHITE=(255,251,253,255); BG=(0,0,0,0)

def new16(): return Image.new('RGBA',(16,16),(0,0,0,0))
def pt(d, coords, c):
    for x,y in coords: d.point((x,y), fill=c)

# Armor item icons — vanilla-shaped silhouettes, only recolored/decorated.
im=new16(); d=ImageDraw.Draw(im)
pt(d,[(x,4) for x in range(3,13)]+[(x,5) for x in range(2,14)]+[(2,y) for y in range(6,10)]+[(13,y) for y in range(6,10)],DARK)
pt(d,[(x,9) for x in range(3,13)]+[(x,6) for x in range(4,12)],MID)
pt(d,[(x,7) for x in range(5,11)],LPINK); pt(d,[(x,8) for x in range(6,10)],PINK); pt(d,[(7,3),(8,3)],GOLD)
im.save(item/'chaina_helmet.png')

im=new16(); d=ImageDraw.Draw(im)
pt(d,[(x,3) for x in range(4,12)]+[(x,4) for x in range(3,13)]+[(3,y) for y in range(5,10)]+[(12,y) for y in range(5,10)],DARK)
pt(d,[(x,5) for x in range(5,11)]+[(x,6) for x in range(4,12)]+[(x,y) for x in range(6,10) for y in range(9,14)],MID)
pt(d,[(x,7) for x in range(5,11)],LPINK); pt(d,[(x,8) for x in range(6,10)],PINK)
pt(d,[(x,y) for x in range(4,6) for y in range(8,13)]+[(x,y) for x in range(10,12) for y in range(8,13)],DARK); pt(d,[(7,10),(8,10)],GOLD)
im.save(item/'chaina_chestplate.png')

im=new16(); d=ImageDraw.Draw(im)
pt(d,[(x,3) for x in range(4,12)]+[(x,4) for x in range(3,13)],DARK); pt(d,[(x,5) for x in range(4,12)],MID)
pt(d,[(x,6) for x in range(5,11)],LPINK); pt(d,[(x,7) for x in range(6,10)],PINK)
pt(d,[(x,y) for x in range(4,7) for y in range(8,14)]+[(x,y) for x in range(9,12) for y in range(8,14)],MID)
pt(d,[(4,y) for y in range(8,14)]+[(11,y) for y in range(8,14)],DARK); pt(d,[(7,8),(8,8)],GOLD)
im.save(item/'chaina_leggings.png')

im=new16(); d=ImageDraw.Draw(im)
pt(d,[(x,y) for x in range(3,6) for y in range(5,11)]+[(x,y) for x in range(10,13) for y in range(5,11)],MID)
pt(d,[(3,y) for y in range(5,11)]+[(12,y) for y in range(5,11)]+[(x,y) for x in range(2,7) for y in range(11,14)]+[(x,y) for x in range(9,14) for y in range(11,14)],DARK)
pt(d,[(4,7),(11,7)],LPINK); pt(d,[(4,12),(11,12)],GOLD)
im.save(item/'chaina_boots.png')

# Tools — retain the normal vanilla handheld item geometry; only pixel art/palette changes.
im=new16(); d=ImageDraw.Draw(im)
pt(d,[(11,1),(10,2),(9,3),(8,4),(7,5),(6,6),(5,7)],LPINK); pt(d,[(10,1),(9,2),(8,3),(7,4),(6,5),(5,6),(4,7)],PINK)
pt(d,[(6,8),(7,8),(5,9),(8,7)],GOLD); pt(d,[(4,10),(5,10),(6,11),(7,12),(8,13)],DARK); pt(d,[(4,11),(5,11),(6,12),(7,13),(8,14)],MID)
im.save(item/'chaina_sword.png')

im=new16(); d=ImageDraw.Draw(im)
pt(d,[(x,2) for x in range(4,12)]+[(x,3) for x in range(3,11)]+[(11,1),(12,1),(10,1)],LPINK); pt(d,[(11,2),(12,2)],PINK)
pt(d,[(8,4),(7,5),(6,6),(5,7),(4,8),(4,9),(5,10),(6,11),(7,12),(8,13)],DARK); pt(d,[(9,4),(8,5),(7,6),(6,7),(5,8),(5,9),(6,10),(7,11),(8,12),(9,13)],MID); pt(d,[(5,10),(6,11)],GOLD)
im.save(item/'chaina_pickaxe.png')

im=new16(); d=ImageDraw.Draw(im)
pt(d,[(8,2),(9,2),(10,2),(11,2),(7,3),(8,3),(9,3),(10,3),(11,3),(12,3),(7,4),(8,4),(9,4),(10,4),(11,4),(6,5),(7,5),(8,5),(9,5),(10,5),(6,6),(7,6),(8,6)],LPINK); pt(d,[(11,1),(12,2),(12,4),(10,5),(9,6)],PINK)
pt(d,[(8,5),(7,6),(6,7),(5,8),(4,9),(4,10),(5,11),(6,12),(7,13)],DARK); pt(d,[(9,5),(8,6),(7,7),(6,8),(5,9),(5,10),(6,11),(7,12),(8,13)],MID); pt(d,[(5,11),(6,12)],GOLD)
im.save(item/'chaina_axe.png')

im=new16(); d=ImageDraw.Draw(im)
pt(d,[(9,1),(10,2),(11,3),(11,4),(10,5),(9,6),(8,6),(7,5),(7,4),(8,3)],LPINK); pt(d,[(10,1),(11,2),(12,3),(12,4),(11,5),(10,6)],PINK)
pt(d,[(8,7),(7,8),(6,9),(5,10),(4,11),(5,12),(6,13)],DARK); pt(d,[(9,7),(8,8),(7,9),(6,10),(5,11),(6,12),(7,13)],MID); pt(d,[(5,12)],GOLD)
im.save(item/'chaina_shovel.png')

im=new16(); d=ImageDraw.Draw(im)
pt(d,[(9,2),(10,2),(11,2),(12,2),(8,3),(9,3),(10,3),(11,3),(8,4),(9,4),(10,4)],LPINK); pt(d,[(11,1),(12,1),(12,3),(11,4)],PINK)
pt(d,[(8,5),(7,6),(6,7),(5,8),(4,9),(4,10),(5,11),(6,12),(7,13)],DARK); pt(d,[(9,5),(8,6),(7,7),(6,8),(5,9),(5,10),(6,11),(7,12),(8,13)],MID); pt(d,[(5,11)],GOLD)
im.save(item/'chaina_hoe.png')

# Tickets: one, two and three bells respectively.
def bell(d,x,y,color=GOLD):
    pt(d,[(x,y),(x+1,y),(x+2,y),(x-1,y+1),(x,y+1),(x+1,y+1),(x+2,y+1),(x+3,y+1),(x,y+2),(x+1,y+2),(x+2,y+2)],color)
    pt(d,[(x+1,y+3)],EDGE)
def ticket(bg, accent, border, bells):
    im=new16(); d=ImageDraw.Draw(im)
    d.rounded_rectangle((1,3,14,12),radius=2,fill=bg,outline=border,width=1)
    d.line((3,5,12,5),fill=accent); d.line((3,10,12,10),fill=accent); d.point((1,7),fill=BG); d.point((14,7),fill=BG)
    starts={1:[6],2:[4,8],3:[3,6,9]}[bells]
    for x in starts: bell(d,x,6)
    return im

ticket(CREAM,LPINK,PINK,1).save(item/'gacha_ticket.png')
ticket((68,49,74,255),LPINK,GOLD,2).save(item/'chaina_special_banner_ticket.png')
ticket((255,236,198,255),GOLD,DARK,3).save(item/'treasure_gacha_ticket.png')

# Armor layers: recolor the existing vanilla-layout layers. Geometry is vanilla because ArmorItem renderer is used.
for name in ['chaina_layer_1.png','chaina_layer_2.png']:
    im=Image.open(armor/name).convert('RGBA'); px=im.load()
    for y in range(im.height):
        for x in range(im.width):
            r,g,b,a=px[x,y]
            if a==0: continue
            br=(r+g+b)/3
            if br<45: c=EDGE
            elif br<85: c=DARK
            elif br<135: c=MID
            elif br<185: c=PINK if r>g+20 else LPINK
            else: c=GOLD if (r>220 and g>170 and b<170) else LPINK
            px[x,y]=(c[0],c[1],c[2],a)
    im.save(armor/name)

print('Applied alpha.7 vanilla-geometry Chaina armor/tools and 1/2/3-bell tickets')
