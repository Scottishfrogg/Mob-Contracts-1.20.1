package net.scottishfrogg.mobcontracts.config;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.ForgeConfigSpec;
import net.scottishfrogg.mobcontracts.contract.ContractDefinition;
import net.scottishfrogg.mobcontracts.contract.ContractType;
import net.scottishfrogg.mobcontracts.data.PlayerContractData;

public final class ContractConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.DoubleValue CONTRACT_INTERVAL_DAYS;
    public static final ForgeConfigSpec.DoubleValue COVENANT_INTERVAL_DAYS;
    public static final ForgeConfigSpec.DoubleValue COVENANT_CHANCE;
    public static final ForgeConfigSpec.BooleanValue CONTRACTS_IN_CHESTS;
    public static final ForgeConfigSpec.BooleanValue HELP_ON_CONTRACTS;
    public static final ForgeConfigSpec.BooleanValue HELP_ON_BOUNTIES;
    public static final ForgeConfigSpec.BooleanValue HELP_ON_COVENANTS;
    public static final ForgeConfigSpec.BooleanValue DISABLE_CONTRACTS;
    public static final ForgeConfigSpec.BooleanValue DISABLE_BOUNTIES;
    public static final ForgeConfigSpec.BooleanValue DISABLE_COVENANTS;
    public static final ForgeConfigSpec.BooleanValue RANK_COLORS;
    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.comment("How often contracts and covenants arrive. One Minecraft day is 20 real minutes.").push("delivery");
        CONTRACT_INTERVAL_DAYS = b.comment("Minecraft days between contract deliveries (default 3)")
                .defineInRange("contractIntervalDays", 3.0, 0.05, 365.0);
        COVENANT_INTERVAL_DAYS = b.comment("Minecraft days between covenant rolls (default 3)")
                .defineInRange("covenantIntervalDays", 3.0, 0.05, 365.0);
        COVENANT_CHANCE = b.comment("Chance that a covenant roll actually gives a covenant, from 0.0 to 1.0. Lower = rarer (default 0.2 = 20%)")
                .defineInRange("covenantChance", 0.20, 0.0, 1.0);
        b.pop();
        b.push("chests");
        CONTRACTS_IN_CHESTS = b.comment("Can basic contracts be found in generated chests? (the odds are in data/contractmod/loot_modifiers/chest_contracts.json)")
                .define("contractsInChests", true);
        b.pop();
        b.comment("Can friends join a contract with /contractmod invite? (turning a type off also stops existing helpers counting)").push("multiplayer");
        HELP_ON_CONTRACTS = b.define("allowHelpOnContracts", true);
        HELP_ON_BOUNTIES = b.define("allowHelpOnBounties", true);
        HELP_ON_COVENANTS = b.define("allowHelpOnCovenants", true);
        b.pop();
        b.comment("Can players switch completed entries off in the Hunt History? (when off, that type is always on)").push("hunt_history");
        DISABLE_CONTRACTS = b.define("allowDisablingContracts", true);
        DISABLE_BOUNTIES = b.define("allowDisablingBounties", true);
        DISABLE_COVENANTS = b.define("allowDisablingCovenants", true);
        b.pop();
        b.push("hunter_ranks");
        RANK_COLORS = b.comment("Does a Hunter Rank recolor the player's name (name tag and chat)? Hovering a name in chat still shows the rank.")
                .define("rankColorsUsername", true);
        b.pop();
        SPEC = b.build();
    }
    private ContractConfig() {}
    public static long contractIntervalTicks() {
        return Math.max(1L, (long) (CONTRACT_INTERVAL_DAYS.get() * 24000.0));
    }
    public static long covenantIntervalTicks() {
        return Math.max(1L, (long) (COVENANT_INTERVAL_DAYS.get() * 24000.0));
    }
    public static boolean helpAllowed(ContractType type) {
        return switch (type) {
            case CONTRACT -> HELP_ON_CONTRACTS.get();
            case BOUNTY -> HELP_ON_BOUNTIES.get();
            case COVENANT -> HELP_ON_COVENANTS.get();
        };
    }
    public static boolean canDisable(ContractType type) {
        return switch (type) {
            case CONTRACT -> DISABLE_CONTRACTS.get();
            case BOUNTY -> DISABLE_BOUNTIES.get();
            case COVENANT -> DISABLE_COVENANTS.get();
        };
    }
    public static String pluralName(ContractType type) {
        return switch (type) {
            case CONTRACT -> "contracts";
            case BOUNTY -> "bounties";
            case COVENANT -> "covenants";
        };
    }
    public static boolean isDisabledFor(Player player, ContractDefinition def) {
        return canDisable(def.type()) && PlayerContractData.isDisabled(player, def.id());
    }
}
