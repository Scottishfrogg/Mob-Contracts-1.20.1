package net.scottishfrogg.mobcontracts.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.scottishfrogg.mobcontracts.client.ClientContractState;
import net.scottishfrogg.mobcontracts.data.PlayerContractData;
import net.scottishfrogg.mobcontracts.team.ContractTeam;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public record ActiveContractPacket(@Nullable ResourceLocation id, int[] progress, int towardBounty,
                                   boolean bountiesUnlocked, String targetName, String helpingName) {
    public static ActiveContractPacket of(ServerPlayer player) {
        ServerPlayer source = ContractTeam.displaySourceFor(player);
        String helping = source == player ? "" : source.getName().getString();
        return new ActiveContractPacket(
                PlayerContractData.getActiveId(source),
                PlayerContractData.getProgress(source),
                PlayerContractData.getTowardBounty(player),
                PlayerContractData.bountiesUnlocked(player),
                PlayerContractData.getCovenantTargetName(source),
                helping);
    }
    public static void encode(ActiveContractPacket msg, FriendlyByteBuf buf) {
        buf.writeNullable(msg.id, FriendlyByteBuf::writeResourceLocation);
        buf.writeVarIntArray(msg.progress);
        buf.writeVarInt(msg.towardBounty);
        buf.writeBoolean(msg.bountiesUnlocked);
        buf.writeUtf(msg.targetName);
        buf.writeUtf(msg.helpingName);
    }
    public static ActiveContractPacket decode(FriendlyByteBuf buf) {
        return new ActiveContractPacket(buf.readNullable(FriendlyByteBuf::readResourceLocation),
                buf.readVarIntArray(), buf.readVarInt(), buf.readBoolean(), buf.readUtf(), buf.readUtf());
    }
    public static void handle(ActiveContractPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientContractState.set(msg.id, msg.progress, msg.towardBounty,
                msg.bountiesUnlocked, msg.targetName, msg.helpingName));
        ctx.get().setPacketHandled(true);
    }
}
