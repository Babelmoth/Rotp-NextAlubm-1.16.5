package com.nextalubm.rotp_nextalbum.client;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.nextalubm.rotp_nextalbum.entity.SexPistolsEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.vector.Vector3d;

public class SexPistolsAnimationClientHandler {
    private static final int PENDING_SUMMON_TICKS = 40;
    private static final int PENDING_KICK_TICKS = 80;
    private static final Map<Integer, PendingSummon> PENDING_SUMMONS = new HashMap<>();
    private static final Map<Integer, PendingKick> PENDING_KICKS = new HashMap<>();

    public static void playSummon(int entityId, int idleVariant, int summonVariant) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        Entity entity = mc.level.getEntity(entityId);
        if (entity instanceof SexPistolsEntity) {
            ((SexPistolsEntity) entity).playSummonAnimation(idleVariant, summonVariant);
        }
        else {
            PENDING_SUMMONS.put(entityId, new PendingSummon(idleVariant, summonVariant));
        }
    }

    public static boolean consumePendingSummon(SexPistolsEntity sexPistols) {
        PendingSummon pending = PENDING_SUMMONS.remove(sexPistols.getId());
        if (pending == null) {
            return false;
        }
        sexPistols.playSummonAnimation(pending.idleVariant, pending.summonVariant);
        return true;
    }

    public static void tickPendingSummons() {
        Iterator<Map.Entry<Integer, PendingSummon>> summonIterator = PENDING_SUMMONS.entrySet().iterator();
        while (summonIterator.hasNext()) {
            PendingSummon pending = summonIterator.next().getValue();
            pending.ticksLeft--;
            if (pending.ticksLeft <= 0) {
                summonIterator.remove();
            }
        }
        Iterator<Map.Entry<Integer, PendingKick>> kickIterator = PENDING_KICKS.entrySet().iterator();
        while (kickIterator.hasNext()) {
            PendingKick pending = kickIterator.next().getValue();
            pending.ticksLeft--;
            if (pending.ticksLeft <= 0) {
                kickIterator.remove();
            }
        }
    }

    public static void playKick(int entityId, double directionX, double directionZ) {
        playKick(entityId, directionX, directionZ, false);
    }

    public static void playKick(int entityId, double directionX, double directionZ, boolean returnToUser) {
        playKick(entityId, directionX, directionZ, returnToUser, false, 0.0D, 0.0D, 0.0D);
    }

    public static void playKick(int entityId, double directionX, double directionZ, boolean returnToUser, boolean hasPosition, double positionX, double positionY, double positionZ) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            PENDING_KICKS.put(entityId, new PendingKick(directionX, directionZ, returnToUser, hasPosition, positionX, positionY, positionZ));
            return;
        }
        Entity entity = mc.level.getEntity(entityId);
        if (entity instanceof SexPistolsEntity) {
            playKick((SexPistolsEntity) entity, directionX, directionZ, returnToUser, hasPosition, positionX, positionY, positionZ);
        }
        else {
            PENDING_KICKS.put(entityId, new PendingKick(directionX, directionZ, returnToUser, hasPosition, positionX, positionY, positionZ));
        }
    }

    public static boolean consumePendingKick(SexPistolsEntity sexPistols) {
        PendingKick pending = PENDING_KICKS.remove(sexPistols.getId());
        if (pending == null) {
            return false;
        }
        playKick(sexPistols, pending.directionX, pending.directionZ, pending.returnToUser, pending.hasPosition, pending.positionX, pending.positionY, pending.positionZ);
        return true;
    }

    private static void playKick(SexPistolsEntity sexPistols, double directionX, double directionZ, boolean returnToUser, boolean hasPosition, double positionX, double positionY, double positionZ) {
        if (hasPosition) {
            sexPistols.setPos(positionX, positionY, positionZ);
            sexPistols.xOld = positionX;
            sexPistols.yOld = positionY;
            sexPistols.zOld = positionZ;
        }
        sexPistols.playKickAnimation(new Vector3d(directionX, 0.0D, directionZ));
        if (returnToUser) {
            sexPistols.returnToUserAfterKick();
        }
    }

    private static class PendingSummon {
        private final int idleVariant;
        private final int summonVariant;
        private int ticksLeft = PENDING_SUMMON_TICKS;

        private PendingSummon(int idleVariant, int summonVariant) {
            this.idleVariant = idleVariant;
            this.summonVariant = summonVariant;
        }
    }

    private static class PendingKick {
        private final double directionX;
        private final double directionZ;
        private final boolean returnToUser;
        private final boolean hasPosition;
        private final double positionX;
        private final double positionY;
        private final double positionZ;
        private int ticksLeft = PENDING_KICK_TICKS;

        private PendingKick(double directionX, double directionZ, boolean returnToUser, boolean hasPosition, double positionX, double positionY, double positionZ) {
            this.directionX = directionX;
            this.directionZ = directionZ;
            this.returnToUser = returnToUser;
            this.hasPosition = hasPosition;
            this.positionX = positionX;
            this.positionY = positionY;
            this.positionZ = positionZ;
        }
    }
}