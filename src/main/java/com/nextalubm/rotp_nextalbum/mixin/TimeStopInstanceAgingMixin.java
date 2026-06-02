package com.nextalubm.rotp_nextalbum.mixin;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.github.standobyte.jojo.capability.world.TimeStopInstance;
import com.nextalubm.rotp_nextalbum.util.AgingEntityUtil;

import net.minecraft.entity.LivingEntity;

@Mixin(value = TimeStopInstance.class, remap = false)
public abstract class TimeStopInstanceAgingMixin {
    @Shadow private int ticksLeft;
    @Shadow @Nullable public final LivingEntity user = null;
    @Shadow public abstract void setTicksLeft(int ticks);

    @Inject(method = "tick", at = @At("HEAD"))
    private void rotpNextAlbum$rapidlyShortenAgedActiveTimeStop(CallbackInfoReturnable<Boolean> cir) {
        if (user != null && !user.level.isClientSide()) {
            int agedTicks = AgingEntityUtil.getAgedActiveTimeStopTicks(user, ticksLeft);
            if (agedTicks < ticksLeft) {
                setTicksLeft(agedTicks);
            }
        }
    }
}
