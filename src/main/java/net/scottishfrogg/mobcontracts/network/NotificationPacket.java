package net.scottishfrogg.mobcontracts.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;
import net.scottishfrogg.mobcontracts.client.ClientNotifications;

import java.util.function.Supplier;

public record NotificationPacket(Component message) {
    public static void encode(NotificationPacket msg, FriendlyByteBuf buf) {
        buf.writeComponent(msg.message);
    }
    public static NotificationPacket decode(FriendlyByteBuf buf) {
        return new NotificationPacket(buf.readComponent());
    }
    public static void handle(NotificationPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientNotifications.push(msg.message));
        ctx.get().setPacketHandled(true);
    }
}
