package com.nextalubm.rotp_nextalbum.client.render;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.client.BeachBoyClientRodTips;
import com.nextalubm.rotp_nextalbum.entity.BeachBoyHookEntity;
import com.nextalubm.rotp_nextalbum.entity.BeachBoyStandEntity;
import com.nextalubm.rotp_nextalbum.util.BeachBoyLinePath;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix3f;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import software.bernie.geckolib3.geo.render.built.GeoBone;
import software.bernie.geckolib3.geo.render.built.GeoCube;
import software.bernie.geckolib3.geo.render.built.GeoModel;
import software.bernie.geckolib3.geo.render.built.GeoQuad;
import software.bernie.geckolib3.geo.render.built.GeoVertex;
import software.bernie.geckolib3.resource.GeckoLibCache;
import software.bernie.geckolib3.util.RenderUtils;

public class BeachBoyHookRenderer extends EntityRenderer<BeachBoyHookEntity> {
    private static final ResourceLocation GEO_MODEL = new ResourceLocation(NextAlubm.MOD_ID, "geo/beach_boy_hook.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(NextAlubm.MOD_ID, "textures/entity/beach_boy_hook.png");
    private static final ResourceLocation LINE_TEXTURE = new ResourceLocation(NextAlubm.MOD_ID, "textures/misc/white.png");
    private static final RenderType HOOK_TYPE = BeachBoyFullBrightRenderType.hook(TEXTURE);
    private static final RenderType HOOK_SEE_THROUGH = BeachBoyFullBrightRenderType.hookSeeThrough(TEXTURE);
    private static final RenderType LINE_TYPE = BeachBoyFullBrightRenderType.line(LINE_TEXTURE);
    private static final int FULLBRIGHT = 0xF000F0;
    private static final float LINE_HALF_WIDTH = 0.016F;
    private static final int LINE_RED = 0xFF;
    private static final int LINE_GREEN = 0xA4;
    private static final int LINE_BLUE = 0xDB;

    public BeachBoyHookRenderer(EntityRendererManager renderManager) {
        super(renderManager);
        this.shadowRadius = 0.0F;
    }

    @Override
    public boolean shouldRender(BeachBoyHookEntity entity, ClippingHelper clippingHelper,
            double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public void render(BeachBoyHookEntity entity, float entityYaw, float partialTicks, MatrixStack matrixStack,
            IRenderTypeBuffer buffer, int packedLight) {
        if (entity.isTrapArmed()) {
            renderTrapBlockGlow(entity, partialTicks, matrixStack, buffer);
            Entity owner = entity.getOwner();
            if (owner != null && !isRemoteCameraFor(entity)) {
                renderTrapStub(entity, owner, partialTicks, matrixStack, buffer, FULLBRIGHT);
            }
            super.render(entity, entityYaw, partialTicks, matrixStack, buffer, FULLBRIGHT);
            return;
        }
        renderHookModel(entity, partialTicks, matrixStack, buffer, FULLBRIGHT);
        Entity owner = entity.getOwner();
        if (owner != null && !isRemoteCameraFor(entity) && !isWhippingHookedTarget(entity, owner)) {
            renderLine(entity, owner, partialTicks, matrixStack, buffer, FULLBRIGHT);
        }
        super.render(entity, entityYaw, partialTicks, matrixStack, buffer, FULLBRIGHT);
    }

    @Override
    protected int getBlockLightLevel(BeachBoyHookEntity entity, BlockPos pos) {
        return 15;
    }

    @Override
    protected int getSkyLightLevel(BeachBoyHookEntity entity, BlockPos pos) {
        return 15;
    }

    private void renderTrapBlockGlow(BeachBoyHookEntity entity, float partialTicks, MatrixStack matrixStack,
            IRenderTypeBuffer buffer) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player != entity.getOwner()) {
            return;
        }
        java.util.Optional<BlockPos> trap = entity.getTrapBlockPos();
        if (!trap.isPresent() || entity.level == null) {
            return;
        }
        BlockPos pos = trap.get();
        net.minecraft.block.BlockState state = entity.level.getBlockState(pos);
        if (state.isAir()) {
            return;
        }
        if (entity instanceof com.github.standobyte.jojo.client.IEntityGlowColor) {
            ((com.github.standobyte.jojo.client.IEntityGlowColor) entity).setGlowColor(
                    java.util.OptionalInt.of(com.nextalubm.rotp_nextalbum.util.BeachBoyStandUtil.beachBoyColor(
                            com.github.standobyte.jojo.power.impl.stand.IStandPower.getStandPowerOptional(mc.player).orElse(null))));
        }
        double x = MathHelper.lerp(partialTicks, entity.xOld, entity.getX());
        double y = MathHelper.lerp(partialTicks, entity.yOld, entity.getY());
        double z = MathHelper.lerp(partialTicks, entity.zOld, entity.getZ());
        matrixStack.pushPose();
        matrixStack.translate(pos.getX() - x, pos.getY() - y, pos.getZ() - z);
        IVertexBuilder builder = buffer.getBuffer(RenderType.outline(
                net.minecraft.client.renderer.texture.AtlasTexture.LOCATION_BLOCKS));
        net.minecraft.client.renderer.BlockRendererDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        dispatcher.getModelRenderer().renderModel(matrixStack.last(), builder, state, dispatcher.getBlockModel(state),
                1.0F, 1.0F, 1.0F, FULLBRIGHT, OverlayTexture.NO_OVERLAY,
                net.minecraftforge.client.model.data.EmptyModelData.INSTANCE);
        matrixStack.popPose();
    }

    private static boolean isWhippingHookedTarget(BeachBoyHookEntity hook, Entity owner) {
        if (hook == null || owner == null || hook.getHookedEntity() == null) {
            return false;
        }
        if (!(owner instanceof LivingEntity)) {
            return false;
        }
        BeachBoyStandEntity stand = com.nextalubm.rotp_nextalbum.util.BeachBoyStandUtil.getStand((LivingEntity) owner);
        return stand != null && stand.getWhipTicks() > 0;
    }

    private static boolean isRemoteCameraFor(BeachBoyHookEntity hook) {
        Entity camera = Minecraft.getInstance().getCameraEntity();
        if (camera instanceof BeachBoyStandEntity) {
            return ((BeachBoyStandEntity) camera).getUser() == hook.getOwner();
        }
        return camera == hook;
    }

    public static void renderHookFromWorld(BeachBoyHookEntity hook, float partialTicks,
            MatrixStack matrixStack, IRenderTypeBuffer buffer, int packedLight) {
        Minecraft mc = Minecraft.getInstance();
        if (hook.isTrapArmed() || hook.getHookedEntity() != null) {
            return;
        }
        if (mc.options.getCameraType() != null && mc.options.getCameraType().isFirstPerson()) {
            return;
        }
        Vector3d camera = mc.gameRenderer.getMainCamera().getPosition();
        Vector3d origin = interpolatedOrigin(hook, partialTicks);
        matrixStack.pushPose();
        matrixStack.translate(origin.x - camera.x, origin.y - camera.y, origin.z - camera.z);
        renderHookModel(hook, partialTicks, matrixStack, buffer, packedLight, HOOK_SEE_THROUGH);
        matrixStack.popPose();
    }

    private void renderHookModel(BeachBoyHookEntity entity, float partialTicks, MatrixStack matrixStack,
            IRenderTypeBuffer buffer, int packedLight) {
        if (isRemoteCameraFor(entity) || entity.isTrapArmed()) {
            return;
        }
        if (entity.getHookedEntity() != null) {
            return;
        }
        renderHookModel(entity, partialTicks, matrixStack, buffer, packedLight, HOOK_TYPE);
    }

    private static void renderHookModel(BeachBoyHookEntity entity, float partialTicks, MatrixStack matrixStack,
            IRenderTypeBuffer buffer, int packedLight, RenderType renderType) {
        GeoModel geoModel = GeckoLibCache.getInstance().getGeoModels().get(GEO_MODEL);
        if (geoModel == null) {
            return;
        }
        float yRot = MathHelper.lerp(partialTicks, entity.yRotO, entity.yRot);
        float xRot = MathHelper.lerp(partialTicks, entity.xRotO, entity.xRot);
        matrixStack.pushPose();
        matrixStack.mulPose(Vector3f.YP.rotationDegrees(-yRot));
        matrixStack.mulPose(Vector3f.XP.rotationDegrees(xRot));

        IVertexBuilder vertexBuilder = buffer.getBuffer(renderType);
        for (GeoBone bone : geoModel.topLevelBones) {
            renderBoneRecursive(bone, matrixStack, vertexBuilder, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        }
        matrixStack.popPose();
    }

    private static void renderBoneRecursive(GeoBone bone, MatrixStack stack, IVertexBuilder bufferIn,
            int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        stack.pushPose();
        RenderUtils.translate(bone, stack);
        RenderUtils.moveToPivot(bone, stack);
        RenderUtils.rotate(bone, stack);
        RenderUtils.scale(bone, stack);
        RenderUtils.moveBackFromPivot(bone, stack);
        if (!bone.isHidden() && bone.childCubes != null) {
            for (GeoCube cube : bone.childCubes) {
                if (cube == null) {
                    continue;
                }
                stack.pushPose();
                renderCube(cube, stack, bufferIn, packedLight, packedOverlay, red, green, blue, alpha);
                stack.popPose();
            }
        }
        if (!bone.childBonesAreHiddenToo()) {
            for (GeoBone child : bone.childBones) {
                renderBoneRecursive(child, stack, bufferIn, packedLight, packedOverlay, red, green, blue, alpha);
            }
        }
        stack.popPose();
    }

    private static void renderCube(GeoCube cube, MatrixStack stack, IVertexBuilder bufferIn,
            int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        RenderUtils.moveToPivot(cube, stack);
        RenderUtils.rotate(cube, stack);
        RenderUtils.moveBackFromPivot(cube, stack);
        Matrix4f matrix4f = stack.last().pose();
        Matrix3f matrix3f = stack.last().normal();
        if (cube.quads == null) {
            return;
        }
        for (GeoQuad quad : cube.quads) {
            if (quad == null) {
                continue;
            }
            Vector3f normal = quad.normal.copy();
            normal.transform(matrix3f);
            for (GeoVertex vertex : quad.vertices) {
                bufferIn.vertex(matrix4f, vertex.position.x(), vertex.position.y(), vertex.position.z())
                        .color(red, green, blue, alpha)
                        .uv(vertex.textureU, vertex.textureV)
                        .overlayCoords(packedOverlay)
                        .uv2(packedLight)
                        .normal(matrix3f, normal.x(), normal.y(), normal.z())
                        .endVertex();
            }
        }
    }

    public static void renderLineFromWorld(BeachBoyHookEntity hook, Entity owner, float partialTicks,
            MatrixStack matrixStack, IRenderTypeBuffer buffer, int packedLight) {
        if (hook.isTrapArmed()) {
            renderTrapStubFromWorld(hook, owner, partialTicks, matrixStack, buffer);
            return;
        }
        if (isWhippingHookedTarget(hook, owner)) {
            return;
        }
        Vector3d camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Vector3d origin = interpolatedOrigin(hook, partialTicks);
        matrixStack.pushPose();
        matrixStack.translate(origin.x - camera.x, origin.y - camera.y, origin.z - camera.z);
        renderLine(hook, owner, partialTicks, matrixStack, buffer, FULLBRIGHT);
        matrixStack.popPose();
    }

    private static void renderLine(BeachBoyHookEntity hook, Entity owner, float partialTicks,
            MatrixStack matrixStack, IRenderTypeBuffer buffer, int packedLight) {
        Vector3d origin = interpolatedOrigin(hook, partialTicks);
        Vector3d startWorld = getLineStartForRender(hook, owner, partialTicks);
        List<Vector3d> anchors = new ArrayList<Vector3d>(hook.getWrapPoints().size() + 2);
        anchors.add(startWorld);
        anchors.addAll(hook.getWrapPoints());
        anchors.add(hook.getLineEnd(partialTicks));
        List<Vector3d> path = BeachBoyLinePath.buildPath(
                hook.level, anchors, hook.getMaxLineLength(), hook.hasLineBlockCollision(), hook);
        if (path.size() < 2) {
            return;
        }

        Vector3d camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        MatrixStack.Entry pose = matrixStack.last();
        IVertexBuilder ribbon = buffer.getBuffer(LINE_TYPE);
        Vector3d prev = path.get(0).subtract(origin);
        for (int i = 1; i < path.size(); i++) {
            Vector3d next = path.get(i).subtract(origin);
            Vector3d segment = next.subtract(prev);
            if (segment.lengthSqr() < 1.0E-12D) {
                prev = next;
                continue;
            }
            Vector3d worldMid = origin.add(prev.add(next).scale(0.5D));
            Vector3d toCam = camera.subtract(worldMid);
            Vector3d side = segment.cross(toCam);
            if (side.lengthSqr() < 1.0E-10D) {
                side = segment.cross(new Vector3d(0.0D, 1.0D, 0.0D));
            }
            if (side.lengthSqr() < 1.0E-10D) {
                prev = next;
                continue;
            }
            side = side.normalize().scale(LINE_HALF_WIDTH);
            addRibbonQuad(ribbon, pose, prev.add(side), prev.subtract(side), next.subtract(side), next.add(side), packedLight);
            prev = next;
        }
    }

    private static Vector3d interpolatedOrigin(BeachBoyHookEntity hook, float partialTicks) {
        return new Vector3d(
                MathHelper.lerp(partialTicks, hook.xOld, hook.getX()),
                MathHelper.lerp(partialTicks, hook.yOld, hook.getY()),
                MathHelper.lerp(partialTicks, hook.zOld, hook.getZ()));
    }

    private static Vector3d getLineStartForRender(BeachBoyHookEntity hook, Entity owner, float partialTicks) {
        if (owner instanceof LivingEntity) {
            Vector3d firstPerson = BeachBoyClientRodTips.tryFirstPerson((LivingEntity) owner, partialTicks);
            if (firstPerson != null) {
                return firstPerson;
            }
            return BeachBoyHookEntity.getRodTip((LivingEntity) owner, partialTicks);
        }
        return hook != null ? hook.getLineStart(partialTicks) : owner.position();
    }

    private static void addRibbonQuad(IVertexBuilder builder, MatrixStack.Entry pose,
            Vector3d p0, Vector3d p1, Vector3d p2, Vector3d p3, int packedLight) {
        addRibbonVertex(builder, pose, p0, packedLight);
        addRibbonVertex(builder, pose, p1, packedLight);
        addRibbonVertex(builder, pose, p2, packedLight);
        addRibbonVertex(builder, pose, p3, packedLight);
        addRibbonVertex(builder, pose, p3, packedLight);
        addRibbonVertex(builder, pose, p2, packedLight);
        addRibbonVertex(builder, pose, p1, packedLight);
        addRibbonVertex(builder, pose, p0, packedLight);
    }

    public static void renderWhipFromWorld(LivingEntity user, BeachBoyStandEntity stand, float partialTicks,
            MatrixStack matrixStack, IRenderTypeBuffer buffer) {
        if (user == null || stand == null || stand.getWhipTicks() <= 0) {
            return;
        }
        float progress = 1.0F - MathHelper.clamp((stand.getWhipTicks() - partialTicks)
                / (float) com.nextalubm.rotp_nextalbum.util.BeachBoyCombatUtil.WHIP_ANIM_TICKS, 0.0F, 1.0F);
        Vector3d origin = getLineStartForRender(null, user, partialTicks);
        Vector3d look = user.getViewVector(partialTicks);
        List<Vector3d> path = com.nextalubm.rotp_nextalbum.util.BeachBoyCombatUtil.buildWhipArc(
                origin, look, stand.isWhipVertical(), stand.isWhipLeftToRight(), stand.isWhipUpward(), progress,
                com.nextalubm.rotp_nextalbum.util.BeachBoyCombatUtil.whipRange(stand.isWhipVertical()));
        renderWorldRibbon(path, matrixStack, buffer);
        if (path.size() >= 2
                && com.nextalubm.rotp_nextalbum.util.BeachBoyCombatUtil.hookedLiving(
                        com.nextalubm.rotp_nextalbum.util.BeachBoyStandUtil.findHook(user)) == null) {
            Vector3d tip = path.get(path.size() - 1);
            Vector3d prev = path.get(path.size() - 2);
            renderHookAtWorld(tip, tip.subtract(prev), matrixStack, buffer);
        }
    }

    public static void renderHookAtWorld(Vector3d worldPos, Vector3d direction, MatrixStack matrixStack,
            IRenderTypeBuffer buffer) {
        GeoModel geoModel = GeckoLibCache.getInstance().getGeoModels().get(GEO_MODEL);
        if (geoModel == null) {
            return;
        }
        Vector3d dir = direction.lengthSqr() < 1.0E-10D ? new Vector3d(0.0D, 0.0D, 1.0D) : direction.normalize();
        float yRot = (float) (MathHelper.atan2(dir.x, dir.z) * (180.0D / Math.PI));
        float xRot = (float) (MathHelper.atan2(dir.y, MathHelper.sqrt(dir.x * dir.x + dir.z * dir.z)) * (180.0D / Math.PI));
        Vector3d camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        matrixStack.pushPose();
        matrixStack.translate(worldPos.x - camera.x, worldPos.y - camera.y, worldPos.z - camera.z);
        matrixStack.mulPose(Vector3f.YP.rotationDegrees(-yRot));
        matrixStack.mulPose(Vector3f.XP.rotationDegrees(-xRot));
        IVertexBuilder vertexBuilder = buffer.getBuffer(HOOK_SEE_THROUGH);
        for (GeoBone bone : geoModel.topLevelBones) {
            renderBoneRecursive(bone, matrixStack, vertexBuilder, FULLBRIGHT, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        }
        matrixStack.popPose();
    }

    private static void renderTrapStubFromWorld(BeachBoyHookEntity hook, Entity owner, float partialTicks,
            MatrixStack matrixStack, IRenderTypeBuffer buffer) {
        Vector3d camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Vector3d origin = interpolatedOrigin(hook, partialTicks);
        matrixStack.pushPose();
        matrixStack.translate(origin.x - camera.x, origin.y - camera.y, origin.z - camera.z);
        renderTrapStub(hook, owner, partialTicks, matrixStack, buffer, FULLBRIGHT);
        matrixStack.popPose();
    }

    private static void renderTrapStub(BeachBoyHookEntity hook, Entity owner, float partialTicks,
            MatrixStack matrixStack, IRenderTypeBuffer buffer, int packedLight) {
        Vector3d origin = interpolatedOrigin(hook, partialTicks);
        Vector3d start = getLineStartForRender(hook, owner, partialTicks);
        Vector3d end = trapStubEnd(owner, start);
        List<Vector3d> path = new ArrayList<Vector3d>();
        path.add(start);
        path.add(end);
        renderRibbonRelative(path, origin, matrixStack, buffer, packedLight);
    }

    private static Vector3d trapStubEnd(Entity owner, Vector3d start) {
        Vector3d down = start.add(0.0D, -2.6D, 0.0D);
        if (owner != null && owner.level != null) {
            net.minecraft.util.math.RayTraceResult hit = owner.level.clip(new net.minecraft.util.math.RayTraceContext(
                    start, down,
                    net.minecraft.util.math.RayTraceContext.BlockMode.COLLIDER,
                    net.minecraft.util.math.RayTraceContext.FluidMode.NONE,
                    owner));
            if (hit.getType() == net.minecraft.util.math.RayTraceResult.Type.BLOCK) {
                return hit.getLocation();
            }
        }
        if (owner != null) {
            return new Vector3d(start.x, owner.getY(), start.z);
        }
        return down;
    }

    private static void renderWorldRibbon(List<Vector3d> worldPoints, MatrixStack matrixStack, IRenderTypeBuffer buffer) {
        if (worldPoints.size() < 2) {
            return;
        }
        Vector3d camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Vector3d origin = worldPoints.get(0);
        matrixStack.pushPose();
        matrixStack.translate(origin.x - camera.x, origin.y - camera.y, origin.z - camera.z);
        renderRibbonRelative(worldPoints, origin, matrixStack, buffer, FULLBRIGHT);
        matrixStack.popPose();
    }

    private static void renderRibbonRelative(List<Vector3d> worldPoints, Vector3d origin,
            MatrixStack matrixStack, IRenderTypeBuffer buffer, int packedLight) {
        Vector3d camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        MatrixStack.Entry pose = matrixStack.last();
        IVertexBuilder ribbon = buffer.getBuffer(LINE_TYPE);
        Vector3d prev = worldPoints.get(0).subtract(origin);
        for (int i = 1; i < worldPoints.size(); i++) {
            Vector3d next = worldPoints.get(i).subtract(origin);
            Vector3d segment = next.subtract(prev);
            if (segment.lengthSqr() < 1.0E-12D) {
                prev = next;
                continue;
            }
            Vector3d worldMid = origin.add(prev.add(next).scale(0.5D));
            Vector3d toCam = camera.subtract(worldMid);
            Vector3d side = segment.cross(toCam);
            if (side.lengthSqr() < 1.0E-10D) {
                side = segment.cross(new Vector3d(0.0D, 1.0D, 0.0D));
            }
            if (side.lengthSqr() < 1.0E-10D) {
                prev = next;
                continue;
            }
            side = side.normalize().scale(LINE_HALF_WIDTH);
            addRibbonQuad(ribbon, pose, prev.add(side), prev.subtract(side), next.subtract(side), next.add(side), packedLight);
            prev = next;
        }
    }

    private static void addRibbonVertex(IVertexBuilder builder, MatrixStack.Entry pose, Vector3d pos, int packedLight) {
        builder.vertex(pose.pose(), (float) pos.x, (float) pos.y, (float) pos.z)
                .color(LINE_RED, LINE_GREEN, LINE_BLUE, 255)
                .uv(0.0F, 0.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(pose.normal(), 0.0F, 1.0F, 0.0F)
                .endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(BeachBoyHookEntity entity) {
        return TEXTURE;
    }
}
