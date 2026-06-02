package com.nextalubm.rotp_nextalbum.client;

import com.nextalubm.rotp_nextalbum.util.SexPistolsTransferOrder;

public final class SexPistolsTransferOrderClientState {
    private static SexPistolsTransferOrder order = SexPistolsTransferOrder.NONE;

    private SexPistolsTransferOrderClientState() {
    }

    public static SexPistolsTransferOrder getOrder() {
        return order;
    }

    public static void setOrder(SexPistolsTransferOrder order) {
        SexPistolsTransferOrderClientState.order = order != null ? order : SexPistolsTransferOrder.NONE;
    }

    public static void cycle(boolean backwards) {
        order = order.cycle(backwards);
    }
}
