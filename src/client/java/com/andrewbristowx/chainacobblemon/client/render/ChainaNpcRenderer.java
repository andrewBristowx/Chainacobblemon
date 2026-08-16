package com.andrewbristowx.chainacobblemon.client.render;

import com.andrewbristowx.chainacobblemon.client.visual.ChainaNpcSkinCache;
import com.andrewbristowx.chainacobblemon.npc.ChainaNpcEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.util.Identifier;

public final class ChainaNpcRenderer extends MobEntityRenderer<ChainaNpcEntity, PlayerEntityModel<ChainaNpcEntity>> {
    private final Identifier fallback;

    public ChainaNpcRenderer(EntityRendererFactory.Context context, boolean slim) {
        super(context, new PlayerEntityModel<>(context.getPart(slim ? EntityModelLayers.PLAYER_SLIM : EntityModelLayers.PLAYER), slim), 0.5F);
        fallback = Identifier.of("minecraft", slim ? "textures/entity/player/slim/alex.png" : "textures/entity/player/wide/steve.png");
    }

    @Override public Identifier getTexture(ChainaNpcEntity entity) {
        return ChainaNpcSkinCache.texture(entity.npcId(), fallback);
    }
}
