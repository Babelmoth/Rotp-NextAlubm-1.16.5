package com.nextalubm.rotp_nextalbum.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.init.InitStands;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = NextAlubm.MOD_ID, bus = EventBusSubscriber.Bus.FORGE)
public final class StandStaminaPauseUtil {
    private static final int STAMINA_RECOVERY_PAUSE_TICKS = 10;
    private static final Map<UUID, Integer> STAMINA_RECOVERY_PAUSES = new HashMap<>();

    private StandStaminaPauseUtil() {
    }

    public static void pause(@Nullable LivingEntity user) {
        if (user instanceof PlayerEntity && !user.level.isClientSide) {
            STAMINA_RECOVERY_PAUSES.put(user.getUUID(), STAMINA_RECOVERY_PAUSE_TICKS);
        }
    }

    public static boolean isTrackedStand(@Nullable IStandPower power) {
        return power != null && power.hasPower()
                && (power.getType() == InitStands.STAND_SEX_PISTOLS.get()
                || power.getType() == InitStands.STAND_THE_GRATEFUL_DEAD.get()
                || power.getType() == InitStands.STAND_BEACH_BOY.get());
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level.isClientSide) {
            return;
        }
        UUID playerId = event.player.getUUID();
        Integer ticks = STAMINA_RECOVERY_PAUSES.get(playerId);
        if (ticks == null) {
            return;
        }
        suppressStaminaRecovery(event.player);
        if (ticks <= 1) {
            STAMINA_RECOVERY_PAUSES.remove(playerId);
        }
        else {
            STAMINA_RECOVERY_PAUSES.put(playerId, ticks - 1);
        }
    }

    private static void suppressStaminaRecovery(PlayerEntity player) {
        IStandPower power = IStandPower.getStandPowerOptional(player).orElse(null);
        if (!isTrackedStand(power) || !power.usesStamina() || power.isStaminaInfinite()) {
            return;
        }
        float regen = power.getStaminaTickGain();
        if (regen > 0.0F) {
            power.addStamina(-regen, true);
        }
    }
}
