package com.nextalubm.rotp_nextalbum.client;

import net.minecraft.util.math.BlockPos;


public final class AgingBlockPosContext {

    private static final ThreadLocal<BlockPos> CURRENT = new ThreadLocal<>();

    private AgingBlockPosContext() {
    }

    public static void set(BlockPos pos) {
        CURRENT.set(pos);
    }

    public static BlockPos get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
