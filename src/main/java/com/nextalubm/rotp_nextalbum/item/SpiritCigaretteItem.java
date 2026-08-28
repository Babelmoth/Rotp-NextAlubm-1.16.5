package com.nextalubm.rotp_nextalbum.item;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import com.github.standobyte.jojo.init.ModItems;
import com.nextalubm.rotp_nextalbum.network.CigaretteSmokePacket;
import com.nextalubm.rotp_nextalbum.util.CigaretteAdvancements;
import com.nextalubm.rotp_nextalbum.util.CigaretteFocusUtil;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
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
import net.minecraftforge.fml.DistExecutor;

public class SpiritCigaretteItem extends Item {
    public static final String NBT_LIT = "RotpNextAlbumCigLit";
    public static final String NBT_BURN = "RotpNextAlbumCigBurn";
    public static final String NBT_INHALE_START = "RotpNextAlbumCigInhaleStart";
    public static final float BASE_BURN_PER_TICK = 1.0F / 2400.0F;
    private static final float INHALE_BURN_MUL = 4.0F;
    private static final int MIN_INHALE_TICKS = 8;

    public SpiritCigaretteItem() {
        super(new Properties().tab(ModItems.MAIN_TAB).stacksTo(16)
                .setISTER(() -> com.nextalubm.rotp_nextalbum.client.render.CigaretteItemRenderer::new));
    }

    public static boolean isCigarette(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof SpiritCigaretteItem;
    }

    public static boolean isLit(ItemStack stack) {
        return isCigarette(stack) && stack.hasTag() && stack.getTag().getBoolean(NBT_LIT);
    }

    public static void setLit(ItemStack stack, boolean lit) {
        if (!isCigarette(stack)) {
            return;
        }
        CompoundNBT tag = stack.getOrCreateTag();
        tag.putBoolean(NBT_LIT, lit);
        if (lit) {
            if (!tag.hasUUID("CigId")) {
                tag.putUUID("CigId", UUID.randomUUID());
            }
        }
        else {
            tag.remove("CigIdMost");
            tag.remove("CigIdLeast");
        }
    }

    public static float getBurn(ItemStack stack) {
        if (!isCigarette(stack) || !stack.hasTag()) {
            return 0F;
        }
        return MathHelper.clamp(stack.getTag().getFloat(NBT_BURN), 0F, 1F);
    }

    public static void setBurn(ItemStack stack, float burn) {
        if (!isCigarette(stack)) {
            return;
        }
        stack.getOrCreateTag().putFloat(NBT_BURN, MathHelper.clamp(burn, 0F, 1F));
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (world.isClientSide() || !(entity instanceof LivingEntity) || !isLit(stack)) {
            return;
        }
        LivingEntity living = (LivingEntity) entity;
        boolean using = living.isUsingItem() && living.getUseItem() == stack;
        int used = using ? living.getTicksUsingItem() : 0;
        float inhaleStart = using && stack.hasTag() ? stack.getTag().getFloat(NBT_INHALE_START) : getBurn(stack);
        boolean consumed = addBurn(stack, world, living, BASE_BURN_PER_TICK * (using ? INHALE_BURN_MUL : 1.0F));
        if (consumed && using && used >= MIN_INHALE_TICKS) {
            CigaretteFocusUtil.addFocus(living, used * CigaretteFocusUtil.INHALE_FOCUS_PER_TICK);
            CigaretteSmokePacket.sendAround(living, used);
            if (inhaleStart <= 0.05F && living instanceof ServerPlayerEntity) {
                CigaretteAdvancements.awardSmokedTheWorld((ServerPlayerEntity) living);
            }
        }
    }

    public static boolean addBurn(ItemStack stack, World world, LivingEntity holder, float amount) {
        if (!isCigarette(stack) || amount <= 0F) {
            return false;
        }
        float next = getBurn(stack) + amount;
        if (next < 1.0F) {
            setBurn(stack, next);
            return false;
        }
        stack.shrink(1);
        if (!stack.isEmpty()) {
            setLit(stack, false);
            setBurn(stack, 0F);
        }
        if (!world.isClientSide()) {
            world.playSound(null, holder.getX(), holder.getY(), holder.getZ(),
                    SoundEvents.FIRE_EXTINGUISH, SoundCategory.PLAYERS, 0.35F, 1.35F);
            holder.stopUsingItem();
        }
        return true;
    }

    @Override
    public ActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (CigaretteMouth.getCount(player) >= CigaretteMouth.MAX_MOUTH) {
                return ActionResult.fail(stack);
            }
            if (!world.isClientSide()) {
                boolean split = stack.getCount() > 1;
                ItemStack toPlace = split ? stack.split(1) : stack;
                boolean justLit = !isLit(toPlace);
                if (justLit) {
                    setLit(toPlace, true);
                }
                if (CigaretteMouth.add(player, toPlace)) {
                    world.playSound(null, player.getX(), player.getY(), player.getZ(),
                            justLit ? SoundEvents.FLINTANDSTEEL_USE : SoundEvents.ARMOR_EQUIP_LEATHER,
                            SoundCategory.PLAYERS, 0.45F, justLit ? 1.15F : 1.2F);
                }
                else if (split && !toPlace.isEmpty() && !player.addItem(toPlace)) {
                    player.drop(toPlace, false);
                }
            }
            return ActionResult.sidedSuccess(player.getItemInHand(hand), world.isClientSide());
        }
        if (!isLit(stack)) {
            light(world, player, stack);
            return ActionResult.sidedSuccess(stack, world.isClientSide());
        }
        stack.getOrCreateTag().putFloat(NBT_INHALE_START, getBurn(stack));
        player.startUsingItem(hand);
        return ActionResult.consume(stack);
    }

    @Override
    public void onUseTick(World world, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (!world.isClientSide() || !isLit(stack)) {
            return;
        }
        int used = getUseDuration(stack) - remainingUseDuration;
        if (used > 0 && used % 10 == 0) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    com.nextalubm.rotp_nextalbum.client.ClientCigaretteState.spawnInhaleSmoke(entity));
        }
    }

    private static void light(World world, PlayerEntity player, ItemStack stack) {
        if (stack.getCount() > 1) {
            stack.shrink(1);
            ItemStack lit = new ItemStack(stack.getItem());
            setLit(lit, true);
            if (!player.addItem(lit)) {
                player.drop(lit, false);
            }
        }
        else {
            setLit(stack, true);
        }
        if (!world.isClientSide()) {
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.FLINTANDSTEEL_USE, SoundCategory.PLAYERS, 0.6F, 1.15F);
        }
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAction getUseAnimation(ItemStack stack) {
        return isLit(stack) ? UseAction.BOW : UseAction.NONE;
    }

    @Override
    public void releaseUsing(ItemStack stack, World world, LivingEntity entity, int timeLeft) {
        int used = getUseDuration(stack) - timeLeft;
        if (used < MIN_INHALE_TICKS || !isLit(stack)) {
            return;
        }
        if (!world.isClientSide()) {
            CigaretteFocusUtil.addFocus(entity, used * CigaretteFocusUtil.INHALE_FOCUS_PER_TICK);
            CigaretteSmokePacket.sendAround(entity, used);
        }
    }

    @Override
    public boolean canContinueUsing(ItemStack oldStack, ItemStack newStack) {
        return oldStack.getItem() == newStack.getItem() && isLit(newStack);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem() || isLit(oldStack) != isLit(newStack);
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return isLit(stack) && getBurn(stack) > 0.02F;
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        return getBurn(stack);
    }

    @Override
    public int getRGBDurabilityForDisplay(ItemStack stack) {
        return isLit(stack) ? 0xE07A2F : 0xC8C8C8;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
        tooltip.add(new TranslationTextComponent("item.nextalbum.spirit_cigarette.tooltip").withStyle(TextFormatting.GRAY));
        tooltip.add(new TranslationTextComponent("item.nextalbum.spirit_cigarette.tooltip.mouth").withStyle(TextFormatting.GRAY));
    }
}
