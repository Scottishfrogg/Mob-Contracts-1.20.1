package net.scottishfrogg.mobcontracts.tracking;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.Advancement;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;
import net.scottishfrogg.mobcontracts.advancement.ModTriggers;
import net.scottishfrogg.mobcontracts.config.ContractConfig;
import net.scottishfrogg.mobcontracts.contract.ContractDefinition;
import net.scottishfrogg.mobcontracts.contract.ContractRegistry;
import net.scottishfrogg.mobcontracts.contract.ContractType;
import net.scottishfrogg.mobcontracts.covenant.CovenantManager;
import net.scottishfrogg.mobcontracts.data.PlayerContractData;
import net.scottishfrogg.mobcontracts.item.ContractItem;
import net.scottishfrogg.mobcontracts.item.ModItems;
import net.scottishfrogg.mobcontracts.network.ModNetwork;

import java.util.List;

public final class ContractGiver {
    private ContractGiver() {}
    public static boolean isUnlocked(ServerPlayer player, ContractDefinition def) {
        ResourceLocation unlock = def.unlock();
        if (unlock == null) return true;
        Advancement adv = player.server.getAdvancements().getAdvancement(unlock);
        return adv != null && player.getAdvancements().getOrStartProgress(adv).isDone();
    }
    public static ItemStack createItem(ContractDefinition def) {
        Item item = switch (def.type()) {
            case CONTRACT -> ModItems.CONTRACT.get();
            case BOUNTY -> ModItems.BOUNTY.get();
            case COVENANT -> ModItems.COVENANT.get();
        };
        return ContractItem.create(item, def.id());
    }
    public static boolean isEligible(ServerPlayer player, ContractDefinition def) {
        return !def.targetsPlayer() || CovenantManager.hasOtherOnlinePlayer(player);
    }
    public static boolean giveRandom(ServerPlayer player, ContractType type) {
        List<ContractDefinition> pool = ContractRegistry.byType(type).stream()
                .filter(d -> isUnlocked(player, d) && isEligible(player, d)
                        && !ContractConfig.isDisabledFor(player, d))
                .toList();
        if (pool.isEmpty()) return false;
        ContractDefinition def = pool.get(player.getRandom().nextInt(pool.size()));
        ItemStack stack = createItem(def);
        ItemHandlerHelper.giveItemToPlayer(player, stack);
        if (type == ContractType.BOUNTY) {
            ModTriggers.BOUNTIES_RECEIVED.trigger(player, PlayerContractData.incrementBountiesReceived(player));
        }
        String what = switch (type) {
            case CONTRACT -> "contract";
            case BOUNTY -> "bounty";
            case COVENANT -> "covenant";
        };
        ModNetwork.notify(player, Component.literal("You have received a new " + what).withStyle(ChatFormatting.AQUA));
        return true;
    }
}
