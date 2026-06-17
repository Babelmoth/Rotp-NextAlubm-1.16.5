package com.nextalubm.rotp_nextalbum.stand;

import java.util.Optional;
import java.util.Random;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.github.standobyte.jojo.advancements.ModCriteriaTriggers;
import com.github.standobyte.jojo.client.ClientUtil;
import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.entity.stand.StandEntityType;
import com.github.standobyte.jojo.entity.stand.StandRelativeOffset;
import com.github.standobyte.jojo.power.impl.stand.IStandManifestation;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.power.impl.stand.StandUtil;
import com.github.standobyte.jojo.power.impl.stand.stats.StandStats;
import com.github.standobyte.jojo.power.impl.stand.type.EntityStandType;
import com.github.standobyte.jojo.util.mod.JojoModUtil;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.client.SexPistolsSkinHelper;
import com.nextalubm.rotp_nextalbum.entity.SexPistolsEntity;
import com.nextalubm.rotp_nextalbum.init.InitSounds;
import com.nextalubm.rotp_nextalbum.init.InitStandEffects;
import com.nextalubm.rotp_nextalbum.init.InitStands;
import com.nextalubm.rotp_nextalbum.network.NetworkHandler;
import com.nextalubm.rotp_nextalbum.network.SexPistolsSummonAnimationPacket;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.PacketDistributor;

public class SexPistolsStandType extends EntityStandType<StandStats> {
    private static final Random RANDOM = new Random();
    private static final ResourceLocation[] PISTOL_ICONS = {
            new ResourceLocation(NextAlubm.MOD_ID, "textures/gui/sex_pistols_no_1.png"),
            new ResourceLocation(NextAlubm.MOD_ID, "textures/gui/sex_pistols_no_2.png"),
            new ResourceLocation(NextAlubm.MOD_ID, "textures/gui/sex_pistols_no_3.png"),
            new ResourceLocation(NextAlubm.MOD_ID, "textures/gui/sex_pistols_no_5.png"),
            new ResourceLocation(NextAlubm.MOD_ID, "textures/gui/sex_pistols_no_6.png"),
            new ResourceLocation(NextAlubm.MOD_ID, "textures/gui/sex_pistols_no_7.png")
    };

    public SexPistolsStandType(AbstractBuilder<?, StandStats> builder) {
        super(builder);
    }

    @Override
    public ResourceLocation getIconTexture(@Nullable IStandPower power) {
        int iconIndex = 0;
        if (power != null && power.getStandManifestation() instanceof SexPistolsEntity) {
            int pistolIndex = ((SexPistolsEntity) power.getStandManifestation()).getPistolIndex();
            if (pistolIndex >= 0 && pistolIndex < PISTOL_ICONS.length) {
                iconIndex = pistolIndex;
            }
        }
        return SexPistolsSkinHelper.getIconTexture(power, PISTOL_ICONS[iconIndex]);
    }

    @Override
    public boolean summon(LivingEntity user, IStandPower standPower, Consumer<StandEntity> beforeTheSummon, boolean withoutNameVoiceLine, boolean addToWorld) {
        if (!standPower.canUsePower()) {
            return false;
        }
        if (!withoutNameVoiceLine && !user.isShiftKeyDown()) {
            JojoModUtil.sayVoiceLine(user, InitSounds.MISTA_SEX_PISTOLS.get());
        }

        triggerAdvancement(standPower, standPower.getStandManifestation());
        if (!user.level.isClientSide()) {
            SexPistolsEntities summonedEntities = getSexPistolsEntities(standPower).orElse(null);
            if (summonedEntities == null) {
                summonedEntities = new SexPistolsEntities(InitStandEffects.SEX_PISTOLS_ENTITIES.get());
                standPower.getContinuousEffects().addEffect(summonedEntities);
            }
            summonedEntities.setSummoned(false);
            summonedEntities.setSummoned(true);

            boolean anySummoned = false;
            double baseAngle = RANDOM.nextDouble() * Math.PI * 2.0D;
            for (int i = 0; i < InitStands.SEX_PISTOLS_ENTITY_TYPES.size(); i++) {
                if (!summonedEntities.isPistolAvailable(i)) {
                    continue;
                }
                StandEntityType<?> entityType = InitStands.SEX_PISTOLS_ENTITY_TYPES.get(i).get();
                SexPistolsEntity standEntity = (SexPistolsEntity) entityType.create(user.level);
                double angle = baseAngle + Math.PI * 2.0D * (double) i / (double) InitStands.SEX_PISTOLS_ENTITY_TYPES.size() + (RANDOM.nextDouble() - 0.5D) * 0.45D;
                double radius = 0.85D + RANDOM.nextDouble() * 0.55D;
                double left = Math.cos(angle) * radius;
                double forward = Math.sin(angle) * radius;
                double layer = (double) (i % 3) / 2.0D;
                double y = 0.15D + layer * 1.15D + (RANDOM.nextDouble() - 0.5D) * 0.35D;
                standEntity.setPistolIndex(i);
                standEntity.setDefaultOffsetFromUser(StandRelativeOffset.withYOffset(left, y, forward));
                standEntity.copyPosition(user);
                standEntity.setUserAndPower(user, standPower);
                standEntity.playSummonAnimation();
                anySummoned = true;
                summonedEntities.addEntity(standEntity);
                beforeTheSummon.accept(standEntity);
                if (addToWorld) {
                    finalizeStandSummonFromAction(user, standPower, standEntity, true);
                    NetworkHandler.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> standEntity), new SexPistolsSummonAnimationPacket(standEntity.getId(), standEntity.getIdleAnimationVariant(), standEntity.getSummonAnimationVariant()));
                }
                standEntity.onStandSummonServerSide();
            }
            if (!anySummoned) {
                summonedEntities.setSummoned(false);
                return false;
            }
            summonedEntities.onSummon();
        }
        return true;

    }

    protected void triggerAdvancement(IStandPower standPower, IStandManifestation stand) {
        if (standPower.getUser() instanceof ServerPlayerEntity) {
            ModCriteriaTriggers.SUMMON_STAND.get().trigger((ServerPlayerEntity) standPower.getUser(), standPower);
        }
    }

    @Override
    public void unsummon(LivingEntity user, IStandPower standPower) {
        if (!user.level.isClientSide()) {
            getSexPistolsEntities(standPower).ifPresent(sexPistols -> {
                for (StandEntity standEntity : sexPistols.getEntities()) {
                    if (standEntity instanceof SexPistolsEntity) {
                        ((SexPistolsEntity) standEntity).prepareSexPistolsUnsummon();
                    }
                    if (standEntity.isManuallyControlled() || standEntity.isRemotePositionFixed()) {
                        standEntity.setManualControl(false, false);
                    }
                    if (!standEntity.isBeingRetracted()) {
                        standEntity.retractStand(true);
                    }
                }
            });
        }
    }

    @Override
    public void forceUnsummon(LivingEntity user, IStandPower standPower) {
        if (!user.level.isClientSide()) {
            getSexPistolsEntities(standPower).ifPresent(sexPistols -> sexPistols.setSummoned(false));
        }
        else if (user.is(ClientUtil.getClientPlayer())) {
            StandUtil.setManualControl(ClientUtil.getClientPlayer(), false, false);
        }
    }

    @Override
    public void onStandSkinSet(IStandPower power, Optional<ResourceLocation> skin) {
        super.onStandSkinSet(power, skin);
        getSexPistolsEntities(power).ifPresent(sexPistols -> {
            for (StandEntity standEntity : sexPistols.getEntities()) {
                standEntity.setStandSkin(skin);
            }
        });
    }

    public static Optional<SexPistolsEntities> getSexPistolsEntities(IStandPower standPower) {
        return standPower.getContinuousEffects().getEffects()
                .filter(effect -> effect.effectType == InitStandEffects.SEX_PISTOLS_ENTITIES.get())
                .map(effect -> (SexPistolsEntities) effect)
                .findFirst();
    }
}