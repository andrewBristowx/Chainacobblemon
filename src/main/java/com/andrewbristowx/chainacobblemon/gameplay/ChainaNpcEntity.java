package com.andrewbristowx.chainacobblemon.gameplay;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.world.World;

/**
 * Lightweight persistent NPC. It extends VillagerEntity so alpha.1 persistence/reconciliation stays
 * compatible while the client can render it with a player model and a per-NPC Chaina skin.
 */
public final class ChainaNpcEntity extends VillagerEntity {
    public ChainaNpcEntity(EntityType<? extends VillagerEntity> type, World world) {
        super(type, world);
        setAiDisabled(true);
        setInvulnerable(true);
        setSilent(true);
        setPersistent();
    }

    @Override public boolean damage(DamageSource source, float amount) { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean cannotDespawn() { return true; }

    @Override public void tick() {
        super.tick();
        setVelocity(0.0, getVelocity().y, 0.0);
        setBodyYaw(getYaw());
        setHeadYaw(getYaw());
    }
}
