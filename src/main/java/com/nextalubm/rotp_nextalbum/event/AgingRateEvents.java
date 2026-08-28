package com.nextalubm.rotp_nextalbum.event;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.compat.TemperatureCompat;
import com.nextalubm.rotp_nextalbum.util.AgingRateState;
import com.nextalubm.rotp_nextalbum.util.ResolveDownedUtil;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Food;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = NextAlubm.MOD_ID, bus = EventBusSubscriber.Bus.FORGE)
public final class AgingRateEvents {
    private static final Set<UUID> MINING_THIS_TICK = new HashSet<UUID>();

    private AgingRateEvents() {
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        PlayerEntity player = event.getPlayer();
        if (player == null || player.level.isClientSide()) {
            return;
        }
        MINING_THIS_TICK.add(player.getUUID());
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level.isClientSide()) {
            return;
        }
        PlayerEntity player = event.player;
        AgingRateState state = AgingRateState.of(player);
        if (player.isSprinting() && !player.isPassenger()) {
            state.addMoveHeat(AgingRateState.SPRINT_HEAT_PER_TICK);
        }
        if (MINING_THIS_TICK.remove(player.getUUID())) {
            state.addMoveHeat(AgingRateState.MINE_HEAT_PER_TICK);
        }
        IStandPower power = IStandPower.getStandPowerOptional(player).orElse(null);
        if (power != null && power.hasPower()) {
            float stamina = power.getStamina();
            float maxStamina = power.getMaxStamina();
            if (state.hasLastStamina && maxStamina > 0F && stamina < state.lastStamina) {
                float fraction = (state.lastStamina - stamina) / maxStamina;
                if (fraction > 0F) {
                    state.addStandHeat(AgingRateState.STAND_HEAT_CAP * fraction);
                }
            }
            state.lastStamina = stamina;
            state.hasLastStamina = true;
        }
        float digestionCap = TemperatureCompat.hasPreciseTemperature()
                ? AgingRateState.DIGESTION_CAP * 0.5F
                : AgingRateState.DIGESTION_CAP;
        state.tickDecay(digestionCap);
        state.tickDownedCool(ResolveDownedUtil.isDowned(player));
        state.save(player);
    }

    @SubscribeEvent
    public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
        LivingEntity entity = event.getEntityLiving();
        if (entity.level.isClientSide() || !(entity instanceof PlayerEntity)) {
            return;
        }
        AgingRateState state = AgingRateState.of(entity);
        state.addMoveHeat(AgingRateState.JUMP_HEAT);
        state.save(entity);
    }

    @SubscribeEvent
    public static void onFinishUsingItem(LivingEntityUseItemEvent.Finish event) {
        LivingEntity entity = event.getEntityLiving();
        if (entity.level.isClientSide() || !(entity instanceof PlayerEntity)) {
            return;
        }
        ItemStack stack = event.getItem();
        if (!stack.getItem().isEdible()) {
            return;
        }
        Food food = stack.getItem().getFoodProperties();
        if (food == null) {
            return;
        }
        float nutrition = food.getNutrition();
        float saturation = food.getSaturationModifier();
        float strength = (nutrition + saturation * nutrition * 2.0F) / 20.0F;
        strength = Math.max(0.15F, Math.min(1.0F, strength));
        if (TemperatureCompat.hasPreciseTemperature()) {
            strength *= 0.5F;
        }
        AgingRateState state = AgingRateState.of(entity);
        float cap = TemperatureCompat.hasPreciseTemperature()
                ? AgingRateState.DIGESTION_CAP * 0.5F
                : AgingRateState.DIGESTION_CAP;
        state.addDigestion(0.45F * strength, cap);
        state.save(entity);
    }
}
