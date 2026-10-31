package net.scottishfrogg.mobcontracts.contract;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public record ContractObjective(Type type, List<ResourceLocation> entities, int count,
                                @Nullable String condition, @Nullable String event,
                                @Nullable String resetOn) {
    public enum Type { KILL, EVENT }
    public static final String RESET_ANY = "any";
    public static final String RESET_EXPLOSION = "explosion";
    public static final String RESET_SHULKER_BULLET = "shulker_bullet";
    public static final String RESET_SHIELD = "shield_block";
    public static final String RESET_WITHER = "wither_effect";
    public static final String RESET_LEAVE_NETHER = "leave_nether";
    private static final Set<String> RESET_TRIGGERS =
            Set.of(RESET_ANY, RESET_EXPLOSION, RESET_SHULKER_BULLET, RESET_SHIELD, RESET_WITHER, RESET_LEAVE_NETHER);
    public static ContractObjective fromJson(JsonObject json) {
        Type type = Type.valueOf(GsonHelper.getAsString(json, "type", "kill").toUpperCase(Locale.ROOT));
        List<ResourceLocation> entities = new ArrayList<>();
        for (JsonElement e : GsonHelper.getAsJsonArray(json, "entities", new JsonArray())) {
            entities.add(new ResourceLocation(e.getAsString()));
        }
        String condition = GsonHelper.getAsString(json, "condition", null);
        String event = GsonHelper.getAsString(json, "event", null);
        if (type == Type.KILL && entities.isEmpty() && condition == null) {
            throw new JsonSyntaxException("Kill objective needs entities or a condition");
        }
        if (type == Type.EVENT && event == null) throw new JsonSyntaxException("Event objective needs an event");
        ContractConditions.validate(condition);
        return new ContractObjective(type, entities, GsonHelper.getAsInt(json, "count", 1), condition, event,
                parseResetOn(json));
    }
    @Nullable
    private static String parseResetOn(JsonObject json) {
        JsonElement el = json.has("reset_on") ? json.get("reset_on") : json.get("reset_on_damage");
        if (el == null) return null;
        String value;
        if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isBoolean()) {
            value = el.getAsBoolean() ? RESET_ANY : null;
        } else {
            value = el.getAsString();
        }
        if (value != null && !RESET_TRIGGERS.contains(value)) {
            throw new JsonSyntaxException("Unknown reset_on trigger '" + value + "'");
        }
        return value;
    }
    public void write(FriendlyByteBuf buf) {
        buf.writeEnum(type);
        buf.writeCollection(entities, FriendlyByteBuf::writeResourceLocation);
        buf.writeVarInt(count);
        buf.writeNullable(condition, FriendlyByteBuf::writeUtf);
        buf.writeNullable(event, FriendlyByteBuf::writeUtf);
        buf.writeNullable(resetOn, FriendlyByteBuf::writeUtf);
    }
    public static ContractObjective read(FriendlyByteBuf buf) {
        return new ContractObjective(
                buf.readEnum(Type.class),
                buf.readCollection(ArrayList::new, FriendlyByteBuf::readResourceLocation),
                buf.readVarInt(),
                buf.readNullable(FriendlyByteBuf::readUtf),
                buf.readNullable(FriendlyByteBuf::readUtf),
                buf.readNullable(FriendlyByteBuf::readUtf));
    }
}
