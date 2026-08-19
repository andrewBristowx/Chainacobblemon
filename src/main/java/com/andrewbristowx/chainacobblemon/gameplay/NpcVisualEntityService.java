package com.andrewbristowx.chainacobblemon.gameplay;

import com.andrewbristowx.chainacobblemon.Chainacobblemon;
import com.andrewbristowx.chainacobblemon.registry.ChainaRegistries;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.UUID;

/** Keeps managed NPCs on the correct skinnable wide/slim Chaina entity type. */
public final class NpcVisualEntityService {
    private static int ticks;
    private static boolean initialized;
    private NpcVisualEntityService() {}

    public static synchronized void initialize() {
        if (initialized) return;
        initialized = true;
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++ticks < 100) return;
            ticks = 0;
            reconcile(server);
        });
    }

    private static void reconcile(MinecraftServer server) {
        if (GameplaySystems.config() == null) return;
        boolean changed = false;
        for (var entry : GameplaySystems.config().npcs.entrySet()) {
            GameplayConfig.Npc npc = entry.getValue();
            if (npc == null || npc.position == null || npc.entityUuid == null || npc.entityUuid.isBlank()) continue;
            ServerWorld world = world(server, npc.position.dimension);
            if (world == null) continue;
            Entity old;
            try { old = world.getEntity(UUID.fromString(npc.entityUuid)); } catch (Exception ignored) { continue; }
            if (old == null || old.isRemoved()) continue;

            EntityType<ChainaNpcEntity> desired = npc.slim ? ChainaRegistries.CHAINA_NPC_SLIM : ChainaRegistries.CHAINA_NPC;
            if (old instanceof ChainaNpcEntity && old.getType() == desired) continue;
            if (!(old instanceof VillagerEntity)) continue;

            ChainaNpcEntity replacement = new ChainaNpcEntity(desired, world);
            replacement.refreshPositionAndAngles(old.getX(), old.getY(), old.getZ(), old.getYaw(), old.getPitch());
            replacement.setAiDisabled(true);
            replacement.setInvulnerable(true);
            replacement.setSilent(true);
            replacement.setPersistent();
            replacement.setCustomName(Text.literal(npc.displayName == null || npc.displayName.isBlank() ? "NPC Chaina" : npc.displayName));
            replacement.setCustomNameVisible(true);
            if (!world.spawnEntity(replacement)) continue;
            old.discard();
            npc.entityUuid = replacement.getUuidAsString();
            changed = true;
        }
        if (changed) {
            GameplaySystems.saveConfig();
            NpcSkinNetworking.broadcastNpcMap();
            Chainacobblemon.LOGGER.info("NPCs Chaina reconciliados con su modelo wide/slim configurado");
        }
    }

    private static ServerWorld world(MinecraftServer server, String id) {
        if (id == null) return null;
        try {
            Identifier identifier = Identifier.of(id);
            for (ServerWorld world : server.getWorlds()) if (world.getRegistryKey().getValue().equals(identifier)) return world;
        } catch (Exception ignored) {}
        return null;
    }
}
