package com.nextalubm.rotp_nextalbum.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.nextalubm.rotp_nextalbum.client.ClientGrabState;
import com.nextalubm.rotp_nextalbum.util.TheGratefulDeadGrabUtil;

import net.minecraft.client.renderer.entity.LivingRenderer;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;

@Mixin(LivingRenderer.class)
public abstract class GrabbedEntityRendererMixin<T extends LivingEntity> {

    @Inject(method = "render(Lnet/minecraft/entity/LivingEntity;FFLcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;I)V",
            at = @At("HEAD"))
    private void rotpNextAlbum$repositionGrabbedEntity(T entity, float entityYaw, float partialTicks,
            MatrixStack matrixStack, net.minecraft.client.renderer.IRenderTypeBuffer buffer,
            int packedLight, CallbackInfo ci) {
        LivingEntity grabber = ClientGrabState.getGrabberOf(entity);
        if (grabber == null) {
            return;
        }
        LivingEntity standAnchor = TheGratefulDeadGrabUtil.getGrabRenderAnchor(grabber);
        if (standAnchor == null) {
            return;
        }

        Vector3d snapPos = TheGratefulDeadGrabUtil.getHeldEntitySnapPos(standAnchor, entity, partialTicks);
        double renderX = MathHelper.lerp(partialTicks, entity.xOld, entity.getX());
        double renderY = MathHelper.lerp(partialTicks, entity.yOld, entity.getY());
        double renderZ = MathHelper.lerp(partialTicks, entity.zOld, entity.getZ());
        matrixStack.translate(snapPos.x - renderX, snapPos.y - renderY, snapPos.z - renderZ);
    }

    @Inject(method = "setupRotations(Lnet/minecraft/entity/LivingEntity;Lcom/mojang/blaze3d/matrix/MatrixStack;FFF)V",
            at = @At("TAIL"))
    private void rotpNextAlbum$tiltGrabbedEntity(T entity, MatrixStack matrixStack,
            float ageInTicks, float rotationYaw, float partialTicks, CallbackInfo ci) {
        LivingEntity grabber = ClientGrabState.getGrabberOf(entity);
        if (grabber == null) {
            return;
        }
        LivingEntity standAnchor = TheGratefulDeadGrabUtil.getGrabRenderAnchor(grabber);
        if (standAnchor == null) {
            return;
        }
        float standYaw = MathHelper.lerp(partialTicks, standAnchor.yHeadRotO, standAnchor.yHeadRot);
        float standPitch = MathHelper.lerp(partialTicks, standAnchor.xRotO, standAnchor.xRot);
        float targetYaw = standYaw + 180.0F;
        matrixStack.mulPose(Vector3f.YP.rotationDegrees(targetYaw - rotationYaw));
        matrixStack.mulPose(Vector3f.XP.rotationDegrees(standPitch));
        matrixStack.translate(0.0D, -entity.getBbHeight() * 0.85D, 0.0D);
    }

    @Redirect(method = "render(Lnet/minecraft/entity/LivingEntity;FFLcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/model/EntityModel;setupAnim(Lnet/minecraft/entity/Entity;FFFFF)V"))
    private void rotpNextAlbum$freezeGrabbedLimbs(EntityModel model, Entity entity,
            float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        if (entity instanceof LivingEntity && ClientGrabState.isEntityGrabbed((LivingEntity) entity)) {
            if (entity instanceof net.minecraft.entity.player.PlayerEntity) {
                model.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            }
            else {
                model.setupAnim(entity, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);
            }
            return;
        }
        model.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
    }
}
