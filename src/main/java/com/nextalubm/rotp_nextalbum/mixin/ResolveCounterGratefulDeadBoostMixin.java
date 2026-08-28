package com.nextalubm.rotp_nextalbum.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.power.impl.stand.ResolveCounter;
import com.nextalubm.rotp_nextalbum.util.TheGratefulDeadResolveUtil;

@Mixin(value = ResolveCounter.class, remap = false)
public abstract class ResolveCounterGratefulDeadBoostMixin {
    @Shadow
    private IStandPower stand;

    @ModifyConstant(method = "addResolveOnAttack", constant = @Constant(floatValue = 5.0F))
    private float rotpNextAlbum$agingAuraAttackBoostCap(float original) {
        return TheGratefulDeadResolveUtil.getAttackBoostCap(stand, original);
    }
}
