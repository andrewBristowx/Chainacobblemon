package com.andrewbristowx.chainacobblemon.client.gameplay;

import com.andrewbristowx.chainacobblemon.Chainacobblemon;
import com.andrewbristowx.chainacobblemon.gameplay.GameplayNetworking;
import com.google.gson.Gson;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Pantallas de jugador y administración con identidad visual Chaina. */
public final class GameplayClient {
    private static final Gson GSON = new Gson();
    private static final int BACKDROP = 0xD80A090A;
    private static final int CHARCOAL = 0xF01C1A1D;
    private static final int PANEL = 0xF02A272B;
    private static final int PANEL_LIGHT = 0xF0393439;
    private static final int CORAL = 0xFFFF4965;
    private static final int CORAL_DARK = 0xFF9F263A;
    private static final int GOLD = 0xFFF5B34B;
    private static final int GOLD_DARK = 0xFFA86C1E;
    private static final int SAKURA = 0xFFFF91A2;
    private static final int PALE = 0xFFFFD6DC;
    private static final int WHITE = 0xFFF7F4F5;
    private static final int MUTED = 0xFFC4B7BA;
    private static boolean initialized;

    private GameplayClient() {}

    public static synchronized void initialize() {
        if (initialized) return;
        initialized = true;
        ClientPlayNetworking.registerGlobalReceiver(GameplayNetworking.OpenGameplayPayload.ID, (payload, context) ->
                context.client().execute(() -> open(payload.json())));
    }

    private static void open(String json) {
        try {
            GameplayNetworking.Snapshot snapshot = GSON.fromJson(json, GameplayNetworking.Snapshot.class);
            if (snapshot == null) return;
            MinecraftClient client = MinecraftClient.getInstance();
            Screen parent = client.currentScreen instanceof ChainaScreen screen ? screen.rootParent : client.currentScreen;
            String type = snapshot.screen == null ? "menu" : snapshot.screen.toLowerCase(Locale.ROOT);
            Screen next = switch (type) {
                case "jobs" -> new JobsScreen(parent, snapshot);
                case "quests" -> new QuestsScreen(parent, snapshot);
                case "shop" -> new ShopScreen(parent, snapshot);
                case "admin" -> new AdminScreen(parent, snapshot);
                default -> new MainMenuScreen(parent, snapshot);
            };
            client.setScreen(next);
        } catch (Exception exception) {
            Chainacobblemon.LOGGER.error("No se pudo abrir la interfaz de gameplay de Chaina", exception);
        }
    }

    private static void action(String action, String screen, String id, int amount) {
        if (ClientPlayNetworking.canSend(GameplayNetworking.GameplayActionPayload.ID)) {
            ClientPlayNetworking.send(new GameplayNetworking.GameplayActionPayload(
                    GameplayNetworking.actionJson(action, screen, id, amount)));
        }
    }

    private abstract static class ChainaScreen extends Screen {
        final Screen rootParent;
        final GameplayNetworking.Snapshot snapshot;
        int x, y, w, h;

        ChainaScreen(Text title, Screen rootParent, GameplayNetworking.Snapshot snapshot) {
            super(title);
            this.rootParent = rootParent;
            this.snapshot = snapshot;
        }

        @Override protected void init() {
            w = Math.min(940, Math.max(500, width - 30));
            h = Math.min(540, Math.max(330, height - 30));
            x = (width - w) / 2;
            y = (height - h) / 2;
            addDrawableChild(button("×", x + w - 34, y + 13, 22, 22, b -> close()));
        }

        void base(DrawContext context, String title, String subtitle) {
            context.fill(0, 0, width, height, BACKDROP);
            context.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0xFF0C0B0C);
            context.fill(x, y, x + w, y + h, CHARCOAL);
            context.fill(x + 3, y + 3, x + w - 3, y + h - 3, PANEL);
            context.fill(x + 3, y + 3, x + w - 3, y + 8, CORAL);
            context.fill(x + 3, y + h - 8, x + w - 3, y + h - 3, GOLD);
            // Sakura pixel-art discreto; sin imágenes grandes de fondo.
            sakura(context, x + 22, y + 20);
            sakura(context, x + w - 52, y + 20);
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(title), x + w / 2, y + 17, WHITE);
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(subtitle), x + w / 2, y + 34, SAKURA);
            String money = snapshot.balance + " " + snapshot.currencySymbol;
            context.drawTextWithShadow(textRenderer, Text.literal(money), x + 18, y + 17, GOLD);
            if (snapshot.message != null && !snapshot.message.isBlank()) {
                context.drawCenteredTextWithShadow(textRenderer, Text.literal(snapshot.message), x + w / 2, y + h - 24, PALE);
            }
        }

        private void sakura(DrawContext c, int sx, int sy) {
            c.fill(sx + 4, sy, sx + 8, sy + 4, SAKURA);
            c.fill(sx, sy + 4, sx + 4, sy + 8, CORAL);
            c.fill(sx + 8, sy + 4, sx + 12, sy + 8, CORAL);
            c.fill(sx + 4, sy + 8, sx + 8, sy + 12, SAKURA);
            c.fill(sx + 5, sy + 5, sx + 7, sy + 7, GOLD);
        }

        ButtonWidget button(String text, int bx, int by, int bw, int bh, java.util.function.Consumer<ButtonWidget> action) {
            return ButtonWidget.builder(Text.literal(text), action::accept).dimensions(bx, by, bw, bh).build();
        }

        void label(DrawContext c, String value, int px, int py, int color) {
            c.drawTextWithShadow(textRenderer, Text.literal(value), px, py, color);
        }

        void centered(DrawContext c, String value, int px, int py, int color) {
            c.drawCenteredTextWithShadow(textRenderer, Text.literal(value), px, py, color);
        }

        void wrapped(DrawContext c, String value, int px, int py, int width, int color, int maxLines) {
            List<OrderedText> lines = textRenderer.wrapLines(Text.literal(value == null ? "" : value), width);
            for (int i = 0; i < Math.min(maxLines, lines.size()); i++) c.drawTextWithShadow(textRenderer, lines.get(i), px, py + i * 11, color);
        }

        void navButton(String text, String screen, int bx, int by, int bw) {
            addDrawableChild(button(text, bx, by, bw, 24, b -> action("open", screen, "", 1)));
        }

        @Override public boolean shouldPause() { return false; }
        @Override public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) { }
        @Override public void close() { if (client != null) client.setScreen(rootParent); }
    }

    private static final class MainMenuScreen extends ChainaScreen {
        MainMenuScreen(Screen parent, GameplayNetworking.Snapshot snapshot) { super(Text.literal("Menú Chaina"), parent, snapshot); }

        @Override protected void init() {
            super.init();
            int bw = Math.min(190, (w - 100) / 3);
            int gap = 16;
            int total = bw * 3 + gap * 2;
            int start = x + (w - total) / 2;
            int row1 = y + 100;
            int row2 = row1 + 64;
            navButton("✿ HISTORIA Y MISIONES", "quests", start, row1, bw);
            navButton("⚒ TRABAJOS", "jobs", start + bw + gap, row1, bw);
            navButton("✦ TIENDA", "shop", start + (bw + gap) * 2, row1, bw);
            addDrawableChild(button("GASHA ESTÁNDAR", start, row2, bw, 24, b -> action("gacha_standard", "menu", "", 1)));
            addDrawableChild(button("GASHA CHAINA", start + bw + gap, row2, bw, 24, b -> action("gacha_chaina", "menu", "", 1)));
            addDrawableChild(button("LOGIN DIARIO", start + (bw + gap) * 2, row2, bw, 24, b -> action("daily", "menu", "", 1)));
            addDrawableChild(button("PASE DE CHAINA", start, row2 + 64, bw, 24, b -> action("pass", "menu", "", 1)));
            addDrawableChild(button("IR AL HUB", start + bw + gap, row2 + 64, bw, 24, b -> action("hub", "menu", "", 1)));
            addDrawableChild(button("IR AL SPAWN", start + (bw + gap) * 2, row2 + 64, bw, 24, b -> action("spawn", "menu", "", 1)));
            if (snapshot.admin) navButton("⚙ PANEL DE ADMINISTRACIÓN", "admin", x + w / 2 - 125, row2 + 130, 250);
        }

        @Override public void render(DrawContext c, int mx, int my, float delta) {
            base(c, "FESTIVAL DEL CASCABEL", "Menú principal de Chaina Cobblemon");
            centered(c, "Bienvenido, " + snapshot.playerName, x + w / 2, y + 67, WHITE);
            int boxY = y + h - 118;
            c.fill(x + 35, boxY, x + w - 35, boxY + 46, 0x88201C21);
            label(c, "Trabajos activos: " + snapshot.activeJobCount + "/" + (snapshot.maxJobs < 0 ? "∞" : snapshot.maxJobs), x + 55, boxY + 11, PALE);
            label(c, "Todos los sistemas del servidor se administran desde este menú.", x + 55, boxY + 27, MUTED);
            super.render(c, mx, my, delta);
        }
    }

    private static final class JobsScreen extends ChainaScreen {
        JobsScreen(Screen parent, GameplayNetworking.Snapshot snapshot) { super(Text.literal("Trabajos"), parent, snapshot); }

        @Override protected void init() {
            super.init();
            addDrawableChild(button("← MENÚ", x + 18, y + h - 54, 100, 24, b -> action("open", "menu", "", 1)));
            int top = y + 82;
            int rowH = Math.max(42, Math.min(55, (h - 155) / Math.max(1, snapshot.jobs.size())));
            for (int i = 0; i < snapshot.jobs.size(); i++) {
                GameplayNetworking.JobView job = snapshot.jobs.get(i);
                int yy = top + i * rowH;
                ButtonWidget b = button(job.active ? "DEJAR" : "ELEGIR", x + w - 108, yy + 9, 78, 22,
                        btn -> action("job_toggle", "jobs", job.id, 1));
                if (!job.active && snapshot.maxJobs >= 0 && snapshot.activeJobCount >= snapshot.maxJobs) b.active = false;
                addDrawableChild(b);
            }
        }

        @Override public void render(DrawContext c, int mx, int my, float delta) {
            base(c, "TRABAJOS DE CHAINA", "Elige profesiones, sube de nivel y gana " + snapshot.currencyName);
            label(c, "Activos: " + snapshot.activeJobCount + "/" + (snapshot.maxJobs < 0 ? "todos" : snapshot.maxJobs), x + 28, y + 62, GOLD);
            int top = y + 82;
            int rowH = Math.max(42, Math.min(55, (h - 155) / Math.max(1, snapshot.jobs.size())));
            for (int i = 0; i < snapshot.jobs.size(); i++) {
                GameplayNetworking.JobView job = snapshot.jobs.get(i);
                int yy = top + i * rowH;
                c.fill(x + 24, yy, x + w - 24, yy + rowH - 5, job.active ? 0xCC4D2630 : 0xAA302C31);
                if (job.active) c.fill(x + 24, yy, x + 29, yy + rowH - 5, CORAL);
                label(c, (job.active ? "◆ " : "◇ ") + job.name + " · Nivel " + job.level, x + 38, yy + 7, job.active ? PALE : WHITE);
                label(c, job.description == null || job.description.isBlank() ? "Progreso: " + job.progress : job.description, x + 38, yy + 22, MUTED);
                int barL = x + w - 280, barR = x + w - 128, barY = yy + 31;
                double fraction = job.nextLevel <= job.levelStart ? 1D : (double)(job.xp - job.levelStart) / (double)(job.nextLevel - job.levelStart);
                int fill = (int)((barR - barL) * MathHelper.clamp(fraction, 0D, 1D));
                c.fill(barL, barY, barR, barY + 7, 0xFF151316);
                c.fill(barL + 1, barY + 1, barL + Math.max(1, fill), barY + 6, CORAL);
                label(c, "+" + job.rewardAmount + " " + snapshot.currencySymbol + " / " + job.rewardEvery + " acciones", barL, yy + 7, GOLD);
            }
            super.render(c, mx, my, delta);
        }
    }

    private static final class QuestsScreen extends ChainaScreen {
        int chapterIndex;
        List<String> chapters = new ArrayList<>();

        QuestsScreen(Screen parent, GameplayNetworking.Snapshot snapshot) {
            super(Text.literal("Historia y misiones"), parent, snapshot);
            for (GameplayNetworking.QuestView q : snapshot.quests) {
                String key = q.chapter + "|" + q.chapterTitle;
                if (!chapters.contains(key)) chapters.add(key);
            }
        }

        @Override protected void init() {
            super.init();
            addDrawableChild(button("← MENÚ", x + 18, y + h - 54, 100, 24, b -> action("open", "menu", "", 1)));
            addDrawableChild(button("‹", x + 28, y + 64, 24, 22, b -> { if (!chapters.isEmpty()) { chapterIndex = Math.floorMod(chapterIndex - 1, chapters.size()); clearAndInit(); }}));
            addDrawableChild(button("›", x + 210, y + 64, 24, 22, b -> { if (!chapters.isEmpty()) { chapterIndex = Math.floorMod(chapterIndex + 1, chapters.size()); clearAndInit(); }}));
            List<GameplayNetworking.QuestView> visible = visibleQuests();
            int top = y + 104;
            int rowH = Math.max(48, Math.min(64, (h - 175) / Math.max(1, visible.size())));
            for (int i = 0; i < visible.size(); i++) {
                GameplayNetworking.QuestView q = visible.get(i);
                if (q.complete && !q.claimed && !q.locked) {
                    addDrawableChild(button("RECLAMAR", x + w - 118, top + i * rowH + 14, 88, 22,
                            b -> action("quest_claim", "quests", q.id, 1)));
                }
            }
        }

        private List<GameplayNetworking.QuestView> visibleQuests() {
            if (chapters.isEmpty()) return snapshot.quests;
            String chapter = chapters.get(Math.min(chapterIndex, chapters.size() - 1)).split("\\|", 2)[0];
            return snapshot.quests.stream().filter(q -> chapter.equals(q.chapter)).limit(6).toList();
        }

        @Override public void render(DrawContext c, int mx, int my, float delta) {
            base(c, "HISTORIA Y MISIONES", "La aventura de Chaina se organiza por capítulos");
            String chapter = chapters.isEmpty() ? "Sin capítulos" : chapters.get(Math.min(chapterIndex, chapters.size() - 1)).replace('|', ' ');
            c.fill(x + 62, y + 62, x + 200, y + 88, 0xAA3B2429);
            centered(c, "CAPÍTULO " + chapter, x + 131, y + 70, GOLD);
            List<GameplayNetworking.QuestView> visible = visibleQuests();
            int top = y + 104;
            int rowH = Math.max(48, Math.min(64, (h - 175) / Math.max(1, visible.size())));
            for (int i = 0; i < visible.size(); i++) {
                GameplayNetworking.QuestView q = visible.get(i);
                int yy = top + i * rowH;
                int bg = q.claimed ? 0x8835412F : q.locked ? 0x88302A2D : q.complete ? 0xAA5E4020 : 0xAA352B30;
                c.fill(x + 28, yy, x + w - 28, yy + rowH - 6, bg);
                String mark = q.claimed ? "✓ " : q.locked ? "🔒 " : q.complete ? "★ " : "◇ ";
                label(c, mark + q.name, x + 42, yy + 7, q.complete && !q.claimed ? GOLD : WHITE);
                wrapped(c, q.description, x + 42, yy + 22, w - 360, MUTED, 2);
                String progress = q.progress + " / " + q.goal;
                label(c, progress, x + w - 250, yy + 8, q.complete ? GOLD : PALE);
                int barL = x + w - 250, barR = x + w - 136, barY = yy + 26;
                int fill = (int)((barR - barL) * MathHelper.clamp(q.progress / (double)Math.max(1, q.goal), 0D, 1D));
                c.fill(barL, barY, barR, barY + 7, 0xFF151316);
                c.fill(barL + 1, barY + 1, barL + Math.max(1, fill), barY + 6, q.complete ? GOLD : CORAL);
                label(c, "Premio: " + q.rewardBalance + " " + snapshot.currencySymbol, x + w - 250, yy + 39, GOLD);
            }
            super.render(c, mx, my, delta);
        }
    }

    private static final class ShopScreen extends ChainaScreen {
        int categoryIndex;
        final List<String> categories = new ArrayList<>();

        ShopScreen(Screen parent, GameplayNetworking.Snapshot snapshot) {
            super(Text.literal("Tienda Chaina"), parent, snapshot);
            for (GameplayNetworking.ShopView item : snapshot.shop) {
                String category = item.category == null || item.category.isBlank() ? "general" : item.category;
                if (!categories.contains(category)) categories.add(category);
            }
        }

        @Override protected void init() {
            super.init();
            addDrawableChild(button("← MENÚ", x + 18, y + h - 54, 100, 24, b -> action("open", "menu", "", 1)));
            addDrawableChild(button("‹", x + 28, y + 64, 24, 22, b -> { if (!categories.isEmpty()) { categoryIndex = Math.floorMod(categoryIndex - 1, categories.size()); clearAndInit(); }}));
            addDrawableChild(button("›", x + 210, y + 64, 24, 22, b -> { if (!categories.isEmpty()) { categoryIndex = Math.floorMod(categoryIndex + 1, categories.size()); clearAndInit(); }}));
            List<GameplayNetworking.ShopView> visible = visibleItems();
            int top = y + 105;
            int rowH = Math.max(42, Math.min(56, (h - 178) / Math.max(1, visible.size())));
            for (int i = 0; i < visible.size(); i++) {
                GameplayNetworking.ShopView item = visible.get(i);
                int yy = top + i * rowH;
                addDrawableChild(button("COMPRAR", x + w - 118, yy + 10, 88, 22, b -> action("shop_buy", "shop", item.id, 1)));
            }
        }

        private List<GameplayNetworking.ShopView> visibleItems() {
            if (categories.isEmpty()) return snapshot.shop.stream().limit(7).toList();
            String category = categories.get(Math.min(categoryIndex, categories.size() - 1));
            return snapshot.shop.stream().filter(v -> category.equals(v.category)).limit(7).toList();
        }

        @Override public void render(DrawContext c, int mx, int my, float delta) {
            base(c, "POKÉ MART DE CHAINA", "Compra objetos con " + snapshot.currencyName);
            String category = categories.isEmpty() ? "general" : categories.get(Math.min(categoryIndex, categories.size() - 1));
            c.fill(x + 62, y + 62, x + 200, y + 88, 0xAA3B2429);
            centered(c, category.toUpperCase(Locale.ROOT), x + 131, y + 70, GOLD);
            List<GameplayNetworking.ShopView> visible = visibleItems();
            int top = y + 105;
            int rowH = Math.max(42, Math.min(56, (h - 178) / Math.max(1, visible.size())));
            for (int i = 0; i < visible.size(); i++) {
                GameplayNetworking.ShopView item = visible.get(i);
                int yy = top + i * rowH;
                c.fill(x + 28, yy, x + w - 28, yy + rowH - 6, i % 2 == 0 ? 0xAA342D31 : 0xAA2D282C);
                label(c, item.name, x + 44, yy + 9, WHITE);
                label(c, item.amount + "x · " + item.item, x + 44, yy + 25, MUTED);
                label(c, item.price + " " + snapshot.currencySymbol, x + w - 245, yy + 16, GOLD);
            }
            super.render(c, mx, my, delta);
        }
    }

    private static final class AdminScreen extends ChainaScreen {
        AdminScreen(Screen parent, GameplayNetworking.Snapshot snapshot) { super(Text.literal("Administración Chaina"), parent, snapshot); }

        @Override protected void init() {
            super.init();
            addDrawableChild(button("← MENÚ", x + 18, y + h - 54, 100, 24, b -> action("open", "menu", "", 1)));
            addDrawableChild(button("RECARGAR TODO", x + w - 158, y + h - 54, 128, 24, b -> action("admin_reload", "admin", "", 1)));
            addDrawableChild(button("ACTUALIZAR NPC/SKINS", x + w - 334, y + h - 54, 166, 24, b -> action("admin_npc_refresh", "admin", "", 1)));
        }

        @Override public void render(DrawContext c, int mx, int my, float delta) {
            base(c, "PANEL DE ADMINISTRACIÓN", "Vista general de los sistemas de Chaina Cobblemon");
            if (!snapshot.admin) {
                centered(c, "No tienes permisos para abrir este panel.", x + w / 2, y + 120, CORAL);
                super.render(c, mx, my, delta); return;
            }
            int top = y + 72;
            String[] labels = {"NPCs", "Dungeons", "Trabajos", "Misiones", "Tienda"};
            int[] values = {snapshot.npcCount, snapshot.dungeonCount, snapshot.jobCount, snapshot.questCount, snapshot.shopCount};
            int cellW = (w - 76) / 5;
            for (int i = 0; i < labels.length; i++) {
                int bx = x + 28 + i * cellW;
                c.fill(bx, top, bx + cellW - 8, top + 55, 0xAA392E33);
                centered(c, labels[i], bx + (cellW - 8) / 2, top + 10, SAKURA);
                centered(c, Integer.toString(values[i]), bx + (cellW - 8) / 2, top + 30, GOLD);
            }
            int mid = y + 145;
            int leftX = x + 28, leftW = (w - 70) / 2;
            c.fill(leftX, mid, leftX + leftW, y + h - 72, 0x99231F23);
            c.fill(leftX + leftW + 14, mid, x + w - 28, y + h - 72, 0x99231F23);
            label(c, "NPCs REGISTRADOS", leftX + 12, mid + 12, GOLD);
            int yy = mid + 32;
            for (String line : snapshot.npcs.stream().limit(12).toList()) {
                label(c, "• " + line, leftX + 12, yy, WHITE); yy += 15;
            }
            int rightX = leftX + leftW + 14;
            label(c, "DUNGEONS REGISTRADAS", rightX + 12, mid + 12, GOLD);
            yy = mid + 32;
            for (String line : snapshot.dungeons.stream().limit(12).toList()) {
                label(c, "• " + line, rightX + 12, yy, WHITE); yy += 15;
            }
            label(c, "Los cambios avanzados se guardan en config/chainacobblemon/ y pueden recargarse sin reiniciar.", x + 32, y + h - 88, MUTED);
            super.render(c, mx, my, delta);
        }
    }
}
