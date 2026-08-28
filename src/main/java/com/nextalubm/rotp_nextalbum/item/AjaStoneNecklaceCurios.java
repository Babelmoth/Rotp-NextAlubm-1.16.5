package com.nextalubm.rotp_nextalbum.item;

import com.nextalubm.rotp_nextalbum.NextAlubm;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;

import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.SlotTypeMessage;
import top.theillusivec4.curios.api.SlotTypePreset;
import top.theillusivec4.curios.api.type.capability.ICurio;

import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public final class AjaStoneNecklaceCurios {
    private static final ResourceLocation CAP_ID = new ResourceLocation(NextAlubm.MOD_ID, "aja_stone_necklace_curio");

    private AjaStoneNecklaceCurios() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.register(AjaStoneNecklaceCurios.class);
    }

    public static void onEnqueueIMC(final InterModEnqueueEvent event) {
        InterModComms.sendTo("curios", SlotTypeMessage.REGISTER_TYPE,
                () -> SlotTypePreset.NECKLACE.getMessageBuilder().size(1).build());
    }

    @SubscribeEvent
    public static void attachCapabilities(AttachCapabilitiesEvent<ItemStack> event) {
        if (!(event.getObject().getItem() instanceof AjaStoneNecklaceItem)) {
            return;
        }
        event.addCapability(CAP_ID, new NecklaceCurio(event.getObject()));
    }

    private static final class NecklaceCurio implements ICurio, ICapabilityProvider {
        private final ItemStack stack;
        private final LazyOptional<ICurio> optional = LazyOptional.of(() -> this);

        private NecklaceCurio(ItemStack stack) {
            this.stack = stack;
        }

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
            return CuriosCapability.ITEM.orEmpty(cap, optional);
        }

        @Override
        public boolean canEquipFromUse(SlotContext slotContext) {
            return true;
        }

        @Override
        public boolean canRender(String identifier, int index, LivingEntity livingEntity) {
            return true;
        }

        @Override
        @OnlyIn(Dist.CLIENT)
        public void render(String identifier, int index, MatrixStack matrixStack, IRenderTypeBuffer buffer, int light,
                LivingEntity entity, float limbSwing, float limbSwingAmount, float partialTicks,
                float ticks, float headYaw, float headPitch) {
            renderNecklace(matrixStack, buffer, light, entity, limbSwing, limbSwingAmount, partialTicks,
                    ticks, headYaw, headPitch, stack);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void renderNecklace(MatrixStack matrixStack, IRenderTypeBuffer buffer, int light, LivingEntity entity,
            float limbSwing, float limbSwingAmount, float partialTicks, float ticks,
            float headYaw, float headPitch, ItemStack stack) {
        AjaStoneNecklaceItem item = (AjaStoneNecklaceItem) stack.getItem();
        if (!(item.getClientRenderer() instanceof com.nextalubm.rotp_nextalbum.client.render.AjaStoneNecklaceRenderer)) {
            item.setClientRenderer(new com.nextalubm.rotp_nextalbum.client.render.AjaStoneNecklaceRenderer());
        }
        com.nextalubm.rotp_nextalbum.client.render.AjaStoneNecklaceRenderer armorRenderer =
                (com.nextalubm.rotp_nextalbum.client.render.AjaStoneNecklaceRenderer) item.getClientRenderer();
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
