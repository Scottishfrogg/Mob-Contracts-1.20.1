package net.scottishfrogg.mobcontracts.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.scottishfrogg.mobcontracts.client.ClientContractState;
import net.scottishfrogg.mobcontracts.contract.ContractDefinition;
import net.scottishfrogg.mobcontracts.contract.ContractRegistry;
import net.scottishfrogg.mobcontracts.contract.ContractType;
import net.scottishfrogg.mobcontracts.covenant.CovenantManager;
import net.scottishfrogg.mobcontracts.data.PlayerContractData;
import net.scottishfrogg.mobcontracts.network.ModNetwork;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ContractItem extends Item {
    public static final String TAG_CONTRACT_ID = "ContractId";
    private final ContractType type;
    public ContractItem(Properties properties, ContractType type) {
        super(properties.stacksTo(1));
        this.type = type;
    }
    public ContractType getType() {
        return type;
    }
    public static ItemStack create(Item item, ResourceLocation contractId) {
        ItemStack stack = new ItemStack(item);
        stack.getOrCreateTag().putString(TAG_CONTRACT_ID, contractId.toString());
        return stack;
    }
    @Nullable
    public static ResourceLocation getContractId(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_CONTRACT_ID)) return null;
        return ResourceLocation.tryParse(tag.getString(TAG_CONTRACT_ID));
    }
    @Nullable
    private static ContractDefinition lookup(ItemStack stack) {
        return ContractRegistry.get(getContractId(stack));
    }
    @Override
    public Component getName(ItemStack stack) {
        ContractDefinition def = lookup(stack);
        Component base = def != null ? Component.literal(def.name()) : super.getName(stack);
        return base.copy().withStyle(ChatFormatting.AQUA);
    }
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        ContractDefinition def = lookup(stack);
        if (def == null) return;
        tooltip.add(Component.literal(def.task().replace("{target}", "a random online player")).withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("Reward: ").append(def.reward().text()).withStyle(ChatFormatting.GOLD));
    }
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(stack);
        if (level.isClientSide) {
            return ClientContractState.hasActive()
                    ? InteractionResultHolder.fail(stack)
                    : InteractionResultHolder.success(stack);
        }
        ServerPlayer sp = (ServerPlayer) player;
        ContractDefinition def = lookup(stack);
        if (def == null) return InteractionResultHolder.fail(stack);
        if (PlayerContractData.hasActive(sp)) {
            sp.displayClientMessage(Component.literal("You already have an active contract")
                    .withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }
        if (def.type() == ContractType.COVENANT && !CovenantManager.canBegin(sp, def)) {
            return InteractionResultHolder.fail(stack);
        }
        PlayerContractData.start(sp, def);
        stack.shrink(1);
        if (def.type() == ContractType.COVENANT) CovenantManager.begin(sp, def);
        ModNetwork.syncActive(sp);
        return InteractionResultHolder.success(stack);
    }
}
