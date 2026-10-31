package net.scottishfrogg.mobcontracts.tracking;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.scottishfrogg.mobcontracts.ContractMod;
import net.scottishfrogg.mobcontracts.config.ContractConfig;
import net.scottishfrogg.mobcontracts.contract.ContractRegistry;
import net.scottishfrogg.mobcontracts.contract.ContractType;
import net.scottishfrogg.mobcontracts.data.PlayerContractData;
import net.scottishfrogg.mobcontracts.network.ModNetwork;

@Mod.EventBusSubscriber(modid = ContractMod.MOD_ID)
public class DeliveryEvents {
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.player instanceof ServerPlayer player)
                || player.tickCount % 20 != 0) return;
        long now = player.server.overworld().getDayTime();
        Long last = PlayerContractData.getLastDeliveryTime(player);
        Long lastCovenant = PlayerContractData.getLastCovenantRoll(player);
        if (last == null || now < last) {
            PlayerContractData.setLastDeliveryTime(player, now);
            last = now;
        }
        if (lastCovenant == null || now < lastCovenant) {
            PlayerContractData.setLastCovenantRoll(player, now);
            lastCovenant = now;
        }
        if (now - last >= ContractConfig.contractIntervalTicks()
                && ContractGiver.giveRandom(player, ContractType.CONTRACT)) {
            PlayerContractData.setLastDeliveryTime(player, now);
        }
        if (now - lastCovenant >= ContractConfig.covenantIntervalTicks()) {
            PlayerContractData.setLastCovenantRoll(player, now);
            if (player.getRandom().nextFloat() < ContractConfig.COVENANT_CHANCE.get()) {
                ContractGiver.giveRandom(player, ContractType.COVENANT);
            }
        }
    }
    @SubscribeEvent
    public static void onAdvancement(AdvancementEvent.AdvancementEarnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ResourceLocation id = event.getAdvancement().getId();
        if (id.equals(PlayerContractData.BOUNTY_UNLOCK)) {
            ModNetwork.syncActive(player);
            ModNetwork.notify(player, Component.literal("You have unlocked bounties").withStyle(ChatFormatting.GOLD));
        }
        boolean unlocksContracts = ContractRegistry.byType(ContractType.CONTRACT).stream()
                .anyMatch(d -> id.equals(d.unlock()));
        if (unlocksContracts) {
            ModNetwork.notify(player, Component.literal("New contracts unlocked").withStyle(ChatFormatting.GOLD));
        }
    }
    private DeliveryEvents() {}
}
