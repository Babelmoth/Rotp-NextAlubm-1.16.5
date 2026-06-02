package com.nextalubm.rotp_nextalbum.event;

import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.util.AgingEntityUtil;
import com.nextalubm.rotp_nextalbum.util.AgingTextUtil;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = NextAlubm.MOD_ID, bus = EventBusSubscriber.Bus.FORGE)
public final class AgingEntityEvents {
    private AgingEntityEvents() {
    }

    @SubscribeEvent
    public static void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        LivingEntity entity = event.getEntityLiving();
        if (entity.level.isClientSide()) {
            return;
        }
        AgingEntityUtil.tickFading(entity);
        if (AgingEntityUtil.getProgress(entity) > 0F || entity instanceof StandEntity) {
            AgingEntityUtil.applyAgingAttributeModifiers(entity);
        }
        AgingEntityUtil.tickAgingSideEffects(entity);
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntityLiving();
        if (!entity.level.isClientSide()) {
            AgingEntityUtil.clearProgressImmediately(entity);
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        float multiplier = AgingEntityUtil.getDamageTakenMultiplier(event.getEntityLiving(), event.getSource());
        if (multiplier > 1F) {
            event.setAmount(event.getAmount() * multiplier);
        }
    }

    @SubscribeEvent
    public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
        LivingEntity entity = event.getEntityLiving();
        float multiplier = AgingEntityUtil.getJumpVelocityMultiplier(entity);
        if (multiplier < 0.999F) {
            entity.setDeltaMovement(entity.getDeltaMovement().multiply(1.0D, multiplier, 1.0D));
        }
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        float multiplier = AgingEntityUtil.getMiningSpeedMultiplier(event.getPlayer());
        if (multiplier < 0.999F) {
            event.setNewSpeed(event.getNewSpeed() * multiplier);
        }
    }

    @SubscribeEvent
    public static void onNameFormat(PlayerEvent.NameFormat event) {
        if (AgingEntityUtil.shouldObfuscateName(event.getPlayer())) {
            event.setDisplayname(AgingTextUtil.obfuscatedNameComponent(event.getDisplayname()));
        }
    }

    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        ServerPlayerEntity player = event.getPlayer();
        if (player != null && AgingEntityUtil.shouldObfuscateName(player)) {
            event.setComponent(new TranslationTextComponent("chat.type.text", player.getDisplayName(),
                    AgingTextUtil.obfuscatedMessageComponent(event.getMessage())));
        }
    }

    public static void syncExistingAgingOnLogin(ServerPlayerEntity player) {
        if (player.level instanceof ServerWorld && AgingEntityUtil.getProgress(player) > 0F) {
            AgingEntityUtil.applyAgingAttributeModifiers(player);
        }
    }
}