package com.nextalubm.rotp_nextalbum.event;

import java.util.UUID;

import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.entity.BeachBoyHookEntity;
import com.nextalubm.rotp_nextalbum.item.BeachBoyRodItem;
import com.nextalubm.rotp_nextalbum.util.BeachBoyCombatUtil;
import com.nextalubm.rotp_nextalbum.util.BeachBoyLine;
import com.nextalubm.rotp_nextalbum.util.BeachBoyPathSwim;
import com.nextalubm.rotp_nextalbum.util.BeachBoySettings;
import com.nextalubm.rotp_nextalbum.util.BeachBoyStandUtil;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = NextAlubm.MOD_ID)
public class BeachBoyEvents {
    private static final UUID HOOKED_SPEED_UUID = UUID.fromString("c8a1b0d2-4e7f-4a91-9c3e-81b2a4d6e8f0");
    private static final double HOOKED_SPEED_PENALTY = -0.40D;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onItemToss(ItemTossEvent event) {
        if (BeachBoyStandUtil.isRodItem(event.getEntityItem().getItem())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (event.getWorld().isClientSide) {
            return;
        }
        Entity entity = event.getEntity();
        if (entity instanceof ItemEntity && BeachBoyStandUtil.isRodItem(((ItemEntity) entity).getItem())) {
            entity.remove();
        }
    }

    @SubscribeEvent
    public static void onPickup(EntityItemPickupEvent event) {
        ItemEntity item = event.getItem();
        if (item == null) {
            return;
        }
        if (BeachBoyStandUtil.isRodItem(item.getItem()) && !BeachBoyStandUtil.isBeachBoyUser(event.getPlayer())) {
            event.setCanceled(true);
            return;
        }
        BeachBoyHookEntity hook = BeachBoyStandUtil.findHookEmbeddedIn(item);
        if (hook != null) {
            if (hook.isTrapArmed()) {
                hook.triggerTrap(event.getPlayer());
            }
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        LivingEntity living = event.getEntityLiving();
        BeachBoyPathSwim.keep(living);
        tickHookedSlow(living);
        if (living.level.isClientSide) {
            return;
        }
        BeachBoyStandUtil.suppressPuppetAi(living);
        BeachBoyCombatUtil.tryTriggerWalk(living);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        PlayerEntity player = event.player;
        if (player.level.isClientSide) {
            return;
        }
        if (BeachBoyStandUtil.isBeachBoyUser(player)) {
            BeachBoySettings.ensure(player);
            ItemStack held = BeachBoyStandUtil.getHeldRod(player);
            if (!held.isEmpty()) {
                BeachBoyRodItem.syncStandScaledDamage(player, held);
            }
        }
        if (BeachBoyStandUtil.isBeachBoySummoned(player)) {
            return;
        }
        for (int i = 0; i < player.inventory.getContainerSize(); i++) {
            ItemStack stack = player.inventory.getItem(i);
            if (BeachBoyStandUtil.isRodItem(stack)) {
                player.inventory.setItem(i, ItemStack.EMPTY);
            }
        }
        BeachBoyHookEntity hook = BeachBoyStandUtil.findHook(player);
        if (hook != null) {
            hook.remove();
        }
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (BeachBoyStandUtil.findHookEmbeddedIn(event.getPlayer()) != null) {
            event.setNewSpeed(event.getNewSpeed() * 0.40F);
        }
    }

    private static void tickHookedSlow(LivingEntity living) {
        ModifiableAttributeInstance speed = living.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        boolean hooked = BeachBoyStandUtil.findHookEmbeddedIn(living) != null;
        AttributeModifier existing = speed.getModifier(HOOKED_SPEED_UUID);
        if (hooked && existing == null) {
            speed.addTransientModifier(new AttributeModifier(HOOKED_SPEED_UUID, "Beach Boy hooked",
                    HOOKED_SPEED_PENALTY, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
        else if (!hooked && existing != null) {
            speed.removeModifier(HOOKED_SPEED_UUID);
        }
    }

    @SubscribeEvent
    public static void onUserDeath(LivingDeathEvent event) {
        if (event.getEntityLiving().level.isClientSide) {
            return;
        }
        LivingEntity dead = event.getEntityLiving();
        BeachBoyHookEntity ownHook = BeachBoyStandUtil.findHook(dead);
        if (ownHook != null) {
            ownHook.remove();
        }
        BeachBoyHookEntity embedded = BeachBoyStandUtil.findHookEmbeddedIn(dead);
        if (embedded != null) {
            embedded.markPendingDeathLoot(dead);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onHookedDrops(net.minecraftforge.event.entity.living.LivingDropsEvent event) {
        if (event.getEntityLiving().level.isClientSide) {
            return;
        }
        LivingEntity dead = event.getEntityLiving();
        BeachBoyHookEntity hook = BeachBoyStandUtil.findHookEmbeddedIn(dead);
        if (hook == null && event.getEntityLiving() != null) {
            for (BeachBoyHookEntity candidate : dead.level.getEntitiesOfClass(BeachBoyHookEntity.class,
                    dead.getBoundingBox().inflate(8.0D), BeachBoyHookEntity::isAlive)) {
                if (candidate.isPendingDeathLoot(dead)) {
                    hook = candidate;
                    break;
                }
            }
        }
        if (hook == null) {
            return;
        }
        for (ItemEntity drop : event.getDrops()) {
            if (drop != null && !drop.getItem().isEmpty()) {
                hook.addDeathLoot(drop.getItem().copy());
                drop.remove();
            }
        }
        event.getDrops().clear();
        hook.clearPendingDeathLoot();
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        Entity target = event.getTarget();
        if (target instanceof BeachBoyHookEntity) {
            BeachBoyLine.tryConductMelee((BeachBoyHookEntity) target, event.getPlayer());
            event.setCanceled(true);
            return;
        }
        if (!event.getPlayer().level.isClientSide) {
            BeachBoyCombatUtil.tryTriggerEntityTrap(target, event.getPlayer());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof ProjectileEntity) || entity.level.isClientSide || BeachBoyLine.isConducting()) {
            return;
        }
        ProjectileEntity projectile = (ProjectileEntity) entity;
        if (projectile instanceof BeachBoyHookEntity) {
            return;
        }
        RayTraceResult result = event.getRayTraceResult();
        AxisAlignedBB box = projectile.getBoundingBox().inflate(BeachBoyLine.ABSOLUTE_MAX_LENGTH);
        for (BeachBoyLine line : BeachBoyLine.allIn(projectile.level, box)) {
            LivingEntity hooked = line.getHookedLiving();
            if (hooked == null || !line.isThornsEnabled()) {
                continue;
            }
            if (result instanceof net.minecraft.util.math.EntityRayTraceResult
                    && ((net.minecraft.util.math.EntityRayTraceResult) result).getEntity() == hooked) {
                if (line.getHook().hasConductedProjectile(projectile.getId())) {
                    event.setCanceled(true);
                    return;
                }
                continue;
            }
            if ((line.crossesImpact(projectile, result) || line.crossesProjectilePath(projectile))
                    && line.tryConductProjectile(projectile, result)) {
                event.setCanceled(true);
                return;
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingAttack(LivingAttackEvent event) {
        if (BeachBoyLine.isConducting() || event.getEntityLiving().level.isClientSide) {
            return;
        }
        DamageSource source = event.getSource();
        if (source == null) {
            return;
        }
        Entity attacker = source.getEntity();
        Entity direct = source.getDirectEntity();
        LivingEntity victim = event.getEntityLiving();
        AxisAlignedBB box = victim.getBoundingBox().inflate(BeachBoyLine.ABSOLUTE_MAX_LENGTH + 8.0D);
        if (attacker != null) {
            AxisAlignedBB other = attacker.getBoundingBox().inflate(BeachBoyLine.ABSOLUTE_MAX_LENGTH + 8.0D);
            box = new AxisAlignedBB(
                    Math.min(box.minX, other.minX), Math.min(box.minY, other.minY), Math.min(box.minZ, other.minZ),
                    Math.max(box.maxX, other.maxX), Math.max(box.maxY, other.maxY), Math.max(box.maxZ, other.maxZ));
        }
        for (BeachBoyLine line : BeachBoyLine.allIn(victim.level, box.inflate(BeachBoyLine.ABSOLUTE_MAX_LENGTH))) {
            LivingEntity hooked = line.getHookedLiving();
            if (hooked == null || hooked == victim || hooked == attacker) {
                continue;
            }
            boolean hitLine = false;
            if (attacker instanceof LivingEntity) {
                LivingEntity living = (LivingEntity) attacker;
                double reach = BeachBoyLine.getMeleeReach(living);
                Vector3d start = living.getEyePosition(1.0F);
                Vector3d end = victim.getBoundingBox().getCenter();
                Vector3d lookEnd = start.add(living.getLookAngle().scale(reach));
                double extra = living instanceof com.github.standobyte.jojo.entity.stand.StandEntity ? 0.35D : 0.0D;
                double minDist = line.isFromOwnerSide(living) ? 1.75D : 0.0D;
                boolean victimInReach = start.distanceTo(end) <= reach + 1.0D;
                hitLine = line.intersectsRay(start, lookEnd, minDist, extra)
                        || (victimInReach && line.intersectsRay(start, end, minDist, extra));
                if (!line.isFromOwnerSide(living)) {
                    hitLine = hitLine || (living.distanceTo(line.getHook()) <= reach + 1.0D
                            && line.intersectsEntity(living));
                }
            }
            else if (direct != null && direct != line.getHook()) {
                hitLine = line.intersectsEntity(direct);
            }
            if (hitLine && line.tryConductAttack(source, event.getAmount(),
                    direct instanceof ProjectileEntity ? direct : null)) {
                event.setCanceled(true);
                return;
            }
        }
        BeachBoyCombatUtil.tryTriggerEntityTrap(victim, attacker);
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!event.getPlayer().level.isClientSide) {
            BeachBoyCombatUtil.tryTriggerBlockTrap(event.getPlayer(), event.getPos());
        }
        if (tryHitLine(event.getPlayer())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getPlayer().level.isClientSide) {
            BeachBoyCombatUtil.tryTriggerBlockTrap(event.getPlayer(), event.getPos());
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!event.getPlayer().level.isClientSide) {
            BeachBoyCombatUtil.tryTriggerEntityTrap(event.getTarget(), event.getPlayer());
        }
    }

    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!event.getPlayer().level.isClientSide) {
            BeachBoyCombatUtil.tryTriggerEntityTrap(event.getTarget(), event.getPlayer());
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        PlayerEntity player = event.getPlayer();
        BlockPos pos = event.getPos();
        if (player != null && !player.level.isClientSide) {
            BeachBoyCombatUtil.tryTriggerBlockTrap(player, pos);
        }
    }

    public static boolean tryHitLine(PlayerEntity player) {
        if (player == null || player.level == null || BeachBoyLine.isConducting()) {
            return false;
        }
        if (BeachBoyLine.isHoldingHamonAction(player)) {
            return false;
        }
        double reach = BeachBoyLine.getMeleeReach(player);
        Vector3d start = player.getEyePosition(1.0F);
        Vector3d end = start.add(player.getLookAngle().scale(reach));
        AxisAlignedBB box = new AxisAlignedBB(start, end).inflate(2.0D);
        for (BeachBoyLine line : BeachBoyLine.allIn(player.level, box)) {
            boolean hit = line.intersectsRay(start, end, 1.75D);
            if (!line.isOwnedBy(player)) {
                hit = hit || (player.distanceTo(line.getHook()) <= reach + 1.0D && line.intersectsEntity(player));
            }
            if (hit && line.tryConductMelee(player)) {
                return true;
            }
        }
        return false;
    }
}
