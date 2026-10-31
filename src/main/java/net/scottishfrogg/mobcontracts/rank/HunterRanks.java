package net.scottishfrogg.mobcontracts.rank;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.PacketDistributor;
import net.scottishfrogg.mobcontracts.config.ContractConfig;
import net.scottishfrogg.mobcontracts.data.PlayerContractData;
import net.scottishfrogg.mobcontracts.network.ModNetwork;
import net.scottishfrogg.mobcontracts.network.RankSyncPacket;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class HunterRanks {
    private HunterRanks() {}
    public static int total(Player player) {
        return PlayerContractData.getCompleted(player)
                + PlayerContractData.getBountiesCompleted(player)
                + PlayerContractData.getCovenantsCompleted(player);
    }
    public static void syncAll(MinecraftServer server) {
        Map<UUID, Integer> totals = new HashMap<>();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) totals.put(p.getUUID(), total(p));
        ModNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(),
                new RankSyncPacket(totals, ContractConfig.RANK_COLORS.get()));
    }
    public static void onCompletion(ServerPlayer player) {
        int total = total(player);
        HunterRank now = HunterRank.of(total);
        if (now != HunterRank.of(total - 1)) {
            Component title = Component.literal(now.title()).withStyle(style -> style.withColor(now.color()));
            ModNetwork.notify(player, Component.literal("Rank up: ").withStyle(ChatFormatting.GOLD).append(title));
            player.server.getPlayerList().broadcastSystemMessage(
                    Component.empty().append(player.getDisplayName())
                            .append(Component.literal(" reached the rank of ").withStyle(ChatFormatting.GRAY))
                            .append(title).append(Component.literal("!").withStyle(ChatFormatting.GRAY)),
                    false);
        }
        syncAll(player.server);
    }
}
