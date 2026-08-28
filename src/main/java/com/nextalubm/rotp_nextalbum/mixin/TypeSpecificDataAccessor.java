package com.nextalubm.rotp_nextalbum.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.github.standobyte.jojo.power.impl.nonstand.INonStandPower;
import com.github.standobyte.jojo.power.impl.nonstand.TypeSpecificData;

@Mixin(TypeSpecificData.class)
public interface TypeSpecificDataAccessor {
    @Accessor(value = "power", remap = false)
    INonStandPower rotpNextAlbum$getPower();
}
