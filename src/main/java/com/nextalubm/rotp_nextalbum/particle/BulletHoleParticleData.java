package com.nextalubm.rotp_nextalbum.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.nextalubm.rotp_nextalbum.init.InitParticles;

import net.minecraft.particles.IParticleData;
import net.minecraft.particles.ParticleType;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.registries.ForgeRegistries;

public class BulletHoleParticleData implements IParticleData {
    public static final Codec<BulletHoleParticleData> CODEC = RecordCodecBuilder.create(builder -> builder.group(
            Codec.INT.fieldOf("dir").forGetter(data -> data.direction.ordinal()),
            Codec.LONG.fieldOf("pos").forGetter(data -> data.blockPos.asLong()))
            .apply(builder, BulletHoleParticleData::new));

    @SuppressWarnings("deprecation")
    public static final IParticleData.IDeserializer<BulletHoleParticleData> DESERIALIZER = new IParticleData.IDeserializer<BulletHoleParticleData>() {
        @Override
        public BulletHoleParticleData fromCommand(ParticleType<BulletHoleParticleData> particleType, StringReader reader) throws CommandSyntaxException {
            reader.expect(' ');
            int dir = reader.readInt();
            reader.expect(' ');
            long pos = reader.readLong();
            return new BulletHoleParticleData(dir, pos);
        }

        @Override
        public BulletHoleParticleData fromNetwork(ParticleType<BulletHoleParticleData> particleType, PacketBuffer buffer) {
            return new BulletHoleParticleData(buffer.readVarInt(), buffer.readLong());
        }
    };

    private final Direction direction;
    private final BlockPos blockPos;

    public BulletHoleParticleData(int directionOrdinal, long blockPosLong) {
        this.direction = Direction.values()[directionOrdinal];
        this.blockPos = BlockPos.of(blockPosLong);
    }

    public BulletHoleParticleData(Direction direction, BlockPos blockPos) {
        this.direction = direction;
        this.blockPos = blockPos;
    }

    public Direction getDirection() {
        return direction;
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    @Override
    public ParticleType<?> getType() {
        return InitParticles.BULLET_HOLE.get();
    }

    @Override
    public void writeToNetwork(PacketBuffer buffer) {
        buffer.writeVarInt(this.direction.ordinal());
        buffer.writeLong(this.blockPos.asLong());
    }

    @Override
    public String writeToString() {
        return ForgeRegistries.PARTICLE_TYPES.getKey(this.getType()) + " " + this.direction.ordinal() + " " + this.blockPos.asLong();
    }
}
