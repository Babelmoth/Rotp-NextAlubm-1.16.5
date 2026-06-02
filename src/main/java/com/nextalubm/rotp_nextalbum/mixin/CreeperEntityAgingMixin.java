package com.nextalubm.rotp_nextalbum.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.nextalubm.rotp_nextalbum.util.AgingEntityUtil;

import net.minecraft.entity.monster.CreeperEntity;

@Mixin(CreeperEntity.class)
public abstract class CreeperEntityAgingMixin {
    @Shadow
    private int swell;
    @Shadow
    private int oldSwell;

    @Shadow
    public abstract void setSwellDir(int state);

    @Inject(method = "tick", at = @At("HEAD"))
    private void rotpNextAlbum$suppressAgedCreeperFuse(CallbackInfo ci) {
        if (AgingEntityUtil.shouldDisableCreeperExplosion((CreeperEntity) (Object) this)) {
            rotpNextAlbum$resetFuse();
        }
    }

    @Inject(method = "ignite", at = @At("HEAD"), cancellable = true)
    private void rotpNextAlbum$preventAgedCreeperIgnite(CallbackInfo ci) {
        if (AgingEntityUtil.shouldDisableCreeperExplosion((CreeperEntity) (Object) this)) {
            rotpNextAlbum$resetFuse();
            ci.cancel();
        }
    }

    @Inject(method = "explodeCreeper", at = @At("HEAD"), cancellable = true)
    private void rotpNextAlbum$preventAgedCreeperExplosion(CallbackInfo ci) {
        if (AgingEntityUtil.shouldDisableCreeperExplosion((CreeperEntity) (Object) this)) {
            rotpNextAlbum$resetFuse();
            ci.cancel();
        }
    }

    @Unique
    private void rotpNextAlbum$resetFuse() {
        this.oldSwell = 0;
        this.swell = 0;
        this.setSwellDir(-1);
    }
}
