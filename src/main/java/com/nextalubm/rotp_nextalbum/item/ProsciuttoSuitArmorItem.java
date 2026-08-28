package com.nextalubm.rotp_nextalbum.item;

import java.util.List;

import javax.annotation.Nullable;

import com.github.standobyte.jojo.client.ClientUtil;
import com.github.standobyte.jojo.init.ModItems;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.client.render.ProsciuttoSuitArmorRenderer;
import com.nextalubm.rotp_nextalbum.util.ProsciuttoShadowUtil;

import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.IArmorMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;
import software.bernie.geckolib3.util.GeckoLibUtil;

public class ProsciuttoSuitArmorItem extends ArmorItem implements IAnimatable {
    public static final int THEME_COLOR = 0x694bdb;
    public static final String MODE_KEY = "ProsciuttoSuitMode";
    public static final int MODE_NORMAL = 0;
    public static final int MODE_NON_OUTFIT = 1;
    public static final int MODE_STEALTH = 2;

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
    private ItemStack renderingItemStack = ItemStack.EMPTY;

    public ProsciuttoSuitArmorItem(IArmorMaterial material, EquipmentSlotType slot, String modelPath, String texturePath, boolean hideHatOverlay, boolean hideBodyOverlay, boolean hideArmOverlays, boolean hideLegOverlays) {
        this(material, slot, modelPath, texturePath, null, null, hideHatOverlay, hideBodyOverlay, hideArmOverlays, hideLegOverlays);
    }

    public ProsciuttoSuitArmorItem(IArmorMaterial material, EquipmentSlotType slot, String modelPath, String texturePath, @Nullable String slimModelPath, @Nullable String slimTexturePath, boolean hideHatOverlay, boolean hideBodyOverlay, boolean hideArmOverlays, boolean hideLegOverlays) {
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

    public static boolean isProsciuttoSuit(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ProsciuttoSuitArmorItem;
    }

    public static int countPieces(LivingEntity entity) {
        if (entity == null) {
            return 0;
        }
        int count = 0;
        for (ItemStack armorStack : entity.getArmorSlots()) {
            if (isProsciuttoSuit(armorStack)) {
                count++;
            }
        }
        return count;
    }

    public static boolean hasFullSet(LivingEntity entity) {
        return countPieces(entity) >= 3;
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
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity, java.util.function.Consumer<T> onBroken) {
        return 0;
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, net.minecraft.entity.Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
        CharacterSuitTooltips.applyUnbreakable(stack);
    }

    public static int getMode(ItemStack stack) {
        if (!isProsciuttoSuit(stack) || !stack.hasTag()) {
            return MODE_NORMAL;
        }
        return stack.getTag().getInt(MODE_KEY);
    }

    public static void setMode(ItemStack stack, int mode) {
        if (!isProsciuttoSuit(stack)) {
            return;
        }
        stack.getOrCreateTag().putInt(MODE_KEY, mode % 3);
    }

    public static int cycleMode(ItemStack stack) {
        int nextMode = (getMode(stack) + 1) % 3;
        setMode(stack, nextMode);
        return nextMode;
    }

    public static boolean shouldHideArmor(LivingEntity entity) {
        return ProsciuttoShadowUtil.isShadow(entity);
    }

    @Override
    public void onArmorTick(ItemStack stack, World world, PlayerEntity player) {
        super.onArmorTick(stack, world, player);
        if (this.getSlot() == EquipmentSlotType.CHEST) {
            if (hasFullSet(player)) {
                int chestMode = getMode(stack);
                ItemStack legs = player.getItemBySlot(EquipmentSlotType.LEGS);
                ItemStack feet = player.getItemBySlot(EquipmentSlotType.FEET);
                if (getMode(legs) != chestMode) {
                    setMode(legs, chestMode);
                }
                if (getMode(feet) != chestMode) {
                    setMode(feet, chestMode);
                }
            }
        }
    }

    public String getModelPath() {
        return renderingSlimVariant && hasSlimVariant() ? slimModelPath : modelPath;
    }

    public String getTexturePath() {
        return getTexturePath(this.renderingItemStack);
    }

    public String getTexturePath(ItemStack stack) {
        int mode = getMode(stack);
        String baseTexture = renderingSlimVariant && hasSlimVariant() && slimTexturePath != null ? slimTexturePath : texturePath;
        if (mode == MODE_NON_OUTFIT) {
            return baseTexture.replace(".png", "_nonoutfit.png");
        }
        return baseTexture;
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

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
        int mode = getMode(stack);
        ITextComponent modeText;
        switch (mode) {
            case MODE_NON_OUTFIT:
                modeText = new TranslationTextComponent("item.nextalbum.prosciutto_suit.appearance.nonoutfit").withStyle(TextFormatting.GOLD);
                break;
            case MODE_STEALTH:
                modeText = new TranslationTextComponent("item.nextalbum.prosciutto_suit.appearance.stealth").withStyle(TextFormatting.GREEN);
                break;
            default:
                modeText = new TranslationTextComponent("item.nextalbum.prosciutto_suit.appearance.normal").withStyle(TextFormatting.DARK_PURPLE);
                break;
        }

        tooltip.add(modeText);
        int worn = 0;
        if (world != null && world.isClientSide) {
            net.minecraft.client.entity.player.ClientPlayerEntity player = net.minecraft.client.Minecraft.getInstance().player;
            worn = countPieces(player);
        }
        tooltip.add(CharacterSuitTooltips.effectLine(THEME_COLOR,
                "item.nextalbum.suit.effect.assassination_squad", worn, 3,
                "item.nextalbum.suit.effect.assassination_squad.desc"));
        tooltip.add(CharacterSuitTooltips.effectLine(THEME_COLOR,
                "item.nextalbum.suit.effect.glorious_death", worn, 3,
                "item.nextalbum.suit.effect.glorious_death.desc"));
        ClientUtil.addItemReferenceQuote(tooltip, this);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    @SuppressWarnings("unchecked")
    public <A extends BipedModel<?>> A getArmorModel(LivingEntity entityLiving, ItemStack itemStack, EquipmentSlotType armorSlot, A defaultModel) {
        this.renderingSlimVariant = isSlimPlayer(entityLiving);
        this.renderingItemStack = itemStack;
        if (!(renderer instanceof ProsciuttoSuitArmorRenderer)) {
            renderer = new ProsciuttoSuitArmorRenderer();
        }
        ProsciuttoSuitArmorRenderer armorRenderer = (ProsciuttoSuitArmorRenderer) renderer;
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
        this.renderingItemStack = stack;
        return NextAlubm.MOD_ID + ":" + getTexturePath(stack);
    }

    @Override
    public void registerControllers(AnimationData data) {
    }

    @Override
    public AnimationFactory getFactory() {
        return factory;
    }
}