package net.scottishfrogg.mobcontracts.item;

import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.scottishfrogg.mobcontracts.ContractMod;
import net.scottishfrogg.mobcontracts.contract.ContractType;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ContractMod.MOD_ID);
    public static final RegistryObject<Item> CONTRACT = ITEMS.register("contract",
            () -> new ContractItem(new Item.Properties(), ContractType.CONTRACT));
    public static final RegistryObject<Item> COVENANT = ITEMS.register("covenant",
            () -> new ContractItem(new Item.Properties(), ContractType.COVENANT));
    public static final RegistryObject<Item> BOUNTY = ITEMS.register("bounty",
            () -> new ContractItem(new Item.Properties(), ContractType.BOUNTY));
    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
