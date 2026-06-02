package com.nextalubm.rotp_nextalbum.entity;

import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.entity.stand.StandEntityType;
import com.github.standobyte.jojo.entity.stand.StandPose;

import net.minecraft.world.World;

public class SilverChariotRequiemEntity extends StandEntity {
    public SilverChariotRequiemEntity(StandEntityType<? extends SilverChariotRequiemEntity> type, World world) {
        super(type, world);
        setStandPose(StandPose.IDLE);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }
}
