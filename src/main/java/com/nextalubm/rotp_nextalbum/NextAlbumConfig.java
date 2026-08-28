package com.nextalubm.rotp_nextalbum;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.InMemoryCommentedFormat;
import com.github.standobyte.jojo.client.ClientUtil;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.config.ModConfig;

@EventBusSubscriber(modid = NextAlubm.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class NextAlbumConfig {

    public static class Common {

        private boolean loaded = false;

        public final ForgeConfigSpec.DoubleValue bulletDamageMultiplier;
        public final ForgeConfigSpec.BooleanValue isBulletDestroyedBlocks;
        public final ForgeConfigSpec.IntValue blockDamageMemoryTicks;
        public final ForgeConfigSpec.DoubleValue stableDamageEndDistance;
        public final ForgeConfigSpec.DoubleValue closeDamageDropDistance;
        public final ForgeConfigSpec.DoubleValue minDistanceDamage;
        public final ForgeConfigSpec.IntValue maxRicochetCount;
        public final ForgeConfigSpec.DoubleValue recoilMultiplier;
        public final ForgeConfigSpec.DoubleValue meleeDamageMultiplier;

        public final ForgeConfigSpec.BooleanValue resolveAuraForAllStands;
        public final ForgeConfigSpec.BooleanValue standResolveAura;
        public final ForgeConfigSpec.BooleanValue onomatopoeiaParticlesEnabled;
        public final ForgeConfigSpec.BooleanValue resolveAuraOnSummon;
        public final ForgeConfigSpec.BooleanValue agingAuraMistParticles;
        public final ForgeConfigSpec.DoubleValue agingSpeedMultiplier;
        public final ForgeConfigSpec.BooleanValue lastActionForAllStands;
        public final ForgeConfigSpec.DoubleValue sexPistolsTargetSearchRange;
        public final ForgeConfigSpec.DoubleValue sexPistolsTransferAssistRange;
        public final ForgeConfigSpec.IntValue sexPistolsReviveTicks;
        public final ForgeConfigSpec.BooleanValue isSexPistolsHunger;
        public final ForgeConfigSpec.IntValue sexPistolsHungerMinTicks;
        public final ForgeConfigSpec.IntValue sexPistolsHungerExtraTicks;

        private Common(ForgeConfigSpec.Builder builder) {
            this(builder, null);
        }

        private Common(ForgeConfigSpec.Builder builder, @Nullable String mainPath) {
            if (mainPath != null) {
                builder.push(mainPath);
            }

            builder.comment(" Settings for Revolver bullets.").push("Revolver Settings");
                bulletDamageMultiplier = builder
                        .comment("    Global damage multiplier applied to all Revolver bullet attacks.",
                                 "    Affects normal shots, Piercing Shot, Splitting Shot, and Encirclement.",
                                 "    0.0 = no damage, 1.0 = default, 2.0 = double damage.")
                        .translation("rotp_nextalbum.config.bulletDamageMultiplier")
                        .defineInRange("bulletDamageMultiplier", 1.0, 0.0, Double.MAX_VALUE);

              isBulletDestroyedBlocks = builder
                    .comment("    Can bullet attack destroy blocks. Use True or False.",
                            "    Affects normal shots and Piercing Shot.",
                            "     Defaults to True.")
                    .translation("rotp_nextalbum.config.IsBulletDestroyedBlocks")
                    .define("IsBulletDestroyedBlocks", true);
                
                blockDamageMemoryTicks = builder
                        .comment( "Duration of damage inflicted by the bullet on the block. Must be an integer",
                                "    Defaults to 100.")
                        .translation("rotp_nextalbum.config.blockDamageMemoryTicks")
                        .defineInRange("blockDamageMemoryTicks", 100, 0, Integer.MAX_VALUE);
                
                stableDamageEndDistance = builder
                        .comment("    Global stable-range profile for revolver bullets, in blocks.",
                                "     Multiplies each revolver's registered stable range (default 50).",
                                "     Damage stays at the revolver's mid-range value until this distance.")
                        .translation("rotp_nextalbum.config.stableDamageEndDistance")
                        .defineInRange("stableDamageEndDistance", 50.0, 0.0, Double.MAX_VALUE);
                
                closeDamageDropDistance = builder
                        .comment("    Global close-range profile for revolver bullets, in blocks.",
                                "     Multiplies each revolver's registered close range (default 5).",
                                "     Inside this range, bullets deal extra (close) damage.")
                        .translation("rotp_nextalbum.config.closeDamageDropDistance")
                        .defineInRange("closeDamageDropDistance", 5.0, 0.0, Double.MAX_VALUE);
                
                minDistanceDamage = builder
                        .comment("    The minimum damage a bullet can reduce to after traveling a certain distance.",
                                "    Defaults to 1.0.")
                        .translation("rotp_nextalbum.config.minDistanceDamage")
                        .defineInRange("minDistanceDamage", 1.0, 0.0, Double.MAX_VALUE);
                
                maxRicochetCount = builder
                        .comment("    The maximum number of times a bullet can ricochet. Must be an integer.",
                                "    Defaults to 2.0.")
                        .translation("rotp_nextalbum.config.maxRicochetCount")
                        .defineInRange("maxRicochetCount", 2, 0, Integer.MAX_VALUE);

                recoilMultiplier = builder
                        .comment("    Global multiplier applied to revolver camera and visual recoil.",
                                "    1.0 = default. 0.0 disables recoil.")
                        .translation("rotp_nextalbum.config.recoilMultiplier")
                        .defineInRange("recoilMultiplier", 1.0, 0.0, 10.0);

                meleeDamageMultiplier = builder
                        .comment("    Global multiplier applied to revolver Shift+left-click melee damage.",
                                "    1.0 = default.")
                        .translation("rotp_nextalbum.config.meleeDamageMultiplier")
                        .defineInRange("meleeDamageMultiplier", 1.0, 0.0, 10.0);
            builder.pop();
            
            builder.comment("Stand Settings").push("General Settings");

             resolveAuraForAllStands = builder
                    .comment("    Enable resolve aura VFX for all stands.",
                            "     Default is True (If false, the aura will only for Nextalbum Stand)")
                    .translation("rotp_nextalbum.config.resolveAuraForAllStands")
                    .define("resolveAuraForAllStands", false);

            standResolveAura = builder
                    .comment("    Enable resolve aura VFX for stands. So that the aura will also appear on stands.",
                            "     Default is true")
                    .translation("rotp_nextalbum.config.standResolveAura")
                    .define("standResolveAura", true);

            onomatopoeiaParticlesEnabled = builder
                    .comment("    Enable onomatopoeia particles (current only sound particles for step sound. onomatopoeia particles can only seen by Stand Users.).",
                            "     Default is true")
                    .translation("rotp_nextalbum.config.onomatopoeiaParticlesEnabled")
                    .define("onomatopoeiaParticlesEnabled", true);

            resolveAuraOnSummon = builder
                    .comment("    Show resolve aura VFX when a stand is summoned, even without the resolve effect.",
                            "     If resolveAuraForAllStands is false, this only affects NextAlbum stands.",
                            "     Default is false")
                    .translation("rotp_nextalbum.config.resolveAuraOnSummon")
                    .define("resolveAuraOnSummon", false);

            agingAuraMistParticles = builder
                    .comment("    Show The Grateful Dead aging aura mist particles.",
                            "     Default is true")
                    .translation("rotp_nextalbum.config.agingAuraMistParticles")
                    .define("agingAuraMistParticles", true);

            agingSpeedMultiplier = builder
                    .comment("    Global multiplier applied to The Grateful Dead aging speed.",
                            "     1.0 = default. Does not bypass the aging-rate window.")
                    .translation("rotp_nextalbum.config.agingSpeedMultiplier")
                    .defineInRange("agingSpeedMultiplier", 1.0, 0.0, 10.0);

            lastActionForAllStands = builder
                    .comment("    When enabled, any stand user has over 1 level resolve can trigger Last-Action.",
                            "     The Last Action is a mechanism focused on recreating scenes from anime where Stand users sacrifice themselves before their deaths.",
                            "     When you die during resolve effect, you will enter Last-Action state",
                            "     Skills stay usable and the timer drains quickly with stamina.",
                            "     Sex Pistols keep their skills locked and can still pick up items or remote-control stands to help others.",
                            "     Default is false.")
                    .translation("rotp_nextalbum.config.unyieldingForSoulUsers")
                    .define("unyieldingForSoulUsers", false);

            builder.push("Sex Pistols Settings");
                sexPistolsTargetSearchRange = builder
                        .comment("    Maximum distance (in blocks) that Sex Pistols can search for targets when redirecting bullets.",
                                 "     Default is 32.0.")
                        .translation("rotp_nextalbum.config.sexPistolsTargetSearchRange")
                        .defineInRange("sexPistolsTargetSearchRange", 32.0, 0.0, Double.MAX_VALUE);
                
                sexPistolsTransferAssistRange = builder
                        .comment("    Maximum distance (in blocks) for Sex Pistols' transfer assist range (relay kicks between stands).",
                                 "     Default is 18.0.")
                        .translation("rotp_nextalbum.config.sexPistolsTransferAssistRange")
                        .defineInRange("sexPistolsTransferAssistRange", 18.0, 0.0, Double.MAX_VALUE);
                
                sexPistolsReviveTicks = builder
                        .comment("    Time (in ticks) required for a defeated Sex Pistols to revive.",
                                 "     20 ticks = 1 second. Default is 1200 (60 seconds).")
                        .translation("rotp_nextalbum.config.sexPistolsReviveTicks")
                        .defineInRange("sexPistolsReviveTicks", 1200, 1, Integer.MAX_VALUE);
                
                isSexPistolsHunger = builder
                        .comment("    Whether Sex Pistols require feeding over time.",
                                 "     When hungry, they will beg for food and refuse to work until fed.")
                        .translation("rotp_nextalbum.config.isSexPistolsHunger")
                        .define("isSexPistolsHunger", true);
                
                sexPistolsHungerMinTicks = builder
                        .comment("    Minimum time (in ticks) before Sex Pistols become hungry.",
                                 "     20 ticks = 1 second. Default is 6000 (5 minutes).")
                        .translation("rotp_nextalbum.config.sexPistolsHungerMinTicks")
                        .defineInRange("sexPistolsHungerMinTicks", 6000, 100, Integer.MAX_VALUE);
                
                sexPistolsHungerExtraTicks = builder
                        .comment("    Additional random time (in ticks) added to hunger timer.",
                                 "     Total hunger time = min + random(0, extra). Default is 6000 (5 minutes).")
                        .translation("rotp_nextalbum.config.sexPistolsHungerExtraTicks")
                        .defineInRange("sexPistolsHungerExtraTicks", 6000, 0, Integer.MAX_VALUE);
            builder.pop();

            if (mainPath != null) {
                builder.pop();
            }
        }

        public boolean isConfigLoaded() {
            return loaded;
        }

        private void onLoadOrReload() {
            loaded = true;
        }


        public float getBulletDamageMultiplier() {
            return bulletDamageMultiplier.get().floatValue();
        }

        public double getStableDamageEndDistance() {
            return stableDamageEndDistance.get();
        }

        public double getCloseDamageDropDistance() {
            return closeDamageDropDistance.get();
        }

        public double getMinDistanceDamage() {
            return minDistanceDamage.get();
        }

        public float getRecoilMultiplier() {
            return recoilMultiplier.get().floatValue();
        }

        public float getMeleeDamageMultiplier() {
            return meleeDamageMultiplier.get().floatValue();
        }

        public double getSexPistolsTargetSearchRange() {
            return sexPistolsTargetSearchRange.get();
        }

        public double getSexPistolsTransferAssistRange() {
            return sexPistolsTransferAssistRange.get();
        }
    }


    public static class Client {
        public final ForgeConfigSpec.BooleanValue gunSway;
        public final ForgeConfigSpec.DoubleValue gunSwaySensitivity;
        public final ForgeConfigSpec.BooleanValue twoHandedFirstPerson;

        private Client(ForgeConfigSpec.Builder builder) {
            builder.comment("First-person revolver presentation.").push("Revolver Display");
            gunSway = builder
                    .comment("    When looking around, the held revolver lags slightly behind the camera.")
                    .define("gunSway", true);
            gunSwaySensitivity = builder
                    .comment("    How strongly look movement sways the revolver. 0.0 = none, 1.0 = default.")
                    .defineInRange("gunSwaySensitivity", 0.55, 0.0, 2.0);
            twoHandedFirstPerson = builder
                    .comment("    Render both hands on the revolver in first person.")
                    .define("twoHandedFirstPerson", true);
            builder.pop();
        }
    }

    static final ForgeConfigSpec commonSpec;
    static final ForgeConfigSpec clientSpec;
    public static final Client CLIENT;

    private static final Common COMMON_FROM_FILE;


    private static final Common COMMON_SYNCED_TO_CLIENT;

    static {
        final Pair<Common, ForgeConfigSpec> specPair =
                new ForgeConfigSpec.Builder().configure(Common::new);
        commonSpec        = specPair.getRight();
        COMMON_FROM_FILE  = specPair.getLeft();

        final Pair<Common, ForgeConfigSpec> syncedSpecPair =
                new ForgeConfigSpec.Builder().configure(builder -> new Common(builder, "synced"));
        CommentedConfig syncedConfig = CommentedConfig.of(InMemoryCommentedFormat.defaultInstance());
        ForgeConfigSpec syncedSpec = syncedSpecPair.getRight();
        syncedSpec.correct(syncedConfig);
        syncedSpec.setConfig(syncedConfig);
        COMMON_SYNCED_TO_CLIENT = syncedSpecPair.getLeft();

        final Pair<Client, ForgeConfigSpec> clientPair =
                new ForgeConfigSpec.Builder().configure(Client::new);
        clientSpec = clientPair.getRight();
        CLIENT = clientPair.getLeft();
    }

 public static Common getCommonConfigInstance(boolean isClientSide) {
        return isClientSide && !ClientUtil.isLocalServer() ? COMMON_SYNCED_TO_CLIENT : COMMON_FROM_FILE;
    }

    public static void resetConfig() {
        COMMON_SYNCED_TO_CLIENT.bulletDamageMultiplier.clearCache();
        COMMON_SYNCED_TO_CLIENT.isBulletDestroyedBlocks.clearCache();
        COMMON_SYNCED_TO_CLIENT.blockDamageMemoryTicks.clearCache();
        COMMON_SYNCED_TO_CLIENT.stableDamageEndDistance.clearCache();
        COMMON_SYNCED_TO_CLIENT.closeDamageDropDistance.clearCache();
        COMMON_SYNCED_TO_CLIENT.minDistanceDamage.clearCache();
        COMMON_SYNCED_TO_CLIENT.maxRicochetCount.clearCache();
        COMMON_SYNCED_TO_CLIENT.recoilMultiplier.clearCache();
        COMMON_SYNCED_TO_CLIENT.meleeDamageMultiplier.clearCache();

        COMMON_SYNCED_TO_CLIENT.resolveAuraForAllStands.clearCache();
        COMMON_SYNCED_TO_CLIENT.standResolveAura.clearCache();
        COMMON_SYNCED_TO_CLIENT.onomatopoeiaParticlesEnabled.clearCache();
        COMMON_SYNCED_TO_CLIENT.resolveAuraOnSummon.clearCache();
        COMMON_SYNCED_TO_CLIENT.agingAuraMistParticles.clearCache();
        COMMON_SYNCED_TO_CLIENT.agingSpeedMultiplier.clearCache();
        COMMON_SYNCED_TO_CLIENT.lastActionForAllStands.clearCache();
        COMMON_SYNCED_TO_CLIENT.sexPistolsTargetSearchRange.clearCache();
        COMMON_SYNCED_TO_CLIENT.sexPistolsTransferAssistRange.clearCache();
        COMMON_SYNCED_TO_CLIENT.sexPistolsReviveTicks.clearCache();
        COMMON_SYNCED_TO_CLIENT.isSexPistolsHunger.clearCache();
        COMMON_SYNCED_TO_CLIENT.sexPistolsHungerMinTicks.clearCache();
        COMMON_SYNCED_TO_CLIENT.sexPistolsHungerExtraTicks.clearCache();
    }



    public static Common getCommonConfig(boolean isClientSide) {
        return isClientSide && !ClientUtil.isLocalServer()
                ? COMMON_SYNCED_TO_CLIENT
                : COMMON_FROM_FILE;
    }


    public static Common getCommonConfig() {
        return COMMON_FROM_FILE;
    }



    public static void register(ModLoadingContext context) {
        context.registerConfig(ModConfig.Type.COMMON, commonSpec);
        context.registerConfig(ModConfig.Type.CLIENT, clientSpec);
    }



    @SubscribeEvent
    public static void onConfigLoad(ModConfig.ModConfigEvent event) {
        ModConfig config = event.getConfig();
        if (NextAlubm.MOD_ID.equals(config.getModId())
                && config.getType() == ModConfig.Type.COMMON) {
            COMMON_FROM_FILE.onLoadOrReload();
        }
    }
}
