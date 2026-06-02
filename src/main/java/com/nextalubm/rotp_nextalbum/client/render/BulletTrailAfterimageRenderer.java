package com.nextalubm.rotp_nextalbum.client.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.client.SexPistolsSkinHelper;
import com.nextalubm.rotp_nextalbum.entity.RevolverBulletEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = NextAlubm.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.FORGE)
public final class BulletTrailAfterimageRenderer {
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final int AFTERIMAGE_LIFETIME = 8;
    private static final float TRAIL_HALF_W = 0.14F;
    private static final Map<Integer, TrailSnapshot> TRACKED_BULLETS = new HashMap<>();
    private static final List<TrailAfterimage> AFTERIMAGES = new ArrayList<>();
    private static ClientWorld trackedWorld;
    private static int clientTick;

    private BulletTrailAfterimageRenderer() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            clear();
            return;
        }
        if (trackedWorld != mc.level) {
            clear();
            trackedWorld = mc.level;
        }
        clientTick++;
        Set<Integer> seen = new HashSet<>();
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof RevolverBulletEntity) || !entity.isAlive()) {
                continue;
            }
            RevolverBulletEntity bullet = (RevolverBulletEntity) entity;
            TrailSnapshot snapshot = captureSnapshot(bullet);
            if (snapshot != null) {
                TRACKED_BULLETS.put(bullet.getId(), snapshot);
                seen.add(bullet.getId());
            }
        }
        Iterator<Map.Entry<Integer, TrailSnapshot>> iterator = TRACKED_BULLETS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, TrailSnapshot> entry = iterator.next();
            if (!seen.contains(entry.getKey())) {
                TrailSnapshot snapshot = entry.getValue();
                if (clientTick - snapshot.lastSeenTick <= 2 && snapshot.points.size() > 1) {
                    AFTERIMAGES.add(new TrailAfterimage(snapshot.points, snapshot.speed, snapshot.piercing, snapshot.texture, clientTick));
                }
                iterator.remove();
            }
        }
        AFTERIMAGES.removeIf(afterimage -> clientTick - afterimage.createdTick > AFTERIMAGE_LIFETIME);
    }

    @SubscribeEvent
    public static void onRenderWorldLast(RenderWorldLastEvent event) {
        if (AFTERIMAGES.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            clear();
            return;
        }
        ActiveRenderInfo camera = mc.gameRenderer.getMainCamera();
        Vector3d cameraPos = camera.getPosition();
        MatrixStack matrixStack = event.getMatrixStack();
        matrixStack.pushPose();
        matrixStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        MatrixStack.Entry pose = matrixStack.last();
        IRenderTypeBuffer.Impl buffer = mc.renderBuffers().bufferSource();
        Iterator<TrailAfterimage> iterator = AFTERIMAGES.iterator();
        while (iterator.hasNext()) {
            TrailAfterimage afterimage = iterator.next();
            float age = (float) (clientTick - afterimage.createdTick) + event.getPartialTicks();
            float fade = 1.0F - MathHelper.clamp(age / (float) AFTERIMAGE_LIFETIME, 0.0F, 1.0F);
            if (fade <= 0.0F) {
                iterator.remove();
                continue;
            }
            IVertexBuilder builder = buffer.getBuffer(RenderType.entityTranslucent(afterimage.texture));
            renderAfterimage(builder, pose, cameraPos, afterimage, fade * fade);
        }
        buffer.endBatch();
        matrixStack.popPose();
    }

    private static TrailSnapshot captureSnapshot(RevolverBulletEntity bullet) {
        List<RevolverBulletEntity.TrailPoint> trail = bullet.getTrailPoints(1.0F);
        if (trail.size() < 2) {
            return null;
        }
        List<Vector3d> points = new ArrayList<>();
        double speed = bullet.getDeltaMovement().length();
        for (RevolverBulletEntity.TrailPoint point : trail) {
            points.add(point.position);
            speed = Math.max(speed, point.speed);
        }
        Vector3d current = bullet.position();
        if (points.get(points.size() - 1).distanceToSqr(current) > 1.0E-6D) {
            points.add(current);
        }
        return new TrailSnapshot(points, Math.max(speed, 1.2D), bullet.isPiercingShotTrail(), SexPistolsSkinHelper.getTrailTexture(bullet.getSexPistolsStandSkin()), clientTick);
    }

    private static void renderAfterimage(IVertexBuilder builder, MatrixStack.Entry pose, Vector3d cameraPos, TrailAfterimage afterimage, float fade) {
        List<Vector3d> points = afterimage.points;
        float speedFactor = MathHelper.clamp((float) ((afterimage.speed - 0.15D) / 5.4D), 0.0F, 1.0F);
        float baseWidth = TRAIL_HALF_W * (0.95F + speedFactor * 2.1F) * (afterimage.piercing ? 1.55F : 1.0F);
        int count = points.size();
        for (int i = 1; i < count; i++) {
            Vector3d fromWorld = points.get(i - 1);
            Vector3d toWorld = points.get(i);
            Vector3d segment = toWorld.subtract(fromWorld);
            if (segment.lengthSqr() < 1.0E-7D) {
                continue;
            }
            Vector3d direction = segment.normalize();
            Vector3d toCamera = cameraPos.subtract(toWorld);
            if (toCamera.lengthSqr() < 1.0E-10D) {
                continue;
            }
            toCamera = toCamera.normalize();
            Vector3d side = direction.cross(toCamera);
            if (side.lengthSqr() < 1.0E-10D) {
                side = direction.cross(new Vector3d(0.0D, 1.0D, 0.0D));
            }
            if (side.lengthSqr() < 1.0E-10D) {
                side = direction.cross(new Vector3d(1.0D, 0.0D, 0.0D));
            }
            if (side.lengthSqr() < 1.0E-10D) {
                continue;
            }
            side = side.normalize();
            float fromProgress = (float) (i - 1) / (float) (count - 1);
            float toProgress = (float) i / (float) (count - 1);
            float fromFade = (0.18F + 0.82F * fromProgress) * fade;
            float toFade = (0.18F + 0.82F * toProgress) * fade;
            renderTexturedSegment(builder, pose, fromWorld, toWorld, side, baseWidth * fromFade, baseWidth * toFade,
                    fromProgress, toProgress, (int) (235.0F * fromFade), (int) (255.0F * toFade));
        }
    }

    private static void renderTexturedSegment(IVertexBuilder builder, MatrixStack.Entry pose,
            Vector3d from, Vector3d to, Vector3d side, float fromHalfWidth, float toHalfWidth,
            float fromU, float toU, int fromAlpha, int toAlpha) {
        if (fromAlpha <= 0 && toAlpha <= 0) {
            return;
        }
        Vector3d sideFrom = side.scale(fromHalfWidth);
        Vector3d sideTo = side.scale(toHalfWidth);
        Vector3d p0 = from.add(sideFrom);
        Vector3d p1 = from.subtract(sideFrom);
        Vector3d p2 = to.subtract(sideTo);
        Vector3d p3 = to.add(sideTo);
        addVertex(builder, pose, p0, fromU, 0.0F, fromAlpha);
        addVertex(builder, pose, p1, fromU, 1.0F, fromAlpha);
        addVertex(builder, pose, p2, toU, 1.0F, toAlpha);
        addVertex(builder, pose, p3, toU, 0.0F, toAlpha);
        addVertex(builder, pose, p3, toU, 0.0F, toAlpha);
        addVertex(builder, pose, p2, toU, 1.0F, toAlpha);
        addVertex(builder, pose, p1, fromU, 1.0F, fromAlpha);
        addVertex(builder, pose, p0, fromU, 0.0F, fromAlpha);
    }

    private static void addVertex(IVertexBuilder builder, MatrixStack.Entry pose,
            Vector3d pos, float u, float v, int alpha) {
        builder.vertex(pose.pose(), (float) pos.x, (float) pos.y, (float) pos.z)
                .color(255, 255, 255, MathHelper.clamp(alpha, 0, 255))
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(FULL_BRIGHT)
                .normal(pose.normal(), 0.0F, 1.0F, 0.0F)
                .endVertex();
    }

    private static void clear() {
        TRACKED_BULLETS.clear();
        AFTERIMAGES.clear();
        trackedWorld = null;
        clientTick = 0;
    }

    private static class TrailSnapshot {
        private final List<Vector3d> points;
        private final double speed;
        private final boolean piercing;
        private final ResourceLocation texture;
        private final int lastSeenTick;

        private TrailSnapshot(List<Vector3d> points, double speed, boolean piercing, ResourceLocation texture, int lastSeenTick) {
            this.points = points;
            this.speed = speed;
            this.piercing = piercing;
            this.texture = texture;
            this.lastSeenTick = lastSeenTick;
        }
    }

    private static class TrailAfterimage {
        private final List<Vector3d> points;
        private final double speed;
        private final boolean piercing;
        private final ResourceLocation texture;
        private final int createdTick;

        private TrailAfterimage(List<Vector3d> points, double speed, boolean piercing, ResourceLocation texture, int createdTick) {
            this.points = points;
            this.speed = speed;
            this.piercing = piercing;
            this.texture = texture;
            this.createdTick = createdTick;
        }
    }
}