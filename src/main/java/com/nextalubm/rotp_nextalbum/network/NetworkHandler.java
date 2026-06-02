package com.nextalubm.rotp_nextalbum.network;

import java.util.concurrent.atomic.AtomicInteger;

import com.nextalubm.rotp_nextalbum.NextAlubm;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    private static final AtomicInteger ID = new AtomicInteger(0);

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(NextAlubm.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    public static void init() {
        CHANNEL.registerMessage(ID.getAndIncrement(), ShootRevolverPacket.class,
                ShootRevolverPacket::encode, ShootRevolverPacket::decode, ShootRevolverPacket::handle);
        CHANNEL.registerMessage(ID.getAndIncrement(), RevolverMeleeAttackPacket.class,
                RevolverMeleeAttackPacket::encode, RevolverMeleeAttackPacket::decode, RevolverMeleeAttackPacket::handle);
        CHANNEL.registerMessage(ID.getAndIncrement(), ReloadRevolverPacket.class,
                ReloadRevolverPacket::encode, ReloadRevolverPacket::decode, ReloadRevolverPacket::handle);
        CHANNEL.registerMessage(ID.getAndIncrement(), MistaHatAmmoClickPacket.class,
                MistaHatAmmoClickPacket::encode, MistaHatAmmoClickPacket::decode, MistaHatAmmoClickPacket::handle);
        CHANNEL.registerMessage(ID.getAndIncrement(), FillRevolverChamberPacket.class,
                FillRevolverChamberPacket::encode, FillRevolverChamberPacket::decode, FillRevolverChamberPacket::handle);
        CHANNEL.registerMessage(ID.getAndIncrement(), SwitchRevolverChamberPacket.class,
                SwitchRevolverChamberPacket::encode, SwitchRevolverChamberPacket::decode, SwitchRevolverChamberPacket::handle);
        CHANNEL.registerMessage(ID.getAndIncrement(), EjectRevolverCasingsPacket.class,
                EjectRevolverCasingsPacket::encode, EjectRevolverCasingsPacket::decode, EjectRevolverCasingsPacket::handle);
        CHANNEL.registerMessage(ID.getAndIncrement(), SpawnRevolverCasingPacket.class,
                SpawnRevolverCasingPacket::encode, SpawnRevolverCasingPacket::decode, SpawnRevolverCasingPacket::handle);
        CHANNEL.registerMessage(ID.getAndIncrement(), RevolverMuzzleFlashPacket.class,
                RevolverMuzzleFlashPacket::encode, RevolverMuzzleFlashPacket::decode, RevolverMuzzleFlashPacket::handle);
        CHANNEL.registerMessage(ID.getAndIncrement(), SwitchSexPistolsSelectionPacket.class,
                SwitchSexPistolsSelectionPacket::encode, SwitchSexPistolsSelectionPacket::decode, SwitchSexPistolsSelectionPacket::handle);
        CHANNEL.registerMessage(ID.getAndIncrement(), SexPistolsSummonAnimationPacket.class,
                SexPistolsSummonAnimationPacket::encode, SexPistolsSummonAnimationPacket::decode, SexPistolsSummonAnimationPacket::handle);
        CHANNEL.registerMessage(ID.getAndIncrement(), SexPistolsKickAnimationPacket.class,
                SexPistolsKickAnimationPacket::encode, SexPistolsKickAnimationPacket::decode, SexPistolsKickAnimationPacket::handle);
        CHANNEL.registerMessage(ID.getAndIncrement(), SexPistolsKickMuzzleFlashPacket.class,
                SexPistolsKickMuzzleFlashPacket::encode, SexPistolsKickMuzzleFlashPacket::decode, SexPistolsKickMuzzleFlashPacket::handle);
        CHANNEL.registerMessage(ID.getAndIncrement(), SexPistolsScoutGlowPacket.class,
                SexPistolsScoutGlowPacket::encode, SexPistolsScoutGlowPacket::decode, SexPistolsScoutGlowPacket::handle);
        CHANNEL.registerMessage(ID.getAndIncrement(), SexPistolsTargetModePacket.class,
                SexPistolsTargetModePacket::encode, SexPistolsTargetModePacket::decode, SexPistolsTargetModePacket::handle);
        CHANNEL.registerMessage(ID.getAndIncrement(), AgingBlockUpdatePacket.class,
                AgingBlockUpdatePacket::encode, AgingBlockUpdatePacket::decode, AgingBlockUpdatePacket::handle);
        CHANNEL.registerMessage(ID.getAndIncrement(), AgingBlockClearPacket.class,
                AgingBlockClearPacket::encode, AgingBlockClearPacket::decode, AgingBlockClearPacket::handle);
    }
}
