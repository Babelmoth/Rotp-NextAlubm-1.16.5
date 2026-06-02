package com.nextalubm.rotp_nextalbum.event;

import java.util.List;

import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.util.mc.damage.StandLinkDamageSource;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.entity.SexPistolsEntity;
import com.nextalubm.rotp_nextalbum.init.InitStands;
import com.nextalubm.rotp_nextalbum.item.RevolverItem;
import com.nextalubm.rotp_nextalbum.stand.SexPistolsEntities;
import com.nextalubm.rotp_nextalbum.stand.SexPistolsStandType;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = NextAlubm.MOD_ID, bus = EventBusSubscriber.Bus.FORGE)
public class SexPistolsHealthEvents {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        LivingEntity user = event.getEntityLiving();
        if (user.level.isClientSide || event.getAmount() <= 0.0F || isSexPistolsLinkedDamage(event.getSource())) {
            return;
        }
        IStandPower.getStandPowerOptional(user).ifPresent(power -> {
            if (!power.hasPower() || power.getType() != InitStands.STAND_SEX_PISTOLS.get()) {
                return;
            }
            SexPistolsEntities sexPistols = SexPistolsStandType.getSexPistolsEntities(power).orElse(null);
            if (sexPistols == null) {
                return;
            }
            List<SexPistolsEntity> pistols = sexPistols.getAvailableLivingPistols();
            if (pistols.isEmpty()) {
                return;
            }
            float damageShare = event.getAmount() / (float) pistols.size();
            for (SexPistolsEntity pistol : pistols) {
                float health = pistol.getHealth() - damageShare;
                if (health <= 0.0F) {
                    pistol.setHealth(0.0F);
                    sexPistols.markPistolDead(pistol);
                }
                else {
                    pistol.setHealth(health);
                }
            }
        });
    }


    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntityLiving();
        if (!entity.level.isClientSide && entity instanceof PlayerEntity) {
            RevolverItem.clearSexPistolsAttachments((PlayerEntity) entity);
        }
    }
    private static boolean isSexPistolsLinkedDamage(DamageSource source) {
        return source instanceof StandLinkDamageSource && ((StandLinkDamageSource) source).getStandEntity() instanceof SexPistolsEntity;
    }
}
