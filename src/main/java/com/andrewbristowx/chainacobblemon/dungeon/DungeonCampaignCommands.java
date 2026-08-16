package com.andrewbristowx.chainacobblemon.dungeon;

import com.andrewbristowx.chainacobblemon.gameplay.GameplaySystems;
import com.andrewbristowx.chainacobblemon.integration.PermissionBridge;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class DungeonCampaignCommands {
    private static boolean initialized;
    private DungeonCampaignCommands() {}

    public static synchronized void register() {
        if (initialized) return;
        initialized = true;
        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) -> {
            dispatcher.register(literal("mazmorras")
                    .requires(s -> PermissionBridge.check(s, GameplaySystems.DUNGEON_USE, 0))
                    .executes(c -> lines(c.getSource(), DungeonCampaignService.list(c.getSource().getPlayerOrThrow())))
                    .then(literal("estado").executes(c -> message(c.getSource(), DungeonCampaignService.status(c.getSource().getPlayerOrThrow())))));

            var campaign = literal("campaign")
                    .requires(s -> PermissionBridge.check(s, GameplaySystems.DUNGEON_USE, 0))
                    .then(literal("list").executes(c -> lines(c.getSource(), DungeonCampaignService.list(c.getSource().getPlayerOrThrow()))))
                    .then(literal("status").executes(c -> message(c.getSource(), DungeonCampaignService.status(c.getSource().getPlayerOrThrow()))))
                    .then(literal("start")
                            .requires(s -> PermissionBridge.check(s, GameplaySystems.ADMIN, 2))
                            .then(argument("id", StringArgumentType.word())
                                    .executes(c -> message(c.getSource(), DungeonCampaignService.forceStart(c.getSource().getPlayerOrThrow(), StringArgumentType.getString(c, "id"))))))
                    .then(literal("reset")
                            .requires(s -> PermissionBridge.check(s, GameplaySystems.ADMIN, 2))
                            .then(argument("player", EntityArgumentType.player())
                                    .executes(c -> message(c.getSource(), DungeonCampaignService.resetSession(EntityArgumentType.getPlayer(c, "player"), true, "admin")))))
                    .then(literal("reload")
                            .requires(s -> PermissionBridge.check(s, GameplaySystems.ADMIN, 2))
                            .executes(c -> { DungeonCampaignService.reload(); return message(c.getSource(), "Dungeon campaigns recargadas."); }))
                    .then(literal("bind")
                            .requires(s -> PermissionBridge.check(s, GameplaySystems.ADMIN, 2))
                            .then(argument("id", StringArgumentType.word())
                                    .then(argument("radius", DoubleArgumentType.doubleArg(4, 512))
                                            .executes(c -> message(c.getSource(), DungeonCampaignService.bind(c.getSource().getPlayerOrThrow(), StringArgumentType.getString(c, "id"), DoubleArgumentType.getDouble(c, "radius")))))))
                    .then(literal("stage")
                            .requires(s -> PermissionBridge.check(s, GameplaySystems.ADMIN, 2))
                            .then(literal("add")
                                    .then(argument("dungeon", StringArgumentType.word())
                                            .then(argument("stage", StringArgumentType.word())
                                                    .then(argument("action", StringArgumentType.word())
                                                            .then(argument("goal", IntegerArgumentType.integer(1, 100000))
                                                                    .then(argument("match", StringArgumentType.string())
                                                                            .executes(c -> message(c.getSource(), DungeonCampaignService.addStage(
                                                                                    StringArgumentType.getString(c, "dungeon"),
                                                                                    StringArgumentType.getString(c, "stage"),
                                                                                    StringArgumentType.getString(c, "action"),
                                                                                    IntegerArgumentType.getInteger(c, "goal"),
                                                                                    StringArgumentType.getString(c, "match")))))))))))
                    .then(literal("reward")
                            .requires(s -> PermissionBridge.check(s, GameplaySystems.ADMIN, 2))
                            .then(argument("dungeon", StringArgumentType.word())
                                    .then(argument("balance", LongArgumentType.longArg(0))
                                            .then(argument("passXp", IntegerArgumentType.integer(0))
                                                    .then(argument("standardRolls", IntegerArgumentType.integer(0))
                                                            .then(argument("chainaRolls", IntegerArgumentType.integer(0))
                                                                    .executes(c -> message(c.getSource(), DungeonCampaignService.setRewards(
                                                                            StringArgumentType.getString(c, "dungeon"),
                                                                            LongArgumentType.getLong(c, "balance"),
                                                                            IntegerArgumentType.getInteger(c, "passXp"),
                                                                            IntegerArgumentType.getInteger(c, "standardRolls"),
                                                                            IntegerArgumentType.getInteger(c, "chainaRolls"))))))))));

            dispatcher.register(literal("chaina").then(literal("dungeon").then(campaign)));
        });
    }

    private static int lines(ServerCommandSource source, java.util.List<String> values) {
        for (String value : values) source.sendFeedback(() -> Text.literal(value), false);
        return 1;
    }
    private static int message(ServerCommandSource source, String value) {
        source.sendFeedback(() -> Text.literal(value), false);
        return 1;
    }
}
