package com.nextalubm.rotp_nextalbum.client;

import com.nextalubm.rotp_nextalbum.util.BeachBoyPathSwim;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

public final class ClientSwimPose {
    private ClientSwimPose() {
    }

    public static void handle(int entityId, int remainingTicks) {
        World world = Minecraft.getInstance().level;
        if (world == null) {
            return;
        }
        Entity entity = world.getEntity(entityId);
        BeachBoyPathSwim.applyFromPacket(entity, remainingTicks);
    }
}
