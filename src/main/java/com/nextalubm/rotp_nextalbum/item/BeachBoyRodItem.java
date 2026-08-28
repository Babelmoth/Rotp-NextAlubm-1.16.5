package com.nextalubm.rotp_nextalbum.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.nextalubm.rotp_nextalbum.client.render.BeachBoyGeoRenderer;
import com.nextalubm.rotp_nextalbum.entity.BeachBoyHookEntity;
import com.nextalubm.rotp_nextalbum.entity.BeachBoyStandEntity;
import com.nextalubm.rotp_nextalbum.util.BeachBoyCombatUtil;
import com.nextalubm.rotp_nextalbum.util.BeachBoyLine;
import com.nextalubm.rotp_nextalbum.util.BeachBoyStandUtil;
import com.nextalubm.rotp_nextalbum.util.StandSummonUtil;
import com.nextalubm.rotp_nextalbum.util.TheGratefulDeadGrabUtil;

import com.github.standobyte.jojo.power.impl.stand.IStandPower;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentType;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.UseAction;
import net.minecraft.stats.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;
import software.bernie.geckolib3.util.GeckoLibUtil;

public class BeachBoyRodItem extends Item implements IAnimatable {
    public static final String CAST_KEY = "Cast";
    public static final String REEL_KEY = "Reel";
    public static final String STAND_DAMAGE_KEY = "StandScaledDamage";
    private static final int FULL_CHARGE_TICKS = 20;
    private static final double DIAMOND_AXE_ATTACK_SPEED = -3.0D;
    private final AnimationFactory factory = GeckoLibUtil.createFactory(this);
    private final Multimap<Attribute, AttributeModifier> defaultModifiers;

    public BeachBoyRodItem(Properties properties) {
        super(properties.setISTER(() -> BeachBoyGeoRenderer::new));
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", 7.0D, AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier", DIAMOND_AXE_ATTACK_SPEED, AttributeModifier.Operation.ADDITION));
        this.defaultModifiers = builder.build();
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlotType slot, ItemStack stack) {
        if (slot != EquipmentSlotType.MAINHAND) {
            return super.getAttributeModifiers(slot, stack);
        }
        if (stack.hasTag() && stack.getTag().contains(STAND_DAMAGE_KEY)) {
            double scaledDamage = stack.getTag().getDouble(STAND_DAMAGE_KEY);
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
            builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", scaledDamage - 1.0D, AttributeModifier.Operation.ADDITION));
            builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier", DIAMOND_AXE_ATTACK_SPEED, AttributeModifier.Operation.ADDITION));
            return builder.build();
        }
        return this.defaultModifiers;
    }

    public static void syncStandScaledDamage(PlayerEntity player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) {
            return;
        }
        IStandPower power = IStandPower.getStandPowerOptional(player).orElse(null);
        double damage = whipDamage(power);
        stack.getOrCreateTag().putDouble(STAND_DAMAGE_KEY, damage);
    }

    private static double whipDamage(@javax.annotation.Nullable IStandPower power) {
        BeachBoyStandEntity stand = BeachBoyStandUtil.getStand(power);
        if (stand != null) {
            return Math.max(4.0D, stand.getAttackDamage());
        }
        return Math.max(4.0D, TheGratefulDeadGrabUtil.getStandPowerStat(power));
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        BeachBoyCombatUtil.tryApplyStrikeEffects(attacker, target);
        return true;
    }

    public static boolean isCast(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.hasTag() && stack.getTag().getBoolean(CAST_KEY);
    }

    public static void setCast(ItemStack stack, boolean cast) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        stack.getOrCreateTag().putBoolean(CAST_KEY, cast);
    }

    public static float getReel(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTag() || !stack.getTag().contains(REEL_KEY)) {
            return BeachBoyLine.DEFAULT_LENGTH;
        }
        return stack.getTag().getFloat(REEL_KEY);
    }

    public static void setReel(ItemStack stack, float length) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        stack.getOrCreateTag().putFloat(REEL_KEY, length);
    }

    public static float getChargeStrength(int usedTicks) {
        float f = (float) usedTicks / (float) FULL_CHARGE_TICKS;
        f = (f * f + f * 2.0F) / 3.0F;
        return MathHelper.clamp(f, 0.0F, 1.0F);
    }

    @Override
    public ActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getItemInHand(hand);
        IStandPower power = IStandPower.getStandPowerOptional(player).orElse(null);
        if (!BeachBoyStandUtil.isBeachBoyUser(player)) {
            return ActionResult.fail(stack);
        }
        StandSummonUtil.ensureSummoned(player, power);
        if (!world.isClientSide && !BeachBoyStandUtil.isBeachBoySummoned(player)) {
            return ActionResult.fail(stack);
        }
        BeachBoyHookEntity existing = BeachBoyStandUtil.findHook(player);
        if (existing != null) {
            if (!world.isClientSide) {
                existing.reelIn();
            }
            setCast(stack, false);
            player.awardStat(Stats.ITEM_USED.get(this));
            return ActionResult.sidedSuccess(stack, world.isClientSide());
        }
        player.startUsingItem(hand);
        return ActionResult.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, World world, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof PlayerEntity)) {
            return;
        }
        PlayerEntity player = (PlayerEntity) entity;
        StandSummonUtil.ensureSummoned(player, IStandPower.getStandPowerOptional(player).orElse(null));
        if (!BeachBoyStandUtil.isBeachBoySummoned(player) || BeachBoyStandUtil.findHook(player) != null) {
            return;
        }
        int used = getUseDuration(stack) - timeLeft;
        float charge = getChargeStrength(used);
        setCast(stack, true);
        if (!world.isClientSide) {
            BeachBoyHookEntity hook = new BeachBoyHookEntity(world, player, charge);
            world.addFreshEntity(hook);
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.FISHING_BOBBER_THROW, SoundCategory.NEUTRAL,
                    0.5F + charge * 0.25F, 0.4F / (random.nextFloat() * 0.4F + 0.8F) + charge * 0.15F);
        }
        else {
            world.playLocalSound(player.getX(), player.getY(), player.getZ(),
                    SoundEvents.FISHING_BOBBER_THROW, SoundCategory.NEUTRAL,
                    0.5F + charge * 0.25F, 0.4F / (random.nextFloat() * 0.4F + 0.8F) + charge * 0.15F, false);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public int getEnchantmentValue() {
        return 1;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        if (enchantment.category == EnchantmentType.FISHING_ROD) {
            return true;
        }
        if (enchantment == Enchantments.UNBREAKING || enchantment == Enchantments.MENDING
                || enchantment == Enchantments.VANISHING_CURSE) {
            return true;
        }
        return super.canApplyAtEnchantingTable(stack, enchantment);
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return true;
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack item, PlayerEntity player) {
        return false;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return false;
    }

    @Override
    public UseAction getUseAnimation(ItemStack stack) {
        return UseAction.BOW;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return oldStack.getItem() != newStack.getItem();
    }

    @Override
    public boolean canContinueUsing(ItemStack oldStack, ItemStack newStack) {
        return oldStack.getItem() == newStack.getItem();
    }

    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    @Override
    public AnimationFactory getFactory() {
        return factory;
    }

    private <E extends IAnimatable> PlayState predicate(AnimationEvent<E> event) {
        event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.beach_boy.idle", true));
        return PlayState.CONTINUE;
    }
}
