from pathlib import Path
import sys

root = Path(sys.argv[1])


def edit(rel, replacements):
    p = root / rel
    s = p.read_text(encoding="utf-8")
    for old, new in replacements:
        if old not in s:
            raise SystemExit(f"pattern not found in {rel}: {old[:120]!r}")
        s = s.replace(old, new)
    p.write_text(s, encoding="utf-8")
    print("patched", rel)


# Login diario: titulo dentro de la cabecera y recompensas inferiores con mas aire.
edit("src/client/java/com/andrewbristowx/chainacobblemon/client/rewards/DailyRewardScreen.java", [
    ('drawCentered(context, "RECOMPENSA DIARIA", 748, 108, 0xFFF5F5F7);',
     'drawCentered(context, "RECOMPENSA DIARIA", 748, 58, 0xFFF5F5F7);'),
    ('drawCentered(context, snapshot.eligible ? "Tu cápsula está lista" : "Vuelve mañana", 748, 146,',
     'drawCentered(context, snapshot.eligible ? "Tu cápsula está lista" : "Vuelve mañana", 748, 104,'),
    ('int cy = panelY + ry(862);', 'int cy = panelY + ry(820);'),
    ('drawRewardIcon(context, reward, cx, cy, rx(42));', 'drawRewardIcon(context, reward, cx, cy, rx(48));'),
    ('drawFittedCenteredPx(context, shortLabel(reward), cx, panelY + ry(936), rx(132), 0xFFDEDEE9);',
     'drawFittedCenteredPx(context, shortLabel(reward), cx, panelY + ry(895), rx(136), 0xFFF5F5F7);'),
])

# Pase: cabecera y barra de XP separadas del borde; textos de tarjetas subidos.
edit("src/client/java/com/andrewbristowx/chainacobblemon/client/rewards/BattlePassScreen.java", [
    ('drawCentered(context, "PASE INFINITO DE CHAINA", 760, 104, 0xFFF5F5F7);',
     'drawCentered(context, "PASE INFINITO DE CHAINA", 760, 54, 0xFFF5F5F7);'),
    ('drawCentered(context, "NIVEL " + snapshot.level, 760, 143, 0xFFF6AD4B);',
     'drawCentered(context, "NIVEL " + snapshot.level, 760, 92, 0xFFF6AD4B);'),
    ('drawCentered(context, snapshot.playerName, 134, 120, 0xFFFFFFFF);',
     'drawCentered(context, snapshot.playerName, 150, 54, 0xFFFFFFFF);'),
    ('drawCentered(context, "Nv. " + snapshot.level, 134, 156, 0xFFF6AD4B);',
     'drawCentered(context, "Nv. " + snapshot.level, 150, 86, 0xFFF6AD4B);'),
    ('int top = panelY + ry(178);', 'int top = panelY + ry(148);'),
    ('drawCentered(context, (snapshot.experience - snapshot.levelStartXp) + " / " + span + " XP", 760, 176, 0xFFFFFFFF);',
     'drawCentered(context, (snapshot.experience - snapshot.levelStartXp) + " / " + span + " XP", 760, 146, 0xFFFFFFFF);'),
    ('drawCentered(context, "Todos los niveles dan premio · Cada 4 niveles: tirada de Chaina · Incluye Tickets de Tesoros", 760, 222, 0xFFF6AD4B);',
     'drawCentered(context, "Todos los niveles dan premio · Cada 4 niveles: tirada de Chaina · Incluye Tickets de Tesoros", 760, 218, 0xFFF6AD4B);'),
    ('int cardH = ry(248);', 'int cardH = ry(232);'),
    ('panelY + ry(premium ? 785 : 438)', 'panelY + ry(premium ? 772 : 425)'),
    ('panelY + ry(premium ? 810 : 463)', 'panelY + ry(premium ? 798 : 451)'),
    ('panelY + ry(premium ? 842 : 495)', 'panelY + ry(premium ? 824 : 477)'),
    ('mouseY >= y + ry(248)', 'mouseY >= y + ry(232)'),
])

# Gasha: titulo en la cabecera; banner/tiradas centrados en el escenario; pity en columna derecha.
gpath = "src/client/java/com/andrewbristowx/chainacobblemon/client/gacha/GachaScreen.java"
p = root / gpath
s = p.read_text(encoding="utf-8")
old = '''        int titleX = panelX + rx(layout.titleX());\n        drawCentered(context, view.treasure() ? "GASHA DE TESOROS DE CHAINA" : (view.chaina() ? "GACHA ESPECIAL DE CHAINA" : "GACHA COBBLEMON"),\n                titleX, panelY + ry(layout.titleY()), 0xFFF5F5F7);\n        drawCentered(context, trim(view.bannerName(), view.chaina() || view.treasure() ? 30 : 38), titleX,\n                panelY + ry(layout.bannerY()), view.chaina() || view.treasure() ? 0xFFFFC4EE : 0xFF8DEBFF);\n        drawCentered(context, "Tiradas: " + view.totalPulls(), titleX,\n                panelY + ry(layout.totalY()), 0xFFD9E9F7);\n'''
new = '''        int headerX = panelX + rx(768);\n        int stageX = panelX + rx(layout.stageX());\n        drawCentered(context, view.treasure() ? "GASHA DE TESOROS DE CHAINA" : (view.chaina() ? "GASHA ESPECIAL DE CHAINA" : "GASHA ESTÁNDAR DE CHAINA"),\n                headerX, panelY + ry(layout.titleY()), 0xFFF5F5F7);\n        drawCentered(context, trim(view.bannerName(), view.chaina() || view.treasure() ? 30 : 38), stageX,\n                panelY + ry(layout.bannerY()), view.chaina() || view.treasure() ? 0xFFFFC4EE : 0xFF8DEBFF);\n        drawCentered(context, "Tiradas: " + view.totalPulls(), stageX,\n                panelY + ry(layout.totalY()), 0xFFD9E9F7);\n'''
if old not in s:
    raise SystemExit("gacha header pattern missing")
s = s.replace(old, new)
start = s.index("    private Layout layout() {")
end = s.index("    private boolean drawItemReward", start)
layout = '''    private Layout layout() {\n        // Composicion comun: cabecera centrada, escenario principal y columna derecha para tickets/pity.\n        if (view.treasure()) {\n            return new Layout(768, 42, 108, 137,\n                    1322, 185, 226, 445, 486, 708, 750,\n                    610, 690, 520, 610, 642, 674,\n                    300, 160, 214, 244,\n                    178, 612, 366, 122, 846, 361, 795, 878, 917,\n                    515, 555);\n        }\n        if (view.chaina()) {\n            return new Layout(768, 42, 108, 137,\n                    1322, 185, 226, 445, 486, 708, 750,\n                    610, 690, 520, 610, 642, 674,\n                    300, 160, 210, 244,\n                    175, 610, 370, 124, 842, 357, 795, 875, 914,\n                    515, 555);\n        }\n        return new Layout(768, 42, 108, 137,\n                1322, 185, 226, 445, 486, 708, 750,\n                610, 690, 520, 610, 642, 674,\n                300, 160, 210, 244,\n                225, 670, 420, 128, 848, 435, 880, 890, 930,\n                515, 555);\n    }\n\n'''
s = s[:start] + layout + s[end:]
p.write_text(s, encoding="utf-8")
print("patched", gpath)

# Dialogos de NPC y misiones: tema oscuro Chaina, texto claro y mas espacio util.
edit("src/client/java/com/andrewbristowx/chainacobblemon/client/npc/NpcDialogueScreen.java", [
    ('panelWidth = Math.min(920, width - 36);', 'panelWidth = Math.min(980, width - 36);'),
    ('panelHeight = Math.min(190, height - 30);', 'panelHeight = Math.min(210, height - 30);'),
    ('context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xFFF8F3F4);',
     'context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xFF202124);'),
    ('context.fill(panelX + 12, panelY + 34, panelX + panelWidth - 12, panelY + panelHeight - 31, 0xFFFDE5E8);',
     'context.fill(panelX + 12, panelY + 38, panelX + panelWidth - 12, panelY + panelHeight - 34, 0xFF2D2E33);'),
    ('panelX + 18, panelY + 16, 0xFF353434', 'panelX + 18, panelY + 16, 0xFFF5F5F7'),
    ('panelX + 18, panelY + 27, 0xFFE56A78', 'panelX + 18, panelY + 28, 0xFFFB9AA6'),
    ('panelX + panelWidth - textRenderer.getWidth(counter) - 18, panelY + 17, 0xFFE56A78',
     'panelX + panelWidth - textRenderer.getWidth(counter) - 18, panelY + 17, 0xFFF6AD4B'),
    ('state.locked() ? 0xFFF1D8DC : 0xFFFFC8D0', 'state.locked() ? 0xFF5A4449 : 0xFFF9556D'),
    ('int portraitY = panelY + 47;', 'int portraitY = panelY + 55;'),
    ('int textX = panelX + 108;', 'int textX = panelX + 112;'),
    ('int y = panelY + 48;', 'int y = panelY + 55;'),
    ('context.drawTextWithShadow(textRenderer, line, textX, y, 0xFF353434);',
     'context.drawTextWithShadow(textRenderer, line, textX, y, 0xFFF5F5F7);'),
    ('int rewardColor = state.rewardClaimed() ? 0xFFB59BAE : 0xFFFFD86A;',
     'int rewardColor = state.rewardClaimed() ? 0xFF9CE3B0 : 0xFFF6AD4B;'),
    ('panelY + panelHeight - 45, 0xFFFF8B9C', 'panelY + panelHeight - 45, 0xFFF9556D'),
    ('panelY + panelHeight - 45, 0xFFFFA8D5', 'panelY + panelHeight - 45, 0xFFFB9AA6'),
])

# Version del proyecto transformado.
gp = root / "gradle.properties"
gs = gp.read_text(encoding="utf-8")
gs = gs.replace("mod_version=0.3.0-alpha.3+1.21.1", "mod_version=0.3.0-alpha.4+1.21.1")
gs = gs.replace("mod_version=0.2.0-alpha.6+1.21.1", "mod_version=0.3.0-alpha.4+1.21.1")
gp.write_text(gs, encoding="utf-8")
print("alpha4 layout/dialogue fixes complete")
