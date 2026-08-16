package com.andrewbristowx.chainacobblemon.client.systems;

import com.andrewbristowx.chainacobblemon.Chainacobblemon;
import com.andrewbristowx.chainacobblemon.registry.ChainaRegistries;
import com.andrewbristowx.chainacobblemon.systems.SystemSnapshots;
import com.andrewbristowx.chainacobblemon.systems.SystemsNetworking;
import com.google.gson.Gson;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

/** Client side UI for alpha.4. Layout is original Chaina styling, mechanics mirror the validated Emi systems. */
public final class SystemsClient {
    private static final Gson GSON = new Gson();
    private static final int CHARCOAL = 0xF01B1A1C;
    private static final int PANEL = 0xF02B292D;
    private static final int PANEL_LIGHT = 0xF03A373D;
    private static final int CORAL = 0xFFF9556D;
    private static final int GOLD = 0xFFF6AD4B;
    private static final int SAKURA = 0xFFFB9AA6;
    private static final int PALE = 0xFFFDC7CB;
    private static final int OFFWHITE = 0xFFF5F5F7;
    private static final int MUTED = 0xFFBDB7C3;

    private SystemsClient() {}

    public static void initialize() {
        ClientPlayNetworking.registerGlobalReceiver(SystemsNetworking.OpenGachaPayload.ID, (payload, context) ->
                context.client().execute(() -> openGacha(payload.json())));
        ClientPlayNetworking.registerGlobalReceiver(SystemsNetworking.OpenDailyPayload.ID, (payload, context) ->
                context.client().execute(() -> openDaily(payload.json())));
        ClientPlayNetworking.registerGlobalReceiver(SystemsNetworking.OpenPassPayload.ID, (payload, context) ->
                context.client().execute(() -> openPass(payload.json())));
    }

    static void gacha(String action, String banner, int value) {
        if (ClientPlayNetworking.canSend(SystemsNetworking.GachaActionPayload.ID))
            ClientPlayNetworking.send(new SystemsNetworking.GachaActionPayload(SystemsNetworking.gachaActionJson(action, banner, value)));
    }

    static void daily(String action) {
        if (ClientPlayNetworking.canSend(SystemsNetworking.DailyActionPayload.ID))
            ClientPlayNetworking.send(new SystemsNetworking.DailyActionPayload(action));
    }

    static void pass(String action, int value) {
        if (ClientPlayNetworking.canSend(SystemsNetworking.PassActionPayload.ID))
            ClientPlayNetworking.send(new SystemsNetworking.PassActionPayload(SystemsNetworking.passActionJson(action, value)));
    }

    private static void openGacha(String json) {
        try {
            SystemSnapshots.GachaSnapshot snapshot = GSON.fromJson(json, SystemSnapshots.GachaSnapshot.class);
            if (snapshot == null) return;
            MinecraftClient client = MinecraftClient.getInstance();
            Screen parent = client.currentScreen instanceof GachaScreen old ? old.parent : client.currentScreen;
            client.setScreen(new GachaScreen(parent, snapshot));
        } catch (Exception exception) { Chainacobblemon.LOGGER.error("Could not open Chaina gasha screen", exception); }
    }

    private static void openDaily(String json) {
        try {
            SystemSnapshots.DailySnapshot snapshot = GSON.fromJson(json, SystemSnapshots.DailySnapshot.class);
            if (snapshot == null) return;
            MinecraftClient client = MinecraftClient.getInstance();
            Screen parent = client.currentScreen instanceof DailyScreen old ? old.parent : client.currentScreen;
            client.setScreen(new DailyScreen(parent, snapshot));
        } catch (Exception exception) { Chainacobblemon.LOGGER.error("Could not open Chaina daily screen", exception); }
    }

    private static void openPass(String json) {
        try {
            SystemSnapshots.PassSnapshot snapshot = GSON.fromJson(json, SystemSnapshots.PassSnapshot.class);
            if (snapshot == null) return;
            MinecraftClient client = MinecraftClient.getInstance();
            Screen parent = client.currentScreen instanceof PassScreen old ? old.parent : client.currentScreen;
            client.setScreen(new PassScreen(parent, snapshot));
        } catch (Exception exception) { Chainacobblemon.LOGGER.error("Could not open Chaina pass screen", exception); }
    }

    private abstract static class ChainaScreen extends Screen {
        final Screen parent;
        int x, y, w, h;
        ChainaScreen(Text title, Screen parent) { super(title); this.parent = parent; }
        @Override protected void init() {
            w = Math.min(900, Math.max(420, width - 36));
            h = Math.min(520, Math.max(300, height - 36));
            x = (width - w) / 2; y = (height - h) / 2;
        }
        void base(DrawContext context, String title, String subtitle) {
            context.fill(0, 0, width, height, 0xCC09090B);
            context.fill(x, y, x + w, y + h, CHARCOAL);
            context.fill(x + 3, y + 3, x + w - 3, y + h - 3, PANEL);
            context.fill(x + 3, y + 3, x + w - 3, y + 8, CORAL);
            context.fill(x + 3, y + h - 8, x + w - 3, y + h - 3, GOLD);
            // Bell / resonance decorative corners, intentionally simple for this systems-first alpha.
            context.fill(x + 18, y + 21, x + 26, y + 29, GOLD);
            context.fill(x + w - 26, y + 21, x + w - 18, y + 29, GOLD);
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(title), x + w / 2, y + 20, OFFWHITE);
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(subtitle), x + w / 2, y + 38, SAKURA);
        }
        void label(DrawContext context, String value, int px, int py, int color) { context.drawTextWithShadow(textRenderer, Text.literal(value), px, py, color); }
        void centered(DrawContext context, String value, int px, int py, int color) { context.drawCenteredTextWithShadow(textRenderer, Text.literal(value), px, py, color); }
        ButtonWidget button(String text, int bx, int by, int bw, int bh, java.util.function.Consumer<ButtonWidget> action) {
            return ButtonWidget.builder(Text.literal(text), action::accept).dimensions(bx, by, bw, bh).build();
        }
        @Override public void close() { if (client != null) client.setScreen(parent); }
        @Override public boolean shouldPause() { return false; }
        @Override public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {}
    }

    private static final class GachaScreen extends ChainaScreen {
        final SystemSnapshots.GachaSnapshot snapshot;
        GachaScreen(Screen parent, SystemSnapshots.GachaSnapshot snapshot) { super(Text.literal("Gasha Chaina"), parent); this.snapshot = snapshot; }
        @Override protected void init() {
            super.init();
            int tabsY = y + 62;
            addDrawableChild(button("ESTÁNDAR", x + 28, tabsY, 130, 24, b -> gacha("banner", "standard", 0)));
            addDrawableChild(button("CHAINA ✦", x + 166, tabsY, 130, 24, b -> gacha("banner", "chaina", 0)));
            int actionsY = y + h - 52;
            addDrawableChild(button("TIRAR ×1", x + w / 2 - 154, actionsY, 145, 28, b -> gacha("pull", snapshot.banner, 1)));
            addDrawableChild(button("TIRAR ×10", x + w / 2 + 9, actionsY, 145, 28, b -> gacha("pull", snapshot.banner, 10)));
            addDrawableChild(button("×", x + w - 35, y + 14, 22, 22, b -> close()));
        }
        @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            boolean chaina = "chaina".equals(snapshot.banner);
            base(context, chaina ? "GASHA DE CHAINA" : "GASHA ESTÁNDAR", chaina ? "Festival del Cascabel · Resonancia especial" : "Banner general de Cobblemon");
            int left = x + 28, top = y + 103;
            context.fill(left, top, left + 245, y + h - 70, PANEL_LIGHT);
            label(context, "TICKETS", left + 16, top + 16, GOLD);
            label(context, "Estándar: " + snapshot.standardTickets + "  + " + snapshot.standardRolls + " virtuales", left + 16, top + 38, OFFWHITE);
            label(context, "Chaina: " + snapshot.chainaTickets + "  + " + snapshot.chainaRolls + " virtuales", left + 16, top + 55, OFFWHITE);
            label(context, "PITY", left + 16, top + 90, GOLD);
            label(context, snapshot.pity + " / " + snapshot.hardPity, left + 16, top + 111, OFFWHITE);
            int barX = left + 16, barY = top + 133, barW = 210;
            context.fill(barX, barY, barX + barW, barY + 9, 0xFF151417);
            float fraction = MathHelper.clamp(snapshot.pity / (float) Math.max(1, snapshot.hardPity), 0F, 1F);
            context.fill(barX + 1, barY + 1, barX + 1 + Math.round((barW - 2) * fraction), barY + 8, snapshot.pity >= snapshot.softPity ? GOLD : CORAL);
            label(context, "Soft pity: " + snapshot.softPity, left + 16, top + 151, MUTED);
            ItemStack ticket = new ItemStack(chaina ? ChainaRegistries.CHAINA_GACHA_TICKET : ChainaRegistries.GACHA_TICKET);
            context.getMatrices().push(); context.getMatrices().translate(left + 95, top + 198, 80); context.getMatrices().scale(3.0F, 3.0F, 1F); context.drawItem(ticket, 0, 0); context.getMatrices().pop();

            int rx = left + 266, rw = x + w - 28 - rx;
            context.fill(rx, top, rx + rw, y + h - 70, 0xE0222025);
            centered(context, snapshot.results.isEmpty() ? "RESULTADOS" : "ÚLTIMAS TIRADAS", rx + rw / 2, top + 14, PALE);
            List<SystemSnapshots.GachaResultView> results = snapshot.results == null ? List.of() : snapshot.results;
            if (results.isEmpty()) {
                centered(context, chaina ? "✦ Haz sonar el cascabel ✦" : "Usa un ticket para comenzar", rx + rw / 2, top + 85, MUTED);
                centered(context, "El servidor decide la tirada y entrega el Pokémon", rx + rw / 2, top + 108, MUTED);
            } else {
                int rowH = results.size() > 5 ? 25 : 38;
                for (int i = 0; i < Math.min(10, results.size()); i++) {
                    SystemSnapshots.GachaResultView r = results.get(i);
                    int yy = top + 40 + i * rowH;
                    int tierColor = tierColor(r.tier);
                    context.fill(rx + 15, yy - 4, rx + rw - 15, yy + rowH - 8, 0x44111114);
                    label(context, (i + 1) + ".", rx + 24, yy + 3, MUTED);
                    label(context, r.name + "  Nv." + r.level, rx + 48, yy + 3, r.shiny ? GOLD : OFFWHITE);
                    String extra = r.tier + (r.shiny ? " · SHINY" : "") + (r.pity ? " · PITY" : "");
                    label(context, extra, rx + rw - 155, yy + 3, tierColor);
                }
            }
            if (snapshot.message != null && !snapshot.message.isBlank()) centered(context, snapshot.message, x + w / 2, y + h - 82, OFFWHITE);
            super.render(context, mouseX, mouseY, delta);
        }
    }

    private static final class DailyScreen extends ChainaScreen {
        final SystemSnapshots.DailySnapshot snapshot;
        DailyScreen(Screen parent, SystemSnapshots.DailySnapshot snapshot) { super(Text.literal("Login diario Chaina"), parent); this.snapshot = snapshot; }
        @Override protected void init() {
            super.init();
            ButtonWidget claim = button(snapshot.eligible ? "RECLAMAR" : "RECLAMADO", x + w / 2 - 85, y + h - 58, 170, 30, b -> daily("claim"));
            claim.active = snapshot.eligible;
            addDrawableChild(claim);
            addDrawableChild(button("×", x + w - 35, y + 14, 22, 22, b -> close()));
        }
        @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            base(context, "LOGIN DIARIO", "Cascabel de bienvenida · vuelve cada día");
            int top = y + 78;
            context.fill(x + 28, top, x + 235, y + h - 82, PANEL_LIGHT);
            label(context, "RACHA", x + 48, top + 24, GOLD);
            label(context, snapshot.streak + " día" + (snapshot.streak == 1 ? "" : "s"), x + 48, top + 46, OFFWHITE);
            label(context, "RECLAMOS", x + 48, top + 84, GOLD);
            label(context, Integer.toString(snapshot.totalClaims), x + 48, top + 106, OFFWHITE);
            label(context, "PRÓXIMA", x + 48, top + 144, GOLD);
            label(context, remaining(snapshot.nextClaimEpochMillis), x + 48, top + 166, OFFWHITE);
            label(context, "ÚLTIMA", x + 48, top + 204, GOLD);
            label(context, trim(snapshot.lastReward, 26), x + 48, top + 226, OFFWHITE);

            int px = x + 260;
            context.fill(px, top, x + w - 28, y + h - 82, 0xE0222025);
            centered(context, snapshot.eligible ? "✦ RECOMPENSA LISTA ✦" : "✓ YA RECLAMADA", (px + x + w - 28) / 2, top + 20, snapshot.eligible ? SAKURA : GOLD);
            centered(context, "Posibles recompensas", (px + x + w - 28) / 2, top + 58, OFFWHITE);
            List<SystemSnapshots.DailyRewardView> rewards = snapshot.possibleRewards == null ? List.of() : snapshot.possibleRewards;
            for (int i = 0; i < Math.min(8, rewards.size()); i++) {
                SystemSnapshots.DailyRewardView reward = rewards.get(i);
                int col = i % 2, row = i / 2;
                int bx = px + 22 + col * ((x + w - 56 - px) / 2);
                int by = top + 88 + row * 51;
                context.fill(bx, by, bx + Math.max(120, (x + w - 80 - px) / 2), by + 39, 0x553A373D);
                label(context, "✦ " + trim(reward.label, 24), bx + 8, by + 8, i % 3 == 0 ? GOLD : PALE);
                label(context, "peso " + reward.weight, bx + 8, by + 23, MUTED);
            }
            if (snapshot.message != null && !snapshot.message.isBlank()) centered(context, snapshot.message, x + w / 2, y + h - 78, OFFWHITE);
            super.render(context, mouseX, mouseY, delta);
        }
    }

    private static final class PassScreen extends ChainaScreen {
        final SystemSnapshots.PassSnapshot snapshot;
        PassScreen(Screen parent, SystemSnapshots.PassSnapshot snapshot) { super(Text.literal("Pase de Chaina"), parent); this.snapshot = snapshot; }
        @Override protected void init() {
            super.init();
            addDrawableChild(button("‹", x + 18, y + h / 2 - 14, 28, 28, b -> pass("page", Math.max(0, snapshot.page - 1))));
            addDrawableChild(button("›", x + w - 46, y + h / 2 - 14, 28, 28, b -> pass("page", snapshot.page + 1)));
            addDrawableChild(button("×", x + w - 35, y + 14, 22, 22, b -> close()));
            addClaimButtons(snapshot.free, false, y + 190);
            addClaimButtons(snapshot.premiumTrack, true, y + 342);
        }
        private void addClaimButtons(List<SystemSnapshots.PassRewardSlot> slots, boolean premium, int by) {
            if (slots == null || slots.isEmpty()) return;
            int innerX = x + 70, innerW = w - 140, gap = 6;
            int cardW = Math.max(62, (innerW - gap * (slots.size() - 1)) / slots.size());
            for (int i = 0; i < slots.size(); i++) {
                SystemSnapshots.PassRewardSlot slot = slots.get(i);
                if (!slot.claimable || premium && !snapshot.premium) continue;
                int bx = innerX + i * (cardW + gap);
                addDrawableChild(button("RECLAMAR", bx + 4, by + 92, cardW - 8, 18, b -> pass(premium ? "claim_premium" : "claim_free", slot.level)));
            }
        }
        @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            base(context, "PASE DE CHAINA", "Resonancia infinita · progreso gratis + premium");
            centered(context, snapshot.playerName + " · NIVEL " + snapshot.level, x + w / 2, y + 69, OFFWHITE);
            long span = Math.max(1, snapshot.nextLevelXp - snapshot.levelStartXp);
            float progress = MathHelper.clamp((snapshot.experience - snapshot.levelStartXp) / (float) span, 0F, 1F);
            int barX = x + 180, barY = y + 91, barW = w - 360;
            context.fill(barX, barY, barX + barW, barY + 10, 0xFF141316);
            context.fill(barX + 1, barY + 1, barX + 1 + Math.round((barW - 2) * progress), barY + 9, CORAL);
            centered(context, (snapshot.experience - snapshot.levelStartXp) + " / " + span + " XP", x + w / 2, y + 106, PALE);
            centered(context, "Tiradas Chaina guardadas: " + snapshot.chainaRolls, x + w / 2, y + 127, GOLD);
            centered(context, "GRATIS", x + 115, y + 159, PALE);
            centered(context, snapshot.premium ? "PREMIUM ACTIVO" : "PREMIUM BLOQUEADO", x + 135, y + 311, snapshot.premium ? GOLD : MUTED);
            drawTrack(context, snapshot.free, false, y + 184);
            drawTrack(context, snapshot.premiumTrack, true, y + 336);
            centered(context, "Página " + (snapshot.page + 1), x + w / 2, y + h - 40, MUTED);
            if (snapshot.message != null && !snapshot.message.isBlank()) centered(context, snapshot.message, x + w / 2, y + h - 58, OFFWHITE);
            super.render(context, mouseX, mouseY, delta);
        }
        private void drawTrack(DrawContext context, List<SystemSnapshots.PassRewardSlot> slots, boolean premium, int top) {
            if (slots == null || slots.isEmpty()) return;
            int innerX = x + 70, innerW = w - 140, gap = 6;
            int cardW = Math.max(62, (innerW - gap * (slots.size() - 1)) / slots.size());
            for (int i = 0; i < slots.size(); i++) {
                SystemSnapshots.PassRewardSlot slot = slots.get(i);
                int bx = innerX + i * (cardW + gap);
                int bg = !slot.unlocked || premium && !snapshot.premium ? 0xA018171A : 0xE03A373D;
                context.fill(bx, top, bx + cardW, top + 112, bg);
                context.fill(bx, top, bx + cardW, top + 3, premium ? GOLD : CORAL);
                centered(context, "NV." + slot.level, bx + cardW / 2, top + 10, OFFWHITE);
                centered(context, slot.amount > 0 ? "✦ ×" + slot.amount : "✧", bx + cardW / 2, top + 39, slot.amount > 0 ? GOLD : MUTED);
                String status = slot.claimed ? "RECLAMADO" : slot.claimable && (!premium || snapshot.premium) ? "LISTO" : slot.unlocked ? "SIN PREMIO" : "BLOQUEADO";
                centered(context, status, bx + cardW / 2, top + 68, slot.claimed ? 0xFF9CE3B0 : slot.claimable ? PALE : MUTED);
            }
        }
    }

    private static int tierColor(String tier) {
        if (tier == null) return OFFWHITE;
        return switch (tier.toUpperCase(Locale.ROOT)) {
            case "LEGENDARY" -> GOLD;
            case "EPIC" -> SAKURA;
            case "RARE" -> 0xFF8BC6FF;
            case "UNCOMMON" -> 0xFF9CE3B0;
            default -> OFFWHITE;
        };
    }

    private static String remaining(long target) {
        Duration duration = Duration.ofMillis(Math.max(0L, target - System.currentTimeMillis()));
        long h = duration.toHours(), m = duration.minusHours(h).toMinutes();
        return String.format(Locale.ROOT, "%02d:%02d", h, m);
    }

    private static String trim(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, Math.max(0, max - 1)) + "…";
    }
}
