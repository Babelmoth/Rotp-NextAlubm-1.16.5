package com.nextalubm.rotp_nextalbum.client.ui.marker;

import java.util.List;
import java.util.Optional;

import com.github.standobyte.jojo.client.ui.marker.MarkerRenderer;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.entity.BeachBoyHookEntity;
import com.nextalubm.rotp_nextalbum.init.InitStands;
import com.nextalubm.rotp_nextalbum.util.BeachBoyStandUtil;

import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.vector.Vector3d;

public class BeachBoyTrapMarker extends MarkerRenderer {
    public BeachBoyTrapMarker(Minecraft mc) {
        super(new ResourceLocation(NextAlubm.MOD_ID, "textures/action/beach_boy_trap.png"),
                InitStands.BEACH_BOY_TRAP.get(), mc);
        this.renderThroughBlocks = true;
    }

    @Override
    protected boolean shouldRender() {
        return mc.player != null && BeachBoyStandUtil.isBeachBoyUser(mc.player) && trapBlock() != null;
    }

    @Override
    protected void updatePositions(List<MarkerInstance> list, float partialTick) {
        BlockPos pos = trapBlock();
        if (pos == null) {
            return;
        }
        list.add(new MarkerInstance(markerPosForBlock(pos), true, Optional.empty()));
    }

    private BlockPos trapBlock() {
        if (mc.player == null) {
            return null;
        }
        BeachBoyHookEntity hook = BeachBoyStandUtil.findHook(mc.player);
        if (hook == null || !hook.isTrapArmed()) {
            return null;
        }
        Optional<BlockPos> trap = hook.getTrapBlockPos();
        return trap.isPresent() ? trap.get() : null;
    }

    public static Vector3d markerPosForBlock(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return Vector3d.atCenterOf(pos).add(0.0D, 0.55D, 0.0D);
        }
        BlockState state = mc.level.getBlockState(pos);
        VoxelShape shape = state.getShape(mc.level, pos);
        AxisAlignedBB bounds = shape.isEmpty() ? new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D) : shape.bounds();
        return new Vector3d(
                pos.getX() + (bounds.minX + bounds.maxX) * 0.5D,
                pos.getY() + bounds.maxY + 0.18D,
                pos.getZ() + (bounds.minZ + bounds.maxZ) * 0.5D);
    }
}
