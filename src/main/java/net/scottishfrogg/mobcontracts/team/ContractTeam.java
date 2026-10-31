package net.scottishfrogg.mobcontracts.team;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.scottishfrogg.mobcontracts.config.ContractConfig;
import net.scottishfrogg.mobcontracts.contract.ContractDefinition;
import net.scottishfrogg.mobcontracts.data.PlayerContractData;
import net.scottishfrogg.mobcontracts.network.ModNetwork;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ContractTeam {
    public static final long INVITE_TICKS = 1200L;
    private record Invite(UUID host, ResourceLocation contract, long expires) {}
    private static final Map<UUID, Invite> PENDING = new HashMap<>();
    private ContractTeam() {}
    public static List<ServerPlayer> helpersOf(ServerPlayer host) {
        List<UUID> ids = PlayerContractData.getHelpers(host);
        if (ids.isEmpty()) return List.of();
        List<ServerPlayer> online = new ArrayList<>();
        for (UUID id : ids) {
            ServerPlayer p = host.server.getPlayerList().getPlayer(id);
            if (p != null) online.add(p);
        }
        return online;
    }
    public static List<ServerPlayer> hostsOf(ServerPlayer helper) {
        List<ServerPlayer> hosts = new ArrayList<>();
        for (ServerPlayer p : helper.server.getPlayerList().getPlayers()) {
            ContractDefinition hostDef = PlayerContractData.getActive(p);
            if (p != helper && hostDef != null && ContractConfig.helpAllowed(hostDef.type())
                    && PlayerContractData.getHelpers(p).contains(helper.getUUID())) {
                hosts.add(p);
            }
        }
        return hosts;
    }
    public static List<ServerPlayer> contractOwnersFor(ServerPlayer actor) {
        List<ServerPlayer> owners = new ArrayList<>();
        if (PlayerContractData.hasActive(actor)) owners.add(actor);
        owners.addAll(hostsOf(actor));
        return owners;
    }
    public static List<ServerPlayer> participants(ServerPlayer host) {
        List<ServerPlayer> all = new ArrayList<>();
        all.add(host);
        all.addAll(helpersOf(host));
        return all;
    }
    public static boolean isHelperOf(ServerPlayer host, UUID id) {
        return PlayerContractData.getHelpers(host).contains(id);
    }
    public static ServerPlayer displaySourceFor(ServerPlayer player) {
        if (PlayerContractData.hasActive(player)) return player;
        List<ServerPlayer> hosts = hostsOf(player);
        return hosts.isEmpty() ? player : hosts.get(0);
    }
    @Nullable
    public static String invite(ServerPlayer host, ServerPlayer target) {
        ContractDefinition def = PlayerContractData.getActive(host);
        if (def == null) return "You need an active contract, bounty or covenant to invite someone";
        if (!ContractConfig.helpAllowed(def.type())) {
            return "Helping on " + ContractConfig.pluralName(def.type()) + " is turned off on this server";
        }
        if (target == host) return "You can't invite yourself";
        String targetName = target.getName().getString();
        if (PlayerContractData.getHelpers(host).contains(target.getUUID())) return targetName + " is already helping you";
        if (def.targetsPlayer()) {
            CompoundTag cov = PlayerContractData.getCovenantData(host);
            if (cov.hasUUID("TargetPlayer") && cov.getUUID("TargetPlayer").equals(target.getUUID())) {
                return "You can't invite the player you are hunting";
            }
        }
        long now = host.server.overworld().getGameTime();
        PENDING.values().removeIf(i -> i.expires() < now);
        PENDING.put(target.getUUID(), new Invite(host.getUUID(), def.id(), now + INVITE_TICKS));
        String hostName = host.getName().getString();
        host.sendSystemMessage(Component.literal("Invitation sent to " + targetName + " (valid for 60 seconds)")
                .withStyle(ChatFormatting.GREEN));
        Component accept = Component.literal("[Accept]").withStyle(Style.EMPTY
                .withColor(ChatFormatting.GREEN).withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/contractmod accept"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.literal("Help " + hostName + " and share the rewards"))));
        target.sendSystemMessage(Component.literal(hostName + " invited you to help with " + def.name()
                + ". Your kills will count and you both get the rewards. ").withStyle(ChatFormatting.YELLOW).append(accept));
        return null;
    }
    @Nullable
    public static String accept(ServerPlayer target) {
        Invite invite = PENDING.remove(target.getUUID());
        if (invite == null) return "You have no pending invitation";
        if (target.server.overworld().getGameTime() > invite.expires()) return "That invitation has expired";
        ServerPlayer host = target.server.getPlayerList().getPlayer(invite.host());
        if (host == null) return "The player who invited you is no longer online";
        ContractDefinition def = PlayerContractData.getActive(host);
        if (def == null || !def.id().equals(invite.contract())) return "That contract is no longer active";
        if (!ContractConfig.helpAllowed(def.type())) {
            return "Helping on " + ContractConfig.pluralName(def.type()) + " is turned off on this server";
        }
        PlayerContractData.addHelper(host, target.getUUID());
        ModNetwork.syncActive(host);
        host.sendSystemMessage(Component.literal(target.getName().getString() + " is now helping with "
                + def.name()).withStyle(ChatFormatting.GREEN));
        target.sendSystemMessage(Component.literal("You are now helping " + host.getName().getString() + " with "
                + def.name() + ". Use /contractmod leave to stop.").withStyle(ChatFormatting.GREEN));
        return null;
    }
    public static boolean leave(ServerPlayer helper) {
        boolean left = false;
        for (ServerPlayer host : helper.server.getPlayerList().getPlayers()) {
            if (PlayerContractData.removeHelper(host, helper.getUUID())) {
                left = true;
                host.sendSystemMessage(Component.literal(helper.getName().getString() + " stopped helping")
                        .withStyle(ChatFormatting.YELLOW));
            }
        }
        if (left) ModNetwork.syncActive(helper);
        return left;
    }
    public static void dismissHelpers(ServerPlayer host, @Nullable String reason) {
        List<ServerPlayer> helpers = helpersOf(host);
        PlayerContractData.clearHelpers(host);
        for (ServerPlayer helper : helpers) {
            if (reason != null) helper.sendSystemMessage(Component.literal(reason).withStyle(ChatFormatting.YELLOW));
            ModNetwork.syncActive(helper);
        }
    }
}
