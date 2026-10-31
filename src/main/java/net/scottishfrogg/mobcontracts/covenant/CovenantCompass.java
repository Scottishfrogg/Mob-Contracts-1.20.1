package net.scottishfrogg.mobcontracts.covenant;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public final class CovenantCompass {
    public static final String TAG = "ContractModCovenantCompass";
    private CovenantCompass() {}
    public static boolean isCompass(ItemStack stack) {
        if (!stack.is(Items.COMPASS)) return false;
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(TAG);
    }
    private static List<ItemStack> allStacks(ServerPlayer player) {
        List<ItemStack> all = new ArrayList<>(player.getInventory().items);
        all.addAll(player.getInventory().armor);
        all.addAll(player.getInventory().offhand);
        return all;
    }
    public static boolean has(ServerPlayer player) {
        for (ItemStack stack : allStacks(player)) {
            if (isCompass(stack)) return true;
        }
        return isCompass(player.containerMenu.getCarried());
    }
    public static void ensureAndPoint(ServerPlayer player, ResourceKey<Level> dimension, BlockPos pos) {
        boolean found = false;
        for (ItemStack stack : allStacks(player)) {
            if (isCompass(stack)) {
                point(stack, dimension, pos);
                found = true;
            }
        }
        ItemStack carried = player.containerMenu.getCarried();
        if (isCompass(carried)) {
            point(carried, dimension, pos);
            found = true;
        }
        if (found) return;
        ItemStack stack = new ItemStack(Items.COMPASS);
        stack.getOrCreateTag().putBoolean(TAG, true);
        stack.setHoverName(Component.literal("Covenant Compass")
                .withStyle(style -> style.withItalic(false).withColor(ChatFormatting.AQUA)));
        point(stack, dimension, pos);
        player.getInventory().add(stack);
    }
    private static void point(ItemStack stack, ResourceKey<Level> dimension, BlockPos pos) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.put("LodestonePos", NbtUtils.writeBlockPos(pos));
        tag.putString("LodestoneDimension", dimension.location().toString());
        tag.putBoolean("LodestoneTracked", false);
    }
    public static void removeAll(ServerPlayer player) {
        player.getInventory().clearOrCountMatchingItems(CovenantCompass::isCompass, -1, player.inventoryMenu.getCraftSlots());
        if (isCompass(player.containerMenu.getCarried())) player.containerMenu.setCarried(ItemStack.EMPTY);
        sweep(player);
        player.containerMenu.broadcastChanges();
    }
    public static void sweep(ServerPlayer player) {
        for (Slot slot : player.containerMenu.slots) {
            if (slot.container != player.getInventory() && isCompass(slot.getItem())) {
                slot.set(ItemStack.EMPTY);
            }
        }
        for (ItemStack stack : allStacks(player)) {
            if (!(stack.getItem() instanceof BundleItem) || !stack.hasTag()) continue;
            CompoundTag tag = stack.getTag();
            if (!tag.contains("Items", Tag.TAG_LIST)) continue;
            ListTag items = tag.getList("Items", Tag.TAG_COMPOUND);
            if (items.removeIf(t -> t instanceof CompoundTag c && c.getString("id").equals("minecraft:compass")
                    && c.getCompound("tag").getBoolean(TAG))) {
                tag.put("Items", items);
            }
        }
    }
}
