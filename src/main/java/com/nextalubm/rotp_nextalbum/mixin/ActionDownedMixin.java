package com.nextalubm.rotp_nextalbum.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.github.standobyte.jojo.action.Action;
import com.github.standobyte.jojo.action.ActionConditionResult;
import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.power.IPower;
import com.nextalubm.rotp_nextalbum.client.ResolveDownedClient;
import com.nextalubm.rotp_nextalbum.util.ResolveDownedUtil;

import net.minecraft.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

@Mixin(Action.class)
public abstract class ActionDownedMixin {
    @Inject(method = "checkConditions", at = @At("HEAD"), cancellable = true, remap = false)
    private void rotpNextAlbum$restrictDownedActions(LivingEntity user, IPower power, ActionTarget target,
                                                     CallbackInfoReturnable<ActionConditionResult> cir) {
        if (user == null) {
            return;
        }
        if (!isUserDowned(user)) {
            return;
        }
        if (shouldLock(user, this)) {
            cir.setReturnValue(ActionConditionResult.NEGATIVE);
        }
    }

    private static boolean isUserDowned(LivingEntity user) {
        if (ResolveDownedUtil.isDowned(user)) {
            return true;
        }
        if (user.level != null && user.level.isClientSide()) {
            Boolean client = DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> () -> ResolveDownedClient.isDowned(user));
            return client != null && client.booleanValue();
        }
        return false;
    }

    private static boolean shouldLock(LivingEntity user, Object action) {
        if (user.level != null && user.level.isClientSide()) {
            Boolean client = DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> () -> ResolveDownedClient.shouldLockAction(user, action));
            if (client != null) {
                return client.booleanValue();
            }
        }
        return ResolveDownedUtil.shouldLockAction(user, action);
    }
}
