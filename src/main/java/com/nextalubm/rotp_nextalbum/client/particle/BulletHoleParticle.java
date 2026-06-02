package com.nextalubm.rotp_nextalbum.client.particle;

import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.nextalubm.rotp_nextalbum.particle.BulletHoleParticleData;

import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.IParticleFactory;
import net.minecraft.client.particle.IParticleRenderType;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SpriteTexturedParticle;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.texture.MissingTextureSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;

public class BulletHoleParticle extends SpriteTexturedParticle {
    private final Direction direction;
    private final BlockPos pos;
    private int uOffset;
    private int vOffset;
    private float textureDensity;

    protected BulletHoleParticle(ClientWorld world, double x, double y, double z, Direction direction, BlockPos pos) {
        super(world, x, y, z);
        this.direction = direction;
        this.pos = pos;
        this.setSprite(this.getSprite(pos));
        this.hasPhysics = false;
        this.gravity = 0.0F;
        this.quadSize = 0.05F;
        this.lifetime = 280 + world.random.nextInt(120);
        this.rCol = 0.22F;
        this.gCol = 0.22F;
        this.bCol = 0.22F;
        this.alpha = 0.95F;
        if (shouldRemove()) {
            this.remove();
        }
    }

    @Override
    protected void setSprite(TextureAtlasSprite sprite) {
        super.setSprite(sprite);
        this.uOffset = this.random.nextInt(16);
        this.vOffset = this.random.nextInt(16);
        this.textureDensity = (sprite.getU1() - sprite.getU0()) / 16.0F;
    }

    private TextureAtlasSprite getSprite(BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            BlockState state = minecraft.level.getBlockState(pos);
            return minecraft.getBlockRenderer().getBlockModelShaper().getTexture(state, minecraft.level, pos);
        }
        return minecraft.getTextureAtlas(PlayerContainer.BLOCK_ATLAS).apply(MissingTextureSprite.getLocation());
    }

    @Override
    protected float getU0() {
        return this.sprite.getU0() + this.uOffset * this.textureDensity;
    }

    @Override
    protected float getV0() {
        return this.sprite.getV0() + this.vOffset * this.textureDensity;
    }

    @Override
    protected float getU1() {
        return this.getU0() + this.textureDensity;
    }

    @Override
    protected float getV1() {
        return this.getV0() + this.textureDensity;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime || shouldRemove()) {
            this.remove();
            return;
        }
        float fadeStart = this.lifetime * 0.82F;
        if (this.age > fadeStart) {
            float fade = 1.0F - (this.age - fadeStart) / (this.lifetime - fadeStart);
            this.alpha = 0.95F * MathHelper.clamp(fade, 0.0F, 1.0F);
        }
    }

    @Override
    public void render(IVertexBuilder buffer, ActiveRenderInfo renderInfo, float partialTicks) {
        Vector3d view = renderInfo.getPosition();
        float particleX = (float) (MathHelper.lerp(partialTicks, this.xo, this.x) - view.x());
        float particleY = (float) (MathHelper.lerp(partialTicks, this.yo, this.y) - view.y());
        float particleZ = (float) (MathHelper.lerp(partialTicks, this.zo, this.z) - view.z());
        Quaternion quaternion = this.direction.getRotation();
        Vector3f[] points = new Vector3f[] {
                new Vector3f(-1.0F, 0.01F, -1.0F),
                new Vector3f(-1.0F, 0.01F, 1.0F),
                new Vector3f(1.0F, 0.01F, 1.0F),
                new Vector3f(1.0F, 0.01F, -1.0F)
        };
        float scale = this.getQuadSize(partialTicks);

        for (int i = 0; i < 4; ++i) {
            Vector3f point = points[i];
            point.transform(quaternion);
            point.mul(scale);
            point.add(particleX, particleY, particleZ);
        }

        float u0 = this.getU0();
        float u1 = this.getU1();
        float v0 = this.getV0();
        float v1 = this.getV1();
        int light = Math.max(12 - this.age / 20, 0);
        int lightColor = light << 20 | light << 4;
        float shade = 0.35F + (light / 12.0F) * 0.65F;
        float red = this.rCol * shade;
        float green = this.gCol * shade;
        float blue = this.bCol * shade;

        buffer.vertex(points[0].x(), points[0].y(), points[0].z()).uv(u1, v1).color(red, green, blue, this.alpha).uv2(lightColor).endVertex();
        buffer.vertex(points[1].x(), points[1].y(), points[1].z()).uv(u1, v0).color(red, green, blue, this.alpha).uv2(lightColor).endVertex();
        buffer.vertex(points[2].x(), points[2].y(), points[2].z()).uv(u0, v0).color(red, green, blue, this.alpha).uv2(lightColor).endVertex();
        buffer.vertex(points[3].x(), points[3].y(), points[3].z()).uv(u0, v1).color(red, green, blue, this.alpha).uv2(lightColor).endVertex();
    }

    @Override
    public IParticleRenderType getRenderType() {
        return IParticleRenderType.TERRAIN_SHEET;
    }

    private boolean shouldRemove() {
        BlockState blockState = this.level.getBlockState(this.pos);
        if (blockState.isAir()) {
            return true;
        }
        VoxelShape shape = blockState.getCollisionShape(this.level, this.pos);
        if (shape.isEmpty()) {
            return true;
        }
        AxisAlignedBB blockBoundingBox = shape.bounds().move(this.pos);
        return !blockBoundingBox.intersects(this.x - 0.1D, this.y - 0.1D, this.z - 0.1D, this.x + 0.1D, this.y + 0.1D, this.z + 0.1D);
    }

    public static class Factory implements IParticleFactory<BulletHoleParticleData> {
        @Override
        public Particle createParticle(BulletHoleParticleData type, ClientWorld world, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new BulletHoleParticle(world, x, y, z, type.getDirection(), type.getBlockPos());
        }
    }
}