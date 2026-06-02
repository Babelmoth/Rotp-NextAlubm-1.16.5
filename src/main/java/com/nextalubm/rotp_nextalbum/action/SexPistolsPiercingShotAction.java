package com.nextalubm.rotp_nextalbum.action;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.github.standobyte.jojo.action.ActionConditionResult;
import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.action.stand.StandAction;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.util.general.LazySupplier;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.init.InitSounds;
import com.nextalubm.rotp_nextalbum.item.RevolverItem;
import com.nextalubm.rotp_nextalbum.stand.SexPistolsEntities;
import com.nextalubm.rotp_nextalbum.stand.SexPistolsStandType;
import com.nextalubm.rotp_nextalbum.util.SexPistolsSoundUtil;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.world.World;

public class SexPistolsPiercingShotAction extends StandAction {
    private final LazySupplier<ResourceLocation> iconTexture;

    public SexPistolsPiercingShotAction(Builder builder) {
        super(builder);
        this.iconTexture = new LazySupplier<>(() -> new ResourceLocation(NextAlubm.MOD_ID, "textures/action/sex_pistols_piercing_shot.png"));
    }

    @Override
    public ResourceLocation getIconTexturePath(@Nullable IStandPower power) {
        return iconTexture.get();
    }

    @Override
    public ActionConditionResult checkConditions(LivingEntity user, IStandPower power, ActionTarget target) {
        ActionConditionResult result = super.checkConditions(user, power, target);
        if (!result.isPositive()) {
            return result;
        }
        return user.getMainHandItem().getItem() instanceof RevolverItem ? ActionConditionResult.POSITIVE : ActionConditionResult.NEGATIVE;
    }

    @Override
    protected void perform(World world, LivingEntity user, IStandPower power, ActionTarget target) {
        if (world.isClientSide || !(user instanceof ServerPlayerEntity)) {
            return;
        }
        ItemStack stack = user.getMainHandItem();
        if (!(stack.getItem() instanceof RevolverItem)) {
            return;
        }
        SexPistolsEntities sexPistols = SexPistolsStandType.getSexPistolsEntities(power).orElse(null);
        if (sexPistols == null) {
            world.playSound(null, user.blockPosition(), SoundEvents.DISPENSER_FAIL, SoundCategory.PLAYERS, 0.45F, 1.0F);
            return;
        }
        RevolverItem revolver = (RevolverItem) stack.getItem();
        RevolverItem.normalizeChambers(stack);
        if (!RevolverItem.canReload(user, stack)) {
            world.playSound(null, user.blockPosition(), SoundEvents.DISPENSER_FAIL, SoundCategory.PLAYERS, 0.45F, 1.0F);
            return;
        }
        if (!hasPotentialPistols(sexPistols, stack)) {
            world.playSound(null, user.blockPosition(), SoundEvents.DISPENSER_FAIL, SoundCategory.PLAYERS, 0.45F, 1.0F);
            return;
        }
        if (!RevolverItem.isReloadMode(stack)) {
            RevolverItem.setReloadMode(stack, true);
            RevolverItem.setAiming(stack, false);
            if (user.getUseItem() == stack) {
                user.stopUsingItem();
            }
            world.playSound(null, user.blockPosition(), InitSounds.REVOLVER_OPEN.get(), SoundCategory.PLAYERS, 0.85F, 1.0F);
        }
        int primaryChamber = RevolverItem.getSelectedChamber(stack);
        if (!RevolverItem.hasBullet(stack, primaryChamber) && !revolver.loadAmmoIntoSelectedChamber((ServerPlayerEntity) user, stack)) {
            world.playSound(null, user.blockPosition(), SoundEvents.DISPENSER_FAIL, SoundCategory.PLAYERS, 0.45F, 1.0F);
            return;
        }
        List<Integer> pistolsToAttach = getPistolsToAttach(sexPistols, stack);
        if (pistolsToAttach.isEmpty()) {
            world.playSound(null, user.blockPosition(), SoundEvents.DISPENSER_FAIL, SoundCategory.PLAYERS, 0.45F, 1.0F);
            return;
        }
        RevolverItem.clearAllPistolChambers(stack);
        for (int chamber = 0; chamber < 6; chamber++) {
            RevolverItem.setEncirclementChamber(stack, chamber, false);
            RevolverItem.setPiercingChamber(stack, chamber, chamber == primaryChamber);
        }
        for (int pistolIndex : pistolsToAttach) {
            RevolverItem.addPistolToChamber(stack, primaryChamber, pistolIndex);
        }
        RevolverItem.applyReloadCooldown(user, stack);
        world.playSound(null, user.blockPosition(), InitSounds.REVOLVER_SPEEDLOADER.get(), SoundCategory.PLAYERS, 0.65F, 1.15F);
        SexPistolsSoundUtil.sayStandReload(user);
    }

    private boolean hasPotentialPistols(SexPistolsEntities sexPistols, ItemStack stack) {
        if (sexPistols == null) {
            return false;
        }
        int[] pistolChambers = RevolverItem.getPistolChambers(stack);
        for (int chamberMask : pistolChambers) {
            if (chamberMask != 0) {
                return true;
            }
        }
        return !sexPistols.getAvailablePistolIndices().isEmpty();
    }

    private List<Integer> getPistolsToAttach(SexPistolsEntities sexPistols, ItemStack stack) {
        boolean[] selected = new boolean[6];
        List<Integer> pistols = new ArrayList<>();
        int[] pistolChambers = RevolverItem.getPistolChambers(stack);
        for (int chamberMask : pistolChambers) {
            for (int pistolIndex = 0; pistolIndex < 6; pistolIndex++) {
                if ((chamberMask & (1 << pistolIndex)) != 0 && !selected[pistolIndex]) {
                    selected[pistolIndex] = true;
                    pistols.add(pistolIndex);
                }
            }
        }
        for (int pistolIndex : sexPistols.getAvailablePistolIndices()) {
            if (!selected[pistolIndex] && sexPistols.markPistolLoaded(pistolIndex)) {
                selected[pistolIndex] = true;
                pistols.add(pistolIndex);
            }
        }
        return pistols;
    }
}
