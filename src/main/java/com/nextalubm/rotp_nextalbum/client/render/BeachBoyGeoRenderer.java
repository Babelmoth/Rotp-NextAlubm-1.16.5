package com.nextalubm.rotp_nextalbum.client.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.nextalubm.rotp_nextalbum.client.BeachBoyClientReel;
import com.nextalubm.rotp_nextalbum.client.render.model.BeachBoyGeoModel;
import com.nextalubm.rotp_nextalbum.entity.BeachBoyHookEntity;
import com.nextalubm.rotp_nextalbum.entity.BeachBoyStandEntity;
import com.nextalubm.rotp_nextalbum.item.BeachBoyRodItem;
import com.nextalubm.rotp_nextalbum.util.BeachBoyStandUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import software.bernie.geckolib3.geo.render.built.GeoBone;
import software.bernie.geckolib3.renderers.geo.GeoItemRenderer;

public class BeachBoyGeoRenderer extends GeoItemRenderer<BeachBoyRodItem> {
    private static final String ROLL_BONE = "roll";
    private static final float ROLL_SMOOTH_SPEED = 12.0F;
    private final BeachBoyGeoModel geoModel;
    private float currentRoll;
    private long lastRollNs;

    public BeachBoyGeoRenderer() {
        super(new BeachBoyGeoModel());
        this.geoModel = (BeachBoyGeoModel) getGeoModelProvider();
    }

    @Override
    public Integer getUniqueID(BeachBoyRodItem animatable) {
        return animatable.hashCode();
    }

    @Override
    public ResourceLocation getTextureLocation(BeachBoyRodItem instance) {
        return geoModel.getTextureLocation(instance, resolveCast(currentItemStack));
    }

    @Override
    public RenderType getRenderType(BeachBoyRodItem animatable, float partialTicks, MatrixStack stack,
            IRenderTypeBuffer renderTypeBuffer, IVertexBuilder vertexBuilder, int packedLightIn,
            ResourceLocation textureLocation) {
        return RenderType.entityCutoutNoCull(textureLocation);
    }

    @Override
    public void renderRecursively(GeoBone bone, MatrixStack matrixStack, IVertexBuilder buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        if (ROLL_BONE.equals(bone.getName())) {
            float length = resolveReel(currentItemStack);
            float target = isLocalHeldStack(currentItemStack)
                    ? BeachBoyClientReel.angleFor(length)
                    : BeachBoyClientReel.reelToRoll(length);
            currentRoll = damp(currentRoll, target);
            bone.setRotationX(currentRoll);
            bone.setRotationY(0.0F);
            bone.setRotationZ(0.0F);
        }
        super.renderRecursively(bone, matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    private float damp(float current, float target) {
        long now = System.nanoTime();
        if (lastRollNs == 0L) {
            lastRollNs = now;
            return target;
        }
        float dt = MathHelper.clamp((now - lastRollNs) / 1_000_000_000.0F, 0.0F, 0.08F);
        lastRollNs = now;
        float alpha = 1.0F - (float) Math.exp(-ROLL_SMOOTH_SPEED * dt);
        return current + (target - current) * alpha;
    }

    private static boolean resolveCast(ItemStack stack) {
        if (isLocalHeldStack(stack)) {
            net.minecraft.client.entity.player.ClientPlayerEntity player = Minecraft.getInstance().player;
            if (BeachBoyStandUtil.findHook(player) != null) {
                return true;
            }
            BeachBoyStandEntity stand = BeachBoyStandUtil.getStand(player);
            return stand != null && stand.getWhipTicks() > 0;
        }
        return BeachBoyRodItem.isCast(stack);
    }

    private static float resolveReel(ItemStack stack) {
        if (isLocalHeldStack(stack)) {
            BeachBoyHookEntity hook = BeachBoyStandUtil.findHook(Minecraft.getInstance().player);
            if (hook != null) {
                return hook.getVisualReelLength();
            }
            BeachBoyStandEntity stand = BeachBoyStandUtil.getStand(Minecraft.getInstance().player);
            if (stand != null) {
                return stand.getMaxLineLength();
            }
        }
        return BeachBoyRodItem.getReel(stack);
    }

    private static boolean isLocalHeldStack(ItemStack stack) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || stack == null || stack.isEmpty()) {
            return false;
        }
        return stack == mc.player.getMainHandItem() || stack == mc.player.getOffhandItem();
    }
}
