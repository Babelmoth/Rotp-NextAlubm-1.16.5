package com.nextalubm.rotp_nextalbum.stand;

import java.util.Optional;

import com.github.standobyte.jojo.action.stand.effect.StandEffectInstance;
import com.github.standobyte.jojo.action.stand.effect.StandEffectType;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.nextalubm.rotp_nextalbum.entity.BeachBoyStandEntity;
import com.nextalubm.rotp_nextalbum.init.InitStandEffects;
import com.nextalubm.rotp_nextalbum.util.BeachBoyLine;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;

public class BeachBoySettingsEffect extends StandEffectInstance {
    private boolean pathLine;
    private boolean crushDamage = true;
    private boolean soundSense = true;
    private float maxLength = BeachBoyLine.DEFAULT_LENGTH;

    public BeachBoySettingsEffect() {
        this(InitStandEffects.BEACH_BOY_SETTINGS.get());
    }

    public BeachBoySettingsEffect(StandEffectType<?> effectType) {
        super(effectType);
    }

    public static Optional<BeachBoySettingsEffect> get(IStandPower power) {
        if (power == null) {
            return Optional.empty();
        }
        return power.getContinuousEffects().getEffectOfType(InitStandEffects.BEACH_BOY_SETTINGS.get());
    }

    public static BeachBoySettingsEffect getOrCreate(IStandPower power) {
        Optional<BeachBoySettingsEffect> existing = get(power);
        if (existing.isPresent()) {
            return existing.get();
        }
        BeachBoySettingsEffect created = new BeachBoySettingsEffect();
        if (power.getStandManifestation() instanceof BeachBoyStandEntity) {
            created.hydrateFromStand((BeachBoyStandEntity) power.getStandManifestation());
        }
        power.getContinuousEffects().addEffect(created);
        return created;
    }

    public void hydrateFromStand(BeachBoyStandEntity stand) {
        if (stand == null) {
            return;
        }
        pathLine = stand.hasLineBlockCollision();
        crushDamage = stand.isCrushDamageEnabled();
        soundSense = stand.isSoundSenseEnabled();
        maxLength = stand.getMaxLineLength();
    }

    public boolean isPathLine() {
        return pathLine;
    }

    public boolean isCrushDamage() {
        return crushDamage;
    }

    public boolean isSoundSense() {
        return soundSense;
    }

    public float getMaxLength() {
        return maxLength;
    }

    public void setPathLine(boolean pathLine) {
        if (this.pathLine == pathLine) {
            return;
        }
        this.pathLine = pathLine;
        applyToStand();
        syncUser();
    }

    public void setCrushDamage(boolean crushDamage) {
        if (this.crushDamage == crushDamage) {
            return;
        }
        this.crushDamage = crushDamage;
        applyToStand();
        syncUser();
    }

    public void setSoundSense(boolean soundSense) {
        if (this.soundSense == soundSense) {
            return;
        }
        this.soundSense = soundSense;
        applyToStand();
        syncUser();
    }

    public void setMaxLength(float maxLength) {
        float next = Math.max(BeachBoyLine.MIN_LENGTH, maxLength);
        if (Float.compare(this.maxLength, next) == 0) {
            return;
        }
        this.maxLength = next;
        applyToStand();
        syncUser();
    }

    public void applyToStand() {
        if (userPower == null) {
            return;
        }
        if (!(userPower.getStandManifestation() instanceof BeachBoyStandEntity)) {
            return;
        }
        BeachBoyStandEntity stand = (BeachBoyStandEntity) userPower.getStandManifestation();
        stand.copySettingsFromEffect(this);
    }

    private void syncUser() {
        if (world != null && !world.isClientSide() && user instanceof ServerPlayerEntity) {
            syncWithUserOnly((ServerPlayerEntity) user);
        }
    }

    @Override
    protected void start() {
        applyToStand();
    }

    @Override
    protected void tick() {
    }

    @Override
    protected void stop() {
    }

    @Override
    protected boolean needsTarget() {
        return false;
    }

    @Override
    public boolean removeOnUserDeath() {
        return false;
    }

    @Override
    public boolean removeOnUserLogout() {
        return false;
    }

    @Override
    public boolean removeOnStandChanged() {
        return false;
    }

    @Override
    public void writeAdditionalPacketData(PacketBuffer buf, boolean toUser) {
        buf.writeBoolean(pathLine);
        buf.writeBoolean(crushDamage);
        buf.writeBoolean(soundSense);
        buf.writeFloat(maxLength);
    }

    @Override
    public void readAdditionalPacketData(PacketBuffer buf, boolean toUser) {
        pathLine = buf.readBoolean();
        crushDamage = buf.readBoolean();
        soundSense = buf.readBoolean();
        maxLength = buf.readFloat();
    }

    @Override
    protected void writeAdditionalSaveData(CompoundNBT nbt) {
        nbt.putBoolean("PathLine", pathLine);
        nbt.putBoolean("CrushDamage", crushDamage);
        nbt.putBoolean("SoundSense", soundSense);
        nbt.putFloat("MaxLength", maxLength);
    }

    @Override
    protected void readAdditionalSaveData(CompoundNBT nbt) {
        pathLine = nbt.getBoolean("PathLine");
        crushDamage = !nbt.contains("CrushDamage") || nbt.getBoolean("CrushDamage");
        soundSense = !nbt.contains("SoundSense") || nbt.getBoolean("SoundSense");
        if (nbt.contains("MaxLength")) {
            maxLength = nbt.getFloat("MaxLength");
        }
    }
}
