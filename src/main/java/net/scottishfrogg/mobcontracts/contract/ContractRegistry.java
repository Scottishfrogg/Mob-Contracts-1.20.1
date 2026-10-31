package net.scottishfrogg.mobcontracts.contract;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class ContractRegistry {
    private static volatile Map<ResourceLocation, ContractDefinition> contracts = Map.of();
    private ContractRegistry() {}
    public static void replace(Map<ResourceLocation, ContractDefinition> newContracts) {
        contracts = Map.copyOf(newContracts);
    }
    @Nullable
    public static ContractDefinition get(@Nullable ResourceLocation id) {
        return id == null ? null : contracts.get(id);
    }
    public static Collection<ContractDefinition> all() {
        return contracts.values();
    }
    public static List<ContractDefinition> basicContracts() {
        return contracts.values().stream()
                .filter(c -> c.type() == ContractType.CONTRACT && c.unlock() == null).toList();
    }
    public static List<ContractDefinition> byType(ContractType type) {
        return contracts.values().stream().filter(c -> c.type() == type).toList();
    }
}
