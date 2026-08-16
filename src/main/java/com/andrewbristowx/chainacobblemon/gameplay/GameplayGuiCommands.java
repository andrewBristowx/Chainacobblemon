package com.andrewbristowx.chainacobblemon.gameplay;

import com.andrewbristowx.chainacobblemon.integration.PermissionBridge;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import static net.minecraft.server.command.CommandManager.literal;

public final class GameplayGuiCommands {
    private GameplayGuiCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("chaina")
                    .executes(c -> { GameplayNetworking.open(c.getSource().getPlayerOrThrow(), "menu"); return 1; })
                    .then(literal("menu").executes(c -> { GameplayNetworking.open(c.getSource().getPlayerOrThrow(), "menu"); return 1; }))
                    .then(literal("interfaz").executes(c -> { GameplayNetworking.open(c.getSource().getPlayerOrThrow(), "menu"); return 1; }))
                    .then(literal("admin")
                            .requires(s -> PermissionBridge.check(s, GameplaySystems.ADMIN, 2))
                            .executes(c -> { GameplayNetworking.open(c.getSource().getPlayerOrThrow(), "admin"); return 1; })));

            dispatcher.register(literal("misiones")
                    .requires(s -> PermissionBridge.check(s, GameplaySystems.QUESTS_USE, 0))
                    .executes(c -> { GameplayNetworking.open(c.getSource().getPlayerOrThrow(), "quests"); return 1; }));
            dispatcher.register(literal("trabajos")
                    .requires(s -> PermissionBridge.check(s, GameplaySystems.JOBS_USE, 0))
                    .executes(c -> { GameplayNetworking.open(c.getSource().getPlayerOrThrow(), "jobs"); return 1; }));
            dispatcher.register(literal("tienda")
                    .requires(s -> PermissionBridge.check(s, GameplaySystems.SHOP_USE, 0))
                    .executes(c -> { GameplayNetworking.open(c.getSource().getPlayerOrThrow(), "shop"); return 1; }));
            dispatcher.register(literal("adminchaina")
                    .requires(s -> PermissionBridge.check(s, GameplaySystems.ADMIN, 2))
                    .executes(c -> { GameplayNetworking.open(c.getSource().getPlayerOrThrow(), "admin"); return 1; }));
        });
    }
}
