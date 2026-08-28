package com.nextalubm.rotp_nextalbum.client.render.model;

import com.github.standobyte.jojo.client.render.entity.model.stand.HumanoidStandModel;
import com.github.standobyte.jojo.client.render.entity.pose.ModelPose;
import com.github.standobyte.jojo.client.render.entity.pose.RotationAngle;
import com.nextalubm.rotp_nextalbum.entity.BeachBoyStandEntity;

public class BeachBoyStandModel extends HumanoidStandModel<BeachBoyStandEntity> {
    public BeachBoyStandModel() {
        super();
    }

    @Override
    protected ModelPose<BeachBoyStandEntity> initIdlePose() {
        return new ModelPose<>(new RotationAngle[] {});
    }

    @Override
    protected RotationAngle[][] initSummonPoseRotations() {
        return new RotationAngle[][] {
            new RotationAngle[] {},
            new RotationAngle[] {}
        };
    }

    @Override
    protected void initActionPoses() {
    }
}
