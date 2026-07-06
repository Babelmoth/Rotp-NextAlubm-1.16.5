package com.nextalubm.rotp_nextalbum.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.github.standobyte.jojo.init.ModStatusEffects;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.init.InitStands;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = NextAlubm.MOD_ID, bus = EventBusSubscriber.Bus.FORGE)
public final class SexPistolsStaminaUtil {
    private static final float NORMAL_FIRE_COST_PER_PISTOL = 40.0F;
    private static final float SPECIAL_FIRE_COST_PER_PISTOL = 50.0F;
    private static final float ENCIRCLEMENT_FIRE_EXTRA_COST = 60.0F;
    private static final float KICK_COST = 10.0F;
    private static final float ENCIRCLEMENT_UPKEEP_COST_PER_PISTOL_TICK = 1.35F;
    private static final float QUICK_RELOAD_BULLET_COST = 5.0F;
    private static final int STAMINA_RECOVERY_PAUSE_TICKS = 10;
    private static final int MAX_QUICK_RELOAD_BULLETS = 6;
    private static final Map<UUID, Integer> STAMINA_RECOVERY_PAUSES = new HashMap<>();

    private SexPistolsStaminaUtil() {}

    public static boolean consumeBulletFireStamina(LivingEntity user, int pistolMask, boolean encirclement, boolean piercingShot, boolean splittingShot) {
        boolean special = encirclement || piercingShot || splittingShot;
        int pistolCount = Integer.bitCount(pistolMask & 63);
        if (pistolCount <= 0 && !special) {
            return true;
        }
        float cost = Math.max(1, pistolCount) * (special ? SPECIAL_FIRE_COST_PER_PISTOL : NORMAL_FIRE_COST_PER_PISTOL);
        if (encirclement) {
            cost += ENCIRCLEMENT_FIRE_EXTRA_COST;
        }
        return consumeStamina(user, cost);
    }

    public static boolean consumeKickStamina(LivingEntity user) {
        return consumeStamina(user, KICK_COST);
    }

    public static boolean consumeEncirclementUpkeepStamina(LivingEntity user, int pistolMask) {
        int pistolCount = Math.max(1, Integer.bitCount(pistolMask & 63));
        float cost = ENCIRCLEMENT_UPKEEP_COST_PER_PISTOL_TICK * pistolCount * SexPistolsResolveUtil.getEncirclementUpkeepMultiplier(user);
        return consumeStamina(user, cost);
    }

    public static int getAffordableQuickReloadBullets(LivingEntity user) {
        Optional<IStandPower> powerOptional = getSexPistolsPower(user);
        if (!powerOptional.isPresent()) {
            return 0;
        }
        IStandPower power = powerOptional.get();
        if (!power.usesStamina() || power.isStaminaInfinite() || user.hasEffect(ModStatusEffects.RESOLVE.get())) {
            return MAX_QUICK_RELOAD_BULLETS;
        }
        return Math.min(MAX_QUICK_RELOAD_BULLETS, (int) (power.getStamina() / QUICK_RELOAD_BULLET_COST));
    }

    public static boolean consumeQuickReloadStamina(LivingEntity user, int bullets) {
        if (bullets <= 0) {
            return true;
        }
        return consumeStamina(user, QUICK_RELOAD_BULLET_COST * bullets);
    }

    public static boolean consumeStamina(LivingEntity user, float amount) {
        if (amount <= 0.0F) {
            return true;
        }
        Optional<IStandPower> powerOptional = getSexPistolsPower(user);
        if (!powerOptional.isPresent()) {
            return false;
        }
        IStandPower power = powerOptional.get();
        if (!power.usesStamina() || power.isStaminaInfinite()) {
            return true;
        }

        boolean isResolved = user.hasEffect(ModStatusEffects.RESOLVE.get());

        if (power.getStamina() < amount) {
            if (isResolved) {
                if (power.getStamina() > 0.0F) {
                    power.consumeStamina(power.getStamina());
                }
                pauseStaminaRecovery(user);
                return true;
            }
            return false;
        }

        boolean consumed = power.consumeStamina(amount);
        if (!consumed && isResolved) {
            if (power.getStamina() > 0.0F) {
                power.consumeStamina(power.getStamina());
            }
            consumed = true;
        }

        if (consumed) {
            pauseStaminaRecovery(user);
        }
        return consumed;
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
        Optional<IStandPower> powerOptional = getSexPistolsPower(player);
        if (!powerOptional.isPresent()) {
            return;
        }
        IStandPower power = powerOptional.get();
        if (!power.usesStamina() || power.isStaminaInfinite()) {
            return;
        }
        float regen = power.getStaminaTickGain();
        if (regen > 0.0F) {
            power.addStamina(-regen, true);
        }
    }

    private static void pauseStaminaRecovery(LivingEntity user) {
        if (user instanceof PlayerEntity && !user.level.isClientSide) {
            STAMINA_RECOVERY_PAUSES.put(user.getUUID(), STAMINA_RECOVERY_PAUSE_TICKS);
        }
    }

    private static Optional<IStandPower> getSexPistolsPower(LivingEntity user) {
        if (user == null) {
            return Optional.empty();
        }
        return IStandPower.getStandPowerOptional(user).resolve()
                .filter(power -> power.hasPower() && power.getType() == InitStands.STAND_SEX_PISTOLS.get());
    }
}