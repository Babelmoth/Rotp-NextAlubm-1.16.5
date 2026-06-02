package com.nextalubm.rotp_nextalbum.client.render;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.client.SexPistolsSkinHelper;
import com.nextalubm.rotp_nextalbum.entity.RevolverBulletEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;

public final class BulletLightTrailRenderer {
    private static final ResourceLocation TRAIL_TEXTURE = new ResourceLocation(NextAlubm.MOD_ID, "textures/entity/projectiles/bullet_trail.png");
    private static final float TRAIL_HALF_W = 0.14F;
    private static final int FULL_BRIGHT = 0xF000F0;

    private BulletLightTrailRenderer() {
    }

    public static void render(MatrixStack matrixStack, IRenderTypeBuffer buffer, RevolverBulletEntity entity, float partialTicks) {
        Vector3d current = entity.tickCount <= 2 ? entity.position() : new Vector3d(
                MathHelper.lerp(partialTicks, entity.xOld, entity.getX()),
                MathHelper.lerp(partialTicks, entity.yOld, entity.getY()),
                MathHelper.lerp(partialTicks, entity.zOld, entity.getZ()));
        List<RevolverBulletEntity.TrailPoint> history = entity.getTrailPoints(partialTicks);
        Vector3d motion = entity.getDeltaMovement();
        double speed = motion.length();
        if (speed < 0.04D) {
            if (history.size() < 2) {
                return;
            }
            motion = getHistoricalMotion(current, history);
            speed = Math.max(1.2D, getHistoricalSpeed(history));
            if (motion.lengthSqr() < 1.0E-7D) {
                return;
            }
        }
        Vector3d motionDir = motion.normalize();

        List<Vector3d> points = new ArrayList<>();
        boolean piercingShot = entity.isPiercingShotTrail();
        int trailLifetime = piercingShot ? 140 : 100;
        double maxTrailDistance = piercingShot ? MathHelper.clamp(speed * 34.0D, 72.0D, 220.0D) : MathHelper.clamp(speed * 28.0D, 48.0D, 160.0D);
        for (RevolverBulletEntity.TrailPoint point : history) {
            Vector3d toCurrent = current.subtract(point.position);
            if (toCurrent.lengthSqr() <= 1.0E-8D || toCurrent.lengthSqr() > maxTrailDistance * maxTrailDistance) {
                continue;
            }
            if (toCurrent.normalize().dot(motionDir) < -0.08D) {
                continue;
            }
            points.add(point.position);
        }
        points.add(current);
        if (points.size() < 2) {
            Vector3d fallback = speed > 1.0E-4D ? motion.normalize().scale(-MathHelper.clamp(speed * 4.8D, 2.0D, 14.0D)) : Vector3d.ZERO;
            if (fallback.lengthSqr() < 1.0E-6D) {
                return;
            }
            points.add(0, current.add(fallback));
        }

        Vector3d cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        matrixStack.pushPose();
        MatrixStack.Entry pose = matrixStack.last();
        IVertexBuilder builder = buffer.getBuffer(RenderType.entityTranslucent(SexPistolsSkinHelper.getTrailTexture(entity.getSexPistolsStandSkin())));

        float speedFactor = MathHelper.clamp((float) ((speed - 0.15D) / 5.4D), 0.0F, 1.0F);
        float baseWidth = TRAIL_HALF_W * (0.95F + speedFactor * 2.1F) * (piercingShot ? 1.55F : 1.0F);
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
            float fromAgeFade = getAgeFade(entity.tickCount, history, points, fromWorld, trailLifetime);
            float toAgeFade = getAgeFade(entity.tickCount, history, points, toWorld, trailLifetime);
            float fromFade = (0.18F + 0.82F * fromProgress) * fromAgeFade;
            float toFade = (0.18F + 0.82F * toProgress) * toAgeFade;
            Vector3d from = fromWorld.subtract(current);
            Vector3d to = toWorld.subtract(current);
            renderTexturedSegment(builder, pose, from, to, side, baseWidth * fromFade, baseWidth * toFade,
                    fromProgress, toProgress, (int) (235.0F * fromFade), (int) (255.0F * toFade));
        }
        matrixStack.popPose();
    }

    private static Vector3d getHistoricalMotion(Vector3d current, List<RevolverBulletEntity.TrailPoint> history) {
        Vector3d motion = current.subtract(history.get(0).position);
        if (motion.lengthSqr() > 1.0E-7D) {
            return motion;
        }
        return history.get(history.size() - 1).position.subtract(history.get(0).position);
    }

    private static double getHistoricalSpeed(List<RevolverBulletEntity.TrailPoint> history) {
        double speed = 0.0D;
        for (RevolverBulletEntity.TrailPoint point : history) {
            speed = Math.max(speed, point.speed);
        }
        return speed;
    }

    private static float getAgeFade(int currentTick, List<RevolverBulletEntity.TrailPoint> history, List<Vector3d> points, Vector3d point, int trailLifetime) {
        if (!points.isEmpty() && point == points.get(points.size() - 1)) {
            return 1.0F;
        }
        int pointTick = currentTick;
        for (RevolverBulletEntity.TrailPoint trailPoint : history) {
            if (trailPoint.position.distanceToSqr(point) < 1.0E-8D) {
                pointTick = trailPoint.tick;
                break;
            }
        }
        float age = MathHelper.clamp((float) (currentTick - pointTick) / (float) Math.max(1, trailLifetime), 0.0F, 1.0F);
        float fade = 1.0F - age;
        return fade * fade;
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
}