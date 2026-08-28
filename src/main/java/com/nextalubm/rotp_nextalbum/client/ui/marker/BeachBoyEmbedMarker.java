package com.nextalubm.rotp_nextalbum.client.ui.marker;

import java.util.List;
import java.util.Optional;

import com.github.standobyte.jojo.client.ui.marker.MarkerRenderer;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.entity.BeachBoyHookEntity;
import com.nextalubm.rotp_nextalbum.init.InitStands;
import com.nextalubm.rotp_nextalbum.util.BeachBoyStandUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

public class BeachBoyEmbedMarker extends MarkerRenderer {
    public BeachBoyEmbedMarker(Minecraft mc) {
        super(new ResourceLocation(NextAlubm.MOD_ID, "textures/action/beach_boy_retract_hook.png"),
                InitStands.BEACH_BOY_RETRACT_HOOK.get(), mc);
        this.renderThroughBlocks = true;
    }

    @Override
    protected boolean shouldRender() {
        return mc.player != null && BeachBoyStandUtil.isBeachBoyUser(mc.player);
    }

    @Override
    protected void updatePositions(List<MarkerInstance> list, float partialTick) {
        if (mc.player == null || mc.level == null) {
            return;
        }
        BeachBoyHookEntity hook = BeachBoyStandUtil.findHook(mc.player);
        if (hook == null || hook.getHookedEntity() == null) {
            return;
        }
        Entity host = hook.getHookedEntity();
        if (host == null || !host.isAlive()) {
            return;
        }
        list.add(new MarkerInstance(
                host.getPosition(partialTick).add(0.0D, host.getBbHeight() * 1.1D, 0.0D),
                true, Optional.empty()));
    }
}
