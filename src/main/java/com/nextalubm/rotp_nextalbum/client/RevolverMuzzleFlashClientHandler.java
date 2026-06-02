package com.nextalubm.rotp_nextalbum.client;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import com.nextalubm.rotp_nextalbum.init.InitParticles;

import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.HandSide;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

public final class RevolverMuzzleFlashClientHandler {
    private RevolverMuzzleFlashClientHandler() {
    }

    public static void spawn(int entityId, boolean aiming, float random) {
        Minecraft mc = Minecraft.getInstance();
        World world = mc.level;
        if (world == null) {
            return;
        }
        Entity entity = world.getEntity(entityId);
        if (!(entity instanceof LivingEntity)) {
            return;
        }
        LivingEntity shooter = (LivingEntity) entity;
        Vector3d look = shooter.getLookAngle();
        if (look.lengthSqr() < 1.0E-6D) {
            return;
        }
        look = look.normalize();
        Vector3d right = new Vector3d(-look.z, 0.0D, look.x);
        if (right.lengthSqr() < 1.0E-6D) {
            right = Vector3d.directionFromRotation(0.0F, shooter.yRot + 90.0F);
        }
        right = right.normalize();
        if (shooter.getMainArm() == HandSide.LEFT) {
            right = right.reverse();
        }
        Vector3d up = right.cross(look);
        if (up.lengthSqr() < 1.0E-6D) {
            up = new Vector3d(0.0D, 1.0D, 0.0D);
        }
        else {
            up = up.normalize();
        }
        double sideOffset = aiming ? 0.02D : 0.22D;
        double upOffset = aiming ? -0.02D : -0.10D;
        double forwardOffset = aiming ? 0.92D : 0.78D;
        Vector3d eye = shooter.getEyePosition(mc.getFrameTime());
        Vector3d muzzle = eye.add(look.scale(forwardOffset)).add(right.scale(sideOffset)).add(up.scale(upOffset));
        double spread = aiming ? 0.014D : 0.028D;
        Vector3d flashVelocity = look.scale(0.035D + random * 0.025D);
        world.addParticle(InitParticles.REVOLVER_MUZZLE_FLASH.get(), muzzle.x, muzzle.y, muzzle.z, flashVelocity.x, flashVelocity.y, flashVelocity.z);
        for (int i = 0; i < 2; i++) {
            double angle = random * Math.PI * 2.0D + i * Math.PI;
            Vector3d offset = right.scale(Math.cos(angle) * spread).add(up.scale(Math.sin(angle) * spread));
            Vector3d pos = muzzle.add(offset).add(look.scale(0.035D * i));
            Vector3d velocity = look.scale(0.025D).add(offset.scale(0.35D));
            world.addParticle(InitParticles.REVOLVER_MUZZLE_FLASH.get(), pos.x, pos.y, pos.z, velocity.x, velocity.y, velocity.z);
        }
        world.addParticle(ParticleTypes.POOF, muzzle.x - look.x * 0.05D, muzzle.y - look.y * 0.05D, muzzle.z - look.z * 0.05D, look.x * 0.015D, look.y * 0.015D, look.z * 0.015D);
        world.addParticle(ParticleTypes.SMOKE, muzzle.x - look.x * 0.10D, muzzle.y - look.y * 0.10D, muzzle.z - look.z * 0.10D, look.x * 0.01D, look.y * 0.01D + 0.006D, look.z * 0.01D);
    }
}
