package com.nextalubm.rotp_nextalbum.stand;

import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.power.impl.stand.stats.StandStats;

import net.minecraft.network.PacketBuffer;


public class NextAlbumStandStats extends StandStats {

    public final double abilityRangeBase;
    public final double abilityRangeMax;

    protected NextAlbumStandStats(Builder builder) {
        super(builder);
        this.abilityRangeBase = builder.abilityRangeBase;
        this.abilityRangeMax = builder.abilityRangeMax;
    }

    protected NextAlbumStandStats(PacketBuffer buf) {
        super(buf);
        this.abilityRangeBase = buf.readDouble();
        this.abilityRangeMax = buf.readDouble();
    }

    @Override
    public void write(PacketBuffer buf) {
        super.write(buf);
        buf.writeDouble(abilityRangeBase);
        buf.writeDouble(abilityRangeMax);
    }

    public double getBaseAbilityRange() {
        return abilityRangeBase;
    }

    public double getDevAbilityRange(float devProgress) {
        return devProgress * (abilityRangeMax - abilityRangeBase);
    }

    public double getCurrentAbilityRange(float devProgress) {
        return abilityRangeBase + getDevAbilityRange(devProgress);
    }

    public static double getAbilityRange(IStandPower power) {
        if (power == null || power.getType() == null) {
            return 50.0D;
        }
        StandStats stats = power.getType().getStats();
        if (stats instanceof NextAlbumStandStats) {
            return ((NextAlbumStandStats) stats).getCurrentAbilityRange(power.getStatsDevelopment());
        }
        return 50.0D;
    }

    static {
        registerFactory(NextAlbumStandStats.class, NextAlbumStandStats::new);
    }

    public static class Builder extends AbstractBuilder<Builder, NextAlbumStandStats> {
        private double abilityRangeBase = 50.0D;
        private double abilityRangeMax = 50.0D;

        public Builder abilityRange(double range) {
            return abilityRange(range, range);
        }

        public Builder abilityRange(double base, double max) {
            base = Math.max(base, 0.0D);
            this.abilityRangeBase = base;
            this.abilityRangeMax = Math.max(base, max);
            return getThis();
        }

        @Override
        protected Builder getThis() {
            return this;
        }

        @Override
        protected NextAlbumStandStats createStats() {
            return new NextAlbumStandStats(this);
        }
    }
}
