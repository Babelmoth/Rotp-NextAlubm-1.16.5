package com.nextalubm.rotp_nextalbum.action;

import javax.annotation.Nullable;

import com.github.standobyte.jojo.action.ActionConditionResult;
import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.action.stand.StandAction;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.util.general.LazySupplier;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.network.NetworkHandler;
import com.nextalubm.rotp_nextalbum.network.SexPistolsTargetModePacket;
import com.nextalubm.rotp_nextalbum.client.SexPistolsTargetModeClientState;
import com.nextalubm.rotp_nextalbum.stand.SexPistolsEntities;
import com.nextalubm.rotp_nextalbum.stand.SexPistolsStandType;
import com.nextalubm.rotp_nextalbum.util.SexPistolsTargetMode;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.PacketDistributor;

public class SexPistolsTargetModeAction extends StandAction {
    private final LazySupplier<ResourceLocation> playersTexture = new LazySupplier<>(() -> new ResourceLocation(NextAlubm.MOD_ID, "textures/action/sex_pistols_target_players.png"));
    private final LazySupplier<ResourceLocation> hostileTexture = new LazySupplier<>(() -> new ResourceLocation(NextAlubm.MOD_ID, "textures/action/sex_pistols_target_hostile.png"));
    private final LazySupplier<ResourceLocation> friendlyTexture = new LazySupplier<>(() -> new ResourceLocation(NextAlubm.MOD_ID, "textures/action/sex_pistols_target_friendly.png"));
    private final LazySupplier<ResourceLocation> allTexture = new LazySupplier<>(() -> new ResourceLocation(NextAlubm.MOD_ID, "textures/action/sex_pistols_target_all.png"));

    public SexPistolsTargetModeAction(Builder builder) {
        super(builder);
    }

    @Override
    public ResourceLocation getIconTexturePath(@Nullable IStandPower power) {
        switch (getMode(power)) {
        case HOSTILE:
            return hostileTexture.get();
        case FRIENDLY:
            return friendlyTexture.get();
        case ALL:
            return allTexture.get();
        case PLAYERS:
        default:
            return playersTexture.get();
        }
    }

    @Override
    public void onClick(World world, LivingEntity user, IStandPower power) {
        super.onClick(world, user, power);
        if (world.isClientSide) {
            SexPistolsTargetModeClientState.cycle(user != null && user.isShiftKeyDown());
        }
    }

    @Override
    protected void perform(World world, LivingEntity user, IStandPower power, ActionTarget target) {
        if (world.isClientSide) {
            return;
        }
        SexPistolsEntities sexPistols = SexPistolsStandType.getSexPistolsEntities(power).orElse(null);
        if (sexPistols != null) {
            sexPistols.cycleTargetMode(user != null && user.isShiftKeyDown());
            if (user instanceof ServerPlayerEntity) {
                NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) user), new SexPistolsTargetModePacket(sexPistols.getTargetMode()));
            }
        }
    }

    @Override
    public boolean greenSelection(IStandPower power, ActionConditionResult conditionCheck) {
        return conditionCheck.isPositive();
    }

    @Override
    public IFormattableTextComponent getTranslatedName(IStandPower power, String key) {
        TranslationTextComponent modeName = new TranslationTextComponent("action.rotp_nextalbum.sex_pistols_target_mode." + getMode(power).name().toLowerCase());
        return new TranslationTextComponent(key, modeName);
    }

    private SexPistolsTargetMode getMode(@Nullable IStandPower power) {
        if (power != null && power.getUser() != null && power.getUser().level.isClientSide) {
            return SexPistolsTargetModeClientState.getMode();
        }
        if (power != null) {
            SexPistolsEntities sexPistols = SexPistolsStandType.getSexPistolsEntities(power).orElse(null);
            if (sexPistols != null) {
                return sexPistols.getTargetMode();
            }
        }
        return SexPistolsTargetMode.ALL;
    }
}