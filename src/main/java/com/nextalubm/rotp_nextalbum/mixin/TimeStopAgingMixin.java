//package com.nextalubm.rotp_nextalbum.mixin;
//
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//
//import com.github.standobyte.jojo.action.ActionConditionResult;
//import com.github.standobyte.jojo.action.ActionTarget;
//import com.github.standobyte.jojo.action.stand.StandAction;
//import com.github.standobyte.jojo.action.stand.TimeStop;
//import com.github.standobyte.jojo.power.impl.stand.IStandPower;
//import com.nextalubm.rotp_nextalbum.util.AgingEntityUtil;
//
//import net.minecraft.entity.LivingEntity;
//
//@Mixin(value = TimeStop.class, remap = false)
//public abstract class TimeStopAgingMixin {
//    @Inject(method = "checkSpecificConditions", at = @At("HEAD"), cancellable = true)
//    private void rotpNextAlbum$preventTimeStopWhenTooAged(LivingEntity user, IStandPower power, ActionTarget target,
//                                                          CallbackInfoReturnable<ActionConditionResult> cir) {
//        if (!AgingEntityUtil.canUseTimeStop(user)) {
//            cir.setReturnValue(ActionConditionResult.NEGATIVE);
//        }
//    }
//
//    @Inject(method = "getTimeStopTicks", at = @At("RETURN"), cancellable = true)
//    private static void rotpNextAlbum$shortenAgedTimeStop(IStandPower standPower, StandAction timeStopAction,
//                                                          CallbackInfoReturnable<Integer> cir) {
//        LivingEntity user = standPower != null ? standPower.getUser() : null;
//        cir.setReturnValue(AgingEntityUtil.getAgedTimeStopTicks(user, cir.getReturnValue()));
//    }
//}
