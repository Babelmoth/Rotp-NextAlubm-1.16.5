package com.nextalubm.rotp_nextalbum.item;

import net.minecraft.util.ResourceLocation;
import com.nextalubm.rotp_nextalbum.NextAlubm;

public class RevolverStats {
    public final float baseDamage;
    public final float piercingDamage;
    public final float bulletSpeed;
    public final float inaccuracy;
    public final float recoilPitch;
    public final float recoilYaw;
    public final int shotCooldownTicks;
    public final int reloadCooldownTicks;

    public final ResourceLocation modelLocation;
    public final ResourceLocation textureLocation;
    public final ResourceLocation animationLocation;

    private RevolverStats(Builder builder) {
        this.baseDamage = builder.baseDamage;
        this.piercingDamage = builder.piercingDamage;
        this.bulletSpeed = builder.bulletSpeed;
        this.inaccuracy = builder.inaccuracy;
        this.recoilPitch = builder.recoilPitch;
        this.recoilYaw = builder.recoilYaw;
        this.shotCooldownTicks = builder.shotCooldownTicks;
        this.reloadCooldownTicks = builder.reloadCooldownTicks;
        this.modelLocation = builder.modelLocation;
        this.textureLocation = builder.textureLocation;
        this.animationLocation = builder.animationLocation;
    }

    public static class Builder {
        private float baseDamage = 10.0F;
        private float piercingDamage = 8.0F;
        private float bulletSpeed = 5.9F;
        private float inaccuracy = 1.0F;
        private float recoilPitch = 5.35F;
        private float recoilYaw = 1.75F;
        private int shotCooldownTicks = 5;
        private int reloadCooldownTicks = 10;
        private ResourceLocation modelLocation = new ResourceLocation(NextAlubm.MOD_ID, "geo/mista_revolver.geo.json");
        private ResourceLocation textureLocation = new ResourceLocation(NextAlubm.MOD_ID, "textures/item/mista_revolver.png");
        private ResourceLocation animationLocation = new ResourceLocation(NextAlubm.MOD_ID, "animations/mista_revolver.animation.json");

        public Builder damage(float base, float piercing) { this.baseDamage = base; this.piercingDamage = piercing; return this; }
        public Builder speed(float speed) { this.bulletSpeed = speed; return this; }
        public Builder inaccuracyMultiplier(float mult) { this.inaccuracy = mult; return this; }
        public Builder recoil(float pitch, float yaw) { this.recoilPitch = pitch; this.recoilYaw = yaw; return this; }
        public Builder cooldowns(int shot, int reload) { this.shotCooldownTicks = shot; this.reloadCooldownTicks = reload; return this; }
        public Builder visuals(String name) {
            this.modelLocation = new ResourceLocation(NextAlubm.MOD_ID, "geo/" + name + ".geo.json");
            this.textureLocation = new ResourceLocation(NextAlubm.MOD_ID, "textures/item/" + name + ".png");
            this.animationLocation = new ResourceLocation(NextAlubm.MOD_ID, "animations/" + name + ".animation.json");
            return this;
        }
        public RevolverStats build() { return new RevolverStats(this); }
    }
}