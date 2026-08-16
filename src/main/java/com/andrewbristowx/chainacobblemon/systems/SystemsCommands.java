package com.andrewbristowx.chainacobblemon.systems;

import com.andrewbristowx.chainacobblemon.integration.PermissionBridge;
import com.andrewbristowx.chainacobblemon.registry.ChainaRegistries;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class SystemsCommands {
    public static final String GACHA_USE = "chainacobblemon.gacha.use";
    public static final String DAILY_USE = "chainacobblemon.daily.use";
    public static final String PASS_USE = "chainacobblemon.pass.use";
    public static final String ADMIN = "chainacobblemon.systems.admin";

    private SystemsCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(literal("chaina")
                        .then(literal("gacha")
                                .requires(source -> PermissionBridge.check(source, GACHA_USE, 0))
                                .executes(context -> openGacha(context.getSource().getPlayerOrThrow(), "standard"))
                                .then(literal("standard").executes(context -> openGacha(context.getSource().getPlayerOrThrow(), "standard")))
                                .then(literal("chaina").executes(context -> openGacha(context.getSource().getPlayerOrThrow(), "chaina"))))
                        .then(literal("daily")
                                .requires(source -> PermissionBridge.check(source, DAILY_USE, 0))
                                .executes(context -> { SystemsNetworking.openDaily(context.getSource().getPlayerOrThrow(), ""); return 1; })
                                .then(literal("reset").requires(source -> PermissionBridge.check(source, ADMIN, 2)).executes(context -> {
                                    ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                                    ChainaSystems.daily().reset(player.getUuid());
                                    context.getSource().sendFeedback(() -> Text.literal("Login diario reiniciado para pruebas."), false);
                                    return 1;
                                })))
                        .then(literal("pass")
                                .requires(source -> PermissionBridge.check(source, PASS_USE, 0))
                                .executes(context -> { SystemsNetworking.openPass(context.getSource().getPlayerOrThrow(), -1, ""); return 1; })
                                .then(literal("addxp").requires(source -> PermissionBridge.check(source, ADMIN, 2))
                                        .then(argument("amount", IntegerArgumentType.integer(1, 1_000_000)).executes(context -> {
                                            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                                            int amount = IntegerArgumentType.getInteger(context, "amount");
                                            long added = ChainaSystems.pass().addXp(player, amount, "admin");
                                            context.getSource().sendFeedback(() -> Text.literal("Pase: +" + added + " XP."), false);
                                            return added > 0 ? 1 : 0;
                                        })))
                        )
                        .then(literal("systems").requires(source -> PermissionBridge.check(source, ADMIN, 2))
                                .then(literal("giveticket")
                                        .then(argument("type", StringArgumentType.word())
                                                .then(argument("amount", IntegerArgumentType.integer(1, 640)).executes(context -> {
                                                    ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                                                    String type = StringArgumentType.getString(context, "type");
                                                    int amount = IntegerArgumentType.getInteger(context, "amount");
                                                    Item item = "chaina".equalsIgnoreCase(type) ? ChainaRegistries.CHAINA_GACHA_TICKET : ChainaRegistries.GACHA_TICKET;
                                                    give(player, item, amount);
                                                    context.getSource().sendFeedback(() -> Text.literal("Entregados " + amount + " tickets " + ("chaina".equalsIgnoreCase(type) ? "Chaina" : "estándar") + "."), false);
                                                    return 1;
                                                }))))
                        )
                ));
    }

    private static int openGacha(ServerPlayerEntity player, String banner) {
        SystemsNetworking.openGacha(player, banner, "");
        return 1;
    }

    private static void give(ServerPlayerEntity player, Item item, int amount) {
        int remaining = amount;
        while (remaining > 0) {
            int count = Math.min(item.getMaxCount(), remaining);
            ItemStack stack = new ItemStack(item, count);
            player.getInventory().insertStack(stack);
            if (!stack.isEmpty()) player.dropItem(stack, false);
            remaining -= count;
        }
        player.getInventory().markDirty();
    }
}
