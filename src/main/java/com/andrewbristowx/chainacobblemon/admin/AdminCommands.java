package com.andrewbristowx.chainacobblemon.admin;

import com.andrewbristowx.chainacobblemon.gameplay.GameplaySystems;
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
    public static synchronized void register(){if(initialized)return;initialized=true;CommandRegistrationCallback.EVENT.register((dispatcher,access,environment)->
            dispatcher.register(literal("chaina").then(literal("admin")
                    .requires(s-> PermissionBridge.check(s, GameplaySystems.ADMIN,2))
                    .then(literal("reiniciar").then(argument("jugador", EntityArgumentType.player()).then(argument("seccion", StringArgumentType.word())
                            .suggests((c,b)->{for(String s:new String[]{"todo","economia","trabajos","misiones","gasha","diario","pase","mazmorras","kits"})b.suggest(s);return b.buildFuture();})
                            .executes(c->{ServerPlayerEntity p=EntityArgumentType.getPlayer(c,"jugador");String r=PlayerResetService.reset(p,StringArgumentType.getString(c,"seccion"));c.getSource().sendFeedback(()->Text.literal(r),true);return 1;}))))
                    .then(literal("kits").then(literal("recargar").executes(c->{ChainaKits.reload();c.getSource().sendFeedback(()->Text.literal("Kits de Chaina recargados."),false);return 1;})))
            ))));}
}
