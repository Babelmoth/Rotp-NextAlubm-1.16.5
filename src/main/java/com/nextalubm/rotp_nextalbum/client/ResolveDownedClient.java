package com.nextalubm.rotp_nextalbum.client;

import java.util.HashMap;
import java.util.Map;

import com.github.standobyte.jojo.client.ClientUtil;
import com.github.standobyte.jojo.init.ModParticles;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.network.NetworkHandler;
import com.nextalubm.rotp_nextalbum.network.ResolveDownedGiveUpPacket;
import com.nextalubm.rotp_nextalbum.util.ResolveDownedUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.KeybindTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = NextAlubm.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.FORGE)
public final class ResolveDownedClient {
    private static final Map<Integer, DownedView> STATES = new HashMap<Integer, DownedView>();
    private static final ITextComponent GIVE_UP_MESSAGE = new TranslationTextComponent(
            "jojo.message.skip_soul_ascension", new KeybindTextComponent("key.sneak"))
            .append(" + ")
            .append(new KeybindTextComponent("key.jump"));
    private static boolean lastGiveUpPacket;

    private ResolveDownedClient() {
    }

    public static void handleSync(int entityId, boolean downed, int ticksLeft, int duration, int giveUpTicks, boolean lockSkills) {
        Minecraft mc = Minecraft.getInstance();
        if (!downed) {
            STATES.remove(entityId);
            if (mc.level != null) {
                Entity entity = mc.level.getEntity(entityId);
                if (entity instanceof LivingEntity) {
                    LivingEntity living = (LivingEntity) entity;
                    if (!isActuallyDead(living)) {
                        living.deathTime = 0;
                    }
                }
            }
            return;
        }
        if (mc.level != null && mc.level.getEntity(entityId) instanceof LivingEntity
                && isActuallyDead((LivingEntity) mc.level.getEntity(entityId))) {
            STATES.remove(entityId);
            return;
        }
        STATES.put(entityId, new DownedView(ticksLeft, duration, giveUpTicks, lockSkills));
    }

    private static boolean isActuallyDead(LivingEntity entity) {
        return entity == null || entity.removed || entity.isDeadOrDying() || entity.getHealth() <= 0.0F;
    }

    public static boolean isDowned(Entity entity) {
        return entity != null && STATES.containsKey(entity.getId());
    }

    public static float getProgress(Entity entity) {
        if (entity == null) {
            return 0.0F;
        }
        DownedView view = STATES.get(entity.getId());
        if (view == null || view.duration <= 0) {
            return 0.0F;
        }
        return 1.0F - (view.ticksLeft / (float) view.duration);
    }

    public static boolean shouldLockAction(LivingEntity user, Object action) {
        DownedView view = user == null ? null : STATES.get(user.getId());
        if (view == null || !view.lockSkills) {
            return false;
        }
        return !ResolveDownedUtil.isAllowedAction(user, action);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.isPaused()) {
            return;
        }
        spawnSoulParticles(mc);
        pruneDeadStates(mc);
        DownedView view = STATES.get(mc.player.getId());
        if (view == null) {
            if (lastGiveUpPacket) {
                NetworkHandler.CHANNEL.sendToServer(new ResolveDownedGiveUpPacket(false));
                lastGiveUpPacket = false;
            }
            return;
        }
        mc.player.deathTime = ResolveDownedUtil.DOWNED_POSE_TICKS;
        boolean holding = mc.player.isShiftKeyDown() && mc.options.keyJump.isDown();
        NetworkHandler.CHANNEL.sendToServer(new ResolveDownedGiveUpPacket(holding));
        lastGiveUpPacket = holding;
        if (holding) {
            mc.gui.setOverlayMessage(new TranslationTextComponent("message.rotp_nextalbum.resolve_downed.give_up"), false);
        }
    }

    private static void pruneDeadStates(Minecraft mc) {
        if (STATES.isEmpty() || mc.level == null) {
            return;
        }
        java.util.Iterator<Integer> iterator = STATES.keySet().iterator();
        while (iterator.hasNext()) {
            Integer entityId = iterator.next();
            Entity entity = mc.level.getEntity(entityId);
            if (!(entity instanceof LivingEntity) || isActuallyDead((LivingEntity) entity)) {
                iterator.remove();
            }
        }
    }

    private static void spawnSoulParticles(Minecraft mc) {
        if (STATES.isEmpty()) {
            return;
        }
        long gameTime = mc.level.getGameTime();
        for (Integer entityId : STATES.keySet()) {
            Entity entity = mc.level.getEntity(entityId);
            if (!(entity instanceof LivingEntity)) {
                continue;
            }
            LivingEntity living = (LivingEntity) entity;
            if (gameTime % 10L == 5L) {
                mc.level.addParticle(ParticleTypes.POOF,
                        living.getRandomX(1.0D), living.getY(living.getRandom().nextDouble() * 0.25D), living.getRandomZ(1.0D),
                        living.getRandom().nextGaussian() * 0.02D,
                        living.getRandom().nextGaussian() * 0.02D,
                        living.getRandom().nextGaussian() * 0.02D);
            }
            if (gameTime % 8L == 0L) {
                for (int i = 0; i < 3; i++) {
                    mc.level.addParticle(ModParticles.SOUL_CLOUD.get(),
                            living.getRandomX(1.0D), living.getRandomY(), living.getRandomZ(1.0D),
                            living.getRandom().nextGaussian() * 0.02D,
                            living.getRandom().nextGaussian() * 0.02D,
                            living.getRandom().nextGaussian() * 0.02D);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onInput(InputUpdateEvent event) {
        if (!isDowned(event.getPlayer())) {
            return;
        }
        event.getMovementInput().forwardImpulse = 0.0F;
        event.getMovementInput().leftImpulse = 0.0F;
        event.getMovementInput().jumping = false;
        event.getMovementInput().up = false;
        event.getMovementInput().down = false;
        event.getMovementInput().left = false;
        event.getMovementInput().right = false;
        if (!(event.getPlayer().isShiftKeyDown() && Minecraft.getInstance().options.keyJump.isDown())) {
            event.getMovementInput().shiftKeyDown = event.getPlayer().isShiftKeyDown();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void cancelHands(RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && isDowned(mc.player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void cancelDeathScreen(GuiOpenEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.getGui() instanceof DeathScreen && mc.player != null && isDowned(mc.player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Pre<?, ?> event) {
        LivingEntity entity = event.getEntity();
        if (isDowned(entity)) {
            if (isActuallyDead(entity)) {
                STATES.remove(entity.getId());
                return;
            }
            entity.deathTime = ResolveDownedUtil.DOWNED_POSE_TICKS;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void renderTimer(RenderGameOverlayEvent.Pre event) {
        if (event.getType() != ElementType.EXPERIENCE) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !isDowned(mc.player) || mc.player.isSpectator()) {
            return;
        }
        DownedView view = STATES.get(mc.player.getId());
        if (view == null) {
            return;
        }
        event.setCanceled(true);
        MatrixStack matrixStack = event.getMatrixStack();
        mc.getProfiler().push("expBar");
        mc.getTextureManager().bind(ClientUtil.ADDITIONAL_UI);
        int xPos = mc.getWindow().getGuiScaledWidth() / 2 - 91;
        int yPos = mc.getWindow().getGuiScaledHeight() - 32 + 3;
        int width = 182;
        float remaining = view.duration <= 0 ? 0F : view.ticksLeft / (float) view.duration;
        int fill = (int) (remaining * (width + 1));
        AbstractGui.blit(matrixStack, xPos, yPos, 0, 0, 208, width, 5, 256, 256);
        if (fill > 0) {
            AbstractGui.blit(matrixStack, xPos, yPos, 0, 0, 213, fill, 5, 256, 256);
        }
        mc.getProfiler().pop();
    }

    private static final class DownedView {
        final int ticksLeft;
        final int duration;
        final int giveUpTicks;
        final boolean lockSkills;

        DownedView(int ticksLeft, int duration, int giveUpTicks, boolean lockSkills) {
            this.ticksLeft = ticksLeft;
            this.duration = duration;
            this.giveUpTicks = giveUpTicks;
            this.lockSkills = lockSkills;
        }
    }
}
