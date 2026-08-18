package com.andrewbristowx.chainacobblemon.client.twitch;

import com.andrewbristowx.chainacobblemon.twitch.TwitchSnapshot;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmLinkScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public final class TwitchScreen extends Screen {
    private final Screen parent;
    private TwitchSnapshot snapshot;

    TwitchScreen(Screen parent, TwitchSnapshot snapshot) {
        super(Text.literal("Chaina × Twitch"));
        this.parent = parent;
        this.snapshot = snapshot;
    }

    void update(TwitchSnapshot next) {
        this.snapshot = next;
        clearChildren();
        init();
    }

    @Override
    protected void init() {
        int panelW = Math.min(390, width - 30);
        int x = (width - panelW) / 2;
        int y = Math.max(35, height / 2 - 135);
        int buttonY = y + 170;
        int gap = 6;
        int third = (panelW - gap * 2) / 3;
        int half = (panelW - gap) / 2;

        String primary = snapshot.linked ? "Sincronizar" : "Vincular Twitch";
        String action = snapshot.linked ? "sync" : "link";
        addDrawableChild(ButtonWidget.builder(Text.literal(primary), button -> TwitchClient.action(action))
                .dimensions(x, buttonY, third, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Desvincular"), button -> TwitchClient.action("unlink"))
                .dimensions(x + third + gap, buttonY, third, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Actualizar"), button -> TwitchClient.action("status"))
                .dimensions(x + (third + gap) * 2, buttonY, third, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("§dVer stream"), button -> openStream())
                .dimensions(x, buttonY + 27, half, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Copiar link"), button -> copyStreamLink())
                .dimensions(x + half + gap, buttonY + 27, half, 20).build());

        int nextY = buttonY + 54;
        if (snapshot.linkUrl != null && !snapshot.linkUrl.isBlank()) {
            addDrawableChild(ButtonWidget.builder(Text.literal("Copiar enlace de vinculacion"), button -> {
                if (client != null) client.keyboard.setClipboard(snapshot.linkUrl);
            }).dimensions(x, nextY, panelW, 20).build());
            nextY += 27;
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("Cerrar"), button -> close())
                .dimensions(x, nextY, panelW, 20).build());
    }

    private String streamUrl() {
        String broadcaster = snapshot.broadcaster == null || snapshot.broadcaster.isBlank() ? "chainavt" : snapshot.broadcaster;
        return "https://twitch.tv/" + broadcaster;
    }

    private void openStream() {
        if (client == null) return;
        ConfirmLinkScreen.open(this, streamUrl(), false);
    }

    private void copyStreamLink() {
        if (client == null) return;
        client.keyboard.setClipboard(streamUrl());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Do not call renderBackground(): in 1.21.1 it applies the blurred menu background.
        // Keep the world readable and only dim it very slightly behind our own panel.
        context.fill(0, 0, width, height, 0x22000000);

        int panelW = Math.min(390, width - 30);
        int x = (width - panelW) / 2;
        int y = Math.max(35, height / 2 - 135);
        int panelH = snapshot.linkUrl != null && !snapshot.linkUrl.isBlank() ? 302 : 275;
        context.fill(x - 10, y - 14, x + panelW + 10, y + panelH, 0xE6191020);
        context.fill(x - 10, y - 14, x + panelW + 10, y - 10, 0xFFFF72B6);
        context.drawCenteredTextWithShadow(textRenderer, "§d§lCHAiNA × TWITCH", width / 2, y, 0xFFFF83C4);
        context.drawCenteredTextWithShadow(textRenderer,
                snapshot.channelOnline ? "§c● EN DIRECTO §f@" + snapshot.broadcaster : "§7● Offline §f@" + snapshot.broadcaster,
                width / 2, y + 22, 0xFFFFFFFF);

        context.drawTextWithShadow(textRenderer, "§7Modo: §f" + snapshot.mode, x, y + 48, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, "§7Cuenta: §f" + (snapshot.linked ? "@" + snapshot.twitchLogin : "No vinculada"), x, y + 64, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, "§7Suscripcion: §f" + tierText(snapshot.tier), x, y + 80, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, "§7Rango Twitch: §d" + snapshot.rankLabel, x, y + 96, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, "§7Ultima sync: §f" + snapshot.lastSync, x, y + 112, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, "§7Canal: §d§n" + streamUrl().replace("https://", ""), x, y + 128, 0xFFFFFFFF);
        if (snapshot.linkCode != null && !snapshot.linkCode.isBlank()) {
            context.drawTextWithShadow(textRenderer, "§7Codigo: §e§l" + snapshot.linkCode, x, y + 144, 0xFFFFFFFF);
        }
        if (snapshot.message != null && !snapshot.message.isBlank()) {
            String text = snapshot.message.length() > 64 ? snapshot.message.substring(0, 63) + "…" : snapshot.message;
            context.drawTextWithShadow(textRenderer, "§b" + text, x, y + 160, 0xFFFFFFFF);
        }
        super.render(context, mouseX, mouseY, delta);
    }

    private static String tierText(int tier) {
        return switch (tier) {
            case 3 -> "Tier 3";
            case 2 -> "Tier 2";
            case 1 -> "Tier 1";
            default -> "Sin sub";
        };
    }

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }
}
