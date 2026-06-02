package com.nextalubm.rotp_nextalbum.client.render.model;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.standobyte.jojo.action.stand.StandEntityAction;
import com.github.standobyte.jojo.client.render.entity.animnew.stand.StandPoseData;
import com.github.standobyte.jojo.client.render.entity.bb.EntityModelUnbaked;
import com.github.standobyte.jojo.client.render.entity.bb.ParseGeckoModel;
import com.github.standobyte.jojo.client.render.entity.model.stand.HumanoidStandModel;
import com.github.standobyte.jojo.client.render.entity.model.stand.bb.BlockbenchStandModelHelper;
import com.github.standobyte.jojo.client.render.entity.pose.IModelPose;
import com.github.standobyte.jojo.client.render.entity.pose.ModelPose;
import com.github.standobyte.jojo.client.render.entity.pose.anim.IActionAnimation;
import com.github.standobyte.jojo.entity.stand.StandPose;
import com.github.standobyte.jojo.util.general.MathUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.entity.SexPistolsEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.resources.IResource;
import net.minecraft.util.HandSide;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;

public class SexPistolsModel extends HumanoidStandModel<SexPistolsEntity> {
    private static final ResourceLocation MODEL = new ResourceLocation(NextAlubm.MOD_ID, "geo/sp_no_1.geo.json");
    private static final ResourceLocation ANIMATION = new ResourceLocation(NextAlubm.MOD_ID, "animations/sp_no_1.animation.json");
    private static final Pattern FIRST_NUMBER = Pattern.compile("(\\d+)");
    public ModelRenderer torso2;
    public ModelRenderer group;
    public ModelRenderer group6;
    private final Map<ModelRenderer, PartTransform> defaultTransforms = new HashMap<>();
    private JsonObject animationsJson;

    public SexPistolsModel() {
        super(64, 64);
        loadBlockbenchModel();
        animationsJson = loadAnimationsJson();
        afterInit();
    }

    private void loadBlockbenchModel() {
        try (IResource resource = Minecraft.getInstance().getResourceManager().getResource(MODEL);
             Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            JsonElement json = new JsonParser().parse(reader);
            EntityModelUnbaked unbaked = ParseGeckoModel.parseGeckoModel(json, MODEL);
            BlockbenchStandModelHelper.fillFromUnbaked(unbaked, this);
        }
        catch (Exception e) {
            NextAlubm.LOGGER.error("Failed to load Sex Pistols Blockbench model", e);
        }
    }

    @Override
    public void putNamedModelPart(String name, ModelRenderer modelPart) {
        super.putNamedModelPart(name, modelPart);
        if (modelPart != null && defaultTransforms != null) {
            defaultTransforms.put(modelPart, new PartTransform(modelPart));
        }
    }
    public void translateToHeldItemHand(MatrixStack matrixStack) {
        if (group != null) {
            group.translateAndRotate(matrixStack);
        }
        else {
            translateToHand(HandSide.RIGHT, matrixStack);
        }
    }



    @Override
    public void afterInit() {
        super.afterInit();
        putNamedModelPart("torso2", torso2);
        putNamedModelPart("group", group);
        putNamedModelPart("group6", group6);
        putNamedModelPart("rightLeg", rightLeg);
    }

    @Override
    public void poseStand(SexPistolsEntity entity, StandPoseData pose, float ticks, float yRotOffsetDeg, float xRotDeg) {
        if (entity != null && entity.isSummonAnimationActive() && pose.standPose != SexPistolsEntity.KICK_POSE && pose.standPose != SexPistolsEntity.IDLE_RANDOM_POSE && pose.standPose != SexPistolsEntity.PICK_ITEM_POSE) {
            float partialTick = ticks - entity.tickCount;
            float summonTicks = MathHelper.clamp(entity.getSummonAnimationSeconds(partialTick) * 10.0F, 0.0F, 20.0F);
            super.poseStand(entity, StandPoseData.start().standPose(StandPose.SUMMON).end(), summonTicks, yRotOffsetDeg, xRotDeg);
            return;
        }
        super.poseStand(entity, pose, ticks, yRotOffsetDeg, xRotDeg);
        if (entity != null && pose.standPose == StandPose.IDLE) {
            applyRecovery(entity, ticks, yRotOffsetDeg * MathUtil.DEG_TO_RAD, xRotDeg * MathUtil.DEG_TO_RAD);
        }
    }

    @Override
    protected IModelPose<SexPistolsEntity> initBaseIdlePose() {
        return new JsonIdlePose(false, true);
    }

    @Override
    protected IModelPose<SexPistolsEntity> initIdlePose2Loop() {
        return new JsonIdlePose(true, true);
    }

    @Override
    protected List<IModelPose<SexPistolsEntity>> initSummonPoses() {
        List<String> names = animationNames(name -> name.startsWith("summon"));
        List<IModelPose<SexPistolsEntity>> poses = new ArrayList<>();
        for (String name : names) {
            poses.add(new JsonTimelinePose(name, 0.0F, false));
        }
        return poses;
    }

    @Override
    protected void initActionPoses() {
        actionAnim.put(SexPistolsEntity.KICK_POSE, new JsonActionAnimation("kick"));
        actionAnim.put(SexPistolsEntity.IDLE_RANDOM_POSE, new JsonActionAnimation("idle_random"));
        actionAnim.put(SexPistolsEntity.PICK_ITEM_POSE, new JsonActionAnimation("pick_item"));
        super.initActionPoses();
    }

    private void applyRecovery(SexPistolsEntity entity, float ticks, float yRotOffsetRad, float xRotRad) {
        float partialTick = ticks - entity.tickCount;
        float blend = entity.getRecoveryAnimationBlend(partialTick);
        if (blend <= 0.0F || entity.getRecoveryAnimationType() == SexPistolsEntity.ANIMATION_SUMMON) {
            return;
        }
        String name = animationNameForRecovery(entity);
        if (name != null) {
            new JsonTimelinePose(name, animationLength(name), false).poseModel(blend, entity, ticks, yRotOffsetRad, xRotRad, entity.getPunchingHand());
        }
    }

    private String animationNameForRecovery(SexPistolsEntity entity) {
        if (entity.getRecoveryAnimationType() == SexPistolsEntity.ANIMATION_KICK) {
            return animationNameForVariant("kick", entity.getRecoveryAnimationVariant());
        }
        if (entity.getRecoveryAnimationType() == SexPistolsEntity.ANIMATION_IDLE_RANDOM) {
            return animationNameForVariant("idle_random", entity.getRecoveryAnimationVariant());
        }
        return null;
    }

    private String animationNameForVariant(String prefix, int variant) {
        List<String> names = animationNames(name -> matchesPrefix(prefix, name));
        return names.isEmpty() ? null : names.get(Math.floorMod(variant, names.size()));
    }

    private boolean matchesPrefix(String prefix, String name) {
        if ("idle_random".equals(prefix)) {
            return "idle_random".equals(name) || name.startsWith("idle_random");
        }
        return name.startsWith(prefix);
    }

    private JsonObject getAnimation(String name) {
        JsonObject animations = getAnimationsJson();
        return animations != null && animations.has(name) ? animations.getAsJsonObject(name) : null;
    }

    private JsonObject getAnimationsJson() {
        if (animationsJson == null) {
            animationsJson = loadAnimationsJson();
        }
        return animationsJson;
    }

    private static JsonObject loadAnimationsJson() {
        try (IResource resource = Minecraft.getInstance().getResourceManager().getResource(ANIMATION);
             Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            JsonObject root = new JsonParser().parse(reader).getAsJsonObject();
            return root.getAsJsonObject("animations");
        }
        catch (Exception e) {
            NextAlubm.LOGGER.error("Failed to load Sex Pistols RotP pose animations", e);
            return null;
        }
    }

    private List<String> animationNames(Predicate<String> predicate) {
        JsonObject animations = getAnimationsJson();
        List<String> names = new ArrayList<>();
        if (animations != null) {
            for (Map.Entry<String, JsonElement> entry : animations.entrySet()) {
                if (predicate.test(entry.getKey())) {
                    names.add(entry.getKey());
                }
            }
        }
        names.sort(Comparator.comparingInt(SexPistolsModel::firstNumber).thenComparing(name -> name));
        return names;
    }

    private static int firstNumber(String name) {
        Matcher matcher = FIRST_NUMBER.matcher(name);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 1;
    }

    private String idleName(SexPistolsEntity entity) {
        if (entity != null && Math.floorMod(entity.getIdleAnimationVariant(), 2) == 1 && getAnimation("idle2") != null) {
            return "idle2";
        }
        return "idle";
    }

    private float idleLoopSeconds(String name) {
        float length = animationLength(name);
        return length > 0.0F ? length * 0.5F : 0.0F;
    }

    private float animationLength(String name) {
        JsonObject animation = getAnimation(name);
        if (animation != null && animation.has("animation_length")) {
            return animation.get("animation_length").getAsFloat();
        }
        if (name.startsWith("summon")) {
            return 2.0F;
        }
        return 0.0F;
    }

    private boolean animationLoops(String name) {
        JsonObject animation = getAnimation(name);
        if (animation == null || !animation.has("loop")) {
            return false;
        }
        JsonElement loop = animation.get("loop");
        return loop.isJsonPrimitive() && loop.getAsJsonPrimitive().isBoolean() && loop.getAsBoolean();
    }

    private float sampledSeconds(String name, float seconds) {
        float length = animationLength(name);
        if (length <= 0.0F) {
            return seconds;
        }
        if (animationLoops(name)) {
            return seconds % length;
        }
        return MathHelper.clamp(seconds, 0.0F, length);
    }

    private float[] readVectorAt(JsonElement element, float seconds) {
        if (element == null) {
            return null;
        }
        if (element.isJsonArray()) {
            return readVector(element.getAsJsonArray());
        }
        if (!element.isJsonObject()) {
            return null;
        }
        JsonObject object = element.getAsJsonObject();
        float[] direct = readKeyframeVector(object);
        if (direct != null) {
            return direct;
        }
        TimelinePoint previous = null;
        TimelinePoint next = null;
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            try {
                float time = Float.parseFloat(entry.getKey());
                float[] vector = readVectorAt(entry.getValue(), seconds);
                if (vector == null) {
                    continue;
                }
                if (time <= seconds && (previous == null || time >= previous.time)) {
                    previous = new TimelinePoint(time, vector);
                }
                if (time >= seconds && (next == null || time <= next.time)) {
                    next = new TimelinePoint(time, vector);
                }
            }
            catch (NumberFormatException e) {
                return null;
            }
        }
        if (previous == null) {
            return next != null ? next.vector : null;
        }
        if (next == null || previous.time == next.time) {
            return previous.vector;
        }
        float delta = MathHelper.clamp((seconds - previous.time) / (next.time - previous.time), 0.0F, 1.0F);
        return new float[] {
                MathHelper.lerp(delta, previous.vector[0], next.vector[0]),
                MathHelper.lerp(delta, previous.vector[1], next.vector[1]),
                MathHelper.lerp(delta, previous.vector[2], next.vector[2])
        };
    }

    private float[] readKeyframeVector(JsonObject object) {
        if (object.has("vector") && object.get("vector").isJsonArray()) {
            return readVector(object.getAsJsonArray("vector"));
        }
        if (object.has("post")) {
            JsonElement post = object.get("post");
            if (post.isJsonArray()) {
                return readVector(post.getAsJsonArray());
            }
            if (post.isJsonObject()) {
                return readKeyframeVector(post.getAsJsonObject());
            }
        }
        return null;
    }

    private static float[] readVector(JsonArray array) {
        if (array == null || array.size() < 3) {
            return null;
        }
        return new float[] { array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat() };
    }

    private PartTransform defaultTransform(ModelRenderer modelPart) {
        return defaultTransforms.computeIfAbsent(modelPart, PartTransform::new);
    }

    private class JsonIdlePose implements IModelPose<SexPistolsEntity> {
        private final boolean loopTarget;
        private final boolean headTracking;
        private UnaryOperator<Float> easing = amount -> amount;

        private JsonIdlePose(boolean loopTarget, boolean headTracking) {
            this.loopTarget = loopTarget;
            this.headTracking = headTracking;
        }

        @Override
        public void poseModel(float rotationAmount, SexPistolsEntity entity, float ticks, float yRotOffsetRad, float xRotRad, HandSide side) {
            rotationAmount = easing.apply(rotationAmount);
            String name = idleName(entity);
            float seconds = loopTarget ? idleLoopSeconds(name) : 0.0F;
            new JsonTimelinePose(name, seconds, headTracking).poseModel(rotationAmount, entity, ticks, yRotOffsetRad, xRotRad, side);
        }

        @Override
        public IModelPose<SexPistolsEntity> setEasing(UnaryOperator<Float> function) {
            this.easing = function;
            return this;
        }
    }

    private class JsonTimelinePose implements IModelPose<SexPistolsEntity> {
        private final String name;
        private final float seconds;
        private final boolean headTracking;
        private UnaryOperator<Float> easing = amount -> amount;

        private JsonTimelinePose(String name, float seconds, boolean headTracking) {
            this.name = name;
            this.seconds = seconds;
            this.headTracking = headTracking;
        }

        @Override
        public void poseModel(float rotationAmount, SexPistolsEntity entity, float ticks, float yRotOffsetRad, float xRotRad, HandSide side) {
            rotationAmount = easing.apply(rotationAmount);
            if (rotationAmount <= 0.0F) {
                return;
            }
            JsonObject animation = getAnimation(name);
            if (animation == null) {
                return;
            }
            JsonObject bones = animation.getAsJsonObject("bones");
            if (bones == null) {
                return;
            }
            float time = sampledSeconds(name, seconds);
            for (Map.Entry<String, JsonElement> entry : bones.entrySet()) {
                ModelRenderer modelPart = getModelPart(entry.getKey());
                if (modelPart == null || !entry.getValue().isJsonObject()) {
                    continue;
                }
                PartTransform defaults = defaultTransform(modelPart);
                JsonObject bone = entry.getValue().getAsJsonObject();
                float[] rotation = readVectorAt(bone.get("rotation"), time);
                if (rotation != null) {
                    modelPart.xRot = MathUtil.rotLerpRad(rotationAmount, modelPart.xRot, defaults.xRot + rotation[0] * MathUtil.DEG_TO_RAD);
                    modelPart.yRot = MathUtil.rotLerpRad(rotationAmount, modelPart.yRot, defaults.yRot + rotation[1] * MathUtil.DEG_TO_RAD);
                    modelPart.zRot = MathUtil.rotLerpRad(rotationAmount, modelPart.zRot, defaults.zRot + rotation[2] * MathUtil.DEG_TO_RAD);
                }
                float[] position = readVectorAt(bone.get("position"), time);
                if (position != null) {
                    modelPart.x = MathHelper.lerp(rotationAmount, modelPart.x, defaults.x + position[0]);
                    modelPart.y = MathHelper.lerp(rotationAmount, modelPart.y, defaults.y - position[1]);
                    modelPart.z = MathHelper.lerp(rotationAmount, modelPart.z, defaults.z + position[2]);
                }
            }
            if (headTracking) {
                final float headAmount = rotationAmount;
                headParts().forEach(part -> {
                    part.yRot = MathUtil.rotLerpRad(headAmount, part.yRot, yRotOffsetRad);
                    part.xRot = MathUtil.rotLerpRad(headAmount, part.xRot, xRotRad);
                    part.zRot = MathHelper.lerp(headAmount, part.zRot, 0.0F);
                });
            }
        }

        @Override
        public IModelPose<SexPistolsEntity> setEasing(UnaryOperator<Float> function) {
            this.easing = function;
            return this;
        }
    }

    private class JsonActionAnimation implements IActionAnimation<SexPistolsEntity> {
        private final String prefix;

        private JsonActionAnimation(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public void animate(StandEntityAction.Phase phase, float phaseCompletion, SexPistolsEntity entity, float ticks, float yRotOffsetRad, float xRotRad, HandSide side) {
            if (entity == null) {
                return;
            }
            String name = "pick_item".equals(prefix) ? "pick_item" : animationNameForVariant(prefix, "kick".equals(prefix) ? entity.getKickAnimationVariant() : entity.getIdleRandomVariant());
            if (name == null) {
                return;
            }
            float partialTick = ticks - entity.tickCount;
            float seconds;
            float blend;
            if ("kick".equals(prefix)) {
                seconds = entity.getKickAnimationSeconds(partialTick);
                blend = entity.getKickAnimationBlend(partialTick);
            }
            else {
                seconds = entity.getIdleRandomAnimationSeconds(partialTick);
                blend = entity.getIdleRandomAnimationBlend(partialTick);
            }
            float length = animationLength(name);
            if (length > 0.0F) {
                seconds = MathHelper.clamp(seconds, 0.0F, length);
            }
            new JsonTimelinePose(name, seconds, false).poseModel(Math.max(blend, 0.001F), entity, ticks, yRotOffsetRad, xRotRad, side);
        }
    }

    private static class TimelinePoint {
        private final float time;
        private final float[] vector;

        private TimelinePoint(float time, float[] vector) {
            this.time = time;
            this.vector = vector;
        }
    }

    private static class PartTransform {
        private final float x;
        private final float y;
        private final float z;
        private final float xRot;
        private final float yRot;
        private final float zRot;

        private PartTransform(ModelRenderer modelPart) {
            this.x = modelPart.x;
            this.y = modelPart.y;
            this.z = modelPart.z;
            this.xRot = modelPart.xRot;
            this.yRot = modelPart.yRot;
            this.zRot = modelPart.zRot;
        }
    }
}