package com.nextalubm.rotp_nextalbum.util;

import java.util.UUID;

import javax.annotation.Nullable;

import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.action.ActionTarget.TargetType;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

public final class SexPistolsForcedTarget {
    public enum Kind {
        ENTITY,
        BLOCK
    }

    private final Kind kind;
    private final UUID entityId;
    private final int entityNetworkId;
    private final BlockPos blockPos;

    private SexPistolsForcedTarget(Kind kind, UUID entityId, int entityNetworkId, BlockPos blockPos) {
        this.kind = kind;
        this.entityId = entityId;
        this.entityNetworkId = entityNetworkId;
        this.blockPos = blockPos;
    }

    public static SexPistolsForcedTarget entity(LivingEntity entity) {
        return new SexPistolsForcedTarget(Kind.ENTITY, entity.getUUID(), entity.getId(), null);
    }

    public static SexPistolsForcedTarget entity(UUID entityId, int entityNetworkId) {
        return new SexPistolsForcedTarget(Kind.ENTITY, entityId, entityNetworkId, null);
    }

    public static SexPistolsForcedTarget block(BlockPos pos) {
        return new SexPistolsForcedTarget(Kind.BLOCK, null, -1, pos.immutable());
    }

    public Kind getKind() {
        return kind;
    }

    @Nullable
    public UUID getEntityId() {
        return entityId;
    }

    public int getEntityNetworkId() {
        return entityNetworkId;
    }

    @Nullable
    public BlockPos getBlockPos() {
        return blockPos;
    }

    public boolean matches(ActionTarget target) {
        if (target == null) {
            return false;
        }
        if (kind == Kind.ENTITY && target.getType() == TargetType.ENTITY) {
            Entity entity = target.getEntity();
            return entity != null && entity.getUUID().equals(entityId);
        }
        if (kind == Kind.BLOCK && target.getType() == TargetType.BLOCK) {
            return target.getBlockPos() != null && target.getBlockPos().equals(blockPos);
        }
        return false;
    }

    public boolean isValid(World world) {
        if (kind == Kind.ENTITY) {
            LivingEntity entity = getLiving(world);
            return entity != null && entity.isAlive();
        }
        return blockPos != null && world != null && !world.getBlockState(blockPos).isAir();
    }

    @Nullable
    public LivingEntity getLiving(World world) {
        if (kind != Kind.ENTITY || world == null) {
            return null;
        }
        Entity entity = null;
        if (world instanceof ServerWorld && entityId != null) {
            entity = ((ServerWorld) world).getEntity(entityId);
        }
        else if (entityNetworkId >= 0) {
            entity = world.getEntity(entityNetworkId);
        }
        return entity instanceof LivingEntity ? (LivingEntity) entity : null;
    }

    public Vector3d getAimPoint(World world) {
        if (kind == Kind.ENTITY) {
            LivingEntity entity = getLiving(world);
            return entity != null ? entity.getBoundingBox().getCenter() : Vector3d.ZERO;
        }
        return Vector3d.atCenterOf(blockPos);
    }

    public CompoundNBT save() {
        CompoundNBT nbt = new CompoundNBT();
        nbt.putString("Kind", kind.name());
        if (entityId != null) {
            nbt.putUUID("Entity", entityId);
        }
        nbt.putInt("EntityNet", entityNetworkId);
        if (blockPos != null) {
            nbt.putInt("X", blockPos.getX());
            nbt.putInt("Y", blockPos.getY());
            nbt.putInt("Z", blockPos.getZ());
        }
        return nbt;
    }

    public static SexPistolsForcedTarget load(CompoundNBT nbt) {
        Kind kind = Kind.BLOCK;
        try {
            kind = Kind.valueOf(nbt.getString("Kind"));
        }
        catch (IllegalArgumentException ignored) {
        }
        if (kind == Kind.ENTITY) {
            UUID id = nbt.hasUUID("Entity") ? nbt.getUUID("Entity") : null;
            return new SexPistolsForcedTarget(Kind.ENTITY, id, nbt.getInt("EntityNet"), null);
        }
        return block(new BlockPos(nbt.getInt("X"), nbt.getInt("Y"), nbt.getInt("Z")));
    }
}
