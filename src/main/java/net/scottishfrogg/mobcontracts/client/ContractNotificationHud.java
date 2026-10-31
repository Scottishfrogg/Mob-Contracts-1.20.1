package net.scottishfrogg.mobcontracts.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public final class ContractNotificationHud {
    private static final int TOP_Y = 12;
    private static final float SCALE = 1.5f;
    public static final IGuiOverlay OVERLAY = (gui, g, partialTick, width, height) -> {
        Minecraft mc = Minecraft.getInstance();
        Component msg = ClientNotifications.getCurrent();
        if (msg == null || mc.options.hideGui) return;
        int alpha = ClientNotifications.getAlpha(partialTick);
        if (alpha < 8) return;
        Font font = mc.font;
        RenderSystem.enableBlend();
        g.pose().pushPose();
        g.pose().translate(width / 2f, TOP_Y, 0);
        g.pose().scale(SCALE, SCALE, 1f);
        g.drawString(font, msg, -font.width(msg) / 2, 0, (alpha << 24) | 0xFFFFFF, true);
        g.pose().popPose();
        RenderSystem.disableBlend();
    };
    private ContractNotificationHud() {}
}
