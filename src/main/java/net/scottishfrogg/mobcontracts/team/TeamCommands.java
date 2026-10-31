package net.scottishfrogg.mobcontracts.team;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.scottishfrogg.mobcontracts.ContractMod;

@Mod.EventBusSubscriber(modid = ContractMod.MOD_ID)
public class TeamCommands {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("contractmod")
                .then(Commands.literal("invite")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> invite(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("accept").executes(ctx -> accept(ctx.getSource())))
                .then(Commands.literal("leave").executes(ctx -> leave(ctx.getSource()))));
    }
    private static int invite(CommandSourceStack src, ServerPlayer target) throws CommandSyntaxException {
        String error = ContractTeam.invite(src.getPlayerOrException(), target);
        if (error != null) {
            src.sendFailure(Component.literal(error));
            return 0;
        }
        return 1;
    }
    private static int accept(CommandSourceStack src) throws CommandSyntaxException {
        String error = ContractTeam.accept(src.getPlayerOrException());
        if (error != null) {
            src.sendFailure(Component.literal(error));
            return 0;
        }
        return 1;
    }
    private static int leave(CommandSourceStack src) throws CommandSyntaxException {
        if (!ContractTeam.leave(src.getPlayerOrException())) {
            src.sendFailure(Component.literal("You are not helping anyone"));
            return 0;
        }
        src.sendSuccess(() -> Component.literal("You stopped helping"), false);
        return 1;
    }
}
