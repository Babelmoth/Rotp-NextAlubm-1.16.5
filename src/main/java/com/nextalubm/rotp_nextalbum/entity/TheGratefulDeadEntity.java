package com.nextalubm.rotp_nextalbum.entity;

import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.entity.stand.StandEntityType;
import com.github.standobyte.jojo.entity.stand.StandPose;

import net.minecraft.world.World;

public class TheGratefulDeadEntity extends StandEntity {
    public TheGratefulDeadEntity(StandEntityType<? extends TheGratefulDeadEntity> type, World world) {
        super(type, world);
        setStandPose(StandPose.IDLE);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }
}
