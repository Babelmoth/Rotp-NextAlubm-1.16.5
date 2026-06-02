package com.nextalubm.rotp_nextalbum.util;

import com.github.standobyte.jojo.entity.stand.StandEntity;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.player.PlayerEntity;

public enum SexPistolsTargetMode {
    PLAYERS,
    HOSTILE,
    FRIENDLY,
    ALL;

    public static SexPistolsTargetMode byId(int id) {
        SexPistolsTargetMode[] values = values();
        return values[Math.floorMod(id, values.length)];
    }

    public SexPistolsTargetMode cycle(boolean backwards) {
        return byId(ordinal() + (backwards ? -1 : 1));
    }

    public boolean matches(LivingEntity owner, LivingEntity entity) {
        if (entity == null || !entity.isAlive() || entity == owner || entity instanceof StandEntity) {
            return false;
        }
        switch (this) {
        case PLAYERS:
            return entity instanceof PlayerEntity && !entity.isAlliedTo(owner) && !((PlayerEntity) entity).isSpectator();
        case HOSTILE:
            return entity instanceof MonsterEntity;
        case FRIENDLY:
            return !(entity instanceof PlayerEntity) && !(entity instanceof MonsterEntity);
        case ALL:
        default:
            return !(entity instanceof PlayerEntity && ((PlayerEntity) entity).isSpectator());
        }
    }
}