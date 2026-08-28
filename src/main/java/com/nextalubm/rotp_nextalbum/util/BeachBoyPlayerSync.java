package com.nextalubm.rotp_nextalbum.util;

import java.util.EnumSet;
import java.util.Set;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.server.SPlayerPositionLookPacket;

public final class BeachBoyPlayerSync {
    private static final Set<SPlayerPositionLookPacket.Flags> KEEP_LOOK = EnumSet.of(
            SPlayerPositionLookPacket.Flags.X_ROT,
            SPlayerPositionLookPacket.Flags.Y_ROT);
    private static final int NO_AWAIT_TELEPORT_ID = Integer.MIN_VALUE;

    private BeachBoyPlayerSync() {
    }

    public static void sendPositionKeepLook(ServerPlayerEntity player) {
        if (player == null || player.connection == null) {
            return;
        }
        player.hurtMarked = true;
        player.hasImpulse = true;
        player.connection.send(new SPlayerPositionLookPacket(
                player.getX(), player.getY(), player.getZ(),
                0.0F, 0.0F,
                KEEP_LOOK,
                NO_AWAIT_TELEPORT_ID));
    }
}
