package net.scottishfrogg.mobcontracts.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.scottishfrogg.mobcontracts.ContractMod;
import net.scottishfrogg.mobcontracts.rank.ClientRanks;

@Mod.EventBusSubscriber(modid = ContractMod.MOD_ID, value = Dist.CLIENT)
public class ClientForgeEvents {
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !Minecraft.getInstance().isPaused()) {
            ClientNotifications.tick();
            while (ModKeys.CANCEL.consumeClick()) {
                ModKeys.onCancelPressed();
            }
            while (ModKeys.HISTORY.consumeClick()) {
                ModKeys.onHistoryPressed();
            }
        }
    }
    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientNotifications.clear();
        ClientRanks.clear();
    }
}
