package com.nextalubm.rotp_nextalbum.client.ui.marker;

import java.util.List;
import java.util.Optional;

import com.github.standobyte.jojo.client.ui.marker.MarkerRenderer;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.entity.SexPistolsEntity;
import com.nextalubm.rotp_nextalbum.init.InitStands;
import com.nextalubm.rotp_nextalbum.util.SexPistolsStandUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;

public class SexPistolsRemoteNumberMarker extends MarkerRenderer {
    private final int pistolIndex;

    public SexPistolsRemoteNumberMarker(Minecraft mc, int pistolIndex) {
        super(iconFor(pistolIndex), mc);
        this.pistolIndex = pistolIndex;
        this.renderThroughBlocks = true;
    }

    private static ResourceLocation iconFor(int pistolIndex) {
        int number = pistolIndex >= 0 && pistolIndex < InitStands.SEX_PISTOLS_NUMBERS.length
                ? InitStands.SEX_PISTOLS_NUMBERS[pistolIndex]
                : 1;
        return new ResourceLocation(NextAlubm.MOD_ID, "textures/gui/number_no" + number + ".png");
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
        AxisAlignedBB box = mc.player.getBoundingBox().inflate(256.0D);
        for (SexPistolsEntity stand : mc.level.getEntitiesOfClass(SexPistolsEntity.class, box,
                candidate -> candidate.isAlive()
                        && candidate.getUser() == mc.player
                        && candidate.getPistolIndex() == pistolIndex
                        && SexPistolsStandUtil.isSexPistolsRemoteControlState(candidate))) {
            list.add(new MarkerInstance(
                    stand.getPosition(partialTick).add(0.0D, stand.getBbHeight() + 0.45D, 0.0D),
                    stand.isRemotePositionFixed(),
                    Optional.empty()));
        }
    }
}
