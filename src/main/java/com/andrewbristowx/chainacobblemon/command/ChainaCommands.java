package com.andrewbristowx.chainacobblemon.command;

import com.andrewbristowx.chainacobblemon.Chainacobblemon;
import com.andrewbristowx.chainacobblemon.hologram.HologramDefinition;
import com.andrewbristowx.chainacobblemon.hologram.HologramManager;
import com.andrewbristowx.chainacobblemon.integration.PermissionBridge;
import com.andrewbristowx.chainacobblemon.integration.PlaceholderIntegration;
import com.andrewbristowx.chainacobblemon.structure.ChainaStructureRegistry;
import com.andrewbristowx.chainacobblemon.structure.ChainaStructureRegistry.StructureLocation;
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
    public static final String STRUCTURE_ADMIN = "chainacobblemon.structure.admin";

    private ChainaCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("chaina")
                    .then(hologramRoot())
                    .then(placeholderRoot())
                    .then(literal("structure").requires(source -> PermissionBridge.check(source, STRUCTURE_ADMIN, 2))
                            .then(structurePlace())
                            .then(structureRegister())
                            .then(structureSet())
                            .then(structureInfo())
                            .then(structureRemove())
                            .then(structureList()))
                    .then(literal("version").executes(context -> {
                        context.getSource().sendFeedback(() -> Text.literal("Chainacobblemon " + Chainacobblemon.VERSION), false);
                        return 1;
                    })));

            dispatcher.register(literal("chainacobblemon")
                    .requires(source -> PermissionBridge.check(source, STRUCTURE_ADMIN, 2))
                    .then(literal("structure")
                            .then(structurePlace())
                            .then(structureRegister())
                            .then(structureSet())
                            .then(structureInfo())
                            .then(structureRemove())
                            .then(structureList())));
        });
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<ServerCommandSource> structurePlace() {
        return literal("place")
                .then(argument("id", StringArgumentType.word()).executes(context -> {
                    ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                    String id = StringArgumentType.getString(context, "id");
                    if (ChainaStructureRegistry.get(context.getSource().getServer(), id) != null) {
                        context.getSource().sendError(Text.literal("La estructura '" + id + "' ya está registrada. Usa /chainacobblemon structure remove " + id + " si quieres reemplazarla."));
                        return 0;
                    }

                    // Use Minecraft's own /place structure implementation so the structure's
                    // processors, palettes and native placement behavior are preserved.
                    String command = "place structure " + id + " ~ ~ ~";
                    int result;
                    try {
                        result = context.getSource().getServer().getCommandManager().executeWithPrefix(context.getSource(), command);
                    } catch (Exception exception) {
                        context.getSource().sendError(Text.literal("No se pudo colocar '" + id + "': " + exception.getMessage()));
                        return 0;
                    }
                    if (result <= 0) {
                        context.getSource().sendError(Text.literal("Minecraft no pudo colocar la estructura '" + id + "'. Revisa el ID y que la estructura exista."));
                        return 0;
                    }

                    if (!ChainaStructureRegistry.register(context.getSource().getServer(), id, player)) {
                        context.getSource().sendError(Text.literal("La estructura se colocó, pero no se pudo guardar su registro."));
                        return 0;
                    }
                    context.getSource().sendFeedback(() -> Text.literal("✓ " + id + " generada y registrada en "
                            + player.getBlockX() + " " + player.getBlockY() + " " + player.getBlockZ()
                            + " (" + player.getServerWorld().getRegistryKey().getValue() + ")."), true);
                    return 1;
                }));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<ServerCommandSource> structureRegister() {
        return literal("register")
                .then(argument("id", StringArgumentType.word()).executes(context -> {
                    ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                    String id = StringArgumentType.getString(context, "id");
                    if (!ChainaStructureRegistry.register(context.getSource().getServer(), id, player)) {
                        context.getSource().sendError(Text.literal("No se pudo guardar la estructura."));
                        return 0;
                    }
                    context.getSource().sendFeedback(() -> Text.literal("✓ '" + id + "' registrada en tu posición: "
                            + player.getBlockX() + " " + player.getBlockY() + " " + player.getBlockZ()), true);
                    return 1;
                }));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<ServerCommandSource> structureSet() {
        return literal("set")
                .then(argument("id", StringArgumentType.word())
                        .then(argument("x", IntegerArgumentType.integer())
                                .then(argument("y", IntegerArgumentType.integer())
                                        .then(argument("z", IntegerArgumentType.integer()).executes(context -> {
                                            String id = StringArgumentType.getString(context, "id");
                                            int x = IntegerArgumentType.getInteger(context, "x");
                                            int y = IntegerArgumentType.getInteger(context, "y");
                                            int z = IntegerArgumentType.getInteger(context, "z");
                                            String dimension = context.getSource().getWorld().getRegistryKey().getValue().toString();
                                            if (!ChainaStructureRegistry.set(context.getSource().getServer(), id, dimension, x, y, z)) {
                                                context.getSource().sendError(Text.literal("No se pudo guardar la coordenada."));
                                                return 0;
                                            }
                                            context.getSource().sendFeedback(() -> Text.literal("✓ '" + id + "' fijada en " + dimension + " " + x + " " + y + " " + z), true);
                                            return 1;
                                        }))))));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<ServerCommandSource> structureInfo() {
        return literal("info")
                .then(argument("id", StringArgumentType.word()).executes(context -> {
                    String id = StringArgumentType.getString(context, "id");
                    StructureLocation location = ChainaStructureRegistry.get(context.getSource().getServer(), id);
                    if (location == null) {
                        context.getSource().sendError(Text.literal("No existe una estructura registrada con ID '" + id + "'."));
                        return 0;
                    }
                    context.getSource().sendFeedback(() -> Text.literal(id + " → " + location.dimension() + " | "
                            + location.x() + " " + location.y() + " " + location.z()), false);
                    return 1;
                }));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<ServerCommandSource> structureRemove() {
        return literal("remove")
                .then(argument("id", StringArgumentType.word()).executes(context -> {
                    String id = StringArgumentType.getString(context, "id");
                    if (!ChainaStructureRegistry.remove(context.getSource().getServer(), id)) {
                        context.getSource().sendError(Text.literal("No existe una estructura registrada con ID '" + id + "'."));
                        return 0;
                    }
                    context.getSource().sendFeedback(() -> Text.literal("Registro de '" + id + "' eliminado. La construcción NO fue borrada."), true);
                    return 1;
                }));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<ServerCommandSource> structureList() {
        return literal("list").executes(context -> {
            var entries = ChainaStructureRegistry.all(context.getSource().getServer());
            if (entries.isEmpty()) {
                context.getSource().sendFeedback(() -> Text.literal("No hay estructuras registradas."), false);
                return 1;
            }
            context.getSource().sendFeedback(() -> Text.literal("§6Estructuras oficiales de CHAINA (" + entries.size() + ")"), false);
            for (var entry : entries) {
                StructureLocation location = entry.getValue();
                context.getSource().sendFeedback(() -> Text.literal("§7- §f" + entry.getKey() + " §8→ §f"
                        + location.dimension() + " " + location.x() + " " + location.y() + " " + location.z()), false);
            }
            return entries.size();
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
