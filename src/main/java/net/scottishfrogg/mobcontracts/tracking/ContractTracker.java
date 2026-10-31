package net.scottishfrogg.mobcontracts.tracking;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.ForgeRegistries;
import net.scottishfrogg.mobcontracts.advancement.ModTriggers;
import net.scottishfrogg.mobcontracts.contract.ContractConditions;
import net.scottishfrogg.mobcontracts.contract.ContractDefinition;
import net.scottishfrogg.mobcontracts.contract.ContractObjective;
import net.scottishfrogg.mobcontracts.contract.ContractReward;
import net.scottishfrogg.mobcontracts.contract.ContractType;
import net.scottishfrogg.mobcontracts.covenant.CovenantManager;
import net.scottishfrogg.mobcontracts.data.PlayerContractData;
import net.scottishfrogg.mobcontracts.network.ModNetwork;
import net.scottishfrogg.mobcontracts.rank.HunterRanks;
import net.scottishfrogg.mobcontracts.team.ContractTeam;

import java.util.Arrays;
import java.util.List;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;

public final class ContractTracker {
    private ContractTracker() {}
    public static void onKill(ServerPlayer actor, LivingEntity victim, DamageSource source) {
        for (ServerPlayer owner : ContractTeam.contractOwnersFor(actor)) {
            applyKill(owner, actor, victim, source);
        }
    }
    public static void onPlayerDamaged(ServerPlayer actor, DamageSource source) {
        for (ServerPlayer owner : ContractTeam.contractOwnersFor(actor)) {
            resetObjectives(owner, actor, o -> {
                String r = o.resetOn();
                if (r == null) return false;
                if (r.equals(ContractObjective.RESET_ANY)) return true;
                if (r.equals(ContractObjective.RESET_EXPLOSION)) return source.is(DamageTypeTags.IS_EXPLOSION);
                return r.equals(ContractObjective.RESET_SHULKER_BULLET) && source.getDirectEntity() instanceof ShulkerBullet;
            });
        }
    }
    public static void onResetTrigger(ServerPlayer actor, String trigger) {
        for (ServerPlayer owner : ContractTeam.contractOwnersFor(actor)) {
            resetObjectives(owner, actor, o -> trigger.equals(o.resetOn()));
        }
    }
    public static void fireEvent(ServerPlayer actor, String event) {
        for (ServerPlayer owner : ContractTeam.contractOwnersFor(actor)) {
            raiseEvent(owner, event, cur -> cur + 1);
        }
    }
    public static void fireEventAtLeast(ServerPlayer actor, String event, int value) {
        for (ServerPlayer owner : ContractTeam.contractOwnersFor(actor)) {
            raiseEvent(owner, event, cur -> Math.max(cur, value));
        }
    }
    public static void fireOwnEvent(ServerPlayer owner, String event) {
        raiseEvent(owner, event, cur -> cur + 1);
    }
    public static boolean hasEventObjective(ServerPlayer actor, String event) {
        for (ServerPlayer owner : ContractTeam.contractOwnersFor(actor)) {
            ContractDefinition def = PlayerContractData.getActive(owner);
            if (def != null && def.objectives().stream()
                    .anyMatch(o -> o.type() == ContractObjective.Type.EVENT && event.equals(o.event()))) {
                return true;
            }
        }
        return false;
    }
    private static void applyKill(ServerPlayer owner, ServerPlayer actor, LivingEntity victim, DamageSource source) {
        ContractDefinition def = PlayerContractData.getActive(owner);
        if (def == null) return;
        ResourceLocation victimId = ForgeRegistries.ENTITY_TYPES.getKey(victim.getType());
        ContractConditions.Context ctx = new ContractConditions.Context(actor, victim, source);
        for (int i = 0; i < def.objectives().size(); i++) {
            ContractObjective o = def.objectives().get(i);
            if (o.type() != ContractObjective.Type.KILL) continue;
            boolean entityMatches = o.entities().isEmpty() || o.entities().contains(victimId);
            if (!entityMatches || !ContractConditions.test(o.condition(), ctx)) continue;
            if (raise(owner, def, i, progressAt(owner, i) + 1)) return;
        }
    }
    private static void resetObjectives(ServerPlayer owner, ServerPlayer actor, Predicate<ContractObjective> shouldReset) {
        ContractDefinition def = PlayerContractData.getActive(owner);
        if (def == null) return;
        int[] progress = Arrays.copyOf(PlayerContractData.getProgress(owner), def.objectives().size());
        boolean reset = false;
        for (int i = 0; i < progress.length; i++) {
            if (progress[i] > 0 && shouldReset.test(def.objectives().get(i))) {
                progress[i] = 0;
                reset = true;
            }
        }
        if (reset) {
            PlayerContractData.setProgress(owner, progress);
            ModNetwork.syncActive(owner);
            Component broken = Component.literal("Streak broken!").withStyle(ChatFormatting.RED);
            owner.displayClientMessage(broken, true);
            if (actor != owner) actor.displayClientMessage(broken, true);
        }
    }
    private static void raiseEvent(ServerPlayer owner, String event, IntUnaryOperator newValue) {
        ContractDefinition def = PlayerContractData.getActive(owner);
        if (def == null) return;
        for (int i = 0; i < def.objectives().size(); i++) {
            ContractObjective o = def.objectives().get(i);
            if (o.type() == ContractObjective.Type.EVENT && event.equals(o.event())) {
                if (raise(owner, def, i, newValue.applyAsInt(progressAt(owner, i)))) return;
            }
        }
    }
    private static int progressAt(ServerPlayer owner, int index) {
        int[] p = PlayerContractData.getProgress(owner);
        return index < p.length ? p[index] : 0;
    }
    private static boolean raise(ServerPlayer owner, ContractDefinition def, int index, int newValue) {
        List<ContractObjective> objectives = def.objectives();
        int[] progress = Arrays.copyOf(PlayerContractData.getProgress(owner), objectives.size());
        int value = Math.min(objectives.get(index).count(), newValue);
        if (value <= progress[index]) return false;
        progress[index] = value;
        PlayerContractData.setProgress(owner, progress);
        boolean done = true;
        for (int i = 0; i < objectives.size(); i++) {
            if (progress[i] < objectives.get(i).count()) {
                done = false;
                break;
            }
        }
        if (done) {
            complete(owner, def);
        } else {
            ModNetwork.syncActive(owner);
        }
        return done;
    }
    private static void giveRewards(ServerPlayer player, ContractDefinition def) {
        for (ContractReward.RewardItem reward : def.reward().items()) {
            int remaining = reward.count();
            while (remaining > 0) {
                ItemStack stack = reward.template();
                int n = Math.min(remaining, stack.getMaxStackSize());
                stack.setCount(n);
                remaining -= n;
                ItemHandlerHelper.giveItemToPlayer(player, stack);
            }
        }
        if (def.reward().xp() > 0) player.giveExperiencePoints(def.reward().xp());
    }
    private static void complete(ServerPlayer owner, ContractDefinition def) {
        List<ServerPlayer> helpers = ContractTeam.helpersOf(owner);
        giveRewards(owner, def);
        for (ServerPlayer helper : helpers) {
            giveRewards(helper, def);
            helper.sendSystemMessage(Component.literal(owner.getName().getString() + "'s " + def.name()
                    + " completed! You received your share of the rewards.").withStyle(ChatFormatting.GREEN));
        }
        ContractTeam.dismissHelpers(owner, null);
        CovenantManager.end(owner);
        PlayerContractData.clearActive(owner);
        boolean earnedBounty = PlayerContractData.recordCompletion(
                owner, def, PlayerContractData.bountiesUnlocked(owner));
        PlayerContractData.recordHistory(owner, def);
        HunterRanks.onCompletion(owner);
        if (def.type() == ContractType.CONTRACT) {
            ModTriggers.CONTRACTS_COMPLETED.trigger(owner, PlayerContractData.getCompleted(owner));
        }
        if (def.type() == ContractType.COVENANT) {
            ModTriggers.COVENANTS_COMPLETED.trigger(owner, PlayerContractData.getCovenantsCompleted(owner));
        }
        ModNetwork.syncActive(owner);
        owner.sendSystemMessage(Component.literal(def.name() + " completed!").withStyle(ChatFormatting.GREEN));
        if (earnedBounty) ContractGiver.giveRandom(owner, ContractType.BOUNTY);
    }
}
