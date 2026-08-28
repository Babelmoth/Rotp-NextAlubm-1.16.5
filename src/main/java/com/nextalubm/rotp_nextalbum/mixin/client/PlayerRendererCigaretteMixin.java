package com.nextalubm.rotp_nextalbum.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.nextalubm.rotp_nextalbum.client.render.layer.CigaretteMouthLayer;

import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.LivingRenderer;
import net.minecraft.client.renderer.entity.PlayerRenderer;
import net.minecraft.client.renderer.entity.model.PlayerModel;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererCigaretteMixin extends LivingRenderer<AbstractClientPlayerEntity, PlayerModel<AbstractClientPlayerEntity>> {
    public PlayerRendererCigaretteMixin(EntityRendererManager manager, PlayerModel<AbstractClientPlayerEntity> model, float shadowRadius) {
        super(manager, model, shadowRadius);
    }

    @Inject(method = "<init>(Lnet/minecraft/client/renderer/entity/EntityRendererManager;Z)V", at = @At("RETURN"))
    private void rotpNextAlbum$addCigaretteMouthLayer(EntityRendererManager manager, boolean slim, CallbackInfo ci) {
        this.addLayer(new CigaretteMouthLayer((PlayerRenderer) (Object) this));
    }
}
