package com.nextalubm.rotp_nextalbum.command;

import java.util.ArrayList;
import java.util.List;

import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.nextalubm.rotp_nextalbum.entity.SexPistolsEntity;
import com.nextalubm.rotp_nextalbum.init.InitEffects;
import com.nextalubm.rotp_nextalbum.init.InitStandEffects;
import com.nextalubm.rotp_nextalbum.init.InitStands;
import com.nextalubm.rotp_nextalbum.stand.SexPistolsEntities;
import com.nextalubm.rotp_nextalbum.stand.SexPistolsStandType;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = "rotp_nextalbum")
public class SexPistolsDebugCommand {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(Commands.literal("spdebug")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("hungry").executes(context -> setHungry(context.getSource(), true)))
                .then(Commands.literal("reset_hunger").executes(context -> setHungry(context.getSource(), false)))
                .then(Commands.literal("joyful")
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 5))
                                .executes(context -> joyful(context.getSource(), IntegerArgumentType.getInteger(context, "level")))))
                .then(Commands.literal("clear_joyful").executes(context -> clearJoyful(context.getSource())))
                .then(Commands.literal("status").executes(context -> status(context.getSource()))));
    }

    private static int setHungry(CommandSource source, boolean hungry) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrException();
        SexPistolsEntities entities = getOrCreatePistolsEntities(player);
        if (entities == null) {
            source.sendSuccess(new StringTextComponent("Sex Pistols debug: player has no Sex Pistols stand"), false);
            return 0;
        }
        entities.debugSetHungry(hungry);
        int pistols = 0;
        for (SexPistolsEntity pistol : getPistols(entities)) {
            pistol.debugSetHungry(hungry);
            pistols++;
        }
        source.sendSuccess(new StringTextComponent("Sex Pistols debug: shared hungry=" + hungry + ", active pistols=" + pistols + ", hungerTicks=" + entities.getHungerTicksDebug()), true);
        return pistols;
    }

    private static int joyful(CommandSource source, int level) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrException();
        player.addEffect(new EffectInstance(InitEffects.JOYFUL.get(), 20 * 60 * 4, Math.max(0, level - 1), false, false, true));
        source.sendSuccess(new StringTextComponent("Sex Pistols debug: applied Joyful level " + level), true);
        return level;
    }

    private static int clearJoyful(CommandSource source) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrException();
        player.removeEffect(InitEffects.JOYFUL.get());
        source.sendSuccess(new StringTextComponent("Sex Pistols debug: cleared Joyful"), true);
        return 1;
    }

    private static int status(CommandSource source) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrException();
        SexPistolsEntities entities = getPistolsEntities(player);
        List<SexPistolsEntity> pistols = entities != null ? getPistols(entities) : new ArrayList<>();
        int hungry = 0;
        int eating = 0;
        StringBuilder builder = new StringBuilder("Sex Pistols debug: sharedHungry=")
                .append(entities != null && entities.isHungryForFood())
                .append(",sharedHungerTicks=")
                .append(entities != null ? entities.getHungerTicksDebug() : -1)
                .append(",pistols=")
                .append(pistols.size());
        for (SexPistolsEntity pistol : pistols) {
            if (pistol.isHungryForFood()) {
                hungry++;
            }
            if (pistol.isEatingItem()) {
                eating++;
            }
            builder.append(" #").append(pistol.getPistolIndex()).append("[hungry=").append(pistol.isHungryForFood())
                    .append(",hungerTicks=").append(pistol.getHungerTicksDebug())
                    .append(",eatTicks=").append(pistol.getEatAnimationTicks()).append("]");
        }
        builder.append(" hungry=").append(hungry).append(" eating=").append(eating);
        source.sendSuccess(new StringTextComponent(builder.toString()), false);
        return pistols.size();
    }

    private static SexPistolsEntities getOrCreatePistolsEntities(ServerPlayerEntity player) {
        IStandPower power = IStandPower.getStandPowerOptional(player).orElse(null);
        if (power == null || !power.hasPower() || power.getType() != InitStands.STAND_SEX_PISTOLS.get()) {
            return null;
        }
        SexPistolsEntities entities = SexPistolsStandType.getSexPistolsEntities(power).orElse(null);
        if (entities == null) {
            entities = new SexPistolsEntities(InitStandEffects.SEX_PISTOLS_ENTITIES.get());
            power.getContinuousEffects().addEffect(entities);
        }
        return entities;
    }

    private static SexPistolsEntities getPistolsEntities(ServerPlayerEntity player) {
        IStandPower power = IStandPower.getStandPowerOptional(player).orElse(null);
        if (power == null || !power.hasPower() || power.getType() != InitStands.STAND_SEX_PISTOLS.get()) {
            return null;
        }
        return SexPistolsStandType.getSexPistolsEntities(power).orElse(null);
    }

    private static List<SexPistolsEntity> getPistols(SexPistolsEntities entities) {
        List<SexPistolsEntity> pistols = new ArrayList<>();
        for (StandEntity entity : entities.getEntityList()) {
            if (entity instanceof SexPistolsEntity && !entity.removed && entity.isAlive()) {
                pistols.add((SexPistolsEntity) entity);
            }
        }
        return pistols;
    }
}