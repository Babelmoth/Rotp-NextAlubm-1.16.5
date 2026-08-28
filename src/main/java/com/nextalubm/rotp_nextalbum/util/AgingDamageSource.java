package com.nextalubm.rotp_nextalbum.util;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

public class AgingDamageSource extends DamageSource {
    public static final AgingDamageSource INSTANCE = new AgingDamageSource();

    private AgingDamageSource() {
        super("rotp_nextalbum.aging");
        bypassArmor();
        setMagic();
    }

    @Override
    public ITextComponent getLocalizedDeathMessage(LivingEntity entity) {
        return new TranslationTextComponent("death.attack.rotp_nextalbum.aging", entity.getDisplayName());
    }
}
