package net.scottishfrogg.mobcontracts.network;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.scottishfrogg.mobcontracts.ContractMod;
import net.scottishfrogg.mobcontracts.contract.ContractRegistry;
import net.scottishfrogg.mobcontracts.team.ContractTeam;

public final class ModNetwork {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ContractMod.MOD_ID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
    private ModNetwork() {}
    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, DefinitionSyncPacket.class,
                DefinitionSyncPacket::encode, DefinitionSyncPacket::decode, DefinitionSyncPacket::handle);
        CHANNEL.registerMessage(id++, ActiveContractPacket.class,
                ActiveContractPacket::encode, ActiveContractPacket::decode, ActiveContractPacket::handle);
        CHANNEL.registerMessage(id++, NotificationPacket.class,
                NotificationPacket::encode, NotificationPacket::decode, NotificationPacket::handle);
        CHANNEL.registerMessage(id++, CancelContractPacket.class,
                CancelContractPacket::encode, CancelContractPacket::decode, CancelContractPacket::handle);
        CHANNEL.registerMessage(id++, RequestHistoryPacket.class,
                RequestHistoryPacket::encode, RequestHistoryPacket::decode, RequestHistoryPacket::handle);
        CHANNEL.registerMessage(id++, ToggleDisabledPacket.class,
                ToggleDisabledPacket::encode, ToggleDisabledPacket::decode, ToggleDisabledPacket::handle);
        CHANNEL.registerMessage(id++, HistorySyncPacket.class,
                HistorySyncPacket::encode, HistorySyncPacket::decode, HistorySyncPacket::handle);
        CHANNEL.registerMessage(id++, RankSyncPacket.class,
                RankSyncPacket::encode, RankSyncPacket::decode, RankSyncPacket::handle);
    }
    public static void syncDefinitions(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new DefinitionSyncPacket(ContractRegistry.all()));
    }
    public static void sendHistory(ServerPlayer player, boolean open) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), HistorySyncPacket.of(player, open));
    }
    public static void syncActive(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), ActiveContractPacket.of(player));
        for (ServerPlayer helper : ContractTeam.helpersOf(player)) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> helper), ActiveContractPacket.of(helper));
        }
    }
    public static void notify(ServerPlayer player, Component message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new NotificationPacket(message));
    }
}
