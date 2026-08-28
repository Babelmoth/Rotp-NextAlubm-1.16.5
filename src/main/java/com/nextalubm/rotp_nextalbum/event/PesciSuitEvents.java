package com.nextalubm.rotp_nextalbum.event;

import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.item.PesciSuitArmorItem;
import com.nextalubm.rotp_nextalbum.util.PesciSuitUtil;

import net.minecraft.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = NextAlubm.MOD_ID, bus = EventBusSubscriber.Bus.FORGE)
public final class PesciSuitEvents {
    private PesciSuitEvents() {
    }

    @SubscribeEvent
    public static void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        LivingEntity entity = event.getEntityLiving();
        if (entity.level.isClientSide()) {
            return;
        }
        if (PesciSuitArmorItem.countPieces(entity) > 0
                || entity.getPersistentData().hasUUID(PesciSuitUtil.NBT_WATCHED)
                || entity.getPersistentData().getBoolean(PesciSuitUtil.NBT_HAD_MODS)) {
            PesciSuitUtil.tick(entity);
        }
    }
}
