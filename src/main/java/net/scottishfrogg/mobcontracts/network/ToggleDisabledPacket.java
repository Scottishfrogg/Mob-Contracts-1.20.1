package net.scottishfrogg.mobcontracts.network;

import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.scottishfrogg.mobcontracts.config.ContractConfig;
import net.scottishfrogg.mobcontracts.contract.ContractDefinition;
import net.scottishfrogg.mobcontracts.contract.ContractRegistry;
import net.scottishfrogg.mobcontracts.data.PlayerContractData;

import java.util.function.Supplier;

public record ToggleDisabledPacket(ResourceLocation id) {
    public static void encode(ToggleDisabledPacket msg, FriendlyByteBuf buf) {
        buf.writeResourceLocation(msg.id);
    }
    public static ToggleDisabledPacket decode(FriendlyByteBuf buf) {
        return new ToggleDisabledPacket(buf.readResourceLocation());
    }
    public static void handle(ToggleDisabledPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer sender = ctx.get().getSender();
        ctx.get().enqueueWork(() -> {
            if (sender == null) return;
            ContractDefinition def = ContractRegistry.get(msg.id);
            if (def != null && !ContractConfig.canDisable(def.type())) {
                sender.displayClientMessage(Component.literal("Disabling " + ContractConfig.pluralName(def.type())
                        + " is turned off on this server").withStyle(ChatFormatting.RED), true);
                return;
            }
            if (PlayerContractData.toggleDisabled(sender, msg.id)) {
                ModNetwork.sendHistory(sender, false);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
