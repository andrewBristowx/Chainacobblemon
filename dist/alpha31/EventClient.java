package com.andrewbristowx.chainacobblemon.client.events;

import com.andrewbristowx.chainacobblemon.Chainacobblemon;
import com.andrewbristowx.chainacobblemon.events.BeautyRoundSnapshot;
import com.andrewbristowx.chainacobblemon.events.EventHudSnapshot;
import com.andrewbristowx.chainacobblemon.events.FishingGameSnapshot;
import com.andrewbristowx.chainacobblemon.events.EventNetworking.BeautyRoundPayload;
import com.andrewbristowx.chainacobblemon.events.EventNetworking.EventHudPayload;
import com.andrewbristowx.chainacobblemon.events.EventNetworking.FishingGamePayload;
import com.andrewbristowx.chainacobblemon.events.EventNetworking.FishingEntityCleanupPayload;
import com.google.gson.Gson;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class EventClient {
    private static final Gson GSON = new Gson();
    private static final long FISHING_CLEANUP_GRACE_MS = 8_000L;
    private static final Map<UUID, Long> FISHING_POKEMON_CLEANUP = new HashMap<>();
    private static final Map<Integer, Long> FISHING_ENTITY_CLEANUP = new HashMap<>();
    private static volatile EventHudSnapshot hud;

    private EventClient() { }

    public static void initialize() {
        ClientPlayNetworking.registerGlobalReceiver(EventHudPayload.ID, (payload, context) -> context.client().execute(() -> {
            try { hud = GSON.fromJson(payload.json(), EventHudSnapshot.class); }
            catch (Exception e) { Chainacobblemon.LOGGER.warn("Invalid Chaina Event HUD payload", e); }
        }));
        ClientPlayNetworking.registerGlobalReceiver(BeautyRoundPayload.ID, (payload, context) -> context.client().execute(() -> {
            try {
                BeautyRoundSnapshot snapshot = GSON.fromJson(payload.json(), BeautyRoundSnapshot.class);
                if (snapshot != null) MinecraftClient.getInstance().setScreen(new BeautyGameScreen(MinecraftClient.getInstance().currentScreen, snapshot));
            } catch (Exception e) { Chainacobblemon.LOGGER.warn("Invalid Chaina Exhibition payload", e); }
        }));
        ClientPlayNetworking.registerGlobalReceiver(FishingGamePayload.ID, (payload, context) -> context.client().execute(() -> {
            try {
                FishingGameSnapshot snapshot = GSON.fromJson(payload.json(), FishingGameSnapshot.class);
                if (snapshot == null || !snapshot.visible) return;
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.currentScreen instanceof FishingGameScreen screen && screen.sessionId().equals(snapshot.sessionId)) {
                    screen.update(snapshot);
                } else {
                    mc.setScreen(new FishingGameScreen(mc.currentScreen, snapshot));
                }
            } catch (Exception e) { Chainacobblemon.LOGGER.warn("Invalid Chaina Fishing payload", e); }
        }));
        ClientPlayNetworking.registerGlobalReceiver(FishingEntityCleanupPayload.ID, (payload, context) ->
                context.client().execute(() -> registerFishingCleanup(payload.token())));
        ClientTickEvents.END_CLIENT_TICK.register(EventClient::tickFishingCleanup);
        HudRenderCallback.EVENT.register((context, tickCounter) -> renderHud(context));
    }

    private static void registerFishingCleanup(String token) {
        if (token == null || token.isBlank()) return;
        try {
            String[] parts = token.split("\\|", 2);
            UUID pokemonUuid = UUID.fromString(parts[0]);
            int entityId = parts.length > 1 ? Integer.parseInt(parts[1]) : -1;
            long until = System.currentTimeMillis() + FISHING_CLEANUP_GRACE_MS;
            FISHING_POKEMON_CLEANUP.merge(pokemonUuid, until, Math::max);
            if (entityId >= 0) FISHING_ENTITY_CLEANUP.merge(entityId, until, Math::max);
            cleanupFishingVisuals(MinecraftClient.getInstance(), System.currentTimeMillis());
        } catch (RuntimeException exception) {
            Chainacobblemon.LOGGER.warn("Invalid Chaina fishing cleanup payload {}", token, exception);
        }
    }

    private static void tickFishingCleanup(MinecraftClient client) {
        long now = System.currentTimeMillis();
        FISHING_POKEMON_CLEANUP.entrySet().removeIf(e -> e.getValue() <= now);
        FISHING_ENTITY_CLEANUP.entrySet().removeIf(e -> e.getValue() <= now);
        if (client.world == null) {
            FISHING_POKEMON_CLEANUP.clear();
            FISHING_ENTITY_CLEANUP.clear();
            return;
        }
        if (FISHING_POKEMON_CLEANUP.isEmpty() && FISHING_ENTITY_CLEANUP.isEmpty()) return;
        cleanupFishingVisuals(client, now);
    }

    private static void cleanupFishingVisuals(MinecraftClient client, long now) {
        if (client.world == null) return;
        ArrayList<Integer> removeIds = new ArrayList<>();
        for (Entity entity : client.world.getEntities()) {
            Long entityUntil = FISHING_ENTITY_CLEANUP.get(entity.getId());
            boolean remove = entityUntil != null && entityUntil > now;
            if (!remove) {
                UUID pokemonUuid = pokemonUuid(entity);
                Long pokemonUntil = pokemonUuid == null ? null : FISHING_POKEMON_CLEANUP.get(pokemonUuid);
                remove = pokemonUntil != null && pokemonUntil > now;
            }
            if (remove) removeIds.add(entity.getId());
        }
        for (int entityId : removeIds) {
            client.world.removeEntity(entityId, Entity.RemovalReason.DISCARDED);
            FISHING_ENTITY_CLEANUP.remove(entityId);
            Chainacobblemon.LOGGER.debug("Removed client-side ghost fishing entity {}", entityId);
        }
    }

    private static UUID pokemonUuid(Entity entity) {
        if (entity == null) return null;
        try {
            Method getPokemon = entity.getClass().getMethod("getPokemon");
            Object pokemon = getPokemon.invoke(entity);
            if (pokemon == null) return null;
            Method getUuid = pokemon.getClass().getMethod("getUuid");
            Object value = getUuid.invoke(pokemon);
            return value instanceof UUID uuid ? uuid : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static void renderHud(DrawContext c) {
        EventHudSnapshot s = hud;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (s == null || !s.visible || mc.player == null || mc.options.hudHidden) return;
        int w = 214;
        int x = c.getScaledWindowWidth() - w - 12;
        int y = 42;
        int rows = Math.max(1, Math.min(5, s.entries == null ? 0 : s.entries.size()));
        int h = 70 + rows * 30 + 28;
        c.fill(x, y, x+w, y+h, 0xD61C1222);
        c.fill(x, y, x+w, y+3, 0xFFFF79B8);
        c.fill(x, y+h-3, x+w, y+h, 0xFFFFD36A);
        c.drawCenteredTextWithShadow(mc.textRenderer, s.title, x+w/2, y+11, 0xFFFF79B8);
        c.drawCenteredTextWithShadow(mc.textRenderer, s.subtitle + "  §f" + s.timer, x+w/2, y+27, 0xFFD4C4DB);
        int ry = y + 48;
        if (s.entries != null) for (int i=0;i<Math.min(5,s.entries.size());i++) {
            EventHudSnapshot.Entry e=s.entries.get(i);
            int bg=e.self?0xAA51335C:0x99302436;
            c.fill(x+8, ry, x+w-8, ry+25, bg);
            String medal=e.rank==1?"§6🥇":e.rank==2?"§7🥈":e.rank==3?"§c🥉":"§d"+e.rank+".";
            c.drawTextWithShadow(mc.textRenderer, medal+" §f"+e.player, x+13, ry+5, 0xFFF8F4FA);
            int valueW=mc.textRenderer.getWidth(e.value);
            c.drawTextWithShadow(mc.textRenderer, e.value, x+w-13-valueW, ry+5, 0xFFFFD36A);
            String detail=e.detail==null?"":e.detail;
            if(detail.length()>31) detail=detail.substring(0,30)+"…";
            c.drawTextWithShadow(mc.textRenderer, "§7"+detail, x+28, ry+15, 0xFFC9B9CF);
            ry+=30;
        }
        String footer=s.footer==null?"":s.footer;
        if(footer.length()>40) footer=footer.substring(0,39)+"…";
        c.drawCenteredTextWithShadow(mc.textRenderer, "§7"+footer, x+w/2, y+h-18, 0xFFC9B9CF);
    }
}
