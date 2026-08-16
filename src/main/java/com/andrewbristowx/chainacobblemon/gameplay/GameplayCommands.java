package com.andrewbristowx.chainacobblemon.gameplay;

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

public final class GameplayCommands {
    private GameplayCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("hub")
                    .requires(s -> PermissionBridge.check(s, GameplaySystems.HUB_USE, 0))
                    .executes(c -> teleport(c.getSource(), false)));
            dispatcher.register(literal("spawn")
                    .requires(s -> PermissionBridge.check(s, GameplaySystems.HUB_USE, 0))
                    .executes(c -> teleport(c.getSource(), true)));

            dispatcher.register(literal("chaina")
                    .then(literal("balance")
                            .requires(s -> PermissionBridge.check(s, GameplaySystems.ECONOMY_USE, 0))
                            .executes(c -> balance(c.getSource())))
                    .then(literal("pay")
                            .requires(s -> PermissionBridge.check(s, GameplaySystems.ECONOMY_USE, 0))
                            .then(argument("player", EntityArgumentType.player())
                                    .then(argument("amount", LongArgumentType.longArg(1))
                                            .executes(c -> pay(c.getSource(), EntityArgumentType.getPlayer(c, "player"), LongArgumentType.getLong(c, "amount")))))));

            dispatcher.register(literal("chaina")
                    .then(literal("jobs")
                            .requires(s -> PermissionBridge.check(s, GameplaySystems.JOBS_USE, 0))
                            .executes(c -> lines(c.getSource(), GameplaySystems.jobLines(c.getSource().getPlayerOrThrow())))
                            .then(literal("join")
                                    .then(argument("id", StringArgumentType.word())
                                            .executes(c -> message(c.getSource(), GameplaySystems.joinJob(c.getSource().getPlayerOrThrow(), StringArgumentType.getString(c, "id"))))))
                            .then(literal("leave")
                                    .then(argument("id", StringArgumentType.word())
                                            .executes(c -> message(c.getSource(), GameplaySystems.leaveJob(c.getSource().getPlayerOrThrow(), StringArgumentType.getString(c, "id"))))))));

            dispatcher.register(literal("chaina")
                    .then(literal("shop")
                            .requires(s -> PermissionBridge.check(s, GameplaySystems.SHOP_USE, 0))
                            .executes(c -> lines(c.getSource(), GameplaySystems.shopLines()))
                            .then(literal("buy")
                                    .then(argument("id", StringArgumentType.word())
                                            .executes(c -> message(c.getSource(), GameplaySystems.buy(c.getSource().getPlayerOrThrow(), StringArgumentType.getString(c, "id"), 1)))
                                            .then(argument("amount", IntegerArgumentType.integer(1, 64))
                                                    .executes(c -> message(c.getSource(), GameplaySystems.buy(c.getSource().getPlayerOrThrow(), StringArgumentType.getString(c, "id"), IntegerArgumentType.getInteger(c, "amount")))))))));

            dispatcher.register(literal("chaina")
                    .then(literal("quests")
                            .requires(s -> PermissionBridge.check(s, GameplaySystems.QUESTS_USE, 0))
                            .executes(c -> lines(c.getSource(), GameplaySystems.questLines(c.getSource().getPlayerOrThrow()))))
                    .then(literal("quest")
                            .requires(s -> PermissionBridge.check(s, GameplaySystems.QUESTS_USE, 0))
                            .then(literal("claim")
                                    .then(argument("id", StringArgumentType.word())
                                            .executes(c -> message(c.getSource(), GameplaySystems.claimQuest(c.getSource().getPlayerOrThrow(), StringArgumentType.getString(c, "id"))))))));

            dispatcher.register(literal("chaina")
                    .then(literal("hub")
                            .requires(s -> PermissionBridge.check(s, GameplaySystems.HUB_USE, 0))
                            .executes(c -> teleport(c.getSource(), false))
                            .then(literal("set")
                                    .requires(s -> PermissionBridge.check(s, GameplaySystems.ADMIN, 2))
                                    .executes(c -> { GameplaySystems.setHub(c.getSource().getPlayerOrThrow(), false); return message(c.getSource(), "Hub guardado."); })))
                    .then(literal("spawn")
                            .requires(s -> PermissionBridge.check(s, GameplaySystems.HUB_USE, 0))
                            .executes(c -> teleport(c.getSource(), true))
                            .then(literal("set")
                                    .requires(s -> PermissionBridge.check(s, GameplaySystems.ADMIN, 2))
                                    .executes(c -> { GameplaySystems.setHub(c.getSource().getPlayerOrThrow(), true); return message(c.getSource(), "Spawn guardado."); }))));

            dispatcher.register(literal("chaina")
                    .then(literal("economy")
                            .requires(s -> PermissionBridge.check(s, GameplaySystems.ADMIN, 2))
                            .then(literal("give")
                                    .then(argument("player", EntityArgumentType.player())
                                            .then(argument("amount", LongArgumentType.longArg(1))
                                                    .executes(c -> {
                                                        ServerPlayerEntity p = EntityArgumentType.getPlayer(c, "player");
                                                        long v = GameplaySystems.deposit(p, LongArgumentType.getLong(c, "amount"));
                                                        return message(c.getSource(), "Nuevo saldo de " + p.getName().getString() + ": " + v);
                                                    }))))
                            .then(literal("take")
                                    .then(argument("player", EntityArgumentType.player())
                                            .then(argument("amount", LongArgumentType.longArg(1))
                                                    .executes(c -> {
                                                        ServerPlayerEntity p = EntityArgumentType.getPlayer(c, "player");
                                                        GameplaySystems.setBalance(p, Math.max(0, GameplaySystems.balance(p) - LongArgumentType.getLong(c, "amount")));
                                                        return message(c.getSource(), "Saldo actualizado: " + GameplaySystems.balance(p));
                                                    }))))
                            .then(literal("set")
                                    .then(argument("player", EntityArgumentType.player())
                                            .then(argument("amount", LongArgumentType.longArg(0))
                                                    .executes(c -> {
                                                        ServerPlayerEntity p = EntityArgumentType.getPlayer(c, "player");
                                                        GameplaySystems.setBalance(p, LongArgumentType.getLong(c, "amount"));
                                                        return message(c.getSource(), "Saldo actualizado: " + GameplaySystems.balance(p));
                                                    }))))));

            dispatcher.register(literal("chaina")
                    .then(literal("npc")
                            .requires(s -> PermissionBridge.check(s, GameplaySystems.ADMIN, 2))
                            .then(literal("list").executes(c -> message(c.getSource(), "NPCs: " + String.join(", ", GameplaySystems.npcIds()))))
                            .then(literal("create")
                                    .then(argument("id", StringArgumentType.word())
                                            .then(argument("type", StringArgumentType.word())
                                                    .then(argument("name", StringArgumentType.greedyString())
                                                            .executes(c -> message(c.getSource(), GameplaySystems.createNpc(
                                                                    c.getSource().getPlayerOrThrow(),
                                                                    StringArgumentType.getString(c, "id"),
                                                                    StringArgumentType.getString(c, "type"),
                                                                    StringArgumentType.getString(c, "name"))))))))
                            .then(literal("trainer")
                                    .then(argument("id", StringArgumentType.word())
                                            .then(argument("trainerId", StringArgumentType.word())
                                                    .then(argument("levelCap", IntegerArgumentType.integer(0, 100))
                                                            .executes(c -> message(c.getSource(), GameplaySystems.configureTrainerNpc(
                                                                    StringArgumentType.getString(c, "id"),
                                                                    StringArgumentType.getString(c, "trainerId"),
                                                                    IntegerArgumentType.getInteger(c, "levelCap"))))))))
                            .then(literal("command")
                                    .then(argument("id", StringArgumentType.word())
                                            .then(argument("command", StringArgumentType.greedyString())
                                                    .executes(c -> message(c.getSource(), GameplaySystems.configureCommandNpc(
                                                            StringArgumentType.getString(c, "id"),
                                                            StringArgumentType.getString(c, "command")))))))
                            .then(literal("delete")
                                    .then(argument("id", StringArgumentType.word())
                                            .executes(c -> message(c.getSource(), GameplaySystems.deleteNpc(StringArgumentType.getString(c, "id"))))))));

            dispatcher.register(literal("chaina")
                    .then(literal("trainer")
                            .then(literal("restore")
                                    .requires(s -> PermissionBridge.check(s, GameplaySystems.TRAINER_USE, 0))
                                    .executes(c -> message(c.getSource(), LevelSyncService.restore(c.getSource().getPlayerOrThrow(), "manual", false) ? "Level Sync restaurado." : "No hay Level Sync activo.")))
                            .then(literal("reload")
                                    .requires(s -> PermissionBridge.check(s, GameplaySystems.ADMIN, 2))
                                    .executes(c -> message(c.getSource(), "Entrenadores RCT registrados: " + RCTBridge.reload(c.getSource().getServer()))))));

            dispatcher.register(literal("chaina")
                    .then(literal("dungeon")
                            .requires(s -> PermissionBridge.check(s, GameplaySystems.DUNGEON_USE, 0))
                            .then(literal("list").executes(c -> message(c.getSource(), "Dungeons: " + String.join(", ", GameplaySystems.dungeonIds()))))
                            .then(literal("bind")
                                    .requires(s -> PermissionBridge.check(s, GameplaySystems.ADMIN, 2))
                                    .then(argument("id", StringArgumentType.word())
                                            .then(argument("radius", DoubleArgumentType.doubleArg(4, 512))
                                                    .executes(c -> message(c.getSource(), GameplaySystems.bindDungeon(c.getSource().getPlayerOrThrow(), StringArgumentType.getString(c, "id"), DoubleArgumentType.getDouble(c, "radius")))))))
                            .then(literal("setboss")
                                    .requires(s -> PermissionBridge.check(s, GameplaySystems.ADMIN, 2))
                                    .then(argument("id", StringArgumentType.word())
                                            .then(argument("entity", StringArgumentType.word())
                                                    .executes(c -> message(c.getSource(), GameplaySystems.setDungeonBoss(StringArgumentType.getString(c, "id"), StringArgumentType.getString(c, "entity")))))))
                            .then(literal("settrainer")
                                    .requires(s -> PermissionBridge.check(s, GameplaySystems.ADMIN, 2))
                                    .then(argument("id", StringArgumentType.word())
                                            .then(argument("trainer", StringArgumentType.word())
                                                    .executes(c -> message(c.getSource(), GameplaySystems.setDungeonTrainer(StringArgumentType.getString(c, "id"), StringArgumentType.getString(c, "trainer")))))))
                            .then(literal("reward")
                                    .requires(s -> PermissionBridge.check(s, GameplaySystems.ADMIN, 2))
                                    .then(argument("id", StringArgumentType.word())
                                            .then(argument("amount", LongArgumentType.longArg(0))
                                                    .executes(c -> message(c.getSource(), GameplaySystems.setDungeonReward(StringArgumentType.getString(c, "id"), LongArgumentType.getLong(c, "amount")))))))
                            .then(literal("complete")
                                    .requires(s -> PermissionBridge.check(s, GameplaySystems.ADMIN, 2))
                                    .then(argument("id", StringArgumentType.word())
                                            .executes(c -> message(c.getSource(), GameplaySystems.completeDungeon(c.getSource().getPlayerOrThrow(), StringArgumentType.getString(c, "id"), "admin"))))))));

            dispatcher.register(literal("chaina")
                    .then(literal("gameplay")
                            .requires(s -> PermissionBridge.check(s, GameplaySystems.ADMIN, 2))
                            .then(literal("reload")
                                    .executes(c -> { GameplaySystems.reload(); return message(c.getSource(), "Gameplay config recargada. Casino sigue excluido."); }))));
        });
    }

    private static int balance(ServerCommandSource source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayerEntity p = source.getPlayerOrThrow();
        return message(source, "Saldo: " + GameplaySystems.balance(p) + " " + GameplaySystems.config().economy.symbol + " (" + GameplaySystems.config().economy.name + ")");
    }

    private static int pay(ServerCommandSource source, ServerPlayerEntity target, long amount) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayerEntity from = source.getPlayerOrThrow();
        if (GameplaySystems.pay(from, target, amount)) {
            target.sendMessage(Text.literal("§aRecibiste " + amount + " " + GameplaySystems.config().economy.symbol + " de " + from.getName().getString()), false);
            return message(source, "Pago realizado.");
        }
        return message(source, "No se pudo realizar el pago. Revisa tu saldo.");
    }

    private static int teleport(ServerCommandSource source, boolean spawn) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return message(source, GameplaySystems.teleport(source.getPlayerOrThrow(), spawn) ? (spawn ? "Teletransportado al spawn." : "Teletransportado al hub.") : "Ese punto aún no está configurado.");
    }

    private static int lines(ServerCommandSource source, java.util.List<String> lines) {
        for (String line : lines) source.sendFeedback(() -> Text.literal(line), false);
        return 1;
    }

    private static int message(ServerCommandSource source, String value) {
        source.sendFeedback(() -> Text.literal(value), false);
        return 1;
    }
}
