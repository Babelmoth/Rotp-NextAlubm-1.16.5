package com.nextalubm.rotp_nextalbum.item;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.github.standobyte.jojo.init.ModItems;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.client.render.MistaSuitArmorRenderer;
import com.nextalubm.rotp_nextalbum.init.InitItems;

import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.IArmorMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;
import software.bernie.geckolib3.util.GeckoLibUtil;

public class MistaSuitArmorItem extends ArmorItem implements IAnimatable {
    private static final String STORED_AMMO_KEY = "StoredRevolverAmmo";
    private static final int MAX_STORED_AMMO = 128;
    private final AnimationFactory factory = GeckoLibUtil.createFactory(this);
    private final String modelPath;
    private final String texturePath;
    private final String slimModelPath;
    private final String slimTexturePath;
    private final boolean hideHatOverlay;
    private final boolean hideBodyOverlay;
    private final boolean hideArmOverlays;
    private final boolean hideLegOverlays;
    private Object renderer;
    private boolean renderingSlimVariant;

    public MistaSuitArmorItem(IArmorMaterial material, EquipmentSlotType slot, String modelPath, String texturePath, boolean hideHatOverlay, boolean hideBodyOverlay, boolean hideArmOverlays, boolean hideLegOverlays) {
        this(material, slot, modelPath, texturePath, null, null, hideHatOverlay, hideBodyOverlay, hideArmOverlays, hideLegOverlays);
    }

    public MistaSuitArmorItem(IArmorMaterial material, EquipmentSlotType slot, String modelPath, String texturePath, @Nullable String slimModelPath, @Nullable String slimTexturePath, boolean hideHatOverlay, boolean hideBodyOverlay, boolean hideArmOverlays, boolean hideLegOverlays) {
        super(material, slot, new Properties().stacksTo(1).tab(ModItems.MAIN_TAB));
        this.modelPath = modelPath;
        this.texturePath = texturePath;
        this.slimModelPath = slimModelPath;
        this.slimTexturePath = slimTexturePath;
        this.hideHatOverlay = hideHatOverlay;
        this.hideBodyOverlay = hideBodyOverlay;
        this.hideArmOverlays = hideArmOverlays;
        this.hideLegOverlays = hideLegOverlays;
    }

    public String getModelPath() {
        return renderingSlimVariant && hasSlimVariant() ? slimModelPath : modelPath;
    }

    public String getTexturePath() {
        return renderingSlimVariant && hasSlimVariant() && slimTexturePath != null ? slimTexturePath : texturePath;
    }

    public boolean hasSlimVariant() {
        return slimModelPath != null && !slimModelPath.isEmpty();
    }

    public boolean hidesHatOverlay() {
        return hideHatOverlay;
    }

    public boolean hidesBodyOverlay() {
        return hideBodyOverlay;
    }

    public boolean hidesArmOverlays() {
        return hideArmOverlays;
    }

    public boolean hidesLegOverlays() {
        return hideLegOverlays;
    }

    public static boolean isMistaHat(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof MistaSuitArmorItem && ((MistaSuitArmorItem) stack.getItem()).getSlot() == EquipmentSlotType.HEAD;
    }

    public static boolean isAmmoItem(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == InitItems.REVOLVER_AMMO.get();
    }

    public static int getStoredAmmo(ItemStack stack) {
        return isMistaHat(stack) ? Math.max(0, Math.min(MAX_STORED_AMMO, stack.getOrCreateTag().getInt(STORED_AMMO_KEY))) : 0;
    }

    public static int getMaxStoredAmmo() {
        return MAX_STORED_AMMO;
    }

    public static List<ItemStack> getStoredAmmoStacks(ItemStack stack) {
        List<ItemStack> stacks = new ArrayList<>();
        int stored = getStoredAmmo(stack);
        while (stored > 0 && stacks.size() < 2) {
            int count = Math.min(stored, InitItems.REVOLVER_AMMO.get().getMaxStackSize());
            stacks.add(new ItemStack(InitItems.REVOLVER_AMMO.get(), count));
            stored -= count;
        }
        return stacks;
    }

    public static int getRemainingAmmoCapacity(ItemStack stack) {
        return Math.max(0, MAX_STORED_AMMO - getStoredAmmo(stack));
    }

    public static int insertAmmo(ItemStack hatStack, ItemStack ammoStack) {
        if (!isMistaHat(hatStack) || !isAmmoItem(ammoStack)) {
            return 0;
        }
        int moved = Math.min(ammoStack.getCount(), getRemainingAmmoCapacity(hatStack));
        if (moved <= 0) {
            return 0;
        }
        setStoredAmmo(hatStack, getStoredAmmo(hatStack) + moved);
        ammoStack.shrink(moved);
        return moved;
    }

    public static boolean consumeOneAmmo(ItemStack hatStack) {
        int stored = getStoredAmmo(hatStack);
        if (stored <= 0) {
            return false;
        }
        setStoredAmmo(hatStack, stored - 1);
        return true;
    }

    public static ItemStack removeAmmoStack(ItemStack hatStack) {
        int stored = getStoredAmmo(hatStack);
        if (stored <= 0) {
            return ItemStack.EMPTY;
        }
        int removed = Math.min(stored, InitItems.REVOLVER_AMMO.get().getMaxStackSize());
        setStoredAmmo(hatStack, stored - removed);
        return new ItemStack(InitItems.REVOLVER_AMMO.get(), removed);
    }

    public static void dropStoredAmmo(ItemStack hatStack, World world, double x, double y, double z) {
        if (world == null || world.isClientSide) {
            return;
        }
        for (ItemStack storedStack : getStoredAmmoStacks(hatStack)) {
            world.addFreshEntity(new ItemEntity(world, x, y, z, storedStack));
        }
        setStoredAmmo(hatStack, 0);
    }

    private static void setStoredAmmo(ItemStack stack, int amount) {
        int clamped = Math.max(0, Math.min(MAX_STORED_AMMO, amount));
        if (clamped <= 0) {
            stack.removeTagKey(STORED_AMMO_KEY);
        }
        else {
            stack.getOrCreateTag().putInt(STORED_AMMO_KEY, clamped);
        }
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return isMistaHat(stack) && getStoredAmmo(stack) > 0;
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        return 1.0D - Math.max(0.0D, Math.min(1.0D, (double) getStoredAmmo(stack) / (double) MAX_STORED_AMMO));
    }

    @Override
    public int getRGBDurabilityForDisplay(ItemStack stack) {
        return 0x7088FF;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    @SuppressWarnings("unchecked")
    public <A extends BipedModel<?>> A getArmorModel(LivingEntity entityLiving, ItemStack itemStack, EquipmentSlotType armorSlot, A defaultModel) {
        this.renderingSlimVariant = isSlimPlayer(entityLiving);
        if (!(renderer instanceof MistaSuitArmorRenderer)) {
            renderer = new MistaSuitArmorRenderer();
        }
        MistaSuitArmorRenderer armorRenderer = (MistaSuitArmorRenderer) renderer;
        armorRenderer.setCurrentItem(entityLiving, itemStack, armorSlot);
        armorRenderer.applyEntityStats(defaultModel);
        return (A) armorRenderer;
    }

    @OnlyIn(Dist.CLIENT)
    private boolean isSlimPlayer(LivingEntity entityLiving) {
        return hasSlimVariant() && entityLiving instanceof AbstractClientPlayerEntity && "slim".equals(((AbstractClientPlayerEntity) entityLiving).getModelName());
    }

    @Override
    public String getArmorTexture(ItemStack stack, net.minecraft.entity.Entity entity, EquipmentSlotType slot, String type) {
        if (entity instanceof LivingEntity) {
            this.renderingSlimVariant = isSlimPlayer((LivingEntity) entity);
        }
        return NextAlubm.MOD_ID + ":" + getTexturePath();
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (isMistaHat(stack) && getStoredAmmo(stack) > 0 && (entity.isInLava() || entity.isOnFire())) {
            dropStoredAmmo(stack, entity.level, entity.getX(), entity.getY(), entity.getZ());
            entity.setItem(stack);
        }
        return false;
    }

    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity, Consumer<T> onBroken) {
        if (isMistaHat(stack) && amount > 0 && stack.getDamageValue() + amount >= stack.getMaxDamage()) {
            dropStoredAmmo(stack, entity.level, entity.getX(), entity.getY(), entity.getZ());
        }
        return amount;
    }

    @Override
    public void registerControllers(AnimationData data) {
    }

    @Override
    public AnimationFactory getFactory() {
        return factory;
    }
}