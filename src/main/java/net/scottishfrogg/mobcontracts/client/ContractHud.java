package net.scottishfrogg.mobcontracts.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.scottishfrogg.mobcontracts.ContractMod;
import net.scottishfrogg.mobcontracts.contract.ContractDefinition;
import net.scottishfrogg.mobcontracts.data.PlayerContractData;

import java.util.StringJoiner;

public final class ContractHud {
    private static final ResourceLocation BAR =
            new ResourceLocation(ContractMod.MOD_ID, "textures/gui/progress_bar.png");
    private static final int BAR_W = 60, BAR_H = 5, MARGIN = 4;
    public static final IGuiOverlay OVERLAY = (gui, g, partialTick, width, height) -> {
        Minecraft mc = Minecraft.getInstance();
        ContractDefinition def = ClientContractState.getActive();
        if (mc.options.hideGui || mc.player == null || def == null) return;
        Font font = mc.font;
        int y = MARGIN;
        String helping = ClientContractState.getHelpingName();
        if (!helping.isEmpty()) {
            y = drawRight(g, font, Component.literal("Helping " + helping).withStyle(ChatFormatting.GREEN), width, y);
        }
        y = drawRight(g, font, Component.literal(def.name()).withStyle(ChatFormatting.AQUA), width, y);
        y = drawRight(g, font, Component.literal(taskText(def) + progressSuffix(def)).withStyle(ChatFormatting.RED), width, y);
        y = drawRight(g, font, Component.literal("Reward: ").append(def.reward().text())
                .withStyle(ChatFormatting.GOLD), width, y);
        if (ClientContractState.bountiesUnlocked()) {
            int x = width - MARGIN - BAR_W;
            int perBounty = PlayerContractData.CONTRACTS_PER_BOUNTY;
            int fill = BAR_W * Math.min(ClientContractState.getTowardBounty(), perBounty) / perBounty;
            g.blit(BAR, x, y + 1, 0, 0, BAR_W, BAR_H, BAR_W, BAR_H * 2);
            if (fill > 0) {
                g.blit(BAR, x, y + 1, 0, BAR_H, fill, BAR_H, BAR_W, BAR_H * 2);
            }
        }
    };
    private static int drawRight(GuiGraphics g, Font font, Component text, int screenWidth, int y) {
        g.drawString(font, text, screenWidth - MARGIN - font.width(text), y, 0xFFFFFF, true);
        return y + font.lineHeight + 1;
    }
    private static String taskText(ContractDefinition def) {
        String target = ClientContractState.getTargetName();
        return def.task().replace("{target}", target.isEmpty() ? "your target" : target);
    }
    private static String progressSuffix(ContractDefinition def) {
        int[] prog = ClientContractState.getProgress();
        StringJoiner joiner = new StringJoiner(", ", " (", ")");
        for (int i = 0; i < def.objectives().size(); i++) {
            int done = i < prog.length ? prog[i] : 0;
            joiner.add(done + "/" + def.objectives().get(i).count());
        }
        return joiner.toString();
    }
    private ContractHud() {}
}
