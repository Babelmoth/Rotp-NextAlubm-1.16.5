package com.nextalubm.rotp_nextalbum.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.power.impl.stand.ResolveCounter;
import com.nextalubm.rotp_nextalbum.util.PesciSuitUtil;

import net.minecraft.entity.LivingEntity;

@Mixin(value = ResolveCounter.class, remap = false)
public abstract class ResolveCounterPesciDecayMixin {
    @Shadow
    private IStandPower stand;

    @Inject(method = "getResolveDecay", at = @At("RETURN"), cancellable = true)
    private void rotpNextAlbum$halveInheritedWillDecay(CallbackInfoReturnable<Float> cir) {
        if (stand == null) {
            return;
        }
        LivingEntity user = stand.getUser();
        if (PesciSuitUtil.shouldHalveResolveDecay(user)) {
            cir.setReturnValue(cir.getReturnValueF() * 0.5F);
        }
    }
}
