package com.nextalubm.rotp_nextalbum.client;

import javax.annotation.Nullable;

import com.nextalubm.rotp_nextalbum.init.InitItems;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.HandSide;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;

public final class BeachBoyClientRodTips {
    public static final double FIRST_PERSON_SIDE = 0.05D;
    public static final double FIRST_PERSON_UP = 0.22D;
    public static final double FIRST_PERSON_FORWARD = 0.36D;

    private BeachBoyClientRodTips() {
    }

    @Nullable
    public static Vector3d tryFirstPerson(LivingEntity owner, float partialTicks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || owner != mc.player || mc.options.getCameraType() == null
                || !mc.options.getCameraType().isFirstPerson() || mc.getCameraEntity() != mc.player) {
            return null;
        }
        PlayerEntity player = mc.player;
        int side = player.getMainArm() == HandSide.RIGHT ? 1 : -1;
        if (player.getOffhandItem().getItem() == InitItems.BEACH_BOY_ROD.get()
                && player.getMainHandItem().getItem() != InitItems.BEACH_BOY_ROD.get()) {
            side = -side;
        }
        float pitch = MathHelper.lerp(partialTicks, player.xRotO, player.xRot);
        float yaw = MathHelper.lerp(partialTicks, player.yRotO, player.yRot);
        Vector3d look = Vector3d.directionFromRotation(pitch, yaw);
        Vector3d right = new Vector3d(-look.z, 0.0D, look.x);
        if (right.lengthSqr() < 1.0E-6D) {
            right = Vector3d.directionFromRotation(0.0F, yaw + 90.0F);
        }
        right = right.normalize();
        Vector3d up = right.cross(look);
        if (up.lengthSqr() < 1.0E-6D) {
            up = new Vector3d(0.0D, 1.0D, 0.0D);
        }
        else {
            up = up.normalize();
        }

        float swing = player.getAttackAnim(partialTicks);
        float swingPow = MathHelper.sin(MathHelper.sqrt(swing) * (float) Math.PI);
        double sideOff = FIRST_PERSON_SIDE * side * (1.0D - 0.25D * swingPow);
        double upOff = FIRST_PERSON_UP - 0.18D * swingPow;
        double forwardOff = FIRST_PERSON_FORWARD - 0.08D * swingPow;

        Vector3d eye = new Vector3d(
                MathHelper.lerp(partialTicks, player.xo, player.getX()),
                MathHelper.lerp(partialTicks, player.yo, player.getY()) + (double) player.getEyeHeight(),
                MathHelper.lerp(partialTicks, player.zo, player.getZ()));
        return eye.add(look.scale(forwardOff)).add(right.scale(sideOff)).add(up.scale(upOff));
    }
}
