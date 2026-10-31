package net.scottishfrogg.mobcontracts.contract;

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

public record ContractDefinition(
        ResourceLocation id,
        ContractType type,
        String name,
        String task,
        @Nullable ResourceLocation unlock,
        List<ContractObjective> objectives,
        ContractReward reward,
        @Nullable EliteTarget elite,
        boolean targetsPlayer) {
    public static ContractDefinition fromJson(ResourceLocation id, JsonObject json) {
        ContractType type = ContractType.valueOf(GsonHelper.getAsString(json, "type").toUpperCase(Locale.ROOT));
        String unlockStr = GsonHelper.getAsString(json, "unlock", null);
        List<ContractObjective> objectives = new ArrayList<>();
        for (JsonElement e : GsonHelper.getAsJsonArray(json, "objectives")) {
            objectives.add(ContractObjective.fromJson(GsonHelper.convertToJsonObject(e, "objective")));
        }
        EliteTarget elite = json.has("elite") ? EliteTarget.fromJson(GsonHelper.getAsJsonObject(json, "elite")) : null;
        boolean targetsPlayer = "player".equals(GsonHelper.getAsString(json, "target", ""));
        if (type == ContractType.COVENANT && (elite != null) == targetsPlayer) {
            throw new JsonSyntaxException("A covenant needs either an \"elite\" or \"target\": \"player\" (not both, not neither)");
        }
        return new ContractDefinition(id, type,
                GsonHelper.getAsString(json, "name"),
                GsonHelper.getAsString(json, "task"),
                unlockStr == null ? null : new ResourceLocation(unlockStr),
                objectives,
                ContractReward.fromJson(GsonHelper.getAsJsonObject(json, "reward")),
                elite, targetsPlayer);
    }
    public void write(FriendlyByteBuf buf) {
        buf.writeResourceLocation(id);
        buf.writeEnum(type);
        buf.writeUtf(name);
        buf.writeUtf(task);
        buf.writeNullable(unlock, FriendlyByteBuf::writeResourceLocation);
        buf.writeCollection(objectives, (b, o) -> o.write(b));
        reward.write(buf);
        buf.writeNullable(elite, (b, e) -> e.write(b));
        buf.writeBoolean(targetsPlayer);
    }
    public static ContractDefinition read(FriendlyByteBuf buf) {
        return new ContractDefinition(
                buf.readResourceLocation(),
                buf.readEnum(ContractType.class),
                buf.readUtf(),
                buf.readUtf(),
                buf.readNullable(FriendlyByteBuf::readResourceLocation),
                buf.readCollection(ArrayList::new, ContractObjective::read),
                ContractReward.read(buf),
                buf.readNullable(EliteTarget::read),
                buf.readBoolean());
    }
}
