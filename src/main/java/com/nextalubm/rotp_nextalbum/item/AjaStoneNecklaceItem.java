package com.nextalubm.rotp_nextalbum.item;

import com.github.standobyte.jojo.client.ClientUtil;
import com.github.standobyte.jojo.client.sound.ClientTickingSoundsHelper;
import com.github.standobyte.jojo.entity.damaging.LightBeamEntity;
import com.github.standobyte.jojo.init.ModEntityTypes;
import com.github.standobyte.jojo.init.ModSounds;
import com.github.standobyte.jojo.init.power.non_stand.ModPowers;
import com.github.standobyte.jojo.init.power.non_stand.hamon.ModHamonSkills;
import com.github.standobyte.jojo.item.CustomModelArmorItem;
import com.github.standobyte.jojo.power.impl.nonstand.INonStandPower;
import com.github.standobyte.jojo.power.impl.nonstand.type.hamon.HamonData;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.init.InitItems;
import com.nextalubm.rotp_nextalbum.util.CuriosIntegration;

import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.KeybindTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;
import software.bernie.geckolib3.util.GeckoLibUtil;

import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import top.theillusivec4.curios.api.SlotTypeMessage;
import top.theillusivec4.curios.api.SlotTypePreset;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.IRenderTypeBuffer;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = NextAlubm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AjaStoneNecklaceItem extends ArmorItem implements IAnimatable, ICurioItem {

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(NextAlubm.MOD_ID, "necklace_beam_sync"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    static {
        CHANNEL.registerMessage(0, PacketFireBeam.class, (msg, buf) -> {}, buf -> new PacketFireBeam(), (msg, ctxSupplier) -> {
            ctxSupplier.get().enqueueWork(() -> {
                ServerPlayerEntity player = ctxSupplier.get().getSender();
                if (player != null && isEquipped(player, InitItems.AJA_STONE_NECKLACE.get()) && !player.getCooldowns().isOnCooldown(InitItems.AJA_STONE_NECKLACE.get())) {
                    fireSuperAjaBeam(player.level, player);
                }
            });
            ctxSupplier.get().setPacketHandled(true);
        });
    }

    public static class PacketFireBeam {
        public PacketFireBeam() {}
    }

    private final AnimationFactory factory = GeckoLibUtil.createFactory(this);
    private Object renderer;

    private static final Map<UUID, Integer> CHARGING_PLAYERS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> RIGHT_CLICK_TIMEOUT = new ConcurrentHashMap<>();

    public AjaStoneNecklaceItem(Item.Properties properties) {
        super(ArmorMaterial.LEATHER, EquipmentSlotType.HEAD, properties);
    }

    public static boolean isEquipped(LivingEntity entity, Item item) {
        if (entity == null) return false;

        if (entity.getItemBySlot(EquipmentSlotType.HEAD).getItem() == item) {
            return true;
        }
        return CuriosApi.getCuriosHelper().findEquippedCurio(item, entity).isPresent();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    @SuppressWarnings("unchecked")
    public <A extends BipedModel<?>> A getArmorModel(LivingEntity entityLiving, ItemStack itemStack, EquipmentSlotType armorSlot, A defaultModel) {
        if (!(renderer instanceof com.nextalubm.rotp_nextalbum.client.render.AjaStoneNecklaceRenderer)) {
            renderer = new com.nextalubm.rotp_nextalbum.client.render.AjaStoneNecklaceRenderer();
        }
        com.nextalubm.rotp_nextalbum.client.render.AjaStoneNecklaceRenderer armorRenderer = (com.nextalubm.rotp_nextalbum.client.render.AjaStoneNecklaceRenderer) renderer;
        armorRenderer.setCurrentItem(entityLiving, itemStack, armorSlot);
        armorRenderer.applyEntityStats(defaultModel);
        return (A) armorRenderer;
    }

    @Override
    public String getArmorTexture(ItemStack stack, net.minecraft.entity.Entity entity, EquipmentSlotType slot, String type) {
        return NextAlubm.MOD_ID + ":textures/models/armor/aja_stone_necklace.png";
    }

    @Override
    public void registerControllers(AnimationData data) {
    }

    @Override
    public AnimationFactory getFactory() {
        return factory;
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        handleRightClick(event.getPlayer(), event);
    }

    @SubscribeEvent
    public static void onRightClickEmpty(PlayerInteractEvent.RightClickEmpty event) {
        handleRightClick(event.getPlayer(), event);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        handleRightClick(event.getPlayer(), event);
    }

    private static void handleRightClick(PlayerEntity player, PlayerInteractEvent event) {
        if (player == null) return;
        UUID uuid = player.getUUID();

        if (CHARGING_PLAYERS.containsKey(uuid)) {
            if (isEquipped(player, InitItems.AJA_STONE_NECKLACE.get())) {
                RIGHT_CLICK_TIMEOUT.put(uuid, 0);
                if (!player.level.isClientSide() && event.isCancelable()) event.setCanceled(true);
            }
            return;
        }

        if (!player.isShiftKeyDown()) return;
        if (!isEquipped(player, InitItems.AJA_STONE_NECKLACE.get())) return;
        if (player.getCooldowns().isOnCooldown(InitItems.AJA_STONE_NECKLACE.get())) return;

        INonStandPower power = INonStandPower.getPlayerNonStandPower(player);
        if (power != null && power.getTypeSpecificData(ModPowers.HAMON.get()).isPresent()) {
            HamonData hamon = power.getTypeSpecificData(ModPowers.HAMON.get()).get();

            if (power.getHeldAction() == null) {
                if (hamon.isSkillLearned(ModHamonSkills.AJA_STONE_KEEPER.get())) {
                    if (!player.level.isClientSide() && event.isCancelable()) {
                        event.setCanceled(true);
                    }
                    fireSuperAjaBeam(player.level, player);
                }
                else {
                    if (sufficientLight(player.level, player)) {
                        if (!player.level.isClientSide() && event.isCancelable()) {
                            event.setCanceled(true);
                        }
                        CHARGING_PLAYERS.put(uuid, 0);
                        RIGHT_CLICK_TIMEOUT.put(uuid, 0);

                        if (player.level.isClientSide()) {
                            ClientTickingSoundsHelper.playStoppableEntitySound(player, ModSounds.AJA_STONE_CHARGING.get(),
                                    0.25F, 1.0F, false, p -> CHARGING_PLAYERS.containsKey(p.getUUID()));
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        PlayerEntity player = event.player;
        UUID uuid = player.getUUID();

        if (CHARGING_PLAYERS.containsKey(uuid)) {
            int timeout = RIGHT_CLICK_TIMEOUT.getOrDefault(uuid, 0) + 1;

            if (!player.isAlive() || !isEquipped(player, InitItems.AJA_STONE_NECKLACE.get()) || timeout > 8) {
                CHARGING_PLAYERS.remove(uuid);
                RIGHT_CLICK_TIMEOUT.remove(uuid);
                return;
            }

            RIGHT_CLICK_TIMEOUT.put(uuid, timeout);
            int ticks = CHARGING_PLAYERS.get(uuid) + 1;

            if (ticks >= 50) {
                CHARGING_PLAYERS.remove(uuid);
                RIGHT_CLICK_TIMEOUT.remove(uuid);

                if (sufficientLight(player.level, player)) {
                    fireSuperAjaBeam(player.level, player);
                }
            } else {
                CHARGING_PLAYERS.put(uuid, ticks);
            }
        }
    }

    private static void fireSuperAjaBeam(World world, PlayerEntity player) {
        float damage = 40.0F;

        player.playSound(ModSounds.AJA_STONE_BEAM.get(),
                Math.min(0.02F * damage, 1.0F),
                1.0F + (player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 0.1F);

        player.getCooldowns().addCooldown(InitItems.AJA_STONE_NECKLACE.get(), 150);

        if (world.isClientSide()) {
            CHANNEL.sendToServer(new PacketFireBeam());
        } else {
            LightBeamEntity beam = new LightBeamEntity(ModEntityTypes.AJA_STONE_BEAM.get(), player, world);
            beam.shoot(damage, 16F + damage / 2F);
            world.addFreshEntity(beam);
        }
    }

    private static boolean sufficientLight(World world, LivingEntity entity) {
        BlockPos pos = entity.blockPosition();
        if (!world.isClientSide()) {
            return world.getMaxLocalRawBrightness(pos) > 9;
        }

        int time = (int) (world.getDayTime() % 24000);
        int light = world.dimension() != World.OVERWORLD ||
                world.isRainingAt(pos) || time > 12866 && time < 23135 ? world.getBrightness(LightType.BLOCK, pos) : world.getMaxLocalRawBrightness(pos);
        return light > 9;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
        tooltip.add(new TranslationTextComponent("item.nextalbum.aja_stone_necklace.tooltips",
                new KeybindTextComponent("key.sneak"),new KeybindTextComponent("key.mouse.right")).withStyle(TextFormatting.GRAY));
        ClientUtil.addItemReferenceQuote(tooltip, this);
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return true;
    }

    @Override
    public boolean canRender(String identifier, int index, LivingEntity livingEntity, ItemStack stack) {
        return true;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void render(String identifier, int index, MatrixStack matrixStack, IRenderTypeBuffer buffer, int light,
                       LivingEntity entity, float limbSwing, float limbSwingAmount, float partialTicks,
                       float ticks, float headYaw, float headPitch, ItemStack stack) {
        if (entity.level.isClientSide) {
            CurioRenderHelper.render(matrixStack, buffer, light, entity, limbSwing, limbSwingAmount, partialTicks, ticks, headYaw, headPitch, stack, this);
        }
    }

    private static class CurioRenderHelper {
        @OnlyIn(Dist.CLIENT)
        public static void render(MatrixStack matrixStack, IRenderTypeBuffer buffer, int light, LivingEntity entity,
                                  float limbSwing, float limbSwingAmount, float partialTicks, float ticks,
                                  float headYaw, float headPitch, ItemStack stack, AjaStoneNecklaceItem item) {

            if (!(item.renderer instanceof com.nextalubm.rotp_nextalbum.client.render.AjaStoneNecklaceRenderer)) {
                item.renderer = new com.nextalubm.rotp_nextalbum.client.render.AjaStoneNecklaceRenderer();
            }
            com.nextalubm.rotp_nextalbum.client.render.AjaStoneNecklaceRenderer armorRenderer =
                    (com.nextalubm.rotp_nextalbum.client.render.AjaStoneNecklaceRenderer) item.renderer;

            armorRenderer.setCurrentItem(entity, stack, EquipmentSlotType.HEAD);

            armorRenderer.young = entity.isBaby();
            armorRenderer.riding = entity.isPassenger();
            armorRenderer.crouching = entity.isCrouching();
            armorRenderer.attackTime = entity.getAttackAnim(partialTicks);

            armorRenderer.setupAnim(entity, limbSwing, limbSwingAmount, ticks, headYaw, headPitch);

            net.minecraft.util.ResourceLocation texture = new net.minecraft.util.ResourceLocation(
                    NextAlubm.MOD_ID, "textures/models/armor/aja_stone_necklace.png");
            com.mojang.blaze3d.vertex.IVertexBuilder vertexBuilder =
                    buffer.getBuffer(net.minecraft.client.renderer.RenderType.armorCutoutNoCull(texture));

            armorRenderer.renderToBuffer(matrixStack, vertexBuilder, light,
                    net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}