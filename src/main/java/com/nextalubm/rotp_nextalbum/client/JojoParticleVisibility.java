package com.nextalubm.rotp_nextalbum.client;

import com.github.standobyte.jojo.power.impl.stand.StandUtil;
import com.nextalubm.rotp_nextalbum.NextAlbumConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class JojoParticleVisibility {
    private JojoParticleVisibility() {
    }

    public static boolean canSeeStands() {
        PlayerEntity player = Minecraft.getInstance().player;
        return player != null && StandUtil.clStandEntityVisibleTo(player);
    }

    public static boolean shouldShowOnomatopoeiaParticles() {
        return NextAlbumConfig.getCommonConfig(true).onomatopoeiaParticlesEnabled.get()
                && canSeeStands();
    }
}
