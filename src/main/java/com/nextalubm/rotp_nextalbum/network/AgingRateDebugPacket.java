package com.nextalubm.rotp_nextalbum.network;

import java.util.function.Supplier;

import com.nextalubm.rotp_nextalbum.client.AgingRateDebugClient;
import com.nextalubm.rotp_nextalbum.util.AgingRateSnapshot;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;

public class AgingRateDebugPacket {
    private final boolean enabled;
    private final float min;
    private final float max;
    private final float zero;
    private final float base;
    private final float actual;

    public AgingRateDebugPacket(boolean enabled, AgingRateSnapshot snapshot) {
        this.enabled = enabled;
        if (snapshot != null) {
            this.min = snapshot.min;
            this.max = snapshot.max;
            this.zero = snapshot.zero;
            this.base = snapshot.base;
            this.actual = snapshot.actual;
        }
        else {
            this.min = 0F;
            this.max = 0F;
            this.zero = 0F;
            this.base = 0F;
            this.actual = 0F;
        }
    }

    private AgingRateDebugPacket(boolean enabled, float min, float max, float zero, float base, float actual) {
        this.enabled = enabled;
        this.min = min;
        this.max = max;
        this.zero = zero;
        this.base = base;
        this.actual = actual;
    }

    public static void encode(AgingRateDebugPacket msg, PacketBuffer buf) {
        buf.writeBoolean(msg.enabled);
        buf.writeFloat(msg.min);
        buf.writeFloat(msg.max);
        buf.writeFloat(msg.zero);
        buf.writeFloat(msg.base);
        buf.writeFloat(msg.actual);
    }

    public static AgingRateDebugPacket decode(PacketBuffer buf) {
        return new AgingRateDebugPacket(buf.readBoolean(), buf.readFloat(), buf.readFloat(),
                buf.readFloat(), buf.readFloat(), buf.readFloat());
    }

    public static void handle(AgingRateDebugPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> AgingRateDebugClient.handle(
                msg.enabled, msg.min, msg.max, msg.zero, msg.base, msg.actual)));
        ctx.get().setPacketHandled(true);
    }
}
