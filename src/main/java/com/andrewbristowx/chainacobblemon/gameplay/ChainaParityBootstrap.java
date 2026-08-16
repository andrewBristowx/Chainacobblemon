package com.andrewbristowx.chainacobblemon.gameplay;

import com.andrewbristowx.chainacobblemon.npc.ChainaNpcController;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.util.math.BlockPos;

/** Migración no destructiva: conserva config existente y agrega módulos/entradas nuevas que falten. */
public final class ChainaParityBootstrap {
    private ChainaParityBootstrap() {}

    public static void initialize() {
        mergeDefaults();
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ensureServiceNpcs(server);
            ChainaNpcController.refresh(server);
        });
    }

    private static void mergeDefaults() {
        GameplayConfig cfg = GameplaySystems.config();
        GameplayConfig defaults = GameplayConfig.defaults();
        if (cfg == null) return;
        defaults.jobs.forEach(cfg.jobs::putIfAbsent);
        defaults.shop.forEach(cfg.shop::putIfAbsent);
        defaults.quests.forEach(cfg.quests::putIfAbsent);
        for (var entry : cfg.jobs.entrySet()) {
            GameplayConfig.Job value = entry.getValue();
            GameplayConfig.Job fallback = defaults.jobs.get(entry.getKey());
            if (value != null && (value.description == null || value.description.isBlank()) && fallback != null) value.description = fallback.description;
        }
        for (var entry : cfg.shop.entrySet()) {
            GameplayConfig.ShopEntry value = entry.getValue();
            GameplayConfig.ShopEntry fallback = defaults.shop.get(entry.getKey());
            if (value != null && (value.category == null || value.category.isBlank())) value.category = fallback == null ? "General" : fallback.category;
        }
        for (var entry : cfg.quests.entrySet()) {
            GameplayConfig.Quest value = entry.getValue();
            GameplayConfig.Quest fallback = defaults.quests.get(entry.getKey());
            if (value == null) continue;
            if ((value.description == null || value.description.isBlank()) && fallback != null) value.description = fallback.description;
            if (value.chapter == null || value.chapter.isBlank()) value.chapter = fallback == null ? "Secundarias" : fallback.chapter;
            if (value.chapterTitle == null || value.chapterTitle.isBlank()) value.chapterTitle = fallback == null ? "Misiones adicionales" : fallback.chapterTitle;
            if (value.track == null || value.track.isBlank()) value.track = fallback == null ? "secundaria" : fallback.track;
        }
        GameplaySystems.saveConfig();
    }

    private static void ensureServiceNpcs(net.minecraft.server.MinecraftServer server) {
        GameplayConfig cfg = GameplaySystems.config();
        if (cfg == null || server == null) return;
        boolean changed = false;
        BlockPos spawn = server.getOverworld().getSpawnPos();
        String dimension = server.getOverworld().getRegistryKey().getValue().toString();
        if (!cfg.npcs.containsKey("enfermera")) {
            GameplayConfig.Npc nurse = new GameplayConfig.Npc();
            nurse.type = "nurse";
            nurse.displayName = "Enfermera del Festival";
            nurse.dialogue = "¡Bienvenido! Puedo curar a todo tu equipo Pokémon.";
            nurse.skinModel = "slim";
            nurse.position = new GameplayConfig.Point(dimension, spawn.getX() + 2.5, spawn.getY(), spawn.getZ() + 0.5, 90F, 0F);
            cfg.npcs.put("enfermera", nurse);
            changed = true;
        }
        if (!cfg.npcs.containsKey("vendedor")) {
            GameplayConfig.Npc shop = new GameplayConfig.Npc();
            shop.type = "shop";
            shop.displayName = "Vendedor del Festival";
            shop.dialogue = "Tengo suministros para tu aventura.";
            shop.skinModel = "wide";
            shop.position = new GameplayConfig.Point(dimension, spawn.getX() - 2.5, spawn.getY(), spawn.getZ() + 0.5, -90F, 0F);
            cfg.npcs.put("vendedor", shop);
            changed = true;
        }
        if (changed) GameplaySystems.saveConfig();
    }
}
