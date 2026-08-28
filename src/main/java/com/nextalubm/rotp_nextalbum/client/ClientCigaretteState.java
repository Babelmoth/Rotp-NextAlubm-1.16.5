package com.nextalubm.rotp_nextalbum.client;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.PointOfView;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;

public final class ClientCigaretteState {
    private static final Map<Integer, Integer> MOUTH_COUNTS = new HashMap<>();

    private ClientCigaretteState() {
    }

    public static void handleMouthSync(int entityId, int count) {
        if (count <= 0) {
            MOUTH_COUNTS.remove(entityId);
        }
        else {
            MOUTH_COUNTS.put(entityId, count);
        }
    }

    public static int getMouthCount(int entityId) {
        Integer count = MOUTH_COUNTS.get(entityId);
        return count == null ? 0 : count.intValue();
    }

    public static void handleSmoke(int entityId, int intensity) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        Entity entity = mc.level.getEntity(entityId);
        if (!(entity instanceof LivingEntity)) {
            return;
        }
        LivingEntity living = (LivingEntity) entity;
        Vector3d look = living.getLookAngle();
        Vector3d mouth = living.getEyePosition(1.0F).add(look.scale(0.35D));
        int count = Math.max(4, Math.min(28, intensity / 3));
        for (int i = 0; i < count; i++) {
            double spread = 0.04D + intensity * 0.0008D;
            mc.level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    mouth.x, mouth.y - 0.12D, mouth.z,
                    look.x * 0.04D + (living.getRandom().nextDouble() - 0.5D) * spread,
                    0.02D + living.getRandom().nextDouble() * 0.04D,
                    look.z * 0.04D + (living.getRandom().nextDouble() - 0.5D) * spread);
        }
    }

    public static void spawnInhaleSmoke(LivingEntity entity) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || entity == null) {
            return;
        }
        boolean firstPerson = entity == mc.player && mc.options.getCameraType() == PointOfView.FIRST_PERSON;
        if (firstPerson) {
            Vector3d view = entity.getEyePosition(1.0F);
            float yaw = entity.yRot * ((float) Math.PI / 180.0F);
            float pitch = entity.xRot * ((float) Math.PI / 180.0F);
            double forwardX = -MathHelper.sin(yaw) * MathHelper.cos(pitch);
            double forwardY = -MathHelper.sin(pitch);
            double forwardZ = MathHelper.cos(yaw) * MathHelper.cos(pitch);
            double rightX = -MathHelper.cos(yaw);
            double rightZ = -MathHelper.sin(yaw);
            int side = entity.getMainArm() == net.minecraft.util.HandSide.RIGHT ? 1 : -1;
            if (entity.getUsedItemHand() == Hand.OFF_HAND) {
                side = -side;
            }
            double px = view.x + forwardX * 0.45D + rightX * side * 0.22D;
            double py = view.y + forwardY * 0.45D - 0.12D;
            double pz = view.z + forwardZ * 0.45D + rightZ * side * 0.22D;
            for (int i = 0; i < 2; i++) {
                mc.level.addParticle(ParticleTypes.SMOKE, px, py, pz,
                        (entity.getRandom().nextDouble() - 0.5D) * 0.02D,
                        (entity.getRandom().nextDouble() - 0.5D) * 0.05D,
                        (entity.getRandom().nextDouble() - 0.5D) * 0.02D);
            }
            return;
        }
        float bodyYaw = entity.yBodyRot * ((float) Math.PI / 180.0F);
        double forwardX = -MathHelper.sin(bodyYaw);
        double forwardZ = MathHelper.cos(bodyYaw);
        double rightX = -MathHelper.cos(bodyYaw);
        double rightZ = -MathHelper.sin(bodyYaw);
        int side = entity.getMainArm() == net.minecraft.util.HandSide.RIGHT ? 1 : -1;
        if (entity.getUsedItemHand() == Hand.OFF_HAND) {
            side = -side;
        }
        double px = entity.getX() + forwardX * 0.55D + rightX * side * -0.08D;
        double py = entity.getEyeY() - 0.18D;
        double pz = entity.getZ() + forwardZ * 0.55D + rightZ * side * -0.08D;
        for (int i = 0; i < 2; i++) {
            mc.level.addParticle(ParticleTypes.SMOKE, px, py, pz,
                    (entity.getRandom().nextDouble() - 0.5D) * 0.04D,
                    (entity.getRandom().nextDouble() - 0.5D) * 0.08D,
                    (entity.getRandom().nextDouble() - 0.5D) * 0.04D);
        }
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        for (PlayerEntity player : mc.level.players()) {
            int count = getMouthCount(player.getId());
            if (count <= 0 || player.tickCount % 15 != 0) {
                continue;
            }
            Vector3d look = player.getLookAngle();
            Vector3d mouth = player.getEyePosition(1.0F).add(look.scale(0.28D));
            mc.level.addParticle(ParticleTypes.SMOKE,
                    mouth.x, mouth.y - 0.18D, mouth.z,
                    look.x * 0.01D, 0.015D, look.z * 0.01D);
        }
    }

    public static void clear() {
        MOUTH_COUNTS.clear();
    }
}
