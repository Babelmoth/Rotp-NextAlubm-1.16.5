package com.nextalubm.rotp_nextalbum.init;

import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.entity.RevolverBulletEntity;

import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class InitEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(
            ForgeRegistries.ENTITIES, NextAlubm.MOD_ID);
    
    
    
    public static final RegistryObject<EntityType<RevolverBulletEntity>> REVOLVER_BULLET = ENTITIES.register("revolver_bullet",
            () -> EntityType.Builder.<RevolverBulletEntity>of(RevolverBulletEntity::new, EntityClassification.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build(NextAlubm.MOD_ID + ":revolver_bullet"));
};
