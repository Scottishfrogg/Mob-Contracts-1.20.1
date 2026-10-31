package net.scottishfrogg.mobcontracts.advancement;

import com.google.gson.JsonObject;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SerializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;

public class MilestoneTrigger extends SimpleCriterionTrigger<MilestoneTrigger.Instance> {
    private final ResourceLocation id;
    public MilestoneTrigger(ResourceLocation id) {
        this.id = id;
    }
    @Override
    public ResourceLocation getId() {
        return id;
    }
    @Override
    protected Instance createInstance(JsonObject json, ContextAwarePredicate player, DeserializationContext ctx) {
        return new Instance(id, player, GsonHelper.getAsInt(json, "count", 1));
    }
    public void trigger(ServerPlayer player, int total) {
        trigger(player, instance -> total >= instance.count);
    }
    public static class Instance extends AbstractCriterionTriggerInstance {
        private final int count;
        public Instance(ResourceLocation id, ContextAwarePredicate player, int count) {
            super(id, player);
            this.count = count;
        }
        @Override
        public JsonObject serializeToJson(SerializationContext ctx) {
            JsonObject json = super.serializeToJson(ctx);
            json.addProperty("count", count);
            return json;
        }
    }
}
