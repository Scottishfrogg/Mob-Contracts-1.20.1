package net.scottishfrogg.mobcontracts.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.scottishfrogg.mobcontracts.rank.ClientRanks;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public record RankSyncPacket(Map<UUID, Integer> totals, boolean colors) {
    public static void encode(RankSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeMap(msg.totals, FriendlyByteBuf::writeUUID, FriendlyByteBuf::writeVarInt);
        buf.writeBoolean(msg.colors);
    }
    public static RankSyncPacket decode(FriendlyByteBuf buf) {
        return new RankSyncPacket(buf.readMap(FriendlyByteBuf::readUUID, FriendlyByteBuf::readVarInt), buf.readBoolean());
    }
    public static void handle(RankSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientRanks.set(msg.totals, msg.colors));
        ctx.get().setPacketHandled(true);
    }
}
