package com.nextalubm.rotp_nextalbum.action;

import java.util.Optional;

import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.action.stand.StandAction;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.nextalubm.rotp_nextalbum.init.InitStandEffects;
import com.nextalubm.rotp_nextalbum.network.AgingBlockClearPacket;
import com.nextalubm.rotp_nextalbum.network.NetworkHandler;
import com.nextalubm.rotp_nextalbum.stand.TheGratefulDeadAgingAuraEffect;
import com.nextalubm.rotp_nextalbum.util.AgingBlockTracker;
import com.nextalubm.rotp_nextalbum.util.AgingEntityUtil;
import com.nextalubm.rotp_nextalbum.util.AgingItemUtil;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.PacketDistributor;

public class TheGratefulDeadRemoveAgingAction extends StandAction {
    private static final int FADE_DURATION_TICKS = 60;

    public TheGratefulDeadRemoveAgingAction(StandAction.Builder builder) {
        super(builder);
    }

    @Override
    public TargetRequirement getTargetRequirement() {
        return TargetRequirement.NONE;
    }

    @Override
    protected void perform(World world, LivingEntity user, IStandPower power, ActionTarget target) {
        if (world.isClientSide() || !(user instanceof ServerPlayerEntity)) {
            return;
        }
        removeAgingAura(power);
        clearAgingForPlayer((ServerPlayerEntity) user);
    }

    public static void clearAgingForPlayer(ServerPlayerEntity player) {
        AgingBlockTracker.startFadeForOwnerInAllWorlds(player, FADE_DURATION_TICKS);
        AgingItemUtil.clearProgressForPlayer(player);
        AgingEntityUtil.clearProgressForOwnerInAllWorlds(player);
        NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new AgingBlockClearPacket());
    }

    public static boolean removeAgingAura(IStandPower power) {
        if (power == null) {
            return false;
        }
        Optional<TheGratefulDeadAgingAuraEffect> existing = power.getContinuousEffects()
                .getEffectOfType(InitStandEffects.THE_GRATEFUL_DEAD_AGING_AURA.get());
        if (!existing.isPresent()) {
            return false;
        }
        existing.get().remove();
        return true;
    }
}
