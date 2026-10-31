package net.scottishfrogg.mobcontracts.rank;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.scottishfrogg.mobcontracts.ContractMod;
import net.scottishfrogg.mobcontracts.config.ContractConfig;

@Mod.EventBusSubscriber(modid = ContractMod.MOD_ID)
public class RankEvents {
    @SubscribeEvent
    public static void onNameFormat(PlayerEvent.NameFormat event) {
        Player player = event.getEntity();
        int total;
        boolean colors;
        if (player instanceof ServerPlayer serverPlayer) {
            total = HunterRanks.total(serverPlayer);
            colors = ContractConfig.RANK_COLORS.get();
        } else if (player.level().isClientSide) {
            Integer synced = ClientRanks.get(player.getUUID());
            if (synced == null) return;
            total = synced;
            colors = ClientRanks.colorsEnabled();
        } else {
            return;
        }
        HunterRank rank = HunterRank.of(total);
        MutableComponent rankText = rank == HunterRank.NONE
                ? Component.literal("None").withStyle(ChatFormatting.DARK_GRAY)
                : Component.literal(rank.title()).withStyle(style -> style.withColor(rank.color()));
        MutableComponent hover = Component.empty()
                .append(event.getUsername().copy().withStyle(ChatFormatting.BOLD))
                .append(Component.literal("\nHunter Rank: ").withStyle(ChatFormatting.GRAY))
                .append(rankText)
                .append(Component.literal("\nHunts completed: " + total).withStyle(ChatFormatting.GRAY));

        boolean recolor = colors && rank != HunterRank.NONE;
        MutableComponent name = event.getDisplayname().copy();
        name.withStyle(style -> {
            Style styled = style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover));
            return recolor ? styled.withColor(rank.color()) : styled;
        });
        event.setDisplayname(name);
    }
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            HunterRanks.syncAll(player.server);
        }
    }
}
