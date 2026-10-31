package net.scottishfrogg.mobcontracts.data;

import net.minecraft.advancements.Advancement;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.scottishfrogg.mobcontracts.ContractMod;
import net.scottishfrogg.mobcontracts.contract.ContractDefinition;
import net.scottishfrogg.mobcontracts.contract.ContractRegistry;
import net.scottishfrogg.mobcontracts.contract.ContractType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class PlayerContractData {
    public static final int CONTRACTS_PER_BOUNTY = 3;
    public static final ResourceLocation BOUNTY_UNLOCK = new ResourceLocation("story/form_obsidian"); // Ice Bucket Challenge
    private PlayerContractData() {}
    private static CompoundTag data(Player player) {
        CompoundTag forgeData = player.getPersistentData();
        if (!forgeData.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)) {
            forgeData.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }
        CompoundTag persisted = forgeData.getCompound(Player.PERSISTED_NBT_TAG);
        if (!persisted.contains(ContractMod.MOD_ID, Tag.TAG_COMPOUND)) {
            persisted.put(ContractMod.MOD_ID, new CompoundTag());
        }
        return persisted.getCompound(ContractMod.MOD_ID);
    }
    @Nullable
    public static ResourceLocation getActiveId(Player player) {
        CompoundTag d = data(player);
        return d.contains("ActiveId") ? ResourceLocation.tryParse(d.getString("ActiveId")) : null;
    }
    @Nullable
    public static ContractDefinition getActive(Player player) {
        return ContractRegistry.get(getActiveId(player));
    }
    public static boolean hasActive(Player player) {
        return getActiveId(player) != null;
    }
    public static int[] getProgress(Player player) {
        return data(player).getIntArray("Progress");
    }
    public static void setProgress(Player player, int[] progress) {
        data(player).putIntArray("Progress", progress);
    }
    public static void start(Player player, ContractDefinition def) {
        CompoundTag d = data(player);
        d.putString("ActiveId", def.id().toString());
        d.putIntArray("Progress", new int[def.objectives().size()]);
    }
    public static void clearActive(Player player) {
        CompoundTag d = data(player);
        d.remove("ActiveId");
        d.remove("Progress");
    }
    public static int getCompleted(Player player) {
        return data(player).getInt("Completed");
    }
    public static void setCompleted(Player player, int value) {
        data(player).putInt("Completed", value);
    }
    public static int getBountiesCompleted(Player player) {
        return data(player).getInt("BountiesCompleted");
    }
    public static int getCovenantsCompleted(Player player) {
        return data(player).getInt("CovenantsCompleted");
    }
    public record HistoryEntry(ResourceLocation id, int count) {}
    public static void recordHistory(Player player, ContractDefinition def) {
        CompoundTag d = data(player);
        ListTag history = d.getList("History", Tag.TAG_COMPOUND);
        String id = def.id().toString();
        for (int i = 0; i < history.size(); i++) {
            CompoundTag entry = history.getCompound(i);
            if (entry.getString("Id").equals(id)) {
                entry.putInt("Count", entry.getInt("Count") + 1);
                d.put("History", history);
                return;
            }
        }
        CompoundTag entry = new CompoundTag();
        entry.putString("Id", id);
        entry.putInt("Count", 1);
        history.add(entry);
        d.put("History", history);
    }
    public static List<HistoryEntry> getHistory(Player player) {
        ListTag history = data(player).getList("History", Tag.TAG_COMPOUND);
        List<HistoryEntry> out = new ArrayList<>();
        for (int i = 0; i < history.size(); i++) {
            CompoundTag entry = history.getCompound(i);
            ResourceLocation id = ResourceLocation.tryParse(entry.getString("Id"));
            if (id != null) out.add(new HistoryEntry(id, entry.getInt("Count")));
        }
        return out;
    }
    public static boolean isDisabled(Player player, ResourceLocation id) {
        ListTag list = data(player).getList("Disabled", Tag.TAG_STRING);
        String s = id.toString();
        for (int i = 0; i < list.size(); i++) {
            if (list.getString(i).equals(s)) return true;
        }
        return false;
    }
    public static boolean toggleDisabled(Player player, ResourceLocation id) {
        if (getHistory(player).stream().noneMatch(e -> e.id().equals(id))) return false;
        CompoundTag d = data(player);
        ListTag list = d.getList("Disabled", Tag.TAG_STRING);
        String s = id.toString();
        if (!list.removeIf(t -> t instanceof StringTag st && st.getAsString().equals(s))) {
            list.add(StringTag.valueOf(s));
        }
        d.put("Disabled", list);
        return true;
    }
    public static List<UUID> getHelpers(Player host) {
        ListTag list = data(host).getList("Helpers", Tag.TAG_STRING);
        List<UUID> ids = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            try {
                ids.add(UUID.fromString(list.getString(i)));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return ids;
    }
    public static void addHelper(Player host, UUID id) {
        if (getHelpers(host).contains(id)) return;
        CompoundTag d = data(host);
        ListTag list = d.getList("Helpers", Tag.TAG_STRING);
        list.add(StringTag.valueOf(id.toString()));
        d.put("Helpers", list);
    }
    public static boolean removeHelper(Player host, UUID id) {
        CompoundTag d = data(host);
        ListTag list = d.getList("Helpers", Tag.TAG_STRING);
        boolean removed = list.removeIf(t -> t instanceof StringTag s && s.getAsString().equals(id.toString()));
        if (removed) d.put("Helpers", list);
        return removed;
    }
    public static void clearHelpers(Player host) {
        data(host).remove("Helpers");
    }
    public static CompoundTag getCovenantData(Player player) {
        CompoundTag d = data(player);
        if (!d.contains("Covenant", Tag.TAG_COMPOUND)) d.put("Covenant", new CompoundTag());
        return d.getCompound("Covenant");
    }
    public static void clearCovenantData(Player player) {
        data(player).remove("Covenant");
    }
    public static String getCovenantTargetName(Player player) {
        return data(player).getCompound("Covenant").getString("TargetName");
    }
    public static int getTowardBounty(Player player) {
        return data(player).getInt("TowardBounty");
    }
    public static int incrementBountiesReceived(Player player) {
        CompoundTag d = data(player);
        int total = d.getInt("BountiesReceived") + 1;
        d.putInt("BountiesReceived", total);
        return total;
    }
    public static boolean recordCompletion(Player player, ContractDefinition def, boolean bountiesUnlocked) {
        CompoundTag d = data(player);
        if (def.type() == ContractType.BOUNTY) {
            d.putInt("BountiesCompleted", d.getInt("BountiesCompleted") + 1);
            return false;
        }
        if (def.type() == ContractType.COVENANT) {
            d.putInt("CovenantsCompleted", d.getInt("CovenantsCompleted") + 1);
        } else {
            d.putInt("Completed", d.getInt("Completed") + 1);
        }
        if (!bountiesUnlocked) return false;
        int toward = d.getInt("TowardBounty") + 1;
        boolean earned = toward >= CONTRACTS_PER_BOUNTY;
        d.putInt("TowardBounty", earned ? 0 : toward);
        return earned;
    }
    public static boolean bountiesUnlocked(ServerPlayer player) {
        Advancement adv = player.server.getAdvancements().getAdvancement(BOUNTY_UNLOCK);
        return adv != null && player.getAdvancements().getOrStartProgress(adv).isDone();
    }
    @Nullable
    public static Long getLastDeliveryTime(Player player) {
        CompoundTag d = data(player);
        return d.contains("LastDelivery", Tag.TAG_LONG) ? d.getLong("LastDelivery") : null;
    }
    public static void setLastDeliveryTime(Player player, long time) {
        data(player).putLong("LastDelivery", time);
    }
    @Nullable
    public static Long getLastCovenantRoll(Player player) {
        CompoundTag d = data(player);
        return d.contains("LastCovenantRoll", Tag.TAG_LONG) ? d.getLong("LastCovenantRoll") : null;
    }
    public static void setLastCovenantRoll(Player player, long time) {
        data(player).putLong("LastCovenantRoll", time);
    }
}
