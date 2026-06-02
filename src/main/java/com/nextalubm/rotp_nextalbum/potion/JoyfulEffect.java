package com.nextalubm.rotp_nextalbum.potion;

import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectType;

public class JoyfulEffect extends Effect {
    public JoyfulEffect() {
        super(EffectType.BENEFICIAL, 0xF4B23A);
    }

    @Override
    public List<ItemStack> getCurativeItems() {
        return Collections.emptyList();
    }
}