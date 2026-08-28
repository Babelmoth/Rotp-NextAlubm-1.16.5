package com.nextalubm.rotp_nextalbum.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.github.standobyte.jojo.action.Action;
import com.github.standobyte.jojo.action.ActionConditionResult;
import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.action.non_stand.HamonAction;
import com.github.standobyte.jojo.power.IPower;
import com.github.standobyte.jojo.util.general.ObjectWrapper;
import com.nextalubm.rotp_nextalbum.util.BeachBoyLine;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;

@Mixin(value = Action.class, remap = false)
public abstract class ActionHamonLineMixin {
    @Inject(method = "checkRangeAndTarget", at = @At("HEAD"), cancellable = true)
    private void rotpNextAlbum$hamonLineRange(ActionTarget target, LivingEntity user, IPower power,
            CallbackInfoReturnable<ActionConditionResult> cir) {
        if (!((Object) this instanceof HamonAction) || user == null || target == null) {
            return;
        }
        Entity entity = target.getEntity();
        if (entity != null && BeachBoyLine.isReachableHookedTarget(user, entity)) {
            cir.setReturnValue(ActionConditionResult.POSITIVE);
        }
    }

    @Inject(method = "targetBeforePerform", at = @At("RETURN"), cancellable = true)
    private void rotpNextAlbum$hamonLineTargetBefore(World world, LivingEntity user, IPower power,
            ActionTarget target, CallbackInfoReturnable<ActionTarget> cir) {
        if (!((Object) this instanceof HamonAction) || user == null) {
            return;
        }
        ActionTarget current = cir.getReturnValue();
        Entity currentEntity = current == null ? null : current.getEntity();
        ActionTarget redirected = BeachBoyLine.redirectHamonAttack(user, currentEntity);
        if (redirected == null) {
            return;
        }
        if (current == null || current == ActionTarget.EMPTY || currentEntity == null
                || currentEntity == redirected.getEntity()) {
            cir.setReturnValue(redirected);
        }
    }

    @Inject(method = "overrideVanillaMouseTarget", at = @At("TAIL"))
    private void rotpNextAlbum$hamonLineOverrideMouse(ObjectWrapper<ActionTarget> targetWrapper, World world,
            LivingEntity user, IPower power, CallbackInfo ci) {
        if (!((Object) this instanceof HamonAction) || user == null || targetWrapper == null) {
            return;
        }
        ActionTarget current = targetWrapper.get();
        Entity currentEntity = current == null ? null : current.getEntity();
        ActionTarget redirected = BeachBoyLine.redirectHamonAttack(user, currentEntity);
        if (redirected == null) {
            return;
        }
        if (current == null || current == ActionTarget.EMPTY || currentEntity == null
                || currentEntity == redirected.getEntity()) {
            targetWrapper.set(redirected);
        }
    }
}
