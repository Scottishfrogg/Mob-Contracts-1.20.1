package net.scottishfrogg.mobcontracts.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.scottishfrogg.mobcontracts.client.HuntHistoryClient;
import net.scottishfrogg.mobcontracts.config.ContractConfig;
import net.scottishfrogg.mobcontracts.contract.ContractDefinition;
import net.scottishfrogg.mobcontracts.contract.ContractRegistry;
import net.scottishfrogg.mobcontracts.data.PlayerContractData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public record HistorySyncPacket(List<Entry> entries, int contracts, int bounties, int covenants, boolean open) {
    public record Entry(ResourceLocation id, int count, boolean disabled, boolean canToggle) {}
    public static HistorySyncPacket of(ServerPlayer player, boolean open) {
        List<PlayerContractData.HistoryEntry> history = new ArrayList<>(PlayerContractData.getHistory(player));
        Collections.reverse(history);
        List<Entry> entries = history.stream()
                .map(h -> {
                    ContractDefinition def = ContractRegistry.get(h.id());
                    boolean can = def == null || ContractConfig.canDisable(def.type());
                    return new Entry(h.id(), h.count(), can && PlayerContractData.isDisabled(player, h.id()), can);
                })
                .toList();
        return new HistorySyncPacket(entries,
                PlayerContractData.getCompleted(player),
                PlayerContractData.getBountiesCompleted(player),
                PlayerContractData.getCovenantsCompleted(player),
                open);
    }
    public static void encode(HistorySyncPacket msg, FriendlyByteBuf buf) {
        buf.writeCollection(msg.entries, (b, e) -> {
            b.writeResourceLocation(e.id());
            b.writeVarInt(e.count());
            b.writeBoolean(e.disabled());
            b.writeBoolean(e.canToggle());
        });
        buf.writeVarInt(msg.contracts);
        buf.writeVarInt(msg.bounties);
        buf.writeVarInt(msg.covenants);
        buf.writeBoolean(msg.open);
    }
    public static HistorySyncPacket decode(FriendlyByteBuf buf) {
        List<Entry> entries = buf.readCollection(ArrayList::new,
                b -> new Entry(b.readResourceLocation(), b.readVarInt(), b.readBoolean(), b.readBoolean()));
        return new HistorySyncPacket(entries, buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readBoolean());
    }
    public static void handle(HistorySyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> HuntHistoryClient.handle(msg)));
        ctx.get().setPacketHandled(true);
    }
}
