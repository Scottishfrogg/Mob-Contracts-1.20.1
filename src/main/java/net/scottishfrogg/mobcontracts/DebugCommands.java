package net.scottishfrogg.mobcontracts;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.RegisterCommandsEvent;
import java.util.function.Predicate;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.ItemHandlerHelper;
import net.scottishfrogg.mobcontracts.advancement.ModTriggers;
import net.scottishfrogg.mobcontracts.contract.ContractDefinition;
import net.scottishfrogg.mobcontracts.contract.ContractRegistry;
import net.scottishfrogg.mobcontracts.contract.ContractType;
import net.scottishfrogg.mobcontracts.covenant.CovenantManager;
import net.scottishfrogg.mobcontracts.data.PlayerContractData;
import net.scottishfrogg.mobcontracts.item.ContractItem;
import net.scottishfrogg.mobcontracts.item.ModItems;
import net.scottishfrogg.mobcontracts.network.ModNetwork;
import net.scottishfrogg.mobcontracts.rank.HunterRanks;
import net.scottishfrogg.mobcontracts.tracking.ContractGiver;

@Mod.EventBusSubscriber(modid = ContractMod.MOD_ID)
public class DebugCommands {
    private static final Predicate<CommandSourceStack> OP = src -> src.hasPermission(2);
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("contractmod")
                .then(Commands.literal("give").requires(OP)
                        .then(Commands.argument("contract", ResourceLocationArgument.id())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(
                                        ContractRegistry.all().stream().map(ContractDefinition::id), builder))
                                .executes(ctx -> give(ctx.getSource(), ResourceLocationArgument.getId(ctx, "contract")))))
                .then(Commands.literal("deliver").requires(OP)
                        .then(Commands.literal("contract").executes(ctx -> deliver(ctx.getSource(), ContractType.CONTRACT)))
                        .then(Commands.literal("bounty").executes(ctx -> deliver(ctx.getSource(), ContractType.BOUNTY)))
                        .then(Commands.literal("covenant").executes(ctx -> deliver(ctx.getSource(), ContractType.COVENANT))))
                .then(Commands.literal("covenant").requires(OP)
                        .then(Commands.literal("near").executes(ctx -> covenantNear(ctx.getSource()))))
                .then(Commands.literal("set_completed").requires(OP)
                        .then(Commands.argument("count", IntegerArgumentType.integer(0))
                                .executes(ctx -> setCompleted(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "count")))))
                .then(Commands.literal("clear").requires(OP).executes(ctx -> clear(ctx.getSource()))));
    }
    private static int give(CommandSourceStack src, ResourceLocation id) throws CommandSyntaxException {
        ServerPlayer player = src.getPlayerOrException();
        ContractDefinition def = ContractRegistry.get(id);
        if (def == null) {
            src.sendFailure(Component.literal("Unknown contract " + id));
            return 0;
        }
        ItemHandlerHelper.giveItemToPlayer(player, ContractGiver.createItem(def));
        src.sendSuccess(() -> Component.literal("Gave " + def.name()), false);
        return 1;
    }
    private static int deliver(CommandSourceStack src, ContractType type) throws CommandSyntaxException {
        ServerPlayer player = src.getPlayerOrException();
        if (!ContractGiver.giveRandom(player, type)) {
            src.sendFailure(Component.literal("No unlocked " + type.name().toLowerCase() + " available"));
            return 0;
        }
        return 1;
    }
    private static int setCompleted(CommandSourceStack src, int count) throws CommandSyntaxException {
        ServerPlayer player = src.getPlayerOrException();
        PlayerContractData.setCompleted(player, count);
        ModTriggers.CONTRACTS_COMPLETED.trigger(player, count);
        HunterRanks.syncAll(src.getServer());
        src.sendSuccess(() -> Component.literal("Completed contracts set to " + count), false);
        return count;
    }
    private static int covenantNear(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer player = src.getPlayerOrException();
        if (!CovenantManager.debugMoveNear(player)) {
            src.sendFailure(Component.literal("Needs an active elite covenant and you in its dimension"));
            return 0;
        }
        src.sendSuccess(() -> Component.literal("Elite target moved near you"), false);
        return 1;
    }
    private static int clear(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer player = src.getPlayerOrException();
        PlayerContractData.clearActive(player);
        ModNetwork.syncActive(player);
        src.sendSuccess(() -> Component.literal("Active contract cleared"), false);
        return 1;
    }
}
