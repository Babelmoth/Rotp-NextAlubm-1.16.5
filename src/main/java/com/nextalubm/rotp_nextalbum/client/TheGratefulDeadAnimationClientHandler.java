package com.nextalubm.rotp_nextalbum.client;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.nextalubm.rotp_nextalbum.entity.TheGratefulDeadEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;

public class TheGratefulDeadAnimationClientHandler {
    private static final int PENDING_SUMMON_TICKS = 40;
    private static final Map<Integer, PendingSummon> PENDING_SUMMONS = new HashMap<>();

    public static void playSummon(int entityId, int idleVariant, int summonVariant) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        Entity entity = mc.level.getEntity(entityId);
        if (entity instanceof TheGratefulDeadEntity) {
            ((TheGratefulDeadEntity) entity).playSummonAnimation(idleVariant, summonVariant);
        }
        else {
            PENDING_SUMMONS.put(entityId, new PendingSummon(idleVariant, summonVariant));
        }
    }

    public static boolean consumePendingSummon(TheGratefulDeadEntity theGratefulDead) {
        PendingSummon pending = PENDING_SUMMONS.remove(theGratefulDead.getId());
        if (pending == null) {
            return false;
        }
        theGratefulDead.playSummonAnimation(pending.idleVariant, pending.summonVariant);
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
}