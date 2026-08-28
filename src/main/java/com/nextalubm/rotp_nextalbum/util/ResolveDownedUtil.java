package com.nextalubm.rotp_nextalbum.util;

import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.init.ModStatusEffects;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.network.PacketManager;
import com.github.standobyte.jojo.network.packets.fromserver.TrResetDeathTimePacket;
import com.nextalubm.rotp_nextalbum.NextAlbumConfig;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.action.TheGratefulDeadAgingAuraAction;
import com.nextalubm.rotp_nextalbum.entity.SexPistolsEntity;
import com.github.standobyte.jojo.util.mod.JojoModUtil;
import com.nextalubm.rotp_nextalbum.init.InitSounds;
import com.nextalubm.rotp_nextalbum.init.InitStands;
import com.nextalubm.rotp_nextalbum.item.ProsciuttoSuitArmorItem;
import com.nextalubm.rotp_nextalbum.network.NetworkHandler;
import com.nextalubm.rotp_nextalbum.network.ResolveDownedSyncPacket;

import net.minecraft.advancements.Advancement;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.PacketDistributor;

public final class ResolveDownedUtil {
    private static final String NBT_KEY = "RotpNextAlbumResolveDowned";
    public static final int DOWNED_POSE_TICKS = 18;
    public static final int COOLDOWN_TICKS = 20 * 60 * 5;
    public static final int GIVE_UP_HOLD_TICKS = 20 * 3;
    public static final int AURA_ACHIEVE_TICKS = 20 * 60;
    public static final int SECONDS_PER_RESOLVE_LEVEL = 30;
    public static final int PROSCIUTTO_BONUS_TICKS = 20 * 60;
    public static final ResourceLocation ADVANCEMENT_ID = new ResourceLocation(NextAlubm.MOD_ID, "glorious_death");
    public static final ResourceLocation LAST_WORDS_ADVANCEMENT_ID = new ResourceLocation(NextAlubm.MOD_ID, "last_words");
    private static final float STAMINA_FOR_FULL_BAR = 100.0F;

    private ResolveDownedUtil() {
    }

    public static boolean isDowned(LivingEntity entity) {
        return entity != null && entity.getPersistentData().getCompound(NBT_KEY).getBoolean("Downed");
    }

    public static int getTicksLeft(LivingEntity entity) {
        return entity.getPersistentData().getCompound(NBT_KEY).getInt("TicksLeft");
    }

    public static int getDuration(LivingEntity entity) {
        int duration = entity.getPersistentData().getCompound(NBT_KEY).getInt("Duration");
        return duration > 0 ? duration : 20 * SECONDS_PER_RESOLVE_LEVEL;
    }

    public static float getProgress(LivingEntity entity) {
        if (!isDowned(entity)) {
            return 0F;
        }
        int duration = getDuration(entity);
        return 1.0F - (getTicksLeft(entity) / (float) duration);
    }

    public static boolean isUnyieldingEnabled() {
        return NextAlbumConfig.getCommonConfigInstance(false).lastActionForAllStands.get();
    }

    public static boolean isSexPistolsUser(LivingEntity entity) {
        IStandPower power = IStandPower.getStandPowerOptional(entity).orElse(null);
        return power != null && power.hasPower() && power.getType() == InitStands.STAND_SEX_PISTOLS.get();
    }

    public static boolean shouldLockSkills(LivingEntity entity) {
        if (entity == null || !isDowned(entity)) {
            return false;
        }
        return entity.getPersistentData().getCompound(NBT_KEY).getBoolean("LockSkills");
    }

    public static boolean isFreeSkillMode(LivingEntity entity) {
        return entity != null && entity.getPersistentData().getCompound(NBT_KEY).getBoolean("FreeSkillMode");
    }

    public static boolean shouldLockAction(LivingEntity user, Object action) {
        if (user == null || !isDowned(user) || !shouldLockSkills(user)) {
            return false;
        }
        return !isAllowedAction(user, action);
    }

    public static boolean isAllowedAction(LivingEntity user, Object action) {
        if (isSexPistolsUser(user)) {
            return false;
        }
        return action instanceof TheGratefulDeadAgingAuraAction;
    }

    public static boolean shouldFadeStand(StandEntity stand) {
        if (stand == null || stand instanceof SexPistolsEntity) {
            return false;
        }
        return isVisualUserDowned(stand.getUser());
    }

    public static float getStandVisualAlpha(StandEntity stand) {
        if (!shouldFadeStand(stand)) {
            return 1.0F;
        }
        float progress = getVisualDownedProgress(stand.getUser());
        return MathHelper.lerp(MathHelper.clamp(progress, 0.0F, 1.0F), 0.72F, 0.28F);
    }

    private static boolean isVisualUserDowned(LivingEntity user) {
        if (user == null) {
            return false;
        }
        if (isDowned(user)) {
            return true;
        }
        if (user.level == null || !user.level.isClientSide()) {
            return false;
        }
        Boolean client = DistExecutor.unsafeCallWhenOn(Dist.CLIENT,
                () -> () -> com.nextalubm.rotp_nextalbum.client.ResolveDownedClient.isDowned(user));
        return Boolean.TRUE.equals(client);
    }

    private static float getVisualDownedProgress(LivingEntity user) {
        if (user == null) {
            return 0.0F;
        }
        if (user.level != null && user.level.isClientSide()) {
            Float client = DistExecutor.unsafeCallWhenOn(Dist.CLIENT,
                    () -> () -> Float.valueOf(com.nextalubm.rotp_nextalbum.client.ResolveDownedClient.getProgress(user)));
            if (client != null && client.floatValue() > 0.0F) {
                return client.floatValue();
            }
        }
        return getProgress(user);
    }

    public static boolean shouldKeepStandAppearance(StandEntity stand) {
        if (!(stand instanceof SexPistolsEntity)) {
            return false;
        }
        return isDowned(stand.getUser());
    }

    public static int computeDuration(ServerPlayerEntity player, IStandPower power) {
        int level = power != null ? power.getResolveLevel() : 0;
        if (level <= 0) {
            return 0;
        }
        int duration = level * 20 * SECONDS_PER_RESOLVE_LEVEL;
        if (ProsciuttoSuitArmorItem.hasFullSet(player)) {
            duration += PROSCIUTTO_BONUS_TICKS;
        }
        return duration;
    }

    public static boolean canStart(ServerPlayerEntity player) {
        if (player == null || player.isCreative() || player.isSpectator()) {
            return false;
        }
        if (player.gameMode.getGameModeForPlayer() == GameType.CREATIVE) {
            return false;
        }
        if (isDowned(player) || getCooldownTicks(player) > 0) {
            return false;
        }
        if (!player.hasEffect(ModStatusEffects.RESOLVE.get())) {
            return false;
        }
        IStandPower power = IStandPower.getStandPowerOptional(player).orElse(null);
        if (power == null || !power.hasPower() || power.getResolveLevel() <= 0) {
            return false;
        }
        if (isUnyieldingEnabled()) {
            return power.willSoulSpawn();
        }
        return power.getType() == InitStands.STAND_THE_GRATEFUL_DEAD.get()
                && InitStands.THE_GRATEFUL_DEAD_AGING_AURA.get().isUnlocked(power);
    }

    public static boolean tryStart(ServerPlayerEntity player, DamageSource source) {
        if (!canStart(player)) {
            return false;
        }
        IStandPower power = IStandPower.getStandPowerOptional(player).orElse(null);
        int duration = computeDuration(player, power);
        if (duration <= 0) {
            return false;
        }
        boolean freeSkillMode = isUnyieldingEnabled();
        boolean lockSkills = !freeSkillMode || isSexPistolsUser(player);

        CompoundNBT tag = getOrCreate(player);
        tag.putBoolean("Downed", true);
        tag.putInt("TicksLeft", duration);
        tag.putInt("Duration", duration);
        tag.putInt("GiveUpTicks", 0);
        tag.putInt("AuraTicks", 0);
        tag.putInt("Cooldown", COOLDOWN_TICKS);
        tag.putBoolean("FreeSkillMode", freeSkillMode);
        tag.putBoolean("LockSkills", lockSkills);
        tag.putBoolean("UsedSkill", false);
        player.getPersistentData().put(NBT_KEY, tag);

        player.setHealth(Math.max(1.0F, player.getMaxHealth() * 0.5F));
        player.deathTime = DOWNED_POSE_TICKS;
        player.hurtTime = 0;
        player.invulnerableTime = 10;
        broadcastDeathMessage(player, source);
        sync(player);
        if (power != null && power.getType() == InitStands.STAND_THE_GRATEFUL_DEAD.get()) {
            JojoModUtil.sayVoiceLine(player, InitSounds.PROSCIUTTO_DEAD.get());
        }
        return true;
    }

    public static void tick(ServerPlayerEntity player) {
        CompoundNBT tag = player.getPersistentData().getCompound(NBT_KEY);
        int cooldown = tag.getInt("Cooldown");
        boolean downed = tag.getBoolean("Downed");
        if (cooldown > 0 && !downed) {
            tag.putInt("Cooldown", cooldown - 1);
            player.getPersistentData().put(NBT_KEY, tag);
        }
        if (!downed) {
            return;
        }
        player.deathTime = DOWNED_POSE_TICKS;
        player.setDeltaMovement(0.0D, Math.min(0.0D, player.getDeltaMovement().y), 0.0D);
        player.zza = 0.0F;
        player.xxa = 0.0F;
        player.setJumping(false);

        IStandPower power = IStandPower.getStandPowerOptional(player).orElse(null);
        if (TheGratefulDeadAgingAuraAction.isAuraActive(power)) {
            tag.putInt("AuraTicks", tag.getInt("AuraTicks") + 1);
        }

        int ticksLeft = tag.getInt("TicksLeft") - 1;
        tag.putInt("TicksLeft", ticksLeft);
        player.getPersistentData().put(NBT_KEY, tag);

        if (player.getHealth() >= player.getMaxHealth() - 0.01F) {
            revive(player);
            return;
        }
        if (ticksLeft <= 0) {
            finishAndDie(player);
            return;
        }
        if (player.tickCount % 10 == 0) {
            sync(player);
        }
    }

    public static void onStaminaConsumed(ServerPlayerEntity player, float amount) {
        if (player == null || amount <= 0.0F || !isDowned(player) || !isFreeSkillMode(player)) {
            return;
        }
        if (shouldLockSkills(player)) {
            return;
        }
        CompoundNBT tag = getOrCreate(player);
        tag.putBoolean("UsedSkill", true);
        int duration = getDuration(player);
        int drain = Math.max(1, Math.round(amount * duration / STAMINA_FOR_FULL_BAR));
        tag.putInt("TicksLeft", Math.max(0, tag.getInt("TicksLeft") - drain));
        player.getPersistentData().put(NBT_KEY, tag);
        if (tag.getInt("TicksLeft") <= 0) {
            finishAndDie(player);
            return;
        }
        if (player.tickCount % 2 == 0) {
            sync(player);
        }
    }

    public static void holdGiveUp(ServerPlayerEntity player, boolean holding) {
        if (!isDowned(player)) {
            return;
        }
        CompoundNBT tag = getOrCreate(player);
        if (!holding) {
            tag.putInt("GiveUpTicks", 0);
            player.getPersistentData().put(NBT_KEY, tag);
            sync(player);
            return;
        }
        int ticks = tag.getInt("GiveUpTicks") + 1;
        tag.putInt("GiveUpTicks", ticks);
        player.getPersistentData().put(NBT_KEY, tag);
        if (ticks >= GIVE_UP_HOLD_TICKS) {
            finishAndDie(player);
            return;
        }
        if (ticks == 1 || ticks % 10 == 0) {
            sync(player);
        }
    }

    public static int getGiveUpTicks(LivingEntity entity) {
        return entity.getPersistentData().getCompound(NBT_KEY).getInt("GiveUpTicks");
    }

    public static void revive(ServerPlayerEntity player) {
        CompoundNBT tag = getOrCreate(player);
        tag.putBoolean("Downed", false);
        tag.putInt("TicksLeft", 0);
        tag.putInt("GiveUpTicks", 0);
        tag.putInt("AuraTicks", 0);
        tag.putBoolean("UsedSkill", false);
        tag.putBoolean("LockSkills", false);
        tag.putBoolean("FreeSkillMode", false);
        player.getPersistentData().put(NBT_KEY, tag);
        player.deathTime = 0;
        AgingRateState rateState = AgingRateState.of(player);
        rateState.clearDownedCool();
        rateState.save(player);
        PacketManager.sendToClientsTrackingAndSelf(new TrResetDeathTimePacket(player.getId()), player);
        sync(player);
    }

    public static void onTrueDeath(ServerPlayerEntity player) {
        boolean glorious = qualifiesForGloriousDeath(player);
        boolean lastWords = qualifiesForLastWords(player);
        clearDowned(player, true);
        if (glorious) {
            awardAdvancement(player, ADVANCEMENT_ID, "glorious_death");
        }
        if (lastWords) {
            awardAdvancement(player, LAST_WORDS_ADVANCEMENT_ID, "last_words");
        }
        syncToAll(player);
    }

    public static void copyOnClone(PlayerEntity original, PlayerEntity clone, boolean wasDeath) {
        CompoundNBT tag = original.getPersistentData().getCompound(NBT_KEY).copy();
        tag.putBoolean("Downed", false);
        tag.putInt("TicksLeft", 0);
        tag.putInt("GiveUpTicks", 0);
        tag.putInt("AuraTicks", 0);
        tag.putBoolean("UsedSkill", false);
        tag.putBoolean("LockSkills", false);
        tag.putBoolean("FreeSkillMode", false);
        if (wasDeath) {
            tag.putInt("Cooldown", 0);
        }
        clone.getPersistentData().put(NBT_KEY, tag);
        if (clone instanceof ServerPlayerEntity) {
            syncToAll((ServerPlayerEntity) clone);
        }
    }

    private static void finishAndDie(ServerPlayerEntity player) {
        boolean glorious = qualifiesForGloriousDeath(player);
        boolean lastWords = qualifiesForLastWords(player);
        clearDowned(player, false);
        syncToAll(player);
        if (glorious) {
            awardAdvancement(player, ADVANCEMENT_ID, "glorious_death");
        }
        if (lastWords) {
            awardAdvancement(player, LAST_WORDS_ADVANCEMENT_ID, "last_words");
        }
        player.hurt(DamageSource.GENERIC, Float.MAX_VALUE);
    }

    private static boolean qualifiesForGloriousDeath(ServerPlayerEntity player) {
        return isDowned(player) && getOrCreate(player).getInt("AuraTicks") >= AURA_ACHIEVE_TICKS;
    }

    private static boolean qualifiesForLastWords(ServerPlayerEntity player) {
        CompoundNBT tag = getOrCreate(player);
        return isDowned(player) && tag.getBoolean("FreeSkillMode") && tag.getBoolean("UsedSkill");
    }

    private static void clearDowned(ServerPlayerEntity player, boolean resetCooldown) {
        CompoundNBT tag = getOrCreate(player);
        tag.putBoolean("Downed", false);
        tag.putInt("TicksLeft", 0);
        tag.putInt("GiveUpTicks", 0);
        tag.putBoolean("LockSkills", false);
        tag.putBoolean("FreeSkillMode", false);
        if (resetCooldown) {
            tag.putInt("Cooldown", 0);
        }
        player.getPersistentData().put(NBT_KEY, tag);
        player.deathTime = 0;
        AgingRateState rateState = AgingRateState.of(player);
        rateState.clearDownedCool();
        rateState.save(player);
    }

    private static void awardAdvancement(ServerPlayerEntity player, ResourceLocation id, String criterion) {
        if (player.getServer() == null) {
            return;
        }
        Advancement advancement = player.getServer().getAdvancements().getAdvancement(id);
        if (advancement != null) {
            player.getAdvancements().award(advancement, criterion);
        }
    }

    private static void broadcastDeathMessage(ServerPlayerEntity player, DamageSource source) {
        ITextComponent message = player.getCombatTracker().getDeathMessage();
        player.server.getPlayerList().broadcastMessage(message, ChatType.SYSTEM, Util.NIL_UUID);
    }

    private static int getCooldownTicks(ServerPlayerEntity player) {
        return player.getPersistentData().getCompound(NBT_KEY).getInt("Cooldown");
    }

    private static CompoundNBT getOrCreate(LivingEntity entity) {
        CompoundNBT tag = entity.getPersistentData().getCompound(NBT_KEY);
        if (tag.isEmpty()) {
            tag = new CompoundNBT();
        }
        return tag;
    }

    public static void sync(ServerPlayerEntity player) {
        sendSync(player, PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player));
    }

    public static void syncToAll(ServerPlayerEntity player) {
        sendSync(player, PacketDistributor.ALL.noArg());
    }

    private static void sendSync(ServerPlayerEntity player, PacketDistributor.PacketTarget target) {
        CompoundNBT tag = player.getPersistentData().getCompound(NBT_KEY);
        NetworkHandler.CHANNEL.send(target, new ResolveDownedSyncPacket(player.getId(), tag.getBoolean("Downed"),
                tag.getInt("TicksLeft"), getDuration(player), tag.getInt("GiveUpTicks"),
                tag.getBoolean("LockSkills")));
    }
}
