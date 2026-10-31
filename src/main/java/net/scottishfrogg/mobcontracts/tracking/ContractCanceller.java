package net.scottishfrogg.mobcontracts.tracking;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.items.ItemHandlerHelper;
import net.scottishfrogg.mobcontracts.contract.ContractDefinition;
import net.scottishfrogg.mobcontracts.contract.ContractRegistry;
import net.scottishfrogg.mobcontracts.covenant.CovenantManager;
import net.scottishfrogg.mobcontracts.data.PlayerContractData;
import net.scottishfrogg.mobcontracts.network.ModNetwork;
import net.scottishfrogg.mobcontracts.team.ContractTeam;

public final class ContractCanceller {
    private ContractCanceller() {}
    public static boolean cancel(ServerPlayer player) {
        ResourceLocation id = PlayerContractData.getActiveId(player);
        if (id == null) {
            if (ContractTeam.leave(player)) {
                player.displayClientMessage(Component.literal("You stopped helping").withStyle(ChatFormatting.YELLOW), true);
                return true;
            }
            player.displayClientMessage(
                    Component.literal("You have no active contract").withStyle(ChatFormatting.RED), true);
            return false;
        }
        ContractDefinition def = ContractRegistry.get(id);
        ContractTeam.dismissHelpers(player, player.getName().getString() + " canceled the contract");
        CovenantManager.end(player);
        PlayerContractData.clearActive(player);
        if (def == null) {
            ModNetwork.syncActive(player);
            player.displayClientMessage(Component.literal("That contract no longer exists and was discarded")
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }
        ItemHandlerHelper.giveItemToPlayer(player, ContractGiver.createItem(def));
        ModNetwork.syncActive(player);
        player.displayClientMessage(Component.literal(def.name() + " canceled, the item was returned to you")
                .withStyle(ChatFormatting.YELLOW), true);
        return true;
    }
}
