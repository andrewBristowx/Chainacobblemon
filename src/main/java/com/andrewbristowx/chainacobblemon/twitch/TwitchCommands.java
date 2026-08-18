package com.andrewbristowx.chainacobblemon.twitch;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.literal;

public final class TwitchCommands {
    private TwitchCommands() { }

    public static void register(TwitchService service) {
        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) -> registerCommands(dispatcher, service));
    }

    private static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher, TwitchService service) {
        dispatcher.register(literal("twitch")
                .executes(context -> playerAction(context.getSource(), service, "open"))
                .then(literal("estado").executes(context -> playerAction(context.getSource(), service, "status")))
                .then(literal("status").executes(context -> playerAction(context.getSource(), service, "status")))
                .then(literal("vincular").executes(context -> playerAction(context.getSource(), service, "link")))
                .then(literal("link").executes(context -> playerAction(context.getSource(), service, "link")))
                .then(literal("sync").executes(context -> playerAction(context.getSource(), service, "sync")))
                .then(literal("desvincular").executes(context -> playerAction(context.getSource(), service, "unlink"))));

        dispatcher.register(literal("chaina")
                .then(literal("twitch")
                        .executes(context -> playerAction(context.getSource(), service, "open"))
                        .then(literal("status").executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayer();
                            if (player == null) return 0;
                            context.getSource().sendFeedback(() -> Text.literal(service.statusLine(player)), false);
                            return 1;
                        }))
                        .then(literal("sync").executes(context -> playerAction(context.getSource(), service, "sync")))
                        .then(literal("test").requires(source -> source.hasPermissionLevel(2))
                                .then(literal("online").executes(context -> testChannel(context.getSource(), service, true)))
                                .then(literal("offline").executes(context -> testChannel(context.getSource(), service, false)))
                                .then(literal("nosub").executes(context -> testTier(context.getSource(), service, 0)))
                                .then(literal("sub")
                                        .then(literal("1").executes(context -> testTier(context.getSource(), service, 1)))
                                        .then(literal("2").executes(context -> testTier(context.getSource(), service, 2)))
                                        .then(literal("3").executes(context -> testTier(context.getSource(), service, 3))))))));
    }

    private static int playerAction(ServerCommandSource source, TwitchService service, String action) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendFeedback(() -> Text.literal("Este comando Twitch debe ejecutarse como jugador."), false);
            return 0;
        }
        service.handleAction(player, action);
        return 1;
    }

    private static int testTier(ServerCommandSource source, TwitchService service, int tier) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;
        boolean ok = service.debugSetTier(player, tier);
        if (!ok) source.sendFeedback(() -> Text.literal("§cLos comandos test solo funcionan con twitch.mode=development."), false);
        return ok ? 1 : 0;
    }

    private static int testChannel(ServerCommandSource source, TwitchService service, boolean online) {
        boolean ok = service.debugSetChannel(online);
        if (!ok) source.sendFeedback(() -> Text.literal("§cLos comandos test solo funcionan con twitch.mode=development."), false);
        return ok ? 1 : 0;
    }
}
