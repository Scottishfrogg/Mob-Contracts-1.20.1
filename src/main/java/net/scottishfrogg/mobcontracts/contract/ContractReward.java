package net.scottishfrogg.mobcontracts.contract;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public record ContractReward(List<RewardItem> items, int xp) {
    public record RewardItem(Item item, int count, @Nullable CompoundTag nbt) {
        public ItemStack template() {
            ItemStack stack = new ItemStack(item);
            if (nbt != null) stack.setTag(nbt.copy());
            return stack;
        }
        public Component text() {
            return Component.literal(count + " ").append(template().getHoverName());
        }
        void write(FriendlyByteBuf buf) {
            buf.writeResourceLocation(ForgeRegistries.ITEMS.getKey(item));
            buf.writeVarInt(count);
            buf.writeNullable(nbt, FriendlyByteBuf::writeNbt);
        }
        static RewardItem read(FriendlyByteBuf buf) {
            Item item = ForgeRegistries.ITEMS.getValue(buf.readResourceLocation());
            return new RewardItem(item, buf.readVarInt(), buf.readNullable(FriendlyByteBuf::readNbt));
        }
    }
    public static ContractReward fromJson(JsonObject json) {
        List<RewardItem> items = new ArrayList<>();
        for (JsonElement el : GsonHelper.getAsJsonArray(json, "items", new JsonArray())) {
            JsonObject obj = GsonHelper.convertToJsonObject(el, "reward item");
            CompoundTag nbt = null;
            if (obj.has("nbt")) {
                try {
                    nbt = TagParser.parseTag(GsonHelper.getAsString(obj, "nbt"));
                } catch (CommandSyntaxException e) {
                    throw new JsonSyntaxException("Bad reward nbt: " + e.getMessage());
                }
            }
            items.add(new RewardItem(GsonHelper.getAsItem(obj, "item"), GsonHelper.getAsInt(obj, "count", 1), nbt));
        }
        return new ContractReward(items, GsonHelper.getAsInt(json, "xp", 0));
    }
    public Component text() {
        List<Component> parts = new ArrayList<>();
        for (RewardItem item : items) parts.add(item.text());
        if (xp > 0 || parts.isEmpty()) parts.add(Component.literal(xp + " XP"));
        MutableComponent out = Component.empty();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) out.append(Component.literal(" + "));
            out.append(parts.get(i));
        }
        return out;
    }
    public void write(FriendlyByteBuf buf) {
        buf.writeCollection(items, (b, i) -> i.write(b));
        buf.writeVarInt(xp);
    }
    public static ContractReward read(FriendlyByteBuf buf) {
        return new ContractReward(buf.readCollection(ArrayList::new, RewardItem::read), buf.readVarInt());
    }
}
