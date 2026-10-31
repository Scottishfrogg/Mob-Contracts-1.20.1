package net.scottishfrogg.mobcontracts.contract;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.scottishfrogg.mobcontracts.ContractMod;

import java.util.HashMap;
import java.util.Map;

public class ContractReloadListener extends SimpleJsonResourceReloadListener {
    public ContractReloadListener() {
        super(new Gson(), "contracts");
    }
    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, ContractDefinition> loaded = new HashMap<>();
        files.forEach((id, json) -> {
            try {
                loaded.put(id, ContractDefinition.fromJson(id, GsonHelper.convertToJsonObject(json, "contract")));
            } catch (Exception e) {
                ContractMod.LOGGER.error("Failed to load contract {}", id, e);
            }
        });
        ContractRegistry.replace(loaded);
        ContractMod.LOGGER.info("Loaded {} contracts", loaded.size());
    }
}
