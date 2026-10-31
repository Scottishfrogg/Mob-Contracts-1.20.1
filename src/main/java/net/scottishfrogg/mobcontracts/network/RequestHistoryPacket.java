package net.scottishfrogg.mobcontracts.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record RequestHistoryPacket() {
    public static void encode(RequestHistoryPacket msg, FriendlyByteBuf buf) {
    }
    public static RequestHistoryPacket decode(FriendlyByteBuf buf) {
        return new RequestHistoryPacket();
    }
    public static void handle(RequestHistoryPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer sender = ctx.get().getSender();
        ctx.get().enqueueWork(() -> {
            if (sender != null) ModNetwork.sendHistory(sender, true);
        });
        ctx.get().setPacketHandled(true);
    }
}
