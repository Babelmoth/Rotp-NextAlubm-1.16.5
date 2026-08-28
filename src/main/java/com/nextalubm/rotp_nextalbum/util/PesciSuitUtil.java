package com.nextalubm.rotp_nextalbum.util;

import java.util.UUID;

import javax.annotation.Nullable;

import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.init.ModEntityAttributes;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.power.impl.stand.ResolveCounter;
import com.nextalubm.rotp_nextalbum.item.PesciSuitArmorItem;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.ForgeMod;

public final class PesciSuitUtil {
    public static final String NBT_WATCHED = "RotpNextAlbumPesciInheritedWill";
    public static final String NBT_SOUND = "RotpNextAlbumPesciBrotherSaid";
    public static final String NBT_HAD_MODS = "RotpNextAlbumPesciStandMods";
    public static final double LOOK_RANGE = 16.0D;
    private static final UUID DAMAGE_MOD = UUID.fromString("a11e4c90-2b7d-4f31-9c88-6d0a1b2e3f40");
    private static final UUID SPEED_MOD = UUID.fromString("b22f5d01-3c8e-5042-ad99-7e1b2c3d4e51");
    private static final UUID DURABILITY_MOD = UUID.fromString("c33a6e12-4d9f-6153-beaa-8f2c3d4e5f62");
    private static final UUID PRECISION_MOD = UUID.fromString("d44b7f23-5e0a-7264-cfbb-9a3d4e5f6073");
    private static final UUID RANGE_MOD = UUID.fromString("e55c8034-6f1b-8375-d0cc-ab4e5f607184");
    private static final UUID MOVE_MOD = UUID.fromString("f66d9145-702c-9486-e1dd-bc5f60719295");

    private PesciSuitUtil() {
    }

    public static float getStandStatMultiplier(LivingEntity user) {
        if (!PesciSuitArmorItem.hasFullSet(user)) {
            return 1.0F;
        }
        IStandPower power = IStandPower.getStandPowerOptional(user).orElse(null);
        if (power == null || !power.hasPower()) {
            return 0.80F;
        }
        ResolveCounter counter = power.getResolveCounter();
        float max = counter.getMaxResolveValue();
        float ratio = max > 0.0F ? MathHelper.clamp(counter.getResolveValue() / max, 0.0F, 1.0F) : 0.0F;
        return 0.80F + 0.70F * ratio;
    }

    public static void tick(LivingEntity user) {
        if (user == null || user.level.isClientSide()) {
            return;
        }
        applyStandModifiers(user);
        tickInheritedWill(user);
    }

    public static void applyStandModifiers(LivingEntity user) {
        IStandPower power = IStandPower.getStandPowerOptional(user).orElse(null);
        StandEntity stand = power != null && power.getStandManifestation() instanceof StandEntity
                ? (StandEntity) power.getStandManifestation() : null;
        if (stand == null) {
            if (!PesciSuitArmorItem.hasFullSet(user)) {
                user.getPersistentData().remove(NBT_HAD_MODS);
            }
            return;
        }
        float multiplier = getStandStatMultiplier(user);
        double amount = multiplier - 1.0D;
        if (!PesciSuitArmorItem.hasFullSet(user) || Math.abs(amount) < 0.001D) {
            removeAll(stand);
            user.getPersistentData().remove(NBT_HAD_MODS);
            return;
        }
        user.getPersistentData().putBoolean(NBT_HAD_MODS, true);
        addOrReplace(stand, Attributes.ATTACK_DAMAGE, DAMAGE_MOD, "Pesci mammoni damage", amount);
        addOrReplace(stand, Attributes.ATTACK_SPEED, SPEED_MOD, "Pesci mammoni speed", amount);
        addOrReplace(stand, ModEntityAttributes.STAND_DURABILITY.get(), DURABILITY_MOD, "Pesci mammoni durability", amount);
        addOrReplace(stand, ModEntityAttributes.STAND_PRECISION.get(), PRECISION_MOD, "Pesci mammoni precision", amount);
        addOrReplace(stand, ForgeMod.REACH_DISTANCE.get(), RANGE_MOD, "Pesci mammoni range", amount);
        addOrReplace(stand, Attributes.MOVEMENT_SPEED, MOVE_MOD, "Pesci mammoni move", amount);
    }

    public static boolean shouldHalveResolveDecay(LivingEntity user) {
        if (!PesciSuitArmorItem.hasFullSet(user) || user.level == null) {
            return false;
        }
        CompoundNBT data = user.getPersistentData();
        if (!data.hasUUID(NBT_WATCHED)) {
            return false;
        }
        UUID id = data.getUUID(NBT_WATCHED);
        if (user.level instanceof net.minecraft.world.server.ServerWorld) {
            net.minecraft.entity.Entity entity = ((net.minecraft.world.server.ServerWorld) user.level).getEntity(id);
            return entity instanceof PlayerEntity && ResolveDownedUtil.isDowned((LivingEntity) entity);
        }
        return false;
    }

    private static void tickInheritedWill(LivingEntity user) {
        if (!PesciSuitArmorItem.hasFullSet(user) || !(user instanceof PlayerEntity)) {
            clearWatch(user);
            return;
        }
        PlayerEntity downed = findNearbyDownedPlayer(user);
        CompoundNBT data = user.getPersistentData();
        if (downed == null) {
            if (data.hasUUID(NBT_WATCHED)) {
                UUID id = data.getUUID(NBT_WATCHED);
                net.minecraft.entity.Entity entity = user.level instanceof net.minecraft.world.server.ServerWorld
                        ? ((net.minecraft.world.server.ServerWorld) user.level).getEntity(id) : null;
                if (!(entity instanceof PlayerEntity) || !ResolveDownedUtil.isDowned((LivingEntity) entity)) {
                    clearWatch(user);
                }
            }
            return;
        }
        IStandPower power = IStandPower.getStandPowerOptional(user).orElse(null);
        if (power == null || !power.hasPower()) {
            return;
        }
        boolean firstLook = !data.hasUUID(NBT_WATCHED) || !data.getUUID(NBT_WATCHED).equals(downed.getUUID());
        data.putUUID(NBT_WATCHED, downed.getUUID());
        power.getResolveCounter().soulAddResolveLook();
        if (firstLook && BeachBoyStandUtil.isBeachBoyUser(user) && !data.getBoolean(NBT_SOUND)) {
            data.putBoolean(NBT_SOUND, true);
            BeachBoySoundUtil.sayBrother(user);
        }
    }

    private static void clearWatch(LivingEntity user) {
        CompoundNBT data = user.getPersistentData();
        data.remove(NBT_WATCHED);
        data.remove(NBT_SOUND);
    }

    @Nullable
    private static PlayerEntity findNearbyDownedPlayer(LivingEntity observer) {
        AxisAlignedBB box = observer.getBoundingBox().inflate(LOOK_RANGE);
        java.util.List<PlayerEntity> players = observer.level.getEntitiesOfClass(PlayerEntity.class, box,
                target -> target != observer && target.isAlive() && ResolveDownedUtil.isDowned(target));
        PlayerEntity best = null;
        double bestDist = LOOK_RANGE * LOOK_RANGE;
        for (int i = 0; i < players.size(); i++) {
            PlayerEntity target = players.get(i);
            double dist = observer.distanceToSqr(target);
            if (dist <= bestDist) {
                bestDist = dist;
                best = target;
            }
        }
        return best;
    }

    private static void addOrReplace(LivingEntity entity, Attribute attribute, UUID id, String name, double amount) {
        ModifiableAttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        instance.removeModifier(id);
        instance.addTransientModifier(new AttributeModifier(id, name, amount, Operation.MULTIPLY_TOTAL));
    }

    private static void removeAll(LivingEntity entity) {
        removeModifier(entity, Attributes.ATTACK_DAMAGE, DAMAGE_MOD);
        removeModifier(entity, Attributes.ATTACK_SPEED, SPEED_MOD);
        removeModifier(entity, ModEntityAttributes.STAND_DURABILITY.get(), DURABILITY_MOD);
        removeModifier(entity, ModEntityAttributes.STAND_PRECISION.get(), PRECISION_MOD);
        removeModifier(entity, ForgeMod.REACH_DISTANCE.get(), RANGE_MOD);
        removeModifier(entity, Attributes.MOVEMENT_SPEED, MOVE_MOD);
    }

    private static void removeModifier(LivingEntity entity, Attribute attribute, UUID id) {
        ModifiableAttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null) {
            instance.removeModifier(id);
        }
    }
}
