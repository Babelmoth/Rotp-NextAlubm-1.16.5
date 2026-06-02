package com.nextalubm.rotp_nextalbum.event;

import java.util.UUID;

import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.entity.RevolverBulletEntity;
import com.nextalubm.rotp_nextalbum.entity.SexPistolsEntity;
import com.nextalubm.rotp_nextalbum.network.NetworkHandler;
import com.nextalubm.rotp_nextalbum.network.SexPistolsKickAnimationPacket;
import com.nextalubm.rotp_nextalbum.network.SexPistolsKickMuzzleFlashPacket;
import com.nextalubm.rotp_nextalbum.stand.SexPistolsEntities;
import com.nextalubm.rotp_nextalbum.stand.SexPistolsStandType;
import com.nextalubm.rotp_nextalbum.util.SexPistolsBulletRedirectUtil;
import com.nextalubm.rotp_nextalbum.util.SexPistolsStandUtil;
import com.nextalubm.rotp_nextalbum.util.SexPistolsTargetMode;
import com.nextalubm.rotp_nextalbum.util.SexPistolsTransferOrder;
import com.nextalubm.rotp_nextalbum.util.SexPistolsSoundUtil;
import com.nextalubm.rotp_nextalbum.util.SexPistolsResolveUtil;
import com.nextalubm.rotp_nextalbum.util.SexPistolsStaminaUtil;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.network.PacketDistributor;

@EventBusSubscriber(modid = NextAlubm.MOD_ID, bus = EventBusSubscriber.Bus.FORGE)
public class SexPistolsProjectileEvents {
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getEntity() instanceof ProjectileEntity)) {
            return;
        }

        ProjectileEntity projectile = (ProjectileEntity) event.getEntity();
        if (projectile.level.isClientSide) {
            return;
        }

        Entity ownerEntity = projectile.getOwner();
        if (!(ownerEntity instanceof LivingEntity)) {
            return;
        }
        LivingEntity owner = (LivingEntity) ownerEntity;
        if (!SexPistolsStandUtil.isSexPistolsUser(owner)) {
            return;
        }

        Vector3d motion = projectile.getDeltaMovement();
        if (motion.lengthSqr() < 1.0E-6D) {
            return;
        }
        RayTraceResult currentResult = event.getRayTraceResult();
        Vector3d hitPos = currentResult != null ? currentResult.getLocation() : projectile.position();
        SexPistolsEntity standHit = getHitSexPistols(projectile, owner, currentResult, hitPos, motion);
        if (standHit == null) {
            return;
        }

        SexPistolsEntities sexPistols = IStandPower.getStandPowerOptional(owner).resolve().flatMap(SexPistolsStandType::getSexPistolsEntities).orElse(null);
        SexPistolsTargetMode targetMode = sexPistols != null ? sexPistols.getTargetMode() : SexPistolsTargetMode.ALL;
        LivingEntity lockedTarget = projectile instanceof RevolverBulletEntity ? ((RevolverBulletEntity) projectile).getSexPistolsLockedTarget(owner, targetMode) : null;
        SexPistolsTransferOrder transferOrder = sexPistols != null ? sexPistols.getTransferOrder() : SexPistolsTransferOrder.NONE;
        SexPistolsBulletRedirectUtil.RedirectResult redirect = SexPistolsBulletRedirectUtil.getRedirect(projectile.level, owner, standHit, hitPos, motion, targetMode, lockedTarget, transferOrder, projectile instanceof RevolverBulletEntity ? ((RevolverBulletEntity) projectile).getLastSexPistolsTransferStandId() : null);
        redirect = applyRedirect(projectile, standHit, redirect, projectile instanceof RevolverBulletEntity);
        if (redirect == null) {
            event.setCanceled(true);
            return;
        }
        if (projectile instanceof RevolverBulletEntity) {
            RevolverBulletEntity bullet = (RevolverBulletEntity) projectile;
            bullet.updateLastSexPistolsKickStand(standHit);
            bullet.updateSexPistolsLockedTarget(redirect.target, targetMode);
            bullet.updateLastSexPistolsTransferStand(redirect);
            bullet.markSexPistolsRedirected();
            bullet.checkImmediateSexPistolsRedirectHit(redirect.target);
        }
        else {
            SexPistolsResolveUtil.recordProjectileResolveRicochet(projectile);
        }
        event.setCanceled(true);
    }

    private static SexPistolsBulletRedirectUtil.RedirectResult applyRedirect(ProjectileEntity projectile, SexPistolsEntity stand, SexPistolsBulletRedirectUtil.RedirectResult redirect, boolean bullet) {
        LivingEntity user = stand.getUser();
        if (user == null || !SexPistolsStaminaUtil.consumeKickStamina(user)) {
            return null;
        }
        boolean critical = false;
        if (projectile instanceof RevolverBulletEntity && stand.getUser() != null) {
            RevolverBulletEntity revolverBullet = (RevolverBulletEntity) projectile;
            critical = revolverBullet.rollSexPistolsCriticalKick(stand.getUser());
            if (critical) {
                redirect = new SexPistolsBulletRedirectUtil.RedirectResult(redirect.position, redirect.motion.scale(1.15D), redirect.target, redirect.relay, redirect.scouting);
                projectile.playSound(SoundEvents.PLAYER_ATTACK_CRIT, 0.7F, 1.65F + stand.getRandom().nextFloat() * 0.2F);
            }
        }
        stand.playKickAnimation(redirect.motion);
        NetworkHandler.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> stand), new SexPistolsKickAnimationPacket(stand.getId(), redirect.motion.x, redirect.motion.z));
        NetworkHandler.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> stand), new SexPistolsKickMuzzleFlashPacket(stand.getId(), bullet, stand.getRandom().nextFloat(), critical));
        if (projectile instanceof RevolverBulletEntity) {
            ((RevolverBulletEntity) projectile).applyExternalSexPistolsRedirect(redirect.position, redirect.motion);
        }
        else {
            projectile.setDeltaMovement(redirect.motion);
            projectile.setPos(redirect.position.x, redirect.position.y, redirect.position.z);
            projectile.hasImpulse = true;
            projectile.hurtMarked = true;
        }
        SexPistolsSoundUtil.playRedirectKick(stand);
        return redirect;
    }

    private static SexPistolsEntity getHitSexPistols(ProjectileEntity projectile, LivingEntity owner, RayTraceResult result, Vector3d hitPos, Vector3d motion) {
        UUID ignoredStandId = null;
        if (projectile instanceof RevolverBulletEntity) {
            RevolverBulletEntity bullet = (RevolverBulletEntity) projectile;
            if (result instanceof EntityRayTraceResult) {
                Entity entity = ((EntityRayTraceResult) result).getEntity();
                if (entity instanceof SexPistolsEntity && bullet.shouldIgnoreSexPistolsRedirectStand((SexPistolsEntity) entity)) {
                    return null;
                }
            }
        }
        if (result instanceof EntityRayTraceResult) {
            Entity entity = ((EntityRayTraceResult) result).getEntity();
            if (entity instanceof SexPistolsEntity) {
                SexPistolsEntity stand = (SexPistolsEntity) entity;
                if (stand.getUser() == owner && SexPistolsStandUtil.isSexPistolsRemoteControlState(stand)) {
                    return stand;
                }
            }
            return null;
        }
        if (projectile instanceof RevolverBulletEntity) {
            RevolverBulletEntity bullet = (RevolverBulletEntity) projectile;
            if (bullet.getLastSexPistolsTransferStandId() != null) {
                ignoredStandId = bullet.getLastSexPistolsTransferStandId();
            }
        }
        Vector3d startPos = projectile.position();
        return SexPistolsBulletRedirectUtil.findOwnSexPistolsHit(projectile.level, owner, startPos, hitPos, ignoredStandId);
    }
}