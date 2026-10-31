package net.scottishfrogg.mobcontracts.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import net.scottishfrogg.mobcontracts.contract.ContractDefinition;
import net.scottishfrogg.mobcontracts.contract.ContractRegistry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public record DefinitionSyncPacket(Collection<ContractDefinition> definitions) {
    public static void encode(DefinitionSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeCollection(msg.definitions, (b, d) -> d.write(b));
    }
    public static DefinitionSyncPacket decode(FriendlyByteBuf buf) {
        return new DefinitionSyncPacket(buf.readCollection(ArrayList::new, ContractDefinition::read));
    }
    public static void handle(DefinitionSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Map<ResourceLocation, ContractDefinition> map = new HashMap<>();
            msg.definitions.forEach(d -> map.put(d.id(), d));
            ContractRegistry.replace(map);
        });
        ctx.get().setPacketHandled(true);
    }
}
