package com.nextalubm.rotp_nextalbum.stand;

import java.util.function.Consumer;

import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.power.impl.stand.stats.StandStats;
import com.github.standobyte.jojo.power.impl.stand.type.EntityStandType;
import com.nextalubm.rotp_nextalbum.entity.TheSunEntity;

import net.minecraft.entity.LivingEntity;

public class TheSunStandType extends EntityStandType<StandStats> {
    public TheSunStandType(AbstractBuilder<?, StandStats> builder) {
        super(builder);
    }

    @Override
    public boolean summon(LivingEntity user, IStandPower standPower, Consumer<StandEntity> beforeTheSummon, boolean withoutNameVoiceLine, boolean addToWorld) {
        return super.summon(user, standPower, standEntity -> {
            if (standEntity instanceof TheSunEntity) {
                TheSunEntity theSun = (TheSunEntity) standEntity;
                theSun.setInitialSunPosition(user);
            }
            beforeTheSummon.accept(standEntity);
        }, withoutNameVoiceLine, addToWorld);
    }
}
