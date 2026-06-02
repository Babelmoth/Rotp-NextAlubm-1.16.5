package com.nextalubm.rotp_nextalbum.mixin;

import java.util.Optional;
import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.nextalubm.rotp_nextalbum.util.AgedLivingEntityAccess;
import com.nextalubm.rotp_nextalbum.util.AgingEntityUtil;

import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;

@Mixin(LivingEntity.class)
public abstract class LivingEntityAgingMixin implements AgedLivingEntityAccess {
    @Unique
    private static final DataParameter<Float> ROTP_NEXT_ALBUM_AGING_PROGRESS =
            EntityDataManager.defineId(LivingEntity.class, DataSerializers.FLOAT);
    @Unique
    private static final DataParameter<Optional<UUID>> ROTP_NEXT_ALBUM_AGING_OWNER =
            EntityDataManager.defineId(LivingEntity.class, DataSerializers.OPTIONAL_UUID);

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void rotpNextAlbum$defineAgingData(CallbackInfo ci) {
        ((LivingEntity) (Object) this).getEntityData().define(ROTP_NEXT_ALBUM_AGING_PROGRESS, 0F);
        ((LivingEntity) (Object) this).getEntityData().define(ROTP_NEXT_ALBUM_AGING_OWNER, Optional.empty());
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void rotpNextAlbum$saveAgingData(CompoundNBT nbt, CallbackInfo ci) {
        float progress = rotpNextAlbum$getAgingProgress();
        if (progress > 0F) {
            nbt.putFloat(AgingEntityUtil.NBT_AGING_PROGRESS, progress);
            UUID owner = rotpNextAlbum$getAgingOwner();
            if (owner != null) {
                nbt.putUUID(AgingEntityUtil.NBT_AGING_OWNER, owner);
            }
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void rotpNextAlbum$readAgingData(CompoundNBT nbt, CallbackInfo ci) {
        if (nbt.contains(AgingEntityUtil.NBT_AGING_PROGRESS)) {
            rotpNextAlbum$setAgingProgress(nbt.getFloat(AgingEntityUtil.NBT_AGING_PROGRESS));
            rotpNextAlbum$setAgingOwner(nbt.hasUUID(AgingEntityUtil.NBT_AGING_OWNER)
                    ? nbt.getUUID(AgingEntityUtil.NBT_AGING_OWNER)
                    : null);
        }
    }

    @Override
    public float rotpNextAlbum$getAgingProgress() {
        return ((LivingEntity) (Object) this).getEntityData().get(ROTP_NEXT_ALBUM_AGING_PROGRESS);
    }

    @Override
    public void rotpNextAlbum$setAgingProgress(float progress) {
        ((LivingEntity) (Object) this).getEntityData().set(ROTP_NEXT_ALBUM_AGING_PROGRESS,
                Math.max(0F, Math.min(AgingEntityUtil.MAX_PROGRESS, progress)));
    }

    @Override
    public UUID rotpNextAlbum$getAgingOwner() {
        return ((LivingEntity) (Object) this).getEntityData().get(ROTP_NEXT_ALBUM_AGING_OWNER).orElse(null);
    }

    @Override
    public void rotpNextAlbum$setAgingOwner(UUID ownerUuid) {
        ((LivingEntity) (Object) this).getEntityData().set(ROTP_NEXT_ALBUM_AGING_OWNER, Optional.ofNullable(ownerUuid));
    }
}
