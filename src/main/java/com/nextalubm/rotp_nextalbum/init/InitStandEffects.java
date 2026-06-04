package com.nextalubm.rotp_nextalbum.init;

import com.github.standobyte.jojo.action.stand.effect.StandEffectType;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.stand.SexPistolsEntities;
import com.nextalubm.rotp_nextalbum.stand.TheGratefulDeadAgingAuraEffect;

import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;

public class InitStandEffects {
    public static final DeferredRegister<StandEffectType<?>> STAND_EFFECTS = DeferredRegister.create(
            (Class<StandEffectType<?>>) ((Class<?>) StandEffectType.class), NextAlubm.MOD_ID);
 
    public static final RegistryObject<StandEffectType<SexPistolsEntities>> SEX_PISTOLS_ENTITIES = STAND_EFFECTS.register("sex_pistols_entities",
            () -> new StandEffectType<>(SexPistolsEntities::new));

    public static final RegistryObject<StandEffectType<TheGratefulDeadAgingAuraEffect>> THE_GRATEFUL_DEAD_AGING_AURA = STAND_EFFECTS.register("the_grateful_dead_aging_aura",
            () -> new StandEffectType<>(TheGratefulDeadAgingAuraEffect::new));
}
