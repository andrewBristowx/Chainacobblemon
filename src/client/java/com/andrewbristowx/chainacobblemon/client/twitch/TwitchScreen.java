package com.andrewbristowx.chainacobblemon.client.twitch;

import com.andrewbristowx.chainacobblemon.twitch.TwitchSnapshot;
import net.minecraft.client.gui.DrawContext;
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
        int y = Math.max(35, height / 2 - 120);
        int buttonY = y + 170;
        int gap = 6;
        int third = (panelW - gap * 2) / 3;

        String primary = snapshot.linked ? "Sincronizar" : "Vincular Twitch";
        String action = snapshot.linked ? "sync" : "link";
        addDrawableChild(ButtonWidget.builder(Text.literal(primary), button -> TwitchClient.action(action))
                .dimensions(x, buttonY, third, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Desvincular"), button -> TwitchClient.action("unlink"))
                .dimensions(x + third + gap, buttonY, third, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Actualizar"), button -> TwitchClient.action("status"))
                .dimensions(x + (third + gap) * 2, buttonY, third, 20).build());

        if (snapshot.linkUrl != null && !snapshot.linkUrl.isBlank()) {
            addDrawableChild(ButtonWidget.builder(Text.literal("Copiar enlace de vinculacion"), button -> {
                if (client != null) client.keyboard.setClipboard(snapshot.linkUrl);
            }).dimensions(x, buttonY + 27, panelW, 20).build());
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("Cerrar"), button -> close())
                .dimensions(x, buttonY + (snapshot.linkUrl != null && !snapshot.linkUrl.isBlank() ? 54 : 27), panelW, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        int panelW = Math.min(390, width - 30);
        int x = (width - panelW) / 2;
        int y = Math.max(35, height / 2 - 120);
        int panelH = snapshot.linkUrl != null && !snapshot.linkUrl.isBlank() ? 275 : 248;
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
        if (snapshot.linkCode != null && !snapshot.linkCode.isBlank()) {
            context.drawTextWithShadow(textRenderer, "§7Codigo: §e§l" + snapshot.linkCode, x, y + 132, 0xFFFFFFFF);
        }
        if (snapshot.message != null && !snapshot.message.isBlank()) {
            String text = snapshot.message.length() > 64 ? snapshot.message.substring(0, 63) + "…" : snapshot.message;
            context.drawTextWithShadow(textRenderer, "§b" + text, x, y + 150, 0xFFFFFFFF);
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
