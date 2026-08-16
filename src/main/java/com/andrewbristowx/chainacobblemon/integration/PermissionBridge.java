package com.andrewbristowx.chainacobblemon.integration;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public final class PermissionBridge {
    private PermissionBridge() {}
    public static boolean check(ServerCommandSource source,String node,int fallbackPermissionLevel){ ServerPlayerEntity player=source.getPlayer(); if(player!=null){ Boolean lp=LuckPermsBridge.permission(player,node); if(lp!=null)return lp; } return source.hasPermissionLevel(fallbackPermissionLevel); }
}
