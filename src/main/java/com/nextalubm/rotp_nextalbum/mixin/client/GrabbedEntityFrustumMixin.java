package com.nextalubm.rotp_nextalbum.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.nextalubm.rotp_nextalbum.client.ClientGrabState;
import com.nextalubm.rotp_nextalbum.util.TheGratefulDeadGrabUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;

@Mixin(EntityRenderer.class)
public abstract class GrabbedEntityFrustumMixin<T extends Entity> {

    @Inject(method = "shouldRender(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/renderer/culling/ClippingHelper;DDD)Z",
            at = @At("HEAD"), cancellable = true)
    private void rotpNextAlbum$forceRenderGrabbed(T entity, ClippingHelper camera,
            double camX, double camY, double camZ, CallbackInfoReturnable<Boolean> cir) {
        if (!(entity instanceof LivingEntity)) {
            return;
        }
        LivingEntity grabber = ClientGrabState.getGrabberOf((LivingEntity) entity);
        if (grabber == null) {
            return;
        }
        LivingEntity standAnchor = TheGratefulDeadGrabUtil.getGrabRenderAnchor(grabber);
        Minecraft mc = Minecraft.getInstance();
        if (grabber == mc.player || camera.isVisible(standAnchor.getBoundingBox())) {
            cir.setReturnValue(true);
        }
    }
}
