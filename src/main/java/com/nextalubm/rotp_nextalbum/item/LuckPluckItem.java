package com.nextalubm.rotp_nextalbum.item;

import com.github.standobyte.jojo.init.ModItems;
import com.nextalubm.rotp_nextalbum.client.render.LuckPluckGeoRenderer;

import net.minecraft.item.ItemTier;
import net.minecraft.item.SwordItem;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import com.github.standobyte.jojo.util.mc.MCUtil;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;
import software.bernie.geckolib3.util.GeckoLibUtil;

public class LuckPluckItem extends SwordItem implements IAnimatable {
    private final AnimationFactory factory = GeckoLibUtil.createFactory(this);
    protected int enchantability = 22;

    public LuckPluckItem() {
        super(ItemTier.DIAMOND, 3, -2.4F, new Properties().tab(ModItems.MAIN_TAB).setISTER(() -> LuckPluckGeoRenderer::new));
    }

    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    @Override
    public AnimationFactory getFactory() {
        return factory;
    }

    public boolean openFingers() {
        return true;
    }

    @Override
    public int getEnchantmentValue() {
        return enchantability;
    }

    private <E extends IAnimatable> PlayState predicate(AnimationEvent<E> event) {
        event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.luck_pluck.idle", true));
        return PlayState.CONTINUE;
    }
}