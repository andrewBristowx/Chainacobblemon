package com.andrewbristowx.chainacobblemon.npc;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;

import java.util.Locale;

/** NPC inmóvil que usa modelo de jugador y skin sincronizada por ID. */
public final class ChainaNpcEntity extends MobEntity {
    private static final TrackedData<String> NPC_ID = DataTracker.registerData(ChainaNpcEntity.class, TrackedDataHandlerRegistry.STRING);
    private static final String NBT_ID = "ChainacobblemonNpcId";

    public ChainaNpcEntity(EntityType<? extends ChainaNpcEntity> type, World world) {
        super(type, world);
        setAiDisabled(true);
        setInvulnerable(true);
        setPersistent();
        setSilent(true);
        setCanPickUpLoot(false);
    }

    @Override protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(NPC_ID, "");
    }

    public String npcId() { return dataTracker.get(NPC_ID); }

    public void setNpcId(String value) {
        String id = value == null ? "" : value.toLowerCase(Locale.ROOT).trim().replaceAll("[^a-z0-9_-]", "");
        dataTracker.set(NPC_ID, id.length() > 32 ? id.substring(0, 32) : id);
    }

    @Override public boolean damage(DamageSource source, float amount) { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean cannotDespawn() { return true; }

    @Override public void tick() {
        super.tick();
        setVelocity(0.0D, getVelocity().y, 0.0D);
        setBodyYaw(getYaw());
        setHeadYaw(getYaw());
    }

    @Override public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putString(NBT_ID, npcId());
    }

    @Override public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        setNpcId(nbt.getString(NBT_ID));
        setAiDisabled(true);
        setInvulnerable(true);
        setPersistent();
        setSilent(true);
    }
}
