package net.scottishfrogg.mobcontracts.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.scottishfrogg.mobcontracts.contract.ContractDefinition;
import net.scottishfrogg.mobcontracts.network.CancelContractPacket;
import net.scottishfrogg.mobcontracts.network.ModNetwork;
import net.scottishfrogg.mobcontracts.network.RequestHistoryPacket;
import org.lwjgl.glfw.GLFW;

public final class ModKeys {
    private static final int CONFIRM_TICKS = 60;
    public static final KeyMapping CANCEL = new KeyMapping("key.contractmod.cancel",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, "key.categories.contractmod");
    public static final KeyMapping HISTORY = new KeyMapping("key.contractmod.history",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, "key.categories.contractmod");
    private static long confirmUntil = -1;
    private ModKeys() {}
    public static void onHistoryPressed() {
        ModNetwork.CHANNEL.sendToServer(new RequestHistoryPacket());
    }
    public static void onCancelPressed() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (!ClientContractState.hasActive()) {
            mc.player.displayClientMessage(
                    Component.literal("You have no active contract").withStyle(ChatFormatting.RED), true);
            return;
        }
        long now = mc.level.getGameTime();
        if (now <= confirmUntil) {
            confirmUntil = -1;
            ModNetwork.CHANNEL.sendToServer(new CancelContractPacket());
            return;
        }
        confirmUntil = now + CONFIRM_TICKS;
        ContractDefinition def = ClientContractState.getActive();
        String name = def != null ? def.name() : "your contract";
        String helping = ClientContractState.getHelpingName();
        String what = helping.isEmpty() ? "cancel " + name : "stop helping " + helping;
        mc.player.displayClientMessage(Component.literal("Press ").append(CANCEL.getTranslatedKeyMessage())
                .append(" again to " + what).withStyle(ChatFormatting.YELLOW), true);
    }
}
