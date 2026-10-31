package net.scottishfrogg.mobcontracts.advancement;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.resources.ResourceLocation;
import net.scottishfrogg.mobcontracts.ContractMod;

public final class ModTriggers {
    public static final MilestoneTrigger CONTRACTS_COMPLETED =
            new MilestoneTrigger(new ResourceLocation(ContractMod.MOD_ID, "contracts_completed"));
    public static final MilestoneTrigger BOUNTIES_RECEIVED =
            new MilestoneTrigger(new ResourceLocation(ContractMod.MOD_ID, "bounties_received"));
    public static final MilestoneTrigger COVENANTS_COMPLETED =
            new MilestoneTrigger(new ResourceLocation(ContractMod.MOD_ID, "covenants_completed"));
    public static void register() {
        CriteriaTriggers.register(CONTRACTS_COMPLETED);
        CriteriaTriggers.register(BOUNTIES_RECEIVED);
        CriteriaTriggers.register(COVENANTS_COMPLETED);
    }
    private ModTriggers() {}
}
