package com.nextalubm.rotp_nextalbum.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.nextalubm.rotp_nextalbum.util.AgingEntityUtil;

import net.minecraft.entity.LivingEntity;

@Mixin(LivingEntity.class)
public abstract class LivingEntitySwingDurationMixin {

    @Inject(method = "getCurrentSwingDuration", at = @At("RETURN"), cancellable = true)
    private void rotpNextAlbum$slowSwingDuration(CallbackInfoReturnable<Integer> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        float multiplier = AgingEntityUtil.getSwingDurationMultiplier(self);
        if (multiplier <= 1.001F) {
            return;
        }
        int original = cir.getReturnValueI();
        int stretched = (int) Math.ceil(original * multiplier);
        if (stretched > original) {
            cir.setReturnValue(stretched);
        }
    }
}
