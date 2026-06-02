package com.nextalubm.rotp_nextalbum.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.nextalubm.rotp_nextalbum.client.AgingEntityRenderContext;
import com.nextalubm.rotp_nextalbum.client.AgingTintingVertexBuilder;

import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.LivingRenderer;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.entity.LivingEntity;

@Mixin(LivingRenderer.class)
public abstract class LivingRendererAgingMixin<T extends LivingEntity, M extends EntityModel<T>> {

    @Shadow
    protected M model;

    @Inject(method = "render(Lnet/minecraft/entity/LivingEntity;FFLcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;I)V",
            at = @At("HEAD"))
    private void rotpNextAlbum$setAgingContext(T entity, float entityYaw, float partialTicks,
                                                MatrixStack matrixStack, IRenderTypeBuffer buffer,
                                                int packedLight, CallbackInfo ci) {
        AgingEntityRenderContext.set(entity);
    }

    @ModifyArg(method = "render(Lnet/minecraft/entity/LivingEntity;FFLcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;I)V",
               at = @At(value = "INVOKE",
                        target = "Lnet/minecraft/client/renderer/entity/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/matrix/MatrixStack;Lcom/mojang/blaze3d/vertex/IVertexBuilder;IIFFFF)V"),
               index = 1)
    private IVertexBuilder rotpNextAlbum$wrapLivingModelVertexBuilder(IVertexBuilder original) {
        if (model instanceof PlayerModel) {
            return original;
        }
        return AgingTintingVertexBuilder.wrap(original, AgingEntityRenderContext.getProgress());
    }

    @Inject(method = "render(Lnet/minecraft/entity/LivingEntity;FFLcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;I)V",
            at = @At("RETURN"))
    private void rotpNextAlbum$clearAgingContext(T entity, float entityYaw, float partialTicks,
                                                  MatrixStack matrixStack, IRenderTypeBuffer buffer,
                                                  int packedLight, CallbackInfo ci) {
        AgingEntityRenderContext.clear();
    }
}