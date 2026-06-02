package com.nextalubm.rotp_nextalbum.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.github.standobyte.jojo.client.render.entity.renderer.stand.StandEntityRenderer;
import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.nextalubm.rotp_nextalbum.client.AgingEntityRenderContext;
import com.nextalubm.rotp_nextalbum.client.AgingTintingRenderTypeBuffer;

import net.minecraft.client.renderer.IRenderTypeBuffer;

@Mixin(value = StandEntityRenderer.class, remap = false)
public abstract class StandEntityRendererAgingMixin {
    @Inject(method = "render(Lcom/github/standobyte/jojo/entity/stand/StandEntity;FFLcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;I)V",
            at = @At("HEAD"))
    private void rotpNextAlbum$setStandAgingRenderContext(StandEntity entity, float entityYaw, float partialTicks,
                                                          MatrixStack matrixStack, IRenderTypeBuffer buffer,
                                                          int packedLight, CallbackInfo ci) {
        AgingEntityRenderContext.set(entity);
    }

    @ModifyVariable(method = "render(Lcom/github/standobyte/jojo/entity/stand/StandEntity;FFLcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;I)V",
            at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private IRenderTypeBuffer rotpNextAlbum$wrapStandAgingRenderBuffer(IRenderTypeBuffer buffer) {
        return AgingTintingRenderTypeBuffer.wrap(buffer, AgingEntityRenderContext.getProgress());
    }

    @Inject(method = "render(Lcom/github/standobyte/jojo/entity/stand/StandEntity;FFLcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;I)V",
            at = @At("RETURN"))
    private void rotpNextAlbum$clearStandAgingRenderContext(StandEntity entity, float entityYaw, float partialTicks,
                                                            MatrixStack matrixStack, IRenderTypeBuffer buffer,
                                                            int packedLight, CallbackInfo ci) {
        AgingEntityRenderContext.clear();
    }
}
