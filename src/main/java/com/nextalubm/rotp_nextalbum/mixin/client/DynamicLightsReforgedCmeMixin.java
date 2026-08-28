package com.nextalubm.rotp_nextalbum.mixin.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "me.lambdaurora.lambdynlights.DynamicLightsReforged", remap = false)
public abstract class DynamicLightsReforgedCmeMixin {

    @Redirect(
            method = {"getDynamicLightLevel", "updateAll"},
            at = @At(value = "INVOKE", target = "Ljava/util/Set;iterator()Ljava/util/Iterator;"),
            remap = false,
            require = 0
    )
    private static Iterator<?> rotpNextAlbum$snapshotLightSources(Set<?> sources) {
        if (sources == null) {
            return Collections.emptyIterator();
        }
        try {
            synchronized (sources) {
                return new ArrayList<Object>(sources).iterator();
            }
        } catch (ConcurrentModificationException e) {
            return Collections.emptyIterator();
        }
    }

    @Redirect(
            method = "addLightSource",
            at = @At(value = "INVOKE", target = "Ljava/util/Set;add(Ljava/lang/Object;)Z"),
            remap = false,
            require = 0
    )
    private static boolean rotpNextAlbum$safeAddLightSource(Set<Object> sources, Object light) {
        synchronized (sources) {
            return sources.add(light);
        }
    }
}
