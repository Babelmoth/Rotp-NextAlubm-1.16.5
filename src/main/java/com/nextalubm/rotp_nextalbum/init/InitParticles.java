package com.nextalubm.rotp_nextalbum.init;

import com.mojang.serialization.Codec;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.particle.BulletHoleParticleData;

import net.minecraft.particles.BasicParticleType;
import net.minecraft.particles.ParticleType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class InitParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, NextAlubm.MOD_ID);

    public static final RegistryObject<ParticleType<BulletHoleParticleData>> BULLET_HOLE = PARTICLES.register("bullet_hole",
            () -> new ParticleType<BulletHoleParticleData>(false, BulletHoleParticleData.DESERIALIZER) {
                @Override
                public Codec<BulletHoleParticleData> codec() {
                    return BulletHoleParticleData.CODEC;
                }
            });
    public static final RegistryObject<BasicParticleType> REVOLVER_CASING = PARTICLES.register("revolver_casing", () -> new BasicParticleType(true));
    public static final RegistryObject<BasicParticleType> REVOLVER_MUZZLE_FLASH = PARTICLES.register("revolver_muzzle_flash", () -> new BasicParticleType(true));
    public static final RegistryObject<BasicParticleType> KICK_MUZZLE_FLASH = PARTICLES.register("kick_muzzle_flash", () -> new BasicParticleType(true));
    public static final RegistryObject<BasicParticleType> KICK_MUZZLE_FLASH_CRITICAL = PARTICLES.register("kick_muzzle_flash_critical", () -> new BasicParticleType(true));
    public static final RegistryObject<BasicParticleType> STAND_RESOLVE_AURA = PARTICLES.register("stand_resolve_aura", () -> new BasicParticleType(false));
}