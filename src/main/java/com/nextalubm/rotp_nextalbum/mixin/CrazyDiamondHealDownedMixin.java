package com.nextalubm.rotp_nextalbum.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.github.standobyte.jojo.action.stand.CrazyDiamondHeal;
import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.entity.stand.StandEntityTask;
import com.github.standobyte.jojo.power.impl.stand.StandUtil;
import com.nextalubm.rotp_nextalbum.util.ResolveDownedUtil;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.world.World;

@Mixin(CrazyDiamondHeal.class)
public abstract class CrazyDiamondHealDownedMixin {
    @Inject(method = "healLivingEntity", at = @At("HEAD"), remap = false)
    private void rotpNextAlbum$allowDownedHeal(World world, LivingEntity entity, StandEntity standEntity,
                                               StandEntityTask task, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity toHeal = StandUtil.getStandUser(entity);
        if (ResolveDownedUtil.isDowned(toHeal) || ResolveDownedUtil.isDowned(entity)) {
            entity.deathTime = 0;
            if (toHeal != entity) {
                toHeal.deathTime = 0;
            }
        }
    }

    @Inject(method = "healLivingEntity", at = @At("RETURN"), remap = false)
    private void rotpNextAlbum$restoreOrReviveDowned(World world, LivingEntity entity, StandEntity standEntity,
                                                     StandEntityTask task, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity toHeal = StandUtil.getStandUser(entity);
        if (!ResolveDownedUtil.isDowned(toHeal) && !ResolveDownedUtil.isDowned(entity)) {
            return;
        }
        if (!world.isClientSide() && toHeal instanceof ServerPlayerEntity
                && toHeal.getHealth() >= toHeal.getMaxHealth() - 0.01F) {
            ResolveDownedUtil.revive((ServerPlayerEntity) toHeal);
            return;
        }
        entity.deathTime = ResolveDownedUtil.DOWNED_POSE_TICKS;
        toHeal.deathTime = ResolveDownedUtil.DOWNED_POSE_TICKS;
    }
}
