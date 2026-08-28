package com.nextalubm.rotp_nextalbum.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.nextalubm.rotp_nextalbum.init.InitEffects;
import com.nextalubm.rotp_nextalbum.network.BeachBoySwimPosePacket;
import com.nextalubm.rotp_nextalbum.network.NetworkHandler;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Pose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.PacketDistributor;

public final class BeachBoyPathSwim {
    private static final Map<UUID, Long> ACTIVE_UNTIL = new HashMap<UUID, Long>();
    private static final Map<UUID, Integer> LAST_SENT_REMAINING = new HashMap<UUID, Integer>();

    private BeachBoyPathSwim() {
    }

    public static boolean isActive(LivingEntity living) {
        return living != null && ACTIVE_UNTIL.containsKey(living.getUUID());
    }

    public static void set(Entity entity, boolean swim) {
        if (!(entity instanceof LivingEntity)) {
            return;
        }
        LivingEntity living = (LivingEntity) entity;
        if (swim) {
            ACTIVE_UNTIL.put(living.getUUID(), living.level.getGameTime() + 2L);
            apply(living);
            return;
        }
        clear(living);
    }

    public static void slam(LivingEntity living, int ticks) {
        if (living == null) {
            return;
        }
        ACTIVE_UNTIL.put(living.getUUID(), living.level.getGameTime() + Math.max(2, ticks));
        apply(living);
    }

    public static void keep(LivingEntity living) {
        if (living == null) {
            return;
        }
        Long until = ACTIVE_UNTIL.get(living.getUUID());
        if (until == null) {
            return;
        }
        if (living.level.getGameTime() > until) {
            clear(living);
            return;
        }
        applyPoseOnly(living);
    }

    public static void clear(LivingEntity living) {
        if (living == null) {
            return;
        }
        boolean had = ACTIVE_UNTIL.remove(living.getUUID()) != null;
        LAST_SENT_REMAINING.remove(living.getUUID());
        if (!had && !(living instanceof PlayerEntity && ((PlayerEntity) living).getForcedPose() == Pose.SWIMMING)) {
            return;
        }
        if (!living.level.isClientSide) {
            sync(living, 0);
        }
        if (otherSystemsForceSwim(living)) {
            return;
        }
        clearPoseOnly(living);
    }

    public static void applyFromPacket(Entity entity, int remainingTicks) {
        if (!(entity instanceof LivingEntity)) {
            return;
        }
        LivingEntity living = (LivingEntity) entity;
        if (remainingTicks <= 0) {
            ACTIVE_UNTIL.remove(living.getUUID());
            if (!otherSystemsForceSwim(living)) {
                clearPoseOnly(living);
            }
            return;
        }
        long until = remainingTicks >= 20
                ? living.level.getGameTime() + remainingTicks
                : Long.MAX_VALUE / 4L;
        ACTIVE_UNTIL.put(living.getUUID(), until);
        applyPoseOnly(living);
    }

    private static void apply(LivingEntity living) {
        applyPoseOnly(living);
        if (!living.level.isClientSide) {
            sync(living, remainingTicks(living));
        }
    }

    private static int remainingTicks(LivingEntity living) {
        Long until = ACTIVE_UNTIL.get(living.getUUID());
        if (until == null) {
            return 0;
        }
        return (int) Math.max(1L, until - living.level.getGameTime());
    }

    private static void sync(LivingEntity living, int remainingTicks) {
        if (living.level.isClientSide || !(living.level instanceof ServerWorld)) {
            return;
        }
        Integer last = LAST_SENT_REMAINING.get(living.getUUID());
        if (last != null && remainingTicks > 0 && last > 0 && remainingTicks <= last) {
            return;
        }
        LAST_SENT_REMAINING.put(living.getUUID(), remainingTicks);
        NetworkHandler.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> living),
                new BeachBoySwimPosePacket(living.getId(), remainingTicks));
    }

    private static boolean otherSystemsForceSwim(LivingEntity living) {
        if (living.getPersistentData().getBoolean(AgingEntityUtil.NBT_FORCED_SWIM)) {
            return true;
        }
        if (living.hasEffect(InitEffects.COLLAPSE.get()) && !TheGratefulDeadGrabUtil.isEntityGrabbed(living)) {
            return true;
        }
        return false;
    }

    private static void applyPoseOnly(LivingEntity living) {
        living.setSwimming(true);
        living.setPose(Pose.SWIMMING);
        if (living instanceof PlayerEntity) {
            ((PlayerEntity) living).setForcedPose(Pose.SWIMMING);
        }
        living.refreshDimensions();
    }

    private static void clearPoseOnly(LivingEntity living) {
        living.setSwimming(false);
        if (living instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) living;
            if (player.getForcedPose() == Pose.SWIMMING) {
                player.setForcedPose(null);
            }
        }
        if (living.getPose() == Pose.SWIMMING && !living.isInWater()) {
            living.setPose(Pose.STANDING);
        }
        living.refreshDimensions();
    }
}
