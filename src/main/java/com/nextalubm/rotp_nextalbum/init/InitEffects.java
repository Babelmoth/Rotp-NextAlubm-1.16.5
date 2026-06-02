package com.nextalubm.rotp_nextalbum.init;

import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.potion.CollapseEffect;
import com.nextalubm.rotp_nextalbum.potion.JoyfulEffect;

import net.minecraft.potion.Effect;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class InitEffects {
    public static final DeferredRegister<Effect> EFFECTS = DeferredRegister.create(ForgeRegistries.POTIONS, NextAlubm.MOD_ID);

    public static final RegistryObject<JoyfulEffect> JOYFUL = EFFECTS.register("joyful", JoyfulEffect::new);
    public static final RegistryObject<CollapseEffect> COLLAPSE = EFFECTS.register("collapse", CollapseEffect::new);
}