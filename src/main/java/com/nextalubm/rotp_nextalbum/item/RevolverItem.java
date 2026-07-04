package com.nextalubm.rotp_nextalbum.item;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.github.standobyte.jojo.client.ClientUtil;
import com.github.standobyte.jojo.init.ModItems;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.nextalubm.rotp_nextalbum.client.RevolverClientEvents;
import com.nextalubm.rotp_nextalbum.client.render.RevolverGeoRenderer;
import com.nextalubm.rotp_nextalbum.entity.RevolverBulletEntity;
import com.nextalubm.rotp_nextalbum.init.InitItems;
import com.nextalubm.rotp_nextalbum.init.InitSounds;
import com.nextalubm.rotp_nextalbum.network.EjectRevolverCasingsPacket;
import com.nextalubm.rotp_nextalbum.network.NetworkHandler;
import com.nextalubm.rotp_nextalbum.network.RevolverMuzzleFlashPacket;
import com.nextalubm.rotp_nextalbum.stand.SexPistolsEntities;
import com.nextalubm.rotp_nextalbum.stand.SexPistolsStandType;
import com.nextalubm.rotp_nextalbum.util.SexPistolsItemAttachmentUtil;
import com.nextalubm.rotp_nextalbum.util.SexPistolsSoundUtil;
import com.nextalubm.rotp_nextalbum.util.SexPistolsStaminaUtil;
import com.nextalubm.rotp_nextalbum.util.SexPistolsResolveUtil;
import com.nextalubm.rotp_nextalbum.util.SexPistolsStandUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.UseAction;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.KeybindTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.PacketDistributor;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;
import software.bernie.geckolib3.util.GeckoLibUtil;

public class RevolverItem extends Item implements IAnimatable {
    public static final String AMMO_KEY = "Ammo";
    public static final String AIM_KEY = "Aim";
    public static final String SHOT_COOLDOWN_KEY = "ShotCooldown";
    public static final String RELOAD_COOLDOWN_KEY = "ReloadCooldown";
    public static final String RELOAD_MODE_KEY = "ReloadMode";
    public static final String SELECTED_CHAMBER_KEY = "SelectedChamber";
    public static final String CHAMBERS_KEY = "Chambers";
    public static final String SPENT_CHAMBERS_KEY = "SpentChambers";
    public static final String PISTOL_CHAMBERS_KEY = "PistolChambers";
    public static final String ENCIRCLEMENT_CHAMBERS_KEY = "EncirclementChambers";
    public static final String PIERCING_CHAMBERS_KEY = "PiercingChambers";
    public static final String SPLITTING_CHAMBERS_KEY = "SplittingChambers";
    
    private static final int MAX_AMMO = 6;
    private static final int FULL_CHAMBERS = (1 << MAX_AMMO) - 1;
    private static final int SHOT_COOLDOWN_TICKS = 5;
    private static final int RELOAD_COOLDOWN_TICKS = 10;
    private static final float RECOIL_PITCH = 5.35F;
    private static final float RECOIL_YAW = 1.75F;
    private static final float BULLET_SPEED = 5.9F;
    private static final double BULLET_SPAWN_FORWARD_OFFSET = 0.45D;

    private static final float AIM_ZOOM_MODIFIER = 1.85F;

    private static final double MELEE_RANGE = 2.75D;
    private static final double MELEE_TRACE_RADIUS = 0.65D;
    private static final float MELEE_DAMAGE = 1.0F;
    private static final double MELEE_KNOCKBACK = 0.45D;

    private final AnimationFactory factory = GeckoLibUtil.createFactory(this);

    public RevolverItem() {
        super(new Properties().stacksTo(1).tab(ModItems.MAIN_TAB).setISTER(() -> RevolverGeoRenderer::new));
    }

    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    @Override
    public AnimationFactory getFactory() {
        return factory;
    }

    private <E extends IAnimatable> PlayState predicate(AnimationEvent<E> event) {
        List<ItemStack> stacks = event.getExtraDataOfType(ItemStack.class);
        if (!stacks.isEmpty() && isReloadMode(stacks.get(0))) {
            event.getController().setAnimation(new AnimationBuilder().addAnimation("reloading", true));
        }
        else {
            event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.revolver.idle", true));
        }
        return PlayState.CONTINUE;
    }

    @Override
    public ActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != Hand.MAIN_HAND) {
            return ActionResult.pass(stack);
        }
        if (isReloadMode(stack)) {
            if (!world.isClientSide) {
                if (player.isShiftKeyDown()) {
                    handleServerExtractChamber((ServerPlayerEntity) player, stack);
                }
                else {
                    handleServerFillChamber((ServerPlayerEntity) player, stack);
                }
            }
            return ActionResult.consume(stack);
        }
        setAiming(stack, true);
        player.startUsingItem(hand);
        return ActionResult.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, World world, LivingEntity entity, int timeLeft) {
        setAiming(stack, false);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAction getUseAnimation(ItemStack stack) {
        return UseAction.NONE;
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int itemSlot, boolean isSelected) {
        normalizeChambers(stack);
        if (getShotCooldown(stack) > 0) {
            setShotCooldown(stack, getShotCooldown(stack) - 1);
        }
        if (getReloadCooldown(stack) > 0) {
            setReloadCooldown(stack, getReloadCooldown(stack) - 1);
        }
        if (!isSelected) {
            setAiming(stack, false);
            setReloadMode(stack, false);
        }
        if (entity instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) entity;
            if (!world.isClientSide) {
                clearInvalidSexPistolsAttachments(stack, player);
            }
            if (isReloadMode(stack)) {
                setAiming(stack, false);
                if (player.getUseItem() == stack) {
                    player.stopUsingItem();
                }
            }
            else if (player.getUseItem() != stack) {
                setAiming(stack, false);
            }
        }
    }


    private static void clearInvalidSexPistolsAttachments(ItemStack stack, PlayerEntity player) {
        if (!hasPistolAttachments(stack)) {
            return;
        }
        if (player.isAlive() && SexPistolsStandUtil.isSexPistolsUser(player)) {
            return;
        }
        clearAllPistolChambers(stack);
        clearLoadedSexPistolsForPlayer(player);
    }

    public static void clearSexPistolsAttachments(PlayerEntity player) {
        clearSexPistolsAttachments(player.inventory.items);
        clearSexPistolsAttachments(player.inventory.offhand);
        clearSexPistolsAttachments(player.inventory.armor);
        clearLoadedSexPistolsForPlayer(player);
    }

    public static int recallSexPistolsAttachments(PlayerEntity player) {
        int pistolMask = collectAndClearSexPistolsAttachments(player.inventory.items)
                | collectAndClearSexPistolsAttachments(player.inventory.offhand)
                | collectAndClearSexPistolsAttachments(player.inventory.armor);
        final int[] recalled = {0};
        IStandPower.getStandPowerOptional(player).ifPresent(power -> SexPistolsStandType.getSexPistolsEntities(power).ifPresent(sexPistols -> {
            recalled[0] += sexPistols.recallLoadedPistols(pistolMask | sexPistols.getLoadedPistolsMask(), getPistolReturnPosition(player));
        }));
        return recalled[0];
    }

    private static int collectAndClearSexPistolsAttachments(List<ItemStack> stacks) {
        int pistolMask = 0;
        for (ItemStack stack : stacks) {
            if (stack.getItem() instanceof RevolverItem && hasPistolAttachments(stack)) {
                for (int chamberMask : getPistolChambers(stack)) {
                    pistolMask |= chamberMask;
                }
                clearAllPistolChambers(stack);
            }
            if (SexPistolsItemAttachmentUtil.hasAttachment(stack)) {
                pistolMask |= SexPistolsItemAttachmentUtil.getPistolMask(stack);
                SexPistolsItemAttachmentUtil.clear(stack);
            }
        }
        return pistolMask;
    }

    private static void clearSexPistolsAttachments(List<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            if (stack.getItem() instanceof RevolverItem && hasPistolAttachments(stack)) {
                clearAllPistolChambers(stack);
            }
            if (SexPistolsItemAttachmentUtil.hasAttachment(stack)) {
                SexPistolsItemAttachmentUtil.clear(stack);
            }
        }
    }

    public static void clearLoadedSexPistolsForPlayer(PlayerEntity player) {
        IStandPower.getStandPowerOptional(player).ifPresent(power -> SexPistolsStandType.getSexPistolsEntities(power).ifPresent(SexPistolsEntities::clearLoadedPistols));
    }
    public void handleServerShoot(ServerPlayerEntity player, ItemStack stack) {
        normalizeChambers(stack);
        if (getShotCooldown(stack) > 0 || isReloadMode(stack)) {
            return;
        }
        int chamber = getSelectedChamber(stack);
        if (!hasBullet(stack, chamber)) {
            player.level.playSound(null, player.blockPosition(), InitSounds.REVOLVER_DRY_FIRE.get(), SoundCategory.PLAYERS, 0.75F, 1.0F);
            advanceChamber(stack, true);
            setShotCooldown(stack, getShotCooldownTicks(player));
            return;
        }
        if (hasSplittingChamber(stack, chamber)) {
            shootSplittingShot(player, stack, chamber);
            setShotCooldown(stack, getShotCooldownTicks(player));
            return;
        }
        if (hasPiercingChamber(stack, chamber)) {
            shootPiercingShot(player, stack, chamber);
            setShotCooldown(stack, getShotCooldownTicks(player));
            return;
        }
        if (hasEncirclementChamber(stack, chamber)) {
            shootEncirclementVolley(player, stack, chamber);
            setShotCooldown(stack, getShotCooldownTicks(player));
            return;
        }
        if (!shootBullet(player, stack, chamber)) {
            setShotCooldown(stack, getShotCooldownTicks(player));
            return;
        }
        setChamberLoaded(stack, chamber, false);
        setChamberSpent(stack, chamber, true);
        advanceChamber(stack, true);
        setShotCooldown(stack, getShotCooldownTicks(player));
    }

    public void handleServerToggleReloadMode(ServerPlayerEntity player, ItemStack stack) {
        normalizeChambers(stack);
        boolean reloadMode = !isReloadMode(stack);
        setReloadMode(stack, reloadMode);
        if (reloadMode) {
            ejectSpentCasings(player, stack);
        }
        setAiming(stack, false);
        if (player.getUseItem() == stack) {
            player.stopUsingItem();
        }
        player.level.playSound(null, player.blockPosition(), reloadMode ? InitSounds.REVOLVER_OPEN.get() : InitSounds.REVOLVER_CLOSE.get(), SoundCategory.PLAYERS, 0.85F, 1.0F);
    }

    public void handleServerMeleeAttack(ServerPlayerEntity player, ItemStack stack) {
        normalizeChambers(stack);
        if (isReloadMode(stack)) {
            return;
        }
        LivingEntity target = findMeleeTarget(player);
        if (target == null) {
            return;
        }
        if (target.hurt(DamageSource.playerAttack(player), MELEE_DAMAGE)) {
            Vector3d look = player.getLookAngle();
            target.setDeltaMovement(target.getDeltaMovement().add(look.x * MELEE_KNOCKBACK, 0.12D, look.z * MELEE_KNOCKBACK));
            target.hasImpulse = true;
            target.hurtMarked = true;
            player.swing(Hand.MAIN_HAND, true);
            player.level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_WEAK, SoundCategory.PLAYERS, 0.7F, 1.15F);
        }
    }

    private LivingEntity findMeleeTarget(ServerPlayerEntity player) {
        Vector3d eye = player.getEyePosition(1.0F);
        Vector3d look = player.getLookAngle();
        Vector3d reach = eye.add(look.scale(MELEE_RANGE));
        AxisAlignedBB area = player.getBoundingBox().expandTowards(look.scale(MELEE_RANGE)).inflate(MELEE_TRACE_RADIUS);
        LivingEntity bestTarget = null;
        double bestDistance = MELEE_RANGE * MELEE_RANGE;
        for (LivingEntity target : player.level.getEntitiesOfClass(LivingEntity.class, area, target -> target != player && target.isAlive() && !target.isSpectator() && target.isPickable())) {
            Vector3d center = target.getBoundingBox().getCenter();
            double distance = distanceToSegmentSqr(center, eye, reach);
            double allowed = Math.max(MELEE_TRACE_RADIUS, target.getBbWidth() * 0.5D + MELEE_TRACE_RADIUS);
            if (distance > allowed * allowed) {
                continue;
            }
            double forwardDistance = center.subtract(eye).dot(look);
            if (forwardDistance < 0.0D || forwardDistance > MELEE_RANGE) {
                continue;
            }
            double distanceToPlayer = player.distanceToSqr(target);
            if (distanceToPlayer < bestDistance) {
                bestDistance = distanceToPlayer;
                bestTarget = target;
            }
        }
        return bestTarget;
    }

    private static double distanceToSegmentSqr(Vector3d point, Vector3d start, Vector3d end) {
        Vector3d segment = end.subtract(start);
        double lengthSqr = segment.lengthSqr();
        if (lengthSqr <= 1.0E-6D) {
            return point.distanceToSqr(start);
        }
        double t = MathHelper.clamp(point.subtract(start).dot(segment) / lengthSqr, 0.0D, 1.0D);
        return point.distanceToSqr(start.add(segment.scale(t)));
    }
    public void handleServerFillChamber(ServerPlayerEntity player, ItemStack stack) {
        normalizeChambers(stack);
        if (!isReloadMode(stack)) {
            return;
        }
        if (!canReload(player, stack)) {
            return;
        }
        int target = findNextEmptyChamber(stack, getSelectedChamber(stack));
        if (target < 0) {
            player.level.playSound(null, player.blockPosition(), SoundEvents.DISPENSER_FAIL, SoundCategory.PLAYERS, 0.45F, 1.6F);
            return;
        }
        setSelectedChamber(stack, target);
        if (!consumeAmmoItem(player)) {
            player.level.playSound(null, player.blockPosition(), SoundEvents.DISPENSER_FAIL, SoundCategory.PLAYERS, 0.45F, 1.0F);
            return;
        }
        setChamberLoaded(stack, target, true);
        setChamberSpent(stack, target, false);
        advanceChamber(stack, true);
        applyReloadCooldown(player, stack);
        player.level.playSound(null, player.blockPosition(), InitSounds.REVOLVER_SPEEDLOADER.get(), SoundCategory.PLAYERS, 0.85F, 1.0F);
    }

    public int loadAvailableAmmoIntoEmptyChambers(ServerPlayerEntity player, ItemStack stack) {
        return loadAvailableAmmoIntoEmptyChambers(player, stack, MAX_AMMO);
    }

    public int loadAvailableAmmoIntoEmptyChambers(ServerPlayerEntity player, ItemStack stack, int maxBullets) {
        normalizeChambers(stack);
        if (!isReloadMode(stack)) {
            return 0;
        }
        if (!canReload(player, stack) || maxBullets <= 0) {
            return 0;
        }
        int selected = getSelectedChamber(stack);
        int loaded = 0;
        for (int i = 0; i < MAX_AMMO && loaded < maxBullets; i++) {
            int chamber = Math.floorMod(selected + i, MAX_AMMO);
            if (hasBullet(stack, chamber)) {
                continue;
            }
            if (!consumeAmmoItem(player)) {
                break;
            }
            setChamberLoaded(stack, chamber, true);
            setChamberSpent(stack, chamber, false);
            loaded++;
        }
        setSelectedChamber(stack, selected);
        if (loaded > 0) {
            applyReloadCooldown(player, stack);
        }
        return loaded;
    }


    public boolean loadAmmoIntoSelectedChamber(ServerPlayerEntity player, ItemStack stack) {
        normalizeChambers(stack);
        if (!isReloadMode(stack)) {
            return false;
        }
        if (!canReload(player, stack)) {
            return false;
        }
        int chamber = getSelectedChamber(stack);
        if (hasBullet(stack, chamber)) {
            return true;
        }
        if (!consumeAmmoItem(player)) {
            return false;
        }
        setChamberLoaded(stack, chamber, true);
        setChamberSpent(stack, chamber, false);
        applyReloadCooldown(player, stack);
        return true;
    }
    public void handleServerExtractChamber(ServerPlayerEntity player, ItemStack stack) {
        normalizeChambers(stack);
        if (!isReloadMode(stack)) {
            return;
        }
        if (!canReload(player, stack)) {
            return;
        }
        int chamber = findNextExtractableChamber(stack, getSelectedChamber(stack));
        if (chamber < 0) {
            player.level.playSound(null, player.blockPosition(), InitSounds.REVOLVER_DRY_FIRE.get(), SoundCategory.PLAYERS, 0.45F, 1.2F);
            return;
        }
        setSelectedChamber(stack, chamber);
        if (hasBullet(stack, chamber)) {
            releasePistolsFromChamber(player, stack, chamber);
            setChamberLoaded(stack, chamber, false);
            giveAmmoItem(player);
            advanceChamber(stack, true);
            applyReloadCooldown(player, stack);
            player.level.playSound(null, player.blockPosition(), InitSounds.REVOLVER_SHELLS_OUT.get(), SoundCategory.PLAYERS, 0.85F, 1.0F);
            return;
        }
        if (hasSpentCasing(stack, chamber)) {
            setChamberSpent(stack, chamber, false);
            advanceChamber(stack, true);
            applyReloadCooldown(player, stack);
            NetworkHandler.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), new EjectRevolverCasingsPacket(player.getId(), 1));
            player.level.playSound(null, player.blockPosition(), InitSounds.REVOLVER_SHELLS_OUT.get(), SoundCategory.PLAYERS, 0.85F, 1.0F);
            return;
        }
        player.level.playSound(null, player.blockPosition(), InitSounds.REVOLVER_DRY_FIRE.get(), SoundCategory.PLAYERS, 0.45F, 1.2F);
    }
    public void handleServerSwitchChamber(ServerPlayerEntity player, ItemStack stack, boolean forward) {
        normalizeChambers(stack);
        if (!isReloadMode(stack)) {
            return;
        }
        advanceChamber(stack, forward);
        player.level.playSound(null, player.blockPosition(), SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundCategory.PLAYERS, 0.5F, forward ? 1.15F : 0.9F);
    }

    public void ejectSpentCasings(ServerPlayerEntity player, ItemStack stack) {
        int count = Integer.bitCount(getSpentChambers(stack));
        if (count <= 0) {
            return;
        }
        setSpentChambers(stack, 0);
        NetworkHandler.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), new EjectRevolverCasingsPacket(player.getId(), count));
        player.level.playSound(null, player.blockPosition(), InitSounds.REVOLVER_SHELLS_OUT.get(), SoundCategory.PLAYERS, 0.75F, 1.0F);
    }

    private void shootEncirclementVolley(ServerPlayerEntity player, ItemStack stack, int startChamber) {
        if (!hasBullet(stack, startChamber) || !hasEncirclementChamber(stack, startChamber)) {
            return;
        }
        float pitch = player.xRot;
        float yaw = player.yRot;
        if (!shootBulletWithMask(player, stack, getPistolMask(stack, startChamber), true, false, false, pitch, yaw)) {
            return;
        }
        setChamberLoaded(stack, startChamber, false);
        setChamberSpent(stack, startChamber, true);
        clearEncirclementChamber(stack, startChamber);
        setSelectedChamber(stack, startChamber + 1);
        applyRecoil(player, stack);
    }
    private void shootPiercingShot(ServerPlayerEntity player, ItemStack stack, int chamber) {
        if (!hasBullet(stack, chamber) || !hasPiercingChamber(stack, chamber)) {
            return;
        }
        float pitch = player.xRot;
        float yaw = player.yRot;
        if (!shootBulletWithMask(player, stack, getPistolMask(stack, chamber), false, true, false, pitch, yaw)) {
            return;
        }
        setChamberLoaded(stack, chamber, false);
        setChamberSpent(stack, chamber, true);
        clearPiercingChamber(stack, chamber);
        setSelectedChamber(stack, chamber + 1);
    }

    private void shootSplittingShot(ServerPlayerEntity player, ItemStack stack, int chamber) {
        if (!hasBullet(stack, chamber) || !hasSplittingChamber(stack, chamber)) {
            return;
        }
        float pitch = player.xRot;
        float yaw = player.yRot;
        if (!shootBulletWithMask(player, stack, getPistolMask(stack, chamber), false, false, true, pitch, yaw)) {
            return;
        }
        setChamberLoaded(stack, chamber, false);
        setChamberSpent(stack, chamber, true);
        clearSplittingChamber(stack, chamber);
        setSelectedChamber(stack, chamber + 1);
    }

    private boolean shootBullet(ServerPlayerEntity player, ItemStack stack, int chamber) {
        return shootBullet(player, stack, chamber, player.xRot, player.yRot);
    }

    private boolean shootBullet(ServerPlayerEntity player, ItemStack stack, int chamber, float pitch, float yaw) {
        return shootBulletWithMask(player, stack, getPistolMask(stack, chamber), hasEncirclementChamber(stack, chamber), false, false, pitch, yaw);
    }

    private boolean shootBulletWithMask(ServerPlayerEntity player, ItemStack stack, int pistolMask, boolean encirclement, boolean piercingShot, boolean splittingShot, float pitch, float yaw) {
        if (!SexPistolsStaminaUtil.consumeBulletFireStamina(player, pistolMask, encirclement, piercingShot, splittingShot)) {
            player.level.playSound(null, player.blockPosition(), SoundEvents.DISPENSER_FAIL, SoundCategory.PLAYERS, 0.45F, 0.75F);
            return false;
        }
        RevolverBulletEntity bullet = new RevolverBulletEntity(player.level, player);
        bullet.setLoadedSexPistolsMask(pistolMask);
        bullet.setEncirclementBullet(encirclement);
        bullet.setPiercingShotBullet(piercingShot);
        bullet.setSplittingShotBullet(splittingShot);
        float inaccuracy = (isAiming(stack) ? 0.3F : 0.9F) * SexPistolsResolveUtil.getBulletInaccuracyMultiplier(player);
        Vector3d direction = Vector3d.directionFromRotation(pitch, yaw);
        Vector3d spawnPos = player.getEyePosition(1.0F).add(direction.scale(BULLET_SPAWN_FORWARD_OFFSET));
        bullet.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        bullet.xOld = spawnPos.x;
        bullet.yOld = spawnPos.y;
        bullet.zOld = spawnPos.z;
        bullet.shootFromRotation(player, pitch, yaw, 0.0F, BULLET_SPEED, inaccuracy);
        bullet.setBaseDamage(piercingShot ? 8.0D : 10.0D);
        player.level.addFreshEntity(bullet);
        NetworkHandler.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), new RevolverMuzzleFlashPacket(player.getId(), isAiming(stack), player.getRandom().nextFloat()));
        player.level.playSound(null, player.blockPosition(), InitSounds.REVOLVER_FIRE.get(), SoundCategory.PLAYERS, 0.95F, 1.0F);
        if (pistolMask != 0) {
            SexPistolsSoundUtil.sayLoadedFire(player);
        }
        if (!encirclement) {
            applyRecoil(player, stack);
        }
        return true;
    }

    private void applyRecoil(ServerPlayerEntity player, ItemStack stack) {
        float pitchRecoil = getPitchRecoil(stack);
        float yawRecoil = getYawRecoil(stack, player.getRandom().nextFloat());
        player.xRot = MathHelper.clamp(player.xRot - pitchRecoil, -90.0F, 90.0F);
        player.yRot += yawRecoil;
    }

    @OnlyIn(Dist.CLIENT)
    public static void applyClientRecoil(ItemStack stack) {
        Minecraft mc = Minecraft.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null) {
            return;
        }
        RevolverClientEvents.addRecoil(getPitchRecoil(stack), getYawRecoil(stack, player.getRandom().nextFloat()));
    }

    private static float getPitchRecoil(ItemStack stack) {
        return RECOIL_PITCH * (isAiming(stack) ? 0.74F : 1.0F);
    }

    private static float getYawRecoil(ItemStack stack, float randomValue) {
        return (randomValue - 0.5F) * RECOIL_YAW * (isAiming(stack) ? 0.55F : 1.0F);
    }

    private void giveAmmoItem(PlayerEntity player) {
        ItemStack extracted = new ItemStack(InitItems.REVOLVER_AMMO.get());
        if (!player.inventory.add(extracted)) {
            player.drop(extracted, false);
        }
    }
    private boolean consumeAmmoItem(PlayerEntity player) {
        List<ItemStack> items = player.inventory.items;
        for (ItemStack inventoryStack : items) {
            if (inventoryStack.getItem() instanceof RevolverAmmoItem && !inventoryStack.isEmpty()) {
                inventoryStack.shrink(1);
                return true;
            }
        }
        return consumeAmmoFromMistaHat(player);
    }

    private boolean consumeAmmoFromMistaHat(PlayerEntity player) {
        for (ItemStack armorStack : player.inventory.armor) {
            if (MistaSuitArmorItem.consumeOneAmmo(armorStack)) {
                return true;
            }
        }
        for (ItemStack inventoryStack : player.inventory.items) {
            if (MistaSuitArmorItem.consumeOneAmmo(inventoryStack)) {
                return true;
            }
        }
        for (ItemStack offhandStack : player.inventory.offhand) {
            if (MistaSuitArmorItem.consumeOneAmmo(offhandStack)) {
                return true;
            }
        }
        return false;
    }

    private static int findNextEmptyChamber(ItemStack stack, int start) {
        for (int i = 0; i < MAX_AMMO; i++) {
            int chamber = Math.floorMod(start + i, MAX_AMMO);
            if (!hasBullet(stack, chamber)) {
                return chamber;
            }
        }
        return -1;
    }

    private static int findNextExtractableChamber(ItemStack stack, int start) {
        for (int i = 0; i < MAX_AMMO; i++) {
            int chamber = Math.floorMod(start + i, MAX_AMMO);
            if (hasBullet(stack, chamber) || hasSpentCasing(stack, chamber)) {
                return chamber;
            }
        }
        return -1;
    }

    private void releasePistolsFromChamber(ServerPlayerEntity player, ItemStack stack, int chamber) {
        int pistolMask = getPistolMask(stack, chamber);
        if (pistolMask == 0) {
            return;
        }
        IStandPower.getStandPowerOptional(player).ifPresent(power -> SexPistolsStandType.getSexPistolsEntities(power).ifPresent(sexPistols -> sexPistols.recallLoadedPistols(pistolMask, getPistolReturnPosition(player))));
    }

    private static Vector3d getPistolReturnPosition(PlayerEntity player) {
        return player.position().add(0.0D, player.getBbHeight() * 0.65D, 0.0D);
    }

    private static void advanceChamber(ItemStack stack, boolean forward) {
        int offset = forward ? 1 : -1;
        setSelectedChamber(stack, getSelectedChamber(stack) + offset);
    }

    public static int getAmmo(ItemStack stack) {
        normalizeChambers(stack);
        return Integer.bitCount(getChambers(stack));
    }

    public static void setAmmo(ItemStack stack, int ammo) {
        int chambers = 0;
        for (int i = 0; i < MathHelper.clamp(ammo, 0, MAX_AMMO); i++) {
            chambers |= 1 << i;
        }
        setChambers(stack, chambers);
        setSpentChambers(stack, 0);
    }

    public static void consumeAmmo(ItemStack stack, int amount) {
        for (int i = 0; i < amount; i++) {
            int chamber = getSelectedChamber(stack);
            if (hasBullet(stack, chamber)) {
                setChamberLoaded(stack, chamber, false);
                setChamberSpent(stack, chamber, true);
            }
            advanceChamber(stack, true);
        }
    }

    public static boolean isReloadMode(ItemStack stack) {
        return getOrCreateTag(stack).getBoolean(RELOAD_MODE_KEY);
    }

    public static void setReloadMode(ItemStack stack, boolean reloadMode) {
        getOrCreateTag(stack).putBoolean(RELOAD_MODE_KEY, reloadMode);
    }

    public static int getSelectedChamber(ItemStack stack) {
        return Math.floorMod(getOrCreateTag(stack).getInt(SELECTED_CHAMBER_KEY), MAX_AMMO);
    }

    public static void setSelectedChamber(ItemStack stack, int chamber) {
        getOrCreateTag(stack).putInt(SELECTED_CHAMBER_KEY, Math.floorMod(chamber, MAX_AMMO));
    }

    public static int getChambers(ItemStack stack) {
        CompoundNBT tag = getOrCreateTag(stack);
        if (!tag.contains(CHAMBERS_KEY)) {
            int ammo = MathHelper.clamp(tag.getInt(AMMO_KEY), 0, MAX_AMMO);
            int chambers = 0;
            for (int i = 0; i < ammo; i++) {
                chambers |= 1 << i;
            }
            setChambers(stack, chambers);
        }
        return tag.getInt(CHAMBERS_KEY) & FULL_CHAMBERS;
    }

    public static void setChambers(ItemStack stack, int chambers) {
        int normalized = chambers & FULL_CHAMBERS;
        CompoundNBT tag = getOrCreateTag(stack);
        tag.putInt(CHAMBERS_KEY, normalized);
        tag.putInt(AMMO_KEY, Integer.bitCount(normalized));
        tag.putInt(SPENT_CHAMBERS_KEY, tag.getInt(SPENT_CHAMBERS_KEY) & FULL_CHAMBERS & ~normalized);
        tag.putInt(ENCIRCLEMENT_CHAMBERS_KEY, tag.getInt(ENCIRCLEMENT_CHAMBERS_KEY) & FULL_CHAMBERS & normalized);
        tag.putInt(PIERCING_CHAMBERS_KEY, tag.getInt(PIERCING_CHAMBERS_KEY) & FULL_CHAMBERS & normalized);
        tag.putInt(SPLITTING_CHAMBERS_KEY, tag.getInt(SPLITTING_CHAMBERS_KEY) & FULL_CHAMBERS & normalized);
    }

    public static boolean hasBullet(ItemStack stack, int chamber) {
        return (getChambers(stack) & (1 << Math.floorMod(chamber, MAX_AMMO))) != 0;
    }

    public static void setChamberLoaded(ItemStack stack, int chamber, boolean loaded) {
        int mask = 1 << Math.floorMod(chamber, MAX_AMMO);
        int chambers = getChambers(stack);
        setChambers(stack, loaded ? chambers | mask : chambers & ~mask);
        if (loaded) {
            setChamberSpent(stack, chamber, false);
        }
        else {
            clearPistolChamber(stack, chamber);
            clearEncirclementChamber(stack, chamber);
            clearPiercingChamber(stack, chamber);
            clearSplittingChamber(stack, chamber);
        }
    }

    public static int getSpentChambers(ItemStack stack) {
        return getOrCreateTag(stack).getInt(SPENT_CHAMBERS_KEY) & FULL_CHAMBERS & ~getChambers(stack);
    }

    public static void setSpentChambers(ItemStack stack, int spentChambers) {
        getOrCreateTag(stack).putInt(SPENT_CHAMBERS_KEY, spentChambers & FULL_CHAMBERS & ~getChambers(stack));
    }

    public static boolean hasSpentCasing(ItemStack stack, int chamber) {
        return (getSpentChambers(stack) & (1 << Math.floorMod(chamber, MAX_AMMO))) != 0;
    }

    public static void setChamberSpent(ItemStack stack, int chamber, boolean spent) {
        int mask = 1 << Math.floorMod(chamber, MAX_AMMO);
        int spentChambers = getSpentChambers(stack);
        setSpentChambers(stack, spent ? spentChambers | mask : spentChambers & ~mask);
    }


    public static int getEncirclementChambers(ItemStack stack) {
        return getOrCreateTag(stack).getInt(ENCIRCLEMENT_CHAMBERS_KEY) & FULL_CHAMBERS & getChambers(stack);
    }

    public static boolean hasEncirclementChamber(ItemStack stack, int chamber) {
        return (getEncirclementChambers(stack) & (1 << Math.floorMod(chamber, MAX_AMMO))) != 0;
    }

    public static void setEncirclementChamber(ItemStack stack, int chamber, boolean encirclement) {
        int mask = 1 << Math.floorMod(chamber, MAX_AMMO);
        int encirclementChambers = getEncirclementChambers(stack);
        getOrCreateTag(stack).putInt(ENCIRCLEMENT_CHAMBERS_KEY, encirclement ? encirclementChambers | mask : encirclementChambers & ~mask);
    }

    public static void clearEncirclementChamber(ItemStack stack, int chamber) {
        setEncirclementChamber(stack, chamber, false);
    }
    public static int getPiercingChambers(ItemStack stack) {
        return getOrCreateTag(stack).getInt(PIERCING_CHAMBERS_KEY) & FULL_CHAMBERS & getChambers(stack);
    }

    public static boolean hasPiercingChamber(ItemStack stack, int chamber) {
        return (getPiercingChambers(stack) & (1 << Math.floorMod(chamber, MAX_AMMO))) != 0;
    }

    public static void setPiercingChamber(ItemStack stack, int chamber, boolean piercing) {
        int mask = 1 << Math.floorMod(chamber, MAX_AMMO);
        int piercingChambers = getPiercingChambers(stack);
        getOrCreateTag(stack).putInt(PIERCING_CHAMBERS_KEY, piercing ? piercingChambers | mask : piercingChambers & ~mask);
    }

    public static void clearPiercingChamber(ItemStack stack, int chamber) {
        setPiercingChamber(stack, chamber, false);
    }
    public static int getSplittingChambers(ItemStack stack) {
        return getOrCreateTag(stack).getInt(SPLITTING_CHAMBERS_KEY) & FULL_CHAMBERS & getChambers(stack);
    }

    public static boolean hasSplittingChamber(ItemStack stack, int chamber) {
        return (getSplittingChambers(stack) & (1 << Math.floorMod(chamber, MAX_AMMO))) != 0;
    }

    public static void setSplittingChamber(ItemStack stack, int chamber, boolean splitting) {
        int mask = 1 << Math.floorMod(chamber, MAX_AMMO);
        int splittingChambers = getSplittingChambers(stack);
        getOrCreateTag(stack).putInt(SPLITTING_CHAMBERS_KEY, splitting ? splittingChambers | mask : splittingChambers & ~mask);
    }

    public static void clearSplittingChamber(ItemStack stack, int chamber) {
        setSplittingChamber(stack, chamber, false);
    }

    public static int[] getPistolChambers(ItemStack stack) {
        CompoundNBT tag = getOrCreateTag(stack);
        int[] saved = tag.getIntArray(PISTOL_CHAMBERS_KEY);
        int[] pistolChambers = new int[MAX_AMMO];
        for (int i = 0; i < MAX_AMMO && i < saved.length; i++) {
            pistolChambers[i] = saved[i] & FULL_CHAMBERS;
        }
        int chambers = getChambers(stack);
        for (int i = 0; i < MAX_AMMO; i++) {
            if ((chambers & (1 << i)) == 0) {
                pistolChambers[i] = 0;
            }
        }
        if (saved.length != MAX_AMMO) {
            tag.putIntArray(PISTOL_CHAMBERS_KEY, pistolChambers);
        }
        return pistolChambers;
    }

    public static int getPistolMask(ItemStack stack, int chamber) {
        return getPistolChambers(stack)[Math.floorMod(chamber, MAX_AMMO)] & FULL_CHAMBERS;
    }

    public static void setPistolMask(ItemStack stack, int chamber, int pistolMask) {
        int normalizedChamber = Math.floorMod(chamber, MAX_AMMO);
        int[] pistolChambers = getPistolChambers(stack);
        pistolChambers[normalizedChamber] = hasBullet(stack, normalizedChamber) ? pistolMask & FULL_CHAMBERS : 0;
        getOrCreateTag(stack).putIntArray(PISTOL_CHAMBERS_KEY, pistolChambers);
    }

    public static void addPistolToChamber(ItemStack stack, int chamber, int pistolIndex) {
        int normalizedChamber = Math.floorMod(chamber, MAX_AMMO);
        setPistolMask(stack, normalizedChamber, getPistolMask(stack, normalizedChamber) | (1 << Math.floorMod(pistolIndex, MAX_AMMO)));
    }

    public static void clearPistolChamber(ItemStack stack, int chamber) {
        int normalizedChamber = Math.floorMod(chamber, MAX_AMMO);
        int[] pistolChambers = getPistolChambers(stack);
        pistolChambers[normalizedChamber] = 0;
        getOrCreateTag(stack).putIntArray(PISTOL_CHAMBERS_KEY, pistolChambers);
    }


    public static boolean hasPistolAttachments(ItemStack stack) {
        int[] pistolChambers = getPistolChambers(stack);
        for (int pistolMask : pistolChambers) {
            if (pistolMask != 0) {
                return true;
            }
        }
        return false;
    }

    public static void clearAllPistolChambers(ItemStack stack) {
        getOrCreateTag(stack).putIntArray(PISTOL_CHAMBERS_KEY, new int[MAX_AMMO]);
    }
    public static boolean isPistolLoadedInAnyChamber(ItemStack stack, int pistolIndex) {
        int mask = 1 << Math.floorMod(pistolIndex, MAX_AMMO);
        int[] pistolChambers = getPistolChambers(stack);
        for (int i = 0; i < MAX_AMMO; i++) {
            if ((pistolChambers[i] & mask) != 0) {
                return true;
            }
        }
        return false;
    }
    public static void normalizeChambers(ItemStack stack) {
        setSelectedChamber(stack, getSelectedChamber(stack));
        setChambers(stack, getChambers(stack));
        setSpentChambers(stack, getSpentChambers(stack));
        getOrCreateTag(stack).putIntArray(PISTOL_CHAMBERS_KEY, getPistolChambers(stack));
        getOrCreateTag(stack).putInt(PIERCING_CHAMBERS_KEY, getPiercingChambers(stack));
        getOrCreateTag(stack).putInt(SPLITTING_CHAMBERS_KEY, getSplittingChambers(stack));
    }

    public static boolean isAiming(ItemStack stack) {
        return getOrCreateTag(stack).getBoolean(AIM_KEY) && !isReloadMode(stack);
    }

    public static void setAiming(ItemStack stack, boolean aiming) {
        getOrCreateTag(stack).putBoolean(AIM_KEY, aiming && !isReloadMode(stack));
    }

    public static boolean isActivelyAiming(LivingEntity entity, ItemStack stack) {
        return entity != null && !stack.isEmpty() && entity.getUseItem() == stack && entity.getUseItemRemainingTicks() > 0 && isAiming(stack);
    }

    public static float getAimZoomModifier() {
        return AIM_ZOOM_MODIFIER;
    }

    public static int getShotCooldown(ItemStack stack) {
        return getOrCreateTag(stack).getInt(SHOT_COOLDOWN_KEY);
    }

    public static void setShotCooldown(ItemStack stack, int ticks) {
        getOrCreateTag(stack).putInt(SHOT_COOLDOWN_KEY, Math.max(ticks, 0));
    }

    public static int getReloadCooldown(ItemStack stack) {
        return getOrCreateTag(stack).getInt(RELOAD_COOLDOWN_KEY);
    }

    public static void setReloadCooldown(ItemStack stack, int ticks) {
        getOrCreateTag(stack).putInt(RELOAD_COOLDOWN_KEY, Math.max(ticks, 0));
    }

    public static boolean canReload(ItemStack stack) {
        return getReloadCooldown(stack) <= 0;
    }

    public static boolean canReload(LivingEntity user, ItemStack stack) {
        return SexPistolsResolveUtil.hasResolve(user) || canReload(stack);
    }

    public static void applyReloadCooldown(ItemStack stack) {
        setReloadCooldown(stack, RELOAD_COOLDOWN_TICKS);
    }

    public static void applyReloadCooldown(LivingEntity user, ItemStack stack) {
        if (SexPistolsResolveUtil.hasResolve(user)) {
            setReloadCooldown(stack, 0);
        }
        else {
            applyReloadCooldown(stack);
        }
    }

    private static int getShotCooldownTicks(LivingEntity user) {
        return SexPistolsResolveUtil.getShotCooldownTicks(user, SHOT_COOLDOWN_TICKS);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return oldStack.getItem() != newStack.getItem();
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return true;
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        return 1.0D - (double) getAmmo(stack) / (double) MAX_AMMO;
    }

    @Override
    public int getRGBDurabilityForDisplay(ItemStack stack) {
        return isReloadMode(stack) ? 0xE75D2F : 0xC68642;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
        tooltip.add(new TranslationTextComponent("item.nextalbum.revolver.tooltips",
                new KeybindTextComponent("key.mouse.left"),new KeybindTextComponent("key.mouse.right"),new KeybindTextComponent("key.rotp_nextalbum.reload_revolver"),new KeybindTextComponent("key.mouse.middle")).withStyle(TextFormatting.GRAY));
        ClientUtil.addItemReferenceQuote(tooltip, this);
    }

    private static CompoundNBT getOrCreateTag(ItemStack stack) {
        return stack.getOrCreateTag();
    }
}
