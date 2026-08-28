package com.nextalubm.rotp_nextalbum.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.nextalubm.rotp_nextalbum.util.SexPistolsForcedTarget;

public final class SexPistolsForcedTargetClientState {
    private static List<SexPistolsForcedTarget> targets = Collections.emptyList();

    private SexPistolsForcedTargetClientState() {
    }

    public static void setTargets(List<SexPistolsForcedTarget> next) {
        targets = next != null ? new ArrayList<>(next) : Collections.emptyList();
    }

    public static List<SexPistolsForcedTarget> getTargets() {
        return targets;
    }
}
