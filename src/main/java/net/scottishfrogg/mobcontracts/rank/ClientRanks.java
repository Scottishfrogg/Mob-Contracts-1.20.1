package net.scottishfrogg.mobcontracts.rank;

import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

public final class ClientRanks {
    private static Map<UUID, Integer> totals = Map.of();
    private static boolean colors = true;
    private ClientRanks() {}
    public static void set(Map<UUID, Integer> newTotals, boolean colorsEnabled) {
        totals = Map.copyOf(newTotals);
        colors = colorsEnabled;
    }
    @Nullable
    public static Integer get(UUID id) {
        return totals.get(id);
    }
    public static boolean colorsEnabled() {
        return colors;
    }
    public static void clear() {
        totals = Map.of();
    }
}
