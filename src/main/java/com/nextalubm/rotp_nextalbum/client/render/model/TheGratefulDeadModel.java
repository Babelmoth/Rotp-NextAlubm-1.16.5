package com.nextalubm.rotp_nextalbum.client.render.model;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

import com.github.standobyte.jojo.action.stand.StandEntityAction;
import com.github.standobyte.jojo.client.render.entity.bb.EntityModelUnbaked;
import com.github.standobyte.jojo.client.render.entity.bb.ParseGeckoModel;
import com.github.standobyte.jojo.client.render.entity.model.stand.HumanoidStandModel;
import com.github.standobyte.jojo.client.render.entity.model.stand.StandEntityModel.VisibilityMode;
import com.github.standobyte.jojo.client.render.entity.model.stand.bb.BlockbenchStandModelHelper;
import com.github.standobyte.jojo.client.render.entity.pose.IModelPose;
import com.github.standobyte.jojo.client.render.entity.pose.anim.IActionAnimation;
import com.github.standobyte.jojo.client.render.entity.pose.anim.barrage.StandTwoHandedBarrageAnimation;
import com.github.standobyte.jojo.entity.stand.StandPose;
import com.github.standobyte.jojo.power.impl.stand.StandInstance.StandPart;
import com.github.standobyte.jojo.util.general.MathUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.entity.TheGratefulDeadEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.resources.IResource;
import net.minecraft.util.HandSide;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;

public class TheGratefulDeadModel extends HumanoidStandModel<TheGratefulDeadEntity> {
    private static final ResourceLocation MODEL = new ResourceLocation(NextAlubm.MOD_ID, "geo/the_grateful_dead.geo.json");
    private static final ResourceLocation ANIMATION = new ResourceLocation(NextAlubm.MOD_ID, "animations/the_grateful_dead.animation.json");
    private static final String IDLE_NAME = "idle";
    private static final String JAB_NAME = "jab";
    private static final String STRONG_PUNCH_NAME = "strongPunch";
    private static final String BARRAGE_NAME = "barrage";
    private static final String BLOCK_NAME = "block";
    private static final String LOWER_TORSO_NAME = "lower_torso";

    private final Map<ModelRenderer, PartTransform> defaultTransforms = new HashMap<>();
    private JsonObject animationsJson;

    public TheGratefulDeadModel() {
        super(128, 128);
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
            NextAlubm.LOGGER.error("Failed to load The Grateful Dead Blockbench model", e);
        }
    }

    private static JsonObject loadAnimationsJson() {
        try (IResource resource = Minecraft.getInstance().getResourceManager().getResource(ANIMATION);
             Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            JsonObject root = new JsonParser().parse(reader).getAsJsonObject();
            return root.getAsJsonObject("animations");
        }
        catch (Exception e) {
            NextAlubm.LOGGER.error("Failed to load The Grateful Dead pose animations", e);
            return null;
        }
    }

    @Override
    public void putNamedModelPart(String name, ModelRenderer modelPart) {
        super.putNamedModelPart(name, modelPart);
        if (modelPart != null && defaultTransforms != null) {
            defaultTransforms.put(modelPart, new PartTransform(modelPart));
        }
    }

    @Override
    protected IModelPose<TheGratefulDeadEntity> initBaseIdlePose() {
        return new JsonIdlePose(false);
    }

    @Override
    protected IModelPose<TheGratefulDeadEntity> initIdlePose2Loop() {
        return new JsonIdlePose(true);
    }

    @Override
    protected void initActionPoses() {
        super.initActionPoses();
        actionAnim.put(StandPose.LIGHT_ATTACK, new JabActionAnim());
        actionAnim.put(StandPose.HEAVY_ATTACK, new OneShotActionAnim(STRONG_PUNCH_NAME, 0.91F, 1.0F));
        actionAnim.put(StandPose.HEAVY_ATTACK_FINISHER, new OneShotActionAnim(STRONG_PUNCH_NAME, 0.91F, 1.0F));
        actionAnim.put(StandPose.BLOCK, new StaticPoseAnim(BLOCK_NAME));
        actionAnim.put(StandPose.BARRAGE, new RightHandOnlyBarrageAnimation(this, new JsonBarrageLoopPose(), idlePose));
    }

    @Override
    protected void partMissing(StandPart standPart) {
        super.partMissing(standPart);
        if (standPart == StandPart.MAIN_BODY) {
            ModelRenderer lt = getModelPart(LOWER_TORSO_NAME);
            if (lt != null) {
                lt.visible = false;
            }
        }
    }

    @Override
    public void updatePartsVisibility(VisibilityMode mode) {
        super.updatePartsVisibility(mode);
        ModelRenderer lt = getModelPart(LOWER_TORSO_NAME);
        if (lt != null && torso != null) {
            lt.visible = torso.visible;
        }
    }


    private void trackArmsXRot(float xRotRad) {
        if (leftArm != null) {
            setSecondXRot(leftArm, xRotRad);
        }
        if (rightArm != null) {
            setSecondXRot(rightArm, xRotRad);
        }
    }

    private JsonObject getAnimation(String name) {
        if (animationsJson == null) {
            animationsJson = loadAnimationsJson();
        }
        return animationsJson != null && animationsJson.has(name) ? animationsJson.getAsJsonObject(name) : null;
    }

    private float animationLength(String name) {
        JsonObject animation = getAnimation(name);
        if (animation != null && animation.has("animation_length")) {
            return animation.get("animation_length").getAsFloat();
        }
        return 0.0F;
    }

    private boolean animationLoops(String name) {
        JsonObject animation = getAnimation(name);
        if (animation == null || !animation.has("loop")) {
            return false;
        }
        JsonElement loop = animation.get("loop");
        if (!loop.isJsonPrimitive()) {
            return false;
        }
        if (loop.getAsJsonPrimitive().isBoolean()) {
            return loop.getAsBoolean();
        }
        return false;
    }

    private float sampledSeconds(String name, float seconds) {
        float length = animationLength(name);
        if (length <= 0.0F) {
            return seconds;
        }
        if (animationLoops(name)) {
            return ((seconds % length) + length) % length;
        }
        return MathHelper.clamp(seconds, 0.0F, length);
    }

    private PartTransform defaultTransform(ModelRenderer modelPart) {
        return defaultTransforms.computeIfAbsent(modelPart, PartTransform::new);
    }

    private void applyJsonPose(String name, float seconds, float blend, float yRotOffsetRad, float xRotRad, boolean headTracking) {
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
                modelPart.xRot = MathUtil.rotLerpRad(blend, modelPart.xRot, defaults.xRot + rotation[0] * MathUtil.DEG_TO_RAD);
                modelPart.yRot = MathUtil.rotLerpRad(blend, modelPart.yRot, defaults.yRot + rotation[1] * MathUtil.DEG_TO_RAD);
                modelPart.zRot = MathUtil.rotLerpRad(blend, modelPart.zRot, defaults.zRot + rotation[2] * MathUtil.DEG_TO_RAD);
            }
            float[] position = readVectorAt(bone.get("position"), time);
            if (position != null) {
                modelPart.x = MathHelper.lerp(blend, modelPart.x, defaults.x + position[0]);
                modelPart.y = MathHelper.lerp(blend, modelPart.y, defaults.y - position[1]);
                modelPart.z = MathHelper.lerp(blend, modelPart.z, defaults.z + position[2]);
            }
        }
        if (headTracking) {
            final float headAmount = blend;
            headParts().forEach(part -> {
                part.yRot = MathUtil.rotLerpRad(headAmount, part.yRot, yRotOffsetRad);
                part.xRot = MathUtil.rotLerpRad(headAmount, part.xRot, xRotRad);
                part.zRot = MathHelper.lerp(headAmount, part.zRot, 0.0F);
            });
        }
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

    private class JsonIdlePose implements IModelPose<TheGratefulDeadEntity> {
        private final boolean loopTarget;
        private UnaryOperator<Float> easing = amount -> amount;

        private JsonIdlePose(boolean loopTarget) {
            this.loopTarget = loopTarget;
        }

        @Override
        public void poseModel(float rotationAmount, TheGratefulDeadEntity entity, float ticks, float yRotOffsetRad, float xRotRad, HandSide side) {
            rotationAmount = easing.apply(rotationAmount);
            if (rotationAmount <= 0.0F) {
                return;
            }
            float length = animationLength(IDLE_NAME);
            float seconds = loopTarget && length > 0.0F ? length * 0.5F : 0.0F;
            applyJsonPose(IDLE_NAME, seconds, rotationAmount, yRotOffsetRad, xRotRad, true);
        }

        @Override
        public IModelPose<TheGratefulDeadEntity> setEasing(UnaryOperator<Float> function) {
            this.easing = function;
            return this;
        }
    }

    private class JsonBarrageLoopPose implements IModelPose<TheGratefulDeadEntity> {
        private UnaryOperator<Float> easing = amount -> amount;

        @Override
        public void poseModel(float rotationAmount, TheGratefulDeadEntity entity, float ticks, float yRotOffsetRad, float xRotRad, HandSide side) {
            float length = animationLength(BARRAGE_NAME);
            if (length <= 0.0F) {
                return;
            }
            float t = MathHelper.clamp(easing.apply(rotationAmount), 0.0F, 0.999999F);
            float seconds = t * length;
            applyJsonPose(BARRAGE_NAME, seconds, 1.0F, yRotOffsetRad, xRotRad, false);
            trackArmsXRot(xRotRad);
        }

        @Override
        public IModelPose<TheGratefulDeadEntity> setEasing(UnaryOperator<Float> function) {
            this.easing = function;
            return this;
        }
    }

    private static class RightHandOnlyBarrageAnimation extends StandTwoHandedBarrageAnimation<TheGratefulDeadEntity> {
        private RightHandOnlyBarrageAnimation(TheGratefulDeadModel model, IModelPose<TheGratefulDeadEntity> loop, IModelPose<TheGratefulDeadEntity> recovery) {
            super(model, loop, recovery);
        }

        @Override
        protected HandSide getHandSide(StandEntityAction.Phase phase, TheGratefulDeadEntity entity, float ticks) {
            return HandSide.RIGHT;
        }

        @Override
        protected boolean switchesArms() {
            return false;
        }
    }
    private class JabActionAnim implements IActionAnimation<TheGratefulDeadEntity> {
        private static final float SECTION_LEN = 0.6F;
        private static final float WINDUP_END = 0.25F;
        private static final float PERFORM_END = 1.0F / 3.0F;

        @Override
        public void animate(StandEntityAction.Phase phase, float phaseCompletion, TheGratefulDeadEntity entity, float ticks, float yRotOffsetRad, float xRotRad, HandSide side) {
            float length = animationLength(JAB_NAME);
            if (length <= 0.0F) {
                return;
            }
            int combo = entity != null ? Math.max(entity.punchComboCount, 1) : 1;
            boolean leftPunch = (combo % 2) == 0;
            float sectionStart = leftPunch ? SECTION_LEN : 0.0F;
            float t;
            if (phase == null) {
                t = 0.0F;
            }
            else {
                switch (phase) {
                    case WINDUP:
                        t = phaseCompletion * WINDUP_END;
                        break;
                    case BUTTON_HOLD:
                    case PERFORM:
                        t = WINDUP_END + phaseCompletion * (PERFORM_END - WINDUP_END);
                        break;
                    case RECOVERY:
                        t = PERFORM_END + phaseCompletion * (1.0F - PERFORM_END);
                        break;
                    default:
                        t = 0.0F;
                }
            }
            float seconds = sectionStart + MathHelper.clamp(t, 0.0F, 1.0F) * SECTION_LEN;
            applyJsonPose(JAB_NAME, seconds, 1.0F, yRotOffsetRad, xRotRad, false);
            trackArmsXRot(xRotRad);
        }
    }

    private class OneShotActionAnim implements IActionAnimation<TheGratefulDeadEntity> {
        private final String name;
        private final float windupRatio;
        private final float performEnd;

        private OneShotActionAnim(String name, float windupRatio, float performEnd) {
            this.name = name;
            this.windupRatio = windupRatio;
            this.performEnd = performEnd;
        }

        @Override
        public void animate(StandEntityAction.Phase phase, float phaseCompletion, TheGratefulDeadEntity entity, float ticks, float yRotOffsetRad, float xRotRad, HandSide side) {
            float length = animationLength(name);
            if (length <= 0.0F) {
                return;
            }
            float t;
            if (phase == null) {
                t = 0.0F;
            }
            else {
                switch (phase) {
                    case WINDUP:
                        t = phaseCompletion * windupRatio;
                        break;
                    case BUTTON_HOLD:
                    case PERFORM:
                        t = windupRatio + phaseCompletion * (performEnd - windupRatio);
                        break;
                    case RECOVERY:
                        t = performEnd + phaseCompletion * (1.0F - performEnd);
                        break;
                    default:
                        t = 0.0F;
                }
            }
            applyJsonPose(name, MathHelper.clamp(t, 0.0F, 1.0F) * length, 1.0F, yRotOffsetRad, xRotRad, false);
            trackArmsXRot(xRotRad);
        }
    }

    private class StaticPoseAnim implements IActionAnimation<TheGratefulDeadEntity> {
        private final String name;

        private StaticPoseAnim(String name) {
            this.name = name;
        }

        @Override
        public void animate(StandEntityAction.Phase phase, float phaseCompletion, TheGratefulDeadEntity entity, float ticks, float yRotOffsetRad, float xRotRad, HandSide side) {
            applyJsonPose(name, 0.0F, 1.0F, yRotOffsetRad, xRotRad, false);
            trackArmsXRot(xRotRad);
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
