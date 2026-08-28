package com.nextalubm.rotp_nextalbum.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.nextalubm.rotp_nextalbum.client.ResolveDownedClient;
import com.nextalubm.rotp_nextalbum.entity.SexPistolsEntity;
import com.nextalubm.rotp_nextalbum.util.ResolveDownedUtil;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

@Mixin(StandEntity.class)
public abstract class StandEntityDownedMixin {

    @Inject(method = "getAlpha", at = @At("RETURN"), cancellable = true, remap = false)
    private void rotpNextAlbum$downedVisualAlpha(float partialTick, CallbackInfoReturnable<Float> cir) {
        float fade = ResolveDownedUtil.getStandVisualAlpha((StandEntity) (Object) this);
        if (fade < 0.999F) {
            cir.setReturnValue(MathHelper.clamp(cir.getReturnValueF() * fade, 0.0F, 1.0F));
        }
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void rotpNextAlbum$downedStandAppearance(CallbackInfo ci) {
        StandEntity self = (StandEntity) (Object) this;
        if (!(self instanceof SexPistolsEntity)) {
            return;
        }
        if (isUserDowned(self.getUser())) {
            self.deathTime = 0;
        }
    }

    private static boolean isUserDowned(LivingEntity user) {
        if (user == null) {
            return false;
        }
        if (ResolveDownedUtil.isDowned(user)) {
            return true;
        }
        if (user.level != null && user.level.isClientSide()) {
            Boolean client = DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> () -> ResolveDownedClient.isDowned(user));
            return client != null && client.booleanValue();
        }
        return false;
    }
}
