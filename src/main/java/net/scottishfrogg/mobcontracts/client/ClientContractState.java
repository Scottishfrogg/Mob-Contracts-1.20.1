package net.scottishfrogg.mobcontracts.client;

import net.minecraft.resources.ResourceLocation;
import net.scottishfrogg.mobcontracts.contract.ContractDefinition;
import net.scottishfrogg.mobcontracts.contract.ContractRegistry;
import org.jetbrains.annotations.Nullable;

public final class ClientContractState {
    @Nullable private static ResourceLocation activeId;
    private static int[] progress = new int[0];
    private static int towardBounty;
    private static boolean bountiesUnlocked;
    private static String targetName = "";
    private static String helpingName = "";
    private ClientContractState() {}
    public static void set(@Nullable ResourceLocation id, int[] prog, int toward, boolean unlocked,
                           String target, String helping) {
        activeId = id;
        progress = prog;
        towardBounty = toward;
        bountiesUnlocked = unlocked;
        targetName = target;
        helpingName = helping;
    }
    public static boolean hasActive() { return activeId != null; }
    @Nullable public static ContractDefinition getActive() { return ContractRegistry.get(activeId); }
    public static int[] getProgress() { return progress; }
    public static int getTowardBounty() { return towardBounty; }
    public static boolean bountiesUnlocked() { return bountiesUnlocked; }
    public static String getTargetName() { return targetName; }
    public static String getHelpingName() { return helpingName; }
}
