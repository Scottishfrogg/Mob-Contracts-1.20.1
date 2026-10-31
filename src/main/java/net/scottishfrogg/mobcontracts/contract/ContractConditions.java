package net.scottishfrogg.mobcontracts.contract;

import com.google.gson.JsonSyntaxException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class ContractConditions {
    public static final String BOW_TAG = "contractmod_bow";
    public static final String CROSSBOW_TAG = "contractmod_crossbow";
    public static final String FIREBALL_ORIGIN_TAG = "contractmod_fireball_origin";
    public static final String SONIC_BOOM_TAG = "contractmod_sonic_boom";
    public static final String CRYSTAL_TAG = "contractmod_crystal_destroyed";
    public record Context(ServerPlayer player, LivingEntity victim, DamageSource source) {}
    @FunctionalInterface
    public interface Condition {
        boolean test(Context ctx, @Nullable String arg);
    }
    private static final Map<String, Condition> CONDITIONS = new HashMap<>();
    static {
        register("daytime", (c, a) -> c.victim().level().isDay());
        register("nighttime", (c, a) -> c.victim().level().getDayTime() % 24000L >= 12000L);
        register("victim_on_fire", (c, a) -> c.victim().isOnFire());
        register("player_on_fire", (c, a) -> c.player().isOnFire());
        register("victim_climbing", (c, a) -> c.victim().onClimbable());
        register("victim_on_land", (c, a) -> c.victim().onGround() && !c.victim().isInWaterOrBubble());
        register("victim_hostile", (c, a) -> c.victim() instanceof Enemy);
        register("riding", (c, a) -> {
            Entity vehicle = c.victim().getVehicle();
            ResourceLocation wanted = a == null ? null : ResourceLocation.tryParse(a);
            return vehicle != null && wanted != null
                    && wanted.equals(ForgeRegistries.ENTITY_TYPES.getKey(vehicle.getType()));
        });
        register("in_dimension", (c, a) -> {
            ResourceLocation wanted = a == null ? null : ResourceLocation.tryParse(a);
            return wanted != null && wanted.equals(c.victim().level().dimension().location());
        });
        register("own_fireball", (c, a) -> c.source().getDirectEntity() instanceof LargeFireball ball
                && ball.getPersistentData().hasUUID(FIREBALL_ORIGIN_TAG)
                && ball.getPersistentData().getUUID(FIREBALL_ORIGIN_TAG).equals(c.victim().getUUID()));
        register("no_gold_armor", (c, a) -> {
            for (ItemStack stack : c.player().getArmorSlots()) {
                if (stack.getItem() instanceof ArmorItem armor && armor.getMaterial() == ArmorMaterials.GOLD) return false;
            }
            return true;
        });
        register("victim_wet", (c, a) -> c.victim().isInWaterRainOrBubble());
        register("fists_only", (c, a) -> c.source().is(DamageTypes.PLAYER_ATTACK)
                && c.source().getDirectEntity() == c.player()
                && c.player().getMainHandItem().isEmpty());
        register("enchanted_sword", (c, a) -> c.source().getDirectEntity() == c.player()
                && c.player().getMainHandItem().getItem() instanceof SwordItem
                && c.player().getMainHandItem().isEnchanted());
        register("wooden_sword", (c, a) -> c.source().getDirectEntity() == c.player()
                && c.player().getMainHandItem().is(Items.WOODEN_SWORD));
        register("bow_kill", (c, a) -> c.source().getDirectEntity() instanceof AbstractArrow arrow
                && arrow.getPersistentData().getBoolean(BOW_TAG));
        register("no_bow_or_crossbow", (c, a) -> !(c.source().getDirectEntity() instanceof AbstractArrow arrow
                && (arrow.getPersistentData().getBoolean(BOW_TAG) || arrow.getPersistentData().getBoolean(CROSSBOW_TAG))));
        register("no_sonic_boom", (c, a) -> !c.victim().getPersistentData().getBoolean(SONIC_BOOM_TAG));
        register("no_crystals_destroyed", (c, a) -> !c.victim().getPersistentData().getBoolean(CRYSTAL_TAG));
        register("crossbow_kill", (c, a) -> c.source().getDirectEntity() instanceof AbstractArrow arrow
                && arrow.getPersistentData().getBoolean(CROSSBOW_TAG));
        register("no_mining_fatigue", (c, a) -> !c.player().hasEffect(MobEffects.DIG_SLOWDOWN));
        register("wearing_pumpkin", (c, a) ->
                c.player().getItemBySlot(EquipmentSlot.HEAD).is(Items.CARVED_PUMPKIN));
        register("awake_days", (c, a) -> c.player().getStats()
                .getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_REST)) >= parseInt(a) * 24000L);
        register("distance", (c, a) -> c.player().distanceTo(c.victim()) >= parseInt(a));
        register("swift_sneak_leggings", (c, a) -> EnchantmentHelper.getItemEnchantmentLevel(
                Enchantments.SWIFT_SNEAK, c.player().getItemBySlot(EquipmentSlot.LEGS)) > 0);
    }
    private ContractConditions() {}
    public static void register(String id, Condition condition) {
        CONDITIONS.put(id, condition);
    }
    private static int parseInt(@Nullable String s) {
        try {
            return s == null ? 0 : Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    public static boolean test(@Nullable String conditions, Context ctx) {
        if (conditions == null || conditions.isBlank()) return true;
        for (String token : conditions.split(",")) {
            String[] parts = token.trim().split(":", 2);
            Condition c = CONDITIONS.get(parts[0]);
            if (c == null || !c.test(ctx, parts.length > 1 ? parts[1] : null)) return false;
        }
        return true;
    }
    public static void validate(@Nullable String conditions) {
        if (conditions == null || conditions.isBlank()) return;
        for (String token : conditions.split(",")) {
            String id = token.trim().split(":", 2)[0];
            if (!CONDITIONS.containsKey(id)) throw new JsonSyntaxException("Unknown condition '" + id + "'");
        }
    }
}
