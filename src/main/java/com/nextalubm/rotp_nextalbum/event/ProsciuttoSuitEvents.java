package com.nextalubm.rotp_nextalbum.event;

import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.network.ProsciuttoShadowSyncPacket;
import com.nextalubm.rotp_nextalbum.util.ProsciuttoShadowUtil;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.PlaySoundAtEntityEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = NextAlubm.MOD_ID, bus = EventBusSubscriber.Bus.FORGE)
public class ProsciuttoSuitEvents {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level.isClientSide()) {
            return;
        }
        ProsciuttoShadowUtil.tick(event.player);
    }

    @SubscribeEvent
    public static void onPlayerNameFormat(PlayerEvent.NameFormat event) {
        if (ProsciuttoShadowUtil.isShadow(event.getPlayer())) {
            event.setDisplayname(new TranslationTextComponent("nextalbum.prosciutto_suit.unknown"));
        }
    }

    @SubscribeEvent
    public static void onTabListNameFormat(PlayerEvent.TabListNameFormat event) {
        if (ProsciuttoShadowUtil.isShadow(event.getPlayer())) {
            event.setDisplayName(event.getPlayer().getName());
        }
    }

    @SubscribeEvent
    public static void onPlaySoundAtEntity(PlaySoundAtEntityEvent event) {
        if (!(event.getEntity() instanceof PlayerEntity)) {
            return;
        }
        PlayerEntity player = (PlayerEntity) event.getEntity();
        if (!ProsciuttoShadowUtil.isShadow(player) || event.getCategory() != SoundCategory.PLAYERS || event.getSound() == null) {
            return;
        }
        String soundName = event.getSound().getLocation().getPath();
        if (soundName.contains("step") || soundName.contains("walk") || soundName.contains("footstep")) {
            event.setCanceled(true);
        }
        else {
            event.setVolume(event.getVolume() * 0.3F);
        }
    }

    @SubscribeEvent
    public static void onVisibility(LivingEvent.LivingVisibilityEvent event) {
        if (ProsciuttoShadowUtil.isShadow(event.getEntityLiving())) {
            event.modifyVisibility(ProsciuttoShadowUtil.SHADOW_VISIBILITY);
        }
    }

    @SubscribeEvent
    public static void onSetAttackTarget(LivingSetAttackTargetEvent event) {
        if (!(event.getEntityLiving() instanceof MobEntity) || event.getTarget() == null) {
            return;
        }
        MobEntity mob = (MobEntity) event.getEntityLiving();
        if (ProsciuttoShadowUtil.shouldLoseTarget(mob, event.getTarget())) {
            mob.setTarget(null);
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        ProsciuttoShadowUtil.markCombat(event.getPlayer());
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntityLiving();
        DamageSource source = event.getSource();
        if (!ProsciuttoShadowUtil.isCombatDamage(source)) {
            return;
        }
        if (entity instanceof PlayerEntity) {
            ProsciuttoShadowUtil.markCombat((PlayerEntity) entity);
        }
        PlayerEntity attacker = ProsciuttoShadowUtil.combatPlayerFrom(source);
        if (attacker != null) {
            ProsciuttoShadowUtil.markCombat(attacker);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getPlayer() instanceof ServerPlayerEntity) {
            ProsciuttoShadowSyncPacket.sync((ServerPlayerEntity) event.getPlayer());
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getPlayer() instanceof ServerPlayerEntity) {
            ProsciuttoShadowSyncPacket.sync((ServerPlayerEntity) event.getPlayer());
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayerEntity && event.getPlayer() instanceof ServerPlayerEntity) {
            ProsciuttoShadowSyncPacket.sync((ServerPlayerEntity) event.getTarget());
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            event.getPlayer().getPersistentData().putBoolean(ProsciuttoShadowUtil.NBT_SHADOW,
                    event.getOriginal().getPersistentData().getBoolean(ProsciuttoShadowUtil.NBT_SHADOW));
            event.getPlayer().getPersistentData().putLong(ProsciuttoShadowUtil.NBT_LAST_COMBAT,
                    event.getOriginal().getPersistentData().getLong(ProsciuttoShadowUtil.NBT_LAST_COMBAT));
        }
    }
}
