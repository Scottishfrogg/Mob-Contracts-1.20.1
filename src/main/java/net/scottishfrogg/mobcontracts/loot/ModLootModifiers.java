package net.scottishfrogg.mobcontracts.loot;

import com.mojang.serialization.Codec;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.scottishfrogg.mobcontracts.ContractMod;

public final class ModLootModifiers {
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, ContractMod.MOD_ID);
    public static final RegistryObject<Codec<ChestContractModifier>> CHEST_CONTRACTS =
            LOOT_MODIFIERS.register("chest_contracts", ChestContractModifier.CODEC);
    private ModLootModifiers() {}
    public static void register(IEventBus bus) {
        LOOT_MODIFIERS.register(bus);
    }
}
