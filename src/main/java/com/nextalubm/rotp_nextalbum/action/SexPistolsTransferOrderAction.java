package com.nextalubm.rotp_nextalbum.action;

import javax.annotation.Nullable;

import com.github.standobyte.jojo.action.ActionConditionResult;
import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.action.stand.StandAction;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.util.general.LazySupplier;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.client.SexPistolsTransferOrderClientState;
import com.nextalubm.rotp_nextalbum.stand.SexPistolsEntities;
import com.nextalubm.rotp_nextalbum.stand.SexPistolsStandType;
import com.nextalubm.rotp_nextalbum.util.SexPistolsTransferOrder;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

public class SexPistolsTransferOrderAction extends StandAction {
    private final LazySupplier<ResourceLocation> texture = new LazySupplier<>(() -> new ResourceLocation(NextAlubm.MOD_ID, "textures/action/sex_pistols_target_sex_pistols.png"));

    public SexPistolsTransferOrderAction(Builder builder) {
        super(builder);
    }

    @Override
    public ResourceLocation getIconTexturePath(@Nullable IStandPower power) {
        return texture.get();
    }

    @Override
    public void onClick(World world, LivingEntity user, IStandPower power) {
        super.onClick(world, user, power);
        if (world.isClientSide) {
            SexPistolsTransferOrderClientState.cycle(user != null && user.isShiftKeyDown());
        }
    }

    @Override
    protected void perform(World world, LivingEntity user, IStandPower power, ActionTarget target) {
        if (world.isClientSide) {
            return;
        }
        SexPistolsEntities sexPistols = SexPistolsStandType.getSexPistolsEntities(power).orElse(null);
        if (sexPistols != null) {
            sexPistols.cycleTransferOrder(user != null && user.isShiftKeyDown());
        }
    }

    @Override
    public boolean greenSelection(IStandPower power, ActionConditionResult conditionCheck) {
        return conditionCheck.isPositive();
    }

    @Override
    public IFormattableTextComponent getTranslatedName(IStandPower power, String key) {
        TranslationTextComponent orderName = new TranslationTextComponent("action.rotp_nextalbum.sex_pistols_transfer_order." + getOrder(power).name().toLowerCase());
        return new TranslationTextComponent(key, orderName);
    }

    private SexPistolsTransferOrder getOrder(@Nullable IStandPower power) {
        if (power != null && power.getUser() != null && power.getUser().level.isClientSide) {
            return SexPistolsTransferOrderClientState.getOrder();
        }
        if (power != null) {
            SexPistolsEntities sexPistols = SexPistolsStandType.getSexPistolsEntities(power).orElse(null);
            if (sexPistols != null) {
                return sexPistols.getTransferOrder();
            }
        }
        return SexPistolsTransferOrder.NONE;
    }
}
