package net.scottishfrogg.mobcontracts.client;

import net.minecraft.client.Minecraft;
import net.scottishfrogg.mobcontracts.network.HistorySyncPacket;

public final class HuntHistoryClient {
    private HuntHistoryClient() {}
    public static void handle(HistorySyncPacket msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof HuntHistoryScreen screen) {
            screen.update(msg);
        } else if (msg.open()) {
            mc.setScreen(new HuntHistoryScreen(msg));
        }
    }
}
