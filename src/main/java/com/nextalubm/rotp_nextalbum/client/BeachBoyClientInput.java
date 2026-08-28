package com.nextalubm.rotp_nextalbum.client;

import com.nextalubm.rotp_nextalbum.entity.BeachBoyHookEntity;
import com.nextalubm.rotp_nextalbum.entity.BeachBoyStandEntity;
import com.nextalubm.rotp_nextalbum.util.BeachBoyStandUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.MovementInput;

public final class BeachBoyClientInput {
    private BeachBoyClientInput() {
    }

    public static boolean applyIfRemote(BeachBoyHookEntity hook) {
        Minecraft mc = Minecraft.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null || hook.getOwner() != player) {
            return false;
        }
        BeachBoyStandEntity stand = BeachBoyStandUtil.getStand(player);
        if (stand == null || !stand.isManuallyControlled()) {
            return false;
        }
        MovementInput input = player.input;
        if (input == null) {
            return false;
        }
        float[] look = captureViewRotation(player, stand);
        float[] move = captureMove(input, stand);
        hook.setRemoteInput(move[0], move[1], move[2] > 0.5F, move[3] > 0.5F, look[0], look[1]);
        return true;
    }

    public static float[] captureViewRotation(ClientPlayerEntity player, BeachBoyStandEntity stand) {
        Minecraft mc = Minecraft.getInstance();
        Entity cameraEntity = mc.getCameraEntity();
        if (cameraEntity == null) {
            cameraEntity = stand != null ? stand : player;
        }
        return new float[] { cameraEntity.yRot, cameraEntity.xRot };
    }

    public static float[] captureMove(MovementInput input, BeachBoyStandEntity stand) {
        float forward = input.forwardImpulse;
        float strafe = input.leftImpulse;
        boolean jump = input.jumping;
        boolean sneak = input.shiftKeyDown;
        if (stand != null) {
            if (Math.abs(stand.zza) > Math.abs(forward)) {
                forward = stand.zza;
            }
            if (Math.abs(stand.xxa) > Math.abs(strafe)) {
                strafe = stand.xxa;
            }
            jump = jump || stand.isControlJumping();
            sneak = sneak || stand.isControlSneaking();
        }
        return new float[] { forward, strafe, jump ? 1.0F : 0.0F, sneak ? 1.0F : 0.0F };
    }
}
