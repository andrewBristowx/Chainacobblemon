#!/usr/bin/env python3
from pathlib import Path
import re
import sys

root = Path(sys.argv[1]) if len(sys.argv) > 1 else Path('/tmp/chainacobblemon')


def read(rel):
    return (root / rel).read_text(encoding='utf-8')

def write(rel, text):
    (root / rel).write_text(text, encoding='utf-8')

# Remove the old project-specific professor texture diagnostics while keeping the generic Chaina guide NPC/story engine.
rel = 'src/main/java/com/andrewbristowx/chainacobblemon/command/ChainacobblemonCommands.java'
t = read(rel)
t = t.replace('import com.andrewbristowx.chainacobblemon.admin.ProfessorNetworking;\n', '')
start = t.find('                        .then(literal("professor")')
end = t.find('                        .then(literal("resetplayer")', start)
if start >= 0 and end > start:
    t = t[:start] + t[end:]
write(rel, t)

# Pre-generation audit must validate systems/regions, not copied custom Pokémon or an Emi-specific professor texture hash.
rel = 'src/main/java/com/andrewbristowx/chainacobblemon/admin/PregenCheckService.java'
t = read(rel)
start = t.find('        if (Chainacobblemon.pokemonCatalog().get("eevee_gatito")')
end = t.find('        RegionalStructureAuditService.AuditResult structures', start)
if start >= 0 and end > start:
    t = t[:start] + t[end:]
t = re.sub(r'^\s*notes\.add\("Profesora Chaina configurada para textura nueva:.*?;\n', '', t, flags=re.M)
t = t.replace('        notes.add("ChainaDungeonLoot y los mods de dungeons se validarán en el pregencheck final después de su integración.");\n',
              '        notes.add("Los mods de dungeons y la campaña de Chaina se validan por separado antes del mapa final.");\n')
write(rel, t)

# Gasha catalog remains generic Cobblemon. Chaina custom species will only be added after visual approval.
rel = 'src/main/java/com/andrewbristowx/chainacobblemon/gacha/catalog/PokemonCatalogService.java'
t = read(rel)
t = re.sub(r'\n\s*/\*\*\n\s*\* Built-in gacha rarity for accepted Chaina variants\..*?\n\s*private static final Map<String, GachaTier> CHAINA_VARIANT_TIERS = Map\.ofEntries\(.*?\n\s*\);\n',
           '\n    // No bundled custom Chaina species yet; rarity comes from Cobblemon and catalog_overrides.json.\n', t, flags=re.S)
t = t.replace('        tier = CHAINA_VARIANT_TIERS.getOrDefault(id, tier);\n', '')
write(rel, t)

# Remove the stale custom-species example comment from the seasonal machine renderer.
rel = 'src/client/java/com/andrewbristowx/chainacobblemon/client/render/SeasonalPokemonWorldRenderer.java'
t = read(rel).replace('chainacobblemon:eevee_gatito', 'cobblemon:absol')
write(rel, t)

# Dialogue portraits use the same automatic RCT/Cobbleverse + visual-folder resolver for every NPC, including the Chaina guide.
rel = 'src/client/java/com/andrewbristowx/chainacobblemon/client/npc/NpcDialogueScreen.java'
t = read(rel)
t = t.replace('import com.andrewbristowx.chainacobblemon.admin.ProfessorNetworking;\n', '')
old = '''        boolean professor = ChallengeCatalog.PROFESSOR_NPC_ID.equalsIgnoreCase(state.id());
        Identifier fallback = Identifier.of(Chainacobblemon.MOD_ID,
                professor ? ProfessorNetworking.TEXTURE_PATH : "textures/entity/shop_npc.png");
        if (!professor) {
            fallback = RctTrainerTextureResolver.resolve(ChallengeCatalog.byNpcId(state.id()), fallback);
        }
        String assetKey = "npc:" + state.id();
        Identifier texture = professor ? fallback
                : (ClientVisualAssetCache.has(assetKey) ? ClientVisualAssetCache.texture(assetKey, fallback) : fallback);
'''
new = '''        Identifier fallback = Identifier.of(Chainacobblemon.MOD_ID, "textures/entity/shop_npc.png");
        fallback = RctTrainerTextureResolver.resolve(ChallengeCatalog.byNpcId(state.id()), fallback);
        String assetKey = "npc:" + state.id();
        Identifier texture = ClientVisualAssetCache.has(assetKey) ? ClientVisualAssetCache.texture(assetKey, fallback) : fallback;
'''
if old not in t:
    raise RuntimeError('No se encontró el bloque de retrato especial del profesor')
t = t.replace(old, new)
# Light Chaina palette: off-white/pale pink panels, coral/gold accents, dark text only for legibility.
t = t.replace('0xEA101927', '0xFFF8F3F4').replace('0xFFFF5AA5', '0xFFF9556D').replace('0xFF49D5ED', '0xFFF6AD4B')
t = t.replace('0xB81A293B', '0xFFFDE5E8').replace('0xFFFFD8ED', '0xFF353434').replace('0xFF9FDCEC', '0xFFE56A78')
t = t.replace('0xFF493849', '0xFFF1D8DC').replace('0xFF334B64', '0xFFFFC8D0').replace('0xFFF4F8FF', '0xFF353434')
write(rel, t)

# Hard checks: no copied custom-variant roster remains in Java runtime logic.
for needle in ('eevee_gatito','snorlax_chaina','espurr_chaina','sprigatito_chaina','zorua_chaina','unown_67'):
    hits = []
    for p in list((root/'src/main/java').rglob('*.java')) + list((root/'src/client/java').rglob('*.java')):
        if needle in p.read_text(encoding='utf-8', errors='ignore'):
            hits.append(str(p.relative_to(root)))
    if hits:
        raise RuntimeError(f'Resto de Pokémon custom {needle}: {hits}')

print('Post-transform Chaina cleanup OK')
