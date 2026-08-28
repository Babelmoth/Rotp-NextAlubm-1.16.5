package com.nextalubm.rotp_nextalbum.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.github.standobyte.jojo.action.stand.StandAction;
import com.github.standobyte.jojo.action.stand.StandEntityBlock;
import com.github.standobyte.jojo.action.stand.StandEntityHeavyAttack;
import com.github.standobyte.jojo.action.stand.StandEntityLightAttack;
import com.github.standobyte.jojo.action.stand.StandEntityMeleeBarrage;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.nextalubm.rotp_nextalbum.util.AgingEntityUtil;

@Mixin(StandAction.class)
public abstract class StandActionStaminaAgingMixin {
    @Inject(method = "getStaminaCost", at = @At("RETURN"), cancellable = true, remap = false)
    private void rotpNextAlbum$ageSkillStaminaCost(IStandPower stand, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(scaleSkillStamina((StandAction) (Object) this, stand, cir.getReturnValueF()));
    }

    @Inject(method = "getStaminaCostTicking", at = @At("RETURN"), cancellable = true, remap = false)
    private void rotpNextAlbum$ageSkillStaminaTickCost(IStandPower stand, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(scaleSkillStamina((StandAction) (Object) this, stand, cir.getReturnValueF()));
    }

    private static float scaleSkillStamina(StandAction action, IStandPower stand, float baseCost) {
        if (baseCost <= 0F || stand == null) {
            return baseCost;
        }
        return isMeleeStaminaAction(action)
                ? baseCost
                : AgingEntityUtil.applySkillStaminaCost(stand.getUser(), baseCost);
    }

    private static boolean isMeleeStaminaAction(StandAction action) {
        return action instanceof StandEntityMeleeBarrage
                || action instanceof StandEntityLightAttack
                || action instanceof StandEntityHeavyAttack
                || action instanceof StandEntityBlock;
    }
}
