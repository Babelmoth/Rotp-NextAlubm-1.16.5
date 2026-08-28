package com.nextalubm.rotp_nextalbum.util;

import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.MathHelper;

public final class AgingRateState {
    private static final String NBT_KEY = "RotpNextAlbumAgingRate";
    public static final float MOVE_HEAT_CAP = 0.40F;
    public static final float STAND_HEAT_CAP = 0.70F;
    public static final float DIGESTION_CAP = 0.18F;
    public static final float SPRINT_HEAT_PER_TICK = 0.0035F;
    public static final float JUMP_HEAT = 0.05F;
    public static final float MINE_HEAT_PER_TICK = 0.0025F;
    public static final float DECAY_PER_TICK = 0.006F;
    public static final int IDLE_BEFORE_DECAY_TICKS = 30;
    public static final float DIGESTION_DECAY_PER_TICK = DIGESTION_CAP / 240.0F;
    public static final int ICE_COOL_MAX_TICKS = 120;
    public static final float DOWNED_COOL_PER_TICK = 0.0035F;
    public static final float DOWNED_COOL_CAP = 1.00F;
    public static final float DOWNED_COOL_DECAY_PER_TICK = 0.014F;

    public float exertion;
    public float digestion;
    public int idleTicks;
    public float lastStamina;
    public boolean hasLastStamina;
    public int iceCoolTicks;
    public float iceCoolAmount;
    public float downedCool;

    public static AgingRateState of(LivingEntity entity) {
        AgingRateState state = new AgingRateState();
        CompoundNBT tag = entity.getPersistentData().getCompound(NBT_KEY);
        state.exertion = tag.getFloat("Exertion");
        state.digestion = tag.getFloat("Digestion");
        state.idleTicks = tag.getInt("IdleTicks");
        state.hasLastStamina = tag.contains("LastStamina");
        state.lastStamina = tag.getFloat("LastStamina");
        state.iceCoolTicks = tag.getInt("IceCoolTicks");
        state.iceCoolAmount = tag.getFloat("IceCoolAmount");
        state.downedCool = tag.getFloat("DownedCool");
        return state;
    }

    public void save(LivingEntity entity) {
        CompoundNBT tag = new CompoundNBT();
        tag.putFloat("Exertion", exertion);
        tag.putFloat("Digestion", digestion);
        tag.putInt("IdleTicks", idleTicks);
        if (hasLastStamina) {
            tag.putFloat("LastStamina", lastStamina);
        }
        tag.putInt("IceCoolTicks", iceCoolTicks);
        tag.putFloat("IceCoolAmount", iceCoolAmount);
        tag.putFloat("DownedCool", downedCool);
        entity.getPersistentData().put(NBT_KEY, tag);
    }

    public void markActivity() {
        idleTicks = 0;
    }

    public void addMoveHeat(float amount) {
        if (amount <= 0F) {
            return;
        }
        markActivity();
        float room = Math.max(0F, MOVE_HEAT_CAP - exertion);
        if (exertion >= STAND_HEAT_CAP) {
            return;
        }
        if (exertion >= MOVE_HEAT_CAP) {
            return;
        }
        exertion = Math.min(MOVE_HEAT_CAP, exertion + Math.min(amount, room));
    }

    public void addStandHeat(float amount) {
        if (amount <= 0F) {
            return;
        }
        markActivity();
        exertion = Math.min(STAND_HEAT_CAP, exertion + amount);
    }

    public void addDigestion(float amount, float cap) {
        if (amount <= 0F) {
            return;
        }
        digestion = digestion + (cap - digestion) * MathHelper.clamp(amount, 0F, 1F);
        digestion = MathHelper.clamp(digestion, 0F, cap);
    }

    public void startIceCool(float amount, int ticks) {
        iceCoolAmount = Math.min(iceCoolAmount + amount, 0.55F);
        iceCoolTicks = Math.max(iceCoolTicks, ticks);
    }

    public float currentIceCool() {
        return iceCoolTicks > 0 ? iceCoolAmount : 0F;
    }

    public void tickDownedCool(boolean downed) {
        if (downed) {
            downedCool = Math.min(DOWNED_COOL_CAP, downedCool + DOWNED_COOL_PER_TICK);
            return;
        }
        if (downedCool > 0F) {
            downedCool = Math.max(0F, downedCool - DOWNED_COOL_DECAY_PER_TICK);
        }
    }

    public void clearDownedCool() {
        downedCool = 0F;
    }

    public void tickDecay(float digestionCap) {
        idleTicks++;
        if (idleTicks > IDLE_BEFORE_DECAY_TICKS && exertion > 0F) {
            exertion = Math.max(0F, exertion - DECAY_PER_TICK);
        }
        if (digestion > 0F) {
            digestion = Math.max(0F, digestion - DIGESTION_DECAY_PER_TICK * (digestionCap / DIGESTION_CAP));
        }
        if (iceCoolTicks > 0) {
            iceCoolTicks--;
            if (iceCoolTicks <= 0) {
                iceCoolAmount = 0F;
            }
        }
    }
}
