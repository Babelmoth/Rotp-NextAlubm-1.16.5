package com.nextalubm.rotp_nextalbum.client.particle;

import com.mojang.blaze3d.vertex.IVertexBuilder;

import net.minecraft.client.particle.IAnimatedSprite;
import net.minecraft.client.particle.IParticleFactory;
import net.minecraft.client.particle.IParticleRenderType;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SpriteTexturedParticle;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particles.BasicParticleType;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AgingMistParticle extends SpriteTexturedParticle {
    private final float facingYaw;
    private final float maxAlpha;

    protected AgingMistParticle(ClientWorld world, double x, double y, double z, IAnimatedSprite sprites) {
        super(world, x, y, z);
        this.facingYaw = this.random.nextFloat() * ((float) Math.PI * 2.0F);
        float speed = 0.0035F + this.random.nextFloat() * 0.0055F;
        this.xd = MathHelper.cos(this.facingYaw) * speed;
        this.yd = 0.0D;
        this.zd = MathHelper.sin(this.facingYaw) * speed;
        this.gravity = 0.0F;
        this.hasPhysics = false;
        this.lifetime = 80 + this.random.nextInt(55);
        this.quadSize *= 2.1F + this.random.nextFloat() * 1.6F;
        this.maxAlpha = 0.16F + this.random.nextFloat() * 0.12F;
        this.alpha = 0.0F;
        this.rCol = 0.84F;
        this.gCol = 0.80F;
        this.bCol = 0.74F;
        this.pickSprite(sprites);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        this.move(this.xd, this.yd, this.zd);
        this.alpha = currentAlpha(this.age);
    }

    private float currentAlpha(int age) {
        float fadeIn = Math.min(18, this.lifetime * 0.22F);
        float fadeOut = Math.min(28, this.lifetime * 0.32F);
        if (age < fadeIn) {
            return this.maxAlpha * (age / fadeIn);
        }
        if (age > this.lifetime - fadeOut) {
            return this.maxAlpha * Math.max(0.0F, (this.lifetime - age) / fadeOut);
        }
        return this.maxAlpha;
    }

    @Override
    public void render(IVertexBuilder buffer, ActiveRenderInfo info, float partialTicks) {
        this.alpha = currentAlpha(this.age);
        Vector3d camera = info.getPosition();
        float x = (float) (MathHelper.lerp(partialTicks, this.xo, this.x) - camera.x);
        float y = (float) (MathHelper.lerp(partialTicks, this.yo, this.y) - camera.y);
        float z = (float) (MathHelper.lerp(partialTicks, this.zo, this.z) - camera.z);
        Quaternion rotation = Vector3f.YP.rotation(-this.facingYaw);
        Vector3f[] corners = new Vector3f[] {
                new Vector3f(-1.0F, -1.0F, 0.0F),
                new Vector3f(-1.0F, 1.0F, 0.0F),
                new Vector3f(1.0F, 1.0F, 0.0F),
                new Vector3f(1.0F, -1.0F, 0.0F)
        };
        float size = this.getQuadSize(partialTicks);
        for (Vector3f corner : corners) {
            corner.transform(rotation);
            corner.mul(size);
            corner.add(x, y, z);
        }
        float u0 = this.getU0();
        float u1 = this.getU1();
        float v0 = this.getV0();
        float v1 = this.getV1();
        int light = this.getLightColor(partialTicks);
        buffer.vertex(corners[0].x(), corners[0].y(), corners[0].z()).uv(u1, v1)
                .color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
        buffer.vertex(corners[1].x(), corners[1].y(), corners[1].z()).uv(u1, v0)
                .color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
        buffer.vertex(corners[2].x(), corners[2].y(), corners[2].z()).uv(u0, v0)
                .color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
        buffer.vertex(corners[3].x(), corners[3].y(), corners[3].z()).uv(u0, v1)
                .color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
    }

    @Override
    public IParticleRenderType getRenderType() {
        return IParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTicks) {
        int packed = super.getLightColor(partialTicks);
        int block = Math.max(packed & 0xFFFF, 8 << 4);
        int sky = Math.max(packed >> 16, 8 << 4);
        return block | (sky << 16);
    }

    @OnlyIn(Dist.CLIENT)
    public static class Factory implements IParticleFactory<BasicParticleType> {
        private final IAnimatedSprite sprites;

        public Factory(IAnimatedSprite sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(BasicParticleType type, ClientWorld world, double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new AgingMistParticle(world, x, y, z, this.sprites);
        }
    }
}
