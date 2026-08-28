package com.nextalubm.rotp_nextalbum.client.ui.marker;

import java.util.List;
import java.util.Optional;

import com.github.standobyte.jojo.client.ui.actionshud.ActionsOverlayGui;
import com.github.standobyte.jojo.client.ui.marker.MarkerRenderer;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.util.mod.JojoModUtil;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.init.InitStands;
import com.nextalubm.rotp_nextalbum.util.TheGratefulDeadGrabUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;

public class TheGratefulDeadGrabMarker extends MarkerRenderer {

    public TheGratefulDeadGrabMarker(Minecraft mc) {
        super(new ResourceLocation(NextAlubm.MOD_ID, "textures/action/the_grateful_dead_grab.png"),
                InitStands.THE_GRATEFUL_DEAD_GRAB.get(), mc);
        this.renderThroughBlocks = false;
    }

    @Override
    protected boolean shouldRender() {
        if (mc.player == null) {
            return false;
        }
        IStandPower power = IStandPower.getStandPowerOptional(mc.player).orElse(null);
        if (power == null || TheGratefulDeadGrabUtil.isGrabbing(power)) {
            return false;
        }
        ActionsOverlayGui hud = ActionsOverlayGui.getInstance();
        return hud.showExtraActionHud(InitStands.THE_GRATEFUL_DEAD_GRAB.get());
    }

    @Override
    protected void updatePositions(List<MarkerInstance> list, float partialTick) {
        if (mc.player == null || mc.level == null) {
            return;
        }
        IStandPower power = IStandPower.getStandPowerOptional(mc.player).orElse(null);
        if (power == null) {
            return;
        }
        final LivingEntity lookSource = TheGratefulDeadGrabUtil.getLookSource(mc.player, power) != null
                ? TheGratefulDeadGrabUtil.getLookSource(mc.player, power)
                : mc.player;

        LivingEntity lookedAt = null;
        RayTraceResult ray = JojoModUtil.rayTraceMultipleEntities(lookSource, TheGratefulDeadGrabUtil.GRAB_RANGE,
                entity -> entity instanceof LivingEntity && entity != mc.player && entity != lookSource,
                net.minecraft.util.math.RayTraceContext.BlockMode.COLLIDER, 0.0D, 0.0D)[0];
        if (ray instanceof EntityRayTraceResult && ((EntityRayTraceResult) ray).getEntity() instanceof LivingEntity) {
            lookedAt = (LivingEntity) ((EntityRayTraceResult) ray).getEntity();
        }

        AxisAlignedBB box = lookSource.getBoundingBox().inflate(TheGratefulDeadGrabUtil.GRAB_RANGE);
        for (LivingEntity entity : mc.level.getEntitiesOfClass(LivingEntity.class, box,
                candidate -> candidate != mc.player && candidate != lookSource
                        && TheGratefulDeadGrabUtil.canGrabEntity(mc.player, power, candidate))) {
            boolean highlighted = lookedAt != null && lookedAt.getId() == entity.getId();
            list.add(new MarkerInstance(
                    entity.getPosition(partialTick).add(0.0D, entity.getBbHeight() * 1.1D, 0.0D),
                    highlighted, Optional.empty()));
        }
    }
}
