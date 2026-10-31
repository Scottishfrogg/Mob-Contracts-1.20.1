package net.scottishfrogg.mobcontracts.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.scottishfrogg.mobcontracts.config.ContractConfig;
import net.scottishfrogg.mobcontracts.rank.HunterRanks;
import java.util.function.Consumer;

public class ContractConfigScreen extends Screen {
    private final Screen parent;
    private String contractDays = format(ContractConfig.CONTRACT_INTERVAL_DAYS.get());
    private String covenantDays = format(ContractConfig.COVENANT_INTERVAL_DAYS.get());
    private String covenantChance = format(ContractConfig.COVENANT_CHANCE.get());
    private boolean chests = ContractConfig.CONTRACTS_IN_CHESTS.get();
    private boolean rankColors = ContractConfig.RANK_COLORS.get();
    private boolean helpContracts = ContractConfig.HELP_ON_CONTRACTS.get();
    private boolean helpBounties = ContractConfig.HELP_ON_BOUNTIES.get();
    private boolean helpCovenants = ContractConfig.HELP_ON_COVENANTS.get();
    private boolean disableContracts = ContractConfig.DISABLE_CONTRACTS.get();
    private boolean disableBounties = ContractConfig.DISABLE_BOUNTIES.get();
    private boolean disableCovenants = ContractConfig.DISABLE_COVENANTS.get();
    public ContractConfigScreen(Screen parent) {
        super(Component.literal("Mob Contracts Config"));
        this.parent = parent;
    }
    private static String format(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
    }
    @Override
    protected void init() {
        int cx = width / 2;
        numberBox(cx + 30, 38, contractDays, s -> contractDays = s);
        numberBox(cx + 30, 62, covenantDays, s -> covenantDays = s);
        numberBox(cx + 30, 86, covenantChance, s -> covenantChance = s);
        toggle(cx - 155, 126, 150, "Contracts in chests", chests, v -> chests = v);
        toggle(cx + 5, 126, 150, "Rank name colors", rankColors, v -> rankColors = v);
        toggle(cx - 155, 166, 100, "Contracts", helpContracts, v -> helpContracts = v);
        toggle(cx - 50, 166, 100, "Bounties", helpBounties, v -> helpBounties = v);
        toggle(cx + 55, 166, 100, "Covenants", helpCovenants, v -> helpCovenants = v);
        toggle(cx - 155, 206, 100, "Contracts", disableContracts, v -> disableContracts = v);
        toggle(cx - 50, 206, 100, "Bounties", disableBounties, v -> disableBounties = v);
        toggle(cx + 55, 206, 100, "Covenants", disableCovenants, v -> disableCovenants = v);
        int buttonY = height - 28;
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> {
            save();
            onClose();
        }).bounds(cx - 155, buttonY, 150, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose())
                .bounds(cx + 5, buttonY, 150, 20).build());
    }
    private void numberBox(int x, int y, String value, Consumer<String> onChange) {
        EditBox box = new EditBox(font, x, y, 70, 18, Component.empty());
        box.setMaxLength(8);
        box.setFilter(s -> s.matches("[0-9]*\\.?[0-9]*"));
        box.setValue(value);
        box.setResponder(onChange);
        addRenderableWidget(box);
    }
    private void toggle(int x, int y, int w, String label, boolean initial, Consumer<Boolean> onChange) {
        addRenderableWidget(CycleButton.onOffBuilder(initial)
                .create(x, y, w, 20, Component.literal(label), (button, value) -> onChange.accept(value)));
    }
    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        int cx = width / 2;
        g.drawCenteredString(font, title, cx, 10, 0xFFFFFF);
        g.drawString(font, "Delivery", cx - 155, 26, 0xFFAA00, false);
        g.drawString(font, "Contract interval (Minecraft days)", cx - 155, 43, 0xFFFFFF, false);
        g.drawString(font, "default 3", cx + 105, 43, 0x777777, false);
        g.drawString(font, "Covenant roll interval (days)", cx - 155, 67, 0xFFFFFF, false);
        g.drawString(font, "default 3", cx + 105, 67, 0x777777, false);
        g.drawString(font, "Covenant chance", cx - 155, 91, 0xFFFFFF, false);
        g.drawString(font, "default 0.2", cx + 105, 91, 0x777777, false);
        g.drawString(font, "Chests and ranks", cx - 155, 114, 0xFFAA00, false);
        g.drawString(font, "Friends can help on", cx - 155, 154, 0xFFAA00, false);
        g.drawString(font, "Players can disable in the Hunt History", cx - 155, 194, 0xFFAA00, false);
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.getSingleplayerServer() == null) {
            g.drawCenteredString(font, Component.literal("You are on a server: edit the server's own config file for real changes")
                    .withStyle(ChatFormatting.RED), cx, height - 42, 0xFF5555);
        }
        super.render(g, mouseX, mouseY, partialTick);
    }
    private static double parse(String text, double current, double min, double max) {
        try {
            return Mth.clamp(Double.parseDouble(text), min, max);
        } catch (NumberFormatException e) {
            return current;
        }
    }
    private void save() {
        ContractConfig.CONTRACT_INTERVAL_DAYS.set(parse(contractDays, ContractConfig.CONTRACT_INTERVAL_DAYS.get(), 0.05, 365.0));
        ContractConfig.COVENANT_INTERVAL_DAYS.set(parse(covenantDays, ContractConfig.COVENANT_INTERVAL_DAYS.get(), 0.05, 365.0));
        ContractConfig.COVENANT_CHANCE.set(parse(covenantChance, ContractConfig.COVENANT_CHANCE.get(), 0.0, 1.0));
        ContractConfig.CONTRACTS_IN_CHESTS.set(chests);
        ContractConfig.RANK_COLORS.set(rankColors);
        ContractConfig.HELP_ON_CONTRACTS.set(helpContracts);
        ContractConfig.HELP_ON_BOUNTIES.set(helpBounties);
        ContractConfig.HELP_ON_COVENANTS.set(helpCovenants);
        ContractConfig.DISABLE_CONTRACTS.set(disableContracts);
        ContractConfig.DISABLE_BOUNTIES.set(disableBounties);
        ContractConfig.DISABLE_COVENANTS.set(disableCovenants);
        ContractConfig.SPEC.save();
        IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server != null) server.execute(() -> HunterRanks.syncAll(server));
    }
    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
