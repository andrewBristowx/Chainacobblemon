package com.andrewbristowx.chainacobblemon.client.rewards;

import com.andrewbristowx.chainacobblemon.client.render.PokemonPortraitRenderer;
import com.andrewbristowx.chainacobblemon.registry.ModRegistries;
import com.andrewbristowx.chainacobblemon.rewards.DailyRewardSnapshot;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.time.Duration;
import java.util.List;

/** Chaina daily reward screen: compact reward focus, readable pool cards and a real streak/status column. */
final class DailyRewardScreen extends Screen {
    private static final int REF_W = 1536;
    private static final int REF_H = 1024;

    private static final int BG = 0xFF1D1E22;
    private static final int PANEL = 0xFF242529;
    private static final int PANEL_2 = 0xFF2D2E33;
    private static final int SIDE = 0xFF362F34;
    private static final int CORAL = 0xFFF9556D;
    private static final int SAKURA = 0xFFFB9AA6;
    private static final int GOLD = 0xFFF6AD4B;
    private static final int TEXT = 0xFFF5F5F7;
    private static final int MUTED = 0xFFCAD0D8;
    private static final int GREEN = 0xFF9CE3B0;

    private final Screen parent;
    private final DailyRewardSnapshot snapshot;
    private final long openedAt = System.currentTimeMillis();
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;

    DailyRewardScreen(Screen parent, DailyRewardSnapshot snapshot) {
        super(Text.literal("Recompensa diaria"));
        this.parent = parent;
        this.snapshot = snapshot;
    }

    Screen parent() { return parent; }

    @Override
    protected void init() {
        int availableW = Math.min(REF_W, Math.max(1, width - 18));
        int availableH = Math.min(REF_H, Math.max(1, height - 18));
        if (availableW / (float) availableH > REF_W / (float) REF_H) {
            panelH = availableH;
            panelW = Math.round(panelH * REF_W / (float) REF_H);
        } else {
            panelW = availableW;
            panelH = Math.round(panelW * REF_H / (float) REF_W);
        }
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;

        addDrawableChild(new InvisibleHotspotButton(panelX + rx(1450), panelY + ry(24), rx(54), ry(48),
                Text.literal("Cerrar recompensa diaria"), button -> close()));

        PassButtonWidget claim = new PassButtonWidget(panelX + rx(392), panelY + ry(585), rx(426), ry(70),
                Text.literal(snapshot.eligible ? "RECLAMAR AHORA" : "RECLAMADO"), button -> DailyRewardClient.send("claim"));
        claim.active = snapshot.eligible;
        addDrawableChild(claim);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xD9000000);
        drawFrame(context);
        drawHeader(context);
        drawRewardStage(context);
        drawPossibleRewards(context);
        drawStatus(context);
        super.render(context, mouseX, mouseY, delta);
    }

    private void drawFrame(DrawContext c) {
        int x = panelX, y = panelY, w = panelW, h = panelH;
        c.fill(x, y, x + w, y + h, BG);
        c.fill(x, y, x + w, y + ry(92), CORAL);
        c.fill(x, y + ry(92), x + w, y + ry(100), GOLD);

        fillRef(c, 28, 120, 1140, 824, PANEL);
        fillRef(c, 1190, 120, 314, 824, SIDE);
        outlineRef(c, 28, 120, 1140, 824, SAKURA, 3);
        outlineRef(c, 1190, 120, 314, 824, GOLD, 3);

        fillRef(c, 335, 220, 540, 350, PANEL_2);
        outlineRef(c, 335, 220, 540, 350, snapshot.eligible ? CORAL : GREEN, 4);
        fillRef(c, 358, 246, 494, 42, 0xFF34353A);

        fillRef(c, 48, 716, 1100, 202, 0xFF202126);
        outlineRef(c, 48, 716, 1100, 202, 0xFF4B4148, 2);

        sakura(c, panelX + rx(60), panelY + ry(135), SAKURA, GOLD);
        sakura(c, panelX + rx(1115), panelY + ry(680), CORAL, GOLD);
        sakura(c, panelX + rx(1460), panelY + ry(140), SAKURA, GOLD);
        sakura(c, panelX + rx(1450), panelY + ry(900), CORAL, GOLD);
    }

    private void drawHeader(DrawContext context) {
        drawCentered(context, "RECOMPENSA DIARIA", 768, 38, TEXT);
        drawCentered(context, snapshot.eligible ? "Tu recompensa está lista" : "Vuelve mañana • mantén tu racha",
                768, 68, snapshot.eligible ? 0xFFFFE1A6 : TEXT);
    }

    private void drawRewardStage(DrawContext context) {
        int cx = panelX + rx(605);
        int cy = panelY + ry(418);
        drawCenteredPx(context, snapshot.eligible ? "PREMIO DEL DÍA" : "ÚLTIMA RECOMPENSA",
                cx, panelY + ry(260), GOLD);

        DailyRewardSnapshot.RewardView reward = snapshot.revealed;
        if (reward == null) {
            drawCenteredPx(context, snapshot.eligible ? "?" : "✓", cx, cy - ry(32),
                    snapshot.eligible ? GOLD : GREEN);
            drawCenteredPx(context, snapshot.eligible ? "CÁPSULA SORPRESA" : safeLastReward(),
                    cx, panelY + ry(515), TEXT);
            drawCenteredPx(context, snapshot.eligible ? "Ábrela y descubre qué te tocó" : "Reclamada por hoy",
                    cx, panelY + ry(546), MUTED);
            return;
        }

        long elapsed = System.currentTimeMillis() - openedAt;
        if (elapsed < 1_250L && snapshot.possibleRewards != null && !snapshot.possibleRewards.isEmpty()) {
            int cycling = (int) ((elapsed / 95L) % snapshot.possibleRewards.size());
            drawRewardIcon(context, snapshot.possibleRewards.get(cycling), cx, cy, rx(108));
            drawCenteredPx(context, "ABRIENDO CÁPSULA…", cx, panelY + ry(520), TEXT);
            return;
        }

        float pulse = elapsed < 1400L ? 0.82F + (float) Math.abs(Math.sin(elapsed / 95.0D)) * 0.24F : 1.0F;
        context.getMatrices().push();
        context.getMatrices().translate(cx, cy, 0.0F);
        context.getMatrices().scale(pulse, pulse, 1.0F);
        context.getMatrices().translate(-cx, -cy, 0.0F);
        drawRewardIcon(context, reward, cx, cy, rx(118));
        context.getMatrices().pop();
        drawFittedCenteredPx(context, reward.label, cx, panelY + ry(518), rx(440), TEXT);
        if (reward.shiny) drawCenteredPx(context, "✦ SHINY ✦", cx, panelY + ry(548), GOLD);
    }

    private void drawPossibleRewards(DrawContext context) {
        drawCentered(context, "POSIBLES PREMIOS", 598, 682, GOLD);
        List<DailyRewardSnapshot.RewardView> rewards = snapshot.possibleRewards == null ? List.of() : snapshot.possibleRewards;
        int[] preferred = rewards.size() >= 8 ? new int[] {0, 1, 2, 4, 5, 6, 7} : new int[] {0, 1, 2, 3, 4, 5, 6};
        int[] centers = {128, 285, 442, 599, 756, 913, 1070};
        for (int index = 0; index < centers.length; index++) {
            int centerRef = centers[index];
            int cardX = centerRef - 68;
            fillRef(context, cardX, 748, 136, 148, index % 2 == 0 ? 0xFF2C2D32 : 0xFF292A2F);
            outlineRef(context, cardX, 748, 136, 148, 0xFF5B4650, 2);
            drawCentered(context, Integer.toString(index + 1), centerRef, 760, 0xFF8B7B83);
            if (index >= rewards.size()) continue;
            DailyRewardSnapshot.RewardView reward = rewards.get(Math.min(preferred[index], rewards.size() - 1));
            int cx = panelX + rx(centerRef);
            drawRewardIcon(context, reward, cx, panelY + ry(814), rx(48));
            drawFittedCenteredPx(context, shortLabel(reward), cx, panelY + ry(865), rx(118), TEXT);
        }
    }

    private void drawStatus(DrawContext context) {
        int cx = panelX + rx(1347);
        drawCenteredPx(context, "RACHA ACTUAL", cx, panelY + ry(175), GOLD);
        drawCenteredPx(context, snapshot.streak + " día" + (snapshot.streak == 1 ? "" : "s"), cx, panelY + ry(212), TEXT);

        int cycle = snapshot.streak <= 0 ? 0 : ((snapshot.streak - 1) % 7) + 1;
        drawCenteredPx(context, "CICLO DE 7 DÍAS", cx, panelY + ry(274), 0xFFFFC8D0);
        int dotStart = 1248;
        for (int i = 0; i < 7; i++) {
            int color = i < cycle ? CORAL : 0xFF5A5055;
            fillRef(context, dotStart + i * 30, 315, 18, 18, color);
        }
        drawCenteredPx(context, cycle + " / 7", cx, panelY + ry(350), MUTED);

        fillRef(context, 1222, 408, 250, 2, 0xFF5A4E54);
        drawCenteredPx(context, "RECLAMOS TOTALES", cx, panelY + ry(448), GOLD);
        drawCenteredPx(context, Integer.toString(snapshot.totalClaims), cx, panelY + ry(488), TEXT);

        fillRef(context, 1222, 548, 250, 2, 0xFF5A4E54);
        drawCenteredPx(context, "PRÓXIMA RECOMPENSA", cx, panelY + ry(590), GOLD);
        drawCenteredPx(context, snapshot.eligible ? "DISPONIBLE AHORA" : remaining(), cx,
                panelY + ry(632), snapshot.eligible ? GREEN : TEXT);

        fillRef(context, 1222, 690, 250, 2, 0xFF5A4E54);
        drawCenteredPx(context, snapshot.eligible ? "NO PIERDAS TU RACHA" : "REGRESA CUANDO EL CONTADOR TERMINE",
                cx, panelY + ry(725), 0xFFFFC8D0);

        if (snapshot.message != null && !snapshot.message.isBlank()) {
            List<net.minecraft.text.OrderedText> lines = textRenderer.wrapLines(Text.literal(snapshot.message), rx(238));
            int y = panelY + ry(780);
            for (int index = 0; index < Math.min(6, lines.size()); index++) {
                net.minecraft.text.OrderedText line = lines.get(index);
                context.drawTextWithShadow(textRenderer, line, cx - textRenderer.getWidth(line) / 2,
                        y + index * Math.max(10, ry(22)), TEXT);
            }
        }
    }

    private String safeLastReward() {
        return snapshot.lastReward == null || snapshot.lastReward.isBlank() ? "RECOMPENSA RECLAMADA" : snapshot.lastReward;
    }

    private String remaining() {
        long millis = Math.max(0L, snapshot.nextClaimEpochMillis - System.currentTimeMillis());
        Duration duration = Duration.ofMillis(millis);
        long hours = duration.toHours();
        long minutes = duration.minusHours(hours).toMinutes();
        return String.format(java.util.Locale.ROOT, "%02d:%02d", hours, minutes);
    }

    private void drawRewardIcon(DrawContext context, DailyRewardSnapshot.RewardView reward, int cx, int cy, int size) {
        if (reward == null) return;
        if ("POKEMON".equals(reward.type)) {
            if (reward.speciesId != null && !reward.speciesId.isBlank()
                    && PokemonPortraitRenderer.draw(context, reward.speciesId, cx, cy + size / 2, size)) return;
            drawLargeItem(context, new ItemStack(ModRegistries.RANDOM_POKEMON_ICON), cx, cy, size);
            return;
        }
        ItemStack stack = rewardItem(reward);
        if (!stack.isEmpty()) {
            drawLargeItem(context, stack, cx, cy, size);
            if (reward.amount > 1) drawCenteredPx(context, "×" + reward.amount, cx + size / 2, cy + size / 3, TEXT);
            return;
        }
        if ("CHAIBELLS".equals(reward.type)) {
            drawLargeItem(context, new ItemStack(ModRegistries.CHAIBELL_ICON), cx, cy, size);
            if (reward.amount > 1) drawCenteredPx(context, "×" + reward.amount, cx + size / 2, cy + size / 3, TEXT);
            return;
        }
        drawCenteredPx(context, "✦", cx, cy - 7, CORAL);
    }

    private ItemStack rewardItem(DailyRewardSnapshot.RewardView reward) {
        if ("CHAINA_ROLLS".equals(reward.type)) return new ItemStack(ModRegistries.CHAINA_SPECIAL_BANNER_TICKET);
        if ("STANDARD_ROLLS".equals(reward.type)) return new ItemStack(ModRegistries.GACHA_TICKET);
        if (!"ITEM".equals(reward.type)) return ItemStack.EMPTY;
        Identifier id = Identifier.tryParse(reward.value);
        if (id == null || !Registries.ITEM.containsId(id)) return ItemStack.EMPTY;
        Item item = Registries.ITEM.get(id);
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    private String shortLabel(DailyRewardSnapshot.RewardView reward) {
        if (reward == null) return "";
        return switch (reward.type) {
            case "CHAIBELLS" -> reward.amount + " ChaiBells";
            case "CHAINA_ROLLS" -> reward.amount + "× Ticket Chaina";
            case "STANDARD_ROLLS" -> reward.amount + "× Ticket Gasha";
            case "POKEMON" -> "Pokémon sorpresa";
            default -> {
                ItemStack stack = rewardItem(reward);
                String name = stack.isEmpty() ? reward.label : stack.getName().getString();
                yield compactItemLabel(reward.amount, name);
            }
        };
    }

    private String compactItemLabel(long amount, String name) {
        if (name == null || name.isBlank()) return amount + "× Premio";
        String compact = name
                .replace("Ticket de Tesoros de Chaina", "Ticket Tesoro")
                .replace("Ticket del Gasha de Chaina", "Ticket Chaina")
                .replace("Ticket Gasha", "Ticket Gasha");
        return amount + "× " + compact;
    }

    private void drawFittedCenteredPx(DrawContext context, String value, int centerX, int y, int maxWidth, int color) {
        String safe = value == null ? "" : value;
        int width = Math.max(1, textRenderer.getWidth(safe));
        float scale = Math.min(1.0F, maxWidth / (float) width);
        context.getMatrices().push();
        context.getMatrices().translate(centerX, y, 185.0F);
        context.getMatrices().scale(scale, scale, 1.0F);
        context.drawTextWithShadow(textRenderer, Text.literal(safe), -width / 2, 0, color);
        context.getMatrices().pop();
    }

    private void drawLargeItem(DrawContext context, ItemStack stack, int centerX, int centerY, int size) {
        float scale = Math.max(1.0F, size / 16.0F);
        context.getMatrices().push();
        context.getMatrices().translate(centerX - 8.0F * scale, centerY - 8.0F * scale, 170.0F);
        context.getMatrices().scale(scale, scale, 1.0F);
        context.drawItem(stack, 0, 0);
        context.getMatrices().pop();
    }

    private void fillRef(DrawContext c, int x, int y, int w, int h, int color) {
        c.fill(panelX + rx(x), panelY + ry(y), panelX + rx(x + w), panelY + ry(y + h), color);
    }

    private void outlineRef(DrawContext c, int x, int y, int w, int h, int color, int thicknessRef) {
        int t = Math.max(1, rx(thicknessRef));
        int px = panelX + rx(x), py = panelY + ry(y), pw = rx(w), ph = ry(h);
        c.fill(px, py, px + pw, py + t, color);
        c.fill(px, py + ph - t, px + pw, py + ph, color);
        c.fill(px, py, px + t, py + ph, color);
        c.fill(px + pw - t, py, px + pw, py + ph, color);
    }

    private void sakura(DrawContext c, int x, int y, int petal, int center) {
        int s = Math.max(2, rx(7));
        c.fill(x + s, y, x + 2 * s, y + s, petal);
        c.fill(x, y + s, x + s, y + 2 * s, petal);
        c.fill(x + 2 * s, y + s, x + 3 * s, y + 2 * s, petal);
        c.fill(x + s, y + 2 * s, x + 2 * s, y + 3 * s, petal);
        c.fill(x + s, y + s, x + 2 * s, y + 2 * s, center);
    }

    private void drawCentered(DrawContext context, String value, int x, int y, int color) {
        drawCenteredPx(context, value, panelX + rx(x), panelY + ry(y), color);
    }

    private void drawCenteredPx(DrawContext context, String value, int x, int y, int color) {
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(value == null ? "" : value), x, y, color);
    }

    private int rx(int value) { return Math.round(value * panelW / (float) REF_W); }
    private int ry(int value) { return Math.round(value * panelH / (float) REF_H); }

    @Override public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) { }
    @Override public void close() { if (client != null) client.setScreen(parent); }
    @Override public boolean shouldPause() { return false; }
}
