package com.nextalubm.rotp_nextalbum.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.nextalubm.rotp_nextalbum.util.TheGratefulDeadGrabUtil;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;

@Mixin(Entity.class)
public class GrabbedEntityCollisionMixin {

    @Inject(method = "canBeCollidedWith", at = @At("HEAD"), cancellable = true)
    private void rotpNextAlbum$disableGrabbedCollision(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (self.level != null && self instanceof LivingEntity
                && TheGratefulDeadGrabUtil.isEntityGrabbed((LivingEntity) self)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
    private void rotpNextAlbum$disableGrabbedPush(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (self.level != null && self instanceof LivingEntity
                && TheGratefulDeadGrabUtil.isEntityGrabbed((LivingEntity) self)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "push(Lnet/minecraft/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void rotpNextAlbum$skipGrabbedEntityPush(Entity other, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (isGrabbedLiving(self) || isGrabbedLiving(other)) {
            ci.cancel();
        }
    }

    private static boolean isGrabbedLiving(Entity entity) {
        return entity instanceof LivingEntity && TheGratefulDeadGrabUtil.isEntityGrabbed((LivingEntity) entity);
    }
}
