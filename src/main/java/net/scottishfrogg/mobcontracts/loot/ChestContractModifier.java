package net.scottishfrogg.mobcontracts.loot;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.scottishfrogg.mobcontracts.config.ContractConfig;
import net.scottishfrogg.mobcontracts.contract.ContractDefinition;
import net.scottishfrogg.mobcontracts.contract.ContractRegistry;
import net.scottishfrogg.mobcontracts.data.PlayerContractData;
import net.scottishfrogg.mobcontracts.tracking.ContractGiver;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Supplier;

public class ChestContractModifier extends LootModifier {
    public static final float DEFAULT_CHANCE = 0.05F;
    public static final Supplier<Codec<ChestContractModifier>> CODEC = Suppliers.memoize(() ->
            RecordCodecBuilder.create(inst -> codecStart(inst)
                    .and(Codec.floatRange(0.0F, 1.0F).optionalFieldOf("chance", DEFAULT_CHANCE)
                            .forGetter(m -> m.chance))
                    .apply(inst, ChestContractModifier::new)));

    private final float chance;
    public ChestContractModifier(LootItemCondition[] conditions, float chance) {
        super(conditions);
        this.chance = chance;
    }
    @NotNull
    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        if (!ContractConfig.CONTRACTS_IN_CHESTS.get()) return loot;
        ResourceLocation table = context.getQueriedLootTableId();
        if (!table.getPath().startsWith("chests/")) return loot;
        if (context.getRandom().nextFloat() >= chance) return loot;
        List<ContractDefinition> pool = ContractRegistry.basicContracts();
        if (context.getParamOrNull(LootContextParams.THIS_ENTITY) instanceof ServerPlayer opener) {
            pool = pool.stream().filter(d -> !ContractConfig.isDisabledFor(opener, d)).toList();
        }
        if (pool.isEmpty()) return loot;
        ContractDefinition def = pool.get(context.getRandom().nextInt(pool.size()));
        loot.add(ContractGiver.createItem(def));
        return loot;
    }
    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }
}
