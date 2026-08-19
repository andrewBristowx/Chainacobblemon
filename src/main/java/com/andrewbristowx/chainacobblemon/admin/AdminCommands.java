package com.andrewbristowx.chainacobblemon.admin;

import com.andrewbristowx.chainacobblemon.gameplay.GameplayAdminService;
import com.andrewbristowx.chainacobblemon.gameplay.GameplaySystems;
import com.andrewbristowx.chainacobblemon.gameplay.NpcSkinNetworking;
import com.andrewbristowx.chainacobblemon.integration.PermissionBridge;
import com.andrewbristowx.chainacobblemon.rewards.ChainaKits;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class AdminCommands {
    private static boolean initialized;
    private AdminCommands() {}

    public static synchronized void register() {
        if (initialized) return;
        initialized = true;
        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) -> {
            var admin = literal("admin")
                    .requires(source -> PermissionBridge.check(source, GameplaySystems.ADMIN, 2))
                    .then(literal("auditoria").executes(context -> {
                        for (String line : ServerAuditService.lines(context.getSource().getServer())) context.getSource().sendFeedback(() -> Text.literal(line), false);
                        return 1;
                    }))
                    .then(literal("recargar").executes(context -> {
                        GameplaySystems.reload();
                        GameplayAdminService.ensureDefaults();
                        ChainaKits.reload();
                        NpcSkinNetworking.broadcastNpcMap();
                        context.getSource().sendFeedback(() -> Text.literal("§aSistemas, skins y kits de Chaina recargados."), true);
                        return 1;
                    }))
                    .then(literal("reiniciar")
                            .then(argument("jugador", EntityArgumentType.player())
                                    .then(argument("seccion", StringArgumentType.word())
                                            .suggests((context, builder) -> {
                                                for (String value : new String[]{"todo", "economia", "trabajos", "misiones", "gasha", "diario", "pase", "mazmorras", "kits"}) builder.suggest(value);
                                                return builder.buildFuture();
                                            })
                                            .executes(context -> {
                                                ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "jugador");
                                                String result = PlayerResetService.reset(player, StringArgumentType.getString(context, "seccion"));
                                                context.getSource().sendFeedback(() -> Text.literal(result), true);
                                                return 1;
                                            }))))
                    .then(literal("npc")
                            .then(literal("skin")
                                    .then(argument("npc", StringArgumentType.word())
                                            .then(argument("skin", StringArgumentType.string())
                                                    .then(argument("modelo", StringArgumentType.word())
                                                            .suggests((context, builder) -> { builder.suggest("slim"); builder.suggest("wide"); return builder.buildFuture(); })
                                                            .executes(context -> {
                                                                String npc = StringArgumentType.getString(context, "npc");
                                                                String skin = StringArgumentType.getString(context, "skin");
                                                                String model = StringArgumentType.getString(context, "modelo");
                                                                if (!model.equalsIgnoreCase("slim") && !model.equalsIgnoreCase("wide")) {
                                                                    context.getSource().sendError(Text.literal("Usa modelo slim o wide."));
                                                                    return 0;
                                                                }
                                                                String result = GameplayAdminService.setNpcSkin(npc, skin, model.equalsIgnoreCase("slim"));
                                                                context.getSource().sendFeedback(() -> Text.literal(result), true);
                                                                return 1;
                                                            })))))
                            .then(literal("dialogo")
                                    .then(argument("npc", StringArgumentType.word())
                                            .then(argument("texto", StringArgumentType.greedyString())
                                                    .executes(context -> {
                                                        String result = GameplayAdminService.setNpcDialogue(StringArgumentType.getString(context, "npc"), StringArgumentType.getString(context, "texto"));
                                                        context.getSource().sendFeedback(() -> Text.literal(result), true);
                                                        return 1;
                                                    })))))
                    .then(literal("kits")
                            .then(literal("recargar")
                                    .executes(context -> {
                                        ChainaKits.reload();
                                        context.getSource().sendFeedback(() -> Text.literal("Kits de Chaina recargados."), false);
                                        return 1;
                                    })));

            dispatcher.register(literal("chaina").then(admin));
        });
    }
}
