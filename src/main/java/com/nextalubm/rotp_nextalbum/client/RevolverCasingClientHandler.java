package com.nextalubm.rotp_nextalbum.client;

import com.nextalubm.rotp_nextalbum.client.particle.RevolverCasingParticle;
import com.nextalubm.rotp_nextalbum.init.InitParticles;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RevolverCasingClientHandler {
    public static void ejectCasings(int entityId, int count) {
        Minecraft mc = Minecraft.getInstance();
        World world = mc.level;
        if (world == null || count <= 0) {
            return;
        }
        if (!(world.getEntity(entityId) instanceof PlayerEntity)) {
            return;
        }
        PlayerEntity player = (PlayerEntity) world.getEntity(entityId);
        boolean spawnItemsFromTheseParticles = mc.player != null && mc.player.getId() == entityId;
        Vector3d look = player.getLookAngle();
        Vector3d horizontalLook = new Vector3d(look.x, 0.0D, look.z);
        if (horizontalLook.lengthSqr() < 1.0E-6D) {
            horizontalLook = new Vector3d(0.0D, 0.0D, 1.0D);
        }
        horizontalLook = horizontalLook.normalize();
        Vector3d right = new Vector3d(-horizontalLook.z, 0.0D, horizontalLook.x).normalize();
        Vector3d basePos = player.position()
                .add(horizontalLook.scale(0.34D))
                .add(right.scale(0.42D))
                .add(0.0D, player.getEyeHeight() - 0.34D, 0.0D);
        for (int i = 0; i < count; i++) {
            double spread = (i - (count - 1) * 0.5D) * 0.055D;
            Vector3d pos = basePos
                    .add(right.scale(spread + (world.random.nextDouble() - 0.5D) * 0.035D))
                    .add(horizontalLook.scale((world.random.nextDouble() - 0.5D) * 0.035D))
                    .add(0.0D, i * 0.018D + world.random.nextDouble() * 0.025D, 0.0D);
            Vector3d velocity = right.scale(0.18D + world.random.nextDouble() * 0.10D)
                    .add(horizontalLook.scale(-0.03D + world.random.nextDouble() * 0.06D))
                    .add(0.0D, 0.11D + world.random.nextDouble() * 0.09D, 0.0D);
            RevolverCasingParticle.queueNextSpawnItemFlag(spawnItemsFromTheseParticles);
            world.addParticle(InitParticles.REVOLVER_CASING.get(), pos.x, pos.y, pos.z, velocity.x, velocity.y, velocity.z);
        }
    }
}