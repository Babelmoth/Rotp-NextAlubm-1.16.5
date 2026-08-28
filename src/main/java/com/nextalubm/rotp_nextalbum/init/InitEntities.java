package com.nextalubm.rotp_nextalbum.init;

import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.entity.BeachBoyHookEntity;
import com.nextalubm.rotp_nextalbum.entity.RevolverBulletEntity;
import com.nextalubm.rotp_nextalbum.entity.ThrownGrabBlockEntity;

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

    public static final RegistryObject<EntityType<ThrownGrabBlockEntity>> THROWN_GRAB_BLOCK = ENTITIES.register("thrown_grab_block",
            () -> EntityType.Builder.<ThrownGrabBlockEntity>of(ThrownGrabBlockEntity::new, EntityClassification.MISC)
                    .sized(0.98F, 0.98F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build(NextAlubm.MOD_ID + ":thrown_grab_block"));

    public static final RegistryObject<EntityType<BeachBoyHookEntity>> BEACH_BOY_HOOK = ENTITIES.register("beach_boy_hook",
            () -> EntityType.Builder.<BeachBoyHookEntity>of(BeachBoyHookEntity::new, EntityClassification.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build(NextAlubm.MOD_ID + ":beach_boy_hook"));
};
