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

    public static final RegistryObject<SoundEvent> THE_GRATEFUL_DEAD_SUMMON_SHOUT = SOUNDS.register("prosciutto_the_grateful_dead",
            () -> new MultiSoundEvent(new ResourceLocation(NextAlubm.MOD_ID, "prosciutto_the_grateful_dead")));
    public static final RegistryObject<SoundEvent> THE_GRATEFUL_DEAD_SUMMON = SOUNDS.register("the_grateful_dead_summon",
            () -> new MultiSoundEvent(new ResourceLocation(NextAlubm.MOD_ID, "the_grateful_dead_summon"),
                    new ResourceLocation(NextAlubm.MOD_ID, "the_grateful_dead_summon_2")));
    public static final RegistryObject<SoundEvent> THE_GRATEFUL_DEAD_UNSUMMON = register("the_grateful_dead_unsummon");
    public static final RegistryObject<SoundEvent> PROSCIUTTO_DEAD = register("prosciutto_dead");
    public static final RegistryObject<SoundEvent> PROSCIUTTO_AGING_PUNCH = SOUNDS.register("prosciutto_aging_punch",
            () -> new MultiSoundEvent(new ResourceLocation(NextAlubm.MOD_ID, "prosciutto_kill"),
                    new ResourceLocation(NextAlubm.MOD_ID, "prosciutto_muda"),
                    new ResourceLocation(NextAlubm.MOD_ID, "prosciutto_take_this"),
                    new ResourceLocation(NextAlubm.MOD_ID, "prosciutto_ora"),
                    new ResourceLocation(NextAlubm.MOD_ID, "prosciutto_noisy")));
    public static final RegistryObject<SoundEvent> PROSCIUTTO_HEAVY_SHOUT = SOUNDS.register("prosciutto_heavy_shout",
            () -> new MultiSoundEvent(new ResourceLocation(NextAlubm.MOD_ID, "prosciutto_take_this"),
                    new ResourceLocation(NextAlubm.MOD_ID, "prosciutto_ora"),
                    new ResourceLocation(NextAlubm.MOD_ID, "prosciutto_noisy")));

    public static final RegistryObject<SoundEvent> PESCI_BEACH_BOY = SOUNDS.register("pesci_beach_boy",
            () -> new MultiSoundEvent(new ResourceLocation(NextAlubm.MOD_ID, "pesci_summon"),
                    new ResourceLocation(NextAlubm.MOD_ID, "pesci_summon_2")));
    public static final RegistryObject<SoundEvent> BEACH_BOY_SUMMON = register("beach_boy_summon");
    public static final RegistryObject<SoundEvent> BEACH_BOY_UNSUMMON = register("beach_boy_unsummon");
    public static final RegistryObject<SoundEvent> BEACH_BOY_REEL_BACK = register("beach_boy_reel_back");
    public static final RegistryObject<SoundEvent> PESCI_GET_MOB = SOUNDS.register("pesci_get_mob_shout",
            () -> new MultiSoundEvent(new ResourceLocation(NextAlubm.MOD_ID, "pesci_get_mob"),
                    new ResourceLocation(NextAlubm.MOD_ID, "pesci_get_mob_full")));
    public static final RegistryObject<SoundEvent> PESCI_REEL_IN = SOUNDS.register("pesci_reel_in",
            () -> new MultiSoundEvent(new ResourceLocation(NextAlubm.MOD_ID, "pesci_reel_you_in"),
                    new ResourceLocation(NextAlubm.MOD_ID, "pesci_wont_let_it_happen")));
    public static final RegistryObject<SoundEvent> PESCI_BROTHER = register("pesci_brother");
    public static final RegistryObject<SoundEvent> PESCI_KILL_YOU = register("pesci_kill_you");
    public static final RegistryObject<SoundEvent> PESCI_LOST_TARGET = register("pesci_lost_target");

    public static final RegistryObject<SoundEvent> REVOLVER_OPEN = register("revolver_open");
    public static final RegistryObject<SoundEvent> REVOLVER_CLOSE = register("revolver_close");
    public static final RegistryObject<SoundEvent> REVOLVER_SHELLS_OUT = register("revolver_shellsout");
    public static final RegistryObject<SoundEvent> REVOLVER_SPEEDLOADER = register("revolver_speedloader");
    public static final RegistryObject<SoundEvent> REVOLVER_FIRE = register("fire");
    public static final RegistryObject<SoundEvent> REVOLVER_SHOT = register("revolver_shot");
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