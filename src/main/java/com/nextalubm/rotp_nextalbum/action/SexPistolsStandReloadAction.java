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
import com.nextalubm.rotp_nextalbum.util.SexPistolsItemAttachmentUtil;
import com.nextalubm.rotp_nextalbum.util.SexPistolsSoundUtil;
import com.nextalubm.rotp_nextalbum.util.SexPistolsStaminaUtil;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;

public class SexPistolsStandReloadAction extends StandAction {
    private final boolean quickReload;
    private final LazySupplier<ResourceLocation> iconTexture;

    public SexPistolsStandReloadAction(Builder builder, boolean quickReload) {
        super(builder);
        this.quickReload = quickReload;
        this.iconTexture = new LazySupplier<>(() -> new ResourceLocation(NextAlubm.MOD_ID, quickReload ? "textures/action/sex_pistols_quick_reload.png" : "textures/action/sex_pistols_stand_reload.png"));
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
        SexPistolsEntities sexPistols = SexPistolsStandType.getSexPistolsEntities(power).orElse(null);
        RevolverItem revolver = (RevolverItem) stack.getItem();
        RevolverItem.normalizeChambers(stack);
        if (!RevolverItem.canReload(user, stack)) {
            world.playSound(null, user.blockPosition(), net.minecraft.util.SoundEvents.DISPENSER_FAIL, SoundCategory.PLAYERS, 0.35F, quickReload ? 1.2F : 1.0F);
            return;
        }
        if (!RevolverItem.isReloadMode(stack)) {
            RevolverItem.setReloadMode(stack, true);
            revolver.ejectSpentCasings((ServerPlayerEntity) user, stack);
            RevolverItem.setAiming(stack, false);
            if (user.getUseItem() == stack) {
                user.stopUsingItem();
            }
            world.playSound(null, user.blockPosition(), InitSounds.REVOLVER_OPEN.get(), SoundCategory.PLAYERS, 0.85F, 1.0F);
        }
        boolean loaded = quickReload ? quickReload((ServerPlayerEntity) user, stack, revolver, sexPistols) : reloadSelected(stack, sexPistols, getAvailablePistols(sexPistols, stack));
        if (loaded) {
            RevolverItem.applyReloadCooldown(user, stack);
            world.playSound(null, user.blockPosition(), InitSounds.REVOLVER_SPEEDLOADER.get(), SoundCategory.PLAYERS, 0.65F, quickReload ? 1.35F : 1.15F);
            SexPistolsSoundUtil.sayStandReload(user);
        }
        else {
            world.playSound(null, user.blockPosition(), net.minecraft.util.SoundEvents.DISPENSER_FAIL, SoundCategory.PLAYERS, 0.45F, quickReload ? 1.2F : 1.0F);
        }
    }


    private List<Integer> getAvailablePistols(SexPistolsEntities sexPistols, ItemStack stack) {
        List<Integer> pistols = sexPistols != null ? sexPistols.getAvailablePistolIndices() : new ArrayList<>();
        pistols.removeIf(index -> RevolverItem.isPistolLoadedInAnyChamber(stack, index));
        return pistols;
    }

    private boolean reloadSelected(ItemStack stack, SexPistolsEntities sexPistols, List<Integer> availablePistols) {
        if (sexPistols == null || availablePistols.isEmpty()) {
            return false;
        }
        int chamber = RevolverItem.getSelectedChamber(stack);
        if (!RevolverItem.hasBullet(stack, chamber)) {
            return false;
        }
        int pistolIndex = availablePistols.get(0);
        if (!sexPistols.markPistolLoaded(pistolIndex)) {
            return false;
        }
        RevolverItem.addPistolToChamber(stack, chamber, pistolIndex);
        return true;
    }

    private boolean quickReload(ServerPlayerEntity player, ItemStack stack, RevolverItem revolver, SexPistolsEntities sexPistols) {
        int affordableBullets = SexPistolsStaminaUtil.getAffordableQuickReloadBullets(player);
        int loadedBullets = affordableBullets > 0 ? revolver.loadAvailableAmmoIntoEmptyChambers(player, stack, affordableBullets) : 0;
        if (loadedBullets > 0) {
            SexPistolsStaminaUtil.consumeQuickReloadStamina(player, loadedBullets);
        }
        List<Integer> loadedChambers = getLoadedChambers(stack);
        boolean loadedPistols = quickReloadPistols(stack, sexPistols, getAvailablePistols(sexPistols, stack), loadedChambers);
        return loadedBullets > 0 || loadedPistols;
    }

    private boolean quickReloadPistols(ItemStack stack, SexPistolsEntities sexPistols, List<Integer> availablePistols, List<Integer> loadedChambers) {
        if (sexPistols == null || availablePistols.isEmpty() || loadedChambers.isEmpty()) {
            return false;
        }
        boolean loadedAny = false;
        for (int pistolIndex : availablePistols) {
            int chamber = getLeastLoadedPistolChamber(stack, loadedChambers);
            if (!sexPistols.markPistolLoaded(pistolIndex)) {
                continue;
            }
            RevolverItem.addPistolToChamber(stack, chamber, pistolIndex);
            loadedAny = true;
        }
        return loadedAny;
    }

    private int getLeastLoadedPistolChamber(ItemStack stack, List<Integer> loadedChambers) {
        int bestChamber = loadedChambers.get(0);
        int bestCount = Integer.bitCount(RevolverItem.getPistolMask(stack, bestChamber));
        for (int i = 1; i < loadedChambers.size(); i++) {
            int chamber = loadedChambers.get(i);
            int count = Integer.bitCount(RevolverItem.getPistolMask(stack, chamber));
            if (count < bestCount) {
                bestChamber = chamber;
                bestCount = count;
            }
        }
        return bestChamber;
    }

    private List<Integer> getLoadedChambers(ItemStack stack) {
        List<Integer> chambers = new ArrayList<>();
        int selected = RevolverItem.getSelectedChamber(stack);
        for (int i = 0; i < 6; i++) {
            int chamber = Math.floorMod(selected + i, 6);
            if (RevolverItem.hasBullet(stack, chamber)) {
                chambers.add(chamber);
            }
        }
        return chambers;
    }
}