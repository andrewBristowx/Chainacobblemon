#!/usr/bin/env python3
from pathlib import Path
import sys

root = Path(sys.argv[1]) if len(sys.argv) > 1 else Path('/tmp/chainacobblemon')
java_roots = [root/'src/main/java', root/'src/client/java']

# User-facing names only. Internal IDs/API names stay untouched for compatibility.
replacements = {
    # Kanto badges
    'Boulder Badge':'Medalla Roca', 'Cascade Badge':'Medalla Cascada',
    'Thunder Badge':'Medalla Trueno', 'Rainbow Badge':'Medalla Arcoíris',
    'Soul Badge':'Medalla Alma', 'Marsh Badge':'Medalla Pantano',
    'Volcano Badge':'Medalla Volcán', 'Earth Badge':'Medalla Tierra',
    # Johto
    'Zephyr Badge':'Medalla Céfiro', 'Hive Badge':'Medalla Colmena',
    'Plain Badge':'Medalla Planicie', 'Fog Badge':'Medalla Niebla',
    'Storm Badge':'Medalla Tormenta', 'Mineral Badge':'Medalla Mineral',
    'Glacier Badge':'Medalla Glaciar', 'Rising Badge':'Medalla Dragón',
    # Hoenn
    'Stone Badge':'Medalla Piedra', 'Knuckle Badge':'Medalla Puño',
    'Dynamo Badge':'Medalla Dinamo', 'Heat Badge':'Medalla Calor',
    'Balance Badge':'Medalla Equilibrio', 'Feather Badge':'Medalla Pluma',
    'Mind Badge':'Medalla Mente', 'Rain Badge':'Medalla Lluvia',
    # Sinnoh
    'Coal Badge':'Medalla Carbón', 'Forest Badge':'Medalla Bosque',
    'Cobble Badge':'Medalla Adoquín', 'Fen Badge':'Medalla Ciénaga',
    'Relic Badge':'Medalla Reliquia', 'Mine Badge':'Medalla Mina',
    'Icicle Badge':'Medalla Carámbano', 'Beacon Badge':'Medalla Faro',
    # Player-facing technical labels
    'Level Sync activo':'Sincronización de nivel activa',
    'Level Sync':'Sincronización de nivel',
    'Level Cap':'límite de nivel',
    'Dungeon adaptativa':'Mazmorra adaptativa',
    'Dungeon completada para ti.':'Mazmorra completada para ti.',
    'Dungeon Pokémon detectada':'Mazmorra Pokémon detectada',
    'Entrenador de dungeon derrotado':'Entrenador de mazmorra derrotado',
    'esta dungeon':'esta mazmorra',
    'una dungeon':'una mazmorra',
    'dentro de una dungeon':'dentro de una mazmorra',
    'los mods de dungeons':'los mods de mazmorras',
    'Loot de cofres: ChainaDungeonLoot':'Botín de cofres: sistema de mazmorras de Chaina',
    'Los cofres y su loot siguen gestionados por ChainaDungeonLoot.':'Los cofres y su botín siguen gestionados por el sistema de mazmorras de Chaina.',
    'reset global':'reinicio global',
}

for base in java_roots:
    if not base.exists(): continue
    for path in base.rglob('*.java'):
        text = path.read_text(encoding='utf-8', errors='ignore')
        new = text
        for old, value in replacements.items():
            new = new.replace(old, value)
        if new != text:
            path.write_text(new, encoding='utf-8')

# Guard the most visible English labels that were inherited from the mature trainer catalog.
catalog = (root/'src/main/java/com/andrewbristowx/chainacobblemon/challenge/ChallengeCatalog.java').read_text(encoding='utf-8')
for forbidden in ('Boulder Badge','Cascade Badge','Thunder Badge','Rainbow Badge','Soul Badge','Marsh Badge','Volcano Badge','Earth Badge','Zephyr Badge','Stone Badge','Coal Badge','Beacon Badge'):
    if forbidden in catalog:
        raise RuntimeError(f'Etiqueta de medalla sin traducir: {forbidden}')

print('Textos visibles principales normalizados a español')
