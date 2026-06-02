package com.nextalubm.rotp_nextalbum.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.nextalubm.rotp_nextalbum.client.AgingEntityRenderContext;
import com.nextalubm.rotp_nextalbum.client.AgingTintingVertexBuilder;

import net.minecraft.client.renderer.entity.model.AgeableModel;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.entity.model.PlayerModel;

@Mixin(AgeableModel.class)
public abstract class AgeableModelAgingMixin {

    @Inject(method = "renderToBuffer(Lcom/mojang/blaze3d/matrix/MatrixStack;Lcom/mojang/blaze3d/vertex/IVertexBuilder;IIFFFF)V",
            at = @At("HEAD"),
            cancellable = true)
    private void rotpNextAlbum$renderAgedPlayer(MatrixStack matrixStack, IVertexBuilder vertexBuilder,
                                                 int packedLight, int packedOverlay, float red, float green,
                                                 float blue, float alpha, CallbackInfo ci) {
        if (!((Object) this instanceof PlayerModel)) {
            return;
        }
        EntityModel<?> entityModel = (EntityModel<?>) (Object) this;
        if (entityModel.young) {
            return;
        }
        float progress = AgingEntityRenderContext.getProgress();
        if (progress <= 0F) {
            return;
        }
        PlayerModel<?> playerModel = (PlayerModel<?>) (Object) this;
        IVertexBuilder agedBuilder = AgingTintingVertexBuilder.wrap(vertexBuilder, progress);
        playerModel.head.render(matrixStack, agedBuilder, packedLight, packedOverlay, red, green, blue, alpha);
        playerModel.body.render(matrixStack, agedBuilder, packedLight, packedOverlay, red, green, blue, alpha);
        playerModel.rightArm.render(matrixStack, agedBuilder, packedLight, packedOverlay, red, green, blue, alpha);
        playerModel.leftArm.render(matrixStack, agedBuilder, packedLight, packedOverlay, red, green, blue, alpha);
        playerModel.rightLeg.render(matrixStack, agedBuilder, packedLight, packedOverlay, red, green, blue, alpha);
        playerModel.leftLeg.render(matrixStack, agedBuilder, packedLight, packedOverlay, red, green, blue, alpha);
        playerModel.hat.render(matrixStack, vertexBuilder, packedLight, packedOverlay, red, green, blue, alpha);
        playerModel.jacket.render(matrixStack, vertexBuilder, packedLight, packedOverlay, red, green, blue, alpha);
        playerModel.rightSleeve.render(matrixStack, vertexBuilder, packedLight, packedOverlay, red, green, blue, alpha);
        playerModel.leftSleeve.render(matrixStack, vertexBuilder, packedLight, packedOverlay, red, green, blue, alpha);
        playerModel.rightPants.render(matrixStack, vertexBuilder, packedLight, packedOverlay, red, green, blue, alpha);
        playerModel.leftPants.render(matrixStack, vertexBuilder, packedLight, packedOverlay, red, green, blue, alpha);
        ci.cancel();
    }
}