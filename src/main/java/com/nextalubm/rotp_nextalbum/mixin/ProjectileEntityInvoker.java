package com.nextalubm.rotp_nextalbum.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.math.EntityRayTraceResult;

@Mixin(ProjectileEntity.class)
public interface ProjectileEntityInvoker {
    @Invoker("onHitEntity")
    void rotpNextAlbum$onHitEntity(EntityRayTraceResult result);
}
