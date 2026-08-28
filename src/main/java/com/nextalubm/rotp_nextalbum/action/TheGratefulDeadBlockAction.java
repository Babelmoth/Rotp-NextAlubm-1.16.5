package com.nextalubm.rotp_nextalbum.action;

import com.github.standobyte.jojo.action.ActionConditionResult;
import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.action.stand.StandEntityBlock;
import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.util.general.LazySupplier;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.util.TheGratefulDeadGrabUtil;

import net.minecraft.util.ResourceLocation;

public class TheGratefulDeadBlockAction extends StandEntityBlock {

    private final LazySupplier<ResourceLocation> iconTexture = new LazySupplier<>(
            () -> new ResourceLocation(NextAlubm.MOD_ID, "textures/action/the_grateful_dead_block.png"));

    public TheGratefulDeadBlockAction() {
        super();
    }

    @Override
    public ResourceLocation getIconTexturePath(IStandPower power) {
        return iconTexture.get();
    }

    @Override
    protected ActionConditionResult checkStandConditions(StandEntity stand, IStandPower power, ActionTarget target) {
        if (TheGratefulDeadGrabUtil.isGrabbing(power)) {
            return conditionMessage("grateful_dead_holding");
        }
        return super.checkStandConditions(stand, power, target);
    }
}
