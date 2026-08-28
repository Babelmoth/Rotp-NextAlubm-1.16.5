package com.nextalubm.rotp_nextalbum.init;

import java.util.ArrayList;
import java.util.List;

import com.github.standobyte.jojo.action.Action;
import com.github.standobyte.jojo.action.stand.StandAction;
import com.github.standobyte.jojo.action.stand.StandEntityActionModifier;
import com.github.standobyte.jojo.action.stand.StandEntityBlock;
import com.github.standobyte.jojo.action.stand.StandEntityHeavyAttack;
import com.github.standobyte.jojo.action.stand.StandEntityLightAttack;
import com.github.standobyte.jojo.action.stand.StandEntityMeleeBarrage;
import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.entity.stand.StandEntityType;
import com.github.standobyte.jojo.entity.stand.StandPose;
import com.github.standobyte.jojo.init.ModSounds;
import com.github.standobyte.jojo.power.impl.stand.stats.StandStats;
import com.github.standobyte.jojo.power.impl.stand.type.EntityStandType;
import com.github.standobyte.jojo.power.impl.stand.type.StandType;
import com.github.standobyte.jojo.power.impl.stand.type.StandType.StandSurvivalGameplayPool;
import com.github.standobyte.jojo.util.mod.StoryPart;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.action.BeachBoyCrushDamageAction;
import com.nextalubm.rotp_nextalbum.action.BeachBoyHeartCrushAction;
import com.nextalubm.rotp_nextalbum.action.BeachBoyLineCollisionAction;
import com.nextalubm.rotp_nextalbum.action.BeachBoyRetractHookAction;
import com.nextalubm.rotp_nextalbum.action.BeachBoyRipAction;
import com.nextalubm.rotp_nextalbum.action.BeachBoySoundSenseAction;
import com.nextalubm.rotp_nextalbum.action.BeachBoyTrapAction;
import com.nextalubm.rotp_nextalbum.action.BeachBoyWhipAction;
import com.nextalubm.rotp_nextalbum.action.SexPistolsEncirclementAction;
import com.nextalubm.rotp_nextalbum.action.SexPistolsForcedTargetAction;
import com.nextalubm.rotp_nextalbum.action.SexPistolsMoveSelectedAction;
import com.nextalubm.rotp_nextalbum.action.SexPistolsPiercingShotAction;
import com.nextalubm.rotp_nextalbum.action.SexPistolsRecallAction;
import com.nextalubm.rotp_nextalbum.action.SexPistolsSplittingShotAction;
import com.nextalubm.rotp_nextalbum.action.SexPistolsStandReloadAction;
import com.nextalubm.rotp_nextalbum.action.SexPistolsTargetModeAction;
import com.nextalubm.rotp_nextalbum.action.SexPistolsTrajectoryVisionAction;
import com.nextalubm.rotp_nextalbum.action.SexPistolsTransferOrderAction;
import com.nextalubm.rotp_nextalbum.action.TheGratefulDeadAgingAuraAction;
import com.nextalubm.rotp_nextalbum.action.TheGratefulDeadAgingInjectAction;
import com.nextalubm.rotp_nextalbum.action.TheGratefulDeadAgingSelfAction;
import com.nextalubm.rotp_nextalbum.action.TheGratefulDeadAgingTouchAction;
import com.nextalubm.rotp_nextalbum.action.TheGratefulDeadBarrageAction;
import com.nextalubm.rotp_nextalbum.action.TheGratefulDeadBlockAction;
import com.nextalubm.rotp_nextalbum.action.TheGratefulDeadFinisher;
import com.nextalubm.rotp_nextalbum.action.TheGratefulDeadGrabAction;
import com.nextalubm.rotp_nextalbum.action.TheGratefulDeadHeavyAttack;
import com.nextalubm.rotp_nextalbum.action.TheGratefulDeadLightAttack;
import com.nextalubm.rotp_nextalbum.action.TheGratefulDeadPlaceAction;
import com.nextalubm.rotp_nextalbum.action.TheGratefulDeadRemoveAgingAction;
import com.nextalubm.rotp_nextalbum.action.TheGratefulDeadRemoveAllAgingAction;
import com.nextalubm.rotp_nextalbum.action.TheGratefulDeadRemoveSelfAgingAction;
import com.nextalubm.rotp_nextalbum.action.TheGratefulDeadThrowAction;
import com.nextalubm.rotp_nextalbum.entity.BeachBoyStandEntity;
import com.nextalubm.rotp_nextalbum.entity.SexPistolsEntity;
import com.nextalubm.rotp_nextalbum.entity.SilverChariotRequiemEntity;
import com.nextalubm.rotp_nextalbum.entity.TheGratefulDeadEntity;
import com.nextalubm.rotp_nextalbum.entity.TheSunEntity;
import com.nextalubm.rotp_nextalbum.stand.NextAlbumStandStats;
import com.nextalubm.rotp_nextalbum.stand.SexPistolsStandType;
import com.nextalubm.rotp_nextalbum.stand.TheSunStandType;

import net.minecraft.util.Util;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.DeferredRegister;

@EventBusSubscriber(modid = NextAlubm.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class InitStands {
    @SuppressWarnings("unchecked")
    public static final DeferredRegister<Action<?>> ACTIONS = DeferredRegister.create(
            (Class<Action<?>>) ((Class<?>) Action.class), NextAlubm.MOD_ID);
    @SuppressWarnings("unchecked")
    public static final DeferredRegister<StandType<?>> STANDS = DeferredRegister.create(
            (Class<StandType<?>>) ((Class<?>) StandType.class), NextAlubm.MOD_ID);

    public static final ITextComponent PART_3_NAME = StoryPart.STARDUST_CRUSADERS.getName();
    public static final ITextComponent PART_4_NAME = StoryPart.DIAMOND_IS_UNBREAKABLE.getName();
    public static final ITextComponent PART_5_NAME = StoryPart.GOLDEN_WIND.getName();


// ======================================== The Sun ========================================

    public static final RegistryObject<StandEntityType<? extends TheSunEntity>> THE_SUN_ENTITY = InitEntities.ENTITIES.register("the_sun",
            () -> new StandEntityType<TheSunEntity>(TheSunEntity::new, 1.5F, 1.5F));

    public static final RegistryObject<TheSunStandType> STAND_THE_SUN = STANDS.register("the_sun",
            () -> new TheSunStandType(new TheSunStandType.Builder<StandStats>()
                    .color(0xffd24a)
                    .storyPartName(StoryPart.STARDUST_CRUSADERS.getName())
                    .leftClickHotbar()
                    .rightClickHotbar()
                    .defaultStats(StandStats.class, new StandStats.Builder()
                            .tier(4)
                            .power(16)
                            .speed(10)
                            .range(500, 500)
                            .durability(20)
                            .precision(0)
                            .randomWeight(0)
                            .build())
                    .setSurvivalGameplayPool(StandSurvivalGameplayPool.OTHER)));


// ======================================== Sex Pistols ========================================

    public static final RegistryObject<SexPistolsForcedTargetAction> SEX_PISTOLS_FORCED_TARGET = ACTIONS.register("sex_pistols_forced_target",
            () -> new SexPistolsForcedTargetAction(new StandAction.Builder()
                                .resolveLevelToUnlock(0)
                                .autoSummonStand()
                                .cooldown(4)));
    public static final RegistryObject<SexPistolsTrajectoryVisionAction> SEX_PISTOLS_TRAJECTORY_VISION = ACTIONS.register("sex_pistols_trajectory_vision",
            () -> new SexPistolsTrajectoryVisionAction(new StandAction.Builder()));
    public static final RegistryObject<SexPistolsTargetModeAction> SEX_PISTOLS_TARGET_MODE = ACTIONS.register("sex_pistols_target_mode",
            () -> new SexPistolsTargetModeAction(new StandAction.Builder()
                                .resolveLevelToUnlock(2)
                                .autoSummonStand()
                                .cooldown(4)));
    public static final RegistryObject<SexPistolsTransferOrderAction> SEX_PISTOLS_TRANSFER_ORDER = ACTIONS.register("sex_pistols_transfer_order",
            () -> new SexPistolsTransferOrderAction(new StandAction.Builder()
                                .resolveLevelToUnlock(0)
                                .autoSummonStand()
                                .cooldown(4)));
    public static final RegistryObject<SexPistolsStandReloadAction> SEX_PISTOLS_STAND_RELOAD = ACTIONS.register("sex_pistols_stand_reload",
            () -> new SexPistolsStandReloadAction(new StandAction.Builder()
                                .resolveLevelToUnlock(0)
                                .autoSummonStand()
                                .cooldown(0), false));
    public static final RegistryObject<SexPistolsStandReloadAction> SEX_PISTOLS_QUICK_RELOAD = ACTIONS.register("sex_pistols_quick_reload",
            () -> new SexPistolsStandReloadAction(new StandAction.Builder()
                                .resolveLevelToUnlock(1)
                                .autoSummonStand()
                                .cooldown(40)
                                .shiftVariationOf(SEX_PISTOLS_STAND_RELOAD), true));
    public static final RegistryObject<SexPistolsEncirclementAction> SEX_PISTOLS_ENCIRCLEMENT = ACTIONS.register("sex_pistols_encirclement",
            () -> new SexPistolsEncirclementAction(new StandAction.Builder()
                                .resolveLevelToUnlock(4)
                                .autoSummonStand()
                                .cooldown(240, 0, 0.5f)));
    public static final RegistryObject<SexPistolsPiercingShotAction> SEX_PISTOLS_PIERCING_SHOT = ACTIONS.register("sex_pistols_piercing_shot",
            () -> new SexPistolsPiercingShotAction(new StandAction.Builder()
                                .resolveLevelToUnlock(2)
                                .autoSummonStand()
                                .cooldown(160, 0, 0.5f)));
    public static final RegistryObject<SexPistolsSplittingShotAction> SEX_PISTOLS_SPLITTING_SHOT = ACTIONS.register("sex_pistols_splitting_shot",
            () -> new SexPistolsSplittingShotAction(new StandAction.Builder()
                                .resolveLevelToUnlock(3)
                                .autoSummonStand()
                                .cooldown(240, 0, 0.5f)));
    public static final RegistryObject<SexPistolsRecallAction> SEX_PISTOLS_RECALL = ACTIONS.register("sex_pistols_recall",
            () -> new SexPistolsRecallAction(new StandAction.Builder()
                                .resolveLevelToUnlock(0)
                                .autoSummonStand()
                                .cooldown(10)));
    public static final RegistryObject<SexPistolsMoveSelectedAction> SEX_PISTOLS_MOVE_SELECTED = ACTIONS.register("sex_pistols_move_selected",
            () -> new SexPistolsMoveSelectedAction(new StandAction.Builder()
                                .resolveLevelToUnlock(1)
                                .autoSummonStand()
                                .cooldown(4)));

    public static final RegistryObject<StandEntityType<? extends SexPistolsEntity>> SEX_PISTOLS_1 = InitEntities.ENTITIES.register("sex_pistols_1",
            () -> new StandEntityType<SexPistolsEntity>(SexPistolsEntity::new, 0.35F, 0.7F)
                    .summonSound(InitSounds.SEX_PISTOLS_SUMMON)
                    .unsummonSound(InitSounds.SEX_PISTOLS_UNSUMMON));
    public static final RegistryObject<StandEntityType<? extends SexPistolsEntity>> SEX_PISTOLS_2 = InitEntities.ENTITIES.register("sex_pistols_2",
            () -> new StandEntityType<SexPistolsEntity>(SexPistolsEntity::new, 0.35F, 0.7F));
    public static final RegistryObject<StandEntityType<? extends SexPistolsEntity>> SEX_PISTOLS_3 = InitEntities.ENTITIES.register("sex_pistols_3",
            () -> new StandEntityType<SexPistolsEntity>(SexPistolsEntity::new, 0.35F, 0.7F));
    public static final RegistryObject<StandEntityType<? extends SexPistolsEntity>> SEX_PISTOLS_5 = InitEntities.ENTITIES.register("sex_pistols_5",
            () -> new StandEntityType<SexPistolsEntity>(SexPistolsEntity::new, 0.35F, 0.7F));
    public static final RegistryObject<StandEntityType<? extends SexPistolsEntity>> SEX_PISTOLS_6 = InitEntities.ENTITIES.register("sex_pistols_6",
            () -> new StandEntityType<SexPistolsEntity>(SexPistolsEntity::new, 0.35F, 0.7F));
    public static final RegistryObject<StandEntityType<? extends SexPistolsEntity>> SEX_PISTOLS_7 = InitEntities.ENTITIES.register("sex_pistols_7",
            () -> new StandEntityType<SexPistolsEntity>(SexPistolsEntity::new, 0.35F, 0.7F));

    public static final List<RegistryObject<StandEntityType<? extends SexPistolsEntity>>> SEX_PISTOLS_ENTITY_TYPES = Util.make(new ArrayList<>(), list -> {
        list.add(SEX_PISTOLS_1);
        list.add(SEX_PISTOLS_2);
        list.add(SEX_PISTOLS_3);
        list.add(SEX_PISTOLS_5);
        list.add(SEX_PISTOLS_6);
        list.add(SEX_PISTOLS_7);
    });

    public static final int[] SEX_PISTOLS_NUMBERS = {1, 2, 3, 5, 6, 7};

    public static final RegistryObject<SexPistolsStandType> STAND_SEX_PISTOLS = STANDS.register("sex_pistols",
            () -> new SexPistolsStandType(new SexPistolsStandType.Builder<StandStats>()
                    .color(0xe75d2f)
                    .storyPartName(StoryPart.GOLDEN_WIND.getName())
                    .leftClickHotbar(
                            SEX_PISTOLS_FORCED_TARGET.get(),
                            SEX_PISTOLS_STAND_RELOAD.get(),
                            SEX_PISTOLS_PIERCING_SHOT.get(),
                            SEX_PISTOLS_SPLITTING_SHOT.get(),
                            SEX_PISTOLS_ENCIRCLEMENT.get())
                    .rightClickHotbar(
                            SEX_PISTOLS_TARGET_MODE.get(),
                            SEX_PISTOLS_TRANSFER_ORDER.get(),
                            SEX_PISTOLS_MOVE_SELECTED.get(),
                            SEX_PISTOLS_RECALL.get())
                    .defaultStats(StandStats.class, new StandStats.Builder()
                            .tier(5)
                            .power(2, 2)
                            .speed(8, 14)
                            .range(40, 40)
                            .durability(12, 16)
                            .precision(16, 20)
                            .randomWeight(1)
                            .build())
                    .addSummonShout(InitSounds.MISTA_SEX_PISTOLS)
                    .addOst(InitSounds.SEX_PISTOLS_OST)));


// ======================================== The Grateful Dead ========================================

    public static final RegistryObject<StandEntityLightAttack> THE_GRATEFUL_DEAD_PUNCH = ACTIONS.register("the_grateful_dead_punch",
            () -> new TheGratefulDeadLightAttack(new StandEntityLightAttack.Builder()
                    .punchSound(ModSounds.STAND_PUNCH_LIGHT)
                    .swingSound(ModSounds.STAND_PUNCH_SWING)));
    public static final RegistryObject<StandEntityMeleeBarrage> THE_GRATEFUL_DEAD_BARRAGE = ACTIONS.register("the_grateful_dead_barrage",
            () -> new TheGratefulDeadBarrageAction(new StandEntityMeleeBarrage.Builder()
                    .resolveLevelToUnlock(1)
                    .barrageHitSound(ModSounds.STAND_PUNCH_LIGHT)
                    .barrageSwingSound(ModSounds.STAND_PUNCH_BARRAGE_SWING)));
    public static final RegistryObject<StandEntityActionModifier> THE_GRATEFUL_DEAD_AGING_INJECT = ACTIONS.register("the_grateful_dead_aging_inject",
            () -> new TheGratefulDeadAgingInjectAction(new StandAction.Builder().staminaCost(15)
                    .resolveLevelToUnlock(2)
                    .autoSummonStand()
                    .shout(InitSounds.PROSCIUTTO_AGING_PUNCH)));
    public static final RegistryObject<TheGratefulDeadFinisher> THE_GRATEFUL_DEAD_FINISHER = ACTIONS.register("the_grateful_dead_finisher",
            () -> new TheGratefulDeadFinisher(new StandEntityHeavyAttack.Builder()
                    .resolveLevelToUnlock(1)
                    .autoSummonStand()
                    .punchSound(ModSounds.STAND_PUNCH_HEAVY)
                    .swingSound(ModSounds.STAND_PUNCH_HEAVY_SWING)
                    .shout(InitSounds.PROSCIUTTO_AGING_PUNCH)
                    .attackRecoveryFollowup(THE_GRATEFUL_DEAD_AGING_INJECT)
                    .standPose(StandPose.HEAVY_ATTACK_FINISHER)));
    public static final RegistryObject<StandEntityHeavyAttack> THE_GRATEFUL_DEAD_HEAVY_PUNCH = ACTIONS.register("the_grateful_dead_heavy_punch",
            () -> new TheGratefulDeadHeavyAttack(new StandEntityHeavyAttack.Builder()
                    .resolveLevelToUnlock(0)
                    .autoSummonStand()
                    .punchSound(ModSounds.STAND_PUNCH_HEAVY)
                    .swingSound(ModSounds.STAND_PUNCH_HEAVY_SWING)
                    .shout(InitSounds.PROSCIUTTO_HEAVY_SHOUT)
                    .attackRecoveryFollowup(THE_GRATEFUL_DEAD_AGING_INJECT)
                    .setFinisherVariation(THE_GRATEFUL_DEAD_FINISHER)
                    .shiftVariationOf(THE_GRATEFUL_DEAD_PUNCH)
                    .shiftVariationOf(THE_GRATEFUL_DEAD_BARRAGE)));
    public static final RegistryObject<StandEntityBlock> THE_GRATEFUL_DEAD_BLOCK = ACTIONS.register("the_grateful_dead_block",
            () -> new TheGratefulDeadBlockAction());
    public static final RegistryObject<TheGratefulDeadAgingTouchAction> THE_GRATEFUL_DEAD_AGING_TOUCH = ACTIONS.register("the_grateful_dead_aging_touch",
            () -> new TheGratefulDeadAgingTouchAction(new StandAction.Builder()
                    .autoSummonStand()
                    .staminaCostTick(1.25F)));
    public static final RegistryObject<TheGratefulDeadAgingSelfAction> THE_GRATEFUL_DEAD_AGING_SELF = ACTIONS.register("the_grateful_dead_aging_self",
            () -> new TheGratefulDeadAgingSelfAction(new StandAction.Builder()
                    .resolveLevelToUnlock(1)
                    .autoSummonStand()
                    .staminaCostTick(1.5F)));
    public static final RegistryObject<TheGratefulDeadRemoveSelfAgingAction> THE_GRATEFUL_DEAD_REMOVE_SELF_AGING = ACTIONS.register("the_grateful_dead_remove_self_aging",
            () -> new TheGratefulDeadRemoveSelfAgingAction(new StandAction.Builder()
                    .resolveLevelToUnlock(1)
                    .autoSummonStand()
                    .cooldown(40)
                    .shiftVariationOf(THE_GRATEFUL_DEAD_AGING_SELF)));
    public static final RegistryObject<TheGratefulDeadAgingAuraAction> THE_GRATEFUL_DEAD_AGING_AURA = ACTIONS.register("the_grateful_dead_aging_aura",
            () -> new TheGratefulDeadAgingAuraAction(new StandAction.Builder()
                    .resolveLevelToUnlock(2)
                    .autoSummonStand()
                    .staminaCost(8)
                    .cooldown(10)));
    public static final RegistryObject<TheGratefulDeadRemoveAgingAction> THE_GRATEFUL_DEAD_REMOVE_AGING = ACTIONS.register("the_grateful_dead_remove_aging",
            () -> new TheGratefulDeadRemoveAgingAction(new StandAction.Builder()
                    .resolveLevelToUnlock(0)
                    .autoSummonStand()
                    .cooldown(40)));
    public static final RegistryObject<TheGratefulDeadRemoveAllAgingAction> THE_GRATEFUL_DEAD_REMOVE_ALL_AGING = ACTIONS.register("the_grateful_dead_remove_all_aging",
            () -> new TheGratefulDeadRemoveAllAgingAction(new StandAction.Builder()
                    .resolveLevelToUnlock(0)
                    .autoSummonStand()
                    .cooldown(40)
                    .shiftVariationOf(THE_GRATEFUL_DEAD_REMOVE_AGING)));
    public static final RegistryObject<TheGratefulDeadGrabAction> THE_GRATEFUL_DEAD_GRAB = ACTIONS.register("the_grateful_dead_grab",
            () -> new TheGratefulDeadGrabAction(new TheGratefulDeadGrabAction.Builder()
                    .resolveLevelToUnlock(3)
                    .autoSummonStand()
                    .staminaCost(25)));
    public static final RegistryObject<TheGratefulDeadThrowAction> THE_GRATEFUL_DEAD_THROW = ACTIONS.register("the_grateful_dead_throw",
            () -> new TheGratefulDeadThrowAction(new TheGratefulDeadThrowAction.Builder()
                    .resolveLevelToUnlock(3)
                    .autoSummonStand()
                    .staminaCost(20)
                    .cooldown(16)
                    .shout(InitSounds.PROSCIUTTO_HEAVY_SHOUT)
                    .shiftVariationOf(THE_GRATEFUL_DEAD_GRAB)));
    public static final RegistryObject<TheGratefulDeadPlaceAction> THE_GRATEFUL_DEAD_PLACE = ACTIONS.register("the_grateful_dead_place",
            () -> new TheGratefulDeadPlaceAction(new TheGratefulDeadPlaceAction.Builder()
                    .resolveLevelToUnlock(3)
                    .autoSummonStand()
                    .staminaCost(5)
                    .cooldown(6)
                    .shiftVariationOf(THE_GRATEFUL_DEAD_THROW)));

    public static final RegistryObject<StandEntityType<? extends TheGratefulDeadEntity>> THE_GRATEFUL_DEAD_ENTITY = InitEntities.ENTITIES.register("the_grateful_dead",
            () -> new StandEntityType<TheGratefulDeadEntity>(TheGratefulDeadEntity::new, 0.65F, 1.8F)
                    .summonSound(InitSounds.THE_GRATEFUL_DEAD_SUMMON)
                    .unsummonSound(InitSounds.THE_GRATEFUL_DEAD_UNSUMMON));

    public static final RegistryObject<EntityStandType<NextAlbumStandStats>> STAND_THE_GRATEFUL_DEAD = STANDS.register("the_grateful_dead",
            () -> new EntityStandType.Builder<NextAlbumStandStats>()
                    .color(0x694bdb)
                    .storyPartName(StoryPart.GOLDEN_WIND.getName())
                    .leftClickHotbar(
                            THE_GRATEFUL_DEAD_PUNCH.get(),
                            THE_GRATEFUL_DEAD_BARRAGE.get(),
                            THE_GRATEFUL_DEAD_GRAB.get())
                    .rightClickHotbar(
                            THE_GRATEFUL_DEAD_BLOCK.get(),
                            THE_GRATEFUL_DEAD_AGING_TOUCH.get(),
                            THE_GRATEFUL_DEAD_AGING_SELF.get(),
                            THE_GRATEFUL_DEAD_AGING_AURA.get(),
                            THE_GRATEFUL_DEAD_REMOVE_AGING.get())
                    .defaultStats(NextAlbumStandStats.class, new NextAlbumStandStats.Builder()
                            .tier(5)
                            .power(10,12)
                            .speed(8,10)
                            .range(10, 10)
                            .durability(16,20)
                            .precision(6,8)
                            .abilityRange(100, 100)
                            .randomWeight(2)
                            .build())
                    .addSummonShout(InitSounds.THE_GRATEFUL_DEAD_SUMMON_SHOUT)
                    .build());


// ======================================== Beach Boy ========================================

    public static final RegistryObject<BeachBoyLineCollisionAction> BEACH_BOY_LINE_COLLISION = ACTIONS.register("beach_boy_line_collision",
            () -> new BeachBoyLineCollisionAction(new StandAction.Builder()
                    .resolveLevelToUnlock(0)
                    .autoSummonStand()
                    .cooldown(4)));
    public static final RegistryObject<BeachBoyCrushDamageAction> BEACH_BOY_CRUSH_DAMAGE = ACTIONS.register("beach_boy_crush_damage",
            () -> new BeachBoyCrushDamageAction(new StandAction.Builder()
                    .resolveLevelToUnlock(0)
                    .autoSummonStand()
                    .cooldown(4)));
    public static final RegistryObject<BeachBoyRetractHookAction> BEACH_BOY_RETRACT_HOOK = ACTIONS.register("beach_boy_retract_hook",
            () -> new BeachBoyRetractHookAction(new StandAction.Builder()
                    .resolveLevelToUnlock(0)
                    .autoSummonStand()
                    .cooldown(10), true));
    public static final RegistryObject<BeachBoyRetractHookAction> BEACH_BOY_QUICK_REEL = ACTIONS.register("beach_boy_quick_reel",
            () -> new BeachBoyRetractHookAction(new StandAction.Builder()
                    .resolveLevelToUnlock(0)
                    .autoSummonStand()
                    .cooldown(10)
                    .shiftVariationOf(BEACH_BOY_RETRACT_HOOK), false));
    public static final RegistryObject<BeachBoySoundSenseAction> BEACH_BOY_SOUND_SENSE = ACTIONS.register("beach_boy_sound_sense",
            () -> new BeachBoySoundSenseAction(new StandAction.Builder()
                    .resolveLevelToUnlock(2)
                    .autoSummonStand()
                    .cooldown(4)));
    public static final RegistryObject<BeachBoyWhipAction> BEACH_BOY_WHIP = ACTIONS.register("beach_boy_whip",
            () -> new BeachBoyWhipAction(new StandAction.Builder()
                    .resolveLevelToUnlock(1)
                    .autoSummonStand()
                    .staminaCost(40)
                    .cooldown(12), false));
    public static final RegistryObject<BeachBoyWhipAction> BEACH_BOY_SLASH = ACTIONS.register("beach_boy_slash",
            () -> new BeachBoyWhipAction(new StandAction.Builder()
                    .resolveLevelToUnlock(2)
                    .autoSummonStand()
                    .staminaCost(40)
                    .cooldown(12)
                    .shiftVariationOf(BEACH_BOY_WHIP), true));
    public static final RegistryObject<BeachBoyRipAction> BEACH_BOY_RIP = ACTIONS.register("beach_boy_rip",
            () -> new BeachBoyRipAction(new StandAction.Builder()
                    .resolveLevelToUnlock(3)
                    .autoSummonStand()
                    .staminaCost(55)
                    .cooldown(48)));
    public static final RegistryObject<BeachBoyHeartCrushAction> BEACH_BOY_HEART_CRUSH = ACTIONS.register("beach_boy_heart_crush",
            () -> new BeachBoyHeartCrushAction(new StandAction.Builder()
                    .resolveLevelToUnlock(4)
                    .autoSummonStand()
                    .staminaCost(110)
                    .cooldown(160)
                    .shiftVariationOf(BEACH_BOY_RIP)));
    public static final RegistryObject<BeachBoyTrapAction> BEACH_BOY_TRAP = ACTIONS.register("beach_boy_trap",
            () -> new BeachBoyTrapAction(new StandAction.Builder()
                    .resolveLevelToUnlock(2)
                    .autoSummonStand()
                    .staminaCost(50)
                    .cooldown(30)));
    public static final RegistryObject<BeachBoyRetractHookAction> BEACH_BOY_TRAP_RETRACT = ACTIONS.register("beach_boy_trap_retract",
            () -> new BeachBoyRetractHookAction(new StandAction.Builder()
                    .resolveLevelToUnlock(2)
                    .autoSummonStand()
                    .staminaCost(5)
                    .cooldown(10)
                    .shiftVariationOf(BEACH_BOY_TRAP), false, "beach_boy_trap_retract"));

    public static final RegistryObject<StandEntityType<? extends BeachBoyStandEntity>> BEACH_BOY_ENTITY = InitEntities.ENTITIES.register("beach_boy",
            () -> new StandEntityType<BeachBoyStandEntity>(BeachBoyStandEntity::new, 0.0F, 0.0F)
                    .summonSound(InitSounds.BEACH_BOY_SUMMON)
                    .unsummonSound(InitSounds.BEACH_BOY_UNSUMMON));

    public static final RegistryObject<EntityStandType<StandStats>> STAND_BEACH_BOY = STANDS.register("beach_boy",
            () -> new EntityStandType.Builder<StandStats>()
                    .color(0x7eb3d6)
                    .storyPartName(StoryPart.GOLDEN_WIND.getName())
                    .leftClickHotbar(
                            BEACH_BOY_WHIP.get(),
                            BEACH_BOY_RIP.get(),
                            BEACH_BOY_TRAP.get())
                    .rightClickHotbar(
                            BEACH_BOY_RETRACT_HOOK.get(),
                            BEACH_BOY_LINE_COLLISION.get(),
                            BEACH_BOY_CRUSH_DAMAGE.get(),
                            BEACH_BOY_SOUND_SENSE.get())
                    .defaultStats(StandStats.class, new StandStats.Builder()
                            .tier(5)
                            .power(8,10)
                            .speed(14,16)
                            .range(100, 100)
                            .durability(8,10)
                            .precision(16,18)
                            .randomWeight(2)
                            .build())
                    .disableStandLeap()
                    .addSummonShout(InitSounds.PESCI_BEACH_BOY)
                    .build());


// ======================================== Silver Chariot Requiem ========================================

    public static final RegistryObject<StandEntityType<? extends SilverChariotRequiemEntity>> SILVER_CHARIOT_REQUIEM_ENTITY = InitEntities.ENTITIES.register("silver_chariot_requiem",
            () -> new StandEntityType<SilverChariotRequiemEntity>(SilverChariotRequiemEntity::new, 0.65F, 1.95F));

    public static final RegistryObject<EntityStandType<StandStats>> STAND_SILVER_CHARIOT_REQUIEM = STANDS.register("silver_chariot_requiem",
            () -> new EntityStandType.Builder<StandStats>()
                    .color(0x262430)
                    .storyPartName(StoryPart.GOLDEN_WIND.getName())
                    .leftClickHotbar()
                    .rightClickHotbar()
                    .defaultStats(StandStats.class, new StandStats.Builder()
                            .tier(6)
                            .power(0)
                            .speed(0)
                            .range(80, 80)
                            .durability(20)
                            .precision(0)
                            .randomWeight(0)
                            .build())
                    .setSurvivalGameplayPool(StandSurvivalGameplayPool.OTHER)
                    .build());


    @SubscribeEvent
    public static void createDefaultStandAttributes(EntityAttributeCreationEvent event) {
        event.put(THE_SUN_ENTITY.get(), StandEntity.createAttributes().build());
        for (RegistryObject<StandEntityType<? extends SexPistolsEntity>> entityType : SEX_PISTOLS_ENTITY_TYPES) {
            event.put(entityType.get(), StandEntity.createAttributes().build());
        }
        event.put(THE_GRATEFUL_DEAD_ENTITY.get(), StandEntity.createAttributes().build());
        event.put(BEACH_BOY_ENTITY.get(), StandEntity.createAttributes().build());
        event.put(SILVER_CHARIOT_REQUIEM_ENTITY.get(), StandEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void afterStandsRegister(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            THE_SUN_ENTITY.get().setStandType(STAND_THE_SUN);
            STAND_THE_SUN.get().setEntityType(THE_SUN_ENTITY);
            for (RegistryObject<StandEntityType<? extends SexPistolsEntity>> entityType : SEX_PISTOLS_ENTITY_TYPES) {
                entityType.get().setStandType(STAND_SEX_PISTOLS);
            }
            STAND_SEX_PISTOLS.get().setEntityType(SEX_PISTOLS_1);
            THE_GRATEFUL_DEAD_ENTITY.get().setStandType(STAND_THE_GRATEFUL_DEAD);
            STAND_THE_GRATEFUL_DEAD.get().setEntityType(THE_GRATEFUL_DEAD_ENTITY);
            BEACH_BOY_ENTITY.get().setStandType(STAND_BEACH_BOY);
            STAND_BEACH_BOY.get().setEntityType(BEACH_BOY_ENTITY);
            SILVER_CHARIOT_REQUIEM_ENTITY.get().setStandType(STAND_SILVER_CHARIOT_REQUIEM);
            STAND_SILVER_CHARIOT_REQUIEM.get().setEntityType(SILVER_CHARIOT_REQUIEM_ENTITY);
        });
    }
}
