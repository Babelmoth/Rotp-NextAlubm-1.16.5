package com.nextalubm.rotp_nextalbum.particle;

import java.util.Random;

import net.minecraft.particles.RedstoneParticleData;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;

/** Dark brown dust burst used when fully-aged plants crumble on contact. */
public final class AgingDustParticleSpawner {

    private static final RedstoneParticleData DUST = new RedstoneParticleData(0.18F, 0.10F, 0.035F, 1.15F);

    private AgingDustParticleSpawner() {
    }

    public static void spawn(ServerWorld world, BlockPos pos, int count) {
        Random random = world.random;
        for (int i = 0; i < count; i++) {
            double x = pos.getX() + 0.2D + random.nextDouble() * 0.6D;
            double y = pos.getY() + 0.1D + random.nextDouble() * 0.8D;
            double z = pos.getZ() + 0.2D + random.nextDouble() * 0.6D;
            double dx = (random.nextDouble() - 0.5D) * 0.06D;
            double dy = 0.02D + random.nextDouble() * 0.06D;
            double dz = (random.nextDouble() - 0.5D) * 0.06D;
            world.sendParticles(DUST, x, y, z, 1, dx, dy, dz, 0.0D);
        }
    }
}
