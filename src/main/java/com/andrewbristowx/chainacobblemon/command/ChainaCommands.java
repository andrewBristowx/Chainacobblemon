package com.andrewbristowx.chainacobblemon.command;

import com.andrewbristowx.chainacobblemon.Chainacobblemon;
import com.andrewbristowx.chainacobblemon.hologram.HologramDefinition;
import com.andrewbristowx.chainacobblemon.hologram.HologramManager;
import com.andrewbristowx.chainacobblemon.integration.PermissionBridge;
import com.andrewbristowx.chainacobblemon.integration.PlaceholderIntegration;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class ChainaCommands {
    public static final String HOLOGRAM_ADMIN = "chainacobblemon.hologram.admin";
    public static final String PLACEHOLDER_USE = "chainacobblemon.placeholder.use";

    private ChainaCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("chaina")
                    .then(hologramRoot())
                    .then(placeholderRoot())
                    .then(literal("version").executes(context -> {
                        context.getSource().sendFeedback(() -> Text.literal("Chainacobblemon " + Chainacobblemon.VERSION), false);
                        return 1;
                    })));
        });
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<ServerCommandSource> hologramRoot() {
        return literal("hologram")
                .requires(source -> PermissionBridge.check(source, HOLOGRAM_ADMIN, 2))
                .then(literal("create").then(argument("id", StringArgumentType.word()).executes(context -> {
                    ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                    String id = StringArgumentType.getString(context, "id");
                    if (!HologramManager.create(id, player)) { context.getSource().sendError(Text.literal("No se pudo crear. El ID ya existe o no es valido.")); return 0; }
                    context.getSource().sendFeedback(() -> Text.literal("Holograma '" + id + "' creado."), false); return 1;
                })))
                .then(literal("line").then(argument("id", StringArgumentType.word()).then(argument("line", IntegerArgumentType.integer(1)).then(argument("text", StringArgumentType.greedyString()).executes(context -> {
                    String id = StringArgumentType.getString(context, "id"); int line = IntegerArgumentType.getInteger(context, "line"); String text = StringArgumentType.getString(context, "text");
                    if (!HologramManager.setLine(id, line, text)) { context.getSource().sendError(Text.literal("Holograma no encontrado.")); return 0; }
                    context.getSource().sendFeedback(() -> Text.literal("Linea " + line + " actualizada."), false); return 1;
                })))))
                .then(literal("addline").then(argument("id", StringArgumentType.word()).then(argument("text", StringArgumentType.greedyString()).executes(context -> {
                    String id = StringArgumentType.getString(context, "id"); String text = StringArgumentType.getString(context, "text");
                    if (!HologramManager.addLine(id, text)) { context.getSource().sendError(Text.literal("Holograma no encontrado.")); return 0; }
                    context.getSource().sendFeedback(() -> Text.literal("Linea agregada."), false); return 1;
                }))))
                .then(literal("removeline").then(argument("id", StringArgumentType.word()).then(argument("line", IntegerArgumentType.integer(1)).executes(context -> {
                    String id = StringArgumentType.getString(context, "id"); int line = IntegerArgumentType.getInteger(context, "line");
                    if (!HologramManager.removeLine(id, line)) { context.getSource().sendError(Text.literal("Holograma o linea no encontrados.")); return 0; }
                    context.getSource().sendFeedback(() -> Text.literal("Linea eliminada."), false); return 1;
                }))))
                .then(literal("move").then(argument("id", StringArgumentType.word()).executes(context -> {
                    ServerPlayerEntity player = context.getSource().getPlayerOrThrow(); String id = StringArgumentType.getString(context, "id");
                    if (!HologramManager.move(id, player)) { context.getSource().sendError(Text.literal("Holograma no encontrado.")); return 0; }
                    context.getSource().sendFeedback(() -> Text.literal("Holograma movido a tu posicion."), false); return 1;
                })))
                .then(literal("delete").then(argument("id", StringArgumentType.word()).executes(context -> {
                    String id = StringArgumentType.getString(context, "id"); if (!HologramManager.delete(id)) { context.getSource().sendError(Text.literal("Holograma no encontrado.")); return 0; }
                    context.getSource().sendFeedback(() -> Text.literal("Holograma eliminado."), false); return 1;
                })))
                .then(literal("refresh").then(argument("id", StringArgumentType.word()).executes(context -> {
                    String id = StringArgumentType.getString(context, "id"); if (!HologramManager.refresh(id)) { context.getSource().sendError(Text.literal("Holograma no encontrado.")); return 0; }
                    context.getSource().sendFeedback(() -> Text.literal("Holograma actualizado."), false); return 1;
                })))
                .then(literal("list").executes(context -> {
                    if (HologramManager.all().isEmpty()) { context.getSource().sendFeedback(() -> Text.literal("No hay hologramas configurados."), false); return 1; }
                    context.getSource().sendFeedback(() -> Text.literal("Hologramas:"), false);
                    for (HologramDefinition definition : HologramManager.all()) context.getSource().sendFeedback(() -> Text.literal(" - " + definition.id + " [" + definition.lines.size() + " lineas]"), false);
                    return HologramManager.all().size();
                }));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<ServerCommandSource> placeholderRoot() {
        return literal("placeholder").requires(source -> PermissionBridge.check(source, PLACEHOLDER_USE, 2)).then(literal("test").then(argument("text", StringArgumentType.greedyString()).executes(context -> {
            String text = StringArgumentType.getString(context, "text"); ServerPlayerEntity player = context.getSource().getPlayer();
            Text parsed = player == null ? PlaceholderIntegration.parseForServer(text, context.getSource().getServer()) : PlaceholderIntegration.parseForPlayer(text, player);
            context.getSource().sendFeedback(() -> parsed, false); return 1;
        })));
    }
}
