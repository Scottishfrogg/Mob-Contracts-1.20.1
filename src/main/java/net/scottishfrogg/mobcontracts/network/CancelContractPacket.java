package net.scottishfrogg.mobcontracts.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.scottishfrogg.mobcontracts.tracking.ContractCanceller;

import java.util.function.Supplier;

public record CancelContractPacket() {
    public static void encode(CancelContractPacket msg, FriendlyByteBuf buf) {
    }
    public static CancelContractPacket decode(FriendlyByteBuf buf) {
        return new CancelContractPacket();
    }
    public static void handle(CancelContractPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer sender = ctx.get().getSender();
        ctx.get().enqueueWork(() -> {
            if (sender != null) ContractCanceller.cancel(sender);
        });
        ctx.get().setPacketHandled(true);
    }
}
