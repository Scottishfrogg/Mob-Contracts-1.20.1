package net.scottishfrogg.mobcontracts;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.scottishfrogg.mobcontracts.tracking.ContractCanceller;

@Mod.EventBusSubscriber(modid = ContractMod.MOD_ID)
public class ContractCommands {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("contract")
                .then(Commands.literal("cancel").executes(ctx -> cancel(ctx.getSource()))));
    }
    private static int cancel(CommandSourceStack src) throws CommandSyntaxException {
        return ContractCanceller.cancel(src.getPlayerOrException()) ? 1 : 0;
    }
}
