package com.andrewbristowx.chainacobblemon.client.gameplay;

import com.andrewbristowx.chainacobblemon.gameplay.ChainaNpcEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.util.Identifier;

/** Player-model renderer for automatically discovered Chaina NPC skins. */
public final class ChainaNpcRenderer extends MobEntityRenderer<ChainaNpcEntity, PlayerEntityModel<ChainaNpcEntity>> {
    private static final Identifier FALLBACK_WIDE = Identifier.of("minecraft", "textures/entity/player/wide/steve.png");
    private static final Identifier FALLBACK_SLIM = Identifier.of("minecraft", "textures/entity/player/slim/alex.png");
    private final boolean slim;

    public ChainaNpcRenderer(EntityRendererFactory.Context context, boolean slim) {
        super(context, new PlayerEntityModel<>(context.getPart(slim ? EntityModelLayers.PLAYER_SLIM : EntityModelLayers.PLAYER), slim), 0.5F);
        this.slim = slim;
    }

    @Override public Identifier getTexture(ChainaNpcEntity entity) {
        return NpcSkinClient.texture(entity.getUuid(), slim ? FALLBACK_SLIM : FALLBACK_WIDE);
    }
}
