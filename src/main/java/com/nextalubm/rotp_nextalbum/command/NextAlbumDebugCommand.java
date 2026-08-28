package com.nextalubm.rotp_nextalbum.command;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.nextalubm.rotp_nextalbum.network.AgingRateDebugPacket;
import com.nextalubm.rotp_nextalbum.network.NetworkHandler;
import com.nextalubm.rotp_nextalbum.util.AgingRateSnapshot;
import com.nextalubm.rotp_nextalbum.util.AgingSpeedUtil;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.network.PacketDistributor;

@EventBusSubscriber(modid = "rotp_nextalbum")
public class NextAlbumDebugCommand {
    private static final Set<UUID> AGING_BAR = new HashSet<UUID>();

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(Commands.literal("nextalbum")
                .then(Commands.literal("debug")
                        .then(Commands.literal("aging_bar")
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(ctx -> setAgingBar(ctx.getSource(),
                                                BoolArgumentType.getBool(ctx, "enabled")))))));
    }

    private static int setAgingBar(CommandSource source, boolean enabled) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrException();
        if (enabled) {
            AGING_BAR.add(player.getUUID());
        }
        else {
            AGING_BAR.remove(player.getUUID());
        }
        AgingRateSnapshot snapshot = AgingSpeedUtil.getSnapshot(player.level, player.blockPosition(), player);
        NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new AgingRateDebugPacket(enabled, snapshot));
        source.sendSuccess(new TranslationTextComponent(
                enabled ? "command.rotp_nextalbum.debug.aging_bar.on"
                        : "command.rotp_nextalbum.debug.aging_bar.off"), false);
        return 1;
    }

    public static boolean isAgingBarEnabled(ServerPlayerEntity player) {
        return player != null && AGING_BAR.contains(player.getUUID());
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level.isClientSide()) {
            return;
        }
        if (!(event.player instanceof ServerPlayerEntity)) {
            return;
        }
        ServerPlayerEntity player = (ServerPlayerEntity) event.player;
        if (!AGING_BAR.contains(player.getUUID()) || player.tickCount % 5 != 0) {
            return;
        }
        AgingRateSnapshot snapshot = AgingSpeedUtil.getSnapshot(player.level, player.blockPosition(), player);
        NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new AgingRateDebugPacket(true, snapshot));
    }
}
