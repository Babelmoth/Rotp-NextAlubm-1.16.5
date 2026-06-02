package com.nextalubm.rotp_nextalbum.client.particle;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import com.github.standobyte.jojo.capability.world.TimeStopHandler;
import com.github.standobyte.jojo.client.ClientTimeStopHandler;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.init.InitSounds;
import com.nextalubm.rotp_nextalbum.network.NetworkHandler;
import com.nextalubm.rotp_nextalbum.network.SpawnRevolverCasingPacket;

import net.minecraft.client.particle.IAnimatedSprite;
import net.minecraft.client.particle.IParticleFactory;
import net.minecraft.client.particle.IParticleRenderType;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SpriteTexturedParticle;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particles.BasicParticleType;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = NextAlubm.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.FORGE)
public class RevolverCasingParticle extends SpriteTexturedParticle {
    private static final Queue<Boolean> SPAWN_ITEM_FLAGS = new LinkedList<>();
    private static final Set<RevolverCasingParticle> ACTIVE_CASINGS = new LinkedHashSet<>();
    private static final int TIME_STOP_FREE_FLIGHT_TICKS = 0;
    private static final int TIME_STOP_SLOWDOWN_TICKS = 3;
    private final boolean spawnItemOnRemove;
    private boolean landed;
    private boolean sentSpawnPacket;
    private boolean timeStop;
    private boolean manuallyTicking;
    private int timeStopFlightTicks;
    private int timeStopSlowdownTicks;
    private double timeStopXd;
    private double timeStopYd;
    private double timeStopZd;

    protected RevolverCasingParticle(ClientWorld world, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, IAnimatedSprite sprite, boolean spawnItemOnRemove) {
        super(world, x, y, z, xSpeed, ySpeed, zSpeed);
        this.spawnItemOnRemove = spawnItemOnRemove;
        this.pickSprite(sprite);
        this.gravity = 0.58F;
        this.hasPhysics = true;
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        this.quadSize = 0.115F + this.random.nextFloat() * 0.03F;
        this.lifetime = 34 + this.random.nextInt(16);
        ACTIVE_CASINGS.add(this);
    }

    public static void queueNextSpawnItemFlag(boolean spawnItem) {
        SPAWN_ITEM_FLAGS.add(spawnItem);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !ClientTimeStopHandler.isTimeStoppedStatic()) {
            return;
        }
        List<RevolverCasingParticle> snapshot = new ArrayList<>(ACTIVE_CASINGS);
        for (RevolverCasingParticle particle : snapshot) {
            if (particle.removed || particle.level == null) {
                continue;
            }
            particle.manuallyTicking = true;
            try {
                particle.tick();
            }
            finally {
                particle.manuallyTicking = false;
            }
        }
        ACTIVE_CASINGS.removeIf(particle -> particle.removed || particle.level == null);
    }

    @Override
    public void tick() {
        if (!manuallyTicking && ClientTimeStopHandler.isTimeStoppedStatic()) {
            return;
        }
        if (handleTimeStopMotion()) {
            return;
        }
        super.tick();
        this.xd *= this.onGround ? 0.42D : 0.985D;
        this.zd *= this.onGround ? 0.42D : 0.985D;
        if (this.onGround && !this.landed) {
            this.landed = true;
            this.level.playLocalSound(this.x, this.y, this.z, getRandomLandingSound(), SoundCategory.PLAYERS, 0.34F, 0.92F + this.random.nextFloat() * 0.22F, false);
        }
    }

    private boolean handleTimeStopMotion() {
        boolean stopped = TimeStopHandler.isTimeStopped(this.level, new BlockPos(this.x, this.y, this.z));
        if (!stopped) {
            if (timeStop) {
                timeStop = false;
                if (this.xd * this.xd + this.yd * this.yd + this.zd * this.zd < 1.0E-7D) {
                    this.xd = timeStopXd;
                    this.yd = timeStopYd;
                    this.zd = timeStopZd;
                }
            }
            return false;
        }
        if (!timeStop) {
            timeStop = true;
            timeStopFlightTicks = TIME_STOP_FREE_FLIGHT_TICKS;
            timeStopSlowdownTicks = TIME_STOP_SLOWDOWN_TICKS;
            timeStopXd = this.xd;
            timeStopYd = this.yd;
            timeStopZd = this.zd;
        }
        if (timeStopFlightTicks > 0) {
            timeStopFlightTicks--;
            tickTimeStopParticle(timeStopXd, timeStopYd, timeStopZd);
            return true;
        }
        if (timeStopSlowdownTicks > 0) {
            double slowdown = (double) timeStopSlowdownTicks / (double) TIME_STOP_SLOWDOWN_TICKS;
            double scale = Math.min(0.35D, slowdown * slowdown * 0.35D);
            tickTimeStopParticle(timeStopXd * scale, timeStopYd * scale, timeStopZd * scale);
            timeStopSlowdownTicks--;
            return true;
        }
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.xd = 0.0D;
        this.yd = 0.0D;
        this.zd = 0.0D;
        if (this.age++ >= this.lifetime) {
            this.remove();
        }
        return true;
    }

    private void tickTimeStopParticle(double xMotion, double yMotion, double zMotion) {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.xd = xMotion;
        this.yd = yMotion;
        this.zd = zMotion;
        this.move(this.xd, this.yd, this.zd);
        this.xd *= this.onGround ? 0.42D : 0.985D;
        this.zd *= this.onGround ? 0.42D : 0.985D;
        if (this.onGround && !this.landed) {
            this.landed = true;
            this.level.playLocalSound(this.x, this.y, this.z, getRandomLandingSound(), SoundCategory.PLAYERS, 0.34F, 0.92F + this.random.nextFloat() * 0.22F, false);
        }
        if (this.age++ >= this.lifetime) {
            this.remove();
        }
    }

    @Override
    public void remove() {
        ACTIVE_CASINGS.remove(this);
        if (!this.removed && this.spawnItemOnRemove && !this.sentSpawnPacket) {
            this.sentSpawnPacket = true;
            NetworkHandler.CHANNEL.sendToServer(new SpawnRevolverCasingPacket(this.x, this.y, this.z));
        }
        super.remove();
    }

    @Override
    public float getQuadSize(float partialTicks) {
        float progress = ((float) this.age + partialTicks) / (float) this.lifetime;
        return this.quadSize * (1.0F - progress * progress);
    }

    @Override
    public IParticleRenderType getRenderType() {
        return IParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    private SoundEvent getRandomLandingSound() {
        switch (this.random.nextInt(4)) {
        case 0:
            return InitSounds.SHELL_CASTING_1.get();
        case 1:
            return InitSounds.SHELL_CASTING_2.get();
        case 2:
            return InitSounds.SHELL_CASTING_3.get();
        default:
            return InitSounds.SHELL_CASTING_4.get();
        }
    }

    public static class Factory implements IParticleFactory<BasicParticleType> {
        private final IAnimatedSprite sprite;

        public Factory(IAnimatedSprite sprite) {
            this.sprite = sprite;
        }

        @Override
        public Particle createParticle(BasicParticleType type, ClientWorld world, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            Boolean spawnItem = SPAWN_ITEM_FLAGS.poll();
            return new RevolverCasingParticle(world, x, y, z, xSpeed, ySpeed, zSpeed, this.sprite, spawnItem != null && spawnItem.booleanValue());
        }
    }
}