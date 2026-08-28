package com.nextalubm.rotp_nextalbum.util;

public final class AgingRateSnapshot {
    public final float min;
    public final float max;
    public final float zero;
    public final float base;
    public final float actual;

    public AgingRateSnapshot(float min, float max, float zero, float base, float actual) {
        this.min = min;
        this.max = max;
        this.zero = zero;
        this.base = base;
        this.actual = actual;
    }
}
