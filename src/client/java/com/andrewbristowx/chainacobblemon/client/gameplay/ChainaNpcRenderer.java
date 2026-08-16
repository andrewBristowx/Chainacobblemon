package com.andrewbristowx.chainacobblemon.client.gameplay;

import com.andrewbristowx.chainacobblemon.Chainacobblemon;
import com.andrewbristowx.chainacobblemon.gameplay.ChainaNpcEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.util.Identifier;

/** Player-model renderer for automatically discovered Chaina NPC skins. */
public final class ChainaNpcRenderer extends MobEntityRenderer<ChainaNpcEntity, PlayerEntityModel<ChainaNpcEntity>> {
    private static final Identifier FALLBACK = Identifier.of("minecraft", "textures/entity/player/wide/steve.png");

    public ChainaNpcRenderer(EntityRendererFactory.Context context) {
        super(context, new PlayerEntityModel<>(context.getPart(EntityModelLayers.PLAYER), false), 0.5F);
    }

    @Override public Identifier getTexture(ChainaNpcEntity entity) {
        return NpcSkinClient.texture(entity.getUuid(), FALLBACK);
    }
}
