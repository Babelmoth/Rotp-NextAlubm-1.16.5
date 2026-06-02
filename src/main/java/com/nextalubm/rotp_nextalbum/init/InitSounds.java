package com.nextalubm.rotp_nextalbum.init;

import com.github.standobyte.jojo.util.mc.OstSoundList;
import com.github.standobyte.jojo.util.mc.MultiSoundEvent;
import com.nextalubm.rotp_nextalbum.NextAlubm;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class InitSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(
            ForgeRegistries.SOUND_EVENTS, NextAlubm.MOD_ID);
    
    public static final RegistryObject<SoundEvent> SEX_PISTOLS_SUMMON = register("sex_pistols_summon");
    public static final RegistryObject<SoundEvent> SEX_PISTOLS_UNSUMMON = register("sex_pistols_unsummon");
    public static final RegistryObject<SoundEvent> SEX_PISTOLS_RICOCHET = SOUNDS.register("sex_pistols_ricochet", 
            () -> new MultiSoundEvent(new ResourceLocation(NextAlubm.MOD_ID, "sex_pistols_ricochet"), new ResourceLocation(NextAlubm.MOD_ID, "sex_pistols_ricochet_yari"), new ResourceLocation(NextAlubm.MOD_ID, "sex_pistols_ricochet_yorodomo")));
    public static final RegistryObject<SoundEvent> SEX_PISTOLS_RICOCHET_HIT = SOUNDS.register("sex_pistols_ricochet_hit", 
            () -> new MultiSoundEvent(new ResourceLocation(NextAlubm.MOD_ID, "sex_pistols_ricochet_hit"), new ResourceLocation(NextAlubm.MOD_ID, "sex_pistols_ricochet_hit_yaho"), new ResourceLocation(NextAlubm.MOD_ID, "sex_pistols_ricochet_hit_ye")));
    public static final RegistryObject<SoundEvent> SEX_PISTOLS_HUNGRY = SOUNDS.register("sex_pistols_hungry", 
            () -> new MultiSoundEvent(new ResourceLocation(NextAlubm.MOD_ID, "sex_pistols_hungry"), new ResourceLocation(NextAlubm.MOD_ID, "sex_pistols_hungry_2")));
    
    public static final RegistryObject<SoundEvent> MISTA_SEX_PISTOLS = SOUNDS.register("mista_sex_pistols", 
            () -> new MultiSoundEvent(new ResourceLocation(NextAlubm.MOD_ID, "mista_sex_pistols"), new ResourceLocation(NextAlubm.MOD_ID, "mista_pistols"))); 
    public static final RegistryObject<SoundEvent> MISTA_RECALL = SOUNDS.register("mista_recall", 
            () -> new MultiSoundEvent(new ResourceLocation(NextAlubm.MOD_ID, "mista_recall"), new ResourceLocation(NextAlubm.MOD_ID, "mista_recall_2")));
    public static final RegistryObject<SoundEvent> MISTA_LOADED_FIRE = SOUNDS.register("mista_loaded_fire", 
            () -> new MultiSoundEvent(new ResourceLocation(NextAlubm.MOD_ID, "mista_loaded_fire"), new ResourceLocation(NextAlubm.MOD_ID, "mista_loaded_fire_kurai"), new ResourceLocation(NextAlubm.MOD_ID, "mista_loaded_fire_sokoda"), new ResourceLocation(NextAlubm.MOD_ID, "mista_loaded_fire_yik")));
    public static final RegistryObject<SoundEvent> MISTA_STAND_RELOAD = register("mista_stand_reload");

    public static final RegistryObject<SoundEvent> THE_GRATEFUL_DEAD_SUMMON_SHOUT = SOUNDS.register("the_grateful_dead", 
            () -> new MultiSoundEvent(new ResourceLocation(NextAlubm.MOD_ID, "the_grateful_dead"), new ResourceLocation(NextAlubm.MOD_ID, "grateful_dead")));

    public static final RegistryObject<SoundEvent> REVOLVER_OPEN = register("revolver_open");
    public static final RegistryObject<SoundEvent> REVOLVER_CLOSE = register("revolver_close");
    public static final RegistryObject<SoundEvent> REVOLVER_SHELLS_OUT = register("revolver_shellsout");
    public static final RegistryObject<SoundEvent> REVOLVER_SPEEDLOADER = register("revolver_speedloader");
    public static final RegistryObject<SoundEvent> REVOLVER_FIRE = register("fire");
    public static final RegistryObject<SoundEvent> REVOLVER_DRY_FIRE = register("dry_fire");
    public static final RegistryObject<SoundEvent> SHELL_CASTING_1 = register("shell_casting_1");
    public static final RegistryObject<SoundEvent> SHELL_CASTING_2 = register("shell_casting_2");
    public static final RegistryObject<SoundEvent> SHELL_CASTING_3 = register("shell_casting_3");
    public static final RegistryObject<SoundEvent> SHELL_CASTING_4 = register("shell_casting_4");

    public static final OstSoundList SEX_PISTOLS_OST = new OstSoundList(
            new ResourceLocation(NextAlubm.MOD_ID, "sex_pistols_ost"), SOUNDS);

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> new SoundEvent(new ResourceLocation(NextAlubm.MOD_ID, name)));
    }
}