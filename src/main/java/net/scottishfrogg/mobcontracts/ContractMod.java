package net.scottishfrogg.mobcontracts;

import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.scottishfrogg.mobcontracts.advancement.ModTriggers;
import net.scottishfrogg.mobcontracts.client.ConfigScreenHook;
import net.scottishfrogg.mobcontracts.client.ContractHud;
import net.scottishfrogg.mobcontracts.client.ContractNotificationHud;
import net.scottishfrogg.mobcontracts.client.ModKeys;
import net.scottishfrogg.mobcontracts.config.ContractConfig;
import net.scottishfrogg.mobcontracts.item.ModItems;
import net.scottishfrogg.mobcontracts.loot.ModLootModifiers;
import net.scottishfrogg.mobcontracts.network.ModNetwork;
import org.slf4j.Logger;

@Mod(ContractMod.MOD_ID)
public class ContractMod {
    public static final String MOD_ID = "contractmod";
    public static final Logger LOGGER = LogUtils.getLogger();
    public ContractMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ContractConfig.SPEC);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ConfigScreenHook::register);
        ModItems.register(modEventBus);
        ModLootModifiers.register(modEventBus);
        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
        modEventBus.addListener(this::addCreative);
    }
    private void commonSetup(final FMLCommonSetupEvent event) {
        ModNetwork.register();
        event.enqueueWork(ModTriggers::register);
    }
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
    }
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
    }
    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
        }
        @SubscribeEvent
        public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
            event.registerAboveAll("contract_hud", ContractHud.OVERLAY);
            event.registerAboveAll("contract_notification", ContractNotificationHud.OVERLAY);
        }
        @SubscribeEvent
        public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
            event.register(ModKeys.CANCEL);
            event.register(ModKeys.HISTORY);
        }
    }
}
