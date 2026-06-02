package com.nextalubm.rotp_nextalbum.util;

import java.util.UUID;

public interface AgedLivingEntityAccess {
    float rotpNextAlbum$getAgingProgress();

    void rotpNextAlbum$setAgingProgress(float progress);

    UUID rotpNextAlbum$getAgingOwner();

    void rotpNextAlbum$setAgingOwner(UUID ownerUuid);
}
