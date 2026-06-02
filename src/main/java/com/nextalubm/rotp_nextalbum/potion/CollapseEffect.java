package com.nextalubm.rotp_nextalbum.potion;

import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectType;

public class CollapseEffect extends Effect {
    public CollapseEffect() {
        super(EffectType.HARMFUL, 0x6F6F7A);
    }

    @Override
    public List<ItemStack> getCurativeItems() {
        return Collections.emptyList();
    }
}
