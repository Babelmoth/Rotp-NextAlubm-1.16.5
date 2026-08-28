package com.nextalubm.rotp_nextalbum.item;

import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.github.standobyte.jojo.client.ClientUtil;
import com.github.standobyte.jojo.init.ModItems;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.client.render.PesciSuitArmorRenderer;

import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.LivingEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.IArmorMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;
import software.bernie.geckolib3.util.GeckoLibUtil;

public class PesciSuitArmorItem extends ArmorItem implements IAnimatable {
    public static final int THEME_COLOR = 0x7eb3d6;
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

    public PesciSuitArmorItem(IArmorMaterial material, EquipmentSlotType slot, String modelPath, String texturePath, boolean hideHatOverlay, boolean hideBodyOverlay, boolean hideArmOverlays, boolean hideLegOverlays) {
        this(material, slot, modelPath, texturePath, null, null, hideHatOverlay, hideBodyOverlay, hideArmOverlays, hideLegOverlays);
    }

    public PesciSuitArmorItem(IArmorMaterial material, EquipmentSlotType slot, String modelPath, String texturePath, @Nullable String slimModelPath, @Nullable String slimTexturePath, boolean hideHatOverlay, boolean hideBodyOverlay, boolean hideArmOverlays, boolean hideLegOverlays) {
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

    public static boolean isPesciSuit(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof PesciSuitArmorItem;
    }

    public static int countPieces(LivingEntity entity) {
        if (entity == null) {
            return 0;
        }
        int count = 0;
        for (ItemStack armorStack : entity.getArmorSlots()) {
            if (isPesciSuit(armorStack)) {
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
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity, Consumer<T> onBroken) {
        return 0;
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, net.minecraft.entity.Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
        CharacterSuitTooltips.applyUnbreakable(stack);
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

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
        net.minecraft.client.entity.player.ClientPlayerEntity player = net.minecraft.client.Minecraft.getInstance().player;
        int worn = countPieces(player);
        tooltip.add(CharacterSuitTooltips.effectLine(THEME_COLOR,
                "item.nextalbum.suit.effect.mammoni_resolve", worn, 3,
                "item.nextalbum.suit.effect.mammoni_resolve.desc"));
        tooltip.add(CharacterSuitTooltips.effectLine(THEME_COLOR,
                "item.nextalbum.suit.effect.inherited_will", worn, 3,
                "item.nextalbum.suit.effect.inherited_will.desc"));
        ClientUtil.addItemReferenceQuote(tooltip, this);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    @SuppressWarnings("unchecked")
    public <A extends BipedModel<?>> A getArmorModel(LivingEntity entityLiving, ItemStack itemStack, EquipmentSlotType armorSlot, A defaultModel) {
        this.renderingSlimVariant = isSlimPlayer(entityLiving);
        if (!(renderer instanceof PesciSuitArmorRenderer)) {
            renderer = new PesciSuitArmorRenderer();
        }
        PesciSuitArmorRenderer armorRenderer = (PesciSuitArmorRenderer) renderer;
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
    public void registerControllers(AnimationData data) {
    }

    @Override
    public AnimationFactory getFactory() {
        return factory;
    }
}
