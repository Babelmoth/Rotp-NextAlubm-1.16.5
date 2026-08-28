package com.nextalubm.rotp_nextalbum.mixin.client;

import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.util.ProsciuttoShadowUtil;

import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerEntityMixin {
    private static final ResourceLocation SHADOW_PLAYER_TEXTURE = new ResourceLocation(NextAlubm.MOD_ID, "textures/entity/shadow_player.png");
    private static final ResourceLocation SHADOW_PLAYER_SLIM_TEXTURE = new ResourceLocation(NextAlubm.MOD_ID, "textures/entity/shadow_player_slim.png");

    @Inject(method = "getSkinTextureLocation", at = @At("HEAD"), cancellable = true, require = 0)
    private void prosciutto$overrideSkin(CallbackInfoReturnable<ResourceLocation> cir) {
        AbstractClientPlayerEntity player = (AbstractClientPlayerEntity) (Object) this;
        if (!ProsciuttoShadowUtil.isShadow(player)) {
            return;
        }
        if ("slim".equals(player.getModelName())) {
            cir.setReturnValue(SHADOW_PLAYER_SLIM_TEXTURE);
        }
        else {
            cir.setReturnValue(SHADOW_PLAYER_TEXTURE);
        }
    }
}
