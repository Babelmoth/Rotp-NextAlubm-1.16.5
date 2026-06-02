package com.nextalubm.rotp_nextalbum.client.particle;

import net.minecraft.client.particle.IAnimatedSprite;
import net.minecraft.client.particle.IParticleFactory;
import net.minecraft.client.particle.IParticleRenderType;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SpriteTexturedParticle;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particles.BasicParticleType;
import net.minecraft.util.math.MathHelper;

public class RevolverMuzzleFlashParticle extends SpriteTexturedParticle {
    protected RevolverMuzzleFlashParticle(ClientWorld world, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, IAnimatedSprite sprite) {
        super(world, x, y, z, xSpeed, ySpeed, zSpeed);
        this.pickSprite(sprite);
        this.hasPhysics = false;
        this.gravity = 0.0F;
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        this.lifetime = 2;
        this.quadSize = 0.21F + this.random.nextFloat() * 0.09F;
        this.rCol = 1.0F;
        this.gCol = 0.88F + this.random.nextFloat() * 0.12F;
        this.bCol = 0.58F + this.random.nextFloat() * 0.20F;
        this.alpha = 1.0F;
        this.roll = this.random.nextFloat() * ((float) Math.PI * 2.0F);
        this.oRoll = this.roll;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.oRoll = this.roll;
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        this.x += this.xd;
        this.y += this.yd;
        this.z += this.zd;
        this.xd *= 0.62D;
        this.yd *= 0.62D;
        this.zd *= 0.62D;
        float progress = (float) this.age / (float) this.lifetime;
        this.alpha = MathHelper.clamp(1.0F - progress * progress, 0.0F, 1.0F);
        this.quadSize *= 0.86F;
    }

    @Override
    public float getQuadSize(float partialTicks) {
        float progress = ((float) this.age + partialTicks) / (float) this.lifetime;
        float flashScale = 1.0F + (1.0F - MathHelper.clamp(progress, 0.0F, 1.0F)) * 0.35F;
        return this.quadSize * flashScale;
    }

    @Override
    public int getLightColor(float partialTicks) {
        return 15728880;
    }

    @Override
    public IParticleRenderType getRenderType() {
        return IParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Factory implements IParticleFactory<BasicParticleType> {
        private final IAnimatedSprite sprite;

        public Factory(IAnimatedSprite sprite) {
            this.sprite = sprite;
        }

        @Override
        public Particle createParticle(BasicParticleType type, ClientWorld world, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new RevolverMuzzleFlashParticle(world, x, y, z, xSpeed, ySpeed, zSpeed, this.sprite);
        }
    }
}
