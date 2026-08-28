package com.nextalubm.rotp_nextalbum.item;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.github.standobyte.jojo.client.ClientUtil;
import com.github.standobyte.jojo.init.ModItems;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.power.impl.stand.ResolveCounter;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.client.render.MistaSuitArmorRenderer;

import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.IArmorMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;
import software.bernie.geckolib3.util.GeckoLibUtil;

@EventBusSubscriber(modid = NextAlubm.MOD_ID, bus = EventBusSubscriber.Bus.FORGE)
public class MistaSuitArmorItem extends ArmorItem implements IAnimatable {
    public static final int THEME_COLOR = 0xe75d2f;
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
        super(material, slot, new Properties().stacksTo(1).tab(ModItems.MAIN_TAB).setNoRepair());
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

    public static boolean isMistaSuit(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof MistaSuitArmorItem;
    }

    public static boolean isMistaHat(ItemStack stack) {
        return isAmmoSlot(stack, EquipmentSlotType.HEAD);
    }

    public static boolean isMistaBoots(ItemStack stack) {
        return isAmmoSlot(stack, EquipmentSlotType.FEET);
    }

    public static boolean isSpeedloaderReady(LivingEntity entity) {
        return entity != null
                && (isMistaHat(entity.getItemBySlot(EquipmentSlotType.HEAD))
                || isMistaBoots(entity.getItemBySlot(EquipmentSlotType.FEET)));
    }

    public static boolean isAmmoContainer(ItemStack stack) {
        return isMistaHat(stack) || isMistaBoots(stack);
    }

    private static boolean isAmmoSlot(ItemStack stack, EquipmentSlotType slot) {
        return isMistaSuit(stack) && ((MistaSuitArmorItem) stack.getItem()).getSlot() == slot;
    }

    public static int countPieces(LivingEntity entity) {
        if (entity == null) {
            return 0;
        }
        int count = 0;
        for (ItemStack armorStack : entity.getArmorSlots()) {
            if (isMistaSuit(armorStack)) {
                count++;
            }
        }
        return count;
    }

    public static boolean hasFullSet(LivingEntity entity) {
        return countPieces(entity) >= 4;
    }

    @Override
    public boolean canBeDepleted() {
        return false;
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return false;
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, net.minecraft.entity.Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
        CharacterSuitTooltips.applyUnbreakable(stack);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
        net.minecraft.client.entity.player.ClientPlayerEntity player = net.minecraft.client.Minecraft.getInstance().player;
        int worn = countPieces(player);
        tooltip.add(CharacterSuitTooltips.effectLine(THEME_COLOR,
                "item.nextalbum.suit.effect.narrow_escape", worn, 4,
                "item.nextalbum.suit.effect.narrow_escape.desc"));
        if (isAmmoContainer(stack)) {
            tooltip.add(CharacterSuitTooltips.effectLine(THEME_COLOR,
                    "item.nextalbum.suit.effect.bullet_storage",
                    isAmmoContainer(player == null ? ItemStack.EMPTY : player.getItemBySlot(getSlot())) ? 1 : 0, 1,
                    "item.nextalbum.suit.effect.bullet_storage.desc"));
            tooltip.add(CharacterSuitTooltips.effectLine(THEME_COLOR,
                    "item.nextalbum.suit.effect.speedloader", 1, 1,
                    "item.nextalbum.suit.effect.speedloader.desc"));
            tooltip.add(new TranslationTextComponent("item.nextalbum.suit.effect.bullet_storage.count",
                    getStoredAmmo(stack), AmmoBundleUtil.MAX_OCCUPANCY).withStyle(TextFormatting.BLUE));
        }
        ClientUtil.addItemReferenceQuote(tooltip, this);
    }

    public static boolean isAmmoItem(ItemStack stack) {
        return AmmoBundleUtil.isAmmoItem(stack);
    }

    public static int getStoredAmmo(ItemStack stack) {
        return isAmmoContainer(stack) ? AmmoBundleUtil.getOccupancy(stack) : 0;
    }

    public static int getMaxStoredAmmo() {
        return AmmoBundleUtil.MAX_OCCUPANCY;
    }

    public static List<ItemStack> getStoredAmmoStacks(ItemStack stack) {
        return isAmmoContainer(stack) ? AmmoBundleUtil.getContents(stack) : new ArrayList<ItemStack>();
    }

    public static int getRemainingAmmoCapacity(ItemStack stack) {
        return isAmmoContainer(stack) ? AmmoBundleUtil.getRemainingCapacity(stack) : 0;
    }

    public static int insertAmmo(ItemStack container, ItemStack ammoStack) {
        return isAmmoContainer(container) ? AmmoBundleUtil.insert(container, ammoStack) : 0;
    }

    public static int extractAmmoToSlot(ItemStack container, ItemStack targetSlotStack) {
        return isAmmoContainer(container) ? AmmoBundleUtil.extractInto(container, targetSlotStack) : 0;
    }

    public static boolean consumeOneAmmo(ItemStack container) {
        return consumeAmmo(container, 1) > 0;
    }

    public static int consumeAmmo(ItemStack container, int amount) {
        return isAmmoContainer(container) ? AmmoBundleUtil.consume(container, amount) : 0;
    }

    public static ItemStack removeAmmoStack(ItemStack container) {
        return isAmmoContainer(container) ? AmmoBundleUtil.removeOneStack(container) : ItemStack.EMPTY;
    }

    public static void dropStoredAmmo(ItemStack container, World world, double x, double y, double z) {
        if (isAmmoContainer(container)) {
            AmmoBundleUtil.dropContents(container, world, x, y, z);
        }
    }

    public static int consumeAmmoFromPlayer(PlayerEntity player, int amount) {
        if (player == null || amount <= 0) {
            return 0;
        }
        int remaining = amount;
        remaining -= consumeFromList(player.inventory.armor, remaining);
        if (remaining > 0) {
            remaining -= consumeFromList(player.inventory.items, remaining);
        }
        if (remaining > 0) {
            remaining -= consumeFromList(player.inventory.offhand, remaining);
        }
        return amount - remaining;
    }

    public static int countStoredAmmoOnPlayer(PlayerEntity player) {
        if (player == null) {
            return 0;
        }
        int total = 0;
        for (ItemStack stack : player.inventory.armor) {
            total += getStoredAmmo(stack);
        }
        for (ItemStack stack : player.inventory.items) {
            total += getStoredAmmo(stack);
        }
        for (ItemStack stack : player.inventory.offhand) {
            total += getStoredAmmo(stack);
        }
        return total;
    }

    private static int consumeFromList(Iterable<ItemStack> stacks, int amount) {
        int remaining = amount;
        for (ItemStack stack : stacks) {
            if (remaining <= 0) {
                break;
            }
            remaining -= consumeAmmo(stack, remaining);
        }
        return amount - remaining;
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntityLiving();
        DamageSource source = event.getSource();

        boolean isRevolverBullet = "revolver_bullet".equals(source.getMsgId());
        boolean isProjectile = source.isProjectile();

        if (isRevolverBullet || isProjectile) {
            int suitPiecesCount = 0;
            for (ItemStack armorStack : entity.getArmorSlots()) {
                if (!armorStack.isEmpty() && armorStack.getItem() instanceof MistaSuitArmorItem) {
                    suitPiecesCount++;
                }
            }

            if (suitPiecesCount > 0) {
                float reductionFactor = suitPiecesCount * 0.15F;
                float currentDamage = event.getAmount();
                float newDamage = currentDamage * (1.0F - reductionFactor);
                event.setAmount(newDamage);
            }
            if (suitPiecesCount >= 4 && entity instanceof PlayerEntity && !entity.level.isClientSide) {
                IStandPower power = IStandPower.getStandPowerOptional(entity).orElse(null);
                if (power != null && power.hasPower()) {
                    LivingEntity attacker = source.getEntity() instanceof LivingEntity
                            ? (LivingEntity) source.getEntity() : entity;
                    ResolveCounter.addResolve(power, attacker, Math.max(1.0F, event.getAmount()));
                }
            }
        }
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return isAmmoContainer(stack) && getStoredAmmo(stack) > 0;
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        return 1.0D - Math.max(0.0D, Math.min(1.0D, (double) getStoredAmmo(stack) / (double) AmmoBundleUtil.MAX_OCCUPANCY));
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
        if (isAmmoContainer(stack) && getStoredAmmo(stack) > 0 && (entity.isInLava() || entity.isOnFire())) {
            dropStoredAmmo(stack, entity.level, entity.getX(), entity.getY(), entity.getZ());
            entity.setItem(stack);
        }
        return false;
    }

    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity, Consumer<T> onBroken) {
        if (isAmmoContainer(stack) && amount > 0 && getStoredAmmo(stack) > 0) {
            dropStoredAmmo(stack, entity.level, entity.getX(), entity.getY(), entity.getZ());
        }
        return 0;
    }

    @Override
    public void registerControllers(AnimationData data) {
    }

    @Override
    public AnimationFactory getFactory() {
        return factory;
    }
}