package net.scottishfrogg.mobcontracts.contract;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record EliteTarget(ResourceLocation entity, String name, ResourceLocation dimension,
                          int minDistance, int maxDistance, boolean anchorAtWorldOrigin,
                          Map<EquipmentSlot, ContractReward.RewardItem> equipment,
                          List<EliteEffect> effects, List<String> behaviors) {
    public static final int MAX_DISTANCE = 500;
    public static final Set<String> BEHAVIORS =
            Set.of("fast_fire", "tipped_harming_arrows", "respawn_on_explosion", "hostile_to_owner");
    public record EliteEffect(ResourceLocation effect, int amplifier) {}
    public static EliteTarget fromJson(JsonObject json) {
        ResourceLocation entity = new ResourceLocation(GsonHelper.getAsString(json, "entity"));
        if (!ForgeRegistries.ENTITY_TYPES.containsKey(entity)) {
            throw new JsonSyntaxException("Unknown elite entity " + entity);
        }
        int max = Math.min(MAX_DISTANCE, GsonHelper.getAsInt(json, "max_distance", MAX_DISTANCE));
        int min = Math.min(max, GsonHelper.getAsInt(json, "min_distance", 250));
        Map<EquipmentSlot, ContractReward.RewardItem> equipment = new LinkedHashMap<>();
        if (json.has("equipment")) {
            for (Map.Entry<String, JsonElement> e : GsonHelper.getAsJsonObject(json, "equipment").entrySet()) {
                equipment.put(slot(e.getKey()), parseItem(GsonHelper.convertToJsonObject(e.getValue(), e.getKey())));
            }
        }
        List<EliteEffect> effects = new ArrayList<>();
        for (JsonElement el : GsonHelper.getAsJsonArray(json, "effects", new JsonArray())) {
            JsonObject o = GsonHelper.convertToJsonObject(el, "effect");
            ResourceLocation id = new ResourceLocation(GsonHelper.getAsString(o, "effect"));
            if (!ForgeRegistries.MOB_EFFECTS.containsKey(id)) throw new JsonSyntaxException("Unknown effect " + id);
            effects.add(new EliteEffect(id, GsonHelper.getAsInt(o, "amplifier", 0)));
        }
        List<String> behaviors = new ArrayList<>();
        for (JsonElement el : GsonHelper.getAsJsonArray(json, "behaviors", new JsonArray())) {
            String b = el.getAsString();
            if (!BEHAVIORS.contains(b)) throw new JsonSyntaxException("Unknown elite behavior '" + b + "'");
            behaviors.add(b);
        }
        return new EliteTarget(entity,
                GsonHelper.getAsString(json, "name"),
                new ResourceLocation(GsonHelper.getAsString(json, "dimension", "minecraft:overworld")),
                min, max,
                "world_origin".equals(GsonHelper.getAsString(json, "anchor", "player")),
                equipment, effects, behaviors);
    }
    private static EquipmentSlot slot(String key) {
        return switch (key) {
            case "head" -> EquipmentSlot.HEAD;
            case "chest" -> EquipmentSlot.CHEST;
            case "legs" -> EquipmentSlot.LEGS;
            case "feet" -> EquipmentSlot.FEET;
            case "mainhand" -> EquipmentSlot.MAINHAND;
            case "offhand" -> EquipmentSlot.OFFHAND;
            default -> throw new JsonSyntaxException("Unknown equipment slot '" + key + "'");
        };
    }
    private static ContractReward.RewardItem parseItem(JsonObject obj) {
        CompoundTag nbt = null;
        if (obj.has("nbt")) {
            try {
                nbt = TagParser.parseTag(GsonHelper.getAsString(obj, "nbt"));
            } catch (CommandSyntaxException e) {
                throw new JsonSyntaxException("Bad equipment nbt: " + e.getMessage());
            }
        }
        return new ContractReward.RewardItem(GsonHelper.getAsItem(obj, "item"), GsonHelper.getAsInt(obj, "count", 1), nbt);
    }
    public void write(FriendlyByteBuf buf) {
        buf.writeResourceLocation(entity);
        buf.writeUtf(name);
        buf.writeResourceLocation(dimension);
        buf.writeVarInt(minDistance);
        buf.writeVarInt(maxDistance);
        buf.writeBoolean(anchorAtWorldOrigin);
        buf.writeVarInt(equipment.size());
        equipment.forEach((slot, item) -> {
            buf.writeEnum(slot);
            item.write(buf);
        });
        buf.writeCollection(effects, (b, e) -> {
            b.writeResourceLocation(e.effect());
            b.writeVarInt(e.amplifier());
        });
        buf.writeCollection(behaviors, FriendlyByteBuf::writeUtf);
    }
    public static EliteTarget read(FriendlyByteBuf buf) {
        ResourceLocation entity = buf.readResourceLocation();
        String name = buf.readUtf();
        ResourceLocation dimension = buf.readResourceLocation();
        int min = buf.readVarInt();
        int max = buf.readVarInt();
        boolean anchor = buf.readBoolean();
        Map<EquipmentSlot, ContractReward.RewardItem> equipment = new LinkedHashMap<>();
        int n = buf.readVarInt();
        for (int i = 0; i < n; i++) {
            EquipmentSlot slot = buf.readEnum(EquipmentSlot.class);
            equipment.put(slot, ContractReward.RewardItem.read(buf));
        }
        List<EliteEffect> effects = buf.readCollection(ArrayList::new,
                b -> new EliteEffect(b.readResourceLocation(), b.readVarInt()));
        List<String> behaviors = buf.readCollection(ArrayList::new, FriendlyByteBuf::readUtf);
        return new EliteTarget(entity, name, dimension, min, max, anchor, equipment, effects, behaviors);
    }
}
