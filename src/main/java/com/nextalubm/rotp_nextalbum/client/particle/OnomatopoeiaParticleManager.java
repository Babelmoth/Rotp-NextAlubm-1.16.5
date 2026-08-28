package com.nextalubm.rotp_nextalbum.client.particle;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.particles.IParticleData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class OnomatopoeiaParticleManager {
    private OnomatopoeiaParticleManager() {
    }

    public static void spawnFootstepParticle(ClientWorld world, IParticleData particle, double x, double y, double z) {
        world.addParticle(particle, true, x, y, z, 0.0D, 0.0D, 0.0D);
    }

    public static boolean isEnabled() {
        return true;
    }
}
