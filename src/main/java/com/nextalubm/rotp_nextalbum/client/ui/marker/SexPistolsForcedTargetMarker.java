package com.nextalubm.rotp_nextalbum.client.ui.marker;

import java.util.List;
import java.util.Optional;

import com.github.standobyte.jojo.client.ui.marker.MarkerRenderer;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.client.SexPistolsForcedTargetClientState;
import com.nextalubm.rotp_nextalbum.client.SexPistolsTargetModeClientState;
import com.nextalubm.rotp_nextalbum.init.InitStands;
import com.nextalubm.rotp_nextalbum.util.SexPistolsAimUtil;
import com.nextalubm.rotp_nextalbum.util.SexPistolsForcedTarget;
import com.nextalubm.rotp_nextalbum.util.SexPistolsStandUtil;
import com.nextalubm.rotp_nextalbum.util.SexPistolsTargetMode;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;

public class SexPistolsForcedTargetMarker extends MarkerRenderer {
    public SexPistolsForcedTargetMarker(Minecraft mc) {
        super(new ResourceLocation(NextAlubm.MOD_ID, "textures/action/sex_pistols_forced_target.png"),
                InitStands.SEX_PISTOLS_FORCED_TARGET.get(), mc);
        this.renderThroughBlocks = true;
    }

    @Override
    protected boolean shouldRender() {
        return mc.player != null && SexPistolsStandUtil.isSexPistolsUser(mc.player);
    }

    @Override
    protected void updatePositions(List<MarkerInstance> list, float partialTick) {
        if (mc.player == null || mc.level == null) {
            return;
        }
        IStandPower power = IStandPower.getStandPowerOptional(mc.player).orElse(null);
        LivingEntity looked = null;
        BlockPos lookedBlock = null;
        if (power != null) {
            com.github.standobyte.jojo.action.ActionTarget look =
                    SexPistolsStandUtil.resolveForcedLookTarget(mc.player, power);
            if (look.getType() == com.github.standobyte.jojo.action.ActionTarget.TargetType.ENTITY
                    && look.getEntity() instanceof LivingEntity) {
                looked = (LivingEntity) look.getEntity();
            }
            else if (look.getType() == com.github.standobyte.jojo.action.ActionTarget.TargetType.BLOCK) {
                lookedBlock = look.getBlockPos();
            }
        }
        for (SexPistolsForcedTarget target : SexPistolsForcedTargetClientState.getTargets()) {
            if (!target.isValid(mc.level)) {
                continue;
            }
            boolean active = target.getKind() == SexPistolsForcedTarget.Kind.ENTITY
                    ? looked != null && looked.getId() == target.getEntityNetworkId()
                    : lookedBlock != null && lookedBlock.equals(target.getBlockPos());
            Vector3d pos = target.getKind() == SexPistolsForcedTarget.Kind.ENTITY
                    ? target.getLiving(mc.level).getPosition(partialTick).add(0.0D, target.getLiving(mc.level).getBbHeight() * 1.1D, 0.0D)
                    : Vector3d.atCenterOf(target.getBlockPos()).add(0.0D, 0.65D, 0.0D);
            list.add(new MarkerInstance(pos, active, Optional.empty()));
        }
        if (SexPistolsTargetModeClientState.getMode() == SexPistolsTargetMode.AIM && power != null) {
            LivingEntity aim = SexPistolsAimUtil.findAimTarget(mc.player, power);
            if (aim != null) {
                list.add(new MarkerInstance(aim.getPosition(partialTick).add(0.0D, aim.getBbHeight() * 1.1D, 0.0D), true, Optional.empty()));
            }
        }
    }
}
