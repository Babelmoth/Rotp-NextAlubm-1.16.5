package com.nextalubm.rotp_nextalbum.action;

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
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.world.World;

public class SexPistolsRecallAction extends StandAction {
    private final LazySupplier<ResourceLocation> iconTexture;

    public SexPistolsRecallAction(Builder builder) {
        super(builder);
        this.iconTexture = new LazySupplier<>(() -> new ResourceLocation(NextAlubm.MOD_ID, "textures/action/sex_pistols_stand_recall.png"));
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
        return user instanceof PlayerEntity ? ActionConditionResult.POSITIVE : ActionConditionResult.NEGATIVE;
    }

    @Override
    protected void perform(World world, LivingEntity user, IStandPower power, ActionTarget target) {
        if (world.isClientSide || !(user instanceof PlayerEntity)) {
            return;
        }
        int recalled = RevolverItem.recallSexPistolsAttachments((PlayerEntity) user);
        SexPistolsEntities sexPistols = SexPistolsStandType.getSexPistolsEntities(power).orElse(null);
        if (sexPistols != null) {
            recalled += sexPistols.recallRemoteControlPistols();
        }
        if (recalled > 0) {
            SexPistolsSoundUtil.sayRecall(user);
        }
        else {
            world.playSound(null, user.blockPosition(), SoundEvents.DISPENSER_FAIL, SoundCategory.PLAYERS, 0.45F, 1.0F);
        }
    }
}
