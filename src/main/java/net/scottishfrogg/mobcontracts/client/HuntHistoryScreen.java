package net.scottishfrogg.mobcontracts.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.scottishfrogg.mobcontracts.contract.ContractDefinition;
import net.scottishfrogg.mobcontracts.contract.ContractRegistry;
import net.scottishfrogg.mobcontracts.network.HistorySyncPacket;
import net.scottishfrogg.mobcontracts.network.ModNetwork;
import net.scottishfrogg.mobcontracts.network.ToggleDisabledPacket;

import java.util.ArrayList;
import java.util.List;

public class HuntHistoryScreen extends Screen {
    private static final int ROW_H = 28;
    private static final int MARGIN = 20;
    private static final int LIST_TOP = 48;
    private List<HistorySyncPacket.Entry> entries = List.of();
    private int contracts, bounties, covenants;
    private double scroll;
    public HuntHistoryScreen(HistorySyncPacket data) {
        super(Component.literal("Hunt History"));
        update(data);
    }
    public void update(HistorySyncPacket data) {
        entries = data.entries();
        contracts = data.contracts();
        bounties = data.bounties();
        covenants = data.covenants();
        clampScroll();
    }
    private int listBottom() { return height - MARGIN; }
    private int maxScroll() { return Math.max(0, entries.size() * ROW_H - (listBottom() - LIST_TOP)); }
    private void clampScroll() { scroll = Mth.clamp(scroll, 0, maxScroll()); }
    private int entryAt(double mouseX, double mouseY) {
        if (mouseX < MARGIN || mouseX >= width - MARGIN || mouseY < LIST_TOP || mouseY >= listBottom()) return -1;
        double y = mouseY - LIST_TOP + scroll;
        int index = (int) (y / ROW_H);
        if (index < 0 || index >= entries.size() || y % ROW_H >= ROW_H - 2) return -1;
        return index;
    }
    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        g.drawString(font, Component.literal("Hunt History").withStyle(ChatFormatting.BOLD), MARGIN, 14, 0xFFFFFF, true);
        g.drawString(font, "Click an entry to disable it so it is never given to you again.",
                MARGIN, 28, 0xAAAAAA, false);
        String[] stats = {"Contracts: " + contracts, "Bounties: " + bounties, "Covenants: " + covenants};
        int statY = 8;
        for (String line : stats) {
            g.drawString(font, line, width - 10 - font.width(line), statY, 0xFFFFFF, true);
            statY += 11;
        }
        int left = MARGIN, right = width - MARGIN, top = LIST_TOP, bottom = listBottom();
        g.fill(left - 2, top - 2, right + 2, bottom + 2, 0x66000000);
        if (entries.isEmpty()) {
            g.drawCenteredString(font, "Nothing completed yet", width / 2, top + 24, 0x888888);
        }
        int hovered = entryAt(mouseX, mouseY);
        g.enableScissor(left, top, right, bottom);
        for (int i = 0; i < entries.size(); i++) {
            int rowY = top + i * ROW_H - (int) scroll;
            if (rowY + ROW_H < top || rowY > bottom) continue;
            drawRow(g, entries.get(i), left, right, rowY, i == hovered);
        }
        g.disableScissor();
        if (maxScroll() > 0) {
            int trackH = bottom - top;
            int thumbH = Math.max(16, trackH * trackH / (entries.size() * ROW_H));
            int thumbY = top + (int) ((trackH - thumbH) * (scroll / maxScroll()));
            g.fill(right + 4, top, right + 7, bottom, 0x55000000);
            g.fill(right + 4, thumbY, right + 7, thumbY + thumbH, 0xFFAAAAAA);
        }
        if (hovered >= 0) renderEntryTooltip(g, entries.get(hovered), mouseX, mouseY);
        super.render(g, mouseX, mouseY, partialTick);
    }
    private void drawRow(GuiGraphics g, HistorySyncPacket.Entry entry, int left, int right, int rowY, boolean hover) {
        ContractDefinition def = ContractRegistry.get(entry.id());
        String name = def != null ? def.name() : entry.id().toString();
        String typeLabel = "Unavailable";
        int typeColor = 0x888888;
        if (def != null) {
            switch (def.type()) {
                case CONTRACT -> { typeLabel = "Contract"; typeColor = 0x55FFFF; }
                case BOUNTY -> { typeLabel = "Bounty"; typeColor = 0xFFAA00; }
                case COVENANT -> { typeLabel = "Covenant"; typeColor = 0xFF55FF; }
            }
        }
        boolean off = entry.disabled();
        int background = off ? 0x33222222 : (hover ? 0x55FFFFFF : 0x33FFFFFF);
        g.fill(left, rowY, right, rowY + ROW_H - 2, background);
        g.drawString(font, name, left + 8, rowY + 5, off ? 0x666666 : typeColor, false);
        String times = entry.count() == 1 ? "1 time" : entry.count() + " times";
        g.drawString(font, typeLabel + "  -  completed " + times, left + 8, rowY + 16, off ? 0x555555 : 0xAAAAAA, false);
        String status = off ? "Disabled" : entry.canToggle() ? "Enabled" : "Always on";
        g.drawString(font, status, right - 8 - font.width(status), rowY + 10, off ? 0x666666 : 0x55FF55, false);
    }
    private void renderEntryTooltip(GuiGraphics g, HistorySyncPacket.Entry entry, int mouseX, int mouseY) {
        ContractDefinition def = ContractRegistry.get(entry.id());
        List<Component> tip = new ArrayList<>();
        if (def != null) {
            tip.add(Component.literal(def.name()).withStyle(ChatFormatting.AQUA));
            tip.add(Component.literal(def.task().replace("{target}", "a random online player")).withStyle(ChatFormatting.RED));
            tip.add(Component.literal("Reward: ").append(def.reward().text()).withStyle(ChatFormatting.GOLD));
        } else {
            tip.add(Component.literal(entry.id().toString()).withStyle(ChatFormatting.GRAY));
            tip.add(Component.literal("No longer available").withStyle(ChatFormatting.RED));
        }
        tip.add(Component.literal(!entry.canToggle()
                ? "Disabling is turned off on this server"
                : entry.disabled()
                ? "Click to enable: it can be given to you again"
                : "Click to disable: it will not be given to you again").withStyle(ChatFormatting.DARK_GRAY));
        g.renderComponentTooltip(font, tip, mouseX, mouseY);
    }
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int index = entryAt(mouseX, mouseY);
            if (index >= 0 && entries.get(index).canToggle()) {
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                ModNetwork.CHANNEL.sendToServer(new ToggleDisabledPacket(entries.get(index).id()));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        scroll -= delta * (ROW_H / 2.0);
        clampScroll();
        return true;
    }
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ModKeys.HISTORY.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
