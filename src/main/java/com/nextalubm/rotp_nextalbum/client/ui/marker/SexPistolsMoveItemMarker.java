package com.nextalubm.rotp_nextalbum.client.ui.marker;

import java.util.List;
import java.util.Optional;

import com.github.standobyte.jojo.client.ui.actionshud.ActionsOverlayGui;
import com.github.standobyte.jojo.client.ui.marker.MarkerRenderer;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.action.SexPistolsMoveSelectedAction;
import com.nextalubm.rotp_nextalbum.init.InitStands;
import com.nextalubm.rotp_nextalbum.util.SexPistolsStandUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;

public class SexPistolsMoveItemMarker extends MarkerRenderer {
    public SexPistolsMoveItemMarker(Minecraft mc) {
        super(new ResourceLocation(NextAlubm.MOD_ID, "textures/action/sex_pistols_stand_recall.png"), mc);
        this.renderThroughBlocks = false;
    }

    @Override
    protected boolean shouldRender() {
        ActionsOverlayGui hud = ActionsOverlayGui.getInstance();
        return mc.player != null && SexPistolsStandUtil.isSexPistolsUser(mc.player) && hud.showExtraActionHud(InitStands.SEX_PISTOLS_MOVE_SELECTED.get());
    }

    @Override
    protected void updatePositions(List<MarkerInstance> list, float partialTick) {
        if (mc.player == null || mc.level == null) {
            return;
        }
        ItemEntity lookedAt = SexPistolsMoveSelectedAction.findLookedAtItem(mc.level, mc.player, SexPistolsMoveSelectedAction.ITEM_TARGET_RANGE);
        AxisAlignedBB box = mc.player.getBoundingBox().inflate(SexPistolsMoveSelectedAction.ITEM_TARGET_RANGE);
        for (ItemEntity itemEntity : mc.level.getEntitiesOfClass(ItemEntity.class, box, SexPistolsMoveSelectedAction::isValidItemTarget)) {
            boolean active = lookedAt != null && lookedAt.getId() == itemEntity.getId();
            list.add(new MarkerInstance(itemEntity.getPosition(partialTick).add(0.0D, 0.35D, 0.0D), active, Optional.empty()));
        }
    }
}
