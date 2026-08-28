package com.nextalubm.rotp_nextalbum.entity;

import java.util.Random;

import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.entity.stand.StandEntityType;
import com.github.standobyte.jojo.entity.stand.StandPose;
import com.github.standobyte.jojo.entity.stand.StandRelativeOffset;
import com.nextalubm.rotp_nextalbum.client.TheGratefulDeadAnimationClientHandler;
import com.nextalubm.rotp_nextalbum.network.NetworkHandler;
import com.nextalubm.rotp_nextalbum.network.TheGratefulDeadSummonAnimationPacket;
import com.nextalubm.rotp_nextalbum.util.TheGratefulDeadGrabUtil;

import net.minecraft.entity.LivingEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.network.PacketDistributor;

public class TheGratefulDeadEntity extends StandEntity implements IEntityAdditionalSpawnData {

    public static final StandPose IDLE_RANDOM_POSE = new StandPose("idleRandom");

    public static final int ANIMATION_NONE = 0;
    public static final int ANIMATION_SUMMON = 1;
    public static final int ANIMATION_IDLE_RANDOM = 2;

    private static final int IDLE_ANIMATION_VARIANTS = 1;
    private static final int SUMMON_ANIMATION_VARIANTS = 1;
    private static final int IDLE_RANDOM_ANIMATION_VARIANTS = 1;

    private static final int SUMMON_ANIMATION_TICKS = 40;
    private static final int IDLE_RANDOM_TICKS = 20;
    private static final int RECOVERY_ANIMATION_TICKS = 7;

    private static final int IDLE_RANDOM_MIN_DELAY = 100;
    private static final int IDLE_RANDOM_EXTRA_DELAY = 200;

    private int idleAnimationVariant;
    private int summonAnimationVariant;
    private int idleRandomVariant;

    private int summonAnimationTicks;
    private int idleRandomTicks;
    private int idleRandomDelay;

    private int recoveryAnimationType;
    private int recoveryAnimationVariant;
    private int recoveryAnimationTicks;
    private int recoveryAnimationStartTick;

    public TheGratefulDeadEntity(StandEntityType<? extends TheGratefulDeadEntity> type, World world) {
        super(type, world);
        idleAnimationVariant = getRandom().nextInt(IDLE_ANIMATION_VARIANTS);
        summonAnimationVariant = getRandom().nextInt(SUMMON_ANIMATION_VARIANTS);
    }

    @Override
    public void setStandPose(StandPose pose) {
        super.setStandPose(pose);
        if (pose != StandPose.IDLE && pose != StandPose.SUMMON && pose != IDLE_RANDOM_POSE) {
            stopSummonAndIdleAnimations();
        }
    }

    private void stopSummonAndIdleAnimations() {
        this.summonAnimationTicks = 0;
        this.idleRandomTicks = 0;
        this.recoveryAnimationTicks = 0;
        this.recoveryAnimationType = ANIMATION_NONE;
    }

    @Override
    public void tick() {
        super.tick();

        if (level.isClientSide) {
            TheGratefulDeadAnimationClientHandler.consumePendingSummon(this);
            TheGratefulDeadAnimationClientHandler.tickPendingSummons();
        }

        tickSummonAnimation();
        tickIdleRandomAnimation();
        tickRecoveryAnimation();
    }


    public int getIdleAnimationVariant() {
        return idleAnimationVariant;
    }

    public int getSummonAnimationVariant() {
        return summonAnimationVariant;
    }

    public int getIdleRandomVariant() {
        return idleRandomVariant;
    }

    public int getRecoveryAnimationType() {
        return recoveryAnimationType;
    }

    public int getRecoveryAnimationVariant() {
        return recoveryAnimationVariant;
    }

    @Override
    public int getSummonPoseRandomByte() {
        return summonAnimationVariant;
    }

    public boolean isSummonAnimationActive() {
        return summonAnimationTicks > 0;
    }

    public float getSummonAnimationSeconds(float partialTick) {
        if (summonAnimationTicks <= 0) {
            return (float) SUMMON_ANIMATION_TICKS / 20.0F;
        }
        return MathHelper.clamp((float) SUMMON_ANIMATION_TICKS - (float) summonAnimationTicks + partialTick, 0.0F, (float) SUMMON_ANIMATION_TICKS) / 20.0F;
    }

    public float getSummonIdleBlend(float partialTick) {
        if (!isSummonAnimationActive()) {
            return 0.0F;
        }
        float progress = getSummonAnimationSeconds(partialTick) / ((float) SUMMON_ANIMATION_TICKS / 20.0F);
        if (progress < 0.1F) {
            return progress / 0.1F;
        }
        if (progress > 0.85F) {
            return 1.0F - ((progress - 0.85F) / 0.15F);
        }
        return 1.0F;
    }

    public float getIdleRandomAnimationSeconds(float partialTick) {
        if (idleRandomTicks <= 0) {
            return (float) IDLE_RANDOM_TICKS / 20.0F;
        }
        return MathHelper.clamp((float) IDLE_RANDOM_TICKS - (float) idleRandomTicks + partialTick, 0.0F, (float) IDLE_RANDOM_TICKS) / 20.0F;
    }

    public float getIdleRandomAnimationBlend(float partialTick) {
        if (idleRandomTicks <= 0) {
            return 0.0F;
        }
        float elapsed = MathHelper.clamp((float) IDLE_RANDOM_TICKS - (float) idleRandomTicks + partialTick, 0.0F, (float) IDLE_RANDOM_TICKS);
        float in = MathHelper.clamp(elapsed / 3.0F, 0.0F, 1.0F);
        return in * in * (3.0F - 2.0F * in);
    }

    public float getRecoveryAnimationBlend(float partialTick) {
        if (recoveryAnimationTicks <= 0 || recoveryAnimationType == ANIMATION_NONE) {
            return 0.0F;
        }
        float raw = MathHelper.clamp(((float) recoveryAnimationTicks + 1.0F - partialTick) / ((float) RECOVERY_ANIMATION_TICKS + 1.0F), 0.0F, 1.0F);
        return raw * raw * (3.0F - 2.0F * raw);
    }


    public void playSummonAnimation() {
        idleAnimationVariant = getRandom().nextInt(IDLE_ANIMATION_VARIANTS);
        summonAnimationVariant = getRandom().nextInt(SUMMON_ANIMATION_VARIANTS);
        startSummonAnimation();
        sendSummonPacketToClients();
    }

    public void playSummonAnimation(int idleVariant, int summonVariant) {
        idleAnimationVariant = Math.floorMod(idleVariant, IDLE_ANIMATION_VARIANTS);
        summonAnimationVariant = Math.floorMod(summonVariant, SUMMON_ANIMATION_VARIANTS);
        startSummonAnimation();
        sendSummonPacketToClients();
    }

    private void sendSummonPacketToClients() {
        if (!level.isClientSide) {
            NetworkHandler.CHANNEL.send(
                    PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> this),
                    new TheGratefulDeadSummonAnimationPacket(getId(), idleAnimationVariant, summonAnimationVariant)
            );
        }
    }

    private void startSummonAnimation() {
        idleRandomTicks = 0;
        recoveryAnimationTicks = 0;
        recoveryAnimationStartTick = 0;
        recoveryAnimationType = ANIMATION_NONE;
        summonAnimationTicks = SUMMON_ANIMATION_TICKS;
        setStandPose(StandPose.SUMMON);
        resetIdleRandomDelay();
    }

    private void startRecoveryAnimation(int type, int variant) {
        recoveryAnimationType = type;
        recoveryAnimationVariant = variant;
        recoveryAnimationTicks = RECOVERY_ANIMATION_TICKS;
        recoveryAnimationStartTick = tickCount;
    }


    private void tickSummonAnimation() {
        if (summonAnimationTicks <= 0) {
            return;
        }
        StandPose pose = getStandPose();
        if (pose != StandPose.SUMMON && pose != StandPose.IDLE && pose != IDLE_RANDOM_POSE) {
            stopSummonAndIdleAnimations();
            return;
        }
        if (pose != StandPose.SUMMON && !isArmsOnlyMode()) {
            setStandPose(StandPose.SUMMON);
        }
        summonAnimationTicks--;
        if (summonAnimationTicks <= 0) {
            startRecoveryAnimation(ANIMATION_SUMMON, summonAnimationVariant);
            if (getStandPose() == StandPose.SUMMON) {
                setStandPose(StandPose.IDLE);
            }
        }
    }

    private void tickIdleRandomAnimation() {
        if (!level.isClientSide) {
            return;
        }
        if (summonAnimationTicks > 0) {
            return;
        }

        if (idleRandomTicks > 0) {
            StandPose pose = getStandPose();
            if (pose != StandPose.IDLE && pose != IDLE_RANDOM_POSE) {
                idleRandomTicks = 0;
                return;
            }
            if (pose != IDLE_RANDOM_POSE) {
                setStandPose(IDLE_RANDOM_POSE);
            }
            idleRandomTicks--;
            if (idleRandomTicks <= 0) {
                startRecoveryAnimation(ANIMATION_IDLE_RANDOM, idleRandomVariant);
                if (getStandPose() == IDLE_RANDOM_POSE) {
                    setStandPose(StandPose.IDLE);
                }
                resetIdleRandomDelay();
            }
            return;
        }

        if (getStandPose() != StandPose.IDLE || recoveryAnimationTicks > 0) {
            resetIdleRandomDelay();
            return;
        }

        if (idleRandomDelay <= 0) {
            resetIdleRandomDelay();
        }
        idleRandomDelay--;

        if (idleRandomDelay <= 0) {
            Random random = getRandom();
            idleRandomVariant = random.nextInt(IDLE_RANDOM_ANIMATION_VARIANTS);
            idleRandomTicks = IDLE_RANDOM_TICKS;
            recoveryAnimationTicks = 0;
            recoveryAnimationStartTick = 0;
            recoveryAnimationType = ANIMATION_NONE;
            setStandPose(IDLE_RANDOM_POSE);
        }
    }

    private void tickRecoveryAnimation() {
        if (recoveryAnimationTicks > 0 && summonAnimationTicks <= 0 && idleRandomTicks <= 0) {
            if (tickCount > recoveryAnimationStartTick) {
                recoveryAnimationTicks--;
            }
            if (recoveryAnimationTicks <= 0) {
                recoveryAnimationType = ANIMATION_NONE;
            }
        }
    }

    private void resetIdleRandomDelay() {
        idleRandomDelay = IDLE_RANDOM_MIN_DELAY + getRandom().nextInt(IDLE_RANDOM_EXTRA_DELAY + 1);
    }

    @Override
    public void defaultRotation() {
        if (TheGratefulDeadGrabUtil.isGrabbing(getUserPower())) {
            LivingEntity look = TheGratefulDeadGrabUtil.getLookSource(this);
            if (look != null) {
                setRot(look.yRot, look.xRot);
                setYHeadRot(this.yRot);
                return;
            }
        }
        super.defaultRotation();
    }

    @Override
    public StandRelativeOffset getDefaultOffsetFromUser() {
        if (TheGratefulDeadGrabUtil.isGrabbing(getUserPower()) && !isArmsOnlyMode()) {
            return StandRelativeOffset.withYOffset(-0.45D, 0.25D, 1.15D);
        }
        return super.getDefaultOffsetFromUser();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }


    @Override
    public void writeSpawnData(PacketBuffer buffer) {
        super.writeSpawnData(buffer);
        buffer.writeVarInt(idleAnimationVariant);
        buffer.writeVarInt(summonAnimationVariant);
    }

    @Override
    public void readSpawnData(PacketBuffer additionalData) {
        super.readSpawnData(additionalData);
        idleAnimationVariant = Math.floorMod(additionalData.readVarInt(), IDLE_ANIMATION_VARIANTS);
        summonAnimationVariant = Math.floorMod(additionalData.readVarInt(), SUMMON_ANIMATION_VARIANTS);
        startSummonAnimation();
    }
}