package com.nextalubm.rotp_nextalbum.client;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.entity.RevolverBulletEntity;
import com.nextalubm.rotp_nextalbum.entity.SexPistolsEntity;
import com.nextalubm.rotp_nextalbum.item.RevolverItem;
import com.nextalubm.rotp_nextalbum.util.SexPistolsBulletRedirectUtil;
import com.nextalubm.rotp_nextalbum.util.SexPistolsStandUtil;
import com.nextalubm.rotp_nextalbum.util.SexPistolsTrajectoryVisionState;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.item.ExperienceBottleEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.entity.projectile.PotionEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ThrowableEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.EggItem;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.ExperienceBottleItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.LingeringPotionItem;
import net.minecraft.item.SnowballItem;
import net.minecraft.item.SplashPotionItem;
import net.minecraft.item.TridentItem;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = NextAlubm.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.FORGE)
public class SexPistolsTrajectoryPreviewRenderer {
    private static final int MAX_SIMULATION_STEPS = 120;
    private static final double MIN_MOTION_SQR = 1.0E-4D;
    private static final double DEFAULT_DRAG = 0.99D;
    private static final double MAX_RENDER_SEGMENT_LENGTH = 0.35D;
    private static final double ARROW_GRAVITY = 0.05D;
    private static final double THROWABLE_GRAVITY = 0.03D;
    private static final double POTION_GRAVITY = 0.05D;
    private static final double EXPERIENCE_BOTTLE_GRAVITY = 0.07D;
    private static final double REVOLVER_BULLET_EFFECTIVE_GRAVITY = 0.018D;
    private static final double REVOLVER_BULLET_SPEED = 5.9D;
    private static final int REVOLVER_MAX_RICOCHETS = 2;
    private static final double RICOCHET_MIN_SPEED_SQR = 0.04D;
    private static final double RICOCHET_SURFACE_OFFSET = 0.18D;
    private static final double RICOCHET_TRAVEL_BIAS = 0.06D;
    private static final float RIBBON_BASE_WIDTH = 0.045F;
    private static final float RIBBON_TAIL_WIDTH = 0.015F;
    private static final float PREVIEW_RED = 0.91F;
    private static final float PREVIEW_GREEN = 0.57F;
    private static final float PREVIEW_BLUE = 0.98F;
    private static final float PREVIEW_ALPHA = 0.42F;
    private static final float HELD_PREVIEW_WIDTH_SCALE = 1.2F;
    private static final float HELD_PREVIEW_ALPHA_SCALE = 1.0F;
    private static final float LIVE_PREVIEW_WIDTH_SCALE = 0.9F;
    private static final float LIVE_PREVIEW_ALPHA_SCALE = 0.8F;
    private static final double FIRST_PERSON_VISUAL_FORWARD_OFFSET = 0.45D;
    private static final double FIRST_PERSON_VISUAL_RIGHT_OFFSET = 0.42D;
    private static final double FIRST_PERSON_VISUAL_DOWN_OFFSET = 0.28D;

    @SubscribeEvent
    public static void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null || mc.level == null) {
            SexPistolsTrajectoryVisionState.setEnabled(false);
            return;
        }
        if (!SexPistolsTrajectoryVisionState.isEnabled()) {
            return;
        }
        if (!SexPistolsStandUtil.isSexPistolsUser(player)) {
            SexPistolsTrajectoryVisionState.setEnabled(false);
            return;
        }

        List<TrajectoryPath> paths = new ArrayList<>();
        TrajectoryPath heldPath = buildHeldAimPath(player, event.getPartialTicks());
        if (heldPath != null && heldPath.points.size() > 1) {
            paths.add(heldPath);
        }
        collectLiveProjectilePaths(player, paths);
        if (paths.isEmpty()) {
            return;
        }

        MatrixStack matrixStack = event.getMatrixStack();
        ActiveRenderInfo camera = mc.gameRenderer.getMainCamera();
        Vector3d cameraPos = camera.getPosition();

        matrixStack.pushPose();
        matrixStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableTexture();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();

        BufferBuilder buffer = Tessellator.getInstance().getBuilder();
        Matrix4f pose = matrixStack.last().pose();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        for (TrajectoryPath path : paths) {
            renderPathRibbon(buffer, pose, cameraPos, path);
        }
        Tessellator.getInstance().end();

        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
        matrixStack.popPose();
    }

    private static TrajectoryPath buildHeldAimPath(ClientPlayerEntity player, float partialTicks) {
        Hand preferredHand = player.isUsingItem() ? player.getUsedItemHand() : Hand.MAIN_HAND;
        TrajectoryPreviewSpec spec = buildSpecFromHeldItem(player, preferredHand, partialTicks);
        if (spec == null && preferredHand != Hand.OFF_HAND) {
            spec = buildSpecFromHeldItem(player, Hand.OFF_HAND, partialTicks);
        }
        if (spec == null && preferredHand != Hand.MAIN_HAND) {
            spec = buildSpecFromHeldItem(player, Hand.MAIN_HAND, partialTicks);
        }
        if (spec == null) {
            return null;
        }
        return simulatePath(player, applyFirstPersonHeldStart(player, partialTicks, spec), HELD_PREVIEW_WIDTH_SCALE, HELD_PREVIEW_ALPHA_SCALE);
    }

    private static TrajectoryPreviewSpec buildSpecFromHeldItem(ClientPlayerEntity player, Hand hand, float partialTicks) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty()) {
            return null;
        }

        Item item = stack.getItem();
        if (item instanceof RevolverItem) {
            Vector3d direction = getDirection(player.xRot, player.yRot);
            Vector3d start = player.getEyePosition(partialTicks).add(direction.scale(0.25D));
            
            
            Vector3d motion = direction.scale(REVOLVER_BULLET_SPEED);
            return new TrajectoryPreviewSpec(start, motion, REVOLVER_BULLET_EFFECTIVE_GRAVITY, DEFAULT_DRAG, REVOLVER_MAX_RICOCHETS);
        }
        if (item instanceof BowItem) {
            if (!player.isUsingItem() || player.getUsedItemHand() != hand || player.getUseItem() != stack) {
                return null;
            }
            int useTicks = stack.getUseDuration() - player.getUseItemRemainingTicks();
            float power = getBowPower(useTicks + partialTicks);
            if (power < 0.1F) {
                return null;
            }
            Vector3d direction = getDirection(player.xRot, player.yRot);
            Vector3d start = player.getEyePosition(partialTicks).add(direction.scale(0.25D));
            Vector3d motion = direction.scale(power * 3.0F);
            return new TrajectoryPreviewSpec(start, motion, ARROW_GRAVITY, DEFAULT_DRAG, 0);
        }
        if (item instanceof CrossbowItem) {
            if (!CrossbowItem.isCharged(stack) || !CrossbowItem.containsChargedProjectile(stack, Items.ARROW)) {
                return null;
            }
            Vector3d direction = getDirection(player.xRot, player.yRot);
            Vector3d start = player.getEyePosition(partialTicks).add(direction.scale(0.25D));
            Vector3d motion = direction.scale(3.15D);
            return new TrajectoryPreviewSpec(start, motion, ARROW_GRAVITY, DEFAULT_DRAG, 0);
        }
        if (item instanceof TridentItem) {
            if (!player.isUsingItem() || player.getUsedItemHand() != hand || player.getUseItem() != stack) {
                return null;
            }
            int charge = stack.getUseDuration() - player.getUseItemRemainingTicks();
            if (charge < 10) {
                return null;
            }
            Vector3d direction = getDirection(player.xRot, player.yRot);
            Vector3d start = player.getEyePosition(partialTicks).add(direction.scale(0.25D));
            Vector3d motion = direction.scale(2.5D);
            return new TrajectoryPreviewSpec(start, motion, ARROW_GRAVITY, DEFAULT_DRAG, 0);
        }
        if (item instanceof SnowballItem || item instanceof EggItem || item instanceof EnderPearlItem) {
            return buildThrowableSpec(player, partialTicks, 0.0F, 1.5D, THROWABLE_GRAVITY);
        }
        if (item instanceof SplashPotionItem || item instanceof LingeringPotionItem) {
            return buildThrowableSpec(player, partialTicks, -20.0F, 0.5D, POTION_GRAVITY);
        }
        if (item instanceof ExperienceBottleItem) {
            return buildThrowableSpec(player, partialTicks, -20.0F, 0.7D, EXPERIENCE_BOTTLE_GRAVITY);
        }
        return null;
    }

    private static void collectLiveProjectilePaths(ClientPlayerEntity player, List<TrajectoryPath> paths) {
        player.level.getEntitiesOfClass(ProjectileEntity.class, player.getBoundingBox().inflate(128.0D),
                projectile -> projectile.isAlive() && isSupportedProjectile(projectile))
                .forEach(projectile -> {
                    TrajectoryPreviewSpec spec = buildSpecFromProjectile(projectile);
                    if (spec != null) {
                        TrajectoryPath path = simulatePath(player, spec, LIVE_PREVIEW_WIDTH_SCALE, LIVE_PREVIEW_ALPHA_SCALE);
                        if (path.points.size() > 1) {
                            paths.add(path);
                        }
                    }
                });
    }

    private static boolean isSupportedProjectile(ProjectileEntity projectile) {
        return projectile instanceof RevolverBulletEntity
                || projectile instanceof AbstractArrowEntity
                || projectile instanceof ThrowableEntity;
    }

    private static TrajectoryPreviewSpec buildSpecFromProjectile(ProjectileEntity projectile) {
        Vector3d motion = projectile.getDeltaMovement();
        if (motion.lengthSqr() < MIN_MOTION_SQR) {
            return null;
        }
        double gravity = getGravityForProjectileEntity(projectile);
        int ricochets = projectile instanceof RevolverBulletEntity ? REVOLVER_MAX_RICOCHETS : 0;
        return new TrajectoryPreviewSpec(projectile.position(), motion, gravity, DEFAULT_DRAG, ricochets);
    }

    private static TrajectoryPreviewSpec buildThrowableSpec(ClientPlayerEntity player, float partialTicks, float pitchOffset, double speed, double gravity) {
        Vector3d direction = getDirection(player.xRot + pitchOffset, player.yRot);
        Vector3d start = player.getEyePosition(partialTicks).add(direction.scale(0.25D));
        Vector3d motion = direction.scale(speed);
        return new TrajectoryPreviewSpec(start, motion, gravity, DEFAULT_DRAG, 0);
    }

    private static TrajectoryPreviewSpec applyFirstPersonHeldStart(ClientPlayerEntity player, float partialTicks, TrajectoryPreviewSpec spec) {
        Minecraft mc = Minecraft.getInstance();
        if (!mc.options.getCameraType().isFirstPerson()) {
            return spec;
        }
        Vector3d eye = player.getEyePosition(partialTicks);
        Vector3d direction = getDirection(player.xRot, player.yRot);
        Vector3d right = Vector3d.directionFromRotation(0.0F, player.yRot + 90.0F).normalize();
        Vector3d up = right.cross(direction);
        if (up.lengthSqr() < 1.0E-6D) {
            up = new Vector3d(0.0D, 1.0D, 0.0D);
        }
        else {
            up = up.normalize();
        }
        Vector3d visualStart = eye.add(direction.scale(FIRST_PERSON_VISUAL_FORWARD_OFFSET))
                .add(right.scale(FIRST_PERSON_VISUAL_RIGHT_OFFSET))
                .subtract(up.scale(FIRST_PERSON_VISUAL_DOWN_OFFSET));
        return new TrajectoryPreviewSpec(visualStart, spec.motion, spec.gravity, spec.drag, spec.maxRicochets);
    }

    private static TrajectoryPath simulatePath(ClientPlayerEntity observer, TrajectoryPreviewSpec spec, float widthScale, float alphaScale) {
        TrajectoryPath path = new TrajectoryPath(widthScale, alphaScale);
        Vector3d pos = spec.start;
        Vector3d motion = spec.motion;
        int ricochets = 0;
        path.points.add(pos);

        for (int step = 0; step < MAX_SIMULATION_STEPS; step++) {
            if (motion.lengthSqr() < MIN_MOTION_SQR) {
                break;
            }

            Vector3d nextPos = pos.add(motion);
            SexPistolsEntity standHit = SexPistolsBulletRedirectUtil.findOwnSexPistolsHit(observer.level, observer, pos, nextPos);
            if (standHit != null && SexPistolsStandUtil.isSexPistolsRemoteControlState(standHit)) {
                Vector3d hitPos = standHit.getBoundingBox().getCenter();
                appendSegmentPoints(path.points, pos, hitPos);
                SexPistolsBulletRedirectUtil.RedirectResult redirect = SexPistolsBulletRedirectUtil.getRedirect(observer.level, observer, standHit, hitPos, motion);
                appendSegmentPoints(path.points, hitPos, redirect.position);
                pos = redirect.position;
                motion = redirect.motion;
                continue;
            }
            BlockRayTraceResult hit = observer.level.clip(new RayTraceContext(pos, nextPos,
                    RayTraceContext.BlockMode.COLLIDER,
                    RayTraceContext.FluidMode.NONE,
                    observer));
            if (hit.getType() != RayTraceResult.Type.MISS) {
                Vector3d hitPos = hit.getLocation();
                appendSegmentPoints(path.points, pos, hitPos);
                if (spec.maxRicochets > 0 && ricochets < spec.maxRicochets) {
                    RicochetResult ricochet = tryRicochet(observer, motion, hit);
                    if (ricochet != null) {
                        ricochets++;
                        appendSegmentPoints(path.points, hitPos, ricochet.position);
                        pos = ricochet.position;
                        motion = ricochet.motion;
                        continue;
                    }
                }
                break;
            }

            appendSegmentPoints(path.points, pos, nextPos);
            pos = nextPos;
            motion = motion.scale(spec.drag);
            motion = new Vector3d(motion.x, motion.y - spec.gravity, motion.z);
        }

        return path;
    }

    private static void appendSegmentPoints(List<Vector3d> points, Vector3d start, Vector3d end) {
        Vector3d segment = end.subtract(start);
        double length = segment.length();
        if (length < 1.0E-6D) {
            return;
        }

        int subdivisions = Math.max(1, (int) Math.ceil(length / MAX_RENDER_SEGMENT_LENGTH));
        for (int i = 1; i <= subdivisions; i++) {
            double progress = (double) i / (double) subdivisions;
            points.add(start.add(segment.scale(progress)));
        }
    }

    private static void renderPathRibbon(BufferBuilder buffer, Matrix4f pose, Vector3d cameraPos, TrajectoryPath path) {
        int segmentCount = path.points.size() - 1;
        for (int i = 0; i < segmentCount; i++) {
            Vector3d start = path.points.get(i);
            Vector3d end = path.points.get(i + 1);
            Vector3d segment = end.subtract(start);
            if (segment.lengthSqr() < 1.0E-6D) {
                continue;
            }

            float progressStart = (float) i / (float) segmentCount;
            float progressEnd = (float) (i + 1) / (float) segmentCount;
            float radiusStart = MathHelper.lerp(progressStart, RIBBON_BASE_WIDTH, RIBBON_TAIL_WIDTH) * path.widthScale;
            float radiusEnd = MathHelper.lerp(progressEnd, RIBBON_BASE_WIDTH, RIBBON_TAIL_WIDTH) * path.widthScale;
            float alphaStart = PREVIEW_ALPHA * (1.0F - progressStart * 0.7F) * path.alphaScale;
            float alphaEnd = PREVIEW_ALPHA * (1.0F - progressEnd * 0.7F) * path.alphaScale;
            renderTubeSegment(buffer, pose, start, end, radiusStart, radiusEnd, alphaStart, alphaEnd);
        }
    }

    private static void renderTubeSegment(BufferBuilder buffer, Matrix4f pose, Vector3d start, Vector3d end, float radiusStart, float radiusEnd, float alphaStart, float alphaEnd) {
        int sides = 6;
        Vector3d direction = end.subtract(start).normalize();
        Vector3d axisA = direction.cross(new Vector3d(0.0D, 1.0D, 0.0D));
        if (axisA.lengthSqr() < 1.0E-6D) {
            axisA = direction.cross(new Vector3d(1.0D, 0.0D, 0.0D));
        }
        if (axisA.lengthSqr() < 1.0E-6D) {
            axisA = new Vector3d(0.0D, 0.0D, 1.0D);
        }
        axisA = axisA.normalize();
        Vector3d axisB = direction.cross(axisA).normalize();
        for (int side = 0; side < sides; side++) {
            double angle0 = Math.PI * 2.0D * (double) side / (double) sides;
            double angle1 = Math.PI * 2.0D * (double) (side + 1) / (double) sides;
            Vector3d normal0 = axisA.scale(Math.cos(angle0)).add(axisB.scale(Math.sin(angle0)));
            Vector3d normal1 = axisA.scale(Math.cos(angle1)).add(axisB.scale(Math.sin(angle1)));
            Vector3d start0 = start.add(normal0.scale(radiusStart));
            Vector3d start1 = start.add(normal1.scale(radiusStart));
            Vector3d end1 = end.add(normal1.scale(radiusEnd));
            Vector3d end0 = end.add(normal0.scale(radiusEnd));
            addVertex(buffer, pose, start0, PREVIEW_RED, PREVIEW_GREEN, PREVIEW_BLUE, alphaStart);
            addVertex(buffer, pose, start1, PREVIEW_RED, PREVIEW_GREEN, PREVIEW_BLUE, alphaStart);
            addVertex(buffer, pose, end1, PREVIEW_RED, PREVIEW_GREEN, PREVIEW_BLUE, alphaEnd);
            addVertex(buffer, pose, end0, PREVIEW_RED, PREVIEW_GREEN, PREVIEW_BLUE, alphaEnd);
        }
    }

    private static void addVertex(BufferBuilder buffer, Matrix4f pose, Vector3d pos, float red, float green, float blue, float alpha) {
        buffer.vertex(pose, (float) pos.x, (float) pos.y, (float) pos.z).color(red, green, blue, alpha).endVertex();
    }

    private static Vector3d getDirection(float pitch, float yaw) {
        return Vector3d.directionFromRotation(pitch, yaw).normalize();
    }

    private static float getBowPower(float useTicks) {
        float power = useTicks / 20.0F;
        power = (power * power + power * 2.0F) / 3.0F;
        if (power > 1.0F) {
            power = 1.0F;
        }
        return power;
    }

    private static double getGravityForProjectileEntity(ProjectileEntity projectile) {
        if (projectile instanceof RevolverBulletEntity) {
            return REVOLVER_BULLET_EFFECTIVE_GRAVITY;
        }
        if (projectile instanceof ExperienceBottleEntity) {
            return EXPERIENCE_BOTTLE_GRAVITY;
        }
        if (projectile instanceof PotionEntity) {
            return POTION_GRAVITY;
        }
        if (projectile instanceof AbstractArrowEntity) {
            return ARROW_GRAVITY;
        }
        if (projectile instanceof ThrowableEntity) {
            return THROWABLE_GRAVITY;
        }
        return THROWABLE_GRAVITY;
    }

    private static RicochetResult tryRicochet(ClientPlayerEntity observer, Vector3d motion, BlockRayTraceResult hit) {
        BlockPos pos = hit.getBlockPos();
        BlockState state = observer.level.getBlockState(pos);
        float hardness = state.getDestroySpeed(observer.level, pos);
        ImpactTier tier = classifyImpact(state, hardness);
        if (tier == ImpactTier.FORCED_RICOCHET) {
            return ricochetFrom(hit, motion, 0.88D);
        }
        if (tier == ImpactTier.HARD_SURFACE && shouldRicochetOnHardSurface(state, motion, hit.getDirection())) {
            return ricochetFrom(hit, motion, 0.72D);
        }
        return null;
    }

    private static RicochetResult ricochetFrom(BlockRayTraceResult hit, Vector3d motion, double speedScale) {
        Vector3d bounced = reflect(motion, hit.getDirection()).scale(speedScale);
        if (bounced.lengthSqr() < RICOCHET_MIN_SPEED_SQR) {
            return null;
        }
        Vector3d normal = Vector3d.atLowerCornerOf(hit.getDirection().getNormal()).scale(RICOCHET_SURFACE_OFFSET);
        Vector3d travelBias = bounced.normalize().scale(RICOCHET_TRAVEL_BIAS);
        Vector3d ricochetPos = hit.getLocation().add(normal).add(travelBias);
        return new RicochetResult(ricochetPos, bounced);
    }

    private static Vector3d reflect(Vector3d motion, Direction face) {
        Vector3d normal = Vector3d.atLowerCornerOf(face.getNormal()).normalize();
        double dot = motion.dot(normal);
        return motion.subtract(normal.scale(2.0D * dot)).multiply(0.95D, 0.95D, 0.95D);
    }

    private static boolean shouldRicochetOnHardSurface(BlockState state, Vector3d motion, Direction face) {
        Vector3d normal = Vector3d.atLowerCornerOf(face.getNormal());
        double motionLengthSqr = motion.lengthSqr();
        if (motionLengthSqr < 1.0E-6D) {
            return false;
        }
        double dot = Math.abs(motion.scale(1.0D / Math.sqrt(motionLengthSqr)).dot(normal));
        Material material = state.getMaterial();
        double threshold = material == Material.METAL || material == Material.HEAVY_METAL ? 0.42D : 0.36D;
        return dot < threshold;
    }

    private static ImpactTier classifyImpact(BlockState state, float hardness) {
        Material material = state.getMaterial();
        if (hardness < 0.0F || isAlwaysRicochetBlock(state)) {
            return ImpactTier.FORCED_RICOCHET;
        }
        if (isFragileMaterial(material) || (!isHardMaterial(material) && hardness >= 0.0F && hardness <= 0.35F)) {
            return ImpactTier.FRAGILE_BREAK;
        }
        if (isHardMaterial(material)) {
            return ImpactTier.HARD_SURFACE;
        }
        if (isSoftBreakMaterial(material)) {
            return ImpactTier.SOFT_BREAK;
        }
        if (hardness <= 1.2F) {
            return ImpactTier.SOFT_BREAK;
        }
        if (hardness <= 2.4F && !material.isLiquid()) {
            return ImpactTier.SOFT_BREAK;
        }
        return ImpactTier.HARD_SURFACE;
    }

    private static boolean isFragileMaterial(Material material) {
        return material == Material.GLASS
                || material == Material.BUILDABLE_GLASS
                || material == Material.LEAVES
                || material == Material.PLANT
                || material == Material.WATER_PLANT
                || material == Material.REPLACEABLE_PLANT
                || material == Material.REPLACEABLE_FIREPROOF_PLANT
                || material == Material.REPLACEABLE_WATER_PLANT
                || material == Material.TOP_SNOW
                || material == Material.SNOW
                || material == Material.DECORATION
                || material == Material.CLOTH_DECORATION
                || material == Material.WEB
                || material == Material.ICE
                || material == Material.ICE_SOLID;
    }

    private static boolean isSoftBreakMaterial(Material material) {
        return material == Material.DIRT
                || material == Material.GRASS
                || material == Material.CLAY
                || material == Material.SAND
                || material == Material.WOOD
                || material == Material.NETHER_WOOD
                || material == Material.BAMBOO
                || material == Material.BAMBOO_SAPLING
                || material == Material.WOOL
                || material == Material.CACTUS
                || material == Material.SPONGE
                || material == Material.VEGETABLE
                || material == Material.CAKE
                || material == Material.EGG
                || material == Material.CORAL
                || material == Material.EXPLOSIVE;
    }

    private static boolean isHardMaterial(Material material) {
        return material == Material.STONE
                || material == Material.METAL
                || material == Material.HEAVY_METAL
                || material == Material.PISTON
                || material == Material.SHULKER_SHELL;
    }

    private static boolean isAlwaysRicochetBlock(BlockState state) {
        Block block = state.getBlock();
        return block == Blocks.BEDROCK
                || block == Blocks.OBSIDIAN
                || block == Blocks.CRYING_OBSIDIAN
                || block == Blocks.DIAMOND_BLOCK
                || block == Blocks.NETHERITE_BLOCK
                || block == Blocks.ANCIENT_DEBRIS;
    }

    private enum ImpactTier {
        FRAGILE_BREAK,
        SOFT_BREAK,
        HARD_SURFACE,
        FORCED_RICOCHET
    }

    private static class TrajectoryPreviewSpec {
        private final Vector3d start;
        private final Vector3d motion;
        private final double gravity;
        private final double drag;
        private final int maxRicochets;

        private TrajectoryPreviewSpec(Vector3d start, Vector3d motion, double gravity, double drag, int maxRicochets) {
            this.start = start;
            this.motion = motion;
            this.gravity = gravity;
            this.drag = drag;
            this.maxRicochets = maxRicochets;
        }
    }

    private static class TrajectoryPath {
        private final List<Vector3d> points = new ArrayList<>();
        private final float widthScale;
        private final float alphaScale;
        private TrajectoryPath(float widthScale, float alphaScale) {
            this.widthScale = widthScale;
            this.alphaScale = alphaScale;
        }

    }

    private static class RicochetResult {
        private final Vector3d position;
        private final Vector3d motion;

        private RicochetResult(Vector3d position, Vector3d motion) {
            this.position = position;
            this.motion = motion;
        }
    }
}