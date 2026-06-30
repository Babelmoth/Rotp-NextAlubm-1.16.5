package com.nextalubm.rotp_nextalbum.action;

import com.github.standobyte.jojo.action.stand.StandEntityMeleeBarrage;
import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class TheGratefulDeadBarrageAction extends StandEntityMeleeBarrage {

    public TheGratefulDeadBarrageAction(StandEntityMeleeBarrage.Builder builder) {
        super(builder);
    }
    
    @Override
    public void afterClick(World world, LivingEntity user, IStandPower power, boolean passedRequirements) {
        super.afterClick(world, user, power, passedRequirements);
        if (power.isActive()) {
            StandEntity standEntity = (StandEntity) power.getStandManifestation();
            standEntity.swingingArm = Hand.MAIN_HAND;
        }
    }

}