package com.nextalubm.rotp_nextalbum.entity;

import java.util.UUID;

import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.entity.stand.StandEntityType;
import com.github.standobyte.jojo.init.ModStatusEffects;
import com.github.standobyte.jojo.itemtracking.SidedItemTrackerMap;
import com.github.standobyte.jojo.itemtracking.itemcap.TrackerItemStack;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.nextalubm.rotp_nextalbum.init.InitItems;
import com.nextalubm.rotp_nextalbum.item.BeachBoyRodItem;
import com.nextalubm.rotp_nextalbum.stand.BeachBoySettingsEffect;
import com.nextalubm.rotp_nextalbum.util.BeachBoyLine;
import com.nextalubm.rotp_nextalbum.util.BeachBoySettings;
import com.nextalubm.rotp_nextalbum.util.BeachBoyStandUtil;

import com.github.standobyte.jojo.network.PacketManager;
import com.github.standobyte.jojo.network.packets.fromserver.StandControlStatusPacket;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

public class BeachBoyStandEntity extends StandEntity {
    private static final String ROD_TRACKER_KEY = "BeachBoyRodTracker";
    private static final DataParameter<Float> DATA_MAX_LENGTH = EntityDataManager.defineId(BeachBoyStandEntity.class, DataSerializers.FLOAT);
    private static final DataParameter<Boolean> DATA_BLOCK_COLLISION = EntityDataManager.defineId(BeachBoyStandEntity.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> DATA_THORNS = EntityDataManager.defineId(BeachBoyStandEntity.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Float> DATA_THORNS_MULTIPLIER = EntityDataManager.defineId(BeachBoyStandEntity.class, DataSerializers.FLOAT);
    private static final DataParameter<Boolean> DATA_SOUND_SENSE = EntityDataManager.defineId(BeachBoyStandEntity.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Integer> DATA_WHIP_TICKS = EntityDataManager.defineId(BeachBoyStandEntity.class, DataSerializers.INT);
    private static final DataParameter<Boolean> DATA_WHIP_VERTICAL = EntityDataManager.defineId(BeachBoyStandEntity.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> DATA_WHIP_LTR = EntityDataManager.defineId(BeachBoyStandEntity.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> DATA_WHIP_UPWARD = EntityDataManager.defineId(BeachBoyStandEntity.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> DATA_CRUSH_DAMAGE = EntityDataManager.defineId(BeachBoyStandEntity.class, DataSerializers.BOOLEAN);

    public BeachBoyStandEntity(StandEntityType<BeachBoyStandEntity> type, World world) {
        super(type, world);
        this.noPhysics = true;
        this.setNoGravity(true);
        entityData.set(DATA_MAX_LENGTH, BeachBoyLine.standRangeCap(this));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_MAX_LENGTH, BeachBoyLine.DEFAULT_LENGTH);
        entityData.define(DATA_BLOCK_COLLISION, false);
        entityData.define(DATA_THORNS, true);
        entityData.define(DATA_THORNS_MULTIPLIER, BeachBoyLine.DEFAULT_THORNS_MULTIPLIER);
        entityData.define(DATA_SOUND_SENSE, true);
        entityData.define(DATA_WHIP_TICKS, 0);
        entityData.define(DATA_WHIP_VERTICAL, false);
        entityData.define(DATA_WHIP_LTR, false);
        entityData.define(DATA_WHIP_UPWARD, true);
        entityData.define(DATA_CRUSH_DAMAGE, true);
    }

    @Override
    public void tick() {
        super.tick();
        followHookCamera();
        int whip = getWhipTicks();
        if (whip > 0 && !level.isClientSide) {
            com.nextalubm.rotp_nextalbum.util.BeachBoyCombatUtil.tickWhippedHost(this);
            entityData.set(DATA_WHIP_TICKS, whip - 1);
        }
        if (level.isClientSide) {
            return;
        }
        LivingEntity user = getUser();
        if (user instanceof PlayerEntity) {
            BeachBoySettings.ensure(user);
            applySettingsFromUser();
            ensureSingleRod((PlayerEntity) user);
            if (!isAlive()) {
                return;
            }
            syncLineSettingsToHook((PlayerEntity) user);
            syncRodVisuals((PlayerEntity) user);
        }
        stopRemoteIfNoHook();
    }

    @Override
    public void travel(Vector3d travelVector) {
        if (isManuallyControlled()) {
            setDeltaMovement(Vector3d.ZERO);
            return;
        }
        super.travel(travelVector);
    }

    @Override
    public void setManualControl(boolean manualControl, boolean keepPosition) {
        if (manualControl) {
            ensureHookForRemote();
            super.setManualControl(true, true);
            return;
        }
        BeachBoyHookEntity hook = BeachBoyStandUtil.findHook(getUser());
        if (hook != null) {
            hook.setRemoteControlled(false);
        }
        super.setManualControl(false, keepPosition);
    }

    public float getControlForward() {
        return this.zza;
    }

    public float getControlStrafe() {
        return this.xxa;
    }

    public boolean isControlJumping() {
        return this.jumping;
    }

    public boolean isControlSneaking() {
        return isShiftKeyDown();
    }

    private void followHookCamera() {
        if (!isManuallyControlled()) {
            return;
        }
        BeachBoyHookEntity hook = BeachBoyStandUtil.findHook(getUser());
        if (hook == null || !hook.isAlive()) {
            return;
        }
        hook.setRemoteControlled(true);
        Entity host = hook.getHookedEntity();
        double x;
        double y;
        double z;
        if (host != null && host.isAlive()) {
            x = host.getX();
            y = host.getY(0.85D);
            z = host.getZ();
        }
        else {
            x = hook.getX();
            y = hook.getY();
            z = hook.getZ();
        }
        setPos(x, y, z);
        setDeltaMovement(Vector3d.ZERO);
    }

    private void ensureHookForRemote() {
        if (level.isClientSide) {
            return;
        }
        LivingEntity user = getUser();
        if (!(user instanceof PlayerEntity)) {
            return;
        }
        PlayerEntity player = (PlayerEntity) user;
        BeachBoyHookEntity hook = BeachBoyStandUtil.findHook(player);
        if (hook == null) {
            hook = new BeachBoyHookEntity(level, player);
            level.addFreshEntity(hook);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.FISHING_BOBBER_THROW, SoundCategory.NEUTRAL, 0.5F, 0.8F);
        }
        hook.setRemoteControlled(true);
    }

    private void stopRemoteIfNoHook() {
        if (!isManuallyControlled()) {
            return;
        }
        LivingEntity user = getUser();
        if (BeachBoyStandUtil.findHook(user) != null) {
            return;
        }
        setManualControl(false, false);
        if (user instanceof ServerPlayerEntity) {
            PacketManager.sendToClient(new StandControlStatusPacket(false, false), (ServerPlayerEntity) user);
        }
    }

    private void applySettingsFromUser() {
        LivingEntity user = getUser();
        if (user == null) {
            return;
        }
        IStandPower power = IStandPower.getStandPowerOptional(user).orElse(null);
        BeachBoySettingsEffect.get(power).ifPresent(this::copySettingsFromEffect);
    }

    public void copySettingsFromEffect(BeachBoySettingsEffect effect) {
        if (effect == null) {
            return;
        }
        entityData.set(DATA_BLOCK_COLLISION, effect.isPathLine());
        entityData.set(DATA_CRUSH_DAMAGE, effect.isCrushDamage());
        entityData.set(DATA_SOUND_SENSE, effect.isSoundSense());
        entityData.set(DATA_MAX_LENGTH, BeachBoyLine.clampLength(effect.getMaxLength(), this));
        LivingEntity user = getUser();
        if (user instanceof PlayerEntity) {
            syncLineSettingsToHook((PlayerEntity) user);
            if (!level.isClientSide) {
                syncRodVisuals((PlayerEntity) user);
            }
        }
    }

    private void ensureSingleRod(PlayerEntity user) {
        int count = countRods(user);
        if (count > 1) {
            int extra = count - 1;
            for (int i = 0; i < user.inventory.getContainerSize() && extra > 0; i++) {
                ItemStack stack = user.inventory.getItem(i);
                if (BeachBoyStandUtil.isRodItem(stack)) {
                    int removed = Math.min(extra, stack.getCount());
                    stack.shrink(removed);
                    extra -= removed;
                }
            }
            return;
        }
        if (count == 1) {
            return;
        }
        UUID trackerId = getRodTrackerId(user);
        if (trackerId != null) {
            TrackerItemStack tracker = SidedItemTrackerMap.getSidedTrackers(user.level).getTracker(trackerId);
            if (tracker != null && tracker.isTracked()) {
                unsummonMissingRod(user);
                return;
            }
        }
        giveRod(user);
    }

    private void unsummonMissingRod(PlayerEntity user) {
        IStandPower power = IStandPower.getStandPowerOptional(user).orElse(null);
        if (power != null && power.getType() != null) {
            power.getType().forceUnsummon(user, power);
        }
    }

    private void giveRod(PlayerEntity user) {
        ItemStack rod = new ItemStack(InitItems.BEACH_BOY_ROD.get());
        BeachBoyRodItem.setReel(rod, getMaxLineLength());
        BeachBoyRodItem.setCast(rod, BeachBoyStandUtil.findHook(user) != null);
        if (user instanceof ServerPlayerEntity) {
            TrackerItemStack tracker = TrackerItemStack.setTracked(rod, (ServerPlayerEntity) user);
            setRodTrackerId(user, tracker.getTrackerId());
        }
        int selected = user.inventory.selected;
        ItemStack held = user.inventory.getItem(selected);
        if (held.isEmpty()) {
            user.inventory.setItem(selected, rod);
            return;
        }
        if (user.inventory.add(rod)) {
            return;
        }
        user.drop(held.copy(), false);
        user.inventory.setItem(selected, rod);
    }

    private int countRods(PlayerEntity user) {
        int count = 0;
        for (int i = 0; i < user.inventory.getContainerSize(); i++) {
            ItemStack stack = user.inventory.getItem(i);
            if (BeachBoyStandUtil.isRodItem(stack)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private void syncLineSettingsToHook(PlayerEntity user) {
        BeachBoyHookEntity hook = BeachBoyStandUtil.findHook(user);
        if (hook == null) {
            return;
        }
        hook.setMaxLineLength(getMaxLineLength());
        hook.setLineBlockCollision(hasLineBlockCollision());
        hook.setLineThornsEnabled(isLineThornsEnabled());
        hook.setLineThornsMultiplier(getLineThornsMultiplier());
    }

    private void syncRodVisuals(PlayerEntity user) {
        boolean cast = BeachBoyStandUtil.findHook(user) != null || getWhipTicks() > 0;
        float reel = getMaxLineLength();
        BeachBoyHookEntity hook = BeachBoyStandUtil.findHook(user);
        if (hook != null) {
            reel = hook.getVisualReelLength();
        }
        boolean changed = false;
        for (int i = 0; i < user.inventory.getContainerSize(); i++) {
            ItemStack stack = user.inventory.getItem(i);
            if (!BeachBoyStandUtil.isRodItem(stack)) {
                continue;
            }
            if (BeachBoyRodItem.isCast(stack) != cast) {
                BeachBoyRodItem.setCast(stack, cast);
                changed = true;
            }
            if (Float.compare(BeachBoyRodItem.getReel(stack), reel) != 0) {
                BeachBoyRodItem.setReel(stack, reel);
                changed = true;
            }
        }
        if (changed) {
            user.inventory.setChanged();
        }
    }

    public float getMaxLineLength() {
        return entityData.get(DATA_MAX_LENGTH);
    }

    public void setMaxLineLength(float length) {
        float clamped = BeachBoyLine.clampLength(length, this);
        entityData.set(DATA_MAX_LENGTH, clamped);
        LivingEntity user = getUser();
        if (user instanceof PlayerEntity) {
            syncLineSettingsToHook((PlayerEntity) user);
            if (!level.isClientSide) {
                syncRodVisuals((PlayerEntity) user);
                IStandPower power = IStandPower.getStandPowerOptional(user).orElse(null);
                BeachBoySettingsEffect.get(power).ifPresent(effect -> {
                    if (Float.compare(effect.getMaxLength(), clamped) != 0) {
                        effect.setMaxLength(clamped);
                    }
                });
            }
        }
    }

    public float adjustMaxLineLength(float delta) {
        float next = getMaxLineLength() + delta;
        setMaxLineLength(next);
        return getMaxLineLength();
    }

    public boolean hasLineBlockCollision() {
        return entityData.get(DATA_BLOCK_COLLISION);
    }

    public void setLineBlockCollision(boolean collide) {
        entityData.set(DATA_BLOCK_COLLISION, collide);
        LivingEntity user = getUser();
        if (user instanceof PlayerEntity) {
            syncLineSettingsToHook((PlayerEntity) user);
        }
    }

    public boolean isLineThornsEnabled() {
        return entityData.get(DATA_THORNS);
    }

    public void setLineThornsEnabled(boolean enabled) {
        entityData.set(DATA_THORNS, enabled);
    }

    public float getLineThornsMultiplier() {
        return entityData.get(DATA_THORNS_MULTIPLIER);
    }

    public void setLineThornsMultiplier(float multiplier) {
        entityData.set(DATA_THORNS_MULTIPLIER, Math.max(0.0F, multiplier));
    }

    public boolean isSoundSenseEnabled() {
        return entityData.get(DATA_SOUND_SENSE);
    }

    public void setSoundSenseEnabled(boolean enabled) {
        entityData.set(DATA_SOUND_SENSE, enabled);
    }

    public boolean isCrushDamageEnabled() {
        return entityData.get(DATA_CRUSH_DAMAGE);
    }

    public void setCrushDamageEnabled(boolean enabled) {
        entityData.set(DATA_CRUSH_DAMAGE, enabled);
    }

    public void playWhip(boolean vertical) {
        entityData.set(DATA_WHIP_VERTICAL, vertical);
        if (vertical) {
            entityData.set(DATA_WHIP_UPWARD, !entityData.get(DATA_WHIP_UPWARD));
        }
        else {
            entityData.set(DATA_WHIP_LTR, !entityData.get(DATA_WHIP_LTR));
        }
        entityData.set(DATA_WHIP_TICKS, com.nextalubm.rotp_nextalbum.util.BeachBoyCombatUtil.WHIP_ANIM_TICKS);
    }

    public void stopWhip() {
        entityData.set(DATA_WHIP_TICKS, 0);
    }

    public int getWhipTicks() {
        return entityData.get(DATA_WHIP_TICKS);
    }

    public boolean isWhipVertical() {
        return entityData.get(DATA_WHIP_VERTICAL);
    }

    public boolean isWhipLeftToRight() {
        return entityData.get(DATA_WHIP_LTR);
    }

    public boolean isWhipUpward() {
        return entityData.get(DATA_WHIP_UPWARD);
    }

    private UUID getRodTrackerId(PlayerEntity user) {
        CompoundNBT data = user.getPersistentData();
        return data.hasUUID(ROD_TRACKER_KEY) ? data.getUUID(ROD_TRACKER_KEY) : null;
    }

    private void setRodTrackerId(PlayerEntity user, UUID uuid) {
        user.getPersistentData().putUUID(ROD_TRACKER_KEY, uuid);
    }

    private void removeAllRods(PlayerEntity user) {
        for (int i = 0; i < user.inventory.getContainerSize(); i++) {
            ItemStack stack = user.inventory.getItem(i);
            if (BeachBoyStandUtil.isRodItem(stack)) {
                user.inventory.setItem(i, ItemStack.EMPTY);
            }
        }
    }

    private void clearTrackedRod(PlayerEntity player) {
        UUID trackerId = getRodTrackerId(player);
        if (trackerId != null) {
            SidedItemTrackerMap trackerMap = SidedItemTrackerMap.getSidedTrackers(player.level);
            TrackerItemStack tracker = trackerMap.getTracker(trackerId);
            if (tracker != null) {
                ItemStack tracked = tracker.getItem();
                if (tracked != null && !tracked.isEmpty()) {
                    tracked.setCount(0);
                }
                trackerMap.removeTracker(trackerId);
            }
            player.getPersistentData().remove(ROD_TRACKER_KEY + "Most");
            player.getPersistentData().remove(ROD_TRACKER_KEY + "Least");
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource damageSrc) {
        return true;
    }

    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        this.addEffect(new EffectInstance(ModStatusEffects.FULL_INVISIBILITY.get(), Integer.MAX_VALUE, Integer.MAX_VALUE, false, false, false));
    }

    @Override
    public float getLeapStrength() {
        return 0.0F;
    }

    @Override
    protected double leapBaseStrength() {
        return 0.0D;
    }

    @Override
    public void addAdditionalSaveData(CompoundNBT nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putFloat("MaxLength", getMaxLineLength());
        nbt.putBoolean("BlockCollision", hasLineBlockCollision());
        nbt.putBoolean("Thorns", isLineThornsEnabled());
        nbt.putFloat("ThornsMul", getLineThornsMultiplier());
        nbt.putBoolean("SoundSense", isSoundSenseEnabled());
        nbt.putBoolean("CrushDamage", isCrushDamageEnabled());
    }

    @Override
    public void readAdditionalSaveData(CompoundNBT nbt) {
        super.readAdditionalSaveData(nbt);
        if (nbt.contains("MaxLength")) {
            setMaxLineLength(nbt.getFloat("MaxLength"));
        }
        setLineBlockCollision(nbt.getBoolean("BlockCollision"));
        setLineThornsEnabled(!nbt.contains("Thorns") || nbt.getBoolean("Thorns"));
        if (nbt.contains("ThornsMul")) {
            setLineThornsMultiplier(nbt.getFloat("ThornsMul"));
        }
        setSoundSenseEnabled(!nbt.contains("SoundSense") || nbt.getBoolean("SoundSense"));
        setCrushDamageEnabled(!nbt.contains("CrushDamage") || nbt.getBoolean("CrushDamage"));
    }

    @Override
    public void onRemovedFromWorld() {
        super.onRemovedFromWorld();
        if (level.isClientSide) {
            return;
        }
        LivingEntity user = getUser();
        if (user instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) user;
            BeachBoyHookEntity hook = BeachBoyStandUtil.findHook(player);
            if (hook != null) {
                hook.remove();
            }
            removeAllRods(player);
            clearTrackedRod(player);
        }
    }
}
