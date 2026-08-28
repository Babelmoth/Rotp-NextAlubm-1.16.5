package com.nextalubm.rotp_nextalbum.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.github.standobyte.jojo.action.Action;
import com.github.standobyte.jojo.action.ActionConditionResult;
import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.action.stand.StandAction;
import com.github.standobyte.jojo.power.IPower;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.nextalubm.rotp_nextalbum.util.AgingEntityUtil;

import net.minecraft.entity.LivingEntity;

@Mixin(Action.class)
public abstract class ActionAgingMixin {
    @Inject(method = "checkConditions", at = @At("HEAD"), cancellable = true, remap = false)
    private void rotpNextAlbum$lockAgedStand(LivingEntity user, IPower power, ActionTarget target,
                                             CallbackInfoReturnable<ActionConditionResult> cir) {
        if (!(((Object) this) instanceof StandAction) || user == null || !AgingEntityUtil.cannotUseStand(user)) {
            return;
        }
        if (power instanceof IStandPower && AgingEntityUtil.isSexPistolsPower((IStandPower) power)) {
            return;
        }
        cir.setReturnValue(ActionConditionResult.NEGATIVE);
    }
}
