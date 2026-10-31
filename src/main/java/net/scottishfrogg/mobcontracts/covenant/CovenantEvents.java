package net.scottishfrogg.mobcontracts.covenant;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DecoratedPotBlock;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.scottishfrogg.mobcontracts.ContractMod;

@Mod.EventBusSubscriber(modid = ContractMod.MOD_ID)
public class CovenantEvents {
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player
                && player.isAlive() && player.tickCount % 10 == 0) {
            CovenantManager.tick(player);
        }
    }
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        CovenantManager.onLivingDeath(event.getEntity(), event.getSource());
    }
    @SubscribeEvent
    public static void onExplosionStart(ExplosionEvent.Start event) {
        if (event.getLevel() instanceof ServerLevel level) {
            Entity source = event.getExplosion().getDirectSourceEntity();
            if (source != null) CovenantManager.onExplosion(level, source);
        }
    }
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel level && event.getEntity() instanceof Mob mob
                && CovenantManager.isOrphanedElite(level, mob)) {
            event.setCanceled(true);
        }
    }
    @SubscribeEvent
    public static void onArrowJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide) return;
        if (event.getEntity() instanceof Arrow arrow && arrow.getOwner() instanceof Mob shooter
                && shooter.getPersistentData().getString(CovenantManager.BEHAVIORS_TAG).contains("tipped_harming_arrows")) {
            arrow.addEffect(new MobEffectInstance(MobEffects.HARM, 1, 0));
        }
    }
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof AbstractSkeleton skeleton)
                || skeleton.level().isClientSide || skeleton.tickCount % 20 != 0) return;
        if (!skeleton.getPersistentData().getString(CovenantManager.BEHAVIORS_TAG).contains("fast_fire")) return;
        for (WrappedGoal wrapped : skeleton.goalSelector.getAvailableGoals()) {
            if (wrapped.getGoal() instanceof RangedBowAttackGoal<?> bowGoal) {
                bowGoal.setMinAttackInterval(2);
            }
        }
    }
    @SubscribeEvent
    public static void onToss(ItemTossEvent event) {
        ItemStack stack = event.getEntity().getItem();
        if (!CovenantCompass.isCompass(stack)) return;
        event.setCanceled(true);
        if (!event.getPlayer().level().isClientSide) event.getPlayer().getInventory().add(stack);
    }
    @SubscribeEvent
    public static void onPlayerDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof ServerPlayer) {
            event.getDrops().removeIf(drop -> CovenantCompass.isCompass(drop.getItem()));
        }
    }
    private static boolean heldCompass(Player player) {
        return CovenantCompass.isCompass(player.getMainHandItem()) || CovenantCompass.isCompass(player.getOffhandItem());
    }
    private static boolean takesItems(Entity entity) {
        return entity instanceof ItemFrame || entity instanceof ArmorStand || entity instanceof Allay;
    }
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (takesItems(event.getTarget()) && heldCompass(event.getEntity())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }
    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (takesItems(event.getTarget()) && heldCompass(event.getEntity())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (heldCompass(event.getEntity())
                && event.getLevel().getBlockState(event.getPos()).getBlock() instanceof DecoratedPotBlock) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }
}
