package com.nextalubm.rotp_nextalbum.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.nextalubm.rotp_nextalbum.client.RevolverClientAnim;
import com.nextalubm.rotp_nextalbum.item.RevolverItem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.layers.HeldItemLayer;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.HandSide;
import net.minecraft.util.math.vector.Vector3f;

@Mixin(HeldItemLayer.class)
public abstract class HeldItemLayerRevolverMixin {

    @Inject(method = "renderArmWithItem", at = @At("HEAD"))
    private void rotpNextAlbum$revolverHeldPose(LivingEntity entity, ItemStack stack, ItemCameraTransforms.TransformType transform,
            HandSide side, MatrixStack matrix, IRenderTypeBuffer buffer, int light, CallbackInfo ci) {
        if (stack.isEmpty() || !(stack.getItem() instanceof RevolverItem) || !(entity instanceof PlayerEntity)) {
            return;
        }
        PlayerEntity player = (PlayerEntity) entity;
        if (player.getMainHandItem() != stack) {
            return;
        }

        float aim = 0.0F;
        float reload = 0.0F;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == player) {
            float partial = mc.getFrameTime();
            aim = RevolverClientAnim.getAimProgress(partial);
            reload = RevolverClientAnim.getReloadProgress(partial);
        }
        else {
            if (RevolverItem.isAiming(stack)) {
                aim = 1.0F;
            }
            if (RevolverItem.isReloadMode(stack)) {
                reload = 1.0F;
            }
        }
        float hold = (1.0F - aim) * (1.0F - reload);
        matrix.translate(0.0D, -0.04D * hold + 0.04D * aim + 0.06D * reload, -0.04D * hold + 0.08D * aim - 0.02D * reload);
        matrix.mulPose(Vector3f.XP.rotationDegrees(-2.0F * aim + 14.0F * reload));
        matrix.mulPose(Vector3f.YP.rotationDegrees(4.0F * reload));
        matrix.mulPose(Vector3f.ZP.rotationDegrees(3.0F * reload));
    }
}
