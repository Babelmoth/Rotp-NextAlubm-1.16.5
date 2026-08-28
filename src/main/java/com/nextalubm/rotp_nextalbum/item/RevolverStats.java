package com.nextalubm.rotp_nextalbum.item;

import com.nextalubm.rotp_nextalbum.NextAlubm;

import net.minecraft.util.ResourceLocation;

public class RevolverStats {
    public static final float DEFAULT_CLOSE_RANGE = 5.0F;
    public static final float DEFAULT_STABLE_RANGE = 50.0F;

    public final float closeDamage;
    public final float baseDamage;
    public final float piercingDamage;
    public final float encirclementDamage;
    public final float encirclementFinishDamage;
    public final float splittingFragmentDamage;
    public final float minDistanceDamage;
    public final float closeRange;
    public final float stableRange;
    public final float farDropRate;

    public final float bulletSpeed;
    public final float hipInaccuracy;
    public final float adsInaccuracy;
    public final float recoilPitch;
    public final float recoilYaw;
    public final float adsRecoilPitchMul;
    public final float adsRecoilYawMul;
    public final float visualRecoilKick;
    public final float visualRecoilLift;
    public final int shotCooldownTicks;
    public final int reloadCooldownTicks;

    public final float meleeDamage;
    public final double meleeRange;
    public final double meleeTraceRadius;
    public final double meleeKnockback;
    public final int meleeAnimTicks;

    public final float adsZoom;
    public final float swaySensitivity;
    public final float criticalBonus;
    public final double bulletSpawnForwardOffset;

    public final ResourceLocation modelLocation;
    public final ResourceLocation textureLocation;
    public final ResourceLocation animationLocation;
    public final boolean heavyFireSound;

    public final FirstPersonPose firstPerson;

    private RevolverStats(Builder builder) {
        this.closeDamage = builder.closeDamage;
        this.baseDamage = builder.baseDamage;
        this.piercingDamage = builder.piercingDamage;
        this.encirclementDamage = builder.encirclementDamage;
        this.encirclementFinishDamage = builder.encirclementFinishDamage;
        this.splittingFragmentDamage = builder.splittingFragmentDamage;
        this.minDistanceDamage = builder.minDistanceDamage;
        this.closeRange = builder.closeRange;
        this.stableRange = builder.stableRange;
        this.farDropRate = builder.farDropRate;
        this.bulletSpeed = builder.bulletSpeed;
        this.hipInaccuracy = builder.hipInaccuracy;
        this.adsInaccuracy = builder.adsInaccuracy;
        this.recoilPitch = builder.recoilPitch;
        this.recoilYaw = builder.recoilYaw;
        this.adsRecoilPitchMul = builder.adsRecoilPitchMul;
        this.adsRecoilYawMul = builder.adsRecoilYawMul;
        this.visualRecoilKick = builder.visualRecoilKick;
        this.visualRecoilLift = builder.visualRecoilLift;
        this.shotCooldownTicks = builder.shotCooldownTicks;
        this.reloadCooldownTicks = builder.reloadCooldownTicks;
        this.meleeDamage = builder.meleeDamage;
        this.meleeRange = builder.meleeRange;
        this.meleeTraceRadius = builder.meleeTraceRadius;
        this.meleeKnockback = builder.meleeKnockback;
        this.meleeAnimTicks = builder.meleeAnimTicks;
        this.adsZoom = builder.adsZoom;
        this.swaySensitivity = builder.swaySensitivity;
        this.criticalBonus = builder.criticalBonus;
        this.bulletSpawnForwardOffset = builder.bulletSpawnForwardOffset;
        this.modelLocation = builder.modelLocation;
        this.textureLocation = builder.textureLocation;
        this.animationLocation = builder.animationLocation;
        this.heavyFireSound = builder.heavyFireSound;
        this.firstPerson = builder.firstPerson.build();
    }

    public static class FirstPersonPose {
        public final float hipX;
        public final float hipY;
        public final float hipZ;
        public final float hipRotX;
        public final float hipRotY;
        public final float hipRotZ;
        public final float adsX;
        public final float adsY;
        public final float adsZ;
        public final float adsRotX;
        public final float adsRotY;
        public final float adsRotZ;
        public final float reloadX;
        public final float reloadY;
        public final float reloadZ;
        public final float reloadRotX;
        public final float reloadRotY;
        public final float reloadRotZ;
        public final float scale;

        private FirstPersonPose(PoseBuilder builder) {
            this.hipX = builder.hipX;
            this.hipY = builder.hipY;
            this.hipZ = builder.hipZ;
            this.hipRotX = builder.hipRotX;
            this.hipRotY = builder.hipRotY;
            this.hipRotZ = builder.hipRotZ;
            this.adsX = builder.adsX;
            this.adsY = builder.adsY;
            this.adsZ = builder.adsZ;
            this.adsRotX = builder.adsRotX;
            this.adsRotY = builder.adsRotY;
            this.adsRotZ = builder.adsRotZ;
            this.reloadX = builder.reloadX;
            this.reloadY = builder.reloadY;
            this.reloadZ = builder.reloadZ;
            this.reloadRotX = builder.reloadRotX;
            this.reloadRotY = builder.reloadRotY;
            this.reloadRotZ = builder.reloadRotZ;
            this.scale = builder.scale;
        }
    }

    public static class PoseBuilder {
        private float hipX = 0.0F;
        private float hipY = 0.0F;
        private float hipZ = 0.0F;
        private float hipRotX = 0.0F;
        private float hipRotY = 0.0F;
        private float hipRotZ = 0.0F;
        private float adsX = 0.0F;
        private float adsY = 0.0F;
        private float adsZ = 0.0F;
        private float adsRotX = 0.0F;
        private float adsRotY = 0.0F;
        private float adsRotZ = 0.0F;
        private float reloadX = 0.10F;
        private float reloadY = 0.18F;
        private float reloadZ = -0.16F;
        private float reloadRotX = 12.0F;
        private float reloadRotY = 42.0F;
        private float reloadRotZ = 8.0F;
        private float scale = 1.0F;

        public PoseBuilder hip(float x, float y, float z, float rotX, float rotY, float rotZ) {
            this.hipX = x;
            this.hipY = y;
            this.hipZ = z;
            this.hipRotX = rotX;
            this.hipRotY = rotY;
            this.hipRotZ = rotZ;
            return this;
        }

        public PoseBuilder ads(float x, float y, float z, float rotX, float rotY, float rotZ) {
            this.adsX = x;
            this.adsY = y;
            this.adsZ = z;
            this.adsRotX = rotX;
            this.adsRotY = rotY;
            this.adsRotZ = rotZ;
            return this;
        }

        public PoseBuilder reload(float x, float y, float z, float rotX, float rotY, float rotZ) {
            this.reloadX = x;
            this.reloadY = y;
            this.reloadZ = z;
            this.reloadRotX = rotX;
            this.reloadRotY = rotY;
            this.reloadRotZ = rotZ;
            return this;
        }

        public PoseBuilder scale(float scale) {
            this.scale = scale;
            return this;
        }

        public FirstPersonPose build() {
            return new FirstPersonPose(this);
        }
    }

    public static class Builder {
        private float closeDamage = 10.0F;
        private float baseDamage = 5.0F;
        private float piercingDamage = 8.0F;
        private float encirclementDamage = 0.75F;
        private float encirclementFinishDamage = 3.0F;
        private float splittingFragmentDamage = 3.0F;
        private float minDistanceDamage = 1.0F;
        private float closeRange = DEFAULT_CLOSE_RANGE;
        private float stableRange = DEFAULT_STABLE_RANGE;
        private float farDropRate = 0.28F;
        private float bulletSpeed = 5.9F;
        private float hipInaccuracy = 0.9F;
        private float adsInaccuracy = 0.3F;
        private float recoilPitch = 5.35F;
        private float recoilYaw = 1.75F;
        private float adsRecoilPitchMul = 0.74F;
        private float adsRecoilYawMul = 0.55F;
        private float visualRecoilKick = 0.085F;
        private float visualRecoilLift = 6.5F;
        private int shotCooldownTicks = 5;
        private int reloadCooldownTicks = 10;
        private float meleeDamage = 1.0F;
        private double meleeRange = 2.75D;
        private double meleeTraceRadius = 0.65D;
        private double meleeKnockback = 0.45D;
        private int meleeAnimTicks = 9;
        private float adsZoom = 1.85F;
        private float swaySensitivity = 1.0F;
        private float criticalBonus = 0.0F;
        private double bulletSpawnForwardOffset = 0.45D;
        private ResourceLocation modelLocation = new ResourceLocation(NextAlubm.MOD_ID, "geo/mista_revolver.geo.json");
        private ResourceLocation textureLocation = new ResourceLocation(NextAlubm.MOD_ID, "textures/item/mista_revolver.png");
        private ResourceLocation animationLocation = new ResourceLocation(NextAlubm.MOD_ID, "animations/mista_revolver.animation.json");
        private boolean heavyFireSound = false;
        private final PoseBuilder firstPerson = new PoseBuilder();

        public Builder damage(float close, float piercing) {
            this.closeDamage = close;
            this.baseDamage = close * 0.5F;
            this.piercingDamage = piercing;
            return this;
        }

        public Builder damage(float close, float stable, float piercing) {
            this.closeDamage = close;
            this.baseDamage = stable;
            this.piercingDamage = piercing;
            return this;
        }

        public Builder specialDamage(float encirclement, float encirclementFinish, float splittingFragment) {
            this.encirclementDamage = encirclement;
            this.encirclementFinishDamage = encirclementFinish;
            this.splittingFragmentDamage = splittingFragment;
            return this;
        }

        public Builder range(float closeBlocks, float stableBlocks, float minDamage) {
            this.closeRange = closeBlocks;
            this.stableRange = stableBlocks;
            this.minDistanceDamage = minDamage;
            return this;
        }

        public Builder farDropRate(float rate) {
            this.farDropRate = rate;
            return this;
        }

        public Builder speed(float speed) {
            this.bulletSpeed = speed;
            return this;
        }

        public Builder inaccuracyMultiplier(float mult) {
            this.hipInaccuracy = 0.9F * mult;
            this.adsInaccuracy = 0.3F * mult;
            return this;
        }

        public Builder inaccuracy(float hip, float ads) {
            this.hipInaccuracy = hip;
            this.adsInaccuracy = ads;
            return this;
        }

        public Builder recoil(float pitch, float yaw) {
            this.recoilPitch = pitch;
            this.recoilYaw = yaw;
            return this;
        }

        public Builder adsRecoil(float pitchMul, float yawMul) {
            this.adsRecoilPitchMul = pitchMul;
            this.adsRecoilYawMul = yawMul;
            return this;
        }

        public Builder visualRecoil(float kick, float lift) {
            this.visualRecoilKick = kick;
            this.visualRecoilLift = lift;
            return this;
        }

        public Builder cooldowns(int shot, int reload) {
            this.shotCooldownTicks = shot;
            this.reloadCooldownTicks = reload;
            return this;
        }

        public Builder heavyFireSound() {
            this.heavyFireSound = true;
            return this;
        }

        public Builder melee(float damage, double range, double knockback) {
            this.meleeDamage = damage;
            this.meleeRange = range;
            this.meleeKnockback = knockback;
            return this;
        }

        public Builder meleeTrace(double radius, int animTicks) {
            this.meleeTraceRadius = radius;
            this.meleeAnimTicks = animTicks;
            return this;
        }

        public Builder adsZoom(float zoom) {
            this.adsZoom = zoom;
            return this;
        }

        public Builder sway(float sensitivity) {
            this.swaySensitivity = sensitivity;
            return this;
        }

        public Builder criticalBonus(float bonus) {
            this.criticalBonus = bonus;
            return this;
        }

        public Builder spawnOffset(double forward) {
            this.bulletSpawnForwardOffset = forward;
            return this;
        }

        public Builder visuals(String name) {
            this.modelLocation = new ResourceLocation(NextAlubm.MOD_ID, "geo/" + name + ".geo.json");
            this.textureLocation = new ResourceLocation(NextAlubm.MOD_ID, "textures/item/" + name + ".png");
            this.animationLocation = new ResourceLocation(NextAlubm.MOD_ID, "animations/" + name + ".animation.json");
            return this;
        }

        public Builder visuals(ResourceLocation model, ResourceLocation texture, ResourceLocation animation) {
            this.modelLocation = model;
            this.textureLocation = texture;
            this.animationLocation = animation;
            return this;
        }

        public Builder hipPose(float x, float y, float z, float rotX, float rotY, float rotZ) {
            this.firstPerson.hip(x, y, z, rotX, rotY, rotZ);
            return this;
        }

        public Builder adsPose(float x, float y, float z, float rotX, float rotY, float rotZ) {
            this.firstPerson.ads(x, y, z, rotX, rotY, rotZ);
            return this;
        }

        public Builder reloadPose(float x, float y, float z, float rotX, float rotY, float rotZ) {
            this.firstPerson.reload(x, y, z, rotX, rotY, rotZ);
            return this;
        }

        public Builder firstPersonScale(float scale) {
            this.firstPerson.scale(scale);
            return this;
        }

        public RevolverStats build() {
            return new RevolverStats(this);
        }
    }
}
