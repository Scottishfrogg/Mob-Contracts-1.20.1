package net.scottishfrogg.mobcontracts;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.scottishfrogg.mobcontracts.contract.ContractReloadListener;
import net.scottishfrogg.mobcontracts.network.ModNetwork;

@Mod.EventBusSubscriber(modid = ContractMod.MOD_ID)
public class CommonEvents {
    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new ContractReloadListener());
    }
    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        ServerPlayer single = event.getPlayer();
        if (single != null) {
            sync(single);
        } else {
            event.getPlayerList().getPlayers().forEach(CommonEvents::sync);
        }
    }
    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ModNetwork.syncActive(player);
        }
    }
    private static void sync(ServerPlayer player) {
        ModNetwork.syncDefinitions(player);
        ModNetwork.syncActive(player);
    }
}
