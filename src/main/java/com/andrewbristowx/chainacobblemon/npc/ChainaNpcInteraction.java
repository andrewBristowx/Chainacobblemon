package com.andrewbristowx.chainacobblemon.npc;

import com.andrewbristowx.chainacobblemon.gameplay.CobblemonBridge;
import com.andrewbristowx.chainacobblemon.gameplay.GameplayConfig;
import com.andrewbristowx.chainacobblemon.gameplay.GameplayNetworking;
import com.andrewbristowx.chainacobblemon.gameplay.GameplaySystems;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;

import java.util.Locale;

/** Intercepta primero servicios visuales; los entrenadores/comandos siguen en el backend existente. */
public final class ChainaNpcInteraction {
    private ChainaNpcInteraction() {}

    public static void initialize() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient || !(player instanceof ServerPlayerEntity sp) || !(entity instanceof ChainaNpcEntity npc)) return ActionResult.PASS;
            GameplayConfig.Npc definition = GameplaySystems.config().npcs.get(npc.npcId());
            if (definition == null || definition.type == null) return ActionResult.PASS;
            return switch (definition.type.toLowerCase(Locale.ROOT)) {
                case "nurse" -> {
                    if (CobblemonBridge.healParty(sp)) {
                        GameplaySystems.recordAction(sp, "heal", "", 1);
                        sp.sendMessage(Text.literal("§dEnfermera Chaina §7» §f¡Tu equipo Pokémon está completamente curado!"), false);
                    } else sp.sendMessage(Text.literal("§cNo se pudo acceder a tu equipo Pokémon."), false);
                    yield ActionResult.SUCCESS;
                }
                case "shop" -> { GameplayNetworking.open(sp, "shop"); yield ActionResult.SUCCESS; }
                case "quest", "story" -> { GameplayNetworking.open(sp, "quests"); yield ActionResult.SUCCESS; }
                case "menu" -> { GameplayNetworking.open(sp, "menu"); yield ActionResult.SUCCESS; }
                default -> ActionResult.PASS;
            };
        });
    }
}
