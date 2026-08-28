package com.nextalubm.rotp_nextalbum.potion;

import java.util.Collections;
import java.util.List;

import com.nextalubm.rotp_nextalbum.util.CigaretteFocusUtil;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.AttributeModifierManager;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectType;

public class FocusEffect extends Effect {
    public FocusEffect() {
        super(EffectType.BENEFICIAL, 0xE8C36A);
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, AttributeModifierManager attributes, int amplifier) {
        super.removeAttributeModifiers(entity, attributes, amplifier);
        CigaretteFocusUtil.onFocusEnded(entity, amplifier);
    }

    @Override
    public List<ItemStack> getCurativeItems() {
        return Collections.emptyList();
    }
}
