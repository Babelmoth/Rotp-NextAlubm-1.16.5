package com.nextalubm.rotp_nextalbum.potion;

import java.util.Collections;
import java.util.List;

import com.nextalubm.rotp_nextalbum.util.CigaretteFocusUtil;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.AttributeModifierManager;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectType;

public class LaxEffect extends Effect {
    public LaxEffect() {
        super(EffectType.HARMFUL, 0x6B7A8A);
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, AttributeModifierManager attributes, int amplifier) {
        super.removeAttributeModifiers(entity, attributes, amplifier);
        CigaretteFocusUtil.applyStandModifiers(entity);
    }

    @Override
    public List<ItemStack> getCurativeItems() {
        return Collections.emptyList();
    }
}
