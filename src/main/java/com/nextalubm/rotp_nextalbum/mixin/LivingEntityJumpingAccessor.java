package com.nextalubm.rotp_nextalbum.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.entity.LivingEntity;

@Mixin(LivingEntity.class)
public interface LivingEntityJumpingAccessor {
    @Accessor("jumping")
    boolean rotpNextAlbum$isJumping();

    @Accessor("jumping")
    void rotpNextAlbum$setJumping(boolean jumping);

    @Invoker("jumpFromGround")
    void rotpNextAlbum$jumpFromGround();
}
