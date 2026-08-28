package com.nextalubm.rotp_nextalbum.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import com.github.standobyte.jojo.entity.damaging.DamagingEntity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.EntityRayTraceResult;

@Mixin(value = DamagingEntity.class, remap = false)
public interface DamagingEntityInvoker {
    @Invoker("hurtTarget")
    boolean rotpNextAlbum$hurtTarget(Entity target, LivingEntity owner);

    @Invoker("afterEntityHit")
    void rotpNextAlbum$afterEntityHit(EntityRayTraceResult entityRayTraceResult, boolean entityHurt);
}
