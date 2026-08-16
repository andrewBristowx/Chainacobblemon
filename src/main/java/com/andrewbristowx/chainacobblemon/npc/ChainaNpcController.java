package com.andrewbristowx.chainacobblemon.npc;

import com.andrewbristowx.chainacobblemon.Chainacobblemon;
import com.andrewbristowx.chainacobblemon.gameplay.GameplayConfig;
import com.andrewbristowx.chainacobblemon.gameplay.GameplaySystems;
import com.andrewbristowx.chainacobblemon.registry.ChainaRegistries;
import com.andrewbristowx.chainacobblemon.visual.ChainaVisualAssetService;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Locale;
import java.util.UUID;

/** Convierte automáticamente los NPC genéricos del backend en NPCs con modelo de jugador y skin por ID. */
public final class ChainaNpcController {
    private static long ticks;
    private ChainaNpcController() {}

    public static void initialize() {
        ChainaVisualAssetService.INSTANCE.initialize();
        ServerLifecycleEvents.SERVER_STARTED.register(ChainaNpcController::refresh);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                ChainaVisualAssetService.INSTANCE.syncAll(handler.player));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++ticks % 200L == 0L) refreshEntities(server);
            if (ticks % 1200L == 0L) ChainaVisualAssetService.INSTANCE.broadcast(server);
        });
    }

    public static void refresh(MinecraftServer server) {
        if (server == null) return;
        refreshEntities(server);
        ChainaVisualAssetService.INSTANCE.broadcast(server);
    }

    private static void refreshEntities(MinecraftServer server) {
        GameplayConfig cfg = GameplaySystems.config();
        if (cfg == null || cfg.npcs == null) return;
        boolean changed = false;
        for (var entry : cfg.npcs.entrySet()) {
            String id = safeId(entry.getKey());
            GameplayConfig.Npc npc = entry.getValue();
            if (id.isBlank() || npc == null || npc.position == null) continue;
            ChainaVisualAssetService.INSTANCE.ensureNpcFolder(id);
            ServerWorld world = world(server, npc.position.dimension);
            if (world == null) continue;
            Entity existing = entity(world, npc.entityUuid);
            boolean slim = "slim".equalsIgnoreCase(npc.skinModel);
            if (existing instanceof ChainaNpcEntity chaina) {
                apply(chaina, id, npc);
                boolean wrongType = slim ? chaina.getType() != ChainaRegistries.CHAINA_NPC_SLIM : chaina.getType() != ChainaRegistries.CHAINA_NPC;
                if (!wrongType) continue;
                chaina.discard();
            } else if (existing != null) {
                existing.discard();
            }
            ChainaNpcEntity entity = new ChainaNpcEntity(slim ? ChainaRegistries.CHAINA_NPC_SLIM : ChainaRegistries.CHAINA_NPC, world);
            entity.refreshPositionAndAngles(npc.position.x, npc.position.y, npc.position.z, npc.position.yaw, npc.position.pitch);
            apply(entity, id, npc);
            if (world.spawnEntity(entity)) {
                npc.entityUuid = entity.getUuidAsString();
                changed = true;
            }
        }
        if (changed) GameplaySystems.saveConfig();
    }

    private static void apply(ChainaNpcEntity entity, String id, GameplayConfig.Npc npc) {
        entity.setNpcId(id);
        entity.setAiDisabled(true);
        entity.setInvulnerable(true);
        entity.setSilent(true);
        entity.setPersistent();
        entity.setCustomName(Text.literal(npc.displayName == null || npc.displayName.isBlank() ? "NPC Chaina" : npc.displayName));
        entity.setCustomNameVisible(true);
    }

    private static Entity entity(ServerWorld world, String uuid) {
        try { return uuid == null || uuid.isBlank() ? null : world.getEntity(UUID.fromString(uuid)); }
        catch (Exception ignored) { return null; }
    }

    private static ServerWorld world(MinecraftServer server, String id) {
        try {
            Identifier target = Identifier.of(id);
            for (ServerWorld world : server.getWorlds()) if (world.getRegistryKey().getValue().equals(target)) return world;
        } catch (Exception ignored) { }
        return null;
    }

    private static String safeId(String value) {
        if (value == null) return "";
        String id = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "");
        return id.length() > 32 ? id.substring(0, 32) : id;
    }
}
