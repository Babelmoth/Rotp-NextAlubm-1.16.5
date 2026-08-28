package com.nextalubm.rotp_nextalbum.item;

import java.util.List;

import javax.annotation.Nullable;

import com.github.standobyte.jojo.init.ModItems;
import com.nextalubm.rotp_nextalbum.entity.SexPistolsEntity;
import com.nextalubm.rotp_nextalbum.compat.TemperatureCompat;
import com.nextalubm.rotp_nextalbum.util.AgingRateState;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.UseAction;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class IceCubeItem extends Item {
    public static final String NBT_MELT = "RotpNextAlbumIceMelt";
    public static final int USE_DURATION = 32;
    private static final float BASE_MELT_PER_TICK = 1.0F / 1800.0F;
    private static final float HELD_MELT_MUL = 2.4F;
    private static final float USE_MELT_MUL = 6.0F;

    public IceCubeItem() {
        super(new Properties().tab(ModItems.MAIN_TAB).stacksTo(1));
    }

    public static boolean isIceCube(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof IceCubeItem;
    }

    public static boolean isHolding(PlayerEntity player) {
        return isIceCube(player.getMainHandItem()) || isIceCube(player.getOffhandItem());
    }

    public static boolean isProvidingCooling(LivingEntity entity) {
        if (entity instanceof PlayerEntity && isHolding((PlayerEntity) entity)) {
            return true;
        }
        return isNearCarriedIce(entity);
    }

    public static boolean isNearCarriedIce(LivingEntity entity) {
        if (entity == null || entity.level == null) {
            return false;
        }
        return !entity.level.getEntitiesOfClass(SexPistolsEntity.class, entity.getBoundingBox().inflate(2.5D),
                stand -> stand.isAlive() && isIceCube(stand.getCarriedItem())).isEmpty();
    }

    public static void tickHeldBy(ItemStack stack, World world, LivingEntity holder) {
        if (world.isClientSide() || !isIceCube(stack) || holder == null) {
            return;
        }
        float envNorm = TemperatureCompat.getEnvironmentNorm(world, holder.blockPosition(), holder);
        float meltMul = 1.0F + 0.85F * envNorm;
        meltMul = MathHelper.clamp(meltMul, 0.12F, 2.10F) * HELD_MELT_MUL;
        addMelt(stack, world, holder, BASE_MELT_PER_TICK * meltMul);
    }

    public static boolean protectsInventory(PlayerEntity player) {
        if (player == null) {
            return false;
        }
        for (int i = 0; i < player.inventory.getContainerSize(); i++) {
            ItemStack stack = player.inventory.getItem(i);
            if (isIceCube(stack) && getMelt(stack) < 1.0F) {
                return true;
            }
        }
        ItemStack carried = player.inventory.getCarried();
        return isIceCube(carried) && getMelt(carried) < 1.0F;
    }

    public static float getUseCooling(PlayerEntity player) {
        if (player.isUsingItem() && isIceCube(player.getUseItem())) {
            return 0.40F;
        }
        return 0F;
    }

    public static float getMelt(ItemStack stack) {
        if (!isIceCube(stack) || !stack.hasTag()) {
            return 0F;
        }
        return MathHelper.clamp(stack.getTag().getFloat(NBT_MELT), 0F, 1F);
    }

    public static void setMelt(ItemStack stack, float melt) {
        if (!isIceCube(stack)) {
            return;
        }
        CompoundNBT tag = stack.getOrCreateTag();
        tag.putFloat(NBT_MELT, MathHelper.clamp(melt, 0F, 1F));
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (world.isClientSide() || !(entity instanceof LivingEntity)) {
            return;
        }
        LivingEntity living = (LivingEntity) entity;
        boolean held = selected || (living.getOffhandItem() == stack);
        boolean using = living.isUsingItem() && living.getUseItem() == stack;
        float envNorm = TemperatureCompat.getEnvironmentNorm(world, living.blockPosition(), living);
        float meltMul = 1.0F + 0.85F * envNorm;
        meltMul = MathHelper.clamp(meltMul, 0.12F, 2.10F);
        if (using) {
            meltMul *= USE_MELT_MUL;
        }
        else if (held) {
            meltMul *= HELD_MELT_MUL;
        }
        addMelt(stack, world, living, BASE_MELT_PER_TICK * meltMul);
    }

    public static void addMelt(ItemStack stack, World world, LivingEntity holder, float amount) {
        if (!isIceCube(stack) || amount <= 0F) {
            return;
        }
        float next = getMelt(stack) + amount;
        if (next < 1.0F) {
            setMelt(stack, next);
            return;
        }
        stack.shrink(1);
        if (!stack.isEmpty()) {
            setMelt(stack, 0F);
        }
        if (!world.isClientSide()) {
            world.playSound(null, holder.getX(), holder.getY(), holder.getZ(),
                    SoundEvents.FIRE_EXTINGUISH, SoundCategory.PLAYERS, 0.35F, 1.35F);
        }
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return USE_DURATION;
    }

    @Override
    public UseAction getUseAnimation(ItemStack stack) {
        return UseAction.DRINK;
    }

    @Override
    public ActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        player.startUsingItem(hand);
        return ActionResult.consume(player.getItemInHand(hand));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, World world, LivingEntity entity) {
        applyLeftoverCool(entity, 1.0F);
        return stack;
    }

    @Override
    public void releaseUsing(ItemStack stack, World world, LivingEntity entity, int timeLeft) {
        int used = USE_DURATION - timeLeft;
        if (used >= 8) {
            applyLeftoverCool(entity, used / (float) USE_DURATION);
        }
    }

    private static void applyLeftoverCool(LivingEntity entity, float strength) {
        AgingRateState state = AgingRateState.of(entity);
        state.startIceCool(0.30F * MathHelper.clamp(strength, 0.25F, 1.0F),
                Math.max(24, (int) (AgingRateState.ICE_COOL_MAX_TICKS * strength)));
        state.save(entity);
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return getMelt(stack) > 0.02F;
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        return getMelt(stack);
    }

    @Override
    public int getRGBDurabilityForDisplay(ItemStack stack) {
        return 0x7EC8E3;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
        tooltip.add(new TranslationTextComponent("item.nextalbum.ice_cube.tooltip").withStyle(TextFormatting.GRAY));
        tooltip.add(new TranslationTextComponent("item.nextalbum.ice_cube.tooltip.inventory").withStyle(TextFormatting.GRAY));
    }
}
