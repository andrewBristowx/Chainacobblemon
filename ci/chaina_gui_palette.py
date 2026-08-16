#!/usr/bin/env python3
from pathlib import Path
import sys

root = Path(sys.argv[1]) if len(sys.argv) > 1 else Path('/tmp/chainacobblemon')
client = root / 'src/client/java/com/andrewbristowx/chainacobblemon/client'

# Chaina UI standard: the world may dim behind a screen, but the actual panels stay
# off-white / pale sakura. Coral and gold are the structural accents, not purple.
COMMON = {
    '0xF5100915': '0xFFF5F5F7',  # old dark panel -> off-white
    '0xFFFF9DDE': '0xFFF9556D', # old Emi pink -> Chaina coral
    '0xFF75418E': '0xFFF6AD4B', # old purple -> Chaina gold
    '0xFF9D5AAF': '0xFFFB9AA6', # secondary purple -> sakura
    '0xFFFFD8F1': '0xFF353434', # light text on dark -> charcoal on light
    '0xFFF0B9DE': '0xFFE56A78', # secondary label -> muted coral
    '0xFFCDB8CA': '0xFF86827F', # muted text -> taupe-gray
}

QUEST_SHOP = {
    # Panels/cards
    '0xA52E1A38': '0xFFFDE5E8',
    '0xA52B1934': '0xFFFDE5E8',
    '0xA53B2144': '0xFFFDE5E8',
    '0x8E28152F': '0xFFF8F3F4',
    '0xD73C2346': '0xFFFDE5E8',
    '0xD04B2B58': '0xFFFFD3D9',
    '0xA6252028': '0xFFE8E0E2',
    '0xA30A0710': '0xB8353434',  # screen dim only
    '0xA80A0710': '0xB8353434',
    '0xAA170D1B': '0xFFF1D8DC',
    '0xFF170D1B': '0xFFF1D8DC',
    '0xFF160D1A': '0xFFF1D8DC',
    '0x706A3C75': '0x70F9556D',
    '0xFF6A3C75': '0xFFFDC7CB',
    '0xFF613164': '0xFFFFC8D0',
    # Strong accents
    '0xFFE37ABF': '0xFFF9556D',
    '0xFFE27BBD': '0xFFF9556D',
    '0xFFF5A8D5': '0xFFE56A78',
    # Primary readable text
    '0xFFFFCDE9': '0xFF353434',
    '0xFFFFD4ED': '0xFF353434',
    '0xFFFFD6ED': '0xFF353434',
    '0xFFFFE6F6': '0xFF353434',
    '0xFFFFE8F7': '0xFF353434',
    '0xFFFFFFFF': '0xFF353434',
    '0xFFE2D4E3': '0xFF353434',
    '0xFFE8CAE1': '0xFF353434',
    # Secondary text
    '0xFFD2B4CE': '0xFF86827F',
    '0xFFAD91AF': '0xFF86827F',
    '0xFFB99DB7': '0xFF86827F',
    '0xFFDCC5DE': '0xFF86827F',
    '0xFFBDA2BC': '0xFF86827F',
    '0xFFE1BCD8': '0xFF86827F',
    '0xFFCCB4CC': '0xFF86827F',
    '0xFFBD8EC8': '0xFFE56A78',
    '0xFFEAB7DD': '0xFFE56A78',
    # Keep reward/status colors, but make gold use the actual Chaina gold.
    '0xFFFFD36A': '0xFFF6AD4B',
}

SIMPLE = {
    'admin/AdminScreen.java': {
        **COMMON,
        '0xFFFFFFFF': '0xFF353434',
    },
    'npc/NpcEditorScreen.java': {
        **COMMON,
        '0xFFA98CA7': '0xFF86827F',
    },
    'progress/QuestJournalScreen.java': {**COMMON, **QUEST_SHOP},
    'shop/ShopScreen.java': {**COMMON, **QUEST_SHOP},
}

for rel, mapping in SIMPLE.items():
    path = client / rel
    text = path.read_text(encoding='utf-8')
    for old, new in mapping.items():
        text = text.replace(old, new)
    path.write_text(text, encoding='utf-8')

# Pass/login use generated light textures. Retune their labels for the light background,
# while preserving green/red state feedback and the mature reveal/claim logic.
for rel in ('rewards/BattlePassScreen.java', 'rewards/DailyRewardScreen.java'):
    path = client / rel
    text = path.read_text(encoding='utf-8')
    mapping = {
        '0xFFFF9DDE': '0xFFF9556D',
        '0xFFFF6EB5': '0xFFF9556D',
        '0xFFFFD36A': '0xFFF6AD4B',
        '0xFFFFE4F4': '0xFF353434',
        '0xFFFFDBEE': '0xFF353434',
        '0xFFFFE8F7': '0xFF353434',
        '0xFFFFF1FA': '0xFF353434',
        '0xFFFFDDEC': '0xFF353434',
        '0xFFE8D8E7': '0xFF86827F',
        '0xFFCEBFD0': '0xFF86827F',
        '0xFFB6A8B5': '0xFF86827F',
        '0xFF9F929E': '0xFF86827F',
    }
    for old, new in mapping.items():
        text = text.replace(old, new)
    path.write_text(text, encoding='utf-8')

# Guard the screens the user explicitly called out. If these legacy panel colors return,
# CI must fail rather than silently shipping another dark/purple Chaina GUI.
legacy = ('0xF5100915', '0xFF75418E', '0xFF613164', '0xFF9D5AAF', '0xFF170D1B')
for rel in ('admin/AdminScreen.java','npc/NpcEditorScreen.java','progress/QuestJournalScreen.java','shop/ShopScreen.java'):
    text = (client / rel).read_text(encoding='utf-8')
    remaining = [c for c in legacy if c in text]
    if remaining:
        raise RuntimeError(f'Paleta oscura heredada en {rel}: {remaining}')
    if '0xFFF9556D' not in text or '0xFFF6AD4B' not in text:
        raise RuntimeError(f'{rel} no usa los acentos coral/dorado de Chaina')

print('Paleta Chaina clara aplicada a Admin/NPC/Misiones/Jobs/Tienda/Pase/Login')
