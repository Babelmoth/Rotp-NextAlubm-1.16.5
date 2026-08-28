package com.nextalubm.rotp_nextalbum.client.particle;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.particle.IAnimatedSprite;
import net.minecraft.client.particle.IParticleFactory;
import net.minecraft.client.particle.IParticleRenderType;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SpriteTexturedParticle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particles.BasicParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.opengl.GL11;

@OnlyIn(Dist.CLIENT)
public class JojoSoundParticle extends SpriteTexturedParticle {
    private final IAnimatedSprite sprites;
    private double baseOffsetY;
    private final float baseRoll;
    private static final int FOOTSTEP_LIFETIME = 18;
    private double riseSpeed;

    protected JojoSoundParticle(ClientWorld world, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, IAnimatedSprite spriteSet) {
        super(world, x, y, z, xSpeed, ySpeed, zSpeed);
        this.sprites = spriteSet;
        this.baseOffsetY = 0;
        this.riseSpeed = 0.008D;

        this.xd = 0.0D;
        this.yd = 0.0D;
        this.zd = 0.0D;
        this.gravity = 0.0F;

        this.quadSize *= 1.2F + (this.random.nextFloat() - 0.5F) * 0.6F;

        float randomAngle = (this.random.nextFloat() - 0.5F) * ((float) Math.PI / 2.0F);
        this.baseRoll = randomAngle;
        this.roll = randomAngle;
        this.oRoll = randomAngle;

        this.lifetime = FOOTSTEP_LIFETIME;
        this.setSpriteFromAge(spriteSet);
    }

    @Override
    public int getLightColor(float partialTick) {
        return 15728880;
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

        riseSpeed *= 0.78D;
        baseOffsetY += riseSpeed;

        double jitterX = (this.random.nextDouble() - 0.5D) * 0.03D;
        double jitterY = (this.random.nextDouble() - 0.5D) * 0.03D;
        double jitterZ = (this.random.nextDouble() - 0.5D) * 0.03D;

        this.x += jitterX;
        this.y = this.yo + baseOffsetY + jitterY;
        this.z += jitterZ;

        this.roll = this.baseRoll + (this.random.nextFloat() - 0.5F) * 0.08F;

        this.setSpriteFromAge(this.sprites);
        float fadeStart = this.lifetime * 0.5F;
        if (this.age > fadeStart) {
            this.alpha = Math.max(0.0F, 1.0F - ((float) (this.age - fadeStart) / (this.lifetime - fadeStart)));
        }
    }

    @Override
    public IParticleRenderType getRenderType() {
        return JOJO_OVERLAY_RENDERER;
    }

    public static final IParticleRenderType JOJO_OVERLAY_RENDERER = new IParticleRenderType() {
        @Override
        public void begin(BufferBuilder buffer, TextureManager textureManager) {
            RenderSystem.disableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            textureManager.bind(AtlasTexture.LOCATION_PARTICLES);
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.PARTICLE);
        }

        @Override
        public void end(Tessellator tesselator) {
            tesselator.end();
            RenderSystem.enableDepthTest();
        }

        @Override
        public String toString() {
            return "JOJO_SOUND_OVERLAY";
        }
    };

    @OnlyIn(Dist.CLIENT)
    public static class Factory implements IParticleFactory<BasicParticleType> {
        private final IAnimatedSprite sprites;

        public Factory(IAnimatedSprite sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(BasicParticleType type, ClientWorld world, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            JojoSoundParticle particle = new JojoSoundParticle(world, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites);
            particle.pickSprite(this.sprites);
            return particle;
        }
    }
}
