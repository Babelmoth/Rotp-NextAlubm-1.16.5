package com.nextalubm.rotp_nextalbum.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.nextalubm.rotp_nextalbum.util.BeachBoyStandUtil;

import com.github.standobyte.jojo.power.impl.stand.IStandPower;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.server.SPlaySoundEffectPacket;
import net.minecraft.network.play.server.SPlaySoundPacket;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.World;

@Mixin(PlayerList.class)
public abstract class PlayerListBeachBoySoundMixin {
    @Shadow
    @Final
    private List<ServerPlayerEntity> players;

    @Inject(method = "broadcast(Lnet/minecraft/entity/player/PlayerEntity;DDDDLnet/minecraft/util/RegistryKey;Lnet/minecraft/network/IPacket;)V",
            at = @At("RETURN"))
    private void rotpNextAlbum$expandBeachBoySound(PlayerEntity except, double x, double y, double z, double radius,
            RegistryKey<World> dimension, IPacket<?> packet, CallbackInfo ci) {
        if (!isSoundPacket(packet) || this.players == null) {
            return;
        }
        double originalSq = radius * radius;
        double expandedSq = BeachBoyStandUtil.SOUND_HEAR_RANGE * BeachBoyStandUtil.SOUND_HEAR_RANGE;
        for (int i = 0; i < this.players.size(); i++) {
            ServerPlayerEntity player = this.players.get(i);
            if (player == except || player.level.dimension() != dimension) {
                continue;
            }
            if (!BeachBoyStandUtil.isSoundSenseEnabled(IStandPower.getStandPowerOptional(player).orElse(null))) {
                continue;
            }
            double distSq = player.distanceToSqr(x, y, z);
            if (distSq < originalSq || distSq >= expandedSq) {
                continue;
            }
            player.connection.send(packet);
        }
    }

    private static boolean isSoundPacket(IPacket<?> packet) {
        return packet instanceof SPlaySoundEffectPacket
                || packet instanceof SPlaySoundPacket;
    }
}
