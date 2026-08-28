package com.nextalubm.rotp_nextalbum.client;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.github.standobyte.jojo.action.stand.StandEntityHeavyAttack;
import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.nextalubm.rotp_nextalbum.util.AgingEntityUtil;
import com.nextalubm.rotp_nextalbum.util.TheGratefulDeadGrabUtil;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;

public final class ClientGrabState {

    public static final class GrabRecord {
        public final int grabberId;
        public final int grabbedId;
        public final float struggle;
        public final boolean blockGrab;
        public final BlockPos blockPos;
        public final int blockStateId;
        public final boolean grabbing;

        public GrabRecord(int grabberId, int grabbedId, float struggle, boolean blockGrab,
                BlockPos blockPos, int blockStateId, boolean grabbing) {
            this.grabberId = grabberId;
            this.grabbedId = grabbedId;
            this.struggle = struggle;
            this.blockGrab = blockGrab;
            this.blockPos = blockPos;
            this.blockStateId = blockStateId;
            this.grabbing = grabbing;
        }
    }

    private static final Map<Integer, GrabRecord> BY_GRABBER = new HashMap<>();
    private static final Map<Integer, Integer> GRABBED_TO_GRABBER = new HashMap<>();

    private static int localGrabbedBy = -1;
    private static float localStruggle = 0F;
    private static boolean pendingAgingInject;

    private ClientGrabState() {
    }

    public static void handleGrabSync(int grabber, int grabbed, float struggleAmount,
            boolean isBlockGrab, int bx, int by, int bz, int blockStateId, boolean grabbing) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) {
            return;
        }

        boolean wasGrabbing = isUserGrabbing(grabber);
        GrabRecord previous = BY_GRABBER.remove(grabber);
        if (previous != null && previous.grabbedId != -1) {
            GRABBED_TO_GRABBER.remove(previous.grabbedId, grabber);
        }

        if (mc.player != null && previous != null && previous.grabbedId == mc.player.getId()) {
            localGrabbedBy = -1;
            localStruggle = 0F;
        }

        if (!grabbing) {
            if (wasGrabbing && mc.player != null && grabber == mc.player.getId() && !previousBlockGrab(previous)) {
                StandEntity stand = TheGratefulDeadGrabUtil.getStandEntity(mc.player);
                if (stand != null && stand.getCurrentTask()
                        .map(task -> task.getAction() instanceof StandEntityHeavyAttack)
                        .orElse(false)) {
                    pendingAgingInject = true;
                }
            }
            refreshDownedPose(mc, previous, null);
            return;
        }

        BlockPos blockPos = isBlockGrab ? new BlockPos(bx, by, bz) : null;
        GrabRecord record = new GrabRecord(grabber, grabbed, struggleAmount, isBlockGrab, blockPos, blockStateId, true);
        BY_GRABBER.put(grabber, record);
        if (grabbed != -1) {
            GRABBED_TO_GRABBER.put(grabbed, grabber);
        }

        if (mc.player != null && grabbed == mc.player.getId()) {
            localGrabbedBy = grabber;
            localStruggle = struggleAmount;
        }
        refreshDownedPose(mc, previous, record);
    }

    private static void refreshDownedPose(Minecraft mc, GrabRecord previous, GrabRecord next) {
        if (mc.level == null) {
            return;
        }
        int previousGrabbed = previous == null ? -1 : previous.grabbedId;
        int nextGrabbed = next == null ? -1 : next.grabbedId;
        if (previousGrabbed != -1 && mc.level.getEntity(previousGrabbed) instanceof LivingEntity) {
            AgingEntityUtil.syncDownedPose((LivingEntity) mc.level.getEntity(previousGrabbed));
        }
        if (nextGrabbed != -1 && nextGrabbed != previousGrabbed
                && mc.level.getEntity(nextGrabbed) instanceof LivingEntity) {
            AgingEntityUtil.syncDownedPose((LivingEntity) mc.level.getEntity(nextGrabbed));
        }
        if (mc.player != null && (previousGrabbed == mc.player.getId() || nextGrabbed == mc.player.getId())) {
            AgingEntityUtil.syncDownedPose(mc.player);
        }
    }

    @Nullable
    public static GrabRecord getRecord(int grabberId) {
        return BY_GRABBER.get(grabberId);
    }

    @Nullable
    public static GrabRecord getRecordForGrabbed(int grabbedId) {
        Integer grabberId = GRABBED_TO_GRABBER.get(grabbedId);
        return grabberId == null ? null : BY_GRABBER.get(grabberId);
    }

    @Nullable
    public static LivingEntity getGrabberOf(LivingEntity grabbed) {
        if (grabbed == null || grabbed.level == null) {
            return null;
        }
        GrabRecord record = getRecordForGrabbed(grabbed.getId());
        if (record == null || !record.grabbing || record.blockGrab) {
            return null;
        }
        if (grabbed.level.getEntity(record.grabberId) instanceof LivingEntity) {
            return (LivingEntity) grabbed.level.getEntity(record.grabberId);
        }
        return null;
    }

    @Nullable
    public static LivingEntity getGrabber() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return null;
        }
        GrabRecord record = BY_GRABBER.get(mc.player.getId());
        if (record == null) {
            return null;
        }
        return mc.player;
    }

    @Nullable
    public static LivingEntity getGrabbed() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return null;
        }
        GrabRecord record = BY_GRABBER.get(mc.player.getId());
        if (record == null || record.grabbedId == -1) {
            return null;
        }
        if (mc.level.getEntity(record.grabbedId) instanceof LivingEntity) {
            return (LivingEntity) mc.level.getEntity(record.grabbedId);
        }
        return null;
    }

    public static boolean isUserGrabbing(int userId) {
        GrabRecord record = BY_GRABBER.get(userId);
        return record != null && record.grabbing;
    }

    public static boolean isGrabbing() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return false;
        }
        return isUserGrabbing(mc.player.getId());
    }

    public static boolean isGrabbingBlock() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return false;
        }
        GrabRecord record = BY_GRABBER.get(mc.player.getId());
        return record != null && record.grabbing && record.blockGrab;
    }

    @Nullable
    public static BlockPos getGrabbedBlock() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return null;
        }
        GrabRecord record = BY_GRABBER.get(mc.player.getId());
        return record == null ? null : record.blockPos;
    }

    @Nullable
    public static BlockState getHeldBlockState(int grabberId) {
        GrabRecord record = BY_GRABBER.get(grabberId);
        if (record == null || !record.blockGrab) {
            return null;
        }
        BlockState state = net.minecraft.block.Block.stateById(record.blockStateId);
        return state.isAir() ? Blocks.AIR.defaultBlockState() : state;
    }

    public static float getStruggleProgress() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return 0F;
        }
        GrabRecord record = BY_GRABBER.get(mc.player.getId());
        return record == null ? 0F : record.struggle;
    }

    public static boolean isLocalPlayerGrabbed() {
        return localGrabbedBy != -1;
    }

    public static int getLocalGrabbedBy() {
        return localGrabbedBy;
    }

    public static float getLocalStruggle() {
        return localStruggle;
    }

    public static boolean isEntityGrabbed(LivingEntity entity) {
        return entity != null && getGrabberOf(entity) != null;
    }

    public static boolean canShowAgingInject() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return false;
        }
        StandEntity stand = TheGratefulDeadGrabUtil.getStandEntity(mc.player);
        boolean heavyTask = stand != null && stand.getCurrentTask()
                .map(task -> task.getAction() instanceof StandEntityHeavyAttack)
                .orElse(false);
        if (!heavyTask) {
            pendingAgingInject = false;
            return false;
        }
        if (isGrabbing() && !isGrabbingBlock()) {
            return true;
        }
        return pendingAgingInject;
    }

    public static void clearPendingAgingInject() {
        pendingAgingInject = false;
    }

    private static boolean previousBlockGrab(GrabRecord previous) {
        return previous != null && previous.blockGrab;
    }

    public static void clear() {
        BY_GRABBER.clear();
        GRABBED_TO_GRABBER.clear();
        localGrabbedBy = -1;
        localStruggle = 0F;
        pendingAgingInject = false;
    }

    public static void clearIfInvolved(int entityId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getId() == entityId) {
            clear();
            return;
        }
        GrabRecord asGrabber = BY_GRABBER.get(entityId);
        if (asGrabber != null) {
            handleGrabSync(entityId, -1, 0F, false, 0, 0, 0, 0, false);
        }
        GrabRecord asVictim = getRecordForGrabbed(entityId);
        if (asVictim != null) {
            handleGrabSync(asVictim.grabberId, -1, 0F, false, 0, 0, 0, 0, false);
        }
    }
}
