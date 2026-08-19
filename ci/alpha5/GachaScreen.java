package com.andrewbristowx.chainacobblemon.client.gacha;

import com.andrewbristowx.chainacobblemon.client.render.PokemonPortraitRenderer;
import com.andrewbristowx.chainacobblemon.gacha.GachaNetworking.GachaView;
import com.andrewbristowx.chainacobblemon.gacha.GachaNetworking.ResultView;
import com.andrewbristowx.chainacobblemon.registry.ModRegistries;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.util.List;

/** Chaina gacha presentation: banner-first stage, readable pity meters and polished x1/x10 actions. */
final class GachaScreen extends Screen {
    private static final int REF_W = 1536;
    private static final int REF_H = 1024;
    private static final long RESULT_STAGGER_MS = 135L;
    private static final long RESULT_POP_MS = 190L;

    private static final int BG = 0xFF1D1E22;
    private static final int PANEL = 0xFF242529;
    private static final int PANEL_2 = 0xFF2B2C31;
    private static final int SIDE = 0xFF362F34;
    private static final int CORAL = 0xFFF9556D;
    private static final int SAKURA = 0xFFFB9AA6;
    private static final int GOLD = 0xFFF6AD4B;
    private static final int CYAN = 0xFF72D7E6;
    private static final int TEXT = 0xFFF5F5F7;
    private static final int MUTED = 0xFFCAD0D8;

    private final Screen parent;
    private final GachaView view;
    private final long resultRevealStartedAt;
    private int panelX, panelY, panelW, panelH;
    private boolean requesting;
    private int soundedResultCount;

    GachaScreen(Screen parent, GachaView view) {
        super(Text.literal("Gasha de Chaina"));
        this.parent = parent;
        this.view = view;
        this.resultRevealStartedAt = Util.getMeasuringTimeMs() + 110L;
    }

    Screen parent() { return parent; }

    @Override
    protected void init() {
        float scale = Math.min((width - 12.0F) / REF_W, (height - 12.0F) / REF_H);
        panelW = Math.max(1, Math.round(REF_W * scale));
        panelH = Math.max(1, Math.round(REF_H * scale));
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        Layout layout = layout();
        addDrawableChild(new Hotspot(panelX + rx(layout.oneX()), panelY + ry(layout.buttonY()),
                rx(layout.buttonWidth()), ry(layout.buttonHeight()), Text.literal("Tirar una vez"), b -> request(1)));
        addDrawableChild(new Hotspot(panelX + rx(layout.tenX()), panelY + ry(layout.buttonY()),
                rx(layout.buttonWidth()), ry(layout.buttonHeight()), Text.literal("Tirar diez veces"), b -> request(10)));
        addDrawableChild(ButtonWidget.builder(Text.literal("×"), button -> close())
                .dimensions(panelX + panelW - rx(55), panelY + ry(22), rx(36), ry(30)).build());
    }

    private void request(int count) {
        if (requesting || isRevealing()) return;
        requesting = true;
        playLocal(SoundEvents.UI_BUTTON_CLICK.value(), count == 10 ? 0.92F : 1.04F);
        GachaClient.pull(view.blockPos(), count);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xE6000000);
        drawFrame(context, mouseX, mouseY);
        drawLabels(context);
        if (view.results() != null && !view.results().isEmpty()) drawResults(context, view.results());
        else drawEmptyStage(context);
        super.render(context, mouseX, mouseY, delta);
    }

    private void drawFrame(DrawContext context, int mouseX, int mouseY) {
        int x = panelX, y = panelY, w = panelW, h = panelH;
        int accent = accentColor();

        context.fill(x, y, x + w, y + h, BG);
        context.fill(x, y, x + w, y + ry(92), CORAL);
        context.fill(x, y + ry(92), x + w, y + ry(100), GOLD);

        fillRef(context, 28, 118, 1138, 785, PANEL);
        outlineRef(context, 28, 118, 1138, 785, 0xFF55434A, 2);
        fillRef(context, 1190, 118, 314, 785, SIDE);
        outlineRef(context, 1190, 118, 314, 785, GOLD, 3);

        fillRef(context, 62, 128, 1070, 62, 0xFF2D2E33);
        outlineRef(context, 62, 128, 1070, 62, accent, 2);

        fillRef(context, 180, 214, 858, 478, PANEL_2);
        outlineRef(context, 180, 214, 858, 478, accent, 3);
        fillRef(context, 206, 240, 806, 44, 0xFF33343A);

        fillRef(context, 1214, 148, 266, 142, 0xFF40363B);
        outlineRef(context, 1214, 148, 266, 142, accent, 2);
        fillRef(context, 1216, 336, 262, 2, 0xFF62525A);
        fillRef(context, 1216, 553, 262, 2, 0xFF62525A);

        drawPityTrack(context, 1230, 446, 234, view.epicPity(), view.epicMaximum(), accent);
        drawPityTrack(context, 1230, 663, 234, view.legendaryPity(), view.legendaryMaximum(), GOLD);

        Layout l = layout();
        drawActionPad(context, l.oneX(), l.buttonY(), l.buttonWidth(), l.buttonHeight(),
                mouseX, mouseY, CORAL, 0xFF3D2E34);
        drawActionPad(context, l.tenX(), l.buttonY(), l.buttonWidth(), l.buttonHeight(),
                mouseX, mouseY, GOLD, 0xFF443A2D);

        sakura(context, panelX + rx(66), panelY + ry(213), SAKURA, GOLD);
        sakura(context, panelX + rx(1090), panelY + ry(655), accent, GOLD);
        sakura(context, panelX + rx(1450), panelY + ry(132), SAKURA, GOLD);
        sakura(context, panelX + rx(1450), panelY + ry(845), CORAL, GOLD);
    }

    private void drawActionPad(DrawContext c, int x, int y, int w, int h, int mouseX, int mouseY, int accent, int idle) {
        int px = panelX + rx(x), py = panelY + ry(y), pw = rx(w), ph = ry(h);
        boolean hover = mouseX >= px && mouseX < px + pw && mouseY >= py && mouseY < py + ph;
        int fill = hover ? accent : idle;
        c.fill(px, py, px + pw, py + ph, fill);
        outline(c, px, py, pw, ph, hover ? TEXT : accent);
        c.fill(px + rx(10), py + ph - ry(8), px + pw - rx(10), py + ph - ry(5), hover ? GOLD : 0xFF6B5960);
    }

    private void drawLabels(DrawContext context) {
        Layout layout = layout();
        int headerX = panelX + rx(768);
        int stageX = panelX + rx(layout.stageX());
        int accent = accentColor();

        drawCentered(context, view.treasure() ? "GASHA DE TESOROS DE CHAINA" : (view.chaina() ? "GASHA ESPECIAL DE CHAINA" : "GASHA ESTÁNDAR DE CHAINA"),
                headerX, panelY + ry(layout.titleY()), TEXT);
        drawCentered(context, trim(view.bannerName(), view.chaina() || view.treasure() ? 34 : 42), stageX,
                panelY + ry(layout.bannerY()), view.treasure() ? 0xFFFFD46D : (view.chaina() ? 0xFFFFC4EE : 0xFFA9F4FF));
        drawCentered(context, "Tiradas acumuladas: " + view.totalPulls(), stageX,
                panelY + ry(layout.totalY()), MUTED);

        int sideX = panelX + rx(layout.sideX());
        drawCentered(context, "SALDO DE TICKETS", sideX, panelY + ry(166), GOLD);
        drawLargeItem(context, ticketStack(), sideX, panelY + ry(222), rx(48));
        drawCentered(context, Long.toString(view.tickets()), sideX, panelY + ry(262), TEXT);

        drawCentered(context, view.treasure() ? "PITY NETHERITA" : "PITY ÉPICO", sideX,
                panelY + ry(layout.epicTitleY()), GOLD);
        drawCentered(context, view.epicPity() + " / " + view.epicMaximum(), sideX,
                panelY + ry(layout.epicValueY()), TEXT);
        drawCentered(context, pityHint(view.epicPity(), view.epicMaximum()), sideX,
                panelY + ry(layout.epicValueY() + 58), MUTED);

        drawCentered(context, view.treasure() ? "PITY CHAINA" : "PITY LEGENDARIO", sideX,
                panelY + ry(layout.legendaryTitleY()), GOLD);
        drawCentered(context, view.legendaryPity() + " / " + view.legendaryMaximum(), sideX,
                panelY + ry(layout.legendaryValueY()), TEXT);
        drawCentered(context, pityHint(view.legendaryPity(), view.legendaryMaximum()), sideX,
                panelY + ry(layout.legendaryValueY() + 58), MUTED);

        drawCentered(context, "TIRAR ×1", panelX + rx(layout.oneCenterX()),
                panelY + ry(layout.buttonTitleY()), TEXT);
        drawCentered(context, "1 TICKET", panelX + rx(layout.oneCenterX()),
                panelY + ry(layout.buttonCostY()), 0xFFFFE0A1);
        drawCentered(context, "TIRAR ×10", panelX + rx(layout.tenCenterX()),
                panelY + ry(layout.buttonTitleY()), 0xFF2F2924);
        drawCentered(context, "10 TICKETS", panelX + rx(layout.tenCenterX()),
                panelY + ry(layout.buttonCostY()), 0xFF4A3823);

        if (requesting) drawCentered(context, "PROCESANDO TIRADA…", stageX,
                panelY + ry(layout.messageY()), accent);
        else if (view.message() != null && !view.message().isBlank())
            drawCentered(context, stripFormatting(trim(view.message(), 54)), stageX,
                    panelY + ry(layout.messageY()), TEXT);
    }

    private void drawResults(DrawContext context, List<ResultView> results) {
        Layout layout = layout();
        int visible = visibleResultCount(results);
        updateRevealSound(results, visible);
        if (visible <= 0) return;

        if (results.size() == 1) {
            ResultView result = results.getFirst();
            int cx = panelX + rx(layout.stageX());
            int border = tierColor(result.tier());
            int cardX = panelX + rx(330), cardY = panelY + ry(300), cardW = rx(560), cardH = ry(348);
            context.fill(cardX, cardY, cardX + cardW, cardY + cardH, 0xEF28292E);
            outline(context, cardX, cardY, cardW, cardH, border);
            context.fill(cardX + rx(18), cardY + ry(18), cardX + cardW - rx(18), cardY + ry(22), border);

            float pop = resultPopScale(0);
            context.getMatrices().push();
            context.getMatrices().translate(cx, panelY + ry(layout.singlePortraitBottomY()), 0.0F);
            context.getMatrices().scale(pop, pop, 1.0F);
            context.getMatrices().translate(-cx, -(panelY + ry(layout.singlePortraitBottomY())), 0.0F);
            int clipLeft = cx - rx(210);
            int clipRight = cx + rx(210);
            int clipTop = panelY + ry(layout.singlePortraitBottomY() - 245);
            int clipBottom = panelY + ry(layout.singleNameY() - 18);
            context.enableScissor(clipLeft, clipTop, clipRight, clipBottom);
            boolean rendered;
            if ("ITEM".equals(result.rewardKind())) {
                context.disableScissor();
                rendered = drawItemReward(context, result, cx, panelY + ry(layout.singlePortraitBottomY() - 82), 7.5F);
            } else {
                rendered = PokemonPortraitRenderer.draw(context, result.speciesId(), cx,
                        panelY + ry(layout.singlePortraitBottomY()), rx(178));
                context.disableScissor();
            }
            if (!rendered) drawCentered(context, "◆", cx,
                    panelY + ry(layout.singlePortraitBottomY() - 70), border);
            context.getMatrices().pop();

            drawCentered(context, trim(result.name(), 32), cx,
                    panelY + ry(layout.singleNameY()), TEXT);
            drawCentered(context, result.tier(), cx,
                    panelY + ry(layout.singleTierY()), border);
            drawCentered(context, "ITEM".equals(result.rewardKind()) ? (result.count() > 1 ? "CANTIDAD ×" + result.count() : "PREMIO OBTENIDO")
                            : "NIVEL " + result.level() + (result.shiny() ? " · SHINY ✦" : ""), cx,
                    panelY + ry(layout.singleLevelY()), TEXT);
            return;
        }

        int columns = 5;
        for (int index = 0; index < Math.min(visible, Math.min(10, results.size())); index++) {
            ResultView result = results.get(index);
            int col = index % columns, row = index / columns;
            int cx = panelX + rx(layout.gridFirstX() + col * layout.gridStepX());
            int top = panelY + ry(layout.gridTopY() + row * layout.gridStepY());
            float pop = resultPopScale(index);
            int cardHalf = rx(78);
            int cardHeight = ry(222);

            context.getMatrices().push();
            context.getMatrices().translate(cx, top + cardHeight / 2.0F, 0.0F);
            context.getMatrices().scale(pop, pop, 1.0F);
            context.getMatrices().translate(-cx, -(top + cardHeight / 2.0F), 0.0F);
            context.fill(cx - cardHalf, top, cx + cardHalf, top + cardHeight, 0xF22D2E32);
            outline(context, cx - cardHalf, top, cardHalf * 2, cardHeight, tierColor(result.tier()));
            context.fill(cx - cardHalf + rx(8), top + ry(8), cx + cardHalf - rx(8), top + ry(11), tierColor(result.tier()));

            int clipLeft = cx - rx(70);
            int clipRight = cx + rx(70);
            int clipTop = top + ry(12);
            int clipBottom = top + ry(143);
            context.enableScissor(clipLeft, clipTop, clipRight, clipBottom);
            boolean rendered;
            if ("ITEM".equals(result.rewardKind())) {
                context.disableScissor();
                rendered = drawItemReward(context, result, cx, top + ry(72), 4.6F);
            } else {
                rendered = PokemonPortraitRenderer.draw(context, result.speciesId(), cx,
                        top + ry(132), rx(92));
                context.disableScissor();
            }
            if (!rendered) drawCentered(context, "◆", cx, top + ry(70), tierColor(result.tier()));
            context.getMatrices().pop();

            drawCentered(context, trim(result.name(), 17), cx, top + ry(156), TEXT);
            drawCentered(context, result.tier(), cx, top + ry(182), tierColor(result.tier()));
            drawCentered(context, "ITEM".equals(result.rewardKind()) ? (result.count() > 1 ? "×" + result.count() : "PREMIO")
                            : "Nv." + result.level() + (result.shiny() ? " ✦" : ""), cx,
                    top + ry(204), TEXT);
        }
    }

    private void drawEmptyStage(DrawContext context) {
        Layout layout = layout();
        int cx = panelX + rx(layout.stageX());
        int accent = accentColor();
        drawCentered(context, "BANNER ACTIVO", cx, panelY + ry(258), accent);
        drawLargeItem(context, ticketStack(), cx, panelY + ry(380), rx(116));
        drawCentered(context, view.treasure() ? "TESOROS Y RECOMPENSAS ESPECIALES" : (view.chaina() ? "EVENTO ESPECIAL DE CHAINA" : "RECOMPENSAS DEL GASHA ESTÁNDAR"),
                cx, panelY + ry(502), TEXT);
        drawCentered(context, "Selecciona una tirada para abrir la cápsula", cx,
                panelY + ry(layout.emptyTitleY()), 0xFFFFD9DF);
        drawCentered(context, "El resultado y el pity los decide el servidor", cx,
                panelY + ry(layout.emptyTextY()), MUTED);
        drawSparkle(context, cx - rx(185), panelY + ry(390), accent);
        drawSparkle(context, cx + rx(190), panelY + ry(338), GOLD);
    }

    private Layout layout() {
        return new Layout(768, 43, 143, 168,
                1347, 382, 414, 599, 631,
                610, 748, 503, 563, 594, 624,
                205, 205, 252, 248,
                242, 654, 320, 92, 813,
                402, 814, 836, 872,
                548, 580);
    }

    private ItemStack ticketStack() {
        if (view.treasure()) return new ItemStack(ModRegistries.TREASURE_GACHA_TICKET);
        if (view.chaina()) return new ItemStack(ModRegistries.CHAINA_SPECIAL_BANNER_TICKET);
        return new ItemStack(ModRegistries.GACHA_TICKET);
    }

    private int accentColor() {
        if (view.treasure()) return GOLD;
        if (view.chaina()) return SAKURA;
        return CYAN;
    }

    private String pityHint(int current, int maximum) {
        if (maximum <= 0) return "";
        int remaining = Math.max(0, maximum - current);
        return remaining == 0 ? "GARANTIZADO" : "Faltan " + remaining;
    }

    private void drawPityTrack(DrawContext c, int x, int y, int w, int current, int maximum, int color) {
        fillRef(c, x, y, w, 14, 0xFF201E20);
        int fill = maximum <= 0 ? 0 : Math.round(w * Math.min(1.0F, Math.max(0.0F, current / (float) maximum)));
        if (fill > 0) fillRef(c, x, y, fill, 14, color);
        outlineRef(c, x, y, w, 14, 0xFF7A6870, 1);
    }

    private int visibleResultCount(List<ResultView> results) {
        if (results == null || results.isEmpty()) return 0;
        long elapsed = Util.getMeasuringTimeMs() - resultRevealStartedAt;
        if (elapsed < 0L) return 0;
        return Math.min(results.size(), 1 + (int) (elapsed / RESULT_STAGGER_MS));
    }

    private float resultPopScale(int index) {
        long elapsed = Util.getMeasuringTimeMs() - resultRevealStartedAt - index * RESULT_STAGGER_MS;
        if (elapsed <= 0L) return 0.82F;
        float progress = Math.min(1.0F, elapsed / (float) RESULT_POP_MS);
        float overshoot = (float) Math.sin(progress * Math.PI) * 0.08F;
        return 0.82F + 0.18F * progress + overshoot;
    }

    private boolean isRevealing() {
        List<ResultView> results = view.results();
        return results != null && !results.isEmpty() && visibleResultCount(results) < results.size();
    }

    private void updateRevealSound(List<ResultView> results, int visible) {
        if (visible <= soundedResultCount || visible <= 0 || client == null) return;
        int index = Math.min(visible, results.size()) - 1;
        ResultView result = results.get(index);
        soundedResultCount = visible;
        switch (result.tier() == null ? "" : result.tier()) {
            case "MYTHICAL", "SPECIAL", "LEGENDARY" ->
                    playLocal(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.02F + index * 0.015F);
            case "EPIC", "RARE" -> playLocal(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.05F + index * 0.02F);
            default -> playLocal(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F + index * 0.025F);
        }
    }

    private void playLocal(SoundEvent sound, float pitch) {
        if (client == null) return;
        client.getSoundManager().play(PositionedSoundInstance.master(sound, Math.min(1.65F, pitch)));
    }

    private boolean drawItemReward(DrawContext context, ResultView result, int centerX, int centerY, float scale) {
        if (result.itemId() == null || result.itemId().isBlank()) return false;
        try {
            Item item = Registries.ITEM.get(Identifier.of(result.itemId()));
            if (item == Items.AIR) return false;
            ItemStack stack = new ItemStack(item, Math.max(1, result.count()));
            context.getMatrices().push();
            context.getMatrices().translate(centerX, centerY, 40.0F);
            context.getMatrices().scale(scale, scale, 1.0F);
            context.drawItem(stack, -8, -8);
            context.getMatrices().pop();
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private void drawLargeItem(DrawContext context, ItemStack stack, int centerX, int centerY, int size) {
        float scale = Math.max(1.0F, size / 16.0F);
        context.getMatrices().push();
        context.getMatrices().translate(centerX - 8.0F * scale, centerY - 8.0F * scale, 80.0F);
        context.getMatrices().scale(scale, scale, 1.0F);
        context.drawItem(stack, 0, 0);
        context.getMatrices().pop();
    }

    private void fillRef(DrawContext c, int x, int y, int w, int h, int color) {
        c.fill(panelX + rx(x), panelY + ry(y), panelX + rx(x + w), panelY + ry(y + h), color);
    }

    private void outlineRef(DrawContext c, int x, int y, int w, int h, int color, int thicknessRef) {
        int px = panelX + rx(x), py = panelY + ry(y), pw = rx(w), ph = ry(h);
        int t = Math.max(1, rx(thicknessRef));
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

    private void drawSparkle(DrawContext c, int x, int y, int color) {
        int s = Math.max(1, rx(4));
        c.fill(x - s, y - 3 * s, x + s, y + 3 * s, color);
        c.fill(x - 3 * s, y - s, x + 3 * s, y + s, color);
    }

    private void drawCentered(DrawContext context, String value, int x, int y, int color) {
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(value == null ? "" : value), x, y, color);
    }

    private void outline(DrawContext c, int x, int y, int w, int h, int color) {
        c.fill(x, y, x + w, y + 2, color);
        c.fill(x, y + h - 2, x + w, y + h, color);
        c.fill(x, y, x + 2, y + h, color);
        c.fill(x + w - 2, y, x + w, y + h, color);
    }

    private int tierColor(String tier) {
        return switch (tier == null ? "" : tier) {
            case "MYTHICAL", "SPECIAL" -> 0xFFFF7AD9;
            case "LEGENDARY" -> 0xFFFFC84A;
            case "EPIC" -> 0xFFC790FF;
            case "RARE" -> 0xFF5FC9FF;
            default -> 0xFFA8F0C6;
        };
    }

    private String trim(String value, int maximum) {
        if (value == null) return "";
        return value.length() <= maximum ? value : value.substring(0, maximum - 1) + "…";
    }

    private String stripFormatting(String value) {
        return value == null ? "" : value.replaceAll("§[0-9A-FK-ORa-fk-or]", "");
    }

    private int rx(int value) { return Math.round(value * panelW / (float) REF_W); }
    private int ry(int value) { return Math.round(value * panelH / (float) REF_H); }

    private record Layout(int titleX, int titleY, int bannerY, int totalY,
                          int sideX, int epicTitleY, int epicValueY, int legendaryTitleY, int legendaryValueY,
                          int stageX, int messageY, int singlePortraitBottomY,
                          int singleNameY, int singleTierY, int singleLevelY,
                          int gridFirstX, int gridStepX, int gridTopY, int gridStepY,
                          int oneX, int tenX, int buttonWidth, int buttonHeight, int buttonY,
                          int oneCenterX, int tenCenterX, int buttonTitleY, int buttonCostY,
                          int emptyTitleY, int emptyTextY) { }

    @Override public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) { }
    @Override public void close() { if (client != null) client.setScreen(parent); }

    private static final class Hotspot extends ButtonWidget {
        private Hotspot(int x, int y, int width, int height, Text narration, PressAction action) {
            super(x, y, width, height, narration, action, DEFAULT_NARRATION_SUPPLIER);
        }
        @Override protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) { }
    }
}
